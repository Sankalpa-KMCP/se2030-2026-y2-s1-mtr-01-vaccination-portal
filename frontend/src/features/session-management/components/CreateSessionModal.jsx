import React, { useState, useEffect, useRef } from 'react';
import { createSession, localToUtcIso } from '../api/sessionApi';
import { getCenterAssignments } from '../../campaign-management/api/campaignConfigurationApi';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';

export default function CreateSessionModal({
  isOpen,
  onClose,
  onSuccess,
  campaigns = [],
  centersMap = {},
}) {
  const [campaignId, setCampaignId] = useState('');
  const [centerId, setCenterId] = useState('');
  const [sessionDate, setSessionDate] = useState('');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [bookingCloseMode, setBookingCloseMode] = useState('SAME_AS_START'); // 'SAME_AS_START' | 'CUSTOM'
  const [bookingCloseDate, setBookingCloseDate] = useState('');
  const [bookingCloseTime, setBookingCloseTime] = useState('');

  const [assignments, setAssignments] = useState([]);
  const [loadingAssignments, setLoadingAssignments] = useState(false);
  const [assignmentError, setAssignmentError] = useState(null);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);

  // Race condition guard for campaign center assignments
  const latestCampaignReqRef = useRef(null);

  useEffect(() => {
    if (!isOpen) {
      // Reset form when modal closes
      setCampaignId('');
      setCenterId('');
      setSessionDate('');
      setStartTime('');
      setEndTime('');
      setBookingCloseMode('SAME_AS_START');
      setBookingCloseDate('');
      setBookingCloseTime('');
      setAssignments([]);
      setLoadingAssignments(false);
      setAssignmentError(null);
      setIsSubmitting(false);
      setSubmitError(null);
      latestCampaignReqRef.current = null;
    }
  }, [isOpen]);

  // Load center assignments whenever campaignId changes
  useEffect(() => {
    if (!campaignId) {
      setAssignments([]);
      setCenterId('');
      setAssignmentError(null);
      setLoadingAssignments(false);
      return;
    }

    const currentReqId = Symbol('campaign_assignment_req');
    latestCampaignReqRef.current = currentReqId;

    setLoadingAssignments(true);
    setAssignmentError(null);
    setCenterId('');

    getCenterAssignments(campaignId)
      .then((data) => {
        if (latestCampaignReqRef.current !== currentReqId) return; // Stale request
        // Filter out cancelled assignments
        const validAssignments = (data || []).filter(
          (a) => a.status === 'ACTIVE' || a.status === 'FUTURE'
        );
        setAssignments(validAssignments);
        setLoadingAssignments(false);
      })
      .catch((err) => {
        if (latestCampaignReqRef.current !== currentReqId) return; // Stale request
        setAssignmentError(err?.message || 'Failed to load campaign center assignments');
        setLoadingAssignments(false);
        setAssignments([]);
      });
  }, [campaignId]);

  if (!isOpen) return null;

  const selectedCenter = centerId && centersMap[centerId] ? centersMap[centerId] : null;
  const scheduleTimezone = selectedCenter?.timezone || 'UTC';

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitError(null);

    // Validation
    if (!campaignId) {
      setSubmitError(new Error('Please select a vaccination campaign'));
      return;
    }
    if (!centerId) {
      setSubmitError(new Error('Please select an authorized vaccination center'));
      return;
    }
    if (!sessionDate) {
      setSubmitError(new Error('Session date is required'));
      return;
    }
    if (!startTime) {
      setSubmitError(new Error('Session start time is required'));
      return;
    }
    if (!endTime) {
      setSubmitError(new Error('Session end time is required'));
      return;
    }
    if (startTime >= endTime) {
      setSubmitError(new Error('Session start time must be strictly before session end time'));
      return;
    }

    let startAtIso;
    let endAtIso;
    let bookingCloseAtIso;

    try {
      startAtIso = localToUtcIso(sessionDate, startTime, scheduleTimezone);
      endAtIso = localToUtcIso(sessionDate, endTime, scheduleTimezone);

      if (bookingCloseMode === 'SAME_AS_START') {
        bookingCloseAtIso = startAtIso;
      } else {
        if (!bookingCloseDate || !bookingCloseTime) {
          setSubmitError(new Error('Custom booking close date and time are required'));
          return;
        }
        bookingCloseAtIso = localToUtcIso(bookingCloseDate, bookingCloseTime, scheduleTimezone);

        if (bookingCloseAtIso > startAtIso) {
          setSubmitError(new Error('Booking close time cannot be after session start time'));
          return;
        }
      }
    } catch (tzErr) {
      setSubmitError(tzErr);
      return;
    }

    const payload = {
      campaignId: Number(campaignId),
      centerId: Number(centerId),
      startAt: startAtIso,
      endAt: endAtIso,
      bookingCloseAt: bookingCloseAtIso,
    };

    setIsSubmitting(true);
    try {
      const created = await createSession(payload);
      setIsSubmitting(false);
      if (onSuccess) {
        onSuccess(created);
      }
      onClose();
    } catch (err) {
      setIsSubmitting(false);
      setSubmitError(err);
    }
  };

  return (
    <div
      className="modal fade show d-block"
      tabIndex="-1"
      role="dialog"
      aria-labelledby="createSessionModalTitle"
      style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
    >
      <div className="modal-dialog modal-lg modal-dialog-centered" role="document">
        <div className="modal-content border-0 shadow">
          <div className="modal-header bg-primary text-white">
            <h5 className="modal-title h5 mb-0" id="createSessionModalTitle">
              Create Draft Vaccination Session
            </h5>
            <button
              type="button"
              className="btn-close btn-close-white"
              aria-label="Close"
              disabled={isSubmitting}
              onClick={onClose}
            />
          </div>

          <form onSubmit={handleSubmit}>
            <div className="modal-body p-4">
              <p className="text-muted small mb-3">
                Schedule a new one-off vaccination session interval for an authorized campaign and center.
                Initial session state will be created as <strong>DRAFT</strong>.
              </p>

              {submitError && <ApiErrorAlert error={submitError} />}
              {assignmentError && <ApiErrorAlert error={assignmentError} />}

              {/* Campaign Selection */}
              <div className="mb-3">
                <label htmlFor="sessionCampaignSelect" className="form-label fw-semibold">
                  Vaccination Campaign <span className="text-danger">*</span>
                </label>
                <select
                  id="sessionCampaignSelect"
                  className="form-select"
                  value={campaignId}
                  onChange={(e) => setCampaignId(e.target.value)}
                  disabled={isSubmitting}
                  required
                >
                  <option value="">-- Select Campaign --</option>
                  {campaigns.map((c) => (
                    <option key={c.id} value={c.id}>
                      [{c.code}] {c.name} ({c.status})
                    </option>
                  ))}
                </select>
              </div>

              {/* Center Selection (Assignment-Aware) */}
              <div className="mb-3">
                <label htmlFor="sessionCenterSelect" className="form-label fw-semibold">
                  Vaccination Center <span className="text-danger">*</span>
                </label>
                {loadingAssignments ? (
                  <div className="p-2 border rounded bg-light">
                    <LoadingSpinner message="Loading authorized centers for selected campaign..." />
                  </div>
                ) : (
                  <select
                    id="sessionCenterSelect"
                    className="form-select"
                    value={centerId}
                    onChange={(e) => setCenterId(e.target.value)}
                    disabled={!campaignId || isSubmitting || assignments.length === 0}
                    required
                  >
                    <option value="">
                      {!campaignId
                        ? '-- Select a campaign first --'
                        : assignments.length === 0
                        ? '-- No active center assignments found for this campaign --'
                        : '-- Select Authorized Center --'}
                    </option>
                    {assignments.map((a) => {
                      const cMeta = centersMap[a.centerId];
                      const tz = cMeta?.timezone || 'UTC';
                      const validity = a.activeTo
                        ? `${a.activeFrom} to ${a.activeTo}`
                        : `From ${a.activeFrom}`;
                      return (
                        <option key={a.id} value={a.centerId}>
                          [{a.centerCode}] {a.centerName} ({tz}) — Assignment: {validity} ({a.status})
                        </option>
                      );
                    })}
                  </select>
                )}
                {centerId && selectedCenter && (
                  <div className="form-text mt-1">
                    <span className="badge bg-light text-dark border me-2">
                      Schedule Timezone: <code>{scheduleTimezone}</code>
                    </span>
                    <span className="text-muted small">
                      All entered local times will be translated according to this center timezone.
                    </span>
                  </div>
                )}
              </div>

              {/* Session Date & Time */}
              <div className="row g-3 mb-3">
                <div className="col-md-4">
                  <label htmlFor="sessionDateInput" className="form-label fw-semibold">
                    Session Date <span className="text-danger">*</span>
                  </label>
                  <input
                    type="date"
                    id="sessionDateInput"
                    className="form-control"
                    value={sessionDate}
                    onChange={(e) => {
                      setSessionDate(e.target.value);
                      if (bookingCloseMode === 'SAME_AS_START') {
                        setBookingCloseDate(e.target.value);
                      }
                    }}
                    disabled={isSubmitting}
                    required
                  />
                </div>

                <div className="col-md-4">
                  <label htmlFor="sessionStartTimeInput" className="form-label fw-semibold">
                    Start Time ({scheduleTimezone}) <span className="text-danger">*</span>
                  </label>
                  <input
                    type="time"
                    id="sessionStartTimeInput"
                    className="form-control"
                    value={startTime}
                    onChange={(e) => {
                      setStartTime(e.target.value);
                      if (bookingCloseMode === 'SAME_AS_START') {
                        setBookingCloseTime(e.target.value);
                      }
                    }}
                    disabled={isSubmitting}
                    required
                  />
                </div>

                <div className="col-md-4">
                  <label htmlFor="sessionEndTimeInput" className="form-label fw-semibold">
                    End Time ({scheduleTimezone}) <span className="text-danger">*</span>
                  </label>
                  <input
                    type="time"
                    id="sessionEndTimeInput"
                    className="form-control"
                    value={endTime}
                    onChange={(e) => setEndTime(e.target.value)}
                    disabled={isSubmitting}
                    required
                  />
                </div>
              </div>

              {/* Booking Close Policy */}
              <div className="card bg-light border p-3 mb-3">
                <label className="form-label fw-semibold mb-2">
                  Booking Intake Close Policy <span className="text-danger">*</span>
                </label>
                <div className="d-flex gap-3 mb-2">
                  <div className="form-check">
                    <input
                      className="form-check-input"
                      type="radio"
                      name="bookingCloseModeRadio"
                      id="bookingCloseSameAsStart"
                      checked={bookingCloseMode === 'SAME_AS_START'}
                      onChange={() => {
                        setBookingCloseMode('SAME_AS_START');
                        setBookingCloseDate(sessionDate);
                        setBookingCloseTime(startTime);
                      }}
                      disabled={isSubmitting}
                    />
                    <label className="form-check-label" htmlFor="bookingCloseSameAsStart">
                      Closes at Session Start (Default)
                    </label>
                  </div>
                  <div className="form-check">
                    <input
                      className="form-check-input"
                      type="radio"
                      name="bookingCloseModeRadio"
                      id="bookingCloseCustom"
                      checked={bookingCloseMode === 'CUSTOM'}
                      onChange={() => {
                        setBookingCloseMode('CUSTOM');
                        if (!bookingCloseDate) setBookingCloseDate(sessionDate);
                        if (!bookingCloseTime) setBookingCloseTime(startTime);
                      }}
                      disabled={isSubmitting}
                    />
                    <label className="form-check-label" htmlFor="bookingCloseCustom">
                      Custom Cutoff Time (Earlier)
                    </label>
                  </div>
                </div>

                {bookingCloseMode === 'CUSTOM' && (
                  <div className="row g-2 mt-1">
                    <div className="col-md-6">
                      <label htmlFor="customBookingCloseDate" className="form-label small text-muted">
                        Booking Close Date
                      </label>
                      <input
                        type="date"
                        id="customBookingCloseDate"
                        className="form-control form-control-sm"
                        value={bookingCloseDate}
                        onChange={(e) => setBookingCloseDate(e.target.value)}
                        disabled={isSubmitting}
                        required
                      />
                    </div>
                    <div className="col-md-6">
                      <label htmlFor="customBookingCloseTime" className="form-label small text-muted">
                        Booking Close Time ({scheduleTimezone})
                      </label>
                      <input
                        type="time"
                        id="customBookingCloseTime"
                        className="form-control form-control-sm"
                        value={bookingCloseTime}
                        onChange={(e) => setBookingCloseTime(e.target.value)}
                        disabled={isSubmitting}
                        required
                      />
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="modal-footer bg-light">
              <button
                type="button"
                className="btn btn-outline-secondary"
                disabled={isSubmitting}
                onClick={onClose}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={isSubmitting || loadingAssignments || !campaignId || !centerId}
              >
                {isSubmitting ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                    Creating Draft Session...
                  </>
                ) : (
                  'Create Draft Session'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
