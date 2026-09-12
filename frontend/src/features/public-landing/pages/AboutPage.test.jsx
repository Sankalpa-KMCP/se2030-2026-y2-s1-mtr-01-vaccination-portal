import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AboutPage from './AboutPage';

function renderPage(onLoginClick = vi.fn()) {
  return render(
    <MemoryRouter>
      <AboutPage onLoginClick={onLoginClick} />
    </MemoryRouter>,
  );
}

describe('AboutPage', () => {
  afterEach(cleanup);

  it('renders the page hero and section headings', () => {
    const { container } = renderPage();

    expect(
      screen.getByRole('heading', { level: 1, name: /a system built on responsibility/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole('heading', { level: 2, name: /seven roles, one registry/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole('heading', { level: 2, name: /what the system governs/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole('heading', { level: 2, name: /accountable by design/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole('heading', { level: 2, name: /three commitments we hold/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole('heading', { level: 2, name: /see the service from account to clinic/i }),
    ).toBeTruthy();
    expect(container.querySelector('.premium-visual--registry')).toBeTruthy();
    expect(container.querySelectorAll('.premium-registry-card')).toHaveLength(7);
  });

  it('lists all seven system roles with scope tags', () => {
    renderPage();

    ['Citizen', 'Healthcare Officer', 'Center Manager', 'Campaign Officer', 'Inventory Officer', 'Management User', 'Administrator'].forEach((role) => {
      expect(screen.getByRole('heading', { level: 3, name: role })).toBeTruthy();
    });
    ['Self-service', 'Clinical operations', 'Center readiness', 'Programme design', 'Inventory responsibilities', 'Oversight', 'System authority'].forEach((scope) => {
      expect(screen.getByText(scope)).toBeTruthy();
    });
  });

  it('lists the five operational ledger domains', () => {
    renderPage();

    ['Campaign lifecycle', 'Center operations', 'Session scheduling', 'Readiness validation', 'Vaccination draft documentation'].forEach((title) => {
      expect(screen.getByRole('heading', { level: 3, name: title })).toBeTruthy();
    });
  });

  it('renders the four trust commitments', () => {
    renderPage();

    ['Controlled access', 'Recorded changes', 'Validated openings', 'Operational safety'].forEach((title) => {
      expect(screen.getByRole('heading', { level: 3, name: title })).toBeTruthy();
    });
  });

  it('renders the three data statement headlines', () => {
    renderPage();

    expect(screen.getByText('Your profile follows your account.')).toBeTruthy();
    expect(screen.getByText('Key lifecycle changes are recorded.')).toBeTruthy();
    expect(screen.getByText('Clinics open prepared.')).toBeTruthy();
  });

  it('continues to the care journey without repeating the login action', () => {
    renderPage();

    expect(screen.getByRole('link', { name: /explore how it works/i }).getAttribute('href')).toBe('/how-it-works');
    expect(screen.queryByRole('button', { name: /sign in to the portal/i })).toBeNull();
  });

  it('provides a skip link targeting main content', () => {
    renderPage();

    expect(screen.getByRole('link', { name: /skip to main content/i }).getAttribute('href')).toBe('#lp-main');
    expect(screen.getByRole('main').getAttribute('id')).toBe('lp-main');
  });
});
