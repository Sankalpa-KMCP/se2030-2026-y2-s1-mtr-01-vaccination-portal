import React, { useEffect, useRef, useState } from 'react';
import { useAuth } from '../../../auth/context/AuthContext';
import { canManageCampaigns, hasRole } from '../../../auth/utils/roleUtils';
import StatusBadge from '../../../components/common/StatusBadge';
import EmptyState from '../../../components/common/EmptyState';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import { campaignStatusVariants } from '../../../components/common/statusVariants';

const STATUSES = ['DRAFT', 'SCHEDULED', 'PUBLISHED', 'ACTIVE', 'SUSPENDED', 'COMPLETED', 'CANCELLED', 'ARCHIVED'];
const EDITABLE = new Set(STATUSES.slice(0, 5));
const TRANSITIONS = {
  DRAFT: ['SCHEDULE', 'CANCEL'], SCHEDULED: ['PUBLISH', 'CANCEL'],
  PUBLISHED: ['ACTIVATE', 'COMPLETE', 'CANCEL'], ACTIVE: ['SUSPEND', 'COMPLETE', 'CANCEL'],
  SUSPENDED: ['RESUME', 'COMPLETE', 'CANCEL'],
};
const label = (value) => value.charAt(0) + value.slice(1).toLowerCase();
const ACTION_ICONS = { EDIT: 'pencil', SCHEDULE: 'calendar-check', PUBLISH: 'megaphone', ACTIVATE: 'play-circle', SUSPEND: 'pause-circle', RESUME: 'play-circle', COMPLETE: 'check-circle', CANCEL: 'x-circle', ARCHIVE: 'archive' };
function formatDate(value) {
  if (!value) return 'Not set';
  const parts = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!parts) return value;
  const date = new Date(Date.UTC(Number(parts[1]), Number(parts[2]) - 1, Number(parts[3])));
  return new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric', timeZone: 'UTC' }).format(date).replace('Sept', 'Sep');
}

function CampaignActions({ campaign, isManager, isAdmin, onSelectCampaign, onEditCampaign, onInitiateAction }) {
  const [open, setOpen] = useState(false);
  const container = useRef(null);
  const trigger = useRef(null);
  const actions = [];
  if (isManager && EDITABLE.has(campaign.status)) actions.push('EDIT');
  if (isManager) actions.push(...(TRANSITIONS[campaign.status] || []));
  if (isAdmin && ['COMPLETED', 'CANCELLED'].includes(campaign.status)) actions.push('ARCHIVE');
  useEffect(() => {
    if (!open) return;
    const closeOutside = (event) => {
      if (!container.current?.contains(event.target)) setOpen(false);
    };
    document.addEventListener('pointerdown', closeOutside);
    return () => document.removeEventListener('pointerdown', closeOutside);
  }, [open]);
  return (
    <div className="campaign-row-actions" ref={container} onBlur={(event) => {
      if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false);
    }} onKeyDown={(event) => {
      if (event.key === 'Escape' && open) {
        event.preventDefault(); setOpen(false); trigger.current?.focus();
      }
    }}>
      <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => onSelectCampaign(campaign.id)}>View details</button>
      {actions.length > 0 && <>
        <button ref={trigger} type="button" className="btn btn-sm btn-outline-secondary" aria-expanded={open}
          aria-controls={`campaign-actions-${campaign.id}`} aria-label={`Actions for ${campaign.name}`}
          onClick={() => setOpen(!open)}>Actions <span aria-hidden="true">▾</span></button>
        {open && <div id={`campaign-actions-${campaign.id}`} className="campaign-action-menu" role="group" aria-label={`Available actions for ${campaign.name}`}>
          {actions.map((action) => <button key={action} type="button" className={action === 'CANCEL' ? 'campaign-cancel-action' : ''}
            onClick={() => {
              setOpen(false); trigger.current?.focus();
              if (action === 'EDIT') onEditCampaign?.(campaign);
              else onInitiateAction(action, campaign);
            }}><i className={`bi bi-${ACTION_ICONS[action]}`} aria-hidden="true" />{action === 'CANCEL' ? 'Cancel campaign' : label(action)}</button>)}
        </div>}
      </>}
    </div>
  );
}

