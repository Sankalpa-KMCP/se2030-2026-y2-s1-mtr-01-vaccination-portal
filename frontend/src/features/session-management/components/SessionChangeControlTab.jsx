import { formatInTimezone } from "../api/sessionApi";
import ApiErrorAlert from "../../../components/common/ApiErrorAlert";
import LoadingSpinner from "../../../components/common/LoadingSpinner";
import useSessionChangeControl from "../hooks/useSessionChangeControl";



export default function SessionChangeControlTab({
  session,
  campaignsMap = {},
  centersMap = {},
  onSessionUpdated,
}) {
  const {
    activeAction,
    history,
    loadingHistory,
    historyError,
    setHistoryError,
    rescheduleDate,
    setRescheduleDate,
    rescheduleStartTime,
    setRescheduleStartTime,
    rescheduleEndTime,
    setRescheduleEndTime,
    rescheduleBookingCloseMode,
    setRescheduleBookingCloseMode,
    rescheduleBookingCloseDate,
    setRescheduleBookingCloseDate,
    rescheduleBookingCloseTime,
    setRescheduleBookingCloseTime,
    transferCenterId,
    transferDate,
    setTransferDate,
    transferStartTime,
    setTransferStartTime,
    transferEndTime,
    setTransferEndTime,
    transferBookingCloseMode,
    setTransferBookingCloseMode,
    transferBookingCloseDate,
    setTransferBookingCloseDate,
    transferBookingCloseTime,
    setTransferBookingCloseTime,
    campaignCenters,
    loadingCampaignCenters,
    campaignCentersError,
    previewResult,
    setPreviewResult,
    loadingPreview,
    previewError,
    setPreviewError,
    previewSnapshotRef,
    reason,
    setReason,
    submittingConfirm,
    confirmError,
    setConfirmError,
    successMessage,
    setSuccessMessage,
    actionMutexOwnerRef,
    confirmHeadingRef,
    tz,
    isScheduled,
    isMutationAllowed,
    fetchHistory,
    handleSelectAction,
    handleTransferCenterChange,
    handleFieldChange,
    handleExecutePreview,
    handleConfirmChange,
    campaign
  } = useSessionChangeControl({ session, campaignsMap, centersMap, onSessionUpdated });

const getChangeTypeBadge = (changeType) => {
    switch (changeType) {
      case 'CANCELLED':
        return <span className="badge bg-danger text-white">CANCELLED</span>;
      case 'RESCHEDULED':
        return <span className="badge bg-primary text-white">RESCHEDULED</span>;
      case 'CENTER_TRANSFERRED':
        return <span className="badge bg-info text-dark">TRANSFERRED</span>;
      default:
        return <span className="badge bg-secondary text-white">{changeType}</span>;
    }
  };

return (
    <div className="d-flex flex-column gap-4">
      {/* Global Success Notification */}
      {successMessage && (
        <div
          className="alert alert-success alert-dismissible fade show border-0 shadow-sm mb-0"
          role="status"
        >
          <strong>Success:</strong> {successMessage}
          <button
            type="button"
            className="btn-close"
            aria-label="Close"
            onClick={() => setSuccessMessage(null)}
          />
        </div>
      )}

      {/* Mutation Availability Check & Action Selector */}
      {isMutationAllowed ? (
        <div className="card border p-3 bg-light">
          <div className="d-flex flex-wrap align-items-center justify-content-between gap-3">
            <div>
              <div className="d-flex align-items-center gap-2 mb-1">
                <h6 className="fw-bold text-dark mb-0">Session Change Operations</h6>
                {campaign && (
                  <span className="badge bg-light text-dark border small">
                    {campaign.code ? `[${campaign.code}] ` : ''}{campaign.name}
                  </span>
                )}
              </div>
              <p className="text-secondary small mb-0">
                Preview impact and apply change control mutations for this{' '}
                <span className="badge bg-secondary">{session.lifecycleStatus}</span> session.
              </p>
            </div>

            <div className="btn-group" role="group" aria-label="Change control actions">
              <button
                type="button"
                className={`btn btn-sm ${
                  activeAction === 'CANCEL' ? 'btn-danger text-white' : 'btn-outline-danger'
                }`}
                onClick={() => handleSelectAction(activeAction === 'CANCEL' ? 'NONE' : 'CANCEL')}
                disabled={actionMutexOwnerRef.current !== null}
              >
                Cancel Session
              </button>
              <button
                type="button"
                className={`btn btn-sm ${
                  activeAction === 'RESCHEDULE' ? 'btn-primary' : 'btn-outline-primary'
                }`}
                onClick={() =>
                  handleSelectAction(activeAction === 'RESCHEDULE' ? 'NONE' : 'RESCHEDULE')
                }
                disabled={actionMutexOwnerRef.current !== null}
              >
                Reschedule
              </button>
              <button
                type="button"
                className={`btn btn-sm ${
                  activeAction === 'TRANSFER' ? 'btn-dark' : 'btn-outline-dark'
                }`}
                onClick={() =>
                  handleSelectAction(activeAction === 'TRANSFER' ? 'NONE' : 'TRANSFER')
                }
                disabled={actionMutexOwnerRef.current !== null}
              >
                Transfer Center
              </button>
            </div>
          </div>

          {/* Active Action Form Card */}
          {activeAction !== 'NONE' && (
            <div
              className="mt-3 p-3 rounded border bg-white shadow-sm"
              role="region"
              aria-labelledby="activeActionHeading"
            >
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h6 id="activeActionHeading" className="fw-bold text-dark mb-0">
                  {activeAction === 'CANCEL' && 'Session Cancellation Workflow'}
                  {activeAction === 'RESCHEDULE' && 'Session Reschedule Workflow'}
                  {activeAction === 'TRANSFER' && 'Session Center Transfer Workflow'}
                </h6>
                <button
                  type="button"
                  className="btn btn-sm btn-outline-secondary"
                  onClick={() => handleSelectAction('NONE')}
                  disabled={actionMutexOwnerRef.current !== null}
                >
                  Close Action
                </button>
              </div>

              {/* Cancellation Form */}
              {activeAction === 'CANCEL' && (
                <div className="mb-3">
                  <div className="alert alert-warning py-2 small mb-3">
                    <strong>Warning:</strong> Cancelling this session will transition it to the
                    terminal <span className="badge bg-danger">CANCELLED</span> status.
                    {isScheduled && (
                      <span className="d-block mt-1">
                        Authoritative appointment and waitlist impact will be assessed via the
                        impact reader.
                      </span>
                    )}
                  </div>

                  {!previewResult && (
                    <div className="d-flex justify-content-start">
                      <button
                        type="button"
                        className="btn btn-sm btn-danger fw-semibold"
                        onClick={handleExecutePreview}
                        disabled={loadingPreview || actionMutexOwnerRef.current !== null}
                      >
                        {loadingPreview ? (
                          <>
                            <span
                              className="spinner-border spinner-border-sm me-2"
                              role="status"
                              aria-hidden="true"
                            />
                            Calculating Cancellation Impact...
                          </>
                        ) : (
                          'Preview Cancellation Impact'
                        )}
                      </button>
                    </div>
                  )}
                </div>
              )}

              {/* Reschedule Form */}
              {activeAction === 'RESCHEDULE' && (
                <div className="mb-3">
                  <p className="text-secondary small mb-3">
                    Proposed schedule will be evaluated in the session's timezone:{' '}
                    <code>{tz}</code>.
                  </p>

                  <div className="row g-3">
                    <div className="col-md-4">
                      <label htmlFor="rescheduleDate" className="form-label small fw-semibold">
                        New Session Date <span className="text-danger">*</span>
                      </label>
                      <input
                        type="date"
                        id="rescheduleDate"
                        className="form-control form-control-sm"
                        value={rescheduleDate}
                        onChange={handleFieldChange(setRescheduleDate)}
                        disabled={loadingPreview || submittingConfirm}
                        required
                      />
                    </div>

                    <div className="col-md-4">
                      <label htmlFor="rescheduleStartTime" className="form-label small fw-semibold">
                        New Start Time ({tz}) <span className="text-danger">*</span>
                      </label>
                      <input
                        type="time"
                        id="rescheduleStartTime"
                        className="form-control form-control-sm"
                        value={rescheduleStartTime}
                        onChange={handleFieldChange(setRescheduleStartTime)}
                        disabled={loadingPreview || submittingConfirm}
                        required
                      />
                    </div>

                    <div className="col-md-4">
                      <label htmlFor="rescheduleEndTime" className="form-label small fw-semibold">
                        New End Time ({tz}) <span className="text-danger">*</span>
                      </label>
                      <input
                        type="time"
                        id="rescheduleEndTime"
                        className="form-control form-control-sm"
                        value={rescheduleEndTime}
                        onChange={handleFieldChange(setRescheduleEndTime)}
                        disabled={loadingPreview || submittingConfirm}
                        required
                      />
                    </div>

                    {/* Booking Intake Close Mode */}
                    <div className="col-md-12">
                      <label className="form-label small fw-semibold d-block mb-1">
                        Booking Intake Close Time
                      </label>
                      <div className="d-flex align-items-center gap-3 mb-2">
                        <div className="form-check">
                          <input
                            className="form-check-input"
                            type="radio"
                            name="rescheduleBookingCloseMode"
                            id="rescheduleBookingCloseSame"
                            value="SAME_AS_START"
                            checked={rescheduleBookingCloseMode === 'SAME_AS_START'}
                            onChange={handleFieldChange(setRescheduleBookingCloseMode)}
                            disabled={loadingPreview || submittingConfirm}
                          />
                          <label
                            className="form-check-label small"
                            htmlFor="rescheduleBookingCloseSame"
                          >
                            Same as Session Start
                          </label>
                        </div>
                        <div className="form-check">
                          <input
                            className="form-check-input"
                            type="radio"
                            name="rescheduleBookingCloseMode"
                            id="rescheduleBookingCloseCustom"
                            value="CUSTOM"
                            checked={rescheduleBookingCloseMode === 'CUSTOM'}
                            onChange={handleFieldChange(setRescheduleBookingCloseMode)}
                            disabled={loadingPreview || submittingConfirm}
                          />
                          <label
                            className="form-check-label small"
                            htmlFor="rescheduleBookingCloseCustom"
                          >
                            Custom Close Time
                          </label>
                        </div>
                      </div>

                      {rescheduleBookingCloseMode === 'CUSTOM' && (
                        <div className="row g-2 p-2 bg-light rounded border">
                          <div className="col-md-6">
                            <label
                              htmlFor="rescheduleBookingCloseDate"
                              className="form-label small text-muted"
                            >
                              Booking Close Date
                            </label>
                            <input
                              type="date"
                              id="rescheduleBookingCloseDate"
                              className="form-control form-control-sm"
                              value={rescheduleBookingCloseDate}
                              onChange={handleFieldChange(setRescheduleBookingCloseDate)}
                              disabled={loadingPreview || submittingConfirm}
                            />
                          </div>
                          <div className="col-md-6">
                            <label
                              htmlFor="rescheduleBookingCloseTime"
                              className="form-label small text-muted"
                            >
                              Booking Close Time ({tz})
                            </label>
                            <input
                              type="time"
                              id="rescheduleBookingCloseTime"
                              className="form-control form-control-sm"
                              value={rescheduleBookingCloseTime}
                              onChange={handleFieldChange(setRescheduleBookingCloseTime)}
                              disabled={loadingPreview || submittingConfirm}
                            />
                          </div>
                        </div>
                      )}
                    </div>
                  </div>

                  {!previewResult && (
                    <div className="d-flex justify-content-start mt-3">
                      <button
                        type="button"
                        className="btn btn-sm btn-primary fw-semibold"
                        onClick={handleExecutePreview}
                        disabled={loadingPreview || actionMutexOwnerRef.current !== null}
                      >
                        {loadingPreview ? (
                          <>
                            <span
                              className="spinner-border spinner-border-sm me-2"
                              role="status"
                              aria-hidden="true"
                            />
                            Calculating Reschedule Preview...
                          </>
                        ) : (
                          'Preview Reschedule Impact'
                        )}
                      </button>
                    </div>
                  )}
                </div>
              )}

              {/* Transfer Form */}
              {activeAction === 'TRANSFER' && (
                <div className="mb-3">
                  <p className="text-secondary small mb-3">
                    Select a target vaccination center authorized for this campaign. The schedule
                    will be evaluated against the target center&apos;s operating hours and timezone.
                  </p>

                  {campaignCentersError && (
                    <div className="mb-3">
                      <ApiErrorAlert error={campaignCentersError} />
                    </div>
                  )}

                  {!loadingCampaignCenters &&
                    !campaignCentersError &&
                    campaignCenters.length === 0 && (
                      <div className="alert alert-info py-2 small mb-3" role="status">
                        No other authorized vaccination centers are assigned to this campaign for transfer.
                      </div>
                    )}

                  <div className="row g-3">
                    <div className="col-md-12">
                      <label htmlFor="transferCenterId" className="form-label small fw-semibold">
                        Target Vaccination Center <span className="text-danger">*</span>
                      </label>
                      <select
                        id="transferCenterId"
                        className="form-select form-select-sm"
                        value={transferCenterId}
                        onChange={handleTransferCenterChange}
                        disabled={
                          loadingCampaignCenters ||
                          !!campaignCentersError ||
                          campaignCenters.length === 0 ||
                          loadingPreview ||
                          submittingConfirm
                        }
                        required
                      >
                        <option value="">-- Select Target Vaccination Center --</option>
                        {campaignCenters.map((ca) => {
                          const cId = ca.centerId || ca.id;
                          const cObj = centersMap[cId];
                          const cName = cObj?.name || ca.centerName || `Center #${cId}`;
                          const cCode = cObj?.code || ca.centerCode || '';
                          const cTz = cObj?.timezone?.trim();
                          return (
                            <option key={cId} value={cId} disabled={!cTz}>
                              {cCode ? `[${cCode}] ` : ''}
                              {cName} {cTz ? `(${cTz})` : '(Timezone unavailable)'}
                            </option>
                          );
                        })}
                      </select>
                      {loadingCampaignCenters && (
                        <div className="text-muted small mt-1">Loading authorized centers...</div>
                      )}
                    </div>

                    {transferCenterId && (
                      <>
                        {(() => {
                          const selectedCenter = centersMap[transferCenterId];
                          const selectedTz = selectedCenter?.timezone?.trim();
                          if (!selectedTz) {
                            return (
                              <div className="col-md-12">
                                <div className="alert alert-warning py-2 small mb-0" role="alert">
                                  Selected center&apos;s timezone metadata is unavailable. Transfer
                                  cannot proceed until center timezone is loaded.
                                </div>
                              </div>
                            );
                          }
                          return (
                            <>
                              <div className="col-md-4">
                                <label htmlFor="transferDate" className="form-label small fw-semibold">
                                  Session Date <span className="text-danger">*</span>
                                </label>
                                <input
                                  type="date"
                                  id="transferDate"
                                  className="form-control form-control-sm"
                                  value={transferDate}
                                  onChange={handleFieldChange(setTransferDate)}
                                  disabled={loadingPreview || submittingConfirm}
                                  required
                                />
                              </div>

                              <div className="col-md-4">
                                <label
                                  htmlFor="transferStartTime"
                                  className="form-label small fw-semibold"
                                >
                                  Start Time ({selectedTz}){' '}
                                  <span className="text-danger">*</span>
                                </label>
                                <input
                                  type="time"
                                  id="transferStartTime"
                                  className="form-control form-control-sm"
                                  value={transferStartTime}
                                  onChange={handleFieldChange(setTransferStartTime)}
                                  disabled={loadingPreview || submittingConfirm}
                                  required
                                />
                              </div>

                              <div className="col-md-4">
                                <label htmlFor="transferEndTime" className="form-label small fw-semibold">
                                  End Time ({selectedTz}){' '}
                                  <span className="text-danger">*</span>
                                </label>
                                <input
                                  type="time"
                                  id="transferEndTime"
                                  className="form-control form-control-sm"
                                  value={transferEndTime}
                                  onChange={handleFieldChange(setTransferEndTime)}
                                  disabled={loadingPreview || submittingConfirm}
                                  required
                                />
                              </div>

                              {/* Booking Intake Close Mode */}
                              <div className="col-md-12">
                                <label className="form-label small fw-semibold d-block mb-1">
                                  Booking Intake Close Time
                                </label>
                                <div className="d-flex align-items-center gap-3 mb-2">
                                  <div className="form-check">
                                    <input
                                      className="form-check-input"
                                      type="radio"
                                      name="transferBookingCloseMode"
                                      id="transferBookingCloseSame"
                                      value="SAME_AS_START"
                                      checked={transferBookingCloseMode === 'SAME_AS_START'}
                                      onChange={handleFieldChange(setTransferBookingCloseMode)}
                                      disabled={loadingPreview || submittingConfirm}
                                    />
                                    <label
                                      className="form-check-label small"
                                      htmlFor="transferBookingCloseSame"
                                    >
                                      Same as Session Start
                                    </label>
                                  </div>
                                  <div className="form-check">
                                    <input
                                      className="form-check-input"
                                      type="radio"
                                      name="transferBookingCloseMode"
                                      id="transferBookingCloseCustom"
                                      value="CUSTOM"
                                      checked={transferBookingCloseMode === 'CUSTOM'}
                                      onChange={handleFieldChange(setTransferBookingCloseMode)}
                                      disabled={loadingPreview || submittingConfirm}
                                    />
                                    <label
                                      className="form-check-label small"
                                      htmlFor="transferBookingCloseCustom"
                                    >
                                      Custom Close Time
                                    </label>
                                  </div>
                                </div>

                                {transferBookingCloseMode === 'CUSTOM' && (
                                  <div className="row g-2 p-2 bg-light rounded border">
                                    <div className="col-md-6">
                                      <label
                                        htmlFor="transferBookingCloseDate"
                                        className="form-label small text-muted"
                                      >
                                        Booking Close Date
                                      </label>
                                      <input
                                        type="date"
                                        id="transferBookingCloseDate"
                                        className="form-control form-control-sm"
                                        value={transferBookingCloseDate}
                                        onChange={handleFieldChange(setTransferBookingCloseDate)}
                                        disabled={loadingPreview || submittingConfirm}
                                      />
                                    </div>
                                    <div className="col-md-6">
                                      <label
                                        htmlFor="transferBookingCloseTime"
                                        className="form-label small text-muted"
                                      >
                                        Booking Close Time ({selectedTz})
                                      </label>
                                      <input
                                        type="time"
                                        id="transferBookingCloseTime"
                                        className="form-control form-control-sm"
                                        value={transferBookingCloseTime}
                                        onChange={handleFieldChange(setTransferBookingCloseTime)}
                                        disabled={loadingPreview || submittingConfirm}
                                      />
                                    </div>
                                  </div>
                                )}
                              </div>
                            </>
                          );
                        })()}
                      </>
                    )}
                  </div>

                  {!previewResult &&
                    transferCenterId &&
                    !!centersMap[transferCenterId]?.timezone?.trim() && (
                      <div className="d-flex justify-content-start mt-3">
                        <button
                          type="button"
                          className="btn btn-sm btn-dark fw-semibold"
                          onClick={handleExecutePreview}
                          disabled={loadingPreview || actionMutexOwnerRef.current !== null}
                        >
                          {loadingPreview ? (
                            <>
                              <span
                                className="spinner-border spinner-border-sm me-2"
                                role="status"
                                aria-hidden="true"
                              />
                              Calculating Transfer Preview...
                            </>
                          ) : (
                            'Preview Center Transfer Impact'
                          )}
                        </button>
                      </div>
                    )}
                </div>
              )}

              {/* Preview Error Banner */}
              {previewError && (
                <div className="mt-3">
                  <ApiErrorAlert error={previewError} onDismiss={() => setPreviewError(null)} />
                </div>
              )}

              {/* Authoritative Preview Display & Confirmation Section */}
              {previewResult && (
                <div
                  className="mt-3 p-3 rounded border bg-light"
                  role="region"
                  aria-live="polite"
                  aria-labelledby="previewImpactHeading"
                >
                  <h6
                    id="previewImpactHeading"
                    ref={confirmHeadingRef}
                    tabIndex="-1"
                    className="fw-bold text-dark mb-3"
                  >
                    Authoritative Impact Preview
                  </h6>

                  {/* Impact Summary Badges */}
                  <div className="row g-3 mb-3">
                    <div className="col-md-6">
                      <div className="p-3 bg-white rounded border d-flex align-items-center justify-content-between">
                        <div>
                          <span className="text-muted small d-block">Affected Appointments</span>
                          <strong className="fs-5 text-dark">
                            {previewResult.affectedAppointmentCount}
                          </strong>
                        </div>
                        <span className="badge bg-primary rounded-pill px-3 py-2">
                          {previewResult.affectedAppointmentCount === 0 ? 'No Impact' : 'Requires Rebooking'}
                        </span>
                      </div>
                    </div>

                    <div className="col-md-6">
                      <div className="p-3 bg-white rounded border d-flex align-items-center justify-content-between">
                        <div>
                          <span className="text-muted small d-block">Affected Waitlist Entries</span>
                          <strong className="fs-5 text-dark">
                            {previewResult.affectedWaitlistCount}
                          </strong>
                        </div>
                        <span className="badge bg-secondary rounded-pill px-3 py-2">
                          {previewResult.affectedWaitlistCount === 0 ? 'No Impact' : 'Notification Pending'}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Schedule / Snapshot Comparison */}
                  {previewResult.oldSnapshot && previewResult.newSnapshot && (
                    <div className="row g-3 mb-3">
                      <div className="col-md-6">
                        <div className="card border-0 bg-white p-3 h-100">
                          <span className="text-muted small fw-bold d-block mb-2">
                            Current Session State
                          </span>
                          <div className="small">
                            <div className="mb-1">
                              <strong>Center:</strong>{' '}
                              {centersMap[previewResult.oldSnapshot.centerId]?.name ||
                                `Center #${previewResult.oldSnapshot.centerId}`}
                            </div>
                            <div className="mb-1">
                              <strong>Start:</strong>{' '}
                              {formatInTimezone(
                                previewResult.oldSnapshot.startAt,
                                previewResult.oldSnapshot.scheduleTimezone
                              )}
                            </div>
                            <div className="mb-1">
                              <strong>End:</strong>{' '}
                              {formatInTimezone(
                                previewResult.oldSnapshot.endAt,
                                previewResult.oldSnapshot.scheduleTimezone
                              )}
                            </div>
                            <div className="mb-1">
                              <strong>Booking Close:</strong>{' '}
                              {formatInTimezone(
                                previewResult.oldSnapshot.bookingCloseAt,
                                previewResult.oldSnapshot.scheduleTimezone
                              )}
                            </div>
                            <div>
                              <strong>Status:</strong>{' '}
                              <span className="badge bg-secondary">
                                {previewResult.oldSnapshot.lifecycleStatus}
                              </span>
                            </div>
                          </div>
                        </div>
                      </div>

                      <div className="col-md-6">
                        <div className="card border-0 bg-white p-3 h-100 border-start border-primary border-3">
                          <span className="text-primary small fw-bold d-block mb-2">
                            Proposed Session State
                          </span>
                          <div className="small">
                            <div className="mb-1">
                              <strong>Center:</strong>{' '}
                              {centersMap[previewResult.newSnapshot.centerId]?.name ||
                                `Center #${previewResult.newSnapshot.centerId}`}
                            </div>
                            <div className="mb-1">
                              <strong>Start:</strong>{' '}
                              {formatInTimezone(
                                previewResult.newSnapshot.startAt,
                                previewResult.newSnapshot.scheduleTimezone
                              )}
                            </div>
                            <div className="mb-1">
                              <strong>End:</strong>{' '}
                              {formatInTimezone(
                                previewResult.newSnapshot.endAt,
                                previewResult.newSnapshot.scheduleTimezone
                              )}
                            </div>
                            <div className="mb-1">
                              <strong>Booking Close:</strong>{' '}
                              {formatInTimezone(
                                previewResult.newSnapshot.bookingCloseAt,
                                previewResult.newSnapshot.scheduleTimezone
                              )}
                            </div>
                            <div>
                              <strong>Status:</strong>{' '}
                              <span className="badge bg-primary">
                                {previewResult.newSnapshot.lifecycleStatus}
                              </span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  )}

                  {/* Cryptographic Preview Token Display */}
                  <div className="mb-3">
                    <span className="text-muted small d-block">
                      Authoritative Verification Token
                    </span>
                    <code
                      className="text-break small bg-white p-1 rounded border d-block font-monospace"
                      style={{ fontSize: '0.75rem' }}
                    >
                      {previewResult.previewToken}
                    </code>
                  </div>

                  {/* Reason Input */}
                  <div className="mb-3">
                    <label htmlFor="changeReasonInput" className="form-label small fw-semibold">
                      Reason for Session Change <span className="text-danger">*</span>
                    </label>
                    <textarea
                      id="changeReasonInput"
                      className="form-control form-control-sm"
                      rows="3"
                      placeholder="Enter a mandatory audit reason explaining this session change (max 500 characters)..."
                      value={reason}
                      onChange={(e) => {
                        setReason(e.target.value);
                        setConfirmError(null);
                      }}
                      maxLength={500}
                      disabled={submittingConfirm}
                      required
                    />
                    <div className="d-flex justify-content-between text-muted small mt-1">
                      <span>Mandatory audit trail reason</span>
                      <span>{reason.length} / 500</span>
                    </div>
                  </div>

                  {/* Confirmation Error Banner */}
                  {confirmError && (
                    <div className="mb-3">
                      <ApiErrorAlert error={confirmError} onDismiss={() => setConfirmError(null)} />
                    </div>
                  )}

                  {/* Confirmation Action Buttons */}
                  <div className="d-flex justify-content-end gap-2">
                    <button
                      type="button"
                      className="btn btn-sm btn-secondary"
                      onClick={() => {
                        setPreviewResult(null);
                        previewSnapshotRef.current = null;
                        setConfirmError(null);
                      }}
                      disabled={submittingConfirm}
                    >
                      Back to Edit
                    </button>
                    <button
                      type="button"
                      className={`btn btn-sm fw-semibold ${
                        activeAction === 'CANCEL'
                          ? 'btn-danger text-white'
                          : activeAction === 'RESCHEDULE'
                          ? 'btn-primary'
                          : 'btn-dark'
                      }`}
                      onClick={handleConfirmChange}
                      disabled={submittingConfirm || actionMutexOwnerRef.current !== null}
                    >
                      {submittingConfirm ? (
                        <>
                          <span
                            className="spinner-border spinner-border-sm me-2"
                            role="status"
                            aria-hidden="true"
                          />
                          Applying Change...
                        </>
                      ) : (
                        `Confirm ${
                          activeAction === 'CANCEL'
                            ? 'Cancellation'
                            : activeAction === 'RESCHEDULE'
                            ? 'Reschedule'
                            : 'Transfer'
                        }`
                      )}
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      ) : (
        <div className="alert alert-info py-2 small mb-0">
          <strong>Notice:</strong> This session is in{' '}
          <span className="badge bg-secondary">{session.lifecycleStatus}</span> status. Change
          operations (Cancel, Reschedule, Transfer) are not permitted on{' '}
          {session.lifecycleStatus} sessions. Full audit event history is available below.
        </div>
      )}

      {/* Change Events Audit History Card */}
      <div className="card border p-3">
        <div className="d-flex justify-content-between align-items-center mb-3">
          <div>
            <h6 className="fw-bold text-dark mb-1">Session Change Audit History</h6>
            <p className="text-secondary small mb-0">
              Immutable, append-only log of change control events for Session #{session.id}.
            </p>
          </div>
          <button
            type="button"
            className="btn btn-sm btn-outline-secondary"
            onClick={fetchHistory}
            disabled={loadingHistory}
          >
            {loadingHistory ? 'Refreshing...' : 'Refresh History'}
          </button>
        </div>

        {historyError && (
          <ApiErrorAlert error={historyError} onDismiss={() => setHistoryError(null)} />
        )}

        {loadingHistory ? (
          <div className="py-4 text-center">
            <LoadingSpinner message="Loading session change history..." />
          </div>
        ) : history.length === 0 ? (
          <div className="text-center py-4 text-muted small bg-light rounded border border-dashed">
            No change events recorded for this session.
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table table-sm table-hover align-middle mb-0">
              <thead className="table-light small">
                <tr>
                  <th scope="col" style={{ width: '60px' }}>#</th>
                  <th scope="col">Timestamp</th>
                  <th scope="col">Change Type</th>
                  <th scope="col">Actor</th>
                  <th scope="col">Reason</th>
                  <th scope="col" className="text-center">Appointments</th>
                  <th scope="col" className="text-center">Waitlist</th>
                </tr>
              </thead>
              <tbody className="small">
                {history.map((event) => (
                  <tr key={event.id}>
                    <td className="font-monospace text-muted">{event.id}</td>
                    <td>
                      <div>{formatInTimezone(event.createdAt, tz)}</div>
                      <div
                        className="text-muted font-monospace"
                        style={{ fontSize: '0.75rem' }}
                      >
                        UTC: {event.createdAt}
                      </div>
                    </td>
                    <td>{getChangeTypeBadge(event.changeType)}</td>
                    <td>
                      <span className="badge bg-light text-dark border font-monospace">
                        {event.actorId ? `Actor #${event.actorId}` : '—'}
                      </span>
                    </td>
                    <td className="text-break" style={{ maxWidth: '280px' }}>
                      {event.reason}
                    </td>
                    <td className="text-center">
                      <span
                        className={`badge ${
                          event.affectedAppointmentCount > 0 ? 'bg-warning text-dark' : 'bg-light text-muted border'
                        }`}
                      >
                        {event.affectedAppointmentCount}
                      </span>
                    </td>
                    <td className="text-center">
                      <span
                        className={`badge ${
                          event.affectedWaitlistCount > 0 ? 'bg-secondary text-white' : 'bg-light text-muted border'
                        }`}
                      >
                        {event.affectedWaitlistCount}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
