import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import CenterOperationsPage from './CenterOperationsPage';
import * as centerApi from '../api/centerOperationsApi';

const authState = vi.hoisted(() => ({
  current: {
    user: { id: 1, email: 'admin@local.test', roles: ['ROLE_ADMIN'] },
    isAuthenticated: true,
  },
}));

vi.mock('../../../auth/context/AuthContext', () => ({
  useAuth: () => authState.current,
}));

vi.mock('../api/centerOperationsApi', () => ({
  listCenters: vi.fn(),
  getCenterById: vi.fn(),
  createCenter: vi.fn(),
  updateCenter: vi.fn(),
  activateCenter: vi.fn(),
  deactivateCenter: vi.fn(),
  getOperatingHours: vi.fn(),
  getClosures: vi.fn(),
  listOperationalResources: vi.fn(),
  listStaffRoster: vi.fn(),
}));

describe('CenterOperationsPage - Center CRUD integration', () => {
  const sampleCenter1 = {
    id: 1,
    code: 'CTR-COL-01',
    name: 'Colombo National Health Center',
    address: '100 Medical Square, Colombo 07',
    contact: '+94 11 2691111',
    timezone: 'Asia/Colombo',
    status: 'ACTIVE',
    safeConcurrentOccupancy: 80,
    sessionServiceCeiling: 200,
    createdAt: '2026-09-17T05:00:00Z',
    updatedAt: '2026-09-17T05:00:00Z',
  };

  const sampleCenter2 = {
    id: 2,
    code: 'CTR-KAN-02',
    name: 'Kandy Regional Center',
    address: '45 Hospital Road, Kandy',
    contact: '+94 81 2233444',
    timezone: 'Asia/Colombo',
    status: 'INACTIVE',
    safeConcurrentOccupancy: 50,
    sessionServiceCeiling: 100,
    createdAt: '2026-09-17T05:00:00Z',
    updatedAt: '2026-09-17T05:00:00Z',
  };

  beforeEach(() => {
    authState.current = {
      user: { id: 1, email: 'admin@local.test', roles: ['ROLE_ADMIN'] },
      isAuthenticated: true,
    };
    centerApi.listCenters.mockResolvedValue([sampleCenter1]);
    centerApi.getCenterById.mockResolvedValue(sampleCenter1);
    centerApi.getOperatingHours.mockResolvedValue([]);
    centerApi.getClosures.mockResolvedValue([]);
    centerApi.listOperationalResources.mockResolvedValue([]);
    centerApi.listStaffRoster.mockResolvedValue([]);
  });

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it('renders empty state when no centers exist and allows opening create modal via Register First Center', async () => {
    centerApi.listCenters.mockResolvedValueOnce([]);

    render(<CenterOperationsPage />);

    expect(await screen.findByText('No Vaccination Centers Registered')).toBeTruthy();
    const registerBtn = screen.getByRole('button', { name: /Register First Center/i });
    expect(registerBtn).toBeTruthy();

    // Click Register First Center button
    fireEvent.click(registerBtn);

    // Modal must be mounted and rendered in DOM even when centers.length === 0
    expect(await screen.findByText('Register New Vaccination Center')).toBeTruthy();
    expect(screen.getByLabelText(/Center Code/i)).toBeTruthy();
    expect(screen.getByLabelText(/Center Name/i)).toBeTruthy();
  });

  it('renders center list and detail for the selected center', async () => {
    centerApi.listCenters.mockResolvedValue([sampleCenter1, sampleCenter2]);
    centerApi.getCenterById.mockImplementation(async (id) => {
      if (String(id) === '2') return sampleCenter2;
      return sampleCenter1;
    });

    render(<CenterOperationsPage />);

    // Center 1 should be selected by default (first active center)
    expect(await screen.findByText('Colombo National Health Center')).toBeTruthy();
    expect(screen.getByText('CTR-COL-01')).toBeTruthy();
    expect(screen.getByText('100 Medical Square, Colombo 07')).toBeTruthy();
    expect(screen.getByText('+94 11 2691111')).toBeTruthy();
    expect(screen.getByText('Active')).toBeTruthy();

    // Select Center 2
    const select = screen.getByLabelText(/Select Vaccination Center/i);
    fireEvent.change(select, { target: { value: '2' } });

    // Center 2 details should load
    expect(await screen.findByText('Kandy Regional Center')).toBeTruthy();
    expect(screen.getByText('CTR-KAN-02')).toBeTruthy();
    expect(screen.getByText('Inactive (Decommissioned)')).toBeTruthy();
  });

  it('validates client-side input when registering a new center', async () => {
    render(<CenterOperationsPage />);
    await screen.findByText('Colombo National Health Center');

    fireEvent.click(screen.getByRole('button', { name: /\+ New Center/i }));
    expect(await screen.findByText('Register New Vaccination Center')).toBeTruthy();

    // Submit with empty fields
    fireEvent.click(screen.getByRole('button', { name: /Register Center/i }));
    expect(await screen.findByText('Center code is required.')).toBeTruthy();

    // Fill code, submit without name
    fireEvent.change(screen.getByLabelText(/Center Code/i), { target: { value: 'CTR-GAL-03' } });
    fireEvent.click(screen.getByRole('button', { name: /Register Center/i }));
    expect(await screen.findByText('Center name is required.')).toBeTruthy();

    // Fill name, submit without address
    fireEvent.change(screen.getByLabelText(/Center Name/i), { target: { value: 'Galle General Center' } });
    fireEvent.click(screen.getByRole('button', { name: /Register Center/i }));
    expect(await screen.findByText('Center physical address is required.')).toBeTruthy();
  });

  it('successfully creates a valid center, closes modal, and refreshes the center registry', async () => {
    const newCenter = {
      id: 3,
      code: 'CTR-GAL-03',
      name: 'Galle General Center',
      address: '12 Fort Road, Galle',
      contact: '+94 91 2233111',
      timezone: 'Asia/Colombo',
      status: 'ACTIVE',
      safeConcurrentOccupancy: 60,
      sessionServiceCeiling: 150,
      createdAt: '2026-09-17T06:00:00Z',
      updatedAt: '2026-09-17T06:00:00Z',
    };

    centerApi.createCenter.mockResolvedValueOnce(newCenter);
    centerApi.listCenters
      .mockResolvedValueOnce([sampleCenter1])
      .mockResolvedValueOnce([sampleCenter1, newCenter]);
    centerApi.getCenterById
      .mockResolvedValueOnce(sampleCenter1)
      .mockResolvedValueOnce(newCenter);

    render(<CenterOperationsPage />);
    await screen.findByText('Colombo National Health Center');

    fireEvent.click(screen.getByRole('button', { name: /\+ New Center/i }));
    await screen.findByText('Register New Vaccination Center');

    fireEvent.change(screen.getByLabelText(/Center Code/i), { target: { value: 'CTR-GAL-03' } });
    fireEvent.change(screen.getByLabelText(/Center Name/i), { target: { value: 'Galle General Center' } });
    fireEvent.change(screen.getByLabelText(/Physical Address/i), { target: { value: '12 Fort Road, Galle' } });
    fireEvent.change(screen.getByLabelText(/Contact Information/i), { target: { value: '+94 91 2233111' } });
    fireEvent.change(screen.getByLabelText(/Safe Concurrent Occupancy/i), { target: { value: '60' } });
    fireEvent.change(screen.getByLabelText(/Session Service Ceiling/i), { target: { value: '150' } });

    fireEvent.click(screen.getByRole('button', { name: /Register Center/i }));

    await waitFor(() => {
      expect(centerApi.createCenter).toHaveBeenCalledWith({
        code: 'CTR-GAL-03',
        name: 'Galle General Center',
        address: '12 Fort Road, Galle',
        contact: '+94 91 2233111',
        safeConcurrentOccupancy: 60,
        sessionServiceCeiling: 150,
        timezone: 'Asia/Colombo',
      });
    });

    // Modal should close and new center details should appear
    await waitFor(() => {
      expect(screen.queryByText('Register New Vaccination Center')).toBeNull();
    });
    expect(centerApi.listCenters).toHaveBeenCalledTimes(2);
  });

  it('displays API error inside create modal when backend registration fails', async () => {
    const error = new Error('Vaccination center already exists with code: CTR-COL-01');
    error.status = 409;
    centerApi.createCenter.mockRejectedValueOnce(error);

    render(<CenterOperationsPage />);
    await screen.findByText('Colombo National Health Center');

    fireEvent.click(screen.getByRole('button', { name: /\+ New Center/i }));
    await screen.findByText('Register New Vaccination Center');

    fireEvent.change(screen.getByLabelText(/Center Code/i), { target: { value: 'CTR-COL-01' } });
    fireEvent.change(screen.getByLabelText(/Center Name/i), { target: { value: 'Duplicate Center' } });
    fireEvent.change(screen.getByLabelText(/Physical Address/i), { target: { value: 'Duplicate Address' } });
    fireEvent.change(screen.getByLabelText(/Contact Information/i), { target: { value: '+94 11 0000000' } });

    fireEvent.click(screen.getByRole('button', { name: /Register Center/i }));

    expect(await screen.findByText('Vaccination center already exists with code: CTR-COL-01')).toBeTruthy();
  });

  it('opens edit modal with existing values, updates center, and refreshes UI', async () => {
    const updatedCenter = {
      ...sampleCenter1,
      name: 'Colombo National Medical Center (Renamed)',
      address: '100 Medical Square Suite B, Colombo 07',
      safeConcurrentOccupancy: 90,
    };

    centerApi.updateCenter.mockResolvedValueOnce(updatedCenter);
    centerApi.listCenters
      .mockResolvedValueOnce([sampleCenter1])
      .mockResolvedValueOnce([updatedCenter]);

    render(<CenterOperationsPage />);
    await screen.findByText('Colombo National Health Center');

    fireEvent.click(screen.getByRole('button', { name: /Edit Details/i }));
    expect(await screen.findByText('Edit Center Details')).toBeTruthy();

    // Verify fields populated
    const nameInput = screen.getByLabelText(/Center Name/i);
    expect(nameInput.value).toBe('Colombo National Health Center');

    fireEvent.change(nameInput, { target: { value: 'Colombo National Medical Center (Renamed)' } });
    fireEvent.change(screen.getByLabelText(/Physical Address/i), {
      target: { value: '100 Medical Square Suite B, Colombo 07' },
    });
    fireEvent.change(screen.getByLabelText(/Safe Concurrent Occupancy/i), { target: { value: '90' } });

    fireEvent.click(screen.getByRole('button', { name: /Save Changes/i }));

    await waitFor(() => {
      expect(centerApi.updateCenter).toHaveBeenCalledWith('1', {
        name: 'Colombo National Medical Center (Renamed)',
        address: '100 Medical Square Suite B, Colombo 07',
        contact: '+94 11 2691111',
        safeConcurrentOccupancy: 90,
        sessionServiceCeiling: 200,
      });
    });

    await waitFor(() => {
      expect(screen.queryByText('Edit Center Details')).toBeNull();
    });
    expect(await screen.findByText('Colombo National Medical Center (Renamed)')).toBeTruthy();
  });

  it('confirms deactivation and updates center status to INACTIVE', async () => {
    const deactivatedCenter = {
      ...sampleCenter1,
      status: 'INACTIVE',
    };

    centerApi.deactivateCenter.mockResolvedValueOnce(deactivatedCenter);
    centerApi.listCenters
      .mockResolvedValueOnce([sampleCenter1])
      .mockResolvedValueOnce([deactivatedCenter]);

    render(<CenterOperationsPage />);
    await screen.findByText('Colombo National Health Center');

    const deactivateButton = await screen.findByRole('button', { name: /Deactivate/i });
    await waitFor(() => expect(deactivateButton.disabled).toBe(false));
    fireEvent.click(deactivateButton);
    expect(await screen.findByText('Confirm Center Deactivation')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: /Yes, Deactivate Center/i }));

    await waitFor(() => {
      expect(centerApi.deactivateCenter).toHaveBeenCalledWith('1');
    });

    await waitFor(() => {
      expect(screen.queryByText('Confirm Center Deactivation')).toBeNull();
    });
    expect(await screen.findByText('Inactive (Decommissioned)')).toBeTruthy();
  });

  it('displays deactivation backend error inside the confirmation modal when blocking sessions exist', async () => {
    const error = new Error('Cannot deactivate vaccination center because 2 future or in-progress session(s) depend on it');
    error.status = 400;
    centerApi.deactivateCenter.mockRejectedValueOnce(error);

    render(<CenterOperationsPage />);
    await screen.findByText('Colombo National Health Center');

    const deactivateButton = await screen.findByRole('button', { name: /Deactivate/i });
    await waitFor(() => expect(deactivateButton.disabled).toBe(false));
    fireEvent.click(deactivateButton);
    await screen.findByText('Confirm Center Deactivation');

    fireEvent.click(screen.getByRole('button', { name: /Yes, Deactivate Center/i }));

    const errorMessages = await screen.findAllByText(
      /Cannot deactivate vaccination center because 2 future or in-progress session\(s\) depend on it/
    );
    expect(errorMessages.length).toBeGreaterThanOrEqual(1);
    // Modal should remain open to allow user to read the message and cancel
    expect(screen.getByText('Confirm Center Deactivation')).toBeTruthy();
  });

  it('confirms reactivation for an inactive center', async () => {
    const inactiveCenter = { ...sampleCenter2 };
    const activatedCenter = { ...sampleCenter2, status: 'ACTIVE' };

    centerApi.listCenters
      .mockResolvedValueOnce([inactiveCenter])
      .mockResolvedValueOnce([activatedCenter]);
    centerApi.getCenterById.mockResolvedValue(inactiveCenter);
    centerApi.activateCenter.mockResolvedValueOnce(activatedCenter);

    render(<CenterOperationsPage />);
    await screen.findByText('Kandy Regional Center');

    const activateBtn = screen.getByRole('button', { name: /Activate/i });
    expect(activateBtn).toBeTruthy();
    fireEvent.click(activateBtn);

    expect(await screen.findByText('Confirm Center Activation')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: /Yes, Activate Center/i }));

    await waitFor(() => {
      expect(centerApi.activateCenter).toHaveBeenCalledWith('2');
    });

    await waitFor(() => {
      expect(screen.queryByText('Confirm Center Activation')).toBeNull();
    });
  });

  it('hides mutation controls (+ New Center, Edit, Deactivate) for unauthorized viewers', async () => {
    authState.current = {
      user: { id: 2, email: 'campaign.officer@local.test', roles: ['ROLE_CAMPAIGN_OFFICER'] },
      isAuthenticated: true,
    };

    render(<CenterOperationsPage />);
    await screen.findByText('Colombo National Health Center');

    // Read-only viewer should see details but no action buttons
    expect(screen.queryByRole('button', { name: /\+ New Center/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /Edit Details/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /Deactivate/i })).toBeNull();
  });
});
