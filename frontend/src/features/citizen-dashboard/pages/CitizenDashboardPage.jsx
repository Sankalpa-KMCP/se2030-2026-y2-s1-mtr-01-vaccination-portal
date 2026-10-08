import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../../auth/context/AuthContext';
import RoleDashboardHero from '../../../auth/components/RoleDashboardHero';
import VaccinationJourneyStepper from '../components/VaccinationJourneyStepper';
import ScreeningOverview from '../components/ScreeningOverview';
import AppointmentOverview from '../components/AppointmentOverview';
import QuickActions from '../components/QuickActions';
import AvailableCampaigns from '../components/AvailableCampaigns';
import RecentActivity from '../components/RecentActivity';
import BottomHealthBanner from '../components/BottomHealthBanner';
import useCitizenDashboardData from '../hooks/useCitizenDashboardData';
import '../citizen-dashboard.css';
import '../../../auth/styles/admin-dashboard.css';

function formatStatus(status) {
  if (!status) return 'Active';
  return status.toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, (character) => character.toUpperCase());
}

export default function CitizenDashboardPage() {
  const { user } = useAuth();
  const dashboardData = useCitizenDashboardData(user?.id);

  return (
    <div className="admin-dashboard role-dashboard role-dashboard--citizen citizen-dashboard">
      <RoleDashboardHero
        user={user}
        titleId="citizen-dashboard-title"
        roleLabel="Citizen"
        description="Track your vaccination journey, complete campaign screening, and manage appointments from one secure dashboard."
        profileDetails={[
          { label: 'Citizen ID', value: user?.id != null ? `#${user.id}` : 'Not provided', icon: 'bi-person-vcard' },
          { label: 'Account Status', value: formatStatus(user?.accountStatus), icon: 'bi-check-circle' },
        ]}
        profileLink={{ to: '/app/citizen/profile', label: 'View Profile' }}
      />

      {user?.accountStatus === 'PENDING_VERIFICATION' && (
        <section className="citizen-dashboard__verification" aria-label="Email verification required">
          <div>
            <h2>Verify your email to activate your account</h2>
            <p>Your email address has not been verified yet. Enter your verification code to complete account activation.</p>
          </div>
          <Link className="btn btn-primary" to="/app/citizen/profile">Verify Email</Link>
        </section>
      )}

      <section className="admin-dashboard__section" aria-labelledby="citizen-overview-title">
        <div className="admin-section-heading">
          <h2 id="citizen-overview-title">Vaccination Overview</h2>
          <p>Your current journey, eligibility information, and appointment status.</p>
        </div>
        <div className="citizen-dashboard__status-row" aria-label="Vaccination status overview">
          <VaccinationJourneyStepper dashboardData={dashboardData} />
          <ScreeningOverview resource={dashboardData.screening} />
          <AppointmentOverview resource={dashboardData.appointments} />
        </div>
      </section>

      <QuickActions />
      <AvailableCampaigns dashboardData={dashboardData} />

      <section className="citizen-dashboard__bottom-row" aria-label="Recent activity and healthcare awareness">
        <RecentActivity dashboardData={dashboardData} />
        <BottomHealthBanner />
      </section>
    </div>
  );
}
