import React from 'react';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import SessionDetailsModal from './SessionDetailsModal';

const mocks = vi.hoisted(() => ({
  auth: { user: { roles: ['ROLE_ADMIN'] } },
  getSessionById: vi.fn(),
  assignmentsChild: vi.fn(() => <div data-testid="assignments-child">Assignments child</div>),
  capacityChild: vi.fn(() => <div data-testid="capacity-child">Capacity child</div>),
}));

vi.mock('../../../auth/context/AuthContext', () => ({
  useAuth: () => mocks.auth,
}));

vi.mock('../api/sessionApi', () => ({
  getSessionById: mocks.getSessionById,
  formatInTimezone: () => 'formatted',
  scheduleSession: vi.fn(),
  startSession: vi.fn(),
  completeSession: vi.fn(),
  suspendSessionBooking: vi.fn(),
  resumeSessionBooking: vi.fn(),
}));

vi.mock('./SessionAssignmentsTab', () => ({ default: mocks.assignmentsChild }));
vi.mock('./SessionCapacityPlanTab', () => ({ default: mocks.capacityChild }));
vi.mock('./SessionTimeSlotsTab', () => ({ default: () => <div>Time slots child</div> }));
vi.mock('./SessionChangeControlTab', () => ({ default: () => <div>Change control child</div> }));
vi.mock('./SessionReadinessTab', () => ({ default: () => <div>Readiness child</div> }));

const session = {
  id: 41,
  lifecycleStatus: 'DRAFT',
  bookingStatus: 'OPEN',
  scheduleTimezone: 'Asia/Colombo',
  startAt: '2026-10-01T03:30:00Z',
  endAt: '2026-10-01T07:30:00Z',
};

function renderModal() {
  return render(
    <SessionDetailsModal
      isOpen
      onClose={vi.fn()}
      sessionId={session.id}
    />,
  );
}

beforeEach(() => {
  mocks.auth.user = { roles: ['ROLE_ADMIN'] };
  mocks.getSessionById.mockReset().mockResolvedValue(session);
  mocks.assignmentsChild.mockClear();
  mocks.capacityChild.mockClear();
});

afterEach(cleanup);

describe('SessionDetailsModal UC-10B authorization', () => {
  it.each(['ROLE_ADMIN', 'ROLE_CENTER_MANAGER'])(
    'shows UC-10B tabs to %s',
    async (role) => {
      mocks.auth.user = { roles: [role] };
      renderModal();

      expect(await screen.findByRole('button', { name: 'Operational Assignments' })).toBeTruthy();
      expect(screen.getByRole('button', { name: 'Capacity Plan' })).toBeTruthy();
    },
  );

  it.each(['ROLE_CAMPAIGN_OFFICER', 'ROLE_HEALTHCARE_OFFICER'])(
    'hides UC-10B tabs and does not mount their children for %s',
    async (role) => {
      mocks.auth.user = { roles: [role] };
      renderModal();

      expect(await screen.findByRole('button', { name: 'Overview' })).toBeTruthy();
      expect(screen.queryByRole('button', { name: 'Operational Assignments' })).toBeNull();
      expect(screen.queryByRole('button', { name: 'Capacity Plan' })).toBeNull();
      expect(mocks.assignmentsChild).not.toHaveBeenCalled();
      expect(mocks.capacityChild).not.toHaveBeenCalled();
    },
  );

  it('keeps unrelated session tabs available', async () => {
    mocks.auth.user = { roles: ['ROLE_CAMPAIGN_OFFICER'] };
    renderModal();

    expect(await screen.findByRole('button', { name: 'Overview' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Time Slots' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Change Control' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Readiness & Diagnostics' })).toBeTruthy();
  });

  it.each([
    ['Operational Assignments', 'assignments-child'],
    ['Capacity Plan', 'capacity-child'],
  ])('resets %s to overview when view permission disappears', async (tabName, childTestId) => {
    mocks.auth.user = { roles: ['ROLE_CENTER_MANAGER'] };
    const view = renderModal();

    fireEvent.click(await screen.findByRole('button', { name: tabName }));
    expect(screen.getByTestId(childTestId)).toBeTruthy();

    mocks.auth.user = { roles: ['ROLE_CAMPAIGN_OFFICER'] };
    view.rerender(
      <SessionDetailsModal isOpen onClose={vi.fn()} sessionId={session.id} />,
    );

    await waitFor(() => {
      expect(screen.queryByTestId(childTestId)).toBeNull();
      expect(screen.getByRole('button', { name: 'Overview' }).className).toContain('active');
    });
  });
});
