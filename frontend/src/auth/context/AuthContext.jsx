/* eslint-disable react-refresh/only-export-components */
import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { getToken, setToken, clearToken, onUnauthorized } from '../authStorage';
import { loginApi, getCurrentUserApi } from '../api/authApi';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isInitializing, setIsInitializing] = useState(true);
  const [initializationError, setInitializationError] = useState(null);
  const [loginError, setLoginError] = useState(null);

  const authGenerationRef = useRef(0);

  const invalidatePendingAuth = useCallback(() => {
    authGenerationRef.current += 1;
    return authGenerationRef.current;
  }, []);

  const bootstrapAuth = useCallback(async () => {
    const currentGeneration = invalidatePendingAuth();
    const token = getToken();

    if (!token) {
      if (currentGeneration === authGenerationRef.current) {
        setUser(null);
        setIsAuthenticated(false);
        setInitializationError(null);
        setIsInitializing(false);
      }
      return;
    }

    try {
      const userProfile = await getCurrentUserApi();
      if (currentGeneration === authGenerationRef.current) {
        setUser(userProfile);
        setIsAuthenticated(true);
        setInitializationError(null);
      }
    } catch (err) {
      if (currentGeneration === authGenerationRef.current) {
        if (err?.status === 401) {
          // Token is expired or invalid -> clear stored token and show login
          clearToken();
          setUser(null);
          setIsAuthenticated(false);
          setInitializationError(null);
        } else {
          // Transient network or server outage -> retain token and display retry state
          setUser(null);
          setIsAuthenticated(false);
          setInitializationError(
            err?.message || 'Authentication service is temporarily unavailable. Please check your connection and retry.'
          );
        }
      }
    } finally {
      if (currentGeneration === authGenerationRef.current) {
        setIsInitializing(false);
      }
    }
  }, [invalidatePendingAuth]);

  useEffect(() => {
    bootstrapAuth();

    // Subscribe to global 401 unauthorized notifications from apiClient
    const unsubscribe = onUnauthorized(() => {
      invalidatePendingAuth();
      clearToken();
      setUser(null);
      setIsAuthenticated(false);
      setLoginError(null);
    });

    return () => {
      invalidatePendingAuth();
      unsubscribe();
    };
  }, [bootstrapAuth, invalidatePendingAuth]);

  const login = useCallback(async (email, password) => {
    setLoginError(null);
    try {
      const data = await loginApi(email, password);
      if (data?.token && data?.user) {
        const persisted = setToken(data.token);
        if (!persisted) {
          clearToken();
          setUser(null);
          setIsAuthenticated(false);
          const storageError = 'Unable to start your browser session. Please check browser storage settings and try again.';
          setLoginError(storageError);
          return { success: false, error: storageError };
        }
        invalidatePendingAuth();
        setUser(data.user);
        setIsAuthenticated(true);
        setInitializationError(null);
        return { success: true };
      } else {
        throw new Error('Authentication response was incomplete');
      }
    } catch (err) {
      const message = err?.message || 'Invalid email or password';
      setLoginError(message);
      return { success: false, error: message };
    }
  }, [invalidatePendingAuth]);

  const logout = useCallback(() => {
    invalidatePendingAuth();
    clearToken();
    setUser(null);
    setIsAuthenticated(false);
    setLoginError(null);
    setInitializationError(null);
  }, [invalidatePendingAuth]);

  const retryBootstrap = useCallback(() => {
    setInitializationError(null);
    setIsInitializing(true);
    bootstrapAuth();
  }, [bootstrapAuth]);

  const clearLoginError = useCallback(() => {
    setLoginError(null);
  }, []);

  const value = {
    user,
    isAuthenticated,
    isInitializing,
    initializationError,
    loginError,
    login,
    logout,
    retryBootstrap,
    clearLoginError,
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
