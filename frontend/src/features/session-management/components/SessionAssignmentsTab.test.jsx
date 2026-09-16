import React from 'react';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import SessionAssignmentsTab from './SessionAssignmentsTab';

const api = vi.hoisted(() => ({
  listResourceAssignments: vi.fn(),
  deleteResourceAssignment: vi.fn(),
  listStaffAssignments: vi.fn(),
  deleteStaffAssignment: vi.fn(),
}));

vi.mock('../api/sessionAssignmentsApi', () => api);
vi.mock('../api/sessionApi', () => ({ formatTimeInTimezone: () => '09:00' }));
vi.mock('./AssignResourceModal', () => ({
  default: () => <div data-testid="resource-modal">Resource modal</div>,
}));
vi.mock('./AssignStaffModal', () => ({
  default: () => <div data-testid="staff-modal">Staff modal</div>,
}));

const draftSession = {
  id: 51,
  centerId: 3,
  lifecycleStatus: 'DRAFT',
  scheduleTimezone: 'Asia/Colombo',
};

beforeEach(() => {
  api.listResourceAssignments.mockReset().mockResolvedValue([]);
  api.listStaffAssignments.mockReset().mockResolvedValue([]);
  api.deleteResourceAssignment.mockReset().mockResolvedValue(null);
  api.deleteStaffAssignment.mockReset().mockResolvedValue(null);
  vi.spyOn(window, 'confirm').mockReturnValue(true);
});

afterEach(() => {
  vi.restoreAllMocks();
  cleanup();
});

describe('SessionAssignmentsTab authorization', () => {
  it.each(['ADMIN', 'CENTER_MANAGER'])('%s can mutate a DRAFT session', async () => {
    render(<SessionAssignmentsTab session={draftSession} canManage />);

    expect(await screen.findByRole('button', { name: '+ Assign Resource' })).toBeTruthy();
    expect(screen.getByRole('button', { name: '+ Assign Staff' })).toBeTruthy();
  });

  it('keeps HEALTHCARE_OFFICER read access without mutation controls', async () => {
    render(<SessionAssignmentsTab session={draftSession} canManage={false} />);

    await waitFor(() => {
      expect(api.listResourceAssignments).toHaveBeenCalledWith(draftSession.id);
      expect(api.listStaffAssignments).toHaveBeenCalledWith(draftSession.id);
    });
    expect(screen.getByText('No operational resources currently assigned to this session.')).toBeTruthy();
    expect(screen.queryByRole('button', { name: '+ Assign Resource' })).toBeNull();
    expect(screen.queryByRole('button', { name: '+ Assign Staff' })).toBeNull();
  });

  it.each(['SCHEDULED', 'IN_PROGRESS'])('is read-only in %s even with management permission', async (status) => {
    render(<SessionAssignmentsTab session={{ ...draftSession, lifecycleStatus: status }} canManage />);

    expect(await screen.findByText(/Assignments are read-only because session is in/)).toBeTruthy();
    expect(screen.queryByRole('button', { name: '+ Assign Resource' })).toBeNull();
    expect(screen.queryByRole('button', { name: '+ Assign Staff' })).toBeNull();
  });

  it.each([
    ['+ Assign Resource', 'resource-modal'],
    ['+ Assign Staff', 'staff-modal'],
  ])('closes and clears the %s modal when permission is lost', async (buttonName, testId) => {
    const view = render(<SessionAssignmentsTab session={draftSession} canManage />);

    fireEvent.click(await screen.findByRole('button', { name: buttonName }));
    expect(screen.getByTestId(testId)).toBeTruthy();

    view.rerender(<SessionAssignmentsTab session={draftSession} canManage={false} />);
    await waitFor(() => expect(screen.queryByTestId(testId)).toBeNull());

    view.rerender(<SessionAssignmentsTab session={draftSession} canManage />);
    expect(screen.queryByTestId(testId)).toBeNull();
  });

  it('renders a backend 403 safely', async () => {
    api.listResourceAssignments.mockResolvedValue([{
      id: 71,
      resourceType: 'ROOM',
      resourceName: 'Room A',
      startAt: '2026-10-01T03:30:00Z',
      endAt: '2026-10-01T07:30:00Z',
    }]);
    api.deleteResourceAssignment.mockRejectedValue(Object.assign(new Error('Forbidden by backend policy'), { status: 403 }));
    render(<SessionAssignmentsTab session={draftSession} canManage />);

    fireEvent.click(await screen.findByRole('button', { name: 'Remove' }));
    expect(await screen.findByText('Permission Denied')).toBeTruthy();
    expect(screen.getByText('You do not have permission to perform this action.')).toBeTruthy();
  });
});
