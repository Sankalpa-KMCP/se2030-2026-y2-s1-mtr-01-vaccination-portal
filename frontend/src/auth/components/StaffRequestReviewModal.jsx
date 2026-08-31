import React, { useEffect, useRef, useState } from 'react';
import {
  getRoleLabel,
  REQUESTABLE_STAFF_ROLES,
  REQUESTABLE_STAFF_ROLE_CODES,
} from '../constants/authRoles';

function formatDate(value) {
  if (!value) return 'Not available';
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

export default function StaffRequestReviewModal({ mode, request, isProcessing, onApprove, onClose, onReject }) {
  const defaultRole = REQUESTABLE_STAFF_ROLE_CODES.includes(request?.requestedRoleCode)
    ? request.requestedRoleCode
    : REQUESTABLE_STAFF_ROLES[0].code;
  const [approvedRole, setApprovedRole] = useState(defaultRole);
  const [reason, setReason] = useState('');
  const [validationError, setValidationError] = useState('');
  const closeButtonRef = useRef(null);

  useEffect(() => {
    setApprovedRole(defaultRole);
    setReason('');
    setValidationError('');
    closeButtonRef.current?.focus();
  }, [defaultRole, mode, request?.id]);

  if (!request) return null;

  const title = mode === 'approve'
    ? 'Approve Staff Account Request'
    : mode === 'reject'
      ? 'Reject Staff Account Request'
      : 'Staff Account Request Details';

  const submit = (event) => {
    event.preventDefault();
    if (mode === 'approve') {
      onApprove(approvedRole);
      return;
    }
    if (mode === 'reject') {
      const trimmedReason = reason.trim();
      if (!trimmedReason) {
        setValidationError('A rejection reason is required.');
        return;
      }
      onReject(trimmedReason);
    }
  };

  return (
    <div
      className="modal d-block"
      role="dialog"
      aria-modal="true"
      aria-labelledby="staff-request-modal-title"
      style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }}
      onKeyDown={(event) => {
        if (event.key === 'Escape' && !isProcessing) onClose();
      }}
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !isProcessing) onClose();
      }}
    >
      <div className="modal-dialog modal-dialog-centered modal-lg">
        <div className="modal-content">
          <div className="modal-header">
            <h2 className="modal-title fs-5" id="staff-request-modal-title">{title}</h2>
            <button
              ref={closeButtonRef}
              type="button"
              className="btn-close"
              aria-label="Close staff request dialog"
              onClick={onClose}
              disabled={isProcessing}
            />
          </div>
          <form onSubmit={submit}>
            <div className="modal-body">
              <dl className="row mb-4">
                <dt className="col-sm-4">Full Name</dt><dd className="col-sm-8">{request.fullName}</dd>
                <dt className="col-sm-4">Email</dt><dd className="col-sm-8">{request.email}</dd>
                <dt className="col-sm-4">Phone</dt><dd className="col-sm-8">{request.phoneNumber}</dd>
                <dt className="col-sm-4">Requested Role</dt><dd className="col-sm-8">{getRoleLabel(request.requestedRoleCode)}</dd>
                <dt className="col-sm-4">Submitted</dt><dd className="col-sm-8">{formatDate(request.createdAt)}</dd>
                <dt className="col-sm-4">Status</dt><dd className="col-sm-8 mb-0">{request.accountStatus}</dd>
              </dl>

              {mode === 'approve' && (
                <div>
                  <label className="form-label fw-semibold" htmlFor="approved-role">Approved Role</label>
                  <select
                    className="form-select"
                    id="approved-role"
                    value={approvedRole}
                    onChange={(event) => setApprovedRole(event.target.value)}
                    disabled={isProcessing}
                  >
                    {REQUESTABLE_STAFF_ROLES.map(({ code, label }) => (
                      <option key={code} value={code}>{label}</option>
                    ))}
                  </select>
                  <div className="form-text">This authoritative role may differ from the role originally requested.</div>
                </div>
              )}

              {mode === 'reject' && (
                <div>
                  {validationError && <div className="alert alert-danger py-2" role="alert">{validationError}</div>}
                  <label className="form-label fw-semibold" htmlFor="rejection-reason">Rejection Reason</label>
                  <textarea
                    className="form-control"
                    id="rejection-reason"
                    rows={4}
                    maxLength={500}
                    value={reason}
                    onChange={(event) => {
                      setReason(event.target.value);
                      setValidationError('');
                    }}
                    disabled={isProcessing}
                    required
                  />
                  <div className="form-text text-end">{reason.length}/500 characters</div>
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-outline-secondary" onClick={onClose} disabled={isProcessing}>Close</button>
              {mode === 'approve' && (
                <button type="submit" className="btn btn-success" disabled={isProcessing}>
                  {isProcessing ? 'Approving...' : 'Approve Request'}
                </button>
              )}
              {mode === 'reject' && (
                <button type="submit" className="btn btn-danger" disabled={isProcessing}>
                  {isProcessing ? 'Rejecting...' : 'Reject Request'}
                </button>
              )}
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
