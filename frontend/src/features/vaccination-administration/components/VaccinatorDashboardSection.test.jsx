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
  slotId: 42,
  status: 'CHECKED_IN',
  bookedAt: '2026-09-19T09:00:00Z',
};

const secondAppointment = {
  ...sampleAppointment,
  id: 102,
  citizenId: 502,
};

function renderSection() {
  return render(
    <MemoryRouter>
      <VaccinatorDashboardSection />
    </MemoryRouter>,
  );
}

describe('VaccinatorDashboardSection', () => {
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
    const { container } = renderSection();
    expect(container.firstChild).toBeNull();
  });

  it('loads my drafts on mount and displays empty state with Open Vaccination Drafts link', async () => {
    renderSection();

    await waitFor(() => {
      expect(draftApiMocks.listMyVaccinationDrafts).toHaveBeenCalledTimes(1);
    });

    expect(screen.getByText('Vaccinator Workspace')).toBeTruthy();
    expect(screen.getByText('Checked-In Queue')).toBeTruthy();
    expect(screen.getByText('My Active Drafts')).toBeTruthy();

    const draftsHeading = screen.getByRole('heading', { name: 'My Active Drafts' });
    const draftsCard = draftsHeading.closest('.card');
    expect(within(draftsCard).getByText('0')).toBeTruthy();
    expect(screen.getByText('No active vaccination drafts.')).toBeTruthy();

    const openLink = screen.getByRole('link', { name: 'Open Vaccination Drafts' });
    expect(openLink).toBeTruthy();
    expect(openLink.getAttribute('href')).toBe('/app/staff/vaccination-drafts');
  });

  it('displays draft error state with retry button when listMyVaccinationDrafts fails', async () => {
    draftApiMocks.listMyVaccinationDrafts.mockRejectedValueOnce(new Error('Network failure loading drafts'));

    renderSection();

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

    // Badge count within My Active Drafts should be 2, not 3
    const draftsHeading = screen.getByRole('heading', { name: 'My Active Drafts' });
    const draftsCard = draftsHeading.closest('.card');
    expect(within(draftsCard).getByText('2')).toBeTruthy();

    // Draft #12 is FINALIZED and must not appear
    expect(screen.queryByText('Draft #12')).toBeNull();

    // Verify status labels
    expect(screen.getByText('In Progress')).toBeTruthy();
    expect(screen.getByText('Ready for Finalization')).toBeTruthy();

    // Summary view must NOT contain mutation/lifecycle action buttons
    expect(screen.queryByRole('button', { name: /edit/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /discard/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /confirm/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /finalize/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /view/i })).toBeNull();

    // Open Vaccination Drafts link must be rendered
    const openLink = screen.getByRole('link', { name: 'Open Vaccination Drafts' });
    expect(openLink).toBeTruthy();
    expect(openLink.getAttribute('href')).toBe('/app/staff/vaccination-drafts');
  });

  it('keeps the newest draft list when an older list request resolves later', async () => {
    const initialList = deferred();
    draftApiMocks.listMyVaccinationDrafts
      .mockReturnValueOnce(initialList.promise)
      .mockResolvedValueOnce([secondDraft]);
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([sampleAppointment]);
    draftApiMocks.createVaccinationDraft.mockResolvedValue({ id: 11 });

    renderSection();

    const slotInput = screen.getByLabelText(/Time Slot ID/i);
    fireEvent.change(slotInput, { target: { value: '42' } });
    fireEvent.submit(slotInput.closest('form'));
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Begin vaccination draft for appointment 101' })).toBeTruthy();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Begin vaccination draft for appointment 101' }));
    await waitFor(() => {
      expect(screen.getByText('Draft #11')).toBeTruthy();
    });

    await act(async () => {
      initialList.resolve([sampleDraft]);
      await initialList.promise;
    });
    await waitFor(() => {
      expect(screen.getByText('Draft #11')).toBeTruthy();
      expect(screen.queryByText('Draft #10')).toBeNull();
    });
  });

  it('does not load queue if slot ID is empty or non-positive', () => {
    renderSection();

    const loadButton = screen.getByRole('button', { name: 'Load Queue' });
    expect(loadButton.hasAttribute('disabled')).toBe(true);

    const slotInput = screen.getByLabelText(/Time Slot ID/i);
    fireEvent.change(slotInput, { target: { value: '0' } });
    fireEvent.submit(loadButton.closest('form'));

    expect(checkInApiMocks.getVaccinatorQueue).not.toHaveBeenCalled();
  });

  it('loads queue when a valid slot ID is entered and submitted', async () => {
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([sampleAppointment]);

    renderSection();

    const slotInput = screen.getByLabelText(/Time Slot ID/i);
    fireEvent.change(slotInput, { target: { value: '42' } });
    fireEvent.submit(slotInput.closest('form'));

    await waitFor(() => {
      expect(checkInApiMocks.getVaccinatorQueue).toHaveBeenCalledWith(42);
    });

    await waitFor(() => {
      expect(screen.getByText('Active Slot #42')).toBeTruthy();
      expect(screen.getByText('Appointment #101')).toBeTruthy();
      expect(
        screen.getByRole('button', { name: 'Begin vaccination draft for appointment 101' }),
      ).toBeTruthy();
    });
  });

  it('clears previous queue immediately when slot input is changed', async () => {
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([sampleAppointment]);

    renderSection();

    const slotInput = screen.getByLabelText(/Time Slot ID/i);
    fireEvent.change(slotInput, { target: { value: '42' } });
    fireEvent.submit(slotInput.closest('form'));

    await waitFor(() => {
      expect(screen.getByText('Appointment #101')).toBeTruthy();
    });

    fireEvent.change(slotInput, { target: { value: '43' } });

    expect(screen.queryByText('Appointment #101')).toBeNull();
    expect(screen.getByText('No Slot Selected')).toBeTruthy();
  });

  it('invalidates a pending queue request when slot input changes without another submit', async () => {
    const slotFourRequest = deferred();
    checkInApiMocks.getVaccinatorQueue.mockReturnValueOnce(slotFourRequest.promise);

    renderSection();

    const slotInput = screen.getByLabelText(/Time Slot ID/i);
    fireEvent.change(slotInput, { target: { value: '4' } });
    fireEvent.submit(slotInput.closest('form'));

    await waitFor(() => {
      expect(checkInApiMocks.getVaccinatorQueue).toHaveBeenCalledWith(4);
    });

    fireEvent.change(slotInput, { target: { value: '42' } });
    await act(async () => {
      slotFourRequest.resolve([{ ...sampleAppointment, id: 4, slotId: 4 }]);
      await slotFourRequest.promise;
    });

    await waitFor(() => {
      expect(screen.queryByText('Appointment #4')).toBeNull();
      expect(screen.getByText('No Slot Selected')).toBeTruthy();
    });
  });

  it('prevents race conditions with monotonic queue request tracking', async () => {
    let resolveFirst;
    const firstPromise = new Promise((resolve) => {
      resolveFirst = resolve;
    });

    checkInApiMocks.getVaccinatorQueue
      .mockImplementationOnce(() => firstPromise)
      .mockResolvedValueOnce([
        { ...sampleAppointment, id: 999, slotId: 43 },
      ]);

    renderSection();

    const slotInput = screen.getByLabelText(/Time Slot ID/i);

    fireEvent.change(slotInput, { target: { value: '42' } });
    fireEvent.submit(slotInput.closest('form'));

    fireEvent.change(slotInput, { target: { value: '43' } });
    fireEvent.submit(slotInput.closest('form'));

    await waitFor(() => {
      expect(screen.getByText('Appointment #999')).toBeTruthy();
      expect(screen.getByText('Active Slot #43')).toBeTruthy();
    });

    await act(async () => {
      resolveFirst([{ ...sampleAppointment, id: 101, slotId: 42 }]);
      await firstPromise;
    });

    expect(screen.getByText('Appointment #999')).toBeTruthy();
    expect(screen.queryByText('Appointment #101')).toBeNull();
  });

  it('displays separate error and empty states for the queue', async () => {
    checkInApiMocks.getVaccinatorQueue.mockRejectedValueOnce(new Error('Slot expired or invalid'));

    renderSection();

    const slotInput = screen.getByLabelText(/Time Slot ID/i);
    fireEvent.change(slotInput, { target: { value: '99' } });
    fireEvent.submit(slotInput.closest('form'));

    await waitFor(() => {
      expect(screen.getByText('Error Loading Queue')).toBeTruthy();
      expect(screen.getByText('Slot expired or invalid')).toBeTruthy();
    });

    checkInApiMocks.getVaccinatorQueue.mockResolvedValueOnce([]);
    fireEvent.change(slotInput, { target: { value: '100' } });
    fireEvent.submit(slotInput.closest('form'));

    await waitFor(() => {
      expect(screen.getByText('No checked-in appointments found for Slot #100.')).toBeTruthy();
    });
  });

  it('starts a vaccination draft with local calendar administration date and refreshes drafts list', async () => {
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([sampleAppointment]);
    draftApiMocks.createVaccinationDraft.mockResolvedValue({ id: 25 });
    draftApiMocks.listMyVaccinationDrafts
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([{ ...sampleDraft, id: 25 }]);

    renderSection();

    const slotInput = screen.getByLabelText(/Time Slot ID/i);
    fireEvent.change(slotInput, { target: { value: '42' } });
    fireEvent.submit(slotInput.closest('form'));

    await waitFor(() => {
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

  it('reports create success separately when the follow-up draft refresh fails', async () => {
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([sampleAppointment]);
    draftApiMocks.createVaccinationDraft.mockResolvedValue({ id: 25 });
    draftApiMocks.listMyVaccinationDrafts
      .mockResolvedValueOnce([])
      .mockRejectedValueOnce(new Error('Draft refresh unavailable'));

    renderSection();
    const slotInput = screen.getByLabelText(/Time Slot ID/i);
    fireEvent.change(slotInput, { target: { value: '42' } });
    fireEvent.submit(slotInput.closest('form'));
    fireEvent.click(await screen.findByRole('button', { name: 'Begin vaccination draft for appointment 101' }));

    await waitFor(() => {
      expect(screen.getByText('Draft Started')).toBeTruthy();
      expect(screen.getByText(/draft created.+could not be refreshed/i)).toBeTruthy();
      expect(screen.queryByText('Draft Not Started')).toBeNull();
    });
  });

  it('tracks simultaneous draft creation independently per appointment', async () => {
    const firstCreate = deferred();
    const secondCreate = deferred();
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([sampleAppointment, secondAppointment]);
    draftApiMocks.createVaccinationDraft
      .mockReturnValueOnce(firstCreate.promise)
      .mockReturnValueOnce(secondCreate.promise);

    renderSection();
    const slotInput = screen.getByLabelText(/Time Slot ID/i);
    fireEvent.change(slotInput, { target: { value: '42' } });
    fireEvent.submit(slotInput.closest('form'));

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
});
