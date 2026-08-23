import React, { useState } from 'react';
import Header from '../components/common/Header';
import Footer from '../components/common/Footer';
import CampaignManagementPage from '../features/campaign-management/pages/CampaignManagementPage';
import CenterOperationsPage from '../features/center-operations/pages/CenterOperationsPage';
import SessionsPage from '../features/session-management/pages/SessionsPage';

export default function App() {
  const [activeView, setActiveView] = useState('campaigns');

  return (
    <div className="d-flex flex-column min-vh-100">
      <Header activeView={activeView} onNavigate={setActiveView} />

      <main className="container my-4 flex-grow-1">
        {activeView === 'campaigns' && <CampaignManagementPage />}
        {activeView === 'center-operations' && <CenterOperationsPage />}
        {activeView === 'sessions' && <SessionsPage />}

        {activeView === 'overview' && (
          <section className="row justify-content-center">
            <div className="col-md-10 col-lg-8">
              <div className="card shadow-sm border-0">
                <div className="card-body p-4 p-md-5">
                  <h1 className="h2 text-primary mb-3">
                    Web-Based Vaccination Portal
                  </h1>
                  <p className="lead text-secondary">
                    Shared Frontend Application Foundation
                  </p>
                  <hr className="my-4" />

                  <div className="alert alert-info border-0 shadow-sm mb-4" role="status">
                    <h2 className="h5 alert-heading mb-2">Foundation Status</h2>
                    <p className="mb-0">
                      The shared frontend application foundation has been initialized successfully using React, Vite, and Bootstrap 5.
                    </p>
                  </div>

                  <div className="p-3 bg-light rounded border">
                    <h3 className="h6 text-dark fw-bold mb-2">Development Guidelines</h3>
                    <p className="text-muted small mb-0">
                      Business modules (MF-01 through MF-05) and supporting services are integrated incrementally through separate reviewed feature branches in accordance with canonical project specifications.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </section>
        )}
      </main>

      <Footer />
    </div>
  );
}
