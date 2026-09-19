import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
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

const sampleDraft = {
  id: 10,
  appointmentId: 101,
  vaccineBatchId: 5,
  doseNumber: 1,
  administrationTime: '2026-09-19T10:00:00.000Z',
  nextDoseDate: '2026-10-19',
  physicalAdministrationConfirmed: true,
  authorizedNotes: 'Initial notes',
  status: 'IN_PROGRESS',
  createdAt: '2026-09-19T09:30:00.000Z',
  updatedAt: '2026-09-19T09:35:00.000Z',
};

const sampleAppointment = {
  id: 101,
  citizenId: 501,
  campaignId: 301,
  slotId: 42,
  status: 'CHECKED_IN',
  bookedAt: '2026-09-19T09:00:00Z',
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
    resolveFirst([{ ...sampleAppointment, id: 101, slotId: 42 }]);

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

  it('opens View modal, displays fresh server details, and closes on Escape or button click', async () => {
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
    });

    // Close via Escape key
    fireEvent.keyDown(window, { key: 'Escape', code: 'Escape' });

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).toBeNull();
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
    });

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
});
