import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  listAdminCorrectionRequests,
  approveCorrectionRequest,
  rejectCorrectionRequest,
} from '../api/certificateApi';
import { formatDisplayDateTime } from '../utils/dateUtils';

export default function AdminCorrectionRequestsPage() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState('ALL');

  // Action modal state
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [actionType, setActionType] = useState(null); // 'VIEW' | 'APPROVE' | 'REJECT'
  const [actionReason, setActionReason] = useState('');
  const [submittingAction, setSubmittingAction] = useState(false);
  const [actionError, setActionError] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');

  const fetchRequests = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await listAdminCorrectionRequests();
      setRequests(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Failed to load correction requests.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRequests();
  }, []);

  const openActionModal = (req, type) => {
    setSelectedRequest(req);
    setActionType(type);
    setActionReason('');
    setActionError('');
    setActionSuccess('');
  };

  const closeActionModal = () => {
    setSelectedRequest(null);
    setActionType(null);
    setActionReason('');
    setActionError('');
    setActionSuccess('');
  };

  const handleConfirmAction = async (e) => {
    e.preventDefault();
    if (actionType === 'REJECT' && !actionReason.trim()) {
      setActionError('Please provide a reason for rejecting this correction request.');
      return;
    }

    setSubmittingAction(true);
    setActionError('');
    try {
      if (actionType === 'APPROVE') {
        await approveCorrectionRequest(
          selectedRequest.vaccinationRecordId,
          selectedRequest.id,
          actionReason.trim(),
        );
        setActionSuccess('Correction request approved. The administering vaccinator may now apply clinical updates.');
      } else if (actionType === 'REJECT') {
        await rejectCorrectionRequest(
          selectedRequest.vaccinationRecordId,
          selectedRequest.id,
          actionReason.trim(),
        );
        setActionSuccess('Correction request has been rejected.');
      }
      await fetchRequests();
      setTimeout(() => {
        closeActionModal();
      }, 1500);
    } catch (err) {
      setActionError(err.message || 'Action failed. Please try again.');
    } finally {
      setSubmittingAction(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'PENDING':
        return <span className="badge bg-warning-subtle text-warning border border-warning fw-semibold">Pending Admin Review</span>;
      case 'APPROVED':
        return <span className="badge bg-info-subtle text-info border border-info fw-semibold">Approved (Awaiting Vaccinator)</span>;
      case 'REJECTED':
        return <span className="badge bg-danger-subtle text-danger border border-danger fw-semibold">Rejected</span>;
      case 'APPLIED':
        return <span className="badge bg-success-subtle text-success border border-success fw-semibold">Completed</span>;
      default:
        return <span className="badge bg-secondary-subtle text-secondary border border-secondary fw-semibold">{status}</span>;
    }
  };

  const filteredRequests = requests.filter((r) => {
    if (filter === 'ALL') return true;
    return r.status === filter;
  });

  return (
    <div className="container py-4">
      {/* Header */}
      <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
        <div>
          <nav aria-label="breadcrumb">
            <ol className="breadcrumb mb-1 small">
              <li className="breadcrumb-item"><Link to="/app/admin">Dashboard</Link></li>
              <li className="breadcrumb-item active" aria-current="page">Correction Requests</li>
            </ol>
          </nav>
          <h1 className="h3 fw-bold text-dark mb-0">Citizen Correction Requests</h1>
          <p className="text-secondary small mb-0">
            Review and approve or reject correction requests submitted by citizens regarding their vaccination records.
          </p>
        </div>
        <div>
          <button
            type="button"
            className="btn btn-outline-secondary btn-sm"
            onClick={fetchRequests}
            disabled={loading}
          >
            <i className="bi bi-arrow-clockwise me-1"></i>Refresh
          </button>
        </div>
      </div>

      {/* Admin Scope Notice */}
      <div className="alert alert-secondary py-2 px-3 small mb-4 border d-flex align-items-center">
        <i className="bi bi-shield-lock-fill text-primary fs-5 me-2"></i>
        <div>
          <strong>Administrative Governance:</strong> Administrators approve or reject citizen requests based on clinic validation. Administrators do not apply clinical modifications to medical records or drafts.
        </div>
      </div>

      {/* Filters */}
      <div className="d-flex gap-2 mb-3">
        {['ALL', 'PENDING', 'APPROVED', 'REJECTED', 'APPLIED'].map((st) => (
          <button
            key={st}
            type="button"
            className={`btn btn-sm ${filter === st ? 'btn-primary' : 'btn-outline-secondary'}`}
            onClick={() => setFilter(st)}
          >
            {st === 'ALL' ? 'All Requests' : st === 'APPLIED' ? 'Completed' : st === 'PENDING' ? 'Pending Review' : st}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="text-secondary small mt-2">Loading correction requests...</p>
        </div>
      ) : error ? (
        <div className="card shadow-sm border-danger p-4 text-center">
          <i className="bi bi-x-circle text-danger fs-1 mb-2"></i>
          <h5 className="text-danger fw-bold">Unable to Load Requests</h5>
          <p className="text-secondary small">{error}</p>
          <div>
            <button className="btn btn-outline-danger btn-sm" onClick={fetchRequests}>
              <i className="bi bi-arrow-clockwise me-1"></i>Retry
            </button>
          </div>
        </div>
      ) : filteredRequests.length === 0 ? (
        <div className="card shadow-sm border-0 p-5 text-center bg-light">
          <i className="bi bi-inbox text-secondary" style={{ fontSize: '3rem' }}></i>
          <h5 className="fw-bold text-dark mt-3">No Correction Requests Found</h5>
          <p className="text-secondary small mb-0">
            {filter === 'ALL'
              ? 'There are currently no citizen correction requests in the system.'
              : `There are no requests matching filter '${filter}'.`}
          </p>
        </div>
      ) : (
        <div className="card shadow-sm border-0 overflow-hidden">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light small text-uppercase text-secondary">
                <tr>
                  <th className="ps-4">Request ID</th>
                  <th>Citizen</th>
                  <th>Record ID</th>
                  <th>Certificate Reference</th>
                  <th>Citizen Message</th>
                  <th>Requested Date</th>
                  <th>Status</th>
                  <th className="text-end pe-4">Actions</th>
                </tr>
              </thead>
              <tbody className="small">
                {filteredRequests.map((req) => (
                  <tr key={req.id}>
                    <td className="ps-4 font-monospace fw-semibold text-dark">
                      #{req.id}
                    </td>
                    <td>
                      <div className="fw-semibold text-dark">{req.citizenName || `Citizen #${req.citizenId}`}</div>
                      <div className="text-secondary small">ID: {req.citizenId}</div>
                    </td>
                    <td className="font-monospace text-secondary">
                      #{req.vaccinationRecordId}
                    </td>
                    <td className="font-monospace text-dark">
                      {req.certificateReference ? (
                        <span>{req.certificateReference}</span>
                      ) : (
                        <span className="text-muted fst-italic">None</span>
                      )}
                    </td>
                    <td style={{ maxWidth: '280px' }} className="text-truncate" title={req.reason}>
                      {req.reason}
                    </td>
                    <td className="text-secondary">
                      {formatDisplayDateTime(req.correctedAt)}
                    </td>
                    <td>{getStatusBadge(req.status)}</td>
                    <td className="text-end pe-4">
                      <div className="d-flex justify-content-end gap-2 flex-wrap">
                        <button
                          type="button"
                          className="btn btn-outline-secondary btn-sm"
                          onClick={() => openActionModal(req, 'VIEW')}
                          title="View Request Details"
                        >
                          <i className="bi bi-eye me-1"></i>View
                        </button>
                        {req.status === 'PENDING' && (
                          <>
                            <button
                              type="button"
                              className="btn btn-success btn-sm fw-semibold"
                              onClick={() => openActionModal(req, 'APPROVE')}
                              title="Approve Request"
                            >
                              <i className="bi bi-check-lg me-1"></i>Approve
                            </button>
                            <button
                              type="button"
                              className="btn btn-outline-danger btn-sm fw-semibold"
                              onClick={() => openActionModal(req, 'REJECT')}
                              title="Reject Request"
                            >
                              <i className="bi bi-x-lg me-1"></i>Reject
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Review Modal (View / Approve / Reject) */}
      {selectedRequest && actionType && (
        <div className="modal fade show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }}>
          <div className="modal-dialog modal-dialog-centered modal-lg">
            <div className="modal-content shadow border-0 overflow-hidden">
              <div className="modal-header bg-light border-bottom">
                <h5 className="modal-title fw-bold text-dark">
                  {actionType === 'APPROVE' && <><i className="bi bi-check-circle-fill text-success me-2"></i>Approve Correction Request #{selectedRequest.id}</>}
                  {actionType === 'REJECT' && <><i className="bi bi-x-circle-fill text-danger me-2"></i>Reject Correction Request #{selectedRequest.id}</>}
                  {actionType === 'VIEW' && <><i className="bi bi-file-text-fill text-primary me-2"></i>Correction Request Details #{selectedRequest.id}</>}
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={closeActionModal}
                  aria-label="Close"
                  disabled={submittingAction}
                ></button>
              </div>

              <form onSubmit={handleConfirmAction}>
                <div className="modal-body p-4">
                  {actionSuccess && (
                    <div className="alert alert-success d-flex align-items-center mb-3" role="alert">
                      <i className="bi bi-check-circle-fill me-2"></i>
                      <div>{actionSuccess}</div>
                    </div>
                  )}

                  {actionError && (
                    <div className="alert alert-danger d-flex align-items-center mb-3" role="alert">
                      <i className="bi bi-exclamation-triangle-fill me-2"></i>
                      <div>{actionError}</div>
                    </div>
                  )}

                  {/* Summary Card */}
                  <div className="card bg-light border-0 p-3 mb-3 small">
                    <div className="row g-2">
                      <div className="col-12 col-md-6">
                        <span className="text-secondary">Citizen:</span>{' '}
                        <strong>{selectedRequest.citizenName || `Citizen #${selectedRequest.citizenId}`}</strong> (ID: {selectedRequest.citizenId})
                      </div>
                      <div className="col-12 col-md-6">
                        <span className="text-secondary">Vaccination Record:</span>{' '}
                        <strong className="font-monospace">#{selectedRequest.vaccinationRecordId}</strong>
                      </div>
                      <div className="col-12 col-md-6">
                        <span className="text-secondary">Certificate Reference:</span>{' '}
                        <span className="font-monospace text-dark">{selectedRequest.certificateReference || 'None'}</span>
                      </div>
                      <div className="col-12 col-md-6">
                        <span className="text-secondary">Certificate Status:</span>{' '}
                        <span>{selectedRequest.certificateStatus || 'N/A'}</span>
                      </div>
                      <div className="col-12 col-md-6">
                        <span className="text-secondary">Administering Vaccinator:</span>{' '}
                        <span>{selectedRequest.vaccinatorName || `User #${selectedRequest.vaccinatorId}`}</span>
                      </div>
                      <div className="col-12 col-md-6">
                        <span className="text-secondary">Requested Timestamp:</span>{' '}
                        <span>{formatDisplayDateTime(selectedRequest.correctedAt)}</span>
                      </div>
                      <div className="col-12">
                        <span className="text-secondary">Current Status:</span>{' '}
                        <span>{getStatusBadge(selectedRequest.status)}</span>
                      </div>
                    </div>
                  </div>

                  {/* Citizen Explanation */}
                  <div className="mb-3">
                    <label className="form-label fw-semibold text-dark small mb-1">
                      Citizen's Explanation / Request Message
                    </label>
                    <div className="p-3 bg-white border rounded small text-dark font-monospace text-break">
                      {selectedRequest.reason}
                    </div>
                  </div>

                  {/* If already reviewed */}
                  {selectedRequest.reviewReason && (
                    <div className="mb-3">
                      <label className="form-label fw-semibold text-secondary small mb-1">
                        Previous Review Notes
                      </label>
                      <div className="p-2 bg-light border rounded small text-secondary">
                        {selectedRequest.reviewReason}
                      </div>
                    </div>
                  )}

                  {/* Reason input for Approve / Reject */}
                  {actionType === 'APPROVE' && (
                    <div className="mb-3">
                      <label htmlFor="actionReason" className="form-label fw-semibold text-dark small">
                        Approval Notes (Optional)
                      </label>
                      <textarea
                        id="actionReason"
                        rows={3}
                        className="form-control form-control-sm"
                        placeholder="Optional review notes for clinic audit..."
                        value={actionReason}
                        onChange={(e) => setActionReason(e.target.value)}
                        maxLength={1000}
                        disabled={submittingAction}
                      />
                      <div className="form-text small text-secondary">
                        Approving permits the administering vaccinator to apply clinical corrections. It does not modify records directly.
                      </div>
                    </div>
                  )}

                  {actionType === 'REJECT' && (
                    <div className="mb-3">
                      <label htmlFor="actionReason" className="form-label fw-semibold text-dark small">
                        Rejection Reason <span className="text-danger">*</span>
                      </label>
                      <textarea
                        id="actionReason"
                        rows={3}
                        className="form-control form-control-sm"
                        placeholder="State reason why this correction request is rejected..."
                        value={actionReason}
                        onChange={(e) => setActionReason(e.target.value)}
                        maxLength={1000}
                        required
                        disabled={submittingAction}
                      />
                    </div>
                  )}
                </div>

                <div className="modal-footer bg-light border-top">
                  <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm"
                    onClick={closeActionModal}
                    disabled={submittingAction}
                  >
                    Close
                  </button>

                  {actionType === 'APPROVE' && (
                    <button
                      type="submit"
                      className="btn btn-success btn-sm fw-semibold"
                      disabled={submittingAction}
                    >
                      {submittingAction ? (
                        <>
                          <span className="spinner-border spinner-border-sm me-1" role="status"></span>
                          Approving...
                        </>
                      ) : (
                        'Confirm Approval'
                      )}
                    </button>
                  )}

                  {actionType === 'REJECT' && (
                    <button
                      type="submit"
                      className="btn btn-danger btn-sm fw-semibold"
                      disabled={submittingAction || !actionReason.trim()}
                    >
                      {submittingAction ? (
                        <>
                          <span className="spinner-border spinner-border-sm me-1" role="status"></span>
                          Rejecting...
                        </>
                      ) : (
                        'Confirm Rejection'
                      )}
                    </button>
                  )}
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
