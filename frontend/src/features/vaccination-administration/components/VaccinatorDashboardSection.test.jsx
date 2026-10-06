import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import VaccinatorDashboardSection from './VaccinatorDashboardSection';
import { getLocalCalendarToday, toApiIsoDate } from '../utils/dateUtils';

const authMock = vi.hoisted(() => ({
  user: null,
}));

const checkInApiMocks = vi.hoisted(() => ({
  getVaccinatorQueue: vi.fn(),
}));

const draftApiMocks = vi.hoisted(() => ({
  listMyVaccinationDrafts: vi.fn(),
  createVaccinationDraft: vi.fn(),
}));

const certificateApiMocks = vi.hoisted(() => ({
  listVaccinatorCorrectionRequests: vi.fn(),
}));

vi.mock('../../../auth/context/AuthContext', () => ({
  useAuth: () => ({ user: authMock.user }),
}));

vi.mock('../../appointment-management/api/checkInApi', () => checkInApiMocks);
vi.mock('../api/vaccinationDraftApi', () => draftApiMocks);
vi.mock('../api/certificateApi', () => certificateApiMocks);

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}

const mockSessions = [
  {
    id: 1,
    slotId: 1,
    centerName: 'Colombo Central Vaccination Center',
    date: '06 Oct 2026',
    startTime: '09:00',
    endTime: '12:00',
    status: 'ACTIVE',
  },
  {
    id: 2,
    slotId: 2,
    centerName: 'Colombo Central Vaccination Center',
    date: '06 Oct 2026',
    startTime: '12:00',
    endTime: '15:00',
    status: 'ACTIVE',
  },
];

const sampleDraft = {
  id: 10,
  appointmentId: 101,
  vaccineBatchId: 5,
  doseNumber: 1,
  administrationTime: '2026-09-19T10:00:45.123Z',
  nextDoseDate: '2026-10-19',
  physicalAdministrationConfirmed: false,
  authorizedNotes: 'Initial notes',
  status: 'IN_PROGRESS',
  createdAt: '2026-09-19T09:30:00.000Z',
  updatedAt: '2026-09-19T09:35:00.000Z',
};

const secondDraft = {
  ...sampleDraft,
  id: 11,
  appointmentId: 102,
  vaccineBatchId: 6,
  authorizedNotes: 'Second draft notes',
  status: 'READY_FOR_FINALIZATION',
  physicalAdministrationConfirmed: true,
};

const finalizedDraft = {
  ...sampleDraft,
  id: 12,
  appointmentId: 103,
  status: 'FINALIZED',
  physicalAdministrationConfirmed: true,
};

const sampleAppointment = {
  id: 101,
  citizenId: 501,
  campaignId: 301,
  slotId: 1,
  status: 'CHECKED_IN',
  bookedAt: '2026-09-19T09:00:00Z',
};

const secondAppointment = {
  ...sampleAppointment,
  id: 102,
  citizenId: 502,
  slotId: 1,
};

function renderSection(props = {}) {
  return render(
    <MemoryRouter>
      <VaccinatorDashboardSection {...props} />
    </MemoryRouter>,
  );
}