export default function CampaignList({ campaigns, selectedStatus, onStatusFilterChange, onSelectCampaign,
  onInitiateAction, onCreateNew, onEditCampaign, searchQuery = '', onSearchChange, onClearFilters, loading = false, error = null }) {
  const { user } = useAuth();
  const isManager = canManageCampaigns(user);
  const isAdmin = hasRole(user, 'ROLE_ADMIN');
  const query = searchQuery.trim().toLowerCase();
  const results = campaigns.filter((campaign) => [campaign.name, campaign.code].some((value) => value?.toLowerCase().includes(query)));
  const filtered = Boolean(query || selectedStatus);
  return (
    <section className="campaign-list-panel" aria-label="Campaign list" aria-busy={loading}>
      <div className="campaign-directory-heading">
        <h2><i className="bi bi-grid-1x2" aria-hidden="true" />Campaign directory</h2>
        <span className="campaign-directory-count">{loading ? 'Loading' : error ? 'Unavailable' : `${results.length} ${results.length === 1 ? 'result' : 'results'}`}</span>
      </div>
      <div className="campaign-toolbar">
        <div className="campaign-search">
          <label htmlFor="campaignSearch">Search campaigns</label>
          <div className="campaign-search-control">
          <i className="bi bi-search" aria-hidden="true" />
          <input id="campaignSearch" type="search" className="form-control" placeholder="Search by campaign name or code"
            value={searchQuery} onChange={(event) => onSearchChange?.(event.target.value)} />
          </div>
        </div>
        <div className="campaign-status-filter">
          <label htmlFor="statusFilter">Status</label>
          <select id="statusFilter" className="form-select" value={selectedStatus} onChange={(event) => onStatusFilterChange(event.target.value)}>
            <option value="">All statuses</option>
            {STATUSES.map((status) => <option key={status} value={status}>{label(status)}</option>)}
          </select>
        </div>
        <button type="button" className="btn btn-link campaign-clear" disabled={!filtered} onClick={onClearFilters}>Clear filters</button>
      </div>
      <div className="campaign-directory-body">
      {loading ? <LoadingSpinner message="Fetching vaccination campaigns..." /> : error ? (
        <p className="campaign-load-error">Campaigns could not be loaded. Please try again.</p>
      ) : results.length === 0 ? (
        <EmptyState icon="bi-megaphone" title={filtered ? 'No matching campaigns' : 'No campaigns yet'}
          action={filtered ? <button type="button" className="btn btn-outline-primary" onClick={onClearFilters}>Clear filters</button>
            : isManager && <button type="button" className="btn btn-primary" onClick={onCreateNew}>Create First Campaign</button>}>
          {filtered ? 'Try another name, code, or status.' : 'Create a campaign to get started.'}
        </EmptyState>
      ) : (
        <table className="campaign-table">
          <thead><tr>{['Campaign', 'Vaccine', 'Target Group', 'Vaccination Dates', 'Status', 'Actions'].map((heading) => <th key={heading} scope="col">{heading}</th>)}</tr></thead>
          <tbody>{results.map((campaign) => <tr key={campaign.id}>
            <td data-label="Campaign" className="campaign-identity"><span className="campaign-name">{campaign.name}</span><span className="campaign-code">{campaign.code}</span></td>
            <td data-label="Vaccine">{campaign.vaccineTypeCode || 'Not set'}</td>
            <td data-label="Target group">{campaign.targetGroupCode || 'Not set'}</td>
            <td data-label="Vaccination dates" className="campaign-dates"><div><span>{formatDate(campaign.serviceStartDate)}</span><span className="campaign-date-end">to {formatDate(campaign.serviceEndDate)}</span></div></td>
            <td data-label="Status"><StatusBadge status={campaign.status} variants={campaignStatusVariants} /></td>
            <td data-label="Actions"><CampaignActions campaign={campaign} isManager={isManager} isAdmin={isAdmin}
              onSelectCampaign={onSelectCampaign} onEditCampaign={onEditCampaign} onInitiateAction={onInitiateAction} /></td>
          </tr>)}</tbody>
        </table>
      )}
      </div>
      <div className="campaign-results" role="status">{loading ? 'Loading campaigns…' : error ? 'Results unavailable' : `${results.length} ${results.length === 1 ? 'campaign' : 'campaigns'}${filtered ? ' matching your filters' : ''}`}</div>
    </section>
  );
}
