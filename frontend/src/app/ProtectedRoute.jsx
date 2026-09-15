import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/context/AuthContext';
import { hasRole } from '../auth/utils/roleUtils';

/**
 * Protects an application route with the existing authentication context and,
 * when provided, an allow-list of canonical roles.
 */
export default function ProtectedRoute({ allowedRoles = null, children = null }) {
  const { isAuthenticated, user } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: location.pathname }}
      />
    );
  }

  if (
    Array.isArray(allowedRoles)
    && allowedRoles.length > 0
    && !hasRole(user, ...allowedRoles)
  ) {
    return (
      <Navigate
        to="/app/not-authorized"
        replace
        state={{ from: location.pathname }}
      />
    );
  }

  return children || <Outlet />;
}
