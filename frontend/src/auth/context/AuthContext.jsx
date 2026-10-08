/* eslint-disable react-refresh/only-export-components */
import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { onUnauthorized, notifyUnauthorized, startAuthSession, endAuthSession, getToken, getSessionPrincipalId, normalizePrincipalId } from '../../shared/auth/authStorage';
import { bootstrapSession } from '../../shared/api/apiClient';
import { loginApi, getCurrentUserApi, logoutApi } from '../api/authApi';
import { ADMIN_ROLE, CITIZEN_ROLE, STAFF_DASHBOARD_ROLES, SUPER_ADMIN_ROLE } from '../constants/authRoles';
import { hasRole } from '../utils/roleUtils';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isInitializing, setIsInitializing] = useState(true);
  const [initializationError, setInitializationError] = useState(null);
  const [loginError, setLoginError] = useState(null);
  const [loginWelcome, setLoginWelcome] = useState(null);

  const authGenerationRef = useRef(0);
  const bootstrapAttemptRef = useRef(0);

  const invalidatePendingAuth = useCallback(() => {
    authGenerationRef.current += 1;
    return authGenerationRef.current;
  }, []);

  const startBootstrapAttempt = useCallback(() => {
    bootstrapAttemptRef.current += 1;
    return bootstrapAttemptRef.current;
  }, []);

  const cancelBootstrapOwnership = useCallback(() => {
    bootstrapAttemptRef.current += 1;
  }, []);

  const bootstrapAuth = useCallback(async () => {
    const currentAttempt = startBootstrapAttempt();
    const currentGeneration = invalidatePendingAuth();
    setIsInitializing(true);

    // Session bootstrap rides on the HttpOnly refresh cookie (canonical §17.1):
    // no token is ever read from browser storage. A definitive refresh failure
    // means logged out; a transport failure keeps the initialization error so
    // retryBootstrap can be used.
    let refreshedToken = null;
    try {
      refreshedToken = await bootstrapSession();
    } catch (err) {
      if (currentAttempt === bootstrapAttemptRef.current) {
        if (currentGeneration === authGenerationRef.current) {
          setUser(null);
          setIsAuthenticated(false);
          setInitializationError(
            err?.message || 'Authentication service is temporarily unavailable. Please check your connection and retry.'
          );
        }
        setIsInitializing(false);
      }
      return;
    }

    if (!refreshedToken) {
      // Definitive unauthenticated result (401 / no cookie / invalid session).
      // Only settle if this bootstrap attempt still owns the initialization lifecycle.
      if (currentAttempt === bootstrapAttemptRef.current) {
        if (currentGeneration === authGenerationRef.current || !getToken()) {
          setUser(null);
          setIsAuthenticated(false);
          setInitializationError(null);
        }
        setIsInitializing(false);
      }
      return;
    }

    // A token was returned from refresh. If the attempt was superseded while
    // refresh was in flight (e.g. by another bootstrap, login, or logout),
    // abort without mutating state or settling initialization.
    if (currentAttempt !== bootstrapAttemptRef.current || currentGeneration !== authGenerationRef.current) {
      return;
    }

    try {
      const userProfile = await getCurrentUserApi();
      if (currentAttempt === bootstrapAttemptRef.current && currentGeneration === authGenerationRef.current) {
        if (normalizePrincipalId(userProfile?.id) === null
          || normalizePrincipalId(userProfile.id) !== getSessionPrincipalId()) {
          endAuthSession();
          notifyUnauthorized();
          return;
        }
        setUser(userProfile);
        setIsAuthenticated(true);
        setInitializationError(null);
      }
    } catch (err) {
      if (currentAttempt === bootstrapAttemptRef.current && currentGeneration === authGenerationRef.current) {
        if (err?.status === 401) {
          // Refreshed access token was already rejected server-side: logged out.
          endAuthSession();
          setUser(null);
          setIsAuthenticated(false);
          setInitializationError(null);
        } else {
          // Transient network or server outage -> display retry state
          setUser(null);
          setIsAuthenticated(false);
          setInitializationError(
            err?.message || 'Authentication service is temporarily unavailable. Please check your connection and retry.'
          );
        }
      }
    } finally {
      if (currentAttempt === bootstrapAttemptRef.current) {
        setIsInitializing(false);
      }
    }
  }, [invalidatePendingAuth, startBootstrapAttempt]);

  useEffect(() => {
    bootstrapAuth();

    // Subscribe to global 401 unauthorized notifications from apiClient
    const unsubscribe = onUnauthorized(() => {
      invalidatePendingAuth();
      endAuthSession();
      setUser(null);
      setIsAuthenticated(false);
      setLoginError(null);
      setLoginWelcome(null);
    });

    return () => {
      cancelBootstrapOwnership();
      invalidatePendingAuth();
      unsubscribe();
    };
  }, [bootstrapAuth, cancelBootstrapOwnership, invalidatePendingAuth]);

  const login = useCallback(async (email, password) => {
    setLoginError(null);
    try {
      const data = await loginApi(email, password);
      if (data?.token && normalizePrincipalId(data?.user?.id) !== null) {
        // The access token lives in memory only; the refresh token rides in the
        // HttpOnly cookie set by the server (canonical §17.1).
        cancelBootstrapOwnership();
        startAuthSession(data.token, data.user.id);
        invalidatePendingAuth();
        setUser(data.user);
        setIsAuthenticated(true);
        const audience = hasRole(data.user, CITIZEN_ROLE)
          ? 'citizen'
          : (
            hasRole(data.user, ...STAFF_DASHBOARD_ROLES)
            && !hasRole(data.user, ADMIN_ROLE, SUPER_ADMIN_ROLE)
              ? 'staff'
              : null
          );
        setLoginWelcome(audience
          ? { audience, userId: normalizePrincipalId(data.user.id) }
          : null);
        setInitializationError(null);
        setIsInitializing(false);
        return { success: true, user: data.user };
      } else {
        throw new Error('Authentication response was incomplete');
      }
    } catch (err) {
      const message = err?.message || 'Invalid email or password';
      setLoginError(message);
      return { success: false, error: message };
    }
  }, [cancelBootstrapOwnership, invalidatePendingAuth]);

  const logout = useCallback(async () => {
    cancelBootstrapOwnership();
    invalidatePendingAuth();
    endAuthSession();
    try {
      await logoutApi();
    } catch {
      // Backend revocation is best-effort; local state clears regardless.
    }
    setUser(null);
    setIsAuthenticated(false);
    setLoginError(null);
    setLoginWelcome(null);
    setInitializationError(null);
    setIsInitializing(false);
  }, [cancelBootstrapOwnership, invalidatePendingAuth]);

  const retryBootstrap = useCallback(() => {
    setInitializationError(null);
    setIsInitializing(true);
    bootstrapAuth();
  }, [bootstrapAuth]);

  const clearLoginError = useCallback(() => {
    setLoginError(null);
  }, []);

  const consumeLoginWelcome = useCallback(() => {
    setLoginWelcome(null);
  }, []);

  const updateCurrentUserProfile = useCallback((profile) => {
    setUser((currentUser) => {
      if (!currentUser || !profile) return currentUser;
      if (profile.id != null && currentUser.id != null && String(profile.id) !== String(currentUser.id)) {
        return currentUser;
      }
      return {
        ...currentUser,
        fullName: profile.fullName,
        phoneNumber: profile.phoneNumber,
        ...(profile.accountStatus ? { accountStatus: profile.accountStatus } : {}),
      };
    });
  }, []);

  const value = {
    user,
    isAuthenticated,
    isInitializing,
    initializationError,
    loginError,
    loginWelcome,
    login,
    logout,
    retryBootstrap,
    clearLoginError,
    consumeLoginWelcome,
    updateCurrentUserProfile,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
