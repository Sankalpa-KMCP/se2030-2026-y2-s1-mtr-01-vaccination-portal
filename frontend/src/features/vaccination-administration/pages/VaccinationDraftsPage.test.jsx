import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { ApiError } from '../../../services/apiClient';
import { vaccinationDraftApi } from '../api/vaccinationDraftApi';
import VaccinationDraftsPage from './VaccinationDraftsPage';
import { getLocalCalendarToday, toApiIsoDate } from '../utils/dateUtils';

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

async function openPage() {
  render(<VaccinationDraftsPage />);
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
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('renders loading followed by draft list and read-only confirmation', async () => {
    render(<VaccinationDraftsPage />);
    expect(screen.getByRole('heading', { name: 'Vaccination Drafts' })).toBeTruthy();
    expect(screen.getByText('Loading drafts…')).toBeTruthy();
    await screen.findByText('Original notes');
    expect(vaccinationDraftApi.list).toHaveBeenCalledTimes(1);
    expect(screen.getByText('101')).toBeTruthy();
    expect(screen.getByText('301')).toBeTruthy();
    expect(screen.getByRole('columnheader', { name: 'Administration date' })).toBeTruthy();
    expect(screen.queryByRole('columnheader', { name: 'Administration time' })).toBeNull();
    expect(screen.getByText('2026-09-16')).toBeTruthy();
    expect(screen.getByText('IN_PROGRESS')).toBeTruthy();
    expect(screen.getByText('Not confirmed')).toBeTruthy();
  });

  it('shows an empty state for active drafts', async () => {
    vaccinationDraftApi.list.mockResolvedValue([]);
    render(<VaccinationDraftsPage />);
    expect(await screen.findByText(/No active vaccination drafts yet/)).toBeTruthy();
  });

  it('creates using all public fields, excluding vaccinator identity and confirmation', async () => {
    await openCreate();
    const today = getLocalCalendarToday();
    expect(screen.getByLabelText('Administration date').value).toBe(today);
    expect(screen.getByLabelText('Administration date').disabled).toBe(true);
    expect(screen.getAllByRole('button', { name: 'Create Draft' })).toHaveLength(2);
    fireEvent.change(screen.getByLabelText('Vaccine batch ID'), { target: { value: '301' } });
    fireEvent.change(screen.getByLabelText('Dose number'), { target: { value: '1' } });
    fireEvent.change(screen.getByLabelText('Next dose date'), { target: { value: '2030-01-01' } });
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

  it('allows incomplete create drafts and uses required/min/maxlength constraints', async () => {
    await openCreate();
    expect(screen.getByLabelText('Appointment ID').required).toBe(true);
    expect(screen.getByLabelText('Appointment ID').min).toBe('1');
    expect(screen.getByLabelText('Authorized notes').maxLength).toBe(2000);
    fireEvent.submit(screen.getByRole('form'));
    await screen.findByText('Draft created successfully.');
    expect(vaccinationDraftApi.create).toHaveBeenCalledWith({
      appointmentId: 101,
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
    expect(screen.getByLabelText('Vaccine batch ID').value).toBe('301');
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
    render(<VaccinationDraftsPage />);
    await waitFor(() => expect(screen.getByRole('alert').textContent).toContain(`Backend error ${status}`));
  });

  it('shows backend deletion rejection', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vaccinationDraftApi.remove.mockRejectedValue(new ApiError('Only an unfinalized draft may be changed', 400));
    await openPage();
    fireEvent.click(screen.getByRole('button', { name: 'Discard draft 1' }));
    expect(await screen.findByText('Only an unfinalized draft may be changed')).toBeTruthy();
  });

  it('confirms and finalizes draft after confirmation prompt and refreshes', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await openPage();
    fireEvent.click(screen.getByRole('button', { name: 'Confirm and finalize draft 1' }));
    await screen.findByText(/Vaccination confirmed and finalized successfully\. Certificate reference: CERT-2026-TEST-XYZ\./);
    expect(vaccinationDraftApi.confirmAndFinalize).toHaveBeenCalledWith(1);
    expect(vaccinationDraftApi.list).toHaveBeenCalledTimes(2);
  });

  it('finalizes draft after confirmation prompt and displays certificate reference', async () => {
    vaccinationDraftApi.list.mockResolvedValue([readyDraft]);
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    render(<VaccinationDraftsPage />);
    const finalizeBtn = await screen.findByRole('button', { name: 'Finalize draft 2' });
    fireEvent.click(finalizeBtn);

    await screen.findByText(/Vaccination finalized successfully\. Certificate reference: CERT-2026-TEST-XYZ\./);
    expect(vaccinationDraftApi.finalize).toHaveBeenCalledWith(2);
    expect(vaccinationDraftApi.list).toHaveBeenCalledTimes(2);
  });

  it('separates Active Drafts and Draft History into tabs with status-gated action buttons', async () => {
    vaccinationDraftApi.list.mockResolvedValue([draft, readyDraft, finalizedDraft]);

    render(<VaccinationDraftsPage />);

    // Tab buttons
    const activeTabBtn = await screen.findByRole('tab', { name: 'Active Drafts (2)' });
    const historyTabBtn = screen.getByRole('tab', { name: 'Draft History (1)' });
    expect(activeTabBtn).toBeTruthy();
    expect(historyTabBtn).toBeTruthy();

    // In Active tab: draft 1 (IN_PROGRESS) has View, Edit, Discard Draft, Confirm & Finalize
    expect(screen.getByRole('button', { name: 'View draft 1' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Edit draft 1' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Discard draft 1' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Confirm and finalize draft 1' })).toBeTruthy();

    // In Active tab: draft 2 (READY_FOR_FINALIZATION) has View and Finalize only
    expect(screen.getByRole('button', { name: 'View draft 2' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Finalize draft 2' })).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Edit draft 2' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Discard draft 2' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Confirm and finalize draft 2' })).toBeNull();

    // Draft 3 (FINALIZED) must NOT appear in the Active tab
    expect(screen.queryByText('Finalized notes')).toBeNull();

    // Switch to Draft History tab
    fireEvent.click(historyTabBtn);

    // In History tab: draft 3 (FINALIZED) appears with View only
    expect(await screen.findByText('Finalized notes')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'View draft 3' })).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Edit draft 3' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Discard draft 3' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Finalize draft 3' })).toBeNull();

    // Active drafts must not be listed in History tab
    expect(screen.queryByText('Original notes')).toBeNull();
    expect(screen.queryByText('Ready notes')).toBeNull();
  });

  it('shows an empty state in Draft History tab when there are no finalized drafts', async () => {
    vaccinationDraftApi.list.mockResolvedValue([draft]);

    render(<VaccinationDraftsPage />);
    const historyTabBtn = await screen.findByRole('tab', { name: 'Draft History (0)' });
    fireEvent.click(historyTabBtn);

    expect(await screen.findByText('No finalized draft history yet.')).toBeTruthy();
  });
});
