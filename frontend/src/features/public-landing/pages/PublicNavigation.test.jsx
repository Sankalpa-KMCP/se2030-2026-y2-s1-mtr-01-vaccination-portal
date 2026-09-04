import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import PublicHeader from '../components/PublicHeader';
import PublicFooter from '../components/PublicFooter';
import PublicServiceGuide from '../components/PublicServiceGuide';

function renderHeader(onLoginClick = vi.fn()) {
  return render(
    <MemoryRouter>
      <PublicHeader onLoginClick={onLoginClick} />
    </MemoryRouter>,
  );
}

function renderFooter() {
  return render(
    <MemoryRouter>
      <PublicFooter />
    </MemoryRouter>,
  );
}

describe('Public site navigation', () => {
  afterEach(cleanup);

  it('renders all three page links plus login in the header', () => {
    renderHeader();

    expect(screen.getByRole('link', { name: 'Home' }).getAttribute('href')).toBe('/');
    expect(screen.getByRole('link', { name: 'How it works' }).getAttribute('href')).toBe('/how-it-works');
    expect(screen.getByRole('link', { name: 'About' }).getAttribute('href')).toBe('/about');
    expect(screen.getByRole('button', { name: 'Login' })).toBeTruthy();
  });

  it('marks the current page in the header nav', () => {
    render(
      <MemoryRouter initialEntries={['/how-it-works']}>
        <PublicHeader onLoginClick={vi.fn()} />
      </MemoryRouter>,
    );

    const activeLink = screen.getByRole('link', { name: 'How it works' });
    expect(activeLink.className).toMatch(/lp-is-active/);

    const homeLink = screen.getByRole('link', { name: 'Home' });
    expect(homeLink.className).not.toMatch(/lp-is-active/);
  });

  it('renders footer navigation as router links', () => {
    renderFooter();

    expect(screen.getByRole('link', { name: 'Home' }).getAttribute('href')).toBe('/');
    expect(screen.getByRole('link', { name: 'How it works' }).getAttribute('href')).toBe('/how-it-works');
    expect(screen.getByRole('link', { name: 'About' }).getAttribute('href')).toBe('/about');
  });

  it('fires onLoginClick from the single shared header button', () => {
    const onLoginClick = vi.fn();
    renderHeader(onLoginClick);
    fireEvent.click(screen.getByRole('button', { name: 'Login' }));
    expect(onLoginClick).toHaveBeenCalledTimes(1);

    cleanup();
    renderFooter();
    expect(screen.queryByRole('button', { name: 'Login' })).toBeNull();
  });

  it('exposes the service banner', () => {
    renderHeader();
    expect(screen.getByText(/an official public health service/i)).toBeTruthy();
  });

  it('connects all public pages through a shared service guide', () => {
    render(
      <MemoryRouter initialEntries={['/how-it-works']}>
        <PublicServiceGuide />
      </MemoryRouter>,
    );

    const guide = screen.getByRole('navigation', { name: /public service guide/i });
    expect(within(guide).getByRole('link', { name: /start with the service/i }).getAttribute('href')).toBe('/');
    const processLink = within(guide).getByRole('link', { name: /follow the care journey/i });
    expect(processLink.getAttribute('href')).toBe('/how-it-works');
    expect(processLink.getAttribute('aria-current')).toBe('page');
    expect(within(guide).getByRole('link', { name: /review roles and safeguards/i }).getAttribute('href')).toBe('/about');
  });
});
