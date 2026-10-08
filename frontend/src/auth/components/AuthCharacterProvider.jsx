import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { AuthCharacterContext } from './authCharacterContext';
import AuthCharacterGroup from './AuthCharacterGroup';
import './auth-characters.css';

export default function AuthCharacterProvider({ children }) {
  const { pathname } = useLocation();
  const [privacy, setPrivacy] = useState({});
  const [activity, setActivity] = useState({ path: '', mode: 'idle' });
  const [celebrating, setCelebrating] = useState(false);
  const failureTimer = useRef();
  const successTimer = useRef();
  const reportPrivacy = useCallback((id, active) => {
    setPrivacy((previous) => {
      if (!active) {
        if (!previous[id]) return previous;
        const next = { ...previous };
        delete next[id];
        return next;
      }
      if (previous[id] === pathname) return previous;
      return { ...previous, [id]: pathname };
    });
  }, [pathname]);
  const reportSubmission = useCallback((active) => {
    clearTimeout(failureTimer.current);
    setActivity({ path: pathname, mode: active ? 'submitting' : 'idle' });
  }, [pathname]);
  const reportResult = useCallback((success) => {
    clearTimeout(failureTimer.current);
    if (success) {
      setCelebrating(true);
      clearTimeout(successTimer.current);
      successTimer.current = setTimeout(() => setCelebrating(false), 600);
      setActivity({ path: pathname, mode: 'success' });
    } else {
      setActivity({ path: pathname, mode: 'failure' });
      failureTimer.current = setTimeout(() => setActivity((previous) => (
        previous.path === pathname ? { path: pathname, mode: 'idle' } : previous
      )), 1000);
    }
  }, [pathname]);
  useEffect(() => {
    clearTimeout(failureTimer.current);
    setActivity({ path: pathname, mode: 'idle' });
    setPrivacy({});
  }, [pathname]);
  useEffect(() => () => {
    clearTimeout(failureTimer.current);
    clearTimeout(successTimer.current);
  }, []);

  const activeMode = activity.path === pathname ? activity.mode : 'idle';
  const privacyActive = Object.values(privacy).includes(pathname);
  const mode = ['success', 'failure', 'submitting'].includes(activeMode)
    ? activeMode : privacyActive ? 'privacy' : 'idle';
  const value = useMemo(() => ({ mode, privacyActive, reportPrivacy, reportSubmission, reportResult }),
    [mode, privacyActive, reportPrivacy, reportSubmission, reportResult]);
  return (
    <AuthCharacterContext.Provider value={value}>
      {children}
      {celebrating && (
        <div className="auth-character-celebration" aria-hidden="true" data-testid="character-celebration">
          <AuthCharacterGroup presentation mode="success" />
          <span>Welcome!</span>
        </div>
      )}
    </AuthCharacterContext.Provider>
  );
}
