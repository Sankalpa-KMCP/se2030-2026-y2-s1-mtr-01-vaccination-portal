import React, { useState } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

export default function CampaignActionModal({
  action,
  loading,
  error,
  onConfirm,
  onCancel,
}) {
  const [reason, setReason] = useState('');
  const [actorId, setActorId] = useState(1);
  const [clientError, setClientError] = useState(null);

  if (!action) return null;

  const { type, campaign } = action;
  const isReasonRequired = type === 'SUSPEND' || type === 'RESUME';

  const getActionTitle = () => {
    switch (type) {
      case 'SCHEDULE': return `Schedule Campaign "${campaign.name}"`;
      case 'PUBLISH': return `Publish Campaign "${campaign.name}"`;
      case 'ACTIVATE': return `Activate Campaign "${campaign.name}"`;
      case 'SUSPEND': return `Suspend Campaign "${campaign.name}"`;
      case 'RESUME': return `Resume Campaign "${campaign.name}"`;
      default: return 'Confirm Lifecycle Action';
    }
  };

  const getActionDescription = () => {
    switch (type) {
      case 'SCHEDULE':
        return 'Transitioning this campaign to SCHEDULED locks the initial draft configuration and marks it ready for operational scheduling and published questionnaire validation.';
      case 'PUBLISH':
        return 'Publishing this campaign makes it visible to administrative planning systems and citizens within the designated service areas.';
      case 'ACTIVATE':
        return 'Activating this campaign opens operational vaccination sessions and enables active service workflows.';
      case 'SUSPEND':
        return 'Suspending this campaign immediately freezes active session operations. A mandatory administrative justification reason is required.';
      case 'RESUME':
        return 'Resuming this campaign restores active operations. A mandatory administrative justification reason is required.';
      default:
        return 'Are you sure you want to proceed with this state transition?';
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setClientError(null);

    if (isReasonRequired && !reason.trim()) {
      setClientError('A justification reason is mandatory for this action.');
      return;
    }

    const payload = isReasonRequired
      ? { reason: reason.trim(), actorId: Number(actorId) }
      : null;

    onConfirm(type, campaign.id, payload);
  };

  return (
    <div
      className="modal fade show d-block"
      tabIndex="-1"
      role="dialog"
      style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }}
    >
      <div className="modal-dialog modal-dialog-centered" role="document">
        <div className="modal-content shadow">
          <form onSubmit={handleSubmit}>
            <div className="modal-header bg-light">
              <h5 className="modal-title fw-bold text-dark">{getActionTitle()}</h5>
              <button
                type="button"
                className="btn-close"
                aria-label="Close"
                onClick={onCancel}
                disabled={loading}
              />
            </div>

            <div className="modal-body p-4">
              <ApiErrorAlert error={clientError || error} onDismiss={() => setClientError(null)} />

              <p className="text-secondary small mb-3">{getActionDescription()}</p>

              {isReasonRequired && (
                <div className="mt-3">
                  <div className="mb-3">
                    <label htmlFor="actionReason" className="form-label fw-semibold text-secondary small">
                      Justification Reason <span className="text-danger">*</span>
                    </label>
                    <textarea
                      id="actionReason"
                      className="form-control form-control-sm"
                      rows={3}
                      placeholder="Specify administrative justification for this lifecycle change..."
                      value={reason}
                      onChange={(e) => setReason(e.target.value)}
                      required
                    />
                  </div>

                  <div className="mb-2">
                    <label htmlFor="actorIdInput" className="form-label fw-semibold text-secondary small">
                      Officer / Actor ID <span className="text-danger">*</span>
                    </label>
                    <input
                      id="actorIdInput"
                      type="number"
                      min={1}
                      className="form-control form-control-sm"
                      value={actorId}
                      onChange={(e) => setActorId(e.target.value)}
                      required
                    />
                    <div className="form-text small" style={{ fontSize: '0.75rem' }}>
                      Coursework convention: Client-supplied authorized officer identity.
                    </div>
                  </div>
                </div>
              )}
            </div>

            <div className="modal-footer bg-light">
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={onCancel}
                disabled={loading}
              >
                Cancel
              </button>
              <button
                type="submit"
                className={`btn btn-sm fw-semibold ${
                  type === 'SUSPEND' ? 'btn-warning' : type === 'ACTIVATE' || type === 'RESUME' ? 'btn-success' : 'btn-primary'
                }`}
                disabled={loading}
              >
                {loading ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" role="status" />
                    Executing...
                  </>
                ) : (
                  `Confirm ${type}`
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
