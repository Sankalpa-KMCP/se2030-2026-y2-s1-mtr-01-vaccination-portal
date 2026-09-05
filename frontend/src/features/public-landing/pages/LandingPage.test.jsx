import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import LandingPage from './LandingPage';

function renderLanding(onLoginClick = vi.fn()) {
  return render(
    <MemoryRouter>
      <LandingPage onLoginClick={onLoginClick} />
    </MemoryRouter>,
  );
}

describe('LandingPage', () => {
  afterEach(cleanup);

  it('renders the hero headline and section headings', () => {
    renderLanding();

    expect(
      screen.getByRole('heading', { level: 1, name: /a clear path from campaign to vaccination record/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole('heading', { level: 2, name: /one service. two ways in/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole('heading', { level: 2, name: /the work behind every appointment/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole('heading', { level: 2, name: /citizen steps and staff operations move together/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole('heading', { level: 2, name: /trust is built into the work/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole('heading', { level: 2, name: /start with your citizen account/i }),
    ).toBeTruthy();
  });

  it('exposes all four authentication routes', () => {
    renderLanding();

    expect(screen.getByRole('link', { name: /create citizen account/i }).getAttribute('href')).toBe('/register/citizen');
    expect(screen.getByRole('link', { name: /sign in as a citizen/i }).getAttribute('href')).toBe('/login/citizen');
    expect(screen.getByRole('link', { name: /request staff account/i }).getAttribute('href')).toBe('/register/staff');
    expect(screen.getByRole('link', { name: /enter staff portal/i }).getAttribute('href')).toBe('/login/staff');
  });

  it('renders an animation-ready care path with readiness before appointments', () => {
    const { container } = renderLanding();
    const artwork = container.querySelector('.home-pathway');

    expect(artwork).toBeTruthy();
    expect(artwork.querySelectorAll('.home-pathway__node-core')).toHaveLength(5);
    expect(artwork.querySelector('.home-pathway__route').getAttribute('pathLength')).toBe('1');
    expect(artwork.querySelector('.home-pathway__route-progress')).toBeTruthy();
    expect(artwork.querySelector('.home-pathway__staff-connector--ready').getAttribute('d')).toMatch(/^M375/);
    expect(artwork.textContent).toContain('SESSION READY');
    expect(artwork.textContent).toContain('Center, capacity and staff confirmed');
  });

  it('fires onLoginClick from the login call-to-action', () => {
    const onLoginClick = vi.fn();
    renderLanding(onLoginClick);

    const loginButtons = screen.getAllByRole('button', { name: 'Login' });
    loginButtons.forEach((button) => button.click());
    expect(onLoginClick).toHaveBeenCalledTimes(loginButtons.length);
    expect(onLoginClick).toHaveBeenCalled();
  });

  it('provides a skip link targeting main content', () => {
    renderLanding();

    expect(screen.getByRole('link', { name: /skip to main content/i }).getAttribute('href')).toBe('#lp-main');
    expect(screen.getByRole('main').getAttribute('id')).toBe('lp-main');
  });

  it('provides five accessible care-path destinations', () => {
    renderLanding();

    const rail = screen.getByRole('navigation', { name: /care path progress/i });
    const expectedDestinations = [
      ['01 Access', '#portal-access'],
      ['02 System', '#capabilities'],
      ['03 Journey', '#how-it-works'],
      ['04 Assurance', '#accountability'],
      ['05 Begin', '#get-started'],
    ];

    expectedDestinations.forEach(([name, href]) => {
      expect(within(rail).getByRole('link', { name }).getAttribute('href')).toBe(href);
    });
    expect(document.getElementById('get-started')).toBeTruthy();
  });
});
