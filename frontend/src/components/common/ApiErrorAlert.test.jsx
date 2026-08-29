import React from 'react';
import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import ApiErrorAlert from './ApiErrorAlert';
import { ApiError, PERMISSION_DENIED_MESSAGE } from '../../services/apiClient';

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
});
