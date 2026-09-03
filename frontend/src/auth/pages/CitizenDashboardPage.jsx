import React from 'react';
import { useAuth } from '../context/AuthContext';
import DashboardProfileCard from '../components/DashboardProfileCard';
import DashboardServiceCard from '../components/DashboardServiceCard';

const CITIZEN_SERVICES = [
  ['Pre-Vaccination Screening', 'Complete screening before eligible appointments become available.', '/app/screening'],
  ['My Eligibility', 'Review your vaccination eligibility status.'],
  ['My Appointments', 'View and manage your vaccination appointments.'],
  ['Screening History', 'Review previously submitted screening information.'],
  ['Vaccination History', 'View your recorded vaccination history.'],
];

export default function CitizenDashboardPage() {
  const { user } = useAuth();

  return (
    <section aria-labelledby="citizen-dashboard-title">
      <div className="mb-4">
        <span className="badge text-bg-primary mb-2">Citizen Dashboard</span>
        <h1 className="display-6 fw-semibold mb-2" id="citizen-dashboard-title">
          Welcome, {user?.fullName || user?.email}
        </h1>
        <p className="text-muted mb-0">Your vaccination account and services in one place.</p>
      </div>

      <div className="row g-4 mb-5">
        <div className="col-12 col-xl-7"><DashboardProfileCard user={user} /></div>
        <div className="col-12 col-xl-5">
          <div className="card border-0 shadow-sm h-100 bg-primary text-white">
            <div className="card-body p-4">
              <h2 className="h4">My Vaccination Portal</h2>
              <p className="mb-0 opacity-75">Citizen self-service features will appear here as each protected workflow becomes available.</p>
            </div>
          </div>
        </div>
      </div>

      <h2 className="h3 mb-3">Citizen Services</h2>
      <div className="row g-4">
        {CITIZEN_SERVICES.map(([title, description, to]) => (
          <DashboardServiceCard key={title} title={title} description={description} to={to} />
        ))}
      </div>
    </section>
  );
}
