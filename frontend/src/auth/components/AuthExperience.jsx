import React, { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigationType, useOutlet } from 'react-router-dom';
import { ArrowIcon, ShieldMark } from './AuthIcons';
import '../pages/LoginPage.css';

const ROUTE_META = {
  '/login': { order: 0, branch: 'Sign in', step: '01' },
  '/register/citizen': { order: 1, branch: 'Citizen registration', step: '02' },
  '/register/staff': { order: 2, branch: 'Staff registration', step: '02' },
};

const TRANSITION_MS = 480;

function useReducedMotion() {
  const [reduced, setReduced] = useState(() => (
    typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
  ));

  useEffect(() => {
    const media = window.matchMedia?.('(prefers-reduced-motion: reduce)');
    if (!media) return undefined;
    const update = () => setReduced(media.matches);
    media.addEventListener?.('change', update);
    return () => media.removeEventListener?.('change', update);
  }, []);

  return reduced;
}

function routeDirection(previousPath, nextPath, navigationType) {
  if (navigationType === 'POP') return 'backward';
  const previous = ROUTE_META[previousPath] || ROUTE_META['/login'];
  const next = ROUTE_META[nextPath] || ROUTE_META['/login'];
  return next.order < previous.order ? 'backward' : 'forward';
}

function resetRouteScroll(stage) {
  const workspace = stage?.closest('.auth-workspace');
  if (workspace) workspace.scrollTop = 0;
  document.documentElement.scrollTop = 0;
  document.body.scrollTop = 0;
}

function AuthRouteTransition() {
  const outlet = useOutlet();
  const location = useLocation();
  const navigationType = useNavigationType();
  const reducedMotion = useReducedMotion();
  const stageRef = useRef(null);
  const currentRef = useRef({ id: location.key, pathname: location.pathname, element: outlet });
  const [layers, setLayers] = useState([{ ...currentRef.current, status: 'current', direction: 'forward' }]);

  useLayoutEffect(() => {
    const previous = currentRef.current;
    if (previous.pathname === location.pathname) return undefined;

    resetRouteScroll(stageRef.current);

    const direction = routeDirection(previous.pathname, location.pathname, navigationType);
    const next = { id: location.key, pathname: location.pathname, element: outlet };
    currentRef.current = next;
    setLayers([
      { ...previous, status: 'outgoing', direction },
      { ...next, status: 'incoming', direction },
    ]);

    const timer = window.setTimeout(() => {
      setLayers([{ ...next, status: 'current', direction }]);
      const focusHeading = () => {
        resetRouteScroll(stageRef.current);
        stageRef.current?.querySelector('.auth-route-layer--current h1')?.focus({ preventScroll: true });
      };
      if (window.requestAnimationFrame) window.requestAnimationFrame(focusHeading);
      else window.setTimeout(focusHeading, 0);
    }, reducedMotion ? 20 : TRANSITION_MS);

    return () => window.clearTimeout(timer);
  }, [location.key, location.pathname, navigationType, outlet, reducedMotion]);

  return (
    <div className="auth-route-stage" ref={stageRef} data-testid="auth-route-stage">
      {layers.map((layer) => {
        const isOutgoing = layer.status === 'outgoing';
        return (
          <div
            className={`auth-route-layer auth-route-layer--${layer.status} auth-route-layer--${layer.direction}`}
            data-auth-path={layer.pathname}
            data-auth-transition={layer.status}
            key={`${layer.id}-${layer.pathname}`}
            aria-hidden={isOutgoing ? 'true' : undefined}
            inert={isOutgoing}
          >
            {layer.element}
          </div>
        );
      })}
    </div>
  );
}

function AuthBrandPanel() {
  return (
    <aside className="auth-brand" aria-label="Vaccination Portal">
      <Link to="/" className="auth-brand__identity" aria-label="Vaccination Portal home">
        <span className="auth-brand__glyph"><ShieldMark size={24} /></span>
        <span>Vaccination Portal</span>
      </Link>

      <div className="auth-brand__statement">
        <p>Public health service</p>
        <h2>Care starts with a secure connection.</h2>
        <span>One trusted sign-in for citizen services and authorized health operations.</span>
      </div>

      <footer className="auth-brand__footer">
        <span>Secure · Private · Role-aware</span>
        <span aria-hidden="true">VP / 2026</span>
      </footer>
    </aside>
  );
}

export default function AuthExperience() {
  const { pathname } = useLocation();
  const meta = ROUTE_META[pathname] || ROUTE_META['/login'];

  useEffect(() => {
    if (!('scrollRestoration' in window.history)) return undefined;
    const previous = window.history.scrollRestoration;
    window.history.scrollRestoration = 'manual';
    return () => { window.history.scrollRestoration = previous; };
  }, []);

  return (
    <div className="auth-experience">
      <div className="auth-shell">
        <AuthBrandPanel />
        <main className="auth-workspace" id="main-content">
          <header className="auth-workspace__rail">
            <div className="auth-workspace__navigation">
              <Link to="/" className="auth-home-link">
                <ArrowIcon direction="left" />
                <span>Portal home</span>
              </Link>
            </div>
            <div className="auth-route-meta" aria-label={`Current area: ${meta.branch}`}>
              <span>{meta.branch}</span>
              <span>{meta.step} / 02</span>
            </div>
          </header>
          <AuthRouteTransition />
        </main>
      </div>
    </div>
  );
}
