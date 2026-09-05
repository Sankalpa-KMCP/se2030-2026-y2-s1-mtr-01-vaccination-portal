import React from 'react';
import { useAuth } from '../context/AuthContext';
import CitizenWelcome from '../../features/citizen-dashboard/components/CitizenWelcome';
import NextActionPanel from '../../features/citizen-dashboard/components/NextActionPanel';
import AppointmentOverview from '../../features/citizen-dashboard/components/AppointmentOverview';
import ScreeningOverview from '../../features/citizen-dashboard/components/ScreeningOverview';
import QuickActions from '../../features/citizen-dashboard/components/QuickActions';
import AccountSummary from '../../features/citizen-dashboard/components/AccountSummary';
import useCitizenDashboardData from '../../features/citizen-dashboard/hooks/useCitizenDashboardData';
import { getNextAction } from '../../features/citizen-dashboard/utils/citizenDashboardState';
import '../../features/citizen-dashboard/citizen-dashboard.css';

export default function CitizenDashboardPage() {
  const { user } = useAuth();
  const dashboardData = useCitizenDashboardData(user?.id);
  const nextAction = getNextAction(dashboardData);

  return (
    <section className="citizen-dashboard" aria-labelledby="citizen-dashboard-title">
      <CitizenWelcome user={user} />
      <NextActionPanel action={nextAction} />
      <div className="citizen-dashboard__status-grid">
        <AppointmentOverview resource={dashboardData.appointments} />
        <ScreeningOverview resource={dashboardData.screening} />
      </div>
      <QuickActions />
      <AccountSummary user={user} />
    </section>
  );
}
