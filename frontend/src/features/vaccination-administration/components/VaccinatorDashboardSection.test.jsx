import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import VaccinatorDashboardSection from './VaccinatorDashboardSection';

const authMock = vi.hoisted(() => ({
  user: null,
}));

const checkInApiMocks = vi.hoisted(() => ({
  getVaccinatorQueue: vi.fn(),
}));

const draftApiMocks = vi.hoisted(() => ({
  listMyVaccinationDrafts: vi.fn(),
  getVaccinationDraft: vi.fn(),
  createVaccinationDraft: vi.fn(),
  updateVaccinationDraft: vi.fn(),
  discardVaccinationDraft: vi.fn(),
}));

vi.mock('../../../auth/context/AuthContext', () => ({
  useAuth: () => ({ user: authMock.user }),
}));

vi.mock('../../appointment-management/api/checkInApi', () => checkInApiMocks);
vi.mock('../api/vaccinationDraftApi', () => draftApiMocks);

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
  physicalAdministrationConfirmed: true,
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

describe('VaccinatorDashboardSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    authMock.user = {
      fullName: 'Vera Vaccinator',
      roles: ['ROLE_VACCINATOR'],
    };
    draftApiMocks.listMyVaccinationDrafts.mockResolvedValue([]);
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([]);
  });

  afterEach(cleanup);

  it('renders nothing if the user does not have ROLE_VACCINATOR', () => {
    authMock.user = {
      fullName: 'Citizen Charlie',
      roles: ['ROLE_CITIZEN'],
    };
    const { container } = render(<VaccinatorDashboardSection />);
    expect(container.firstChild).toBeNull();
  });

  it('loads my drafts on mount and displays empty state when none exist', async () => {
    render(<VaccinatorDashboardSection />);

    await waitFor(() => {
      expect(draftApiMocks.listMyVaccinationDrafts).toHaveBeenCalledTimes(1);
    });

    expect(screen.getByText('Vaccinator Workspace')).toBeTruthy();
    expect(screen.getByText('Checked-In Queue')).toBeTruthy();
    expect(screen.getByText('My Active Drafts')).toBeTruthy();
    expect(
      screen.getByText(/You have no vaccination drafts yet\. Start one from the checked-in queue\./i),
    ).toBeTruthy();
  });

  it('displays draft error state with retry button when listMyVaccinationDrafts fails', async () => {
    draftApiMocks.listMyVaccinationDrafts.mockRejectedValueOnce(new Error('Network failure loading drafts'));

    render(<VaccinatorDashboardSection />);

    await waitFor(() => {
      expect(screen.getByText('Could Not Load Drafts')).toBeTruthy();
      expect(screen.getByText('Network failure loading drafts')).toBeTruthy();
    });

    // Test retry
    draftApiMocks.listMyVaccinationDrafts.mockResolvedValueOnce([sampleDraft]);
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

    await waitFor(() => {
      expect(screen.getByText('Draft #10')).toBeTruthy();
    });
  });

  it('keeps the newest draft list when an older list request resolves later', async () => {
    const initialList = deferred();
    draftApiMocks.listMyVaccinationDrafts
      .mockReturnValueOnce(initialList.promise)
      .mockResolvedValueOnce([secondDraft]);
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([sampleAppointment]);
    draftApiMocks.createVaccinationDraft.mockResolvedValue({ id: 11 });

    render(<VaccinatorDashboardSection />);

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
    render(<VaccinatorDashboardSection />);

    const loadButton = screen.getByRole('button', { name: 'Load Queue' });
    expect(loadButton.hasAttribute('disabled')).toBe(true);

    const slotInput = screen.getByLabelText(/Time Slot ID/i);
    fireEvent.change(slotInput, { target: { value: '0' } });
    fireEvent.submit(loadButton.closest('form'));

    expect(checkInApiMocks.getVaccinatorQueue).not.toHaveBeenCalled();
  });

  it('loads queue when a valid slot ID is entered and submitted', async () => {
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([sampleAppointment]);

    render(<VaccinatorDashboardSection />);

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

    render(<VaccinatorDashboardSection />);

    const slotInput = screen.getByLabelText(/Time Slot ID/i);
    fireEvent.change(slotInput, { target: { value: '42' } });
    fireEvent.submit(slotInput.closest('form'));

    await waitFor(() => {
      expect(screen.getByText('Appointment #101')).toBeTruthy();
    });

    // Change slot input to something else
    fireEvent.change(slotInput, { target: { value: '43' } });

    // Old queue should be cleared immediately
    expect(screen.queryByText('Appointment #101')).toBeNull();
    expect(screen.getByText('No Slot Selected')).toBeTruthy();
  });

  it('invalidates a pending queue request when slot input changes without another submit', async () => {
    const slotFourRequest = deferred();
    checkInApiMocks.getVaccinatorQueue.mockReturnValueOnce(slotFourRequest.promise);

    render(<VaccinatorDashboardSection />);

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

    render(<VaccinatorDashboardSection />);

    const slotInput = screen.getByLabelText(/Time Slot ID/i);

    // Request 1: Slot 42 (slow)
    fireEvent.change(slotInput, { target: { value: '42' } });
    fireEvent.submit(slotInput.closest('form'));

    // Request 2: Slot 43 (fast)
    fireEvent.change(slotInput, { target: { value: '43' } });
    fireEvent.submit(slotInput.closest('form'));

    await waitFor(() => {
      expect(screen.getByText('Appointment #999')).toBeTruthy();
      expect(screen.getByText('Active Slot #43')).toBeTruthy();
    });

    // Now resolve Request 1
    await act(async () => {
      resolveFirst([{ ...sampleAppointment, id: 101, slotId: 42 }]);
      await firstPromise;
    });

    // Queue should still show Appointment 999 from Request 2, NOT 101 from Request 1
    expect(screen.getByText('Appointment #999')).toBeTruthy();
    expect(screen.queryByText('Appointment #101')).toBeNull();
  });

  it('displays separate error and empty states for the queue', async () => {
    // 1. Error state
    checkInApiMocks.getVaccinatorQueue.mockRejectedValueOnce(new Error('Slot expired or invalid'));

    render(<VaccinatorDashboardSection />);

    const slotInput = screen.getByLabelText(/Time Slot ID/i);
    fireEvent.change(slotInput, { target: { value: '99' } });
    fireEvent.submit(slotInput.closest('form'));

    await waitFor(() => {
      expect(screen.getByText('Error Loading Queue')).toBeTruthy();
      expect(screen.getByText('Slot expired or invalid')).toBeTruthy();
    });

    // 2. Empty state
    checkInApiMocks.getVaccinatorQueue.mockResolvedValueOnce([]);
    fireEvent.change(slotInput, { target: { value: '100' } });
    fireEvent.submit(slotInput.closest('form'));

    await waitFor(() => {
      expect(screen.getByText('No checked-in appointments found for Slot #100.')).toBeTruthy();
    });
  });

  it('starts a vaccination draft and refreshes drafts list', async () => {
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([sampleAppointment]);
    draftApiMocks.createVaccinationDraft.mockResolvedValue({ id: 25 });
    draftApiMocks.listMyVaccinationDrafts
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([{ ...sampleDraft, id: 25 }]);

    render(<VaccinatorDashboardSection />);

    const slotInput = screen.getByLabelText(/Time Slot ID/i);
    fireEvent.change(slotInput, { target: { value: '42' } });
    fireEvent.submit(slotInput.closest('form'));

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Begin vaccination draft for appointment 101' })).toBeTruthy();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Begin vaccination draft for appointment 101' }));

    await waitFor(() => {
      expect(draftApiMocks.createVaccinationDraft).toHaveBeenCalledWith({ appointmentId: 101 });
      expect(screen.getByText('Draft Started')).toBeTruthy();
    });
  });

  it('reports create success separately when the follow-up draft refresh fails', async () => {
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([sampleAppointment]);
    draftApiMocks.createVaccinationDraft.mockResolvedValue({ id: 25 });
    draftApiMocks.listMyVaccinationDrafts
      .mockResolvedValueOnce([])
      .mockRejectedValueOnce(new Error('Draft refresh unavailable'));

    render(<VaccinatorDashboardSection />);
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

    render(<VaccinatorDashboardSection />);
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

  it('opens View modal with initial focus, displays fresh details, and restores focus after Escape', async () => {
    draftApiMocks.listMyVaccinationDrafts.mockResolvedValue([sampleDraft]);
    draftApiMocks.getVaccinationDraft.mockResolvedValue(sampleDraft);

    render(<VaccinatorDashboardSection />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'View vaccination draft 10' })).toBeTruthy();
    });

    const viewButton = screen.getByRole('button', { name: 'View vaccination draft 10' });
    fireEvent.click(viewButton);

    await waitFor(() => {
      expect(draftApiMocks.getVaccinationDraft).toHaveBeenCalledWith(10);
      const dialog = screen.getByRole('dialog');
      expect(dialog).toBeTruthy();
      expect(within(dialog).getByText('Draft #10')).toBeTruthy();
      expect(within(dialog).getByText('Initial notes')).toBeTruthy();
      expect(within(dialog).getByText('Physically Confirmed')).toBeTruthy();
      expect(document.activeElement).toBe(within(dialog).getByRole('button', { name: 'Close dialog' }));
    });

    // Close via Escape key
    fireEvent.keyDown(window, { key: 'Escape', code: 'Escape' });

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).toBeNull();
      expect(document.activeElement).toBe(viewButton);
    });
  });

  it('ignores a stale View response after another draft is opened', async () => {
    const firstDetail = deferred();
    const secondDetail = deferred();
    draftApiMocks.listMyVaccinationDrafts.mockResolvedValue([sampleDraft, secondDraft]);
    draftApiMocks.getVaccinationDraft
      .mockReturnValueOnce(firstDetail.promise)
      .mockReturnValueOnce(secondDetail.promise);

    render(<VaccinatorDashboardSection />);
    const firstButton = await screen.findByRole('button', { name: 'View vaccination draft 10' });
    fireEvent.click(firstButton);
    fireEvent.click(screen.getByRole('button', { name: 'View vaccination draft 11' }));

    secondDetail.resolve(secondDraft);
    await waitFor(() => {
      expect(within(screen.getByRole('dialog')).getByText('Second draft notes')).toBeTruthy();
    });

    await act(async () => {
      firstDetail.resolve(sampleDraft);
      await firstDetail.promise;
    });
    await waitFor(() => {
      const dialog = screen.getByRole('dialog');
      expect(within(dialog).getByText('Draft #11')).toBeTruthy();
      expect(within(dialog).queryByText('Initial notes')).toBeNull();
    });
  });

  it('handles Edit modal: validates no-change, clearing fields, and sends true PATCH with only modified fields', async () => {
    draftApiMocks.listMyVaccinationDrafts.mockResolvedValue([sampleDraft]);
    draftApiMocks.getVaccinationDraft.mockResolvedValue(sampleDraft);
    draftApiMocks.updateVaccinationDraft.mockResolvedValue({ ...sampleDraft, doseNumber: 2 });

    render(<VaccinatorDashboardSection />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Edit vaccination draft 10' })).toBeTruthy();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Edit vaccination draft 10' }));

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeTruthy();
      expect(screen.getByLabelText(/Dose Number/i)).toBeTruthy();
      expect(document.activeElement).toBe(screen.getByLabelText(/Vaccine Batch ID/i));
    });

    const editDialog = screen.getByRole('dialog');
    const editForm = editDialog.querySelector('form');

    // 1. Submit with no changes
    fireEvent.submit(editForm);
    expect(screen.getByText('No changes to save.')).toBeTruthy();
    expect(draftApiMocks.updateVaccinationDraft).not.toHaveBeenCalled();

    // 2. Clear an existing non-empty field (authorizedNotes)
    const notesInput = screen.getByLabelText(/Authorized Notes/i);
    fireEvent.change(notesInput, { target: { value: '' } });
    fireEvent.submit(editForm);
    expect(
      screen.getByText('Clearing this existing value is not supported by the current API.'),
    ).toBeTruthy();
    expect(draftApiMocks.updateVaccinationDraft).not.toHaveBeenCalled();

    // 3. Restore notes and change only doseNumber from 1 to 2
    fireEvent.change(notesInput, { target: { value: 'Initial notes' } });
    const doseInput = screen.getByLabelText(/Dose Number/i);
    fireEvent.change(doseInput, { target: { value: '2' } });

    fireEvent.submit(editForm);

    await waitFor(() => {
      expect(draftApiMocks.updateVaccinationDraft).toHaveBeenCalledWith(10, {
        doseNumber: 2,
      });
      expect(screen.queryByRole('dialog')).toBeNull();
      expect(screen.getByText('Draft Updated')).toBeTruthy();
    });
  });

  it('omits unchanged seconds-bearing administration time and sends a genuine datetime change', async () => {
    draftApiMocks.listMyVaccinationDrafts.mockResolvedValue([sampleDraft]);
    draftApiMocks.getVaccinationDraft.mockResolvedValue(sampleDraft);
    draftApiMocks.updateVaccinationDraft.mockResolvedValue(sampleDraft);

    render(<VaccinatorDashboardSection />);
    fireEvent.click(await screen.findByRole('button', { name: 'Edit vaccination draft 10' }));
    const timeInput = await screen.findByLabelText(/Administration Time/i);
    const doseInput = screen.getByLabelText(/Dose Number/i);

    fireEvent.change(doseInput, { target: { value: '2' } });
    fireEvent.submit(screen.getByRole('dialog').querySelector('form'));
    await waitFor(() => {
      expect(draftApiMocks.updateVaccinationDraft).toHaveBeenLastCalledWith(10, { doseNumber: 2 });
    });

    draftApiMocks.getVaccinationDraft.mockResolvedValue(sampleDraft);
    fireEvent.click(screen.getByRole('button', { name: 'Edit vaccination draft 10' }));
    const reopenedTimeInput = await screen.findByLabelText(/Administration Time/i);
    const changedLocalValue = `${reopenedTimeInput.value.slice(0, 15)}1`;
    fireEvent.change(reopenedTimeInput, { target: { value: changedLocalValue } });
    fireEvent.submit(screen.getByRole('dialog').querySelector('form'));

    await waitFor(() => {
      expect(draftApiMocks.updateVaccinationDraft).toHaveBeenLastCalledWith(10, {
        administrationTime: new Date(changedLocalValue).toISOString(),
      });
    });
    expect(timeInput.value).toBeTruthy();
  });

  it('keeps Edit detail and PATCH identity bound to the newest opened draft', async () => {
    const firstDetail = deferred();
    const secondDetail = deferred();
    draftApiMocks.listMyVaccinationDrafts.mockResolvedValue([sampleDraft, secondDraft]);
    draftApiMocks.getVaccinationDraft
      .mockReturnValueOnce(firstDetail.promise)
      .mockReturnValueOnce(secondDetail.promise);
    draftApiMocks.updateVaccinationDraft.mockResolvedValue(secondDraft);

    render(<VaccinatorDashboardSection />);
    fireEvent.click(await screen.findByRole('button', { name: 'Edit vaccination draft 10' }));
    fireEvent.click(screen.getByRole('button', { name: 'Edit vaccination draft 11' }));

    secondDetail.resolve(secondDraft);
    const doseInput = await screen.findByLabelText(/Dose Number/i);
    await act(async () => {
      firstDetail.resolve(sampleDraft);
      await firstDetail.promise;
    });
    fireEvent.change(doseInput, { target: { value: '2' } });
    fireEvent.submit(screen.getByRole('dialog').querySelector('form'));

    await waitFor(() => {
      expect(draftApiMocks.updateVaccinationDraft).toHaveBeenCalledWith(11, { doseNumber: 2 });
    });
    expect(draftApiMocks.updateVaccinationDraft).not.toHaveBeenCalledWith(10, expect.anything());
  });

  it('blocks PATCH when the active dialog ID and fetched baseline ID do not match', async () => {
    draftApiMocks.listMyVaccinationDrafts.mockResolvedValue([sampleDraft]);
    draftApiMocks.getVaccinationDraft.mockResolvedValue(secondDraft);

    render(<VaccinatorDashboardSection />);
    fireEvent.click(await screen.findByRole('button', { name: 'Edit vaccination draft 10' }));
    const doseInput = await screen.findByLabelText(/Dose Number/i);
    fireEvent.change(doseInput, { target: { value: '2' } });
    fireEvent.submit(screen.getByRole('dialog').querySelector('form'));

    expect(screen.getByText(/draft context is stale/i)).toBeTruthy();
    expect(draftApiMocks.updateVaccinationDraft).not.toHaveBeenCalled();
  });

  it('handles Discard modal: requires reason, preserves reason and stays open on error, closes on success', async () => {
    draftApiMocks.listMyVaccinationDrafts.mockResolvedValue([sampleDraft]);

    render(<VaccinatorDashboardSection />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Discard vaccination draft 10' })).toBeTruthy();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Discard vaccination draft 10' }));

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeTruthy();
      expect(screen.getByLabelText(/Audit Reason/i)).toBeTruthy();
      expect(document.activeElement).toBe(screen.getByLabelText(/Audit Reason/i));
    });

    const closeButton = within(screen.getByRole('dialog')).getByRole('button', { name: 'Close dialog' });
    const discardButton = within(screen.getByRole('dialog')).getByRole('button', { name: 'Discard Draft' });
    closeButton.focus();
    fireEvent.keyDown(window, { key: 'Tab', shiftKey: true });
    expect(document.activeElement).toBe(discardButton);

    const discardDialog = screen.getByRole('dialog');
    const discardForm = discardDialog.querySelector('form');

    // 1. Submit without reason
    fireEvent.submit(discardForm);
    expect(screen.getByText(/A deletion reason is required to discard a vaccination draft\./i)).toBeTruthy();
    expect(draftApiMocks.discardVaccinationDraft).not.toHaveBeenCalled();

    // 2. Type reason, API fails
    const reasonInput = screen.getByLabelText(/Audit Reason/i);
    fireEvent.change(reasonInput, { target: { value: 'Wrong patient identified' } });

    draftApiMocks.discardVaccinationDraft.mockRejectedValueOnce(new Error('Server error discarding draft'));
    fireEvent.submit(discardForm);

    await waitFor(() => {
      expect(screen.getByText('Server error discarding draft')).toBeTruthy();
      // Dialog must remain open
      expect(screen.getByRole('dialog')).toBeTruthy();
      // Reason must be preserved
      expect(screen.getByLabelText(/Audit Reason/i).value).toBe('Wrong patient identified');
    });

    // 3. Retry with successful discard
    draftApiMocks.discardVaccinationDraft.mockResolvedValueOnce({ status: 204 });
    fireEvent.submit(discardForm);

    await waitFor(() => {
      expect(draftApiMocks.discardVaccinationDraft).toHaveBeenCalledWith(10, 'Wrong patient identified');
      expect(screen.queryByRole('dialog')).toBeNull();
      expect(screen.getByText('Draft Discarded')).toBeTruthy();
    });
  });

  it('does not close a replacement dialog when an earlier discard completes', async () => {
    const discardRequest = deferred();
    draftApiMocks.listMyVaccinationDrafts.mockResolvedValue([sampleDraft, secondDraft]);
    draftApiMocks.discardVaccinationDraft.mockReturnValue(discardRequest.promise);
    draftApiMocks.getVaccinationDraft.mockResolvedValue(secondDraft);

    render(<VaccinatorDashboardSection />);
    fireEvent.click(await screen.findByRole('button', { name: 'Discard vaccination draft 10' }));
    fireEvent.change(screen.getByLabelText(/Audit Reason/i), { target: { value: 'Duplicate draft' } });
    fireEvent.submit(screen.getByRole('dialog').querySelector('form'));

    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Close dialog' }));
    fireEvent.click(screen.getByRole('button', { name: 'View vaccination draft 11' }));
    await waitFor(() => {
      expect(within(screen.getByRole('dialog')).getByText('Second draft notes')).toBeTruthy();
    });

    await act(async () => {
      discardRequest.resolve(null);
      await discardRequest.promise;
    });
    await waitFor(() => {
      expect(within(screen.getByRole('dialog')).getByText('Draft #11')).toBeTruthy();
    });
  });
});
