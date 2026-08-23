/* eslint-disable react-refresh/only-export-components */
import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { getToken, setToken, clearToken, onUnauthorized } from '../authStorage';
import { loginApi, getCurrentUserApi } from '../api/authApi';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isInitializing, setIsInitializing] = useState(true);
  const [initializationError, setInitializationError] = useState(null);
  const [loginError, setLoginError] = useState(null);

  const bootstrapAuth = useCallback(async () => {
    const token = getToken();
    if (!token) {
      setUser(null);
      setIsAuthenticated(false);
      setInitializationError(null);
      setIsInitializing(false);
      return;
    }

    try {
      const userProfile = await getCurrentUserApi();
      setUser(userProfile);
      setIsAuthenticated(true);
      setInitializationError(null);
    } catch (err) {
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
    } finally {
      setIsInitializing(false);
    }
  }, []);

  useEffect(() => {
    bootstrapAuth();

    // Subscribe to global 401 unauthorized notifications from apiClient
    const unsubscribe = onUnauthorized(() => {
      clearToken();
      setUser(null);
      setIsAuthenticated(false);
      setLoginError(null);
    });

    return () => {
      unsubscribe();
    };
  }, [bootstrapAuth]);

  const login = useCallback(async (email, password) => {
    setLoginError(null);
    try {
      const data = await loginApi(email, password);
      if (data?.token && data?.user) {
        setToken(data.token);
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
  }, []);

  const logout = useCallback(() => {
    clearToken();
    setUser(null);
    setIsAuthenticated(false);
    setLoginError(null);
    setInitializationError(null);
  }, []);

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
