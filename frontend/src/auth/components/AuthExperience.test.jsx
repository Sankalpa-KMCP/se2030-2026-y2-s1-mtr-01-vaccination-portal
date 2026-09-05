import React from 'react';
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Link, MemoryRouter, Route, Routes, useNavigate } from 'react-router-dom';
import AuthExperience from './AuthExperience';

function HubPage() {
  return (
    <section>
      <h1 tabIndex="-1">Access Hub</h1>
      <Link to="/login/citizen">Citizen route</Link>
    </section>
  );
}

function CitizenPage() {
  const navigate = useNavigate();
  return (
    <section>
      <h1 tabIndex="-1">Citizen Access</h1>
      <button type="button" onClick={() => navigate(-1)}>Back</button>
    </section>
  );
}

function renderExperience() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <Routes>
        <Route element={<AuthExperience />}>
          <Route path="/login" element={<HubPage />} />
          <Route path="/login/citizen" element={<CitizenPage />} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe('AuthExperience route transitions', () => {
  afterEach(() => {
    cleanup();
    vi.useRealTimers();
  });

  it('keeps outgoing content inert while the next auth route enters', () => {
    vi.useFakeTimers();
    const { container } = renderExperience();
    document.documentElement.scrollTop = 120;

    expect(container.querySelectorAll('.auth-route-layer')).toHaveLength(1);
    expect(screen.getByRole('navigation', { name: 'Access navigation' })).toBeTruthy();
    expect(screen.getByRole('link', { name: 'All access' }).getAttribute('aria-current')).toBe('page');
    fireEvent.click(screen.getByRole('link', { name: 'Citizen route' }));

    const outgoing = container.querySelector('[data-auth-transition="outgoing"]');
    const incoming = container.querySelector('[data-auth-transition="incoming"]');
    expect(outgoing?.getAttribute('data-auth-path')).toBe('/login');
    expect(outgoing?.hasAttribute('inert')).toBe(true);
    expect(outgoing?.getAttribute('aria-hidden')).toBe('true');
    expect(outgoing?.classList.contains('auth-route-layer--forward')).toBe(true);
    expect(incoming?.getAttribute('data-auth-path')).toBe('/login/citizen');
    expect(document.documentElement.scrollTop).toBe(0);

    act(() => vi.advanceTimersByTime(479));
    expect(container.querySelectorAll('.auth-route-layer')).toHaveLength(2);
    act(() => vi.advanceTimersByTime(1));
    expect(container.querySelectorAll('.auth-route-layer')).toHaveLength(1);
    expect(container.querySelector('[data-auth-transition="current"]')?.getAttribute('data-auth-path')).toBe('/login/citizen');
    expect(screen.getByRole('link', { name: 'Citizen' }).getAttribute('aria-current')).toBe('page');
  });

  it('uses backward direction for browser-history navigation', () => {
    vi.useFakeTimers();
    const { container } = renderExperience();
    fireEvent.click(screen.getByRole('link', { name: 'Citizen route' }));
    act(() => vi.advanceTimersByTime(480));

    fireEvent.click(screen.getByRole('button', { name: 'Back' }));
    const incoming = container.querySelector('[data-auth-transition="incoming"]');
    expect(incoming?.getAttribute('data-auth-path')).toBe('/login');
    expect(incoming?.classList.contains('auth-route-layer--backward')).toBe(true);
  });
});
