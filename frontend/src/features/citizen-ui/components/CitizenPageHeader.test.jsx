import React from 'react';
import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { Link, MemoryRouter } from 'react-router-dom';
import CitizenPageHeader from './CitizenPageHeader';

function renderHeader(props = {}) {
  return render(
    <MemoryRouter>
      <CitizenPageHeader title="My citizen page" {...props} />
    </MemoryRouter>,
  );
}

afterEach(cleanup);

describe('CitizenPageHeader', () => {
  it('renders eyebrow, h1 title, and subtitle in the standard variant', () => {
    const { container } = renderHeader({
      eyebrow: 'Citizen service',
      subtitle: 'Supporting description.',
    });

    expect(screen.getByText('Citizen service')).toBeTruthy();
    expect(screen.getByRole('heading', { level: 1, name: 'My citizen page' })).toBeTruthy();
    expect(screen.getByText('Supporting description.')).toBeTruthy();
    expect(container.querySelector('.citizen-page-header--standard')).toBeTruthy();
  });

  it('renders a semantic back link with its destination', () => {
    renderHeader({ backTo: '/app/citizen', backLabel: 'Back to Dashboard' });
    expect(screen.getByRole('link', { name: 'Back to Dashboard' }).getAttribute('href')).toBe('/app/citizen');
  });

  it('renders actions, badge, and meta content', () => {
    renderHeader({
      actions: <Link to="/app/screening/history">View history</Link>,
      badge: 'Current',
      meta: <span>Reference 42</span>,
    });

    expect(screen.getByRole('link', { name: 'View history' }).getAttribute('href')).toBe('/app/screening/history');
    expect(screen.getByText('Current')).toBeTruthy();
    expect(screen.getByText('Reference 42')).toBeTruthy();
  });

  it('supports the hero variant', () => {
    const { container } = renderHeader({ variant: 'hero' });
    expect(container.querySelector('.citizen-page-header--hero')).toBeTruthy();
  });

  it('safely omits optional content', () => {
    const { container } = renderHeader();
    expect(container.querySelector('.citizen-page-header__back')).toBeNull();
    expect(container.querySelector('.citizen-page-header__subtitle')).toBeNull();
    expect(container.querySelector('.citizen-page-header__aside')).toBeNull();
  });
});
