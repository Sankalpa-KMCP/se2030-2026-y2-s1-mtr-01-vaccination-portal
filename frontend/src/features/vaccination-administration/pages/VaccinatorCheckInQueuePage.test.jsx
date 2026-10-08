import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import VaccinatorCheckInQueuePage from './VaccinatorCheckInQueuePage';

const navigateMock = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => navigateMock,
  };
});

const checkInApiMocks = vi.hoisted(() => ({
  getMyAssignedVaccinationSessions: vi.fn(),
  getVaccinatorQueue: vi.fn(),
}));

const vaccinationDraftApiMocks = vi.hoisted(() => ({
  createVaccinationDraft: vi.fn(),
  listMyVaccinationDrafts: vi.fn(),
}));

vi.mock('../../appointment-management/api/checkInApi', () => checkInApiMocks);
vi.mock('../api/vaccinationDraftApi', () => vaccinationDraftApiMocks);

const mockSessions = [
  {
    id: 1,
    slotId: 10,
    centerName: 'Colombo Central Vaccination Center',
    date: '06 Oct 2026',
    startTime: '09:00',
    endTime: '12:00',
    status: 'ACTIVE',
  },
  {
    id: 2,
    slotId: 20,
    centerName: 'Colombo Central Vaccination Center',
    date: '06 Oct 2026',
    startTime: '12:00',
    endTime: '15:00',
    status: 'ACTIVE',
  },
];

const sampleAppointment = {
  id: 112,
  citizenId: 501,
  citizenName: 'Kasun Perera',
  campaignId: 201,
  campaignName: 'National COVID-19 Booster Campaign 2026',
  slotId: 14,
  status: 'CHECKED_IN',
  bookedAt: '2026-10-06T09:15:00Z',
};

function renderPage(props = {}) {
  return render(
    <MemoryRouter>
      <VaccinatorCheckInQueuePage {...props} />
    </MemoryRouter>
  );
}

describe('VaccinatorCheckInQueuePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    navigateMock.mockReset();
    checkInApiMocks.getMyAssignedVaccinationSessions.mockResolvedValue(mockSessions);
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([]);
    vaccinationDraftApiMocks.listMyVaccinationDrafts.mockResolvedValue([]);
    vaccinationDraftApiMocks.createVaccinationDraft.mockResolvedValue({ id: 1 });
  });

  afterEach(() => {
    cleanup();
  });

  it('renders page title and subtitle', async () => {
    renderPage({ sessions: mockSessions });

    expect(screen.getByRole('heading', { name: 'Check-In Queue' })).toBeTruthy();
    expect(screen.getByText('Citizens checked in and ready for vaccination.')).toBeTruthy();
  });

  it('loads the signed-in vaccinator assigned sessions when none are provided', async () => {
    renderPage();

    await waitFor(() => {
      expect(checkInApiMocks.getMyAssignedVaccinationSessions).toHaveBeenCalledTimes(1);
      expect(checkInApiMocks.getVaccinatorQueue).toHaveBeenCalledWith(10);
    });
  });

  it('does NOT contain raw manual Time Slot ID input', async () => {
    renderPage({ sessions: mockSessions });

    expect(screen.queryByLabelText(/Time Slot ID/i)).toBeNull();
    expect(screen.queryByPlaceholderText(/Time Slot/i)).toBeNull();
    expect(screen.queryByRole('button', { name: /Load Queue/i })).toBeNull();
  });

  it('does NOT render check-in mutation buttons (Check In, Reverse, No-Show)', async () => {
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([sampleAppointment]);
    renderPage({ sessions: mockSessions });

    await waitFor(() => {
      expect(screen.getByText('Appointment #112')).toBeTruthy();
    });

    expect(screen.queryByRole('button', { name: /^Check In$/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /Reverse Check-In/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /Mark No-Show/i })).toBeNull();
    expect(screen.queryByLabelText(/Staff Override/i)).toBeNull();
  });

  it('renders compact empty queue message when no patients are checked in', async () => {
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([]);
    renderPage({ sessions: mockSessions });

    await waitFor(() => {
      expect(screen.getByText('No checked-in patients are currently ready for vaccination.')).toBeTruthy();
    });
  });

  it('renders checked-in appointments and "Start Vaccination" button', async () => {
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([sampleAppointment]);
    renderPage({ sessions: mockSessions });

    await waitFor(() => {
      expect(screen.getByText('Appointment #112')).toBeTruthy();
      expect(screen.getByText('Kasun Perera')).toBeTruthy();
      expect(screen.getByText('National COVID-19 Booster Campaign 2026')).toBeTruthy();
      expect(screen.getByRole('button', { name: /Start vaccination for appointment 112/i })).toBeTruthy();
    });
  });

  it('navigates to drafts without calling createVaccinationDraft when "Start Vaccination" is clicked', async () => {
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([sampleAppointment]);
    renderPage({ sessions: mockSessions });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Start vaccination for appointment 112/i })).toBeTruthy();
    });

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /Start vaccination for appointment 112/i }));
    });

    expect(vaccinationDraftApiMocks.createVaccinationDraft).not.toHaveBeenCalled();
    expect(navigateMock).toHaveBeenCalledWith(
      '/app/staff/vaccination-drafts',
      expect.objectContaining({
        state: { preselectedAppointment: sampleAppointment },
      })
    );
  });

  it('navigates to drafts directly when draft already exists for appointment', async () => {
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([sampleAppointment]);
    vaccinationDraftApiMocks.listMyVaccinationDrafts.mockResolvedValue([
      { id: 99, appointmentId: 112, status: 'IN_PROGRESS' },
    ]);

    renderPage({ sessions: mockSessions });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Start vaccination for appointment 112/i })).toBeTruthy();
      expect(screen.getByText('Open Draft')).toBeTruthy();
    });

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /Start vaccination for appointment 112/i }));
    });

    expect(vaccinationDraftApiMocks.createVaccinationDraft).not.toHaveBeenCalled();
    expect(navigateMock).toHaveBeenCalledWith(
      '/app/staff/vaccination-drafts',
      expect.objectContaining({
        state: { preselectedAppointment: sampleAppointment },
      })
    );
  });

  it('displays message when no sessions are assigned', async () => {
    renderPage({ sessions: [] });

    expect(screen.getByText('No active vaccination session is currently assigned to you.')).toBeTruthy();
  });
});
