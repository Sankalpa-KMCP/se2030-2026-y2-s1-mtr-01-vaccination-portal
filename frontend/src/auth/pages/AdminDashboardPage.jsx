import React from 'react';
import { useAuth } from '../context/AuthContext';
import DashboardProfileCard from '../components/DashboardProfileCard';
import DashboardServiceCard from '../components/DashboardServiceCard';

export default function AdminDashboardPage() {
  const { user } = useAuth();

  return (
    <section aria-labelledby="admin-dashboard-title">
      <div className="mb-4">
        <span className="badge text-bg-danger mb-2">Administrator</span>
        <h1 className="display-6 fw-semibold mb-2" id="admin-dashboard-title">
          Welcome, {user?.fullName || user?.email}
        </h1>
        <p className="text-muted mb-0">Review account access and administer shared portal configuration.</p>
      </div>

      <div className="mb-5"><DashboardProfileCard user={user} showRoles /></div>

      <h2 className="h3 mb-3">Administration</h2>
      <div className="row g-4">
        <DashboardServiceCard
          title="Staff Account Requests"
          description="Review pending staff account requests and assign authoritative roles."
          to="/app/admin/staff-requests"
        />
        <DashboardServiceCard
          title="Screening Questions"
          description="Manage the shared pre-vaccination screening question catalogue."
          to="/app/screening/questions"
        />
        <DashboardServiceCard
          title="Screening Questionnaires"
          description="Build and manage pre-vaccination screening questionnaires."
          to="/app/screening/questionnaires"
        />
      </div>
    </section>
  );
}