describe('VaccinatorDashboardSection (Option B)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    authMock.user = {
      fullName: 'Vera Vaccinator',
      roles: ['ROLE_VACCINATOR'],
    };
    draftApiMocks.listMyVaccinationDrafts.mockResolvedValue([]);
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([]);
    certificateApiMocks.listVaccinatorCorrectionRequests.mockResolvedValue([]);
  });

  afterEach(cleanup);

  it('renders nothing if the user does not have ROLE_VACCINATOR', () => {
    authMock.user = {
      fullName: 'Citizen Charlie',
      roles: ['ROLE_CITIZEN'],
    };
    const { container } = renderSection({ sessions: mockSessions });
    expect(container.firstChild).toBeNull();
  });

  it('renders Option B header, active session card, summary metrics, and task-oriented workspace when sessions are provided', async () => {
    renderSection({ sessions: mockSessions });

    await waitFor(() => {
      expect(draftApiMocks.listMyVaccinationDrafts).toHaveBeenCalledTimes(1);
    });

    // Option B Header
    expect(screen.getByRole('heading', { name: 'Vaccinator Dashboard' })).toBeTruthy();
    expect(screen.getByText(/Welcome, Vera Vaccinator • Vaccinator/)).toBeTruthy();
    expect(screen.getByText("Manage today's vaccination work")).toBeTruthy();

    // Option B Active Session Card
    expect(screen.getByText('Current Session')).toBeTruthy();
    expect(screen.getByText('Active Session')).toBeTruthy();
    expect(screen.getByText('Colombo Central Vaccination Center')).toBeTruthy();
    expect(screen.getByText(/06 Oct 2026 · 09:00 – 12:00/)).toBeTruthy();

    // Summary Metric Cards
    expect(screen.getByText('Waiting in Queue')).toBeTruthy();
    expect(screen.getByText('Active Drafts')).toBeTruthy();
    expect(screen.getByText('Corrections Needed')).toBeTruthy();

    // Two-Column Task Workspace
    expect(screen.getByRole('heading', { name: 'Checked-In Queue' })).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Active Work' })).toBeTruthy();

    // Raw Slot ID input should be completely absent
    expect(screen.queryByLabelText(/Time Slot ID/i)).toBeNull();
    expect(screen.queryByRole('button', { name: 'Load Queue' })).toBeNull();
    expect(screen.queryByText(/Morning Slot \(Slot #1\)/i)).toBeNull();

    // Open Vaccination Drafts link
    const openLink = screen.getByRole('link', { name: 'Open Vaccination Drafts' });
    expect(openLink).toBeTruthy();
    expect(openLink.getAttribute('href')).toBe('/app/staff/vaccination-drafts');
  });

  it('displays empty state when no session is assigned or sessions prop is omitted', async () => {
    renderSection();

    await waitFor(() => {
      expect(screen.getByText('No active vaccination session is currently assigned.')).toBeTruthy();
      expect(screen.getByText('No Active Session')).toBeTruthy();
      expect(screen.getByText('You can continue working with active drafts and approved corrections.')).toBeTruthy();
      expect(screen.getByText('Select or receive an assigned vaccination session to view the queue.')).toBeTruthy();
    });

    expect(screen.queryByText('Active Session')).toBeNull();
    expect(checkInApiMocks.getVaccinatorQueue).not.toHaveBeenCalled();
  });

  it('displays empty state when sessions prop is empty array', async () => {
    renderSection({ sessions: [] });

    await waitFor(() => {
      expect(screen.getByText('No active vaccination session is currently assigned.')).toBeTruthy();
      expect(screen.getByText('Select or receive an assigned vaccination session to view the queue.')).toBeTruthy();
    });

    expect(screen.queryByText('Active Session')).toBeNull();
    expect(checkInApiMocks.getVaccinatorQueue).not.toHaveBeenCalled();
  });

  it('auto-selects single session without rendering session dropdown', async () => {
    const singleSession = [mockSessions[0]];
    renderSection({ sessions: singleSession });

    await waitFor(() => {
      expect(screen.getByText('Active Session')).toBeTruthy();
      expect(screen.getByText('Colombo Central Vaccination Center')).toBeTruthy();
    });

    // Dropdown should NOT be shown when only 1 session exists
    expect(screen.queryByLabelText('Change Session')).toBeNull();
    expect(checkInApiMocks.getVaccinatorQueue).toHaveBeenCalledWith(1);
  });

  it('allows switching sessions via dropdown and auto-loads queue for selected session', async () => {
    renderSection({ sessions: mockSessions });

    await waitFor(() => {
      expect(checkInApiMocks.getVaccinatorQueue).toHaveBeenCalledWith(1);
    });

    const selector = screen.getByLabelText('Change Session');
    expect(selector).toBeTruthy();

    fireEvent.change(selector, { target: { value: '2' } });

    await waitFor(() => {
      expect(checkInApiMocks.getVaccinatorQueue).toHaveBeenCalledWith(2);
      expect(screen.getAllByText(/12:00 – 15:00/).length).toBeGreaterThan(0);
    });
  });

  it('displays draft error state with retry button when listMyVaccinationDrafts fails', async () => {
    draftApiMocks.listMyVaccinationDrafts.mockRejectedValueOnce(new Error('Network failure loading drafts'));

    renderSection({ sessions: mockSessions });

    await waitFor(() => {
      expect(screen.getByText('Could Not Load Drafts')).toBeTruthy();
      expect(screen.getByText('Network failure loading drafts')).toBeTruthy();
    });

    draftApiMocks.listMyVaccinationDrafts.mockResolvedValueOnce([sampleDraft]);
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

    await waitFor(() => {
      expect(screen.getByText('Draft #10')).toBeTruthy();
    });
  });

  it('renders active drafts only (IN_PROGRESS, READY_FOR_FINALIZATION) and excludes FINALIZED drafts', async () => {
    draftApiMocks.listMyVaccinationDrafts.mockResolvedValue([
      sampleDraft,
      secondDraft,
      finalizedDraft,
    ]);

    renderSection();

    await waitFor(() => {
      expect(screen.getByText('Draft #10')).toBeTruthy();
      expect(screen.getByText('Draft #11')).toBeTruthy();
    });

    // Draft #12 is FINALIZED and must not appear
    expect(screen.queryByText('Draft #12')).toBeNull();

    // Verify status labels
    expect(screen.getByText('In Progress')).toBeTruthy();
    expect(screen.getByText('Ready for Finalization')).toBeTruthy();

    // Summary view must NOT contain mutation action buttons
    expect(screen.queryByRole('button', { name: /edit/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /discard/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /confirm/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /finalize/i })).toBeNull();

    // Open Draft link must be rendered
    const openDraftLinks = screen.getAllByRole('link', { name: 'Open Draft' });
    expect(openDraftLinks.length).toBe(2);
  });

  it('displays Action Required banner and Open Correction link when approved corrections exist', async () => {
    certificateApiMocks.listVaccinatorCorrectionRequests.mockResolvedValue([
      { id: 1, status: 'APPROVED', citizenName: 'Jane Citizen', reason: 'Typo' },
      { id: 2, status: 'PENDING', citizenName: 'John Citizen', reason: 'Review pending' },
    ]);

    renderSection();

    await waitFor(() => {
      expect(screen.getByText('ACTION REQUIRED')).toBeTruthy();
      expect(screen.getByText(/1 correction/)).toBeTruthy();
    });

    const openCorrLink = screen.getByRole('link', { name: 'Open Correction' });
    expect(openCorrLink).toBeTruthy();
    expect(openCorrLink.getAttribute('href')).toBe('/app/staff/correction-requests');
  });

  it('renders checked-in queue and allows vaccinator to begin vaccination draft', async () => {
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([sampleAppointment]);
    draftApiMocks.createVaccinationDraft.mockResolvedValue({ id: 25 });
    draftApiMocks.listMyVaccinationDrafts
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([{ ...sampleDraft, id: 25 }]);

    renderSection({ sessions: mockSessions });

    await waitFor(() => {
      expect(screen.getByText('Appointment #101')).toBeTruthy();
      expect(screen.getByRole('button', { name: 'Begin vaccination draft for appointment 101' })).toBeTruthy();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Begin vaccination draft for appointment 101' }));

    const expectedDate = toApiIsoDate(getLocalCalendarToday());
    await waitFor(() => {
      expect(draftApiMocks.createVaccinationDraft).toHaveBeenCalledWith({
        appointmentId: 101,
        administrationTime: expectedDate,
      });
      expect(screen.getByText('Draft Started')).toBeTruthy();
    });
  });

  it('tracks simultaneous draft creation independently per appointment', async () => {
    const firstCreate = deferred();
    const secondCreate = deferred();
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([sampleAppointment, secondAppointment]);
    draftApiMocks.createVaccinationDraft
      .mockReturnValueOnce(firstCreate.promise)
      .mockReturnValueOnce(secondCreate.promise);

    renderSection({ sessions: mockSessions });

    const firstButton = await screen.findByRole('button', { name: 'Begin vaccination draft for appointment 101' });
    const secondButton = screen.getByRole('button', { name: 'Begin vaccination draft for appointment 102' });
    fireEvent.click(firstButton);
    fireEvent.click(secondButton);

    expect(firstButton.disabled).toBe(true);
    expect(secondButton.disabled).toBe(true);
    expect(draftApiMocks.createVaccinationDraft).toHaveBeenCalledTimes(2);

    firstCreate.resolve({ id: 20 });
    await waitFor(() => {
      expect(firstButton.disabled).toBe(false);
      expect(secondButton.disabled).toBe(true);
    });

    secondCreate.resolve({ id: 21 });
    await waitFor(() => {
      expect(secondButton.disabled).toBe(false);
    });
  });

  it('displays separate error and empty states for the queue', async () => {
    checkInApiMocks.getVaccinatorQueue.mockRejectedValueOnce(new Error('Network error loading queue'));

    renderSection({ sessions: mockSessions });

    await waitFor(() => {
      expect(screen.getByText('Error Loading Queue')).toBeTruthy();
      expect(screen.getByText('Network error loading queue')).toBeTruthy();
    });

    checkInApiMocks.getVaccinatorQueue.mockResolvedValueOnce([]);
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

    await waitFor(() => {
      expect(screen.getByText('No citizens are currently waiting for this session.')).toBeTruthy();
    });
  });

  it('reports create success separately when the follow-up draft refresh fails', async () => {
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([sampleAppointment]);
    draftApiMocks.createVaccinationDraft.mockResolvedValue({ id: 25 });
    draftApiMocks.listMyVaccinationDrafts
      .mockResolvedValueOnce([])
      .mockRejectedValueOnce(new Error('Draft refresh unavailable'));

    renderSection({ sessions: mockSessions });

    fireEvent.click(await screen.findByRole('button', { name: 'Begin vaccination draft for appointment 101' }));

    await waitFor(() => {
      expect(screen.getByText('Draft Started')).toBeTruthy();
      expect(screen.getByText(/draft created.+could not be refreshed/i)).toBeTruthy();
      expect(screen.queryByText('Draft Not Started')).toBeNull();
    });
  });
});
