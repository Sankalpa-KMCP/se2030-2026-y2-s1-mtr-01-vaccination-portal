import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, cleanup, fireEvent } from '@testing-library/react';
import CampaignList from './CampaignList';

const authMocks = vi.hoisted(() => ({
  currentUser: null,
}));

vi.mock('../../../auth/context/AuthContext', () => ({
  useAuth: () => ({ user: authMocks.currentUser }),
}));

describe('CampaignList — Archive Authorization and Action Visibility', () => {
  const defaultProps = {
    campaigns: [],
    selectedStatus: '',
    onStatusFilterChange: vi.fn(),
    onSelectCampaign: vi.fn(),
    onInitiateAction: vi.fn(),
    onCreateNew: vi.fn(),
  };

  const sampleCompletedCampaign = {
    id: 1,
    code: 'CMP-001',
    name: 'Completed Flu Drive',
    status: 'COMPLETED',
    vaccineTypeCode: 'FLU',
    targetGroupCode: 'ADULT',
    serviceStartDate: '2026-01-01',
    serviceEndDate: '2026-01-31',
  };

  const sampleCancelledCampaign = {
    id: 2,
    code: 'CMP-002',
    name: 'Cancelled Polio Drive',
    status: 'CANCELLED',
    vaccineTypeCode: 'POLIO',
    targetGroupCode: 'CHILDREN',
    serviceStartDate: '2026-02-01',
    serviceEndDate: '2026-02-28',
  };

  const sampleActiveCampaign = {
    id: 3,
    code: 'CMP-003',
    name: 'Active Covid Drive',
    status: 'ACTIVE',
    vaccineTypeCode: 'COVID',
    targetGroupCode: 'ALL',
    serviceStartDate: '2026-03-01',
    serviceEndDate: '2026-03-31',
  };

  const sampleDraftCampaign = {
    id: 4,
    code: 'CMP-004',
    name: 'Draft Booster Drive',
    status: 'DRAFT',
    vaccineTypeCode: 'BOOST',
    targetGroupCode: 'ELDERLY',
    serviceStartDate: '2026-04-01',
    serviceEndDate: '2026-04-30',
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders Archive button for ROLE_ADMIN on COMPLETED campaign and invokes onInitiateAction', async () => {
    authMocks.currentUser = { id: 1, roles: ['ROLE_ADMIN'] };
    const onInitiateAction = vi.fn();

    render(
      <CampaignList
        {...defaultProps}
        campaigns={[sampleCompletedCampaign]}
        onInitiateAction={onInitiateAction}
      />
    );

    const archiveBtn = screen.getByRole('button', { name: 'Archive' });
    expect(archiveBtn).toBeTruthy();

    fireEvent.click(archiveBtn);
    expect(onInitiateAction).toHaveBeenCalledWith('ARCHIVE', sampleCompletedCampaign);
  });

  it('renders Archive button for ROLE_ADMIN on CANCELLED campaign', () => {
    authMocks.currentUser = { id: 1, roles: ['ROLE_ADMIN'] };

    render(
      <CampaignList
        {...defaultProps}
        campaigns={[sampleCancelledCampaign]}
      />
    );

    expect(screen.getByRole('button', { name: 'Archive' })).toBeTruthy();
  });

  it('does NOT render Archive button for ROLE_CAMPAIGN_OFFICER on COMPLETED campaign', () => {
    authMocks.currentUser = { id: 2, roles: ['ROLE_CAMPAIGN_OFFICER'] };

    render(
      <CampaignList
        {...defaultProps}
        campaigns={[sampleCompletedCampaign]}
      />
    );

    expect(screen.queryByRole('button', { name: 'Archive' })).toBeNull();
    // Details button is still visible
    expect(screen.getByRole('button', { name: 'Details' })).toBeTruthy();
  });

  it('does NOT render Archive button for ROLE_CAMPAIGN_OFFICER on CANCELLED campaign', () => {
    authMocks.currentUser = { id: 2, roles: ['ROLE_CAMPAIGN_OFFICER'] };

    render(
      <CampaignList
        {...defaultProps}
        campaigns={[sampleCancelledCampaign]}
      />
    );

    expect(screen.queryByRole('button', { name: 'Archive' })).toBeNull();
    expect(screen.getByRole('button', { name: 'Details' })).toBeTruthy();
  });

  it('preserves other legitimate campaign management actions for ROLE_CAMPAIGN_OFFICER', () => {
    authMocks.currentUser = { id: 2, roles: ['ROLE_CAMPAIGN_OFFICER'] };

    render(
      <CampaignList
        {...defaultProps}
        campaigns={[sampleActiveCampaign, sampleDraftCampaign]}
      />
    );

    // Header New Campaign button is present
    expect(screen.getByRole('button', { name: /\+ New Campaign/i })).toBeTruthy();

    // Active campaign actions: Suspend, Complete, Cancel
    expect(screen.getByRole('button', { name: 'Suspend' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Complete' })).toBeTruthy();

    // Draft campaign actions: Schedule, Cancel
    expect(screen.getByRole('button', { name: 'Schedule' })).toBeTruthy();
    expect(screen.getAllByRole('button', { name: 'Cancel' }).length).toBeGreaterThanOrEqual(1);
  });

  it('does NOT render Archive button for non-terminal campaign states (ACTIVE, DRAFT) even for ROLE_ADMIN', () => {
    authMocks.currentUser = { id: 1, roles: ['ROLE_ADMIN'] };

    render(
      <CampaignList
        {...defaultProps}
        campaigns={[sampleActiveCampaign, sampleDraftCampaign]}
      />
    );

    expect(screen.queryByRole('button', { name: 'Archive' })).toBeNull();
  });

  it('renders Edit button for ROLE_ADMIN on allowed states and invokes onEditCampaign', () => {
    authMocks.currentUser = { id: 1, roles: ['ROLE_ADMIN'] };
    const onEditCampaign = vi.fn();

    render(
      <CampaignList
        {...defaultProps}
        campaigns={[sampleDraftCampaign, sampleActiveCampaign]}
        onEditCampaign={onEditCampaign}
      />
    );

    const editButtons = screen.getAllByRole('button', { name: 'Edit' });
    expect(editButtons.length).toBe(2);

    fireEvent.click(editButtons[0]);
    expect(onEditCampaign).toHaveBeenCalledWith(sampleDraftCampaign);
  });

  it('renders Edit button for ROLE_CAMPAIGN_OFFICER on allowed states', () => {
    authMocks.currentUser = { id: 2, roles: ['ROLE_CAMPAIGN_OFFICER'] };

    render(
      <CampaignList
        {...defaultProps}
        campaigns={[sampleDraftCampaign, sampleActiveCampaign]}
      />
    );

    const editButtons = screen.getAllByRole('button', { name: 'Edit' });
    expect(editButtons.length).toBe(2);
  });

  it('does NOT render Edit button for non-manager roles (e.g. ROLE_CITIZEN, ROLE_CENTER_MANAGER)', () => {
    authMocks.currentUser = { id: 3, roles: ['ROLE_CENTER_MANAGER'] };

    render(
      <CampaignList
        {...defaultProps}
        campaigns={[sampleDraftCampaign, sampleActiveCampaign]}
      />
    );

    expect(screen.queryByRole('button', { name: 'Edit' })).toBeNull();
  });

  it('does NOT render Edit button for closed states (COMPLETED, CANCELLED, ARCHIVED) even for ROLE_ADMIN', () => {
    authMocks.currentUser = { id: 1, roles: ['ROLE_ADMIN'] };
    const sampleArchivedCampaign = {
      id: 5,
      code: 'CMP-005',
      name: 'Archived Drive',
      status: 'ARCHIVED',
    };

    render(
      <CampaignList
        {...defaultProps}
        campaigns={[sampleCompletedCampaign, sampleCancelledCampaign, sampleArchivedCampaign]}
      />
    );

    expect(screen.queryByRole('button', { name: 'Edit' })).toBeNull();
  });
});
