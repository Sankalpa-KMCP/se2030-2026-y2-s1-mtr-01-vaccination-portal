import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
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
      screen.getByRole('heading', { level: 1, name: /manage your vaccination journey in one place/i }),
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

  it('exposes registration routes and universal login entry points', () => {
    renderLanding();

    // The landing page intentionally offers several citizen-registration entry
    // points (hero CTA and care-paths CTA); every one of them must route to the
    // citizen registration page.
    const registrationLinks = screen.getAllByRole('link', { name: /create citizen account/i });
    expect(registrationLinks.length).toBeGreaterThan(0);
    registrationLinks.forEach((link) => expect(link.getAttribute('href')).toBe('/register/citizen'));
    expect(screen.getByRole('link', { name: /^sign in$/i }).getAttribute('href')).toBe('/login');
    expect(screen.getByRole('link', { name: /request staff account/i }).getAttribute('href')).toBe('/register/staff');
    expect(screen.getByRole('link', { name: /manage your appointments/i }).getAttribute('href')).toBe('/login');
  });

  it('explains the selected capability and updates its connected outcome', () => {
    renderLanding();
    const choices = screen.getByRole('group', { name: /explore portal capabilities/i });
    const time = within(choices).getByRole('button', { name: /time sessions and capacity/i });
    const record = within(choices).getByRole('button', { name: /documentation vaccination draft documentation/i });
    expect(time.getAttribute('aria-pressed')).toBe('true');
    const detail = document.getElementById(time.getAttribute('aria-controls'));
    expect(within(detail).getByRole('heading', { name: /sessions and capacity/i })).toBeTruthy();
    expect(within(detail).getByText('Bookable time slots')).toBeTruthy();
    fireEvent.click(record);
    expect(record.getAttribute('aria-pressed')).toBe('true');
    expect(time.getAttribute('aria-pressed')).toBe('false');
    expect(within(detail).getByRole('heading', { name: /vaccination draft documentation/i })).toBeTruthy();
    expect(within(detail).getByText('Protected draft')).toBeTruthy();
    expect(within(detail).queryByText('Bookable time slots')).toBeNull();
    expect(detail.getAttribute('aria-live')).toBe('polite');
  });

  it('pairs each citizen journey stage with its staff operation', () => {
    renderLanding();
    const journey = screen.getByRole('list', { name: /citizen and staff vaccination journey/i });
    const stages = within(journey).getAllByRole('listitem');
    expect(stages).toHaveLength(4);
    stages.forEach((stage) => {
      expect(within(stage).getByRole('heading', { level: 3 })).toBeTruthy();
      expect(within(stage).getByText('Behind the scenes')).toBeTruthy();
    });
    expect(screen.getByRole('link', { name: /already registered\? access the portal/i }).getAttribute('href')).toBe('/login');
  });

  it('renders one concise hero image with a supporting caption', () => {
    renderLanding();

    expect(
      screen.getByRole('img', {
        name: /healthcare worker assisting a citizen during a vaccination appointment/i,
      }),
    ).toBeTruthy();
    expect(screen.getByText(/screening, appointments, and records through one portal/i)).toBeTruthy();
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

  it('keeps the hero focused without gallery controls or promotional claims', () => {
    renderLanding();

    expect(screen.queryByRole('tablist', { name: /visual showcase/i })).toBeNull();
    expect(screen.queryByText(/live portal service/i)).toBeNull();
    expect(screen.queryByText(/compassionate senior care/i)).toBeNull();
    expect(screen.queryByText(/priority assisted clinic bay/i)).toBeNull();
  });
});
