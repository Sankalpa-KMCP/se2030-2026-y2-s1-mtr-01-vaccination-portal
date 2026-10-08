import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '../auth/context/AuthContext';
import PortalRoutes from './routes';
import AssistantWidget from '../features/assistant/components/AssistantWidget';
import AuthCharacterProvider from '../auth/components/AuthCharacterProvider';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AuthCharacterProvider>
          <PortalRoutes />
          <AssistantWidget />
        </AuthCharacterProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
