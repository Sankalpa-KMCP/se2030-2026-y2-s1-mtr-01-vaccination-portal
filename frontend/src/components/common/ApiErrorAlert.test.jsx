import React from 'react';
import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import ApiErrorAlert from './ApiErrorAlert';
import { ApiError, PERMISSION_DENIED_MESSAGE } from '../../shared/api/apiClient';

describe('ApiErrorAlert', () => {
  afterEach(() => {
    cleanup();
  });

  it('presents a 403 as permission denied without exposing generic backend wording', () => {
    render(<ApiErrorAlert error={new ApiError('Forbidden', 403, { message: 'Forbidden' })} />);

    expect(screen.getByText('Permission Denied')).toBeTruthy();
    expect(screen.getByText(PERMISSION_DENIED_MESSAGE)).toBeTruthy();
  });

  it('presents a network error using the shared Bootstrap alert', () => {
    render(<ApiErrorAlert error={new ApiError('Unable to reach the server.', 0)} />);

    expect(screen.getByRole('alert').className).toContain('alert-danger');
    expect(screen.getByText('Connection Error')).toBeTruthy();
    expect(screen.getByText('Unable to reach the server.')).toBeTruthy();
  });

  it('renders both list-shaped and map-shaped validation details', () => {
    const { rerender } = render(
      <ApiErrorAlert error={new ApiError('Invalid request', 400, {
        fieldErrors: [{ field: 'email', message: 'Email is invalid' }],
      })} />,
    );

    expect(screen.getByText('Email is invalid')).toBeTruthy();

    rerender(
      <ApiErrorAlert error={new ApiError('Invalid request', 400, {
        fieldErrors: { citizenId: ['Citizen ID is required'] },
      })} />,
    );

    expect(screen.getByText('Citizen ID is required')).toBeTruthy();
  });

  it.each([
    [404, 'Not Found'],
    [409, 'Request Conflict'],
    [400, 'Request Error'],
    [401, 'Request Error'],
  ])('keeps the generic title and server message for status %s', (status, title) => {
    render(<ApiErrorAlert error={new ApiError('Server message', status)} />);
    expect(screen.getByText(title)).toBeTruthy();
    expect(screen.getByText('Server message')).toBeTruthy();
  });

  it('renders nothing without an error and preserves its fallback wording', () => {
    const view = render(<ApiErrorAlert error={null} />);
    expect(view.container.firstChild).toBeNull();
    view.rerender(<ApiErrorAlert error={{ status: 500, data: { fieldErrors: 'malformed' } }} />);
    expect(screen.getByText('An unexpected error occurred')).toBeTruthy();
  });

  it('keeps field-error order and the dismiss action', () => {
    let dismissed = false;
    const view = render(<ApiErrorAlert
      error={{ status: 400, message: 'Invalid', data: { fieldErrors: { first: ['A', 'B'], second: 'C' } } }}
      onDismiss={() => { dismissed = true; }}
    />);
    expect([...view.container.querySelectorAll('li')].map((item) => item.textContent))
      .toEqual(['first: A', 'first: B', 'second: C']);
    screen.getByRole('button', { name: 'Close' }).click();
    expect(dismissed).toBe(true);
  });
});
