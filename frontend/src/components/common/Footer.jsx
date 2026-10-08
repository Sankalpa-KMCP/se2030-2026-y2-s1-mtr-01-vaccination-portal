import React from 'react';

export default function Footer() {
  return (
    <footer className="app-footer text-center py-3 mt-auto">
      <div className="container">
        <small>
          Web-Based Vaccination Portal &copy; {new Date().getFullYear()} — SE2030 Project Baseline
        </small>
      </div>
    </footer>
  );
}
