import React from 'react';
import { useAuth } from '../context/AuthContext';
import DashboardProfileCard from '../components/DashboardProfileCard';
import DashboardServiceCard from '../components/DashboardServiceCard';
import {
  canViewAnalytics,
  canViewCampaigns,
  canViewCenters,
  canViewSessions,
  hasRole,
} from '../utils/roleUtils';

export default function StaffDashboardPage() {
  const { user } = useAuth();
  const services = [
    hasRole(user, 'ROLE_HEALTHCARE_OFFICER') && [
      'Screening Review',
      'Review citizen pre-vaccination screenings, request corrections, and record eligibility decisions.',
      '/app/staff/screening/reviews',
    ],
    canViewCampaigns(user) && ['Campaigns', 'View vaccination campaigns available to your assigned role.', '/app/campaigns'],
    canViewCenters(user) && ['Center Operations', 'Access vaccination-center operational information.', '/app/center-operations'],
    canViewSessions(user) && ['Sessions', 'View and manage vaccination sessions permitted for your role.', '/app/sessions'],
    canViewAnalytics(user) && ['Operational Analytics', 'Review operational vaccination programme analytics.', '/app/analytics'],
  ].filter(Boolean);

  return (
    <section aria-labelledby="staff-dashboard-title">
      <div className="mb-4">
        <span className="badge text-bg-primary mb-2">Staff Dashboard</span>
        <h1 className="display-6 fw-semibold mb-2" id="staff-dashboard-title">
          Welcome, {user?.fullName || user?.email}
        </h1>
        <p className="text-muted mb-0">Your available tools reflect your authoritative staff role.</p>
      </div>

      <div className="mb-5"><DashboardProfileCard user={user} showRoles /></div>

      <h2 className="h3 mb-3">Available Portal Areas</h2>
      {services.length > 0 ? (
        <div className="row g-4">
          {services.map(([title, description, to]) => (
            <DashboardServiceCard key={title} title={title} description={description} to={to} />
          ))}
        </div>
      ) : (
        <div className="alert alert-info">No additional operational modules are currently available for your role.</div>
      )}
    </section>
  );
}
