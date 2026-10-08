import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ApiError } from '../../../shared/api/apiClient';
import { vaccinationDraftApi } from '../api/vaccinationDraftApi';
import { getAvailableBatches } from '../api/certificateApi';
import VaccinationDraftsPage from './VaccinationDraftsPage';
import { getLocalCalendarToday, toApiIsoDate } from '../utils/dateUtils';

vi.mock('../api/certificateApi', () => ({
  getAvailableBatches: vi.fn(),
}));

vi.mock('../api/vaccinationDraftApi', () => ({
  vaccinationDraftApi: {
    list: vi.fn(),
    get: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    remove: vi.fn(),
    confirmAdministration: vi.fn(),
    finalize: vi.fn(),
    confirmAndFinalize: vi.fn(),
  },
}));

const draft = {
  id: 1,
  appointmentId: 101,
  vaccineBatchId: 301,
  doseNumber: 1,
  administrationTime: '2026-09-16T10:00:00Z',
  nextDoseDate: '2030-01-01',
  authorizedNotes: 'Original notes',
  status: 'IN_PROGRESS',
  vaccinatorId: 201,
  physicalAdministrationConfirmed: false,
};

const readyDraft = {
  ...draft,
  id: 2,
  appointmentId: 102,
  status: 'READY_FOR_FINALIZATION',
  physicalAdministrationConfirmed: true,
  authorizedNotes: 'Ready notes',
};

const finalizedDraft = {
  ...draft,
  id: 3,
  appointmentId: 103,
  status: 'FINALIZED',
  physicalAdministrationConfirmed: true,
  authorizedNotes: 'Finalized notes',
};

function renderWithRouter(ui) {
  return render(<MemoryRouter>{ui}</MemoryRouter>);
}

async function openPage() {
  renderWithRouter(<VaccinationDraftsPage />);
  await screen.findByText('Original notes');
}

async function openCreate() {
  await openPage();
  fireEvent.click(screen.getByRole('button', { name: 'Create Draft' }));
  fireEvent.change(screen.getByLabelText('Appointment ID'), { target: { value: '101' } });
}

