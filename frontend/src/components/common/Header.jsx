import React from 'react';
import { Link, NavLink } from 'react-router-dom';

export default function Header() {
  return (
    <header>
      <nav className="navbar navbar-expand-lg navbar-dark bg-primary shadow-sm" aria-label="Main Navigation">
        <div className="container">
          <Link to="/" className="navbar-brand fw-bold text-decoration-none text-white">
            Web-Based Vaccination Portal
          </Link>
          <div className="navbar-nav ms-auto">
            <NavLink to="/" end className={({ isActive }) => `nav-link ${isActive ? 'active fw-bold' : ''}`}>
              Home
            </NavLink>
            <NavLink to="/appointments/book" className={({ isActive }) => `nav-link ${isActive ? 'active fw-bold' : ''}`}>
              Book Appointment
            </NavLink>
            <NavLink to="/appointments/mine" className={({ isActive }) => `nav-link ${isActive ? 'active fw-bold' : ''}`}>
              My Appointments
            </NavLink>
          </div>
        </div>
      </nav>
    </header>
  );
}
