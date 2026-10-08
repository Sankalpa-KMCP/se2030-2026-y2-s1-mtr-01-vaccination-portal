import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import VaccinatorDashboardSection from './VaccinatorDashboardSection';
import { getLocalCalendarToday } from '../utils/dateUtils';

const authMock = vi.hoisted(() => ({ user: null }));
const checkInApiMocks = vi.hoisted(() => ({
  getMyAssignedVaccinationSessions: vi.fn(),
  getVaccinatorQueue: vi.fn(),
}));
const draftApiMocks = vi.hoisted(() => ({
  listMyVaccinationDrafts: vi.fn(),
  createVaccinationDraft: vi.fn(),
}));
const certificateApiMocks = vi.hoisted(() => ({
  listVaccinatorCorrectionRequests: vi.fn(),
  listVaccinatorRecords: vi.fn(),
}));

vi.mock('../../../auth/context/AuthContext', () => ({ useAuth: () => ({ user: authMock.user }) }));
vi.mock('../../appointment-management/api/checkInApi', () => checkInApiMocks);
vi.mock('../api/vaccinationDraftApi', () => draftApiMocks);
vi.mock('../api/certificateApi', () => certificateApiMocks);

const sessions = [{
  id: 9,
  centerName: 'Colombo Central Vaccination Center',
  startAt: '2026-10-09T04:30:00Z',
  endAt: '2026-10-09T09:30:00Z',
  slots: [{ id: 17 }, { id: 18 }],
}];
const appointment = {
  id: 114,
  citizenId: 15,
  campaignId: 1,
  slotId: 17,
  status: 'CHECKED_IN',
};

function renderSection(props = {}) {
  return render(<MemoryRouter><VaccinatorDashboardSection {...props} /></MemoryRouter>);
}

describe('VaccinatorDashboardSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    authMock.user = { fullName: 'Dewmini', roles: ['ROLE_VACCINATOR'] };
    checkInApiMocks.getMyAssignedVaccinationSessions.mockResolvedValue(sessions);
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([]);
    draftApiMocks.listMyVaccinationDrafts.mockResolvedValue([]);
    draftApiMocks.createVaccinationDraft.mockResolvedValue({ id: 30 });
    certificateApiMocks.listVaccinatorCorrectionRequests.mockResolvedValue([]);
    certificateApiMocks.listVaccinatorRecords.mockResolvedValue([]);
  });

  afterEach(cleanup);

  it('renders nothing for a non-vaccinator', () => {
    authMock.user = { roles: ['ROLE_CITIZEN'] };
    const { container } = renderSection();
    expect(container.firstChild).toBeNull();
  });

  it('loads assigned sessions and aggregates every session slot', async () => {
    checkInApiMocks.getVaccinatorQueue
      .mockResolvedValueOnce([appointment])
      .mockResolvedValueOnce([{ ...appointment, id: 115, slotId: 18 }]);
    renderSection();

    expect(await screen.findByText('Appointment #114')).toBeTruthy();
    expect(screen.getByText('Appointment #115')).toBeTruthy();
    expect(checkInApiMocks.getMyAssignedVaccinationSessions).toHaveBeenCalledTimes(1);
    expect(checkInApiMocks.getVaccinatorQueue).toHaveBeenCalledWith(17);
    expect(checkInApiMocks.getVaccinatorQueue).toHaveBeenCalledWith(18);
  });

  it('shows only the four requested metrics and one compact work area', async () => {
    renderSection({ sessions });
    const metrics = screen.getByLabelText('Vaccinator metrics');
    expect(within(metrics).getByText('Ready Patients')).toBeTruthy();
    expect(within(metrics).getByText('Corrections Needed')).toBeTruthy();
    expect(within(metrics).getByText('Finalized Today')).toBeTruthy();
    expect(within(metrics).getByText('Today’s Session')).toBeTruthy();
    expect(screen.getByRole('heading', { name: "Today's Checked-In Patients" })).toBeTruthy();
    expect(screen.queryByText('Current Session')).toBeNull();
    expect(screen.queryByText('Active Work')).toBeNull();
    expect(screen.queryByRole('link', { name: /full check-in queue/i })).toBeNull();
  });

  it('counts approved corrections and records finalized today', async () => {
    certificateApiMocks.listVaccinatorCorrectionRequests.mockResolvedValue([
      { id: 1, status: 'APPROVED' },
      { id: 2, status: 'PENDING' },
    ]);
    certificateApiMocks.listVaccinatorRecords.mockResolvedValue([
      { id: 5, finalizedAt: `${getLocalCalendarToday()}T05:00:00Z` },
      { id: 6, finalizedAt: '2026-01-01T05:00:00Z' },
    ]);
    renderSection({ sessions });

    await waitFor(() => {
      const metrics = screen.getByLabelText('Vaccinator metrics');
      expect(within(metrics).getAllByText('1')).toHaveLength(2);
    });
  });

  it('shows an assigned-session empty state without raw slot entry', async () => {
    renderSection({ sessions: [] });
    expect(await screen.findByText('No active vaccination session is currently assigned.')).toBeTruthy();
    expect(screen.queryByLabelText(/Time Slot ID/i)).toBeNull();
    expect(screen.queryByRole('button', { name: /Load Queue/i })).toBeNull();
  });

  it('renders patient section as strictly read-only with no draft actions or mutation chips', async () => {
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([appointment]);
    renderSection({ sessions });

    expect(await screen.findByText('Appointment #114')).toBeTruthy();
    expect(screen.getByText(/Citizen #15/)).toBeTruthy();
    expect(screen.queryByRole('button', { name: /Start vaccination/i })).toBeNull();
    expect(screen.queryByRole('link', { name: /Open Draft/i })).toBeNull();
    expect(screen.queryByText(/^Checked In$/i)).toBeNull();
    expect(draftApiMocks.createVaccinationDraft).not.toHaveBeenCalled();
  });
});
