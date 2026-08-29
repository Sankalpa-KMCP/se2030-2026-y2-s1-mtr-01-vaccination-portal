import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '../auth/context/AuthContext';
import PortalRoutes from './routes';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <PortalRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}
