import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
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
      screen.getByRole('heading', { level: 1, name: /vaccination care, coordinated for your community/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole('heading', { level: 2, name: /choose your portal/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole('heading', { level: 2, name: /one portal for the whole journey/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole('heading', { level: 2, name: /how programme operations work/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole('heading', { level: 2, name: /built for operational trust/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole('heading', { level: 2, name: /ready to get started/i }),
    ).toBeTruthy();
  });

  it('exposes all four authentication routes', () => {
    renderLanding();

    expect(screen.getByRole('link', { name: /create citizen account/i }).getAttribute('href')).toBe('/register/citizen');
    expect(screen.getByRole('link', { name: /citizen login/i }).getAttribute('href')).toBe('/login/citizen');
    expect(screen.getByRole('link', { name: /request staff account/i }).getAttribute('href')).toBe('/register/staff');
    expect(screen.getByRole('link', { name: /staff login/i }).getAttribute('href')).toBe('/login/staff');
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
});
