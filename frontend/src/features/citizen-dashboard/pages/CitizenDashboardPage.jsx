import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../../auth/context/AuthContext';
import CitizenWelcome from '../components/CitizenWelcome';
import VaccinationJourneyStepper from '../components/VaccinationJourneyStepper';
import ScreeningOverview from '../components/ScreeningOverview';
import AppointmentOverview from '../components/AppointmentOverview';
import QuickActions from '../components/QuickActions';
import AvailableCampaigns from '../components/AvailableCampaigns';
import RecentActivity from '../components/RecentActivity';
import BottomHealthBanner from '../components/BottomHealthBanner';
import useCitizenDashboardData from '../hooks/useCitizenDashboardData';
import '../citizen-dashboard.css';

export default function CitizenDashboardPage() {
  const { user } = useAuth();
  const dashboardData = useCitizenDashboardData(user?.id);

  return (
    <section className="citizen-dashboard" aria-labelledby="citizen-dashboard-title">
      {/* 1. Greeting + Citizen Context + Healthcare Visual Banner */}
      <CitizenWelcome user={user} />

      {user?.accountStatus === 'PENDING_VERIFICATION' && (
        <section className="citizen-dashboard__verification" aria-label="Email verification required">
          <div>
            <h2>Verify your email to activate your account</h2>
            <p>Your email address has not been verified yet. Enter your verification code to complete account activation.</p>
          </div>
          <Link className="btn btn-primary" to="/app/citizen/profile">Verify Email</Link>
        </section>
      )}

      {/* 2. Main Status Row: Journey Card (45%) + Eligibility Card (27%) + Appointment Card (28%) */}
      <section className="citizen-dashboard__status-row" aria-label="Vaccination status overview">
        <VaccinationJourneyStepper dashboardData={dashboardData} />
        <ScreeningOverview resource={dashboardData.screening} />
        <AppointmentOverview resource={dashboardData.appointments} />
      </section>

      {/* 3. Quick Actions: Full-Width White Card Row */}
      <QuickActions />

      {/* 4. Available Campaigns Section */}
      <AvailableCampaigns dashboardData={dashboardData} />

      {/* 5. Bottom Row: Recent Activity (Left) + Bottom Health Banner (Right) */}
      <section className="citizen-dashboard__bottom-row" aria-label="Recent activity and healthcare awareness">
        <RecentActivity dashboardData={dashboardData} />
        <BottomHealthBanner />
      </section>
    </section>
  );
}
