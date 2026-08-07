import React from 'react';

export default function Header() {
  return (
    <header>
      <nav className="navbar navbar-expand-lg navbar-dark bg-primary shadow-sm" aria-label="Main Navigation">
        <div className="container">
          <span className="navbar-brand fw-bold">
            Web-Based Vaccination Portal
          </span>
          <span className="navbar-text text-white-50 fs-7">
            Shared Application Foundation
          </span>
        </div>
      </nav>
    </header>
  );
}
