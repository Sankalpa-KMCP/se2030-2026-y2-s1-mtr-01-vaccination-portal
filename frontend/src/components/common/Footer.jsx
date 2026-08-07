import React from 'react';

export default function Footer() {
  return (
    <footer className="bg-light text-center text-muted py-3 mt-auto border-top">
      <div className="container">
        <small>
          Web-Based Vaccination Portal &copy; {new Date().getFullYear()} — SE2030 Project Baseline
        </small>
      </div>
    </footer>
  );
}
