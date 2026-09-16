import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { ApiError } from '../../../services/apiClient';
import { vaccinationDraftApi } from '../api/vaccinationDraftApi';
import VaccinationDraftsPage from './VaccinationDraftsPage';

vi.mock('../api/vaccinationDraftApi', () => ({
  vaccinationDraftApi: { list: vi.fn(), get: vi.fn(), create: vi.fn(), update: vi.fn(), remove: vi.fn() },
}));

const draft = { id: 1, appointmentId: 101, vaccineBatchId: 301, doseNumber: 1,
  administrationTime: '2026-09-16T10:00:00Z', nextDoseDate: '2030-01-01', authorizedNotes: 'Original notes',
  status: 'IN_PROGRESS', vaccinatorId: 201, physicalAdministrationConfirmed: false };

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
  });
  afterEach(() => { cleanup(); vi.restoreAllMocks(); });

  it('renders loading followed by draft list and read-only confirmation', async () => {
    render(<VaccinationDraftsPage />);
    expect(screen.getByRole('heading', { name: 'Vaccination Drafts' })).toBeTruthy();
    expect(screen.getByText('Loading drafts…')).toBeTruthy();
    await screen.findByText('Original notes');
    expect(vaccinationDraftApi.list).toHaveBeenCalledTimes(1);
    expect(screen.getByText('101')).toBeTruthy();
    expect(screen.getByText('301')).toBeTruthy();
    expect(screen.getByText('IN_PROGRESS')).toBeTruthy();
    expect(screen.getByText('Not confirmed')).toBeTruthy();
  });

  it('shows an empty state', async () => {
    vaccinationDraftApi.list.mockResolvedValue([]);
    render(<VaccinationDraftsPage />);
    expect(await screen.findByText(/No vaccination drafts yet/)).toBeTruthy();
  });

  it('creates using all public fields, excluding vaccinator identity and confirmation', async () => {
    await openCreate();
    fireEvent.change(screen.getByLabelText('Vaccine batch ID'), { target: { value: '301' } });
    fireEvent.change(screen.getByLabelText('Dose number'), { target: { value: '1' } });
    fireEvent.change(screen.getByLabelText('Administration time'), { target: { value: '2026-09-16T10:00:00' } });
    fireEvent.change(screen.getByLabelText('Next dose date'), { target: { value: '2030-01-01' } });
    fireEvent.change(screen.getByLabelText('Authorized notes'), { target: { value: 'Demo notes' } });
    fireEvent.submit(screen.getByRole('form', { name: 'Vaccination draft form' }));
    await screen.findByText('Draft created successfully.');
    const payload = vaccinationDraftApi.create.mock.calls[0][0];
    expect(payload).toEqual({ appointmentId: 101, vaccineBatchId: 301, doseNumber: 1,
      administrationTime: new Date('2026-09-16T10:00:00').toISOString(), nextDoseDate: '2030-01-01', authorizedNotes: 'Demo notes' });
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
    expect(vaccinationDraftApi.create).toHaveBeenCalledWith({ appointmentId: 101 });
  });

  it('fetches and populates edit, then PATCHes changed editable fields only', async () => {
    await openPage();
    fireEvent.click(screen.getByRole('button', { name: 'Edit draft 1' }));
    await screen.findByRole('heading', { name: 'Edit draft 1' });
    expect(vaccinationDraftApi.get).toHaveBeenCalledWith(1);
    expect(screen.getByLabelText('Appointment ID').disabled).toBe(true);
    expect(screen.getByLabelText('Vaccine batch ID').value).toBe('301');
    fireEvent.change(screen.getByLabelText('Authorized notes'), { target: { value: 'Updated notes' } });
    fireEvent.submit(screen.getByRole('form'));
    await screen.findByText('Draft updated successfully.');
    expect(vaccinationDraftApi.update).toHaveBeenCalledWith(1, { authorizedNotes: 'Updated notes' });
    expect(screen.queryByRole('form')).toBeNull();
    expect(vaccinationDraftApi.list).toHaveBeenCalledTimes(2);
  });

  it('requires confirmation and does not delete when cancelled', async () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
    await openPage();
    fireEvent.click(screen.getByRole('button', { name: 'Delete draft 1' }));
    expect(confirm).toHaveBeenCalledWith(expect.stringContaining('Delete vaccination draft 1?'));
    expect(vaccinationDraftApi.remove).not.toHaveBeenCalled();
  });

  it('deletes after confirmation and refreshes with a success message', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await openPage();
    fireEvent.click(screen.getByRole('button', { name: 'Delete draft 1' }));
    await screen.findByText('Draft deleted successfully.');
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
    fireEvent.click(screen.getByRole('button', { name: 'Delete draft 1' }));
    expect(await screen.findByText('Only an unfinalized draft may be changed')).toBeTruthy();
  });
});
