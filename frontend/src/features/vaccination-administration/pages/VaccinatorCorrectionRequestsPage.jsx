import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  listVaccinatorCorrectionRequests,
  applyCorrectionRequest,
  getRecordDetails,
} from '../api/certificateApi';
import { formatDisplayDateTime } from '../utils/dateUtils';

export default function VaccinatorCorrectionRequestsPage() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState('ALL');

  // Modals state
  const [viewingRequest, setViewingRequest] = useState(null);
  const [applyRequest, setApplyRequest] = useState(null);
  const [targetRecordDetails, setTargetRecordDetails] = useState(null);
  const [loadingRecordDetails, setLoadingRecordDetails] = useState(false);

  // Apply form fields
  const [selectedField, setSelectedField] = useState('doseNumber');
  const [newValue, setNewValue] = useState('');
  const [submittingApply, setSubmittingApply] = useState(false);
  const [applyError, setApplyError] = useState('');
  const [applySuccess, setApplySuccess] = useState('');

  const fetchRequests = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await listVaccinatorCorrectionRequests();
      setRequests(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Failed to load administered correction requests.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRequests();
  }, []);

  const openApplyModal = async (req) => {
    setApplyRequest(req);
    setSelectedField('doseNumber');
    setNewValue('');
    setApplyError('');
    setApplySuccess('');
    setTargetRecordDetails(null);
    setLoadingRecordDetails(true);

    try {
      const record = await getRecordDetails(req.vaccinationRecordId);
      setTargetRecordDetails(record);
      // Pre-fill initial value based on selected field
      setNewValue(String(record.doseNumber || '1'));
    } catch (err) {
      setApplyError(err.message || 'Could not fetch current record details.');
    } finally {
      setLoadingRecordDetails(false);
    }
  };

  const handleFieldChange = (field) => {
    setSelectedField(field);
    setApplyError('');
    if (!targetRecordDetails) return;

    switch (field) {
      case 'doseNumber':
        setNewValue(String(targetRecordDetails.doseNumber || '1'));
        break;
      case 'administrationTime':
        setNewValue(targetRecordDetails.administrationTime ? targetRecordDetails.administrationTime.slice(0, 16) : '');
        break;
      case 'nextDoseDate':
        setNewValue(targetRecordDetails.nextDoseDate ? targetRecordDetails.nextDoseDate.slice(0, 10) : '');
        break;
      case 'authorizedNotes':
        setNewValue(targetRecordDetails.authorizedNotes || '');
        break;
      default:
        setNewValue('');
    }
  };

  const closeApplyModal = () => {
    setApplyRequest(null);
    setTargetRecordDetails(null);
    setApplyError('');
    setApplySuccess('');
  };

  const handleConfirmApply = async (e) => {
    e.preventDefault();
    if (!newValue || !newValue.trim()) {
      setApplyError('Please enter a corrected replacement value.');
      return;
    }

    setSubmittingApply(true);
    setApplyError('');
    try {
      let formattedValue = newValue.trim();
      if (selectedField === 'administrationTime' && formattedValue && !formattedValue.endsWith('Z')) {
        // Ensure ISO-8601 UTC representation if date-local was used
        try {
          formattedValue = new Date(formattedValue).toISOString();
        } catch {
          // keep original
        }
      }

      await applyCorrectionRequest(
        applyRequest.vaccinationRecordId,
        applyRequest.id,
        {
          fieldName: selectedField,
          newValue: formattedValue,
        },
      );

      setApplySuccess('Correction successfully applied. The previous certificate has been superseded and an updated valid certificate has been issued.');
      await fetchRequests();
      setTimeout(() => {
        closeApplyModal();
      }, 2000);
    } catch (err) {
      setApplyError(err.message || 'Failed to apply correction. Please verify the value.');
    } finally {
      setSubmittingApply(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'PENDING':
        return <span className="badge bg-warning-subtle text-warning border border-warning fw-semibold">Awaiting Admin Approval</span>;
      case 'APPROVED':
        return <span className="badge bg-primary text-white fw-semibold">Action Required</span>;
      case 'REJECTED':
        return <span className="badge bg-danger-subtle text-danger border border-danger fw-semibold">Rejected</span>;
      case 'APPLIED':
        return <span className="badge bg-success-subtle text-success border border-success fw-semibold">Completed</span>;
      default:
        return <span className="badge bg-secondary-subtle text-secondary border border-secondary fw-semibold">{status}</span>;
    }
  };

  const counts = {
    APPROVED: requests.filter((r) => r.status === 'APPROVED').length,
    PENDING: requests.filter((r) => r.status === 'PENDING').length,
    HISTORY: requests.filter((r) => r.status === 'APPLIED' || r.status === 'REJECTED').length,
    ALL: requests.length,
  };

  // Sort requests: APPROVED first (urgent task), then PENDING (waiting), then others
  const sortedRequests = [...requests].sort((a, b) => {
    const priority = { APPROVED: 0, PENDING: 1, APPLIED: 2, REJECTED: 3 };
    const pA = priority[a.status] ?? 4;
    const pB = priority[b.status] ?? 4;
    if (pA !== pB) return pA - pB;
    return new Date(b.correctedAt || 0) - new Date(a.correctedAt || 0);
  });

  const filteredRequests = sortedRequests.filter((r) => {
    if (filter === 'ALL') return true;
    if (filter === 'HISTORY') return r.status === 'APPLIED' || r.status === 'REJECTED';
    return r.status === filter;
  });

  return (
    <div className="container py-4">
      {/* Header */}
      <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
        <div>
          <nav aria-label="breadcrumb">
            <ol className="breadcrumb mb-1 small">
              <li className="breadcrumb-item"><Link to="/app/staff">Vaccinator Workspace</Link></li>
              <li className="breadcrumb-item active" aria-current="page">Correction Requests</li>
            </ol>
          </nav>
          <h1 className="h3 fw-bold text-dark mb-0">Administered Correction Requests</h1>
          <p className="text-secondary small mb-0">
            Review citizen correction requests associated with vaccinations you administered, and apply administrative-approved clinical corrections.
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

      {/* Clinical Integrity Notice */}
      <div className="alert alert-info py-2 px-3 small mb-4 border d-flex align-items-center">
        <i className="bi bi-info-circle-fill text-info fs-5 me-2"></i>
        <div>
          <strong>Clinical Integrity:</strong> Vaccinators may only apply corrections after Administrative approval. Applying a correction creates an append-only audit record, marks the existing certificate as <code>SUPERSEDED</code>, and issues an updated <code>VALID</code> digital certificate.
        </div>
      </div>

      {/* Summary Metric Cards */}
      <div className="row g-3 mb-4">
        <div className="col-12 col-md-4">
          <div
            className={`card border-0 shadow-sm h-100 ${filter === 'APPROVED' ? 'border-bottom border-primary border-3' : ''}`}
            onClick={() => setFilter('APPROVED')}
            style={{ cursor: 'pointer' }}
          >
            <div className="card-body p-3">
              <div className="d-flex justify-content-between align-items-center">
                <span className="small text-secondary fw-semibold">Needs Action</span>
                <span className="badge bg-primary text-white">Approved</span>
              </div>
              <div className="h3 fw-bold mb-0 text-primary mt-2">{counts.APPROVED}</div>
              <div className="small text-muted mt-1">Admin approved · Ready for clinical update</div>
            </div>
          </div>
        </div>

        <div className="col-12 col-md-4">
          <div
            className={`card border-0 shadow-sm h-100 ${filter === 'PENDING' ? 'border-bottom border-warning border-3' : ''}`}
            onClick={() => setFilter('PENDING')}
            style={{ cursor: 'pointer' }}
          >
            <div className="card-body p-3">
              <div className="d-flex justify-content-between align-items-center">
                <span className="small text-secondary fw-semibold">Awaiting Admin</span>
                <span className="badge bg-warning-subtle text-warning border border-warning">Pending Review</span>
              </div>
              <div className="h3 fw-bold mb-0 text-dark mt-2">{counts.PENDING}</div>
              <div className="small text-muted mt-1">Under administrative review</div>
            </div>
          </div>
        </div>

        <div className="col-12 col-md-4">
          <div
            className={`card border-0 shadow-sm h-100 ${filter === 'HISTORY' ? 'border-bottom border-secondary border-3' : ''}`}
            onClick={() => setFilter('HISTORY')}
            style={{ cursor: 'pointer' }}
          >
            <div className="card-body p-3">
              <div className="d-flex justify-content-between align-items-center">
                <span className="small text-secondary fw-semibold">Completed History</span>
                <span className="badge bg-secondary-subtle text-secondary border border-secondary">Archive</span>
              </div>
              <div className="h3 fw-bold mb-0 text-dark mt-2">{counts.HISTORY}</div>
              <div className="small text-muted mt-1">Applied corrections &amp; rejected requests</div>
            </div>
          </div>
        </div>
      </div>

      {/* Filter Buttons */}
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2 className="h5 fw-bold text-dark mb-0">
          {filter === 'APPROVED' ? 'Requests Needing Clinical Action' : filter === 'PENDING' ? 'Requests Awaiting Admin Review' : filter === 'HISTORY' ? 'Correction History' : 'All Administered Requests'}
        </h2>
        <div className="d-flex gap-2 flex-wrap">
          {[
            { key: 'ALL', label: `All (${counts.ALL})` },
            { key: 'APPROVED', label: `Needs Action (${counts.APPROVED})` },
            { key: 'PENDING', label: `Awaiting Admin (${counts.PENDING})` },
            { key: 'HISTORY', label: `History (${counts.HISTORY})` },
          ].map(({ key, label }) => (
            <button
              key={key}
              type="button"
              className={`btn btn-sm ${filter === key ? 'btn-primary' : 'btn-outline-secondary'}`}
              onClick={() => setFilter(key)}
            >
              {label}
            </button>
          ))}
        </div>
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
              ? 'There are currently no correction requests for vaccinations you administered.'
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
                  <th>Certificate</th>
                  <th>Citizen Request Message</th>
                  <th>Admin Review Status</th>
                  <th>Requested Date</th>
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
                    <td>{getStatusBadge(req.status)}</td>
                    <td className="text-secondary">
                      {formatDisplayDateTime(req.correctedAt)}
                    </td>
                    <td className="text-end pe-4">
                      <div className="d-flex justify-content-end gap-2 flex-wrap">
                        <button
                          type="button"
                          className="btn btn-outline-secondary btn-sm"
                          onClick={() => setViewingRequest(req)}
                          title="View Request Details"
                        >
                          <i className="bi bi-eye me-1"></i>View
                        </button>
                        {req.status === 'APPROVED' && (
                          <button
                            type="button"
                            className="btn btn-primary btn-sm fw-semibold"
                            onClick={() => openApplyModal(req)}
                            title="Apply Clinical Correction"
                          >
                            <i className="bi bi-check2-circle me-1"></i>Apply Correction
                          </button>
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

      {/* View Only Modal */}
      {viewingRequest && (
        <div className="modal fade show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }}>
          <div className="modal-dialog modal-dialog-centered modal-lg">
            <div className="modal-content shadow border-0 overflow-hidden">
              <div className="modal-header bg-light border-bottom">
                <h5 className="modal-title fw-bold text-dark">
                  <i className="bi bi-file-text-fill text-primary me-2"></i>
                  Correction Request #{viewingRequest.id}
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setViewingRequest(null)}
                  aria-label="Close"
                ></button>
              </div>
              <div className="modal-body p-4 small">
                <div className="card bg-light border-0 p-3 mb-3">
                  <div className="row g-2">
                    <div className="col-12 col-md-6">
                      <span className="text-secondary">Citizen:</span>{' '}
                      <strong>{viewingRequest.citizenName || `Citizen #${viewingRequest.citizenId}`}</strong>
                    </div>
                    <div className="col-12 col-md-6">
                      <span className="text-secondary">Vaccination Record:</span>{' '}
                      <strong className="font-monospace">#{viewingRequest.vaccinationRecordId}</strong>
                    </div>
                    <div className="col-12 col-md-6">
                      <span className="text-secondary">Certificate:</span>{' '}
                      <span className="font-monospace">{viewingRequest.certificateReference || 'None'}</span>
                    </div>
                    <div className="col-12 col-md-6">
                      <span className="text-secondary">Status:</span>{' '}
                      <span>{getStatusBadge(viewingRequest.status)}</span>
                    </div>
                    <div className="col-12 col-md-6">
                      <span className="text-secondary">Requested Date:</span>{' '}
                      <span>{formatDisplayDateTime(viewingRequest.correctedAt)}</span>
                    </div>
                    {viewingRequest.reviewedAt && (
                      <div className="col-12 col-md-6">
                        <span className="text-secondary">Admin Review Date:</span>{' '}
                        <span>{formatDisplayDateTime(viewingRequest.reviewedAt)}</span>
                      </div>
                    )}
                  </div>
                </div>

                <div className="mb-3">
                  <label className="form-label fw-semibold text-dark mb-1">Citizen Request Message</label>
                  <div className="p-3 bg-white border rounded font-monospace text-break">
                    {viewingRequest.reason}
                  </div>
                </div>

                {viewingRequest.reviewReason && (
                  <div className="mb-3">
                    <label className="form-label fw-semibold text-secondary mb-1">Admin Review Notes</label>
                    <div className="p-2 bg-light border rounded text-secondary">
                      {viewingRequest.reviewReason}
                    </div>
                  </div>
                )}

                {viewingRequest.status === 'APPLIED' && (
                  <div className="alert alert-success d-flex align-items-center mb-0">
                    <i className="bi bi-check-circle-fill me-2 fs-5"></i>
                    <div>
                      <strong>Correction Completed:</strong> Corrected field: <code>{viewingRequest.fieldName}</code>. Old value: <code>{viewingRequest.oldValue || 'none'}</code>, New value: <code>{viewingRequest.newValue}</code>.
                    </div>
                  </div>
                )}
              </div>
              <div className="modal-footer bg-light border-top">
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm"
                  onClick={() => setViewingRequest(null)}
                >
                  Close
                </button>
                {viewingRequest.status === 'APPROVED' && (
                  <button
                    type="button"
                    className="btn btn-primary btn-sm fw-semibold"
                    onClick={() => {
                      const req = viewingRequest;
                      setViewingRequest(null);
                      openApplyModal(req);
                    }}
                  >
                    <i className="bi bi-check2-circle me-1"></i>Apply Correction
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Apply Correction Modal (Vaccinator only, Approved requests only) */}
      {applyRequest && (
        <div className="modal fade show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }}>
          <div className="modal-dialog modal-dialog-centered modal-lg">
            <div className="modal-content shadow border-0 overflow-hidden">
              <div className="modal-header bg-light border-bottom">
                <h5 className="modal-title fw-bold text-dark">
                  <i className="bi bi-patch-check-fill text-primary me-2"></i>
                  Apply Approved Correction #{applyRequest.id}
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={closeApplyModal}
                  aria-label="Close"
                  disabled={submittingApply}
                ></button>
              </div>

              <form onSubmit={handleConfirmApply}>
                <div className="modal-body p-4">
                  {applySuccess && (
                    <div className="alert alert-success d-flex align-items-center mb-3" role="alert">
                      <i className="bi bi-check-circle-fill me-2 fs-5"></i>
                      <div>{applySuccess}</div>
                    </div>
                  )}

                  {applyError && (
                    <div className="alert alert-danger d-flex align-items-center mb-3" role="alert">
                      <i className="bi bi-exclamation-triangle-fill me-2 fs-5"></i>
                      <div>{applyError}</div>
                    </div>
                  )}

                  {/* Citizen Message */}
                  <div className="mb-3">
                    <label className="form-label fw-semibold text-dark small mb-1">
                      Citizen Request Explanation
                    </label>
                    <div className="p-3 bg-light border rounded small font-monospace text-break">
                      {applyRequest.reason}
                    </div>
                  </div>

                  {/* Current Record Data */}
                  <div className="card bg-white border p-3 mb-3 small">
                    <h6 className="fw-bold text-secondary mb-2">
                      <i className="bi bi-lock-fill me-1"></i>Current Immutable Record Data
                    </h6>
                    {loadingRecordDetails ? (
                      <div className="text-secondary small">
                        <span className="spinner-border spinner-border-sm me-1" role="status"></span>
                        Loading record snapshot...
                      </div>
                    ) : targetRecordDetails ? (
                      <div className="row g-2">
                        <div className="col-6 col-md-4">
                          <span className="text-muted">Dose Number:</span>{' '}
                          <strong>{targetRecordDetails.doseNumber}</strong>
                        </div>
                        <div className="col-6 col-md-4">
                          <span className="text-muted">Vaccine Batch:</span>{' '}
                          <span className="font-monospace">#{targetRecordDetails.vaccineBatchId}</span>
                        </div>
                        <div className="col-6 col-md-4">
                          <span className="text-muted">Vaccine Name:</span>{' '}
                          <span>{targetRecordDetails.vaccineName || 'N/A'}</span>
                        </div>
                        <div className="col-12 col-md-6">
                          <span className="text-muted">Administered Time:</span>{' '}
                          <span>{formatDisplayDateTime(targetRecordDetails.administrationTime)}</span>
                        </div>
                        <div className="col-12 col-md-6">
                          <span className="text-muted">Next Dose Date:</span>{' '}
                          <span>{targetRecordDetails.nextDoseDate || 'None'}</span>
                        </div>
                        <div className="col-12">
                          <span className="text-muted">Authorized Notes:</span>{' '}
                          <span className="fst-italic">{targetRecordDetails.authorizedNotes || 'None'}</span>
                        </div>
                      </div>
                    ) : null}
                  </div>

                  {/* Inventory Protection Notice */}
                  <div className="alert alert-info py-2 px-3 small mb-3">
                    <i className="bi bi-info-circle me-1"></i>
                    <strong>Inventory Protection:</strong> Vaccine batch corrections cannot be applied through this workflow because they affect inventory records. Contact the appropriate inventory/administrative process if batch reconciliation is required.
                  </div>

                  {/* Field Selector */}
                  <div className="row g-3 mb-3">
                    <div className="col-12 col-md-6">
                      <label htmlFor="selectCorrectionField" className="form-label fw-semibold text-dark small">
                        Select Field to Correct <span className="text-danger">*</span>
                      </label>
                      <select
                        id="selectCorrectionField"
                        className="form-select form-select-sm"
                        value={selectedField}
                        onChange={(e) => handleFieldChange(e.target.value)}
                        disabled={submittingApply}
                      >
                        <option value="doseNumber">Dose Number</option>
                        <option value="administrationTime">Administration Date &amp; Time</option>
                        <option value="nextDoseDate">Next Dose Date</option>
                        <option value="authorizedNotes">Authorized Clinical Notes</option>
                      </select>
                    </div>

                    <div className="col-12 col-md-6">
                      <label htmlFor="correctionNewValue" className="form-label fw-semibold text-dark small">
                        Corrected Replacement Value <span className="text-danger">*</span>
                      </label>
                      {selectedField === 'doseNumber' ? (
                        <input
                          id="correctionNewValue"
                          type="number"
                          min="1"
                          max="10"
                          className="form-control form-control-sm"
                          value={newValue}
                          onChange={(e) => setNewValue(e.target.value)}
                          required
                          disabled={submittingApply}
                        />
                      ) : selectedField === 'administrationTime' ? (
                        <input
                          id="correctionNewValue"
                          type="datetime-local"
                          className="form-control form-control-sm"
                          value={newValue}
                          onChange={(e) => setNewValue(e.target.value)}
                          required
                          disabled={submittingApply}
                        />
                      ) : selectedField === 'nextDoseDate' ? (
                        <input
                          id="correctionNewValue"
                          type="date"
                          className="form-control form-control-sm"
                          value={newValue}
                          onChange={(e) => setNewValue(e.target.value)}
                          required
                          disabled={submittingApply}
                        />
                      ) : (
                        <textarea
                          id="correctionNewValue"
                          rows={2}
                          className="form-control form-control-sm"
                          placeholder="Enter revised clinical notes..."
                          value={newValue}
                          onChange={(e) => setNewValue(e.target.value)}
                          maxLength={1000}
                          required
                          disabled={submittingApply}
                        />
                      )}
                    </div>
                  </div>

                  <div className="alert alert-warning py-2 px-3 small mb-0">
                    <i className="bi bi-shield-exclamation me-1"></i>
                    <strong>Profile Isolation:</strong> Citizen name/profile correction requires a separate identity/profile correction workflow. Only supported clinical fields can be corrected here.
                  </div>
                </div>

                <div className="modal-footer bg-light border-top">
                  <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm"
                    onClick={closeApplyModal}
                    disabled={submittingApply}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary btn-sm fw-semibold"
                    disabled={submittingApply || !newValue.trim()}
                  >
                    {submittingApply ? (
                      <>
                        <span className="spinner-border spinner-border-sm me-1" role="status"></span>
                        Applying &amp; Reissuing...
                      </>
                    ) : (
                      'Apply Correction & Reissue Certificate'
                    )}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
