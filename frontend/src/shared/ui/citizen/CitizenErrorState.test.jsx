import React from 'react';
import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { PERMISSION_DENIED_MESSAGE } from '../../api/apiClient';
import CitizenErrorState from './CitizenErrorState';

afterEach(cleanup);

describe('CitizenErrorState', () => {
  it('keeps meaningful API error text in the subdued citizen alert', () => {
    render(<CitizenErrorState error={Object.assign(new Error('Campaign unavailable'), { status: 404 })} />);
    expect(screen.getByRole('alert')).toBeTruthy();
    expect(screen.getByText('Information not found')).toBeTruthy();
    expect(screen.getByText('Campaign unavailable')).toBeTruthy();
  });

  it('renders nothing without an error and keeps the citizen fallback punctuation', () => {
    const view = render(<CitizenErrorState error={null} />);
    expect(view.container.firstChild).toBeNull();
    view.rerender(<CitizenErrorState error={{ status: 500 }} />);
    expect(screen.getByText('An unexpected error occurred.')).toBeTruthy();
  });

  it.each([
    [0, 'Connection unavailable'],
    [403, 'Access unavailable'],
    [409, 'Request needs attention'],
    [400, 'We could not complete this request'],
  ])('preserves the citizen title for status %s', (status, title) => {
    render(<CitizenErrorState error={{ status, message: 'Server message' }} />);
    expect(screen.getByText(title)).toBeTruthy();
  });

  it('overrides backend wording for 403 and keeps a caller title override', () => {
    render(<CitizenErrorState error={{ status: 403, message: 'Forbidden' }} title="Eligibility unavailable" />);
    expect(screen.getByText('Eligibility unavailable')).toBeTruthy();
    expect(screen.getByText(PERMISSION_DENIED_MESSAGE)).toBeTruthy();
    expect(screen.queryByText('Forbidden')).toBeNull();
  });

  it('preserves ordered object and array field errors', () => {
    const view = render(<CitizenErrorState error={{
      status: 400, message: 'Invalid', data: { fieldErrors: { first: ['First issue', 'Second issue'], last: 'Last issue' } },
    }} />);
    expect([...view.container.querySelectorAll('.citizen-error-fields li')].map((item) => item.textContent))
      .toEqual(['first: First issue', 'first: Second issue', 'last: Last issue']);
    view.rerender(<CitizenErrorState error={{
      status: 400, message: 'Invalid', data: { fieldErrors: [{ field: 'email', message: 'Email issue' }, { field: 'blank' }] },
    }} />);
    expect([...view.container.querySelectorAll('.citizen-error-fields li')].map((item) => item.textContent))
      .toEqual(['email: Email issue']);
  });
});
