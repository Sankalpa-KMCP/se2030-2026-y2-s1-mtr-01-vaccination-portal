import React from 'react';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import SessionCapacityPlanTab from './SessionCapacityPlanTab';

const api = vi.hoisted(() => ({
  listCapacityPlans: vi.fn(),
  previewCapacityPlan: vi.fn(),
  createCapacityPlan: vi.fn(),
}));

vi.mock('../api/sessionApi', () => ({
  ...api,
  formatInTimezone: () => 'formatted',
}));

const draftSession = {
  id: 61,
  lifecycleStatus: 'DRAFT',
  scheduleTimezone: 'Asia/Colombo',
};

function preview(recommendedThroughput = 10) {
  return {
    recommendedThroughput,
    effectiveStationCount: 1,
    assignedVaccinatorCount: 1,
    availableStationCount: 1,
    fullyEquippedStationCount: 1,
    sessionServiceCeiling: 30,
    calculatedAt: '2026-10-01T00:00:00Z',
  };
}

async function enterPlanAndPreview(approvedCapacity) {
  fireEvent.change(screen.getByLabelText(/Fully Equipped Stations/), { target: { value: '1' } });
  fireEvent.change(screen.getByLabelText(/Target Approved Capacity/), { target: { value: String(approvedCapacity) } });
  fireEvent.click(screen.getByRole('button', { name: 'Preview Calculation' }));
  await screen.findByText('Server Calculation Preview');
}

beforeEach(() => {
  api.listCapacityPlans.mockReset().mockResolvedValue([]);
  api.previewCapacityPlan.mockReset().mockResolvedValue(preview());
  api.createCapacityPlan.mockReset().mockResolvedValue({ revision: 1 });
});

afterEach(cleanup);

describe('SessionCapacityPlanTab authorization', () => {
  it('allows CENTER_MANAGER to create a normal plan and refreshes history', async () => {
    render(<SessionCapacityPlanTab session={draftSession} canManage canOverride />);
    await enterPlanAndPreview(8);

    fireEvent.click(screen.getByRole('button', { name: 'Approve & Create Capacity Plan' }));
    await waitFor(() => expect(api.createCapacityPlan).toHaveBeenCalledTimes(1));
    expect(api.listCapacityPlans).toHaveBeenCalledTimes(2);
  });

  it('shows and submits override reason only for CENTER_MANAGER when required', async () => {
    render(<SessionCapacityPlanTab session={draftSession} canManage canOverride />);
    await enterPlanAndPreview(12);

    const reason = screen.getByLabelText(/Capacity Override Reason/);
    fireEvent.change(reason, { target: { value: 'Extra surge team confirmed' } });
    fireEvent.click(screen.getByRole('button', { name: 'Approve & Create Capacity Plan' }));

    await waitFor(() => expect(api.createCapacityPlan).toHaveBeenCalledWith(
      draftSession.id,
      expect.objectContaining({ overrideReason: 'Extra surge team confirmed' }),
    ));
  });

  it('allows ADMIN normal planning without exposing override reason', async () => {
    render(<SessionCapacityPlanTab session={draftSession} canManage canOverride={false} />);
    await enterPlanAndPreview(8);

    expect(screen.queryByLabelText(/Capacity Override Reason/)).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: 'Approve & Create Capacity Plan' }));
    await waitFor(() => expect(api.createCapacityPlan).toHaveBeenCalledTimes(1));
  });

  it('prevents ADMIN from submitting an above-recommendation plan', async () => {
    render(<SessionCapacityPlanTab session={draftSession} canManage canOverride={false} />);
    await enterPlanAndPreview(12);

    expect(screen.queryByLabelText(/Capacity Override Reason/)).toBeNull();
    expect(screen.getByRole('alert').textContent).toContain(
      'A CENTER_MANAGER must authorize capacity above the recommendation.',
    );
    const createButton = screen.getByRole('button', { name: 'Approve & Create Capacity Plan' });
    expect(createButton.disabled).toBe(true);
    fireEvent.click(createButton);
    expect(api.createCapacityPlan).not.toHaveBeenCalled();
  });

  it('keeps HEALTHCARE_OFFICER on history-only access', async () => {
    render(<SessionCapacityPlanTab session={draftSession} canManage={false} canOverride={false} />);

    await waitFor(() => expect(api.listCapacityPlans).toHaveBeenCalledWith(draftSession.id));
    expect(screen.getByText('Immutable Revision History')).toBeTruthy();
    expect(screen.queryByText('Plan & Preview Capacity')).toBeNull();
    expect(api.previewCapacityPlan).not.toHaveBeenCalled();
  });

  it.each(['SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'])(
    'keeps a manager read-only in %s',
    async (status) => {
      render(
        <SessionCapacityPlanTab
          session={{ ...draftSession, lifecycleStatus: status }}
          canManage
          canOverride
        />,
      );

      expect(await screen.findByText(/Capacity plans are read-only because session is in/)).toBeTruthy();
      expect(screen.queryByText('Plan & Preview Capacity')).toBeNull();
    },
  );

  it('clears override state when override permission is lost', async () => {
    const view = render(<SessionCapacityPlanTab session={draftSession} canManage canOverride />);
    await enterPlanAndPreview(12);
    fireEvent.change(screen.getByLabelText(/Capacity Override Reason/), { target: { value: 'Temporary reason' } });

    view.rerender(<SessionCapacityPlanTab session={draftSession} canManage canOverride={false} />);
    await waitFor(() => expect(screen.queryByText('Server Calculation Preview')).toBeNull());

    view.rerender(<SessionCapacityPlanTab session={draftSession} canManage canOverride />);
    expect(screen.queryByDisplayValue('Temporary reason')).toBeNull();
  });

  it('surfaces a backend 403 as defense-in-depth', async () => {
    api.createCapacityPlan.mockRejectedValue(Object.assign(new Error('Forbidden'), { status: 403 }));
    render(<SessionCapacityPlanTab session={draftSession} canManage canOverride />);
    await enterPlanAndPreview(8);

    fireEvent.click(screen.getByRole('button', { name: 'Approve & Create Capacity Plan' }));
    expect(await screen.findByText(/not authorized to approve a capacity override/)).toBeTruthy();
  });
});
