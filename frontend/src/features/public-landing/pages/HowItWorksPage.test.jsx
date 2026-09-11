import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import HowItWorksPage from './HowItWorksPage';

function renderPage(onLoginClick = vi.fn()) {
  return render(
    <MemoryRouter>
      <HowItWorksPage onLoginClick={onLoginClick} />
    </MemoryRouter>,
  );
}

describe('HowItWorksPage', () => {
  afterEach(cleanup);

  it('renders the page hero and section headings', () => {
    const { container } = renderPage();

    expect(
      screen.getByRole('heading', { level: 1, name: /one system, two journeys/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole('heading', { level: 2, name: /follow a journey from start to finish/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole('heading', { level: 2, name: /no session opens until every check passes/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole('heading', { level: 2, name: /frequently asked/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole('heading', { level: 2, name: /see what keeps the service trustworthy/i }),
    ).toBeTruthy();
    expect(container.querySelector('.premium-visual--journey')).toBeTruthy();
    expect(container.querySelectorAll('.premium-journey-map__node')).toHaveLength(5);
  });

  it('renders the citizen journey by default with all five steps', () => {
    renderPage();

    ['Create your account', 'Complete pre-vaccination screening', 'Book an appointment', 'Attend the clinic', 'Your record updates'].forEach((title) => {
      expect(screen.getByRole('heading', { level: 3, name: title })).toBeTruthy();
    });
  });

  it('switches to the staff journey via the tablist', () => {
    renderPage();

    const staffTab = screen.getByRole('tab', { name: /for health staff/i });
    fireEvent.click(staffTab);

    expect(screen.getByRole('tabpanel')).toBeTruthy();
    ['Request staff access', 'Administrator review', 'Plan campaign and center', 'Schedule sessions', 'Verify readiness and open'].forEach((title) => {
      expect(screen.getByRole('heading', { level: 3, name: title })).toBeTruthy();
    });
  });

  it('implements the accessible tabs pattern with arrow-key navigation', () => {
    renderPage();

    const citizenTab = screen.getByRole('tab', { name: /for citizens/i });
    const staffTab = screen.getByRole('tab', { name: /for health staff/i });

    expect(citizenTab.getAttribute('aria-selected')).toBe('true');
    expect(staffTab.getAttribute('aria-selected')).toBe('false');
    expect(staffTab.getAttribute('tabindex')).toBe('-1');
    expect(staffTab.getAttribute('aria-controls')).toMatch(/^lp-journey-panel-/);

    fireEvent.keyDown(citizenTab, { key: 'ArrowRight' });
    expect(staffTab.getAttribute('aria-selected')).toBe('true');

    fireEvent.keyDown(staffTab, { key: 'ArrowLeft' });
    expect(citizenTab.getAttribute('aria-selected')).toBe('true');
  });

  it('shows all five readiness checks and the verdict', () => {
    renderPage();

    ['Operating hours', 'Active stations', 'Planned capacity', 'Qualified staff', 'No schedule conflicts'].forEach((label) => {
      expect(screen.getByText(label)).toBeTruthy();
    });
    expect(screen.getByText('Ready to open')).toBeTruthy();
  });

  it('toggles FAQ items accessibly', () => {
    renderPage();

    const firstQuestion = 'Who can see my vaccination record?';
    const secondQuestion = 'How long does staff access take to approve?';

    const firstTrigger = screen.getByRole('button', { name: firstQuestion });
    expect(firstTrigger.getAttribute('aria-expanded')).toBe('true');

    fireEvent.click(firstTrigger);
    expect(firstTrigger.getAttribute('aria-expanded')).toBe('false');

    const secondTrigger = screen.getByRole('button', { name: secondQuestion });
    fireEvent.click(secondTrigger);
    expect(secondTrigger.getAttribute('aria-expanded')).toBe('true');
    expect(firstTrigger.getAttribute('aria-expanded')).toBe('false');
  });

  it('exposes FAQ regions wired to their buttons', () => {
    renderPage();

    const trigger = screen.getByRole('button', { name: 'Who can see my vaccination record?' });
    const regionId = trigger.getAttribute('aria-controls');
    expect(regionId).toBeTruthy();
    const region = document.getElementById(regionId);
    expect(region).toBeTruthy();
    expect(region.getAttribute('role')).toBe('region');
  });

  it('continues to the system overview without repeating the login action', () => {
    renderPage();

    expect(screen.getByRole('link', { name: /explore system safeguards/i }).getAttribute('href')).toBe('/about');
    expect(screen.queryByRole('button', { name: /sign in to the portal/i })).toBeNull();
  });

  it('provides a skip link targeting main content', () => {
    renderPage();

    expect(screen.getByRole('link', { name: /skip to main content/i }).getAttribute('href')).toBe('#lp-main');
    expect(screen.getByRole('main').getAttribute('id')).toBe('lp-main');
  });
});
