import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import MasterDataManagementModal from './MasterDataManagementModal';
import * as api from '../api/campaignApi';

vi.mock('../api/campaignApi', () => ({
  listAllVaccineTypes: vi.fn(), listAllTargetGroups: vi.fn(),
  createVaccineType: vi.fn(), updateVaccineType: vi.fn(),
  activateVaccineType: vi.fn(), deactivateVaccineType: vi.fn(), deleteVaccineType: vi.fn(),
  createTargetGroup: vi.fn(), updateTargetGroup: vi.fn(),
  activateTargetGroup: vi.fn(), deactivateTargetGroup: vi.fn(), deleteTargetGroup: vi.fn(),
}));

const vaccine = { id: 1, code: 'TEST-V', name: 'Test vaccine', doseUnit: 'dose', active: true };
const group = { id: 2, code: 'TEST-G', description: 'Test group', active: false };

beforeEach(() => {
  vi.resetAllMocks();
  api.listAllVaccineTypes.mockResolvedValue([vaccine]);
  api.listAllTargetGroups.mockResolvedValue([group]);
});
afterEach(cleanup);

async function open(category) {
  render(<MasterDataManagementModal onClose={vi.fn()} />);
  await screen.findByText(vaccine.code);
  await waitFor(() => expect(screen.getByRole('button', { name: 'Target Groups (1)' })).toBeTruthy());
  if (category === 'group') fireEvent.click(screen.getByRole('button', { name: 'Target Groups (1)' }));
}

describe.each([
  ['vaccine', vaccine, 'deleteVaccineType', 'Vaccine Types'],
  ['group', group, 'deleteTargetGroup', 'Target Groups'],
])('%s deletion', (category, entry, deleteMethod, tabLabel) => {
  it('names the entry and cancellation sends no request', async () => {
    await open(category);
    fireEvent.click(screen.getByRole('button', { name: `Delete ${entry.code}` }));
    expect(screen.getByText(/This action cannot be undone/)).toBeTruthy();
    expect(screen.getAllByText(entry.code)).toHaveLength(2);
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(api[deleteMethod]).not.toHaveBeenCalled();
    expect(screen.getByText(entry.code)).toBeTruthy();
  });

  it('removes the row, updates the count and clears its edit form', async () => {
    await open(category);
    fireEvent.click(screen.getByRole('button', { name: 'Edit' }));
    fireEvent.click(screen.getByRole('button', { name: `Delete ${entry.code}` }));
    fireEvent.click(screen.getByRole('button', { name: 'Delete permanently' }));
    await screen.findByText(/deleted successfully/);
    expect(api[deleteMethod]).toHaveBeenCalledWith(entry.id);
    expect(screen.getByRole('button', { name: `${tabLabel} (0)` })).toBeTruthy();
    expect(screen.queryByText(entry.code)).toBeNull();
    expect(screen.queryByRole('button', { name: 'Save Changes' })).toBeNull();
    expect(screen.getByText(category === 'vaccine' ? 'No vaccine types configured.' : 'No target groups configured.')).toBeTruthy();
  });

  it('disables conflicting controls while deletion is pending', async () => {
    let resolveDelete;
    api[deleteMethod].mockImplementation(() => new Promise((resolve) => { resolveDelete = resolve; }));
    await open(category);
    fireEvent.click(screen.getByRole('button', { name: `Delete ${entry.code}` }));
    fireEvent.click(screen.getByRole('button', { name: 'Delete permanently' }));
    for (const name of ['Deleting...', 'Cancel', 'Edit', `Delete ${entry.code}`, category === 'vaccine' ? 'Deactivate' : 'Activate']) {
      expect(screen.getByRole('button', { name }).disabled).toBe(true);
    }
    await act(async () => resolveDelete());
    await screen.findByText(/deleted successfully/);
    expect(api[deleteMethod]).toHaveBeenCalledTimes(1);
  });

  it.each([409, 500])('retains the row and shows a %s error', async (status) => {
    const message = status === 409 ? 'Entry is used. Deactivate it instead.' : 'Unable to delete entry.';
    api[deleteMethod].mockRejectedValue(Object.assign(new Error(message), { status }));
    await open(category);
    fireEvent.click(screen.getByRole('button', { name: `Delete ${entry.code}` }));
    fireEvent.click(screen.getByRole('button', { name: 'Delete permanently' }));
    await screen.findByText(message);
    expect(screen.getByRole('button', { name: `${tabLabel} (1)` })).toBeTruthy();
    expect(screen.getByRole('button', { name: `Delete ${entry.code}` }).disabled).toBe(false);
    expect(screen.queryByText(/deleted successfully/)).toBeNull();
  });
});
