import React from 'react';
import { useAuth } from '../context/AuthContext';
import CitizenWelcome from '../../features/citizen-dashboard/components/CitizenWelcome';
import VaccinationJourneyStepper from '../../features/citizen-dashboard/components/VaccinationJourneyStepper';
import ScreeningOverview from '../../features/citizen-dashboard/components/ScreeningOverview';
import AppointmentOverview from '../../features/citizen-dashboard/components/AppointmentOverview';
import QuickActions from '../../features/citizen-dashboard/components/QuickActions';
import AvailableCampaigns from '../../features/citizen-dashboard/components/AvailableCampaigns';
import RecentActivity from '../../features/citizen-dashboard/components/RecentActivity';
import BottomHealthBanner from '../../features/citizen-dashboard/components/BottomHealthBanner';
import useCitizenDashboardData from '../../features/citizen-dashboard/hooks/useCitizenDashboardData';
import '../../features/citizen-dashboard/citizen-dashboard.css';

export default function CitizenDashboardPage() {
  const { user } = useAuth();
  const dashboardData = useCitizenDashboardData(user?.id);

  return (
    <section className="citizen-dashboard" aria-labelledby="citizen-dashboard-title">
      {/* 1. Greeting + Citizen Context + Healthcare Visual Banner */}
      <CitizenWelcome user={user} />

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
