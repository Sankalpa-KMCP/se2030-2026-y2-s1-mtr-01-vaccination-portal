import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import EmptyState from '../../../components/common/EmptyState';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import PageHeader from '../../../components/common/PageHeader';
import { listCampaigns } from '../../campaign-management/api/campaignApi';
import { listScreeningReviewQueue } from '../api/screeningApi';
import {
  PRELIMINARY_SCREENING_RESULT,
  SCREENING_SUBMISSION_STATUS_LABELS,
} from '../constants/screeningConstants';
import { formatScreeningTimestamp } from '../utils/screeningHistory';

const PRELIMINARY_LABELS = {
  [PRELIMINARY_SCREENING_RESULT.PASS]: 'Preliminary pass',
  [PRELIMINARY_SCREENING_RESULT.REVIEW_REQUIRED]: 'Healthcare review required',
};

function submittedTime(submission) {
  const value = submission.submittedAt || submission.createdAt;
  const timestamp = Date.parse(value);
  return Number.isNaN(timestamp) ? Number.POSITIVE_INFINITY : timestamp;
}

function sortOldestFirst(submissions) {
  return [...submissions].sort((left, right) => {
    const byTime = submittedTime(left) - submittedTime(right);
    return byTime || Number(left.id || 0) - Number(right.id || 0);
  });
}

function campaignDisplay(campaign, campaignId) {
  if (!campaign) return { name: `Campaign #${campaignId}`, code: null };
  return { name: campaign.name || `Campaign #${campaignId}`, code: campaign.code || null };
}

export default function HealthcareOfficerScreeningReviewQueuePage() {
  const [campaignId, setCampaignId] = useState('');
  const [campaigns, setCampaigns] = useState([]);
  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [queueError, setQueueError] = useState(null);
  const [campaignError, setCampaignError] = useState(null);
  const [refreshVersion, setRefreshVersion] = useState(0);

  useEffect(() => {
    let active = true;
    listCampaigns()
      .then((result) => {
        if (active) {
          setCampaigns(Array.isArray(result) ? result : []);
          setCampaignError(null);
        }
      })
      .catch(() => {
        if (active) {
          setCampaigns([]);
          setCampaignError('Campaign details could not be loaded. Screening records remain available.');
        }
      });
    return () => { active = false; };
  }, [refreshVersion]);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setQueueError(null);
    listScreeningReviewQueue(campaignId ? { campaignId } : {})
      .then((result) => {
        if (active) setSubmissions(sortOldestFirst(Array.isArray(result) ? result : []));
      })
      .catch((error) => {
        if (active) {
          setSubmissions([]);
          setQueueError(error);
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [campaignId, refreshVersion]);

  const campaignsById = useMemo(
    () => new Map(campaigns.map((campaign) => [String(campaign.id), campaign])),
    [campaigns],
  );

  return (
    <section aria-labelledby="screening-review-queue-title">
      <PageHeader
        id="screening-review-queue-title"
        eyebrow="Healthcare Officer"
        title="Screening Review"
        subtitle="Review the longest-waiting citizen screenings and record the next clinical action."
        actions={(
          <button
            type="button"
            className="btn btn-outline-primary"
            onClick={() => setRefreshVersion((version) => version + 1)}
            disabled={loading}
          >
            <i className="bi bi-arrow-clockwise me-2" aria-hidden="true" />
            Refresh
          </button>
        )}
      />

      <div className="card border-0 shadow-sm mb-4">
        <div className="card-body p-3 p-md-4">
          <label className="form-label fw-semibold" htmlFor="screening-review-campaign-filter">
            Campaign
          </label>
          <select
            id="screening-review-campaign-filter"
            className="form-select"
            value={campaignId}
            onChange={(event) => setCampaignId(event.target.value)}
          >
            <option value="">All campaigns</option>
            {campaigns.map((campaign) => (
              <option key={campaign.id} value={campaign.id}>
                {campaign.name || `Campaign #${campaign.id}`}
              </option>
            ))}
          </select>
        </div>
      </div>

      {campaignError && <div className="alert alert-warning" role="status">{campaignError}</div>}
      {queueError && (
        <>
          <ApiErrorAlert error={queueError} />
          <button type="button" className="btn btn-outline-primary mb-4" onClick={() => setRefreshVersion((version) => version + 1)}>
            Retry loading queue
          </button>
        </>
      )}

      {loading ? (
        <LoadingSpinner message="Loading screening review queue..." />
      ) : !queueError && submissions.length === 0 ? (
        <div className="card border-0 shadow-sm">
          <EmptyState icon="bi-clipboard-check" title="No screenings are currently awaiting review.">
            New submissions requiring healthcare review will appear here.
          </EmptyState>
        </div>
      ) : !queueError ? (
        <div className="card border-0 shadow-sm overflow-hidden">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th scope="col">Citizen</th>
                  <th scope="col">Campaign</th>
                  <th scope="col">Submitted</th>
                  <th scope="col">Preliminary result</th>
                  <th scope="col">Status</th>
                  <th scope="col" className="text-end">Action</th>
                </tr>
              </thead>
              <tbody>
                {submissions.map((submission) => {
                  const campaign = campaignDisplay(
                    campaignsById.get(String(submission.campaignId)),
                    submission.campaignId,
                  );
                  return (
                    <tr key={submission.id}>
                      <td className="fw-semibold">Citizen #{submission.citizenId}</td>
                      <td>
                        <div>{campaign.name}</div>
                        {campaign.code && <div className="small text-secondary">{campaign.code}</div>}
                      </td>
                      <td>{formatScreeningTimestamp(submission.submittedAt || submission.createdAt)}</td>
                      <td>{PRELIMINARY_LABELS[submission.preliminaryResult] || submission.preliminaryResult || 'Not available'}</td>
                      <td>{SCREENING_SUBMISSION_STATUS_LABELS[submission.status] || submission.status}</td>
                      <td className="text-end">
                        <Link className="btn btn-primary btn-sm" to={`/app/staff/screening/reviews/${submission.id}`}>
                          Review
                        </Link>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      ) : null}
    </section>
  );
}
