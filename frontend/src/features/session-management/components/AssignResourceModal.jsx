/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 */

import React, { useState, useEffect } from 'react';
import { listOperationalResources } from '../../center-operations/api/centerOperationsApi';
import { createResourceAssignment } from '../api/sessionAssignmentsApi';
import { localToUtcIso, formatDateInTimezone, formatTimeInTimezone } from '../api/sessionApi';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';

export default function AssignResourceModal({
  isOpen,
  onClose,
  session,
  onSuccess,
}) {
  const [resources, setResources] = useState([]);
  const [loadingResources, setLoadingResources] = useState(false);
  const [selectedResourceId, setSelectedResourceId] = useState('');
  const [useCustomInterval, setUseCustomInterval] = useState(false);
  const [customStartTime, setCustomStartTime] = useState('');
  const [customEndTime, setCustomEndTime] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const tz = session?.scheduleTimezone || 'UTC';
  const sessionDate = session?.startAt ? formatDateInTimezone(session.startAt, tz) : '';
  const defaultStartTime = session?.startAt ? formatTimeInTimezone(session.startAt, tz) : '09:00';
  const defaultEndTime = session?.endAt ? formatTimeInTimezone(session.endAt, tz) : '17:00';

  useEffect(() => {
    if (!isOpen || !session?.centerId) {
      setResources([]);
      setSelectedResourceId('');
      setUseCustomInterval(false);
      setCustomStartTime('');
      setCustomEndTime('');
      setError(null);
      return;
    }

    setLoadingResources(true);
    setError(null);
    setCustomStartTime(defaultStartTime);
    setCustomEndTime(defaultEndTime);

    listOperationalResources(session.centerId)
      .then((data) => {
        const activeResources = (data || []).filter((r) => r.status === 'ACTIVE');
        setResources(activeResources);
        if (activeResources.length > 0) {
          setSelectedResourceId(String(activeResources[0].id));
        }
        setLoadingResources(false);
      })
      .catch((err) => {
        setError(err);
        setLoadingResources(false);
      });
  }, [isOpen, session, defaultStartTime, defaultEndTime]);

  if (!isOpen || !session) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selectedResourceId) {
      setError(new Error('Please select an operational resource to assign.'));
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      let startAtIso = session.startAt;
      let endAtIso = session.endAt;

      if (useCustomInterval) {
        if (!customStartTime || !customEndTime) {
          throw new Error('Please specify both start and end times for the custom assignment interval.');
        }
        startAtIso = localToUtcIso(sessionDate, customStartTime, tz);
        endAtIso = localToUtcIso(sessionDate, customEndTime, tz);

        if (startAtIso >= endAtIso) {
          throw new Error('Assignment start time must be before assignment end time.');
        }
        if (startAtIso < session.startAt || endAtIso > session.endAt) {
          throw new Error('Assignment interval must fall strictly within the session operating window.');
        }
      }

      const payload = {
        resourceId: Number(selectedResourceId),
        startAt: startAtIso,
        endAt: endAtIso,
      };

      await createResourceAssignment(session.id, payload);
      setSubmitting(false);
      onSuccess?.();
      onClose?.();
    } catch (err) {
      setError(err);
      setSubmitting(false);
    }
  };

  return (
    <div
      className="modal fade show d-block"
      tabIndex="-1"
      role="dialog"
      aria-labelledby="assignResourceModalTitle"
      style={{ backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 1060 }}
    >
      <div className="modal-dialog modal-dialog-centered" role="document">
        <div className="modal-content border-0 shadow">
          <div className="modal-header bg-light border-bottom">
            <h5 className="modal-title h5 mb-0 text-dark fw-bold" id="assignResourceModalTitle">
              Assign Operational Resource
            </h5>
            <button
              type="button"
              className="btn-close"
              aria-label="Close"
              disabled={submitting}
              onClick={onClose}
            />
          </div>

          <form onSubmit={handleSubmit}>
            <div className="modal-body p-4">
              {error && <ApiErrorAlert error={error} />}

              {loadingResources && <LoadingSpinner message="Loading active center resources..." />}

              {!loadingResources && (
                <div className="d-flex flex-column gap-3">
                  {/* Resource Select */}
                  <div>
                    <label htmlFor="resourceSelect" className="form-label fw-semibold text-dark mb-1">
                      Center Resource <span className="text-danger">*</span>
                    </label>
                    {resources.length === 0 ? (
                      <div className="alert alert-warning py-2 small mb-0">
                        No active operational resources found for this center.
                      </div>
                    ) : (
                      <select
                        id="resourceSelect"
                        className="form-select"
                        value={selectedResourceId}
                        onChange={(e) => setSelectedResourceId(e.target.value)}
                        disabled={submitting}
                        required
                      >
                        {resources.map((r) => (
                          <option key={r.id} value={r.id}>
                            [{r.resourceType}] {r.code} — {r.name}
                          </option>
                        ))}
                      </select>
                    )}
                    <small className="text-muted d-block mt-1">
                      Note: Only resources of type <code>VACCINATION_STATION</code> count toward session scheduling readiness.
                    </small>
                  </div>

                  {/* Interval Mode */}
                  <div className="card border bg-light p-3">
                    <div className="form-check mb-2">
                      <input
                        type="checkbox"
                        className="form-check-input"
                        id="useCustomResourceInterval"
                        checked={useCustomInterval}
                        onChange={(e) => setUseCustomInterval(e.target.checked)}
                        disabled={submitting}
                      />
                      <label className="form-check-label fw-semibold text-dark" htmlFor="useCustomResourceInterval">
                        Specify custom sub-interval within session window
                      </label>
                    </div>

                    {!useCustomInterval ? (
                      <div className="small text-muted">
                        Assignment covers full session window:{' '}
                        <strong>
                          {defaultStartTime} – {defaultEndTime} ({tz})
                        </strong>
                      </div>
                    ) : (
                      <div className="row g-2 mt-1">
                        <div className="col-6">
                          <label htmlFor="customResStart" className="form-label small text-muted mb-1">
                            Start Time ({tz})
                          </label>
                          <input
                            type="time"
                            id="customResStart"
                            className="form-control form-control-sm"
                            value={customStartTime}
                            onChange={(e) => setCustomStartTime(e.target.value)}
                            disabled={submitting}
                            required
                          />
                        </div>
                        <div className="col-6">
                          <label htmlFor="customResEnd" className="form-label small text-muted mb-1">
                            End Time ({tz})
                          </label>
                          <input
                            type="time"
                            id="customResEnd"
                            className="form-control form-control-sm"
                            value={customEndTime}
                            onChange={(e) => setCustomEndTime(e.target.value)}
                            disabled={submitting}
                            required
                          />
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>

            <div className="modal-footer bg-light">
              <button
                type="button"
                className="btn btn-secondary"
                disabled={submitting}
                onClick={onClose}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={submitting || loadingResources || resources.length === 0}
              >
                {submitting ? 'Assigning...' : 'Assign Resource'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
