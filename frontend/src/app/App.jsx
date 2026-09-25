import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '../auth/context/AuthContext';
import PortalRoutes from './routes';
import AssistantWidget from '../features/assistant/components/AssistantWidget';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <PortalRoutes />
        <AssistantWidget />
      </AuthProvider>
    </BrowserRouter>
  );
}
