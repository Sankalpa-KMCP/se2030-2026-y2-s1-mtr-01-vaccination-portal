import React from 'react';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

function getStatusBadgeClass(status) {
  switch (status) {
    case 'DRAFT': return 'badge bg-secondary';
    case 'SCHEDULED': return 'badge bg-info text-dark';
    case 'PUBLISHED': return 'badge bg-primary';
    case 'ACTIVE': return 'badge bg-success';
    case 'SUSPENDED': return 'badge bg-warning text-dark';
    case 'CANCELLED': return 'badge bg-danger';
    case 'COMPLETED': return 'badge bg-dark';
    case 'ARCHIVED': return 'badge bg-secondary opacity-75';
    default: return 'badge bg-secondary';
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '—';
  try {
    const d = new Date(dateStr);
    return isNaN(d.getTime()) ? dateStr : d.toLocaleDateString();
  } catch {
    return dateStr;
  }
}

function formatDateTime(dateTimeStr) {
  if (!dateTimeStr) return '—';
  try {
    const d = new Date(dateTimeStr);
    return isNaN(d.getTime()) ? dateTimeStr : d.toLocaleString();
  } catch {
    return dateTimeStr;
  }
}

export default function CampaignDetailsModal({
  campaign,
  lifecycleEvents,
  loading,
  error,
  onClose,
  onInitiateAction,
}) {
  if (!campaign && !loading && !error) return null;

  return (
    <div
      className="modal fade show d-block"
      tabIndex="-1"
      role="dialog"
      style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }}
    >
      <div className="modal-dialog modal-lg modal-dialog-scrollable" role="document">
        <div className="modal-content shadow">
          <div className="modal-header bg-light">
            <h5 className="modal-title fw-bold">
              {campaign ? `${campaign.name} (${campaign.code})` : 'Campaign Details'}
            </h5>
            <button
              type="button"
              className="btn-close"
              aria-label="Close"
              onClick={onClose}
            />
          </div>

          <div className="modal-body p-4">
            <ApiErrorAlert error={error} />

            {loading ? (
              <LoadingSpinner message="Loading campaign details..." />
            ) : campaign ? (
              <>
                <div className="d-flex justify-content-between align-items-center mb-3">
                  <div>
                    <span className="text-muted small">Status: </span>
                    <span className={`${getStatusBadgeClass(campaign.status)} fs-6 ms-1`}>
                      {campaign.status}
                    </span>
                  </div>
                  <div className="small text-muted font-monospace">
                    ID: #{campaign.id}
                  </div>
                </div>

                {campaign.description && (
                  <div className="alert alert-light border small text-secondary mb-4">
                    {campaign.description}
                  </div>
                )}

                <div className="row g-3 mb-4">
                  <div className="col-md-6">
                    <div className="p-3 bg-light rounded border h-100">
                      <h6 className="fw-bold text-dark mb-2">Vaccine & Target Scope</h6>
                      <ul className="list-unstyled mb-0 small">
                        <li className="mb-1">
                          <strong>Vaccine Type: </strong>
                          {campaign.vaccineType ? (
                            <span>{campaign.vaccineType.name} ({campaign.vaccineType.code})</span>
                          ) : (
                            <span className="text-muted">N/A</span>
                          )}
                        </li>
                        <li>
                          <strong>Target Group: </strong>
                          {campaign.targetGroup ? (
                            <span>{campaign.targetGroup.name} ({campaign.targetGroup.code})</span>
                          ) : (
                            <span className="text-muted">N/A</span>
                          )}
                        </li>
                      </ul>
                    </div>
                  </div>

                  <div className="col-md-6">
                    <div className="p-3 bg-light rounded border h-100">
                      <h6 className="fw-bold text-dark mb-2">Schedule & Booking Windows</h6>
                      <ul className="list-unstyled mb-0 small">
                        <li className="mb-1">
                          <strong>Service Period: </strong>
                          {formatDate(campaign.serviceStartDate)} to {formatDate(campaign.serviceEndDate)}
                        </li>
                        <li>
                          <strong>Booking Window: </strong>
                          {formatDateTime(campaign.bookingOpenAt)} to {formatDateTime(campaign.bookingCloseAt)}
                        </li>
                      </ul>
                    </div>
                  </div>
                </div>

                <h6 className="fw-bold text-dark border-bottom pb-2 mb-3">
                  Lifecycle Audit History
                </h6>

                {lifecycleEvents && lifecycleEvents.length > 0 ? (
                  <div className="table-responsive mb-3">
                    <table className="table table-sm table-striped small mb-0">
                      <thead>
                        <tr>
                          <th>Transition</th>
                          <th>Reason</th>
                          <th>Actor ID</th>
                          <th>Timestamp</th>
                        </tr>
                      </thead>
                      <tbody>
                        {lifecycleEvents.map((evt) => (
                          <tr key={evt.id}>
                            <td>
                              <span className="badge bg-secondary me-1">{evt.fromStatus}</span>
                              &rarr;
                              <span className="badge bg-primary ms-1">{evt.toStatus}</span>
                            </td>
                            <td>{evt.reason || '—'}</td>
                            <td className="font-monospace">#{evt.actorId || '1'}</td>
                            <td>{formatDateTime(evt.occurredAt)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <p className="text-muted small mb-3">No lifecycle state transition events recorded yet.</p>
                )}

                <div className="d-flex flex-wrap gap-2 pt-2 border-top">
                  <span className="text-muted small align-self-center me-2">Actions:</span>
                  {campaign.status === 'DRAFT' && (
                    <button
                      type="button"
                      className="btn btn-sm btn-info text-dark"
                      onClick={() => onInitiateAction('SCHEDULE', campaign)}
                    >
                      Schedule Campaign
                    </button>
                  )}
                  {campaign.status === 'SCHEDULED' && (
                    <button
                      type="button"
                      className="btn btn-sm btn-primary"
                      onClick={() => onInitiateAction('PUBLISH', campaign)}
                    >
                      Publish Campaign
                    </button>
                  )}
                  {campaign.status === 'PUBLISHED' && (
                    <button
                      type="button"
                      className="btn btn-sm btn-success"
                      onClick={() => onInitiateAction('ACTIVATE', campaign)}
                    >
                      Activate Campaign
                    </button>
                  )}
                  {campaign.status === 'ACTIVE' && (
                    <button
                      type="button"
                      className="btn btn-sm btn-warning text-dark"
                      onClick={() => onInitiateAction('SUSPEND', campaign)}
                    >
                      Suspend Campaign
                    </button>
                  )}
                  {campaign.status === 'SUSPENDED' && (
                    <button
                      type="button"
                      className="btn btn-sm btn-success"
                      onClick={() => onInitiateAction('RESUME', campaign)}
                    >
                      Resume Campaign
                    </button>
                  )}
                </div>
              </>
            ) : null}
          </div>

          <div className="modal-footer bg-light">
            <button type="button" className="btn btn-secondary btn-sm" onClick={onClose}>
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
