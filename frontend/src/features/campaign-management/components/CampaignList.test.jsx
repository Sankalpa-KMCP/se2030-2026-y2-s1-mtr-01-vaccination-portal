import React, { useState } from 'react';
import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, cleanup, fireEvent } from '@testing-library/react';
import CampaignList from './CampaignList';
const auth = vi.hoisted(() => ({ user: null }));
vi.mock('../../../auth/context/AuthContext', () => ({ useAuth: () => ({ user: auth.user }) }));
afterEach(cleanup);
const campaign = { id: 1, name: 'Flu campaign', code: 'CMP-FLU', status: 'ACTIVE', vaccineTypeCode: 'FLU', targetGroupCode: 'ADULT', serviceStartDate: '2026-09-01', serviceEndDate: '2026-10-31' };
const props = { campaigns: [campaign], selectedStatus: '', onStatusFilterChange: vi.fn(), onSelectCampaign: vi.fn(), onInitiateAction: vi.fn(), onCreateNew: vi.fn(), onEditCampaign: vi.fn() };
function openActions() { fireEvent.click(screen.getByRole('button', { name: /Actions for/ })); }
describe('Campaign list navigation', () => {
  it.each([
    ['DRAFT', ['Edit', 'Schedule', 'Cancel campaign']],
    ['SCHEDULED', ['Edit', 'Publish', 'Cancel campaign']],
    ['PUBLISHED', ['Edit', 'Activate', 'Complete', 'Cancel campaign']],
    ['ACTIVE', ['Edit', 'Suspend', 'Complete', 'Cancel campaign']],
    ['SUSPENDED', ['Edit', 'Resume', 'Complete', 'Cancel campaign']],
    ['COMPLETED', ['Archive']], ['CANCELLED', ['Archive']], ['ARCHIVED', []],
  ])('keeps administrator actions correct for %s', (status, expected) => {
    auth.user = { roles: ['ROLE_ADMIN'] };
    render(<CampaignList {...props} campaigns={[{ ...campaign, status }]} />);
    expect(screen.getByRole('button', { name: 'View details' })).toBeTruthy();
    if (expected.length) {
      openActions();
      const group = screen.getByRole('group', { name: /Available actions/ });
      expect(Array.from(group.querySelectorAll('button')).map((button) => button.textContent)).toEqual(expected);
    } else expect(screen.queryByRole('button', { name: /Actions for/ })).toBeNull();
  });
  it.each(['ROLE_CENTER_MANAGER', 'ROLE_MANAGEMENT_USER', 'ROLE_CITIZEN'])('hides mutations for %s', (role) => {
    auth.user = { roles: [role] };
    render(<CampaignList {...props} />);
    expect(screen.queryByRole('button', { name: /Actions for/ })).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: 'View details' }));
    expect(props.onSelectCampaign).toHaveBeenCalledWith(1);
  });
  it('allows campaign officer actions but restricts archiving', () => {
    auth.user = { roles: ['ROLE_CAMPAIGN_OFFICER'] };
    const view = render(<CampaignList {...props} />);
    openActions();
    expect(screen.getByRole('button', { name: 'Suspend' })).toBeTruthy();
    view.rerender(<CampaignList {...props} campaigns={[{ ...campaign, status: 'COMPLETED' }]} />);
    expect(screen.queryByRole('button', { name: /Actions for/ })).toBeNull();
  });
  it('returns focus on Escape and calls existing lifecycle and edit callbacks', () => {
    auth.user = { roles: ['ROLE_ADMIN'] };
    render(<CampaignList {...props} />);
    openActions();
    const edit = screen.getByRole('button', { name: 'Edit' });
    edit.focus(); fireEvent.keyDown(edit, { key: 'Escape' });
    expect(document.activeElement).toBe(screen.getByRole('button', { name: /Actions for/ }));
    expect(screen.queryByRole('button', { name: 'Edit' })).toBeNull();
    openActions(); fireEvent.click(screen.getByRole('button', { name: 'Edit' }));
    expect(props.onEditCampaign).toHaveBeenCalledWith(campaign);
    openActions(); fireEvent.click(screen.getByRole('button', { name: 'Cancel campaign' }));
    expect(props.onInitiateAction).toHaveBeenCalledWith('CANCEL', campaign);
  });
  it('searches names and codes without case or surrounding spaces and clears both filters', () => {
    auth.user = { roles: ['ROLE_ADMIN'] };
    function Harness() {
      const [query, setQuery] = useState(''); const [status, setStatus] = useState('ACTIVE');
      return <CampaignList {...props} searchQuery={query} onSearchChange={setQuery} selectedStatus={status}
        onStatusFilterChange={setStatus} onClearFilters={() => { setQuery(''); setStatus(''); }} />;
    }
    render(<Harness />);
    fireEvent.change(screen.getByLabelText('Search campaigns'), { target: { value: '  cmp-FLU  ' } });
    expect(screen.getByText('1 campaign matching your filters')).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Search campaigns'), { target: { value: ' flu CAMPAIGN ' } });
    expect(screen.getByText('Flu campaign')).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Search campaigns'), { target: { value: 'missing' } });
    expect(screen.getByText('No matching campaigns')).toBeTruthy();
    fireEvent.click(screen.getAllByRole('button', { name: 'Clear filters' })[0]);
    expect(screen.getByLabelText('Status').value).toBe('');
    expect(screen.getByLabelText('Search campaigns').value).toBe('');
    expect(screen.getByText('1 campaign')).toBeTruthy();
  });
  it('keeps controls visible during loading and does not show an empty state', () => {
    render(<CampaignList {...props} campaigns={[]} loading />);
    expect(screen.getByLabelText('Search campaigns')).toBeTruthy();
    expect(screen.getByLabelText('Status')).toBeTruthy();
    expect(screen.queryByText('No campaigns yet')).toBeNull();
  });
  it('distinguishes empty data, load errors, and readable dates', () => {
    const view = render(<CampaignList {...props} campaigns={[]} />);
    expect(screen.getByText('No campaigns yet')).toBeTruthy();
    view.rerender(<CampaignList {...props} campaigns={[]} error={new Error('failed')} />);
    expect(screen.queryByText('No campaigns yet')).toBeNull();
    expect(screen.getByText('Results unavailable')).toBeTruthy();
    view.rerender(<CampaignList {...props} />);
    expect(screen.getByText('01 Sep 2026')).toBeTruthy();
    expect(screen.queryByText('Booking Window')).toBeNull();
  });
});
