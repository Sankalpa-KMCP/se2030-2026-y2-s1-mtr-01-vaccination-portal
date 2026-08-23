import React, { useState, useEffect } from 'react';
import { listStaffRoster } from '../../center-operations/api/centerOperationsApi';
import { createStaffAssignment } from '../api/sessionAssignmentsApi';
import { localToUtcIso, formatDateInTimezone, formatTimeInTimezone } from '../api/sessionApi';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';

export default function AssignStaffModal({
  isOpen,
  onClose,
  session,
  onSuccess,
}) {
  const [roster, setRoster] = useState([]);
  const [loadingRoster, setLoadingRoster] = useState(false);
  const [selectedStaffRosterId, setSelectedStaffRosterId] = useState('');
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
      setRoster([]);
      setSelectedStaffRosterId('');
      setUseCustomInterval(false);
      setCustomStartTime('');
      setCustomEndTime('');
      setError(null);
      return;
    }

    setLoadingRoster(true);
    setError(null);
    setCustomStartTime(defaultStartTime);
    setCustomEndTime(defaultEndTime);

    listStaffRoster(session.centerId, null, 'ACTIVE')
      .then((data) => {
        const activeRoster = (data || []).filter((s) => s.status === 'ACTIVE');
        setRoster(activeRoster);
        if (activeRoster.length > 0) {
          setSelectedStaffRosterId(String(activeRoster[0].id));
        }
        setLoadingRoster(false);
      })
      .catch((err) => {
        setError(err);
        setLoadingRoster(false);
      });
  }, [isOpen, session, defaultStartTime, defaultEndTime]);

  if (!isOpen || !session) return null;

  const selectedMember = roster.find((s) => String(s.id) === String(selectedStaffRosterId));

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selectedStaffRosterId || !selectedMember) {
      setError(new Error('Please select an active staff member to assign.'));
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
        staffRosterId: Number(selectedStaffRosterId),
        role: selectedMember.role,
        startAt: startAtIso,
        endAt: endAtIso,
      };

      await createStaffAssignment(session.id, payload);
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
      aria-labelledby="assignStaffModalTitle"
      style={{ backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 1060 }}
    >
      <div className="modal-dialog modal-dialog-centered" role="document">
        <div className="modal-content border-0 shadow">
          <div className="modal-header bg-light border-bottom">
            <h5 className="modal-title h5 mb-0 text-dark fw-bold" id="assignStaffModalTitle">
              Assign Operational Staff
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

              {loadingRoster && <LoadingSpinner message="Loading active center staff roster..." />}

              {!loadingRoster && (
                <div className="d-flex flex-column gap-3">
                  {/* Staff Select */}
                  <div>
                    <label htmlFor="staffSelect" className="form-label fw-semibold text-dark mb-1">
                      Center Staff Member <span className="text-danger">*</span>
                    </label>
                    {roster.length === 0 ? (
                      <div className="alert alert-warning py-2 small mb-0">
                        No active staff roster entries found for this center.
                      </div>
                    ) : (
                      <select
                        id="staffSelect"
                        className="form-select"
                        value={selectedStaffRosterId}
                        onChange={(e) => setSelectedStaffRosterId(e.target.value)}
                        disabled={submitting}
                        required
                      >
                        {roster.map((s) => (
                          <option key={s.id} value={s.id}>
                            User #{s.userId} — {s.role}
                          </option>
                        ))}
                      </select>
                    )}
                    {selectedMember && (
                      <div className="mt-2 small text-muted">
                        Assigned Role: <strong className="text-dark">{selectedMember.role}</strong> (Derived from center qualification)
                      </div>
                    )}
                    <small className="text-muted d-block mt-1">
                      Note: Only staff with role <code>VACCINATOR</code> count toward session scheduling readiness.
                    </small>
                  </div>

                  {/* Interval Mode */}
                  <div className="card border bg-light p-3">
                    <div className="form-check mb-2">
                      <input
                        type="checkbox"
                        className="form-check-input"
                        id="useCustomStaffInterval"
                        checked={useCustomInterval}
                        onChange={(e) => setUseCustomInterval(e.target.checked)}
                        disabled={submitting}
                      />
                      <label className="form-check-label fw-semibold text-dark" htmlFor="useCustomStaffInterval">
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
                          <label htmlFor="customStaffStart" className="form-label small text-muted mb-1">
                            Start Time ({tz})
                          </label>
                          <input
                            type="time"
                            id="customStaffStart"
                            className="form-control form-control-sm"
                            value={customStartTime}
                            onChange={(e) => setCustomStartTime(e.target.value)}
                            disabled={submitting}
                            required
                          />
                        </div>
                        <div className="col-6">
                          <label htmlFor="customStaffEnd" className="form-label small text-muted mb-1">
                            End Time ({tz})
                          </label>
                          <input
                            type="time"
                            id="customStaffEnd"
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
                disabled={submitting || loadingRoster || roster.length === 0}
              >
                {submitting ? 'Assigning...' : 'Assign Staff'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
