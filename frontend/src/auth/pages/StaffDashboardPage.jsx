import React, { useCallback } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import RoleDashboardHero from '../components/RoleDashboardHero';
import PostLoginGreeting from '../components/PostLoginGreeting';
import { getRoleLabel } from '../constants/authRoles';
import {
  canViewAnalytics,
  canViewCampaigns,
  canViewCenters,
  canViewSessions,
  hasRole,
} from '../utils/roleUtils';
import VaccinatorDashboardSection from '../../features/vaccination-administration/components/VaccinatorDashboardSection';
import HcoDashboardView from '../../features/screening/components/dashboard/HcoDashboardView';
import '../styles/admin-dashboard.css';

function StaffActionCard({ title, description, to, icon, decoration }) {
  return (
    <Link className="admin-action-card card" to={to} aria-label={`Open ${title}`}>
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

export default function StaffDashboardPage() {
  const { user, loginWelcome, consumeLoginWelcome } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const showLoginGreeting = loginWelcome?.audience === 'staff'
    && loginWelcome.userId != null
    && String(loginWelcome.userId) === String(user?.id);
  const dismissLoginGreeting = useCallback(() => {
    consumeLoginWelcome?.();
    navigate(location.pathname, { replace: true, state: null });
  }, [consumeLoginWelcome, location.pathname, navigate]);
  const isVaccinator = hasRole(user, 'ROLE_VACCINATOR');
  const canManageCheckIn = hasRole(user, 'ROLE_CENTER_MANAGER', 'ROLE_ADMIN');
  const isInventoryOfficer = hasRole(user, 'ROLE_INVENTORY_OFFICER');
  const isHealthcareOfficer = hasRole(user, 'ROLE_HEALTHCARE_OFFICER')
    && !isVaccinator
    && !canManageCheckIn
    && !isInventoryOfficer;
  const roleLabel = (user?.roles || []).map(getRoleLabel).join(' · ') || 'Staff Member';

  const services = [
    hasRole(user, 'ROLE_VACCINATOR') && {
      title: 'Vaccination Drafts',
      description: 'Create, review, edit, and delete unfinalized vaccination drafts.',
      to: '/app/staff/vaccination-drafts',
      icon: 'bi-clipboard2-pulse',
      decoration: 'bi-file-earmark-medical',
    },
    hasRole(user, 'ROLE_HEALTHCARE_OFFICER') && {
      title: 'Screening Review',
      description: 'Review citizen screenings, request corrections, and record eligibility decisions.',
      to: '/app/staff/screening/reviews',
      icon: 'bi-clipboard2-check',
      decoration: 'bi-person-check',
    },
    hasRole(user, 'ROLE_HEALTHCARE_OFFICER') && {
      title: 'Screening Questions',
      description: 'Create and maintain the clinical question and answer-option catalogue.',
      to: '/app/screening/questions',
      icon: 'bi-ui-checks-grid',
      decoration: 'bi-question-circle',
    },
    hasRole(user, 'ROLE_HEALTHCARE_OFFICER') && {
      title: 'Screening Questionnaires',
      description: 'Author, publish, and manage questionnaire versions and eligibility rules.',
      to: '/app/screening/questionnaires',
      icon: 'bi-journal-medical',
      decoration: 'bi-clipboard-data',
    },
    canViewCampaigns(user) && {
      title: 'Campaigns',
      description: 'View vaccination campaigns available to your assigned role.',
      to: '/app/campaigns',
      icon: 'bi-megaphone',
      decoration: 'bi-calendar3',
    },
    canViewCenters(user) && {
      title: 'Center Operations',
      description: 'Access vaccination-center operational information.',
      to: '/app/center-operations',
      icon: 'bi-hospital',
      decoration: 'bi-building',
    },
    canViewSessions(user) && {
      title: 'Sessions',
      description: 'View and manage vaccination sessions permitted for your role.',
      to: '/app/sessions',
      icon: 'bi-calendar-week',
      decoration: 'bi-clock-history',
    },
    canViewAnalytics(user) && {
      title: 'Operational Analytics',
      description: 'Review operational vaccination programme analytics.',
      to: '/app/analytics',
      icon: 'bi-bar-chart-line',
      decoration: 'bi-pie-chart',
    },
    (hasRole(user, 'ROLE_INVENTORY_OFFICER') || hasRole(user, 'ROLE_ADMIN')) && {
      title: 'Inventory & Batches',
      description: 'Manage MF-04 manufacturers, vaccine batches, and center stock.',
      to: '/app/inventory',
      icon: 'bi-box-seam',
      decoration: 'bi-capsule',
    },
    canManageCheckIn && {
      title: 'Check-In & Queue Management',
      description: 'Process arrivals, manage overrides, record no-shows, and monitor clinic flow.',
      to: '/app/checkin/queue',
      icon: 'bi-people',
      decoration: 'bi-person-check',
    },
    (!canManageCheckIn && isVaccinator) && {
      title: 'Check-In Queue',
      description: 'Load the checked-in queue for your active session slot.',
      to: '/app/checkin/queue',
      icon: 'bi-person-lines-fill',
      decoration: 'bi-list-check',
    },
  ].filter(Boolean);

  return (
    <>
      {showLoginGreeting && (
        <PostLoginGreeting
          audience="staff"
          name={user?.fullName?.trim() || user?.email || 'Staff Member'}
          onComplete={dismissLoginGreeting}
        />
      )}
      <div
        className="admin-dashboard role-dashboard role-dashboard--staff"
        inert={showLoginGreeting ? true : undefined}
      >
      <RoleDashboardHero
        user={user}
        titleId="staff-dashboard-title"
        roleLabel={roleLabel}
        description="Access the operational tools assigned to your authoritative staff role from one secure workspace."
      />

      {isHealthcareOfficer ? (
        <section className="admin-dashboard__section" aria-label="Healthcare officer workspace">
          <HcoDashboardView user={user} showHeader={false} embedded />
        </section>
      ) : (
        <>
          {isVaccinator && (
            <section className="admin-dashboard__section" aria-labelledby="staff-vaccination-work-title">
              <div className="admin-section-heading">
                <h2 id="staff-vaccination-work-title">Today&apos;s Vaccination Work</h2>
                <p>Manage checked-in citizens, active drafts, and approved correction requests.</p>
              </div>
              <VaccinatorDashboardSection showHeader={false} />
            </section>
          )}

          <section className="admin-dashboard__section" aria-labelledby="staff-areas-title">
            <div className="admin-section-heading">
              <h2 id="staff-areas-title">Available Portal Areas</h2>
              <p>Your available tools reflect your assigned staff role.</p>
            </div>
            {services.length > 0 ? (
              <div className="admin-management-grid">
                {services.map((service) => <StaffActionCard key={service.title} {...service} />)}
              </div>
            ) : (
              <div className="alert alert-info">No additional operational modules are currently available for your role.</div>
            )}
          </section>
        </>
      )}
      </div>
    </>
  );
}