describe('Vaccination Drafts CRUD', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vaccinationDraftApi.list.mockResolvedValue([draft]);
    vaccinationDraftApi.get.mockResolvedValue(draft);
    vaccinationDraftApi.create.mockResolvedValue(draft);
    vaccinationDraftApi.update.mockResolvedValue(draft);
    vaccinationDraftApi.remove.mockResolvedValue(null);
    vaccinationDraftApi.confirmAdministration.mockResolvedValue({
      ...draft,
      physicalAdministrationConfirmed: true,
      status: 'READY_FOR_FINALIZATION',
    });
    vaccinationDraftApi.finalize.mockResolvedValue({
      draftId: 1,
      certificateReference: 'CERT-2026-TEST-XYZ',
    });
    vaccinationDraftApi.confirmAndFinalize.mockResolvedValue({
      draftId: 1,
      certificateReference: 'CERT-2026-TEST-XYZ',
    });
    getAvailableBatches.mockResolvedValue([]);
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('renders loading followed by draft list and read-only confirmation', async () => {
    renderWithRouter(<VaccinationDraftsPage />);
    expect(screen.getByRole('heading', { name: 'Vaccination Drafts' })).toBeTruthy();
    expect(screen.getByText('Loading drafts…')).toBeTruthy();
    await screen.findByText('Original notes');
    expect(vaccinationDraftApi.list).toHaveBeenCalledTimes(1);
    expect(screen.getByText('101')).toBeTruthy();
    expect(screen.getByText('301')).toBeTruthy();
    expect(screen.getByRole('columnheader', { name: 'Administration date' })).toBeTruthy();
    expect(screen.queryByRole('columnheader', { name: 'Administration time' })).toBeNull();
    expect(screen.getByText('2026-09-16')).toBeTruthy();
    expect(screen.getAllByText('In Progress').length).toBeGreaterThan(0);
    expect(screen.getByText('Not confirmed')).toBeTruthy();
  });

  it('shows an empty state for active drafts', async () => {
    vaccinationDraftApi.list.mockResolvedValue([]);
    renderWithRouter(<VaccinationDraftsPage />);
    expect(await screen.findByText(/No active vaccination drafts yet/)).toBeTruthy();
  });

  it('creates using all public fields, excluding vaccinator identity and confirmation', async () => {
    getAvailableBatches.mockResolvedValue([
      {
        id: 301,
        batchNumber: 'BNT-2026-B01',
        vaccineName: 'Pfizer',
        availableQuantity: 10,
        expiryDate: '2026-12-31',
      },
    ]);
    await openCreate();
    const today = getLocalCalendarToday();
    expect(screen.getByLabelText('Administration date').value).toBe(today);
    expect(screen.getByLabelText('Administration date').disabled).toBe(true);
    expect(screen.getAllByRole('button', { name: 'Create Draft' })).toHaveLength(2);
    expect(screen.getByText('Enter the dose sequence being administered.')).toBeTruthy();
    expect(screen.getByText('Leave blank if no further dose is currently required or scheduled.')).toBeTruthy();
    await screen.findByRole('option', { name: /BNT-2026-B01 — Pfizer/ });
    fireEvent.change(screen.getByLabelText('Vaccine Batch *'), { target: { value: '301' } });
    fireEvent.change(screen.getByLabelText('Dose Being Administered *'), { target: { value: '1' } });
    fireEvent.change(screen.getByLabelText(/Next dose date/i), { target: { value: '2030-01-01' } });
    fireEvent.change(screen.getByLabelText('Authorized notes'), { target: { value: 'Demo notes' } });
    fireEvent.submit(screen.getByRole('form', { name: 'Vaccination draft form' }));
    await screen.findByText('Draft created successfully.');
    const payload = vaccinationDraftApi.create.mock.calls[0][0];
    expect(payload).toEqual({
      appointmentId: 101,
      vaccineBatchId: 301,
      doseNumber: 1,
      administrationTime: toApiIsoDate(today),
      nextDoseDate: '2030-01-01',
      authorizedNotes: 'Demo notes',
    });
    expect(payload.administrationTime).toMatch(/^\d{4}-\d{2}-\d{2}T00:00:00Z$/);
    expect(payload).not.toHaveProperty('vaccinatorId');
    expect(payload).not.toHaveProperty('physicalAdministrationConfirmed');
    expect(screen.queryByRole('form')).toBeNull();
    expect(vaccinationDraftApi.list).toHaveBeenCalledTimes(2);
  });

  it('enforces required constraints and positive dose validation on create drafts', async () => {
    getAvailableBatches.mockResolvedValue([
      {
        id: 301,
        batchNumber: 'BNT-2026-B01',
        vaccineName: 'Pfizer',
        availableQuantity: 10,
        expiryDate: '2026-12-31',
      },
    ]);
    await openCreate();
    expect(screen.getByLabelText('Appointment ID').required).toBe(true);
    expect(screen.getByLabelText('Appointment ID').min).toBe('1');
    expect(screen.getByLabelText('Dose Being Administered *').required).toBe(true);
    expect(screen.getByLabelText('Dose Being Administered *').min).toBe('1');
    expect(screen.getByLabelText('Authorized notes').maxLength).toBe(2000);
    await screen.findByRole('option', { name: /BNT-2026-B01 — Pfizer/ });
    fireEvent.change(screen.getByLabelText('Vaccine Batch *'), { target: { value: '301' } });
    fireEvent.change(screen.getByLabelText('Dose Being Administered *'), { target: { value: '1' } });
    fireEvent.submit(screen.getByRole('form'));
    await screen.findByText('Draft created successfully.');
    expect(vaccinationDraftApi.create).toHaveBeenCalledWith({
      appointmentId: 101,
      vaccineBatchId: 301,
      doseNumber: 1,
      administrationTime: toApiIsoDate(getLocalCalendarToday()),
    });
  });

  it('views draft details with GET and closes the read-only panel', async () => {
    await openPage();
    fireEvent.click(screen.getByRole('button', { name: 'View draft 1' }));
    await screen.findByRole('heading', { name: 'View draft 1' });
    expect(vaccinationDraftApi.get).toHaveBeenCalledWith(1);
    expect(screen.getByText('Draft ID')).toBeTruthy();
    expect(screen.getByText('Vaccine Batch ID')).toBeTruthy();
    expect(screen.getByText('Administration Date')).toBeTruthy();
    expect(screen.getAllByText('2026-09-16').length).toBeGreaterThan(0);
    expect(screen.getByText('Physical Confirmation')).toBeTruthy();
    expect(screen.getAllByText('Not confirmed').length).toBeGreaterThan(0);
    fireEvent.click(screen.getByRole('button', { name: 'Close' }));
    expect(screen.queryByRole('heading', { name: 'View draft 1' })).toBeNull();
  });

  it('fetches and populates edit, then PATCHes changed editable fields only', async () => {
    await openPage();
    fireEvent.click(screen.getByRole('button', { name: 'Edit draft 1' }));
    await screen.findByRole('heading', { name: 'Edit draft 1' });
    expect(vaccinationDraftApi.get).toHaveBeenCalledWith(1);
    expect(screen.getByLabelText('Appointment ID').disabled).toBe(true);
    expect(screen.getByLabelText('Administration date').value).toBe('2026-09-16');
    expect(screen.getByLabelText('Administration date').disabled).toBe(true);
    expect(screen.getByLabelText('Vaccine Batch *').value).toBe('301');
    expect(screen.getByRole('button', { name: 'Update Draft' })).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Authorized notes'), { target: { value: 'Updated notes' } });
    fireEvent.submit(screen.getByRole('form'));
    await screen.findByText('Draft updated successfully.');
    expect(vaccinationDraftApi.update).toHaveBeenCalledWith(1, { authorizedNotes: 'Updated notes' });
    expect(screen.queryByRole('form')).toBeNull();
    expect(vaccinationDraftApi.list).toHaveBeenCalledTimes(2);
  });

  it('requires confirmation and does not discard when cancelled', async () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
    await openPage();
    fireEvent.click(screen.getByRole('button', { name: 'Discard draft 1' }));
    expect(confirm).toHaveBeenCalledWith(expect.stringContaining('Discard vaccination draft 1?'));
    expect(vaccinationDraftApi.remove).not.toHaveBeenCalled();
  });

  it('discards after confirmation and refreshes with a success message', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await openPage();
    fireEvent.click(screen.getByRole('button', { name: 'Discard draft 1' }));
    await screen.findByText('Draft discarded successfully.');
    expect(vaccinationDraftApi.remove).toHaveBeenCalledWith(1);
    expect(vaccinationDraftApi.list).toHaveBeenCalledTimes(2);
  });

  it('shows backend field validation messages and keeps the form open', async () => {
    vaccinationDraftApi.create.mockRejectedValue(new ApiError('Validation failed', 400, {
      fieldErrors: [{ field: 'appointmentId', message: 'Appointment ID must be positive' }],
    }));
    await openCreate();
    fireEvent.submit(screen.getByRole('form'));
    await screen.findByText('Validation failed');
    expect(screen.getByRole('alert').textContent).toContain('Appointment ID must be positive');
    expect(screen.getByRole('form')).toBeTruthy();
  });

  it.each([401, 403, 404, 409, 500, 0])('shows API/network errors for status %s', async (status) => {
    vaccinationDraftApi.list.mockRejectedValue(new ApiError(`Backend error ${status}`, status));
    renderWithRouter(<VaccinationDraftsPage />);
    await waitFor(() => expect(screen.getByRole('alert').textContent).toContain(`Backend error ${status}`));
  });

  it('shows backend deletion rejection', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vaccinationDraftApi.remove.mockRejectedValue(new ApiError('Only an unfinalized draft may be changed', 400));
    await openPage();
    fireEvent.click(screen.getByRole('button', { name: 'Discard draft 1' }));
    expect(await screen.findByText('Only an unfinalized draft may be changed')).toBeTruthy();
  });

  it('shows Confirm & Finalize for IN_PROGRESS and does not show standalone Finalize', async () => {
    vaccinationDraftApi.list.mockResolvedValue([draft]);

    renderWithRouter(<VaccinationDraftsPage />);
    await screen.findByText('Original notes');

    expect(screen.getByRole('button', { name: 'View draft 1' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Edit draft 1' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Discard draft 1' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Confirm and finalize draft 1' })).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Finalize draft 1' })).toBeNull();
  });

  it('shows Confirm & Finalize for READY_FOR_FINALIZATION and does not show standalone Finalize', async () => {
    vaccinationDraftApi.list.mockResolvedValue([readyDraft]);

    renderWithRouter(<VaccinationDraftsPage />);
    await screen.findByText('Ready notes');

    expect(screen.getByRole('button', { name: 'View draft 2' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Confirm and finalize draft 2' })).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Finalize draft 2' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Edit draft 2' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Discard draft 2' })).toBeNull();
  });

  it('ensures Draft History tab is absent and FINALIZED drafts are absent from the page', async () => {
    vaccinationDraftApi.list.mockResolvedValue([draft, readyDraft, finalizedDraft]);

    renderWithRouter(<VaccinationDraftsPage />);
    await screen.findByText('Original notes');

    // Tabs must be completely absent
    expect(screen.queryByRole('tab')).toBeNull();
    expect(screen.queryByRole('tablist')).toBeNull();
    expect(screen.queryByText(/Draft History/)).toBeNull();

    // Active drafts must be visible
    expect(screen.getByText('Original notes')).toBeTruthy();
    expect(screen.getByText('Ready notes')).toBeTruthy();

    // FINALIZED draft must NOT appear on Vaccination Drafts page
    expect(screen.queryByText('Finalized notes')).toBeNull();
    expect(screen.queryByRole('button', { name: 'View draft 3' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Finalize draft 3' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Confirm and finalize draft 3' })).toBeNull();
  });

  it('displays correct confirmation dialog text for IN_PROGRESS draft', async () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);
    await openPage();

    fireEvent.click(screen.getByRole('button', { name: 'Confirm and finalize draft 1' }));

    expect(confirmSpy).toHaveBeenCalledWith(
      expect.stringContaining('Confirm that the vaccine has physically been administered to this citizen and finalize the vaccination?')
    );
    expect(confirmSpy).toHaveBeenCalledWith(
      expect.stringContaining('This action will permanently record the vaccination, deduct inventory, complete the appointment, and issue the official digital certificate.')
    );
    expect(confirmSpy).toHaveBeenCalledWith(
      expect.stringContaining('This action cannot be edited or discarded afterward.')
    );
    expect(vaccinationDraftApi.confirmAndFinalize).toHaveBeenCalledWith(1);
  });

  it('displays correct confirmation dialog text for legacy READY_FOR_FINALIZATION draft', async () => {
    vaccinationDraftApi.list.mockResolvedValue([readyDraft]);
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);

    renderWithRouter(<VaccinationDraftsPage />);
    await screen.findByText('Ready notes');

    fireEvent.click(screen.getByRole('button', { name: 'Confirm and finalize draft 2' }));

    expect(confirmSpy).toHaveBeenCalledWith(
      expect.stringContaining('Physical administration has already been confirmed for this draft. Complete finalization now?')
    );
    expect(confirmSpy).toHaveBeenCalledWith(
      expect.stringContaining('This will permanently record the vaccination, deduct inventory, complete the appointment, and issue the official digital certificate.')
    );
    expect(vaccinationDraftApi.confirmAndFinalize).toHaveBeenCalledWith(2);
  });

  it('successful completion removes draft from active list, displays success message, and refreshes', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vaccinationDraftApi.list.mockResolvedValueOnce([draft]).mockResolvedValueOnce([]);

    await openPage();
    fireEvent.click(screen.getByRole('button', { name: 'Confirm and finalize draft 1' }));

    await screen.findByText(
      'Vaccination confirmed and finalized successfully. The permanent vaccination record and digital certificate are now available.'
    );
    expect(vaccinationDraftApi.confirmAndFinalize).toHaveBeenCalledWith(1);
    expect(vaccinationDraftApi.list).toHaveBeenCalledTimes(2);

    // After refresh with empty list, active drafts empty alert should appear
    expect(await screen.findByText(/No active vaccination drafts yet/)).toBeTruthy();
  });

  it('omits redundant navigation link to Vaccination Records from top actions', async () => {
    vaccinationDraftApi.list.mockResolvedValue([draft]);

    renderWithRouter(<VaccinationDraftsPage />);
    await screen.findByText('Original notes');

    // Redundant top action link should not exist since it is now in main navigation
    const recordsLink = screen.queryByRole('link', { name: 'Vaccination Records' });
    expect(recordsLink).toBeNull();
  });

  it('renders available batches dropdown and positive integer dose input when creating a draft', async () => {
    getAvailableBatches.mockResolvedValue([
      {
        id: 501,
        batchNumber: 'BATCH-2026-X',
        vaccineName: 'Pfizer Comirnaty',
        availableQuantity: 42,
        expiryDate: '2026-12-31',
      },
    ]);

    await openPage();
    fireEvent.click(screen.getByRole('button', { name: 'Create Draft' }));
    fireEvent.change(screen.getByLabelText('Appointment ID'), { target: { value: '112' } });

    await screen.findByRole('option', { name: /BATCH-2026-X — Pfizer Comirnaty — 42 available · Expires 2026-12-31/ });

    const doseInput = screen.getByLabelText('Dose Being Administered *');
    expect(doseInput.tagName).toBe('INPUT');
    expect(doseInput.getAttribute('type')).toBe('number');
    expect(doseInput.getAttribute('min')).toBe('1');
    expect(doseInput.getAttribute('step')).toBe('1');
    expect(screen.getByText('Enter the dose sequence being administered.')).toBeTruthy();

    fireEvent.change(doseInput, { target: { value: '2' } });
    expect(doseInput.value).toBe('2');

    const nextDoseInput = screen.getByLabelText(/Next dose date/i);
    expect(nextDoseInput.getAttribute('min')).toBe(getLocalCalendarToday());
  });

  it('renders read-only Patient/Appointment summary card when preselectedAppointment is passed in location state', async () => {
    const preselectedAppt = {
      id: 112,
      citizenId: 8,
      citizenName: 'Kasun Perera',
      campaignId: 1,
      campaignName: 'National COVID-19 Booster Campaign 2026',
      slotId: 14,
      status: 'CHECKED_IN',
    };

    getAvailableBatches.mockResolvedValue([
      {
        id: 501,
        batchNumber: 'BNT-2026-B01',
        vaccineName: 'Pfizer Comirnaty',
        availableQuantity: 42,
        expiryDate: '2026-12-31',
      },
    ]);

    render(
      <MemoryRouter initialEntries={[{ pathname: '/app/staff/vaccination-drafts', state: { preselectedAppointment: preselectedAppt } }]}>
        <VaccinationDraftsPage />
      </MemoryRouter>
    );

    await screen.findByText('Original notes');

    // Should immediately open form with summary card
    expect(screen.getByRole('heading', { name: 'Create Vaccination Draft' })).toBeTruthy();
    expect(screen.getByText('Patient / Appointment')).toBeTruthy();
    expect(screen.getByText('Kasun Perera')).toBeTruthy();
    expect(screen.getByText(/Appointment #112/)).toBeTruthy();
    expect(screen.getByText(/National COVID-19 Booster Campaign 2026/)).toBeTruthy();
    expect(screen.getByText('Checked In ✓')).toBeTruthy();

    // Raw Slot ID 14 must NOT be displayed as Appointment ID
    expect(screen.queryByText('Appointment #14')).toBeNull();
    // Manual text input for Appointment ID is hidden in preselected mode
    expect(screen.queryByLabelText('Appointment ID')).toBeNull();

    // Verify batch selection
    await screen.findByRole('option', { name: /BNT-2026-B01 — Pfizer Comirnaty — 42 available · Expires 2026-12-31/ });
    fireEvent.change(screen.getByLabelText('Vaccine Batch *'), { target: { value: '501' } });
    fireEvent.change(screen.getByLabelText('Dose Being Administered *'), { target: { value: '1' } });

    // Submit draft
    fireEvent.submit(screen.getByRole('form', { name: 'Vaccination draft form' }));
    await screen.findByText('Draft created successfully.');

    expect(vaccinationDraftApi.create).toHaveBeenCalledWith(
      expect.objectContaining({
        appointmentId: 112,
        vaccineBatchId: 501,
        doseNumber: 1,
      })
    );
    expect(vaccinationDraftApi.create).not.toHaveBeenCalledWith(
      expect.objectContaining({
        appointmentId: 14,
      })
    );
  });

  it('displays message when no available batches exist for the appointment and disables submit', async () => {
    getAvailableBatches.mockResolvedValue([]);

    await openPage();
    fireEvent.click(screen.getByRole('button', { name: 'Create Draft' }));
    fireEvent.change(screen.getByLabelText('Appointment ID'), { target: { value: '112' } });

    await screen.findByText('No compatible vaccine batches with available stock are currently available for this appointment.');
    const form = screen.getByRole('form', { name: 'Vaccination draft form' });
    const submitBtn = within(form).getByRole('button', { name: 'Create Draft' });
    expect(submitBtn.disabled).toBe(true);

    // Verify no manual batch override toggle or input exists
    expect(screen.queryByRole('button', { name: /Enter ID manually/i })).toBeNull();
    expect(screen.queryByPlaceholderText('Enter batch ID')).toBeNull();
    expect(screen.queryByPlaceholderText('Vaccine batch ID')).toBeNull();
  });

  it('rejects dose 0 or negative values and accepts valid positive integer dose', async () => {
    getAvailableBatches.mockResolvedValue([
      {
        id: 501,
        batchNumber: 'BATCH-2026-X',
        vaccineName: 'Pfizer Comirnaty',
        availableQuantity: 42,
        expiryDate: '2026-12-31',
      },
    ]);

    await openPage();
    fireEvent.click(screen.getByRole('button', { name: 'Create Draft' }));
    fireEvent.change(screen.getByLabelText('Appointment ID'), { target: { value: '112' } });

    await screen.findByRole('option', { name: /BATCH-2026-X — Pfizer Comirnaty — 42 available · Expires 2026-12-31/ });
    fireEvent.change(screen.getByLabelText('Vaccine Batch *'), { target: { value: '501' } });

    const form = screen.getByRole('form', { name: 'Vaccination draft form' });
    const submitBtn = within(form).getByRole('button', { name: 'Create Draft' });
    const doseInput = screen.getByLabelText('Dose Being Administered *');

    // Reject 0
    fireEvent.change(doseInput, { target: { value: '0' } });
    expect(submitBtn.disabled).toBe(true);

    // Reject negative
    fireEvent.change(doseInput, { target: { value: '-1' } });
    expect(submitBtn.disabled).toBe(true);

    // Reject blank
    fireEvent.change(doseInput, { target: { value: '' } });
    expect(submitBtn.disabled).toBe(true);

    // Accept valid positive integer
    fireEvent.change(doseInput, { target: { value: '1' } });
    expect(submitBtn.disabled).toBe(false);

    // Verify no hard-coded Dose 1 - Dose 6 select options exist in the DOM
    expect(screen.queryByRole('option', { name: 'Dose 1' })).toBeNull();
    expect(screen.queryByRole('option', { name: 'Dose 6' })).toBeNull();
    expect(screen.queryByText(/Suggested from vaccination history/i)).toBeNull();
  });
});
