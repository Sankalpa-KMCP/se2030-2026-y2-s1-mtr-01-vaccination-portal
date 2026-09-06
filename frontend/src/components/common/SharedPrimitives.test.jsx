import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import StatusBadge from './StatusBadge';
import PageHeader from './PageHeader';
import EmptyState from './EmptyState';
import SuccessAlert from './SuccessAlert';
import {
  appointmentStatusVariants,
  campaignStatusVariants,
  sessionStatusVariants,
} from './statusVariants';

describe('StatusBadge', () => {
  afterEach(() => cleanup());

  it('renders the domain-specific class for a known status', () => {
    render(<StatusBadge status="SCHEDULED" variants={campaignStatusVariants} />);
    const badge = screen.getByText('SCHEDULED');
    expect(badge.className).toContain('bg-info-subtle');
  });

  it('keeps session mappings independent from campaign mappings', () => {
    const { unmount } = render(<StatusBadge status="SCHEDULED" variants={campaignStatusVariants} />);
    expect(screen.getByText('SCHEDULED').className).toContain('bg-info-subtle');
    unmount();

    render(<StatusBadge status="SCHEDULED" variants={sessionStatusVariants} />);
    expect(screen.getByText('SCHEDULED').className).toContain('bg-info-subtle');
  });

  it('falls back to a neutral chip for unknown statuses', () => {
    render(<StatusBadge status="SOME_NEW_STATE" variants={appointmentStatusVariants} />);
    const badge = screen.getByText('SOME_NEW_STATE');
    expect(badge.className).toContain('badge');
    expect(badge.className).toContain('bg-secondary-subtle');
  });

  it('appends spacing classes when provided', () => {
    render(<StatusBadge status="CONFIRMED" variants={appointmentStatusVariants} className="ms-2" />);
    expect(screen.getByText('CONFIRMED').className).toContain('ms-2');
  });
});

describe('PageHeader', () => {
  afterEach(cleanup);

  it('renders title, eyebrow, subtitle, and actions with heading wiring', () => {
    render(
      <PageHeader
        title="Campaign Management"
        eyebrow="Programme Planning"
        subtitle="Create and publish vaccination campaigns."
        actions={<button type="button">New Campaign</button>}
      />,
    );

    const heading = screen.getByRole('heading', { level: 1, name: 'Campaign Management' });
    expect(heading.className).toContain('h3');
    expect(screen.getByText('Programme Planning')).toBeTruthy();
    expect(screen.getByText('Create and publish vaccination campaigns.')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'New Campaign' })).toBeTruthy();
    expect(heading.id).toBeTruthy();
  });

  it('derives a stable heading id from the title', () => {
    render(<PageHeader title="Center Operations" />);
    expect(screen.getByRole('heading', { level: 1 }).id).toBe('page-heading-center-operations');
  });
});

describe('EmptyState', () => {
  afterEach(cleanup);

  it('renders icon, title, body, and action', () => {
    render(
      <EmptyState icon="bi-calendar-week" title="No sessions found" action={<button type="button">Create one</button>}>
        Nothing matches the current filters.
      </EmptyState>,
    );

    expect(screen.getByText('No sessions found')).toBeTruthy();
    expect(screen.getByText('Nothing matches the current filters.')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Create one' })).toBeTruthy();
    const icon = document.querySelector('.bi-calendar-week');
    expect(icon).toBeTruthy();
    expect(icon.getAttribute('aria-hidden')).toBe('true');
  });
});

describe('SuccessAlert', () => {
  afterEach(cleanup);

  it('renders the message with status semantics', () => {
    render(<SuccessAlert message="Campaign published successfully." />);
    expect(screen.getByRole('status')).toBeTruthy();
    expect(screen.getByText('Campaign published successfully.')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Close' })).toBeNull();
  });

  it('supports manual dismissal', () => {
    const onDismiss = vi.fn();
    render(<SuccessAlert message="Saved." onDismiss={onDismiss} />);
    fireEvent.click(screen.getByRole('button', { name: 'Close' }));
    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  it('auto-dismisses after the configured delay', () => {
    vi.useFakeTimers();
    const onDismiss = vi.fn();
    render(<SuccessAlert message="Saved." onDismiss={onDismiss} autoDismissMs={5000} />);
    expect(onDismiss).not.toHaveBeenCalled();
    vi.advanceTimersByTime(5100);
    expect(onDismiss).toHaveBeenCalledTimes(1);
    vi.useRealTimers();
  });
});
