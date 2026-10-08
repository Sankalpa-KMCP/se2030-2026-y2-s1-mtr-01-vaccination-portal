import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import PostLoginGreeting from './PostLoginGreeting';

describe('PostLoginGreeting', () => {
  afterEach(() => {
    cleanup();
    document.body.style.overflow = '';
  });

  it('renders staff-specific confirmation and the authenticated name', () => {
    render(
      <PostLoginGreeting
        audience="staff"
        name="Vera Vaccinator"
        onComplete={vi.fn()}
      />,
    );

    expect(screen.getByRole('dialog', { name: 'Welcome, Vera Vaccinator' })).toBeTruthy();
    expect(screen.getByText('Staff sign-in successful')).toBeTruthy();
    expect(screen.getByText('Your secure operational workspace is ready.')).toBeTruthy();
  });

  it('renders administrator-specific confirmation and the authenticated name', () => {
    render(
      <PostLoginGreeting
        audience="admin"
        name="Portal Admin"
        onComplete={vi.fn()}
      />,
    );

    expect(screen.getByRole('dialog', { name: 'Welcome, Portal Admin' })).toBeTruthy();
    expect(screen.getByText('Administrator sign-in successful')).toBeTruthy();
    expect(screen.getByText('Your secure administration workspace is ready.')).toBeTruthy();
  });
});
