import React from 'react';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import InventoryManagementPage from './InventoryManagementPage';
import { inventoryApi } from '../api/inventoryApi';

vi.mock('../api/inventoryApi', () => ({
  inventoryApi: {
    loadAll: vi.fn(), create: vi.fn(), update: vi.fn(), remove: vi.fn(),
    receiveStock: vi.fn(), adjustStock: vi.fn(), transferStock: vi.fn(),
  },
}));

// Batch dates must stay relative to "now": a hardcoded expiry silently turned the
// fixture batch EXPIRED (disabling adjustments and zeroing the available-doses
// summary) once the calendar passed it. 60 days ahead stays outside the 30-day
// near-expiry window; manufacture stays 30 days behind.
const isoDate = (offsetDays) => {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
};

const payload = [
  [{ id: 1, code: 'ACME', name: 'Acme Vaccines' }],
  [{ id: 2, manufacturerId: 1, vaccineTypeId: 3, batchNumber: 'LOT-42', manufactureDate: isoDate(-30), expiryDate: isoDate(60), batchStatus: 'ACTIVE' }],
  [{ id: 4, centerId: 5, batchId: 2, quantity: 8, lockVersion: 0 }],
  { vaccineTypes: [{ id: 3, code: 'MRNA', name: 'mRNA' }], centers: [{ id: 5, code: 'CTR', name: 'Central Clinic' }] },
  [{ id: 6, inventoryId: 4, type: 'RECEIPT', quantityChange: 8, beforeQuantity: 0, afterQuantity: 8, actorId: 7, reason: 'Delivery', createdAt: '2026-09-16T08:00:00Z' }],
];

describe('InventoryManagementPage', () => {
  beforeEach(() => { vi.clearAllMocks(); inventoryApi.loadAll.mockResolvedValue(payload); });
  afterEach(() => cleanup());

  it('renders live summaries, canonical alerts, and the inventory overview', async () => {
    render(<InventoryManagementPage />);
    expect(await screen.findByRole('heading', { name: 'Inventory & Batch Management' })).toBeTruthy();
    const dosesCard = screen.getByText('Available doses').closest('article');
    expect(within(dosesCard).getByText('8')).toBeTruthy();
    expect(screen.getByText('Central Clinic')).toBeTruthy();
    expect(screen.getAllByText(/Low stock/i).length).toBeGreaterThan(0);
    expect(screen.getByText('LOT-42')).toBeTruthy();
    expect(screen.getByText('Available')).toBeTruthy();
  });

  it('shows read-only transaction history', async () => {
    render(<InventoryManagementPage />);
    await screen.findByText('LOT-42');
    fireEvent.click(screen.getByRole('button', { name: 'Stock history' }));
    expect(screen.getByText('Delivery')).toBeTruthy();
    expect(screen.getByText('User #7')).toBeTruthy();
    expect(screen.queryByRole('button', { name: /edit transaction/i })).toBeNull();
  });

  it('submits a reason-required adjustment through the MF-04 API', async () => {
    inventoryApi.adjustStock.mockResolvedValue({});
    render(<InventoryManagementPage />);
    await screen.findByText('LOT-42');
    fireEvent.click(screen.getByRole('button', { name: 'Adjust' }));
    fireEvent.change(screen.getByLabelText('Quantity change'), { target: { value: '-2' } });
    fireEvent.change(screen.getByLabelText('Reason (required)'), { target: { value: 'Damaged vial' } });
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    fireEvent.click(screen.getByRole('button', { name: 'Confirm operation' }));
    await waitFor(() => expect(inventoryApi.adjustStock).toHaveBeenCalledWith({ inventoryId: 4, quantityChange: -2, reason: 'Damaged vial' }));
  });

  it('transfers an available batch to a different center', async () => {
    const transferPayload = [...payload];
    transferPayload[3] = {
      ...payload[3],
      centers: [...payload[3].centers, { id: 9, code: 'CTR-2', name: 'East Clinic' }],
    };
    inventoryApi.loadAll.mockResolvedValue(transferPayload);
    inventoryApi.transferStock.mockResolvedValue({});
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    render(<InventoryManagementPage />);
    await screen.findByText('LOT-42');
    fireEvent.click(screen.getByRole('button', { name: 'Transfer' }));
    fireEvent.change(screen.getByLabelText('Destination center'), { target: { value: '9' } });
    fireEvent.change(screen.getByLabelText('Quantity to transfer'), { target: { value: '3' } });
    fireEvent.click(screen.getByRole('button', { name: 'Confirm operation' }));

    await waitFor(() => expect(inventoryApi.transferStock).toHaveBeenCalledWith({
      inventoryId: 4, destinationCenterId: 9, quantity: 3, reason: null,
    }));
  });
});
