import React from 'react';

export default function Header({ activeView = 'overview', onNavigate }) {
  return (
    <header>
      <nav className="navbar navbar-expand-lg navbar-dark bg-primary shadow-sm" aria-label="Main Navigation">
        <div className="container">
          <span className="navbar-brand fw-bold me-4">
            Web-Based Vaccination Portal
          </span>

          <div className="d-flex align-items-center flex-grow-1">
            <ul className="navbar-nav me-auto mb-2 mb-lg-0 flex-row gap-2">
              <li className="nav-item">
                <button
                  type="button"
                  className={`btn btn-sm ${activeView === 'overview' ? 'btn-light text-primary fw-bold' : 'btn-outline-light border-0'}`}
                  onClick={() => onNavigate && onNavigate('overview')}
                >
                  Overview
                </button>
              </li>
              <li className="nav-item">
                <button
                  type="button"
                  className={`btn btn-sm ${activeView === 'campaigns' ? 'btn-light text-primary fw-bold' : 'btn-outline-light border-0'}`}
                  onClick={() => onNavigate && onNavigate('campaigns')}
                >
                  Campaigns
                </button>
              </li>
              <li className="nav-item">
                <button
                  type="button"
                  className={`btn btn-sm ${activeView === 'center-operations' ? 'btn-light text-primary fw-bold' : 'btn-outline-light border-0'}`}
                  onClick={() => onNavigate && onNavigate('center-operations')}
                >
                  Center Operations
                </button>
              </li>
            </ul>

            <span className="navbar-text text-white-50 small d-none d-md-inline">
              MF-02 Campaign Management
            </span>
          </div>
        </div>
      </nav>
    </header>
  );
}
