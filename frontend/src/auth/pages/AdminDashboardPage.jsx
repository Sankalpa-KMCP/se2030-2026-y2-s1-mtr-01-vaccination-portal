import React, { useCallback, useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import PostLoginGreeting from '../components/PostLoginGreeting';
import { listStaffRequests } from '../api/adminStaffRequestsApi';
import { ADMIN_ROLE, getRoleLabel, SUPER_ADMIN_ROLE } from '../constants/authRoles';
import { hasRole } from '../utils/roleUtils';
import { listCenters } from '../../features/center-operations/api/centerOperationsApi';
import { getOperationalAnalytics } from '../../features/analytics/api/analyticsApi';
import approvedHeroIllustration from '../assets/admin-dashboard-hero-approved.png';
import '../styles/admin-dashboard.css';

const operationalLinks = [
  {
    title: 'Campaign Management',
    description: 'Configure, schedule, and oversee vaccination campaigns.',
    to: '/app/campaigns',
    icon: 'bi-megaphone',
    decoration: 'bi-calendar3',
  },
  {
    title: 'Center Operations',
    description: 'Manage vaccination centers, operating hours, and resource allocations.',
    to: '/app/center-operations',
    icon: 'bi-hospital',
    decoration: 'bi-building',
  },
  {
    title: 'Session Management',
    description: 'Schedule sessions, manage time slots, and monitor booking capacity.',
    to: '/app/sessions',
    icon: 'bi-calendar-week',
    decoration: 'bi-clock-history',
  },
  {
    title: 'Operational Analytics',
    description: 'Review operational throughput and vaccination metrics.',
    to: '/app/analytics',
    icon: 'bi-bar-chart-line',
    decoration: 'bi-pie-chart',
  },
  {
    title: 'Inventory & Batches',
    description: 'Manage MF-04 manufacturers, vaccine batches, and center stock.',
    to: '/app/inventory',
    icon: 'bi-box-seam',
    decoration: 'bi-capsule',
  },
  {
    title: 'Check-In & Queue Management',
    description: 'Process citizen arrival check-ins, manage status overrides, record no-shows, and monitor clinic flow.',
    to: '/app/checkin/queue',
    icon: 'bi-people',
    decoration: 'bi-person-check',
  },
];

function useOverviewMetrics() {
  const [snapshot, setSnapshot] = useState({ loading: true, centers: null, analytics: null, requests: null });

  useEffect(() => {
    let active = true;
    Promise.allSettled([
      listCenters('ACTIVE'),
      getOperationalAnalytics(),
      listStaffRequests(),
    ]).then(([centers, analytics, requests]) => {
      if (!active) return;
      setSnapshot({
        loading: false,
        centers: centers.status === 'fulfilled' && Array.isArray(centers.value) ? centers.value : null,
        analytics: analytics.status === 'fulfilled' ? analytics.value : null,
        requests: requests.status === 'fulfilled' && Array.isArray(requests.value) ? requests.value : null,
      });
    });
    return () => { active = false; };
  }, []);

  return snapshot;
}

function MetricCard({ label, value, icon, tone, to, loading }) {
  const available = typeof value === 'number' && Number.isFinite(value);
  const display = loading ? '…' : available ? value.toLocaleString() : '—';
  const note = loading ? 'Loading live data' : available ? 'Live portal data' : 'Temporarily unavailable';

  return (
    <Link className={`admin-metric-card card admin-metric-card--${tone}`} to={to} aria-label={`${label}: ${loading ? 'loading' : available ? display : 'unavailable'}`}>
      <span className="admin-icon-tile" aria-hidden="true"><i className={`bi ${icon}`} /></span>
      <span className="admin-metric-card__content">
        <span className="admin-metric-card__label">{label}</span>
        <strong className="admin-metric-card__value">{display}</strong>
        <span className="admin-metric-card__note">{note}</span>
      </span>
      <i className="bi bi-arrow-up-right admin-metric-card__arrow" aria-hidden="true" />
    </Link>
  );
}

function ActionCard({ title, description, to, icon, decoration, featured = false }) {
  return (
    <Link className={`admin-action-card card${featured ? ' admin-action-card--featured' : ''}`} to={to} aria-label={`Open ${title}`}>
      <span className="admin-icon-tile" aria-hidden="true"><i className={`bi ${icon}`} /></span>
      <span className="admin-action-card__copy">
        <h3>{title}</h3>
        <span>{description}</span>
      </span>
      <span className="admin-action-card__arrow" aria-hidden="true"><i className="bi bi-arrow-right" /></span>
      <i className={`bi ${decoration} admin-action-card__decoration`} aria-hidden="true" />
    </Link>
  );
}

export default function AdminDashboardPage() {
  const { user, loginWelcome, consumeLoginWelcome } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const showLoginGreeting = loginWelcome?.audience === 'admin'
    && loginWelcome.userId != null
    && String(loginWelcome.userId) === String(user?.id);
  const dismissLoginGreeting = useCallback(() => {
    consumeLoginWelcome?.();
    navigate(location.pathname, { replace: true, state: null });
  }, [consumeLoginWelcome, location.pathname, navigate]);
  const isSuperAdmin = hasRole(user, SUPER_ADMIN_ROLE);
  const roleLabel = getRoleLabel(isSuperAdmin ? SUPER_ADMIN_ROLE : ADMIN_ROLE);
  const displayName = user?.fullName || user?.email || 'Administrator';
  const metrics = useOverviewMetrics();
  const analytics = metrics.analytics;

  const overviewCards = [
    { label: 'Active Centers', value: metrics.centers?.length, icon: 'bi-people-fill', tone: 'green', to: '/app/center-operations' },
    { label: 'Scheduled Sessions', value: analytics?.sessionCountByLifecycleStatus?.SCHEDULED, icon: 'bi-calendar-check', tone: 'blue', to: '/app/sessions' },
    { label: 'Approved Capacity', value: analytics?.totalApprovedCapacity, icon: 'bi-person-check-fill', tone: 'purple', to: '/app/analytics' },
    { label: 'Reserved Slots', value: analytics?.totalReservedSlots, icon: 'bi-calendar2-check', tone: 'amber', to: '/app/analytics' },
    { label: 'Pending Staff Requests', value: metrics.requests?.length, icon: 'bi-file-earmark-person', tone: 'teal', to: '/app/admin/staff-requests' },
  ];

  return (
    <>
      {showLoginGreeting && (
        <PostLoginGreeting
          audience="admin"
          name={displayName}
          onComplete={dismissLoginGreeting}
        />
      )}
      <div className="admin-dashboard" inert={showLoginGreeting ? true : undefined}>
      <section className="admin-hero" aria-labelledby="admin-dashboard-title">
        <div className="admin-hero__copy">
          <span className="admin-role-pill">{roleLabel}</span>
          <h1 id="admin-dashboard-title">Welcome back,<br /><span>{displayName}</span></h1>
          <p>Manage your vaccination programs, centers, staff, and operations from a single, secure platform.</p>
        </div>
        <img className="admin-hero__illustration" src={approvedHeroIllustration} alt="" aria-hidden="true" />
        <section className="admin-profile card" aria-labelledby="admin-profile-title">
          <div className="admin-profile__heading">
            <span className="admin-icon-tile" aria-hidden="true"><i className="bi bi-person-fill" /></span>
            <h2 id="admin-profile-title">Account Profile</h2>
          </div>
          <dl>
            <div><dt><i className="bi bi-person" aria-hidden="true" />Full Name</dt><dd>{user?.fullName || 'Not provided'}</dd></div>
            <div><dt><i className="bi bi-envelope" aria-hidden="true" />Email</dt><dd>{user?.email || 'Not provided'}</dd></div>
            <div><dt><i className="bi bi-telephone" aria-hidden="true" />Phone Number</dt><dd>{user?.phoneNumber || 'Not provided'}</dd></div>
            <div><dt><i className="bi bi-shield-check" aria-hidden="true" />Portal Role</dt><dd><span className="admin-profile__role">{roleLabel}</span></dd></div>
          </dl>
        </section>
      </section>

      <section className="admin-dashboard__section" aria-labelledby="admin-overview-title">
        <div className="admin-section-heading">
          <h2 id="admin-overview-title">Operational Overview</h2>
          <p>Key insights across your vaccination operations.</p>
        </div>
        <div className="admin-metric-grid" aria-live="polite">
          {overviewCards.map((card) => <MetricCard key={card.label} {...card} loading={metrics.loading} />)}
        </div>
      </section>

      <section className="admin-dashboard__section" aria-labelledby="admin-administration-title">
        <div className="admin-section-heading">
          <h2 id="admin-administration-title">Administration</h2>
          <p>Manage portal access and administrative functions.</p>
        </div>
        <div className="admin-administration-grid">
          <ActionCard
            title="Staff Account Requests"
            description="Review pending staff account requests and assign authoritative roles."
            to="/app/admin/staff-requests"
            icon="bi-people-fill"
            decoration="bi-person-vcard"
            featured
          />
        </div>
      </section>

      <section className="admin-dashboard__section" aria-labelledby="admin-management-title">
        <div className="admin-section-heading">
          <h2 id="admin-management-title">Operational Management</h2>
          <p>Oversee daily vaccination operations and resources.</p>
        </div>
        <div className="admin-management-grid">
          {operationalLinks.map((card) => <ActionCard key={card.title} {...card} />)}
          {isSuperAdmin && (
            <ActionCard
              title="Screening Review Queue"
              description="Review citizen pre-vaccination screenings and record clinical decisions."
              to="/app/staff/screening/reviews"
              icon="bi-clipboard2-pulse"
              decoration="bi-clipboard-check"
            />
          )}
        </div>
      </section>
      </div>
    </>
  );
}
