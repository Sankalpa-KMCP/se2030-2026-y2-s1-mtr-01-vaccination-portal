import React from 'react';
import { useAuth } from '../context/AuthContext';
import DashboardProfileCard from '../components/DashboardProfileCard';
import DashboardServiceCard from '../components/DashboardServiceCard';
import { SUPER_ADMIN_ROLE } from '../constants/authRoles';
import { hasRole } from '../utils/roleUtils';

export default function AdminDashboardPage() {
  const { user } = useAuth();
  const isSuperAdmin = hasRole(user, SUPER_ADMIN_ROLE);

  return (
    <section aria-labelledby="admin-dashboard-title">
      <div className="mb-4">
        <span className={`badge ${isSuperAdmin ? 'text-bg-dark' : 'text-bg-danger'} mb-2`}>
          {isSuperAdmin ? 'Super Administrator' : 'Administrator'}
        </span>
        <h1 className="display-6 fw-semibold mb-2" id="admin-dashboard-title">
          Welcome, {user?.fullName || user?.email}
        </h1>
        <p className="text-muted mb-0">
          {isSuperAdmin
            ? 'Super administrative control: manage portal administration and oversee cross-module operational tools.'
            : 'Review account access and administer shared portal configuration.'}
        </p>
      </div>

      <div className="mb-5"><DashboardProfileCard user={user} showRoles /></div>

      <h2 className="h3 mb-3">Administration</h2>
      <div className="row g-4 mb-4">
        <DashboardServiceCard
          title="Staff Account Requests"
          description="Review pending staff account requests and assign authoritative roles."
          to="/app/admin/staff-requests"
        />
      </div>

      {isSuperAdmin && (
        <>
          <h2 className="h3 mb-3">Operational Management</h2>
          <div className="row g-4">
            <DashboardServiceCard
              title="Campaign Management"
              description="Configure, schedule, and oversee vaccination campaigns."
              to="/app/campaigns"
            />
            <DashboardServiceCard
              title="Center Operations"
              description="Manage vaccination centers, operating hours, and resource allocations."
              to="/app/center-operations"
            />
            <DashboardServiceCard
              title="Session Management"
              description="Schedule sessions, manage time slots, and monitor booking capacity."
              to="/app/sessions"
            />
            <DashboardServiceCard
              title="Operational Analytics"
              description="Review operational throughput and vaccination metrics."
              to="/app/analytics"
            />
            <DashboardServiceCard
              title="Screening Review Queue"
              description="Review citizen pre-vaccination screenings and record clinical decisions."
              to="/app/staff/screening/reviews"
            />
          </div>
        </>
      )}
    </section>
  );
}
