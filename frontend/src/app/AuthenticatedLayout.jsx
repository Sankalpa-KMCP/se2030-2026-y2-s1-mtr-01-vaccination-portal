import React from 'react';
import { Outlet } from 'react-router-dom';
import Header from '../components/common/Header';
import Footer from '../components/common/Footer';

export default function AuthenticatedLayout({ onLogout }) {
  return (
    <div className="d-flex flex-column min-vh-100">
      <Header onLogout={onLogout} />
      <main className="container my-4 flex-grow-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
