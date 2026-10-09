import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  listMyVaccinations,
  listMyCertificates,
  downloadCertificate,
  listMyCorrectionRequests,
  requestVaccinationCorrection,
} from '../api/certificateApi';

export default function CitizenVaccinationsPage() {
  const [records, setRecords] = useState([]);
  const [certificates, setCertificates] = useState([]);
  const [corrections, setCorrections] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [downloadingId, setDownloadingId] = useState(null);
  const [downloadError, setDownloadError] = useState('');
  const [selectedCert, setSelectedCert] = useState(null);
  const [copiedToken, setCopiedToken] = useState(false);

  // Report Certificate / Record Issue modal state
  const [issueTarget, setIssueTarget] = useState(null);
  const [issueCategory, setIssueCategory] = useState('Certificate Discrepancy');
  const [issueMessage, setIssueMessage] = useState('');
  const [submittingIssue, setSubmittingIssue] = useState(false);
  const [issueError, setIssueError] = useState('');
  const [issueSuccess, setIssueSuccess] = useState('');

  const fetchData = async () => {
    setLoading(true);
    setError('');
    try {
      const [vaxRecords, certs, myCorrections] = await Promise.all([
        listMyVaccinations(),
        listMyCertificates(),
        listMyCorrectionRequests().catch(() => []),
      ]);
      setRecords(Array.isArray(vaxRecords) ? vaxRecords : []);
      setCertificates(Array.isArray(certs) ? certs : []);
      setCorrections(Array.isArray(myCorrections) ? myCorrections : []);
    } catch (err) {
      setError(err.message || 'Failed to load vaccination history and certificates.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleDownload = async (certId, certRef) => {
    if (!certId) return;
    setDownloadingId(certId);
    setDownloadError('');
    try {
      await downloadCertificate(certId, certRef);
    } catch (err) {
      setDownloadError(err.message || 'Could not download certificate PDF. Please try again.');
    } finally {
      setDownloadingId(null);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'VALID':
        return <span className="badge bg-success-subtle text-success border border-success fw-semibold">VALID</span>;
      case 'REVOKED':
        return <span className="badge bg-danger-subtle text-danger border border-danger fw-semibold">REVOKED</span>;
      case 'SUPERSEDED':
        return <span className="badge bg-warning-subtle text-warning border border-warning fw-semibold">SUPERSEDED</span>;
      default:
        return <span className="badge bg-secondary-subtle text-secondary border border-secondary fw-semibold">PENDING</span>;
    }
  };

  const getCorrectionBadge = (status) => {
    switch (status) {
      case 'PENDING':
        return <span className="badge bg-warning-subtle text-warning border border-warning fw-semibold ms-1">Awaiting Admin Review</span>;
      case 'APPROVED':
        return <span className="badge bg-info-subtle text-info border border-info fw-semibold ms-1">Approved</span>;
      case 'REJECTED':
        return <span className="badge bg-danger-subtle text-danger border border-danger fw-semibold ms-1">Rejected</span>;
      case 'APPLIED':
        return <span className="badge bg-success-subtle text-success border border-success fw-semibold ms-1">Completed</span>;
      default:
        return null;
    }
  };

  const findActiveCorrection = (recordId) => {
    if (!recordId || !corrections.length) return null;
    return corrections.find((c) => c.vaccinationRecordId === recordId);
  };

  const openIssueModal = (target) => {
    setIssueTarget(target);
    setIssueCategory('Certificate Discrepancy');
    setIssueMessage('');
    setIssueError('');
    setIssueSuccess('');
  };

  const closeIssueModal = () => {
    setIssueTarget(null);
    setIssueCategory('Certificate Discrepancy');
    setIssueMessage('');
    setIssueError('');
    setIssueSuccess('');
  };

  const handleSubmitIssue = async (e) => {
    e.preventDefault();
    if (!issueMessage.trim()) {
      setIssueError('Please describe the problem you would like corrected.');
      return;
    }

    const recordId = issueTarget.vaccinationRecordId || issueTarget.id;
    if (!recordId) {
      setIssueError('Associated vaccination record ID is missing.');
      return;
    }

    setSubmittingIssue(true);
    setIssueError('');
    try {
      await requestVaccinationCorrection(recordId, issueMessage.trim());
      setIssueSuccess('Issue report submitted for review by clinic administrators.');
      await fetchData();
      setTimeout(() => {
        closeIssueModal();
      }, 1500);
    } catch (err) {
      setIssueError(err.message || 'Failed to submit issue report. Please try again.');
    } finally {
      setSubmittingIssue(false);
    }
  };

  // Build unified items from records and certificates
  const unifiedItems = (() => {
    if (records.length > 0) {
      return records.map((rec, index) => {
        const foundCert = certificates.find((c) => c.vaccinationRecordId === rec.id || (rec.certificateId && c.id === rec.certificateId))
          || (certificates.length === records.length ? certificates[index] : null);
        const cert = foundCert || ((rec.certificateId || rec.certificateReference) ? {
          id: rec.certificateId,
          certificateReference: rec.certificateReference,
          status: rec.certificateStatus || 'VALID',
          doseNumber: rec.doseNumber,
          vaccineBatchId: rec.vaccineBatchId,
          administrationTime: rec.administrationTime,
        } : null);

        const hasCert = Boolean(cert?.certificateReference || rec.certificateReference);
        const certStatus = cert?.status || rec.certificateStatus || (hasCert ? 'VALID' : 'RECORDED');

        return {
          id: rec.id,
          recordId: rec.id,
          vaccineName: rec.vaccineName || cert?.vaccineName || 'Authorized Vaccine',
          doseNumber: rec.doseNumber || cert?.doseNumber || 1,
          batchNumber: rec.batchNumber || cert?.batchNumber || (rec.vaccineBatchId ? `Batch #${rec.vaccineBatchId}` : (cert?.vaccineBatchId ? `Batch #${cert.vaccineBatchId}` : 'Authorized Batch')),
          vaccineBatchId: rec.vaccineBatchId || cert?.vaccineBatchId,
          centerName: rec.centerName || cert?.centerName || (rec.centerId ? `Center #${rec.centerId}` : 'Vaccination Center'),
          administrationTime: rec.administrationTime || cert?.administrationTime || cert?.issuedAt,
          nextDoseDate: rec.nextDoseDate,
          certificate: cert,
          certificateId: cert?.id || rec.certificateId || null,
          certificateReference: cert?.certificateReference || rec.certificateReference || null,
          certificateStatus: certStatus,
        };
      });
    }

    // Fallback if records endpoint is empty but certificates exist
    return certificates.map((cert) => ({
      id: cert.vaccinationRecordId || cert.id,
      recordId: cert.vaccinationRecordId || cert.id,
      vaccineName: cert.vaccineName || 'Authorized Vaccine',
      doseNumber: cert.doseNumber || 1,
      batchNumber: cert.batchNumber || (cert.vaccineBatchId ? `Batch #${cert.vaccineBatchId}` : 'Authorized Batch'),
      vaccineBatchId: cert.vaccineBatchId,
      centerName: cert.centerName || 'Vaccination Center',
      administrationTime: cert.administrationTime || cert.issuedAt,
      nextDoseDate: null,
      certificate: cert,
      certificateId: cert.id,
      certificateReference: cert.certificateReference,
      certificateStatus: cert.status || 'VALID',
    }));
  })();

  const hasAnyItems = unifiedItems.length > 0;

  return (
    <div className="container py-4">
      {/* Header section */}
      <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
        <div>
          <nav aria-label="breadcrumb">
            <ol className="breadcrumb mb-1 small">
              <li className="breadcrumb-item"><Link to="/app/citizen">Dashboard</Link></li>
              <li className="breadcrumb-item active" aria-current="page">My Vaccinations</li>
            </ol>
          </nav>
          <h1 className="h3 fw-bold text-dark mb-0">My Vaccinations &amp; Certificates</h1>
          <p className="text-secondary small mb-0">
            View your permanent vaccination history and download officially certified digital vaccination credentials.
          </p>
        </div>
        <div>
          <Link to="/app/appointments/book" className="btn btn-outline-primary btn-sm fw-semibold">
            <i className="bi bi-calendar-plus me-1"></i>Book New Appointment
          </Link>
        </div>
      </div>

      {downloadError && (
        <div className="alert alert-danger alert-dismissible fade show" role="alert">
          <i className="bi bi-exclamation-triangle-fill me-2"></i>{downloadError}
          <button type="button" className="btn-close" onClick={() => setDownloadError('')} aria-label="Close"></button>
        </div>
      )}

      {loading ? (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="text-secondary small mt-2">Loading your vaccination records...</p>
        </div>
      ) : error ? (
        <div className="card shadow-sm border-danger p-4 text-center">
          <i className="bi bi-x-circle text-danger fs-1 mb-2"></i>
          <h5 className="text-danger fw-bold">Unable to Load Records</h5>
          <p className="text-secondary small">{error}</p>
          <div>
            <button className="btn btn-outline-danger btn-sm" onClick={fetchData}>
              <i className="bi bi-arrow-clockwise me-1"></i>Retry
            </button>
          </div>
        </div>
      ) : !hasAnyItems ? (
        <div className="card shadow-sm border-0 p-5 text-center bg-light">
          <div className="mb-3 text-secondary">
            <i className="bi bi-shield-check" style={{ fontSize: '3rem' }}></i>
          </div>
          <h4 className="fw-bold text-dark">No Vaccination Records Yet</h4>
          <p className="text-secondary small max-width-md mx-auto mb-4">
            You do not have any recorded vaccinations yet. Once you complete a vaccination appointment at an authorized center, your permanent clinical record and verifiable digital certificate will appear here.
          </p>
          <div>
            <Link to="/app/appointments/book" className="btn btn-primary fw-semibold px-4">
              <i className="bi bi-calendar-plus me-1"></i>Book an Appointment
            </Link>
          </div>
        </div>
      ) : (
        <div className="row g-4">
          {/* Unified Primary Section: Vaccination Records & Digital Certificates */}
          <div className="col-12">
            <div className="card shadow-sm border-0 overflow-hidden">
              <div className="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                <h2 className="h6 mb-0 fw-bold text-dark">
                  <i className="bi bi-shield-check text-primary me-2"></i>Vaccination Records &amp; Certificates
                </h2>
                <span className="badge bg-primary-subtle text-primary fw-semibold">
                  {unifiedItems.length} {unifiedItems.length === 1 ? 'Record' : 'Records'}
                </span>
              </div>
              <div className="card-body p-0">
                <div className="table-responsive">
                  <table className="table table-hover align-middle mb-0">
                    <thead className="table-light small text-uppercase text-secondary">
                      <tr>
                        <th className="ps-4">Certificate Ref</th>
                        <th>Vaccine &amp; Dose</th>
                        <th>Batch Code</th>
                        <th>Center</th>
                        <th>Date Administered</th>
                        <th>Status</th>
                        <th className="text-end pe-4">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="small">
                      {unifiedItems.map((item) => {
                        const existingCorrection = findActiveCorrection(item.recordId);
                        const hasCert = Boolean(item.certificateId && item.certificateReference);
                        return (
                          <tr key={item.recordId}>
                            <td className="ps-4 font-monospace fw-semibold text-dark">
                              {item.certificateReference ? (
                                item.certificateReference
                              ) : (
                                <span className="text-muted fst-italic">Pending issuance</span>
                              )}
                            </td>
                            <td>
                              <div className="fw-bold text-dark">{item.vaccineName}</div>
                              <span className="badge bg-light text-dark border">
                                Dose {item.doseNumber}
                              </span>
                            </td>
                            <td className="font-monospace text-secondary">
                              {item.batchNumber}
                            </td>
                            <td className="text-secondary">
                              {item.centerName}
                            </td>
                            <td className="text-secondary">
                              <div>
                                {item.administrationTime
                                  ? new Date(item.administrationTime).toLocaleDateString(undefined, { dateStyle: 'medium' })
                                  : 'N/A'}
                              </div>
                              {item.nextDoseDate && (
                                <div className="text-xs text-muted">
                                  Next: {new Date(item.nextDoseDate).toLocaleDateString(undefined, { dateStyle: 'medium' })}
                                </div>
                              )}
                            </td>
                            <td>
                              <div className="d-flex align-items-center flex-wrap gap-1">
                                {getStatusBadge(item.certificateStatus)}
                                {existingCorrection && getCorrectionBadge(existingCorrection.status)}
                              </div>
                            </td>
                            <td className="text-end pe-4">
                              <div className="d-flex justify-content-end gap-2 flex-wrap">
                                {hasCert && (
                                  <button
                                    type="button"
                                    className="btn btn-outline-secondary btn-sm"
                                    onClick={() => setSelectedCert(item.certificate)}
                                    title="View Certificate Details"
                                  >
                                    <i className="bi bi-eye me-1"></i>View
                                  </button>
                                )}
                                {hasCert && item.certificateId && (
                                  <button
                                    type="button"
                                    className="btn btn-outline-primary btn-sm"
                                    disabled={downloadingId === item.certificateId}
                                    onClick={() => handleDownload(item.certificateId, item.certificateReference)}
                                    title="Download Official PDF Certificate"
                                  >
                                    {downloadingId === item.certificateId ? (
                                      <>
                                        <span className="spinner-border spinner-border-sm me-1" role="status"></span>
                                        Downloading...
                                      </>
                                    ) : (
                                      <>
                                        <i className="bi bi-file-earmark-pdf-fill me-1"></i>PDF
                                      </>
                                    )}
                                  </button>
                                )}
                                {item.certificateStatus === 'VALID' && (
                                  <button
                                    type="button"
                                    className="btn btn-outline-warning btn-sm text-dark"
                                    onClick={() => openIssueModal(item)}
                                    title="Report Certificate Issue"
                                  >
                                    <i className="bi bi-pencil-square me-1"></i>Request Correction
                                  </button>
                                )}
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Citizen Report Issue / Request Correction Modal */}
      {issueTarget && (
        <div className="modal fade show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content shadow border-0 overflow-hidden">
              <div className="modal-header bg-light border-bottom">
                <h5 className="modal-title fw-bold text-dark">
                  <i className="bi bi-pencil-square text-warning me-2"></i>
                  Request Correction / Report Issue
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={closeIssueModal}
                  aria-label="Close"
                  disabled={submittingIssue}
                ></button>
              </div>
              <form onSubmit={handleSubmitIssue}>
                <div className="modal-body p-4">
                  {issueSuccess && (
                    <div className="alert alert-success d-flex align-items-center mb-3" role="alert">
                      <i className="bi bi-check-circle-fill me-2"></i>
                      <div>{issueSuccess}</div>
                    </div>
                  )}

                  {issueError && (
                    <div className="alert alert-danger d-flex align-items-center mb-3" role="alert">
                      <i className="bi bi-exclamation-triangle-fill me-2"></i>
                      <div>{issueError}</div>
                    </div>
                  )}

                  {/* Summary of Record */}
                  <div className="card bg-light border-0 p-3 mb-3 small">
                    <div className="row g-2">
                      {issueTarget.certificateReference && (
                        <div className="col-12">
                          <span className="text-secondary">Certificate:</span>{' '}
                          <strong className="font-monospace text-dark">{issueTarget.certificateReference}</strong>
                        </div>
                      )}
                      <div className="col-6">
                        <span className="text-secondary">Dose:</span>{' '}
                        <strong>Dose {issueTarget.doseNumber || 1}</strong>
                      </div>
                      <div className="col-6">
                        <span className="text-secondary">Vaccine Batch:</span>{' '}
                        <span className="font-monospace">#{issueTarget.vaccineBatchId || 'N/A'}</span>
                      </div>
                      {(issueTarget.administrationTime || issueTarget.issuedAt) && (
                        <div className="col-12">
                          <span className="text-secondary">Vaccination Date:</span>{' '}
                          <span>{new Date(issueTarget.administrationTime || issueTarget.issuedAt).toLocaleDateString(undefined, { dateStyle: 'medium' })}</span>
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="alert alert-secondary py-2 px-3 small mb-3">
                    <i className="bi bi-shield-lock me-1"></i>
                    <strong>Scope Restriction:</strong> Citizens may only request corrections for certificate-facing display issues (such as spelling or identification display issues). Clinical vaccination records, dose numbers, and vaccine batches cannot be altered by citizens.
                  </div>

                  <div className="mb-3">
                    <label htmlFor="issueCategory" className="form-label fw-semibold text-dark small">
                      Issue Category
                    </label>
                    <select
                      id="issueCategory"
                      className="form-select form-select-sm"
                      value={issueCategory}
                      onChange={(e) => setIssueCategory(e.target.value)}
                      disabled={submittingIssue}
                    >
                      <option value="Certificate Display Discrepancy">Certificate Display Discrepancy</option>
                      <option value="Name / Personal Details Spelling Issue">Name / Personal Details Spelling Issue</option>
                      <option value="Citizen ID / Identification Display Issue">Citizen ID / Identification Display Issue</option>
                      <option value="Other Certificate Issue">Other Certificate Issue</option>
                    </select>
                  </div>

                  <div className="mb-3">
                    <label htmlFor="correctionMessage" className="form-label fw-semibold text-dark small">
                      Describe what you believe should be corrected <span className="text-danger">*</span>
                    </label>
                    <textarea
                      id="correctionMessage"
                      aria-label="Describe what you believe should be corrected"
                      rows={4}
                      className="form-control"
                      placeholder="Example: My name is spelled incorrectly on the certificate, or my national identification number has a typo."
                      value={issueMessage}
                      onChange={(e) => setIssueMessage(e.target.value)}
                      maxLength={1000}
                      required
                      disabled={submittingIssue}
                    />
                    <div className="form-text small text-secondary">
                      Max 1000 characters. Please provide clear details so administrators can verify your certificate issue.
                    </div>
                  </div>

                  <div className="alert alert-info py-2 px-3 small mb-0">
                    <i className="bi bi-info-circle me-1"></i>
                    <strong>Note:</strong> Citizen name/profile correction requires a separate identity/profile correction workflow. Clinical corrections are reviewed by Administrators and applied by the administering Vaccinator.
                  </div>
                </div>
                <div className="modal-footer bg-light border-top">
                  <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm"
                    onClick={closeIssueModal}
                    disabled={submittingIssue}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-warning btn-sm fw-semibold text-dark"
                    disabled={submittingIssue || !issueMessage.trim()}
                  >
                    {submittingIssue ? (
                      <>
                        <span className="spinner-border spinner-border-sm me-1" role="status"></span>
                        Submitting...
                      </>
                    ) : (
                      'Submit Request'
                    )}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Certificate Details Modal */}
      {selectedCert && (
        <div className="modal fade show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }}>
          <div className="modal-dialog modal-dialog-centered modal-lg">
            <div className="modal-content shadow border-0 overflow-hidden">
              <div className="modal-header bg-light border-bottom">
                <h5 className="modal-title fw-bold text-dark">
                  <i className="bi bi-shield-check text-primary me-2"></i>
                  Certificate: {selectedCert.certificateReference}
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setSelectedCert(null)}
                  aria-label="Close"
                ></button>
              </div>
              <div className="modal-body p-4">
                {/* Validity notification banner */}
                {selectedCert.status === 'VALID' && (
                  <div className="alert alert-success d-flex align-items-center mb-4" role="alert">
                    <i className="bi bi-check-circle-fill fs-4 me-3"></i>
                    <div>
                      <strong>Official Valid Credential</strong>
                      <div className="small">This certificate is active, cryptographically signed, and valid for health clearance.</div>
                    </div>
                  </div>
                )}
                {selectedCert.status === 'REVOKED' && (
                  <div className="alert alert-danger d-flex align-items-center mb-4" role="alert">
                    <i className="bi bi-x-octagon-fill fs-4 me-3"></i>
                    <div>
                      <strong>Certificate Revoked</strong>
                      <div className="small">
                        This certificate was revoked on {selectedCert.revokedAt ? new Date(selectedCert.revokedAt).toLocaleString() : 'N/A'}.
                        {selectedCert.revocationReason && ` Reason: ${selectedCert.revocationReason}`}
                      </div>
                    </div>
                  </div>
                )}
                {selectedCert.status === 'SUPERSEDED' && (
                  <div className="alert alert-warning d-flex align-items-center mb-4" role="alert">
                    <i className="bi bi-exclamation-triangle-fill fs-4 me-3"></i>
                    <div>
                      <strong>Certificate Superseded</strong>
                      <div className="small">
                        This certificate has been replaced by an updated credential due to an approved clinical correction. It remains available for historical and audit reference.
                      </div>
                    </div>
                  </div>
                )}

                <div className="row g-3">
                  <div className="col-12 col-md-6">
                    <div className="p-3 border rounded bg-light">
                      <div className="text-secondary small fw-semibold">Dose Administered</div>
                      <div className="fs-5 fw-bold text-dark">Dose {selectedCert.doseNumber || 1}</div>
                    </div>
                  </div>
                  <div className="col-12 col-md-6">
                    <div className="p-3 border rounded bg-light">
                      <div className="text-secondary small fw-semibold">Status</div>
                      <div className="mt-1">{getStatusBadge(selectedCert.status)}</div>
                    </div>
                  </div>
                  <div className="col-12 col-md-6">
                    <div className="p-3 border rounded bg-light">
                      <div className="text-secondary small fw-semibold">Issued Timestamp</div>
                      <div className="text-dark">
                        {selectedCert.issuedAt ? new Date(selectedCert.issuedAt).toLocaleString() : 'N/A'}
                      </div>
                    </div>
                  </div>
                  <div className="col-12 col-md-6">
                    <div className="p-3 border rounded bg-light">
                      <div className="text-secondary small fw-semibold">Administration Time</div>
                      <div className="text-dark">
                        {selectedCert.administrationTime ? new Date(selectedCert.administrationTime).toLocaleString() : 'N/A'}
                      </div>
                    </div>
                  </div>
                  <div className="col-12 col-md-6">
                    <div className="p-3 border rounded bg-light">
                      <div className="text-secondary small fw-semibold">Vaccine Batch ID</div>
                      <div className="font-monospace text-dark">#{selectedCert.vaccineBatchId}</div>
                    </div>
                  </div>
                  <div className="col-12 col-md-6">
                    <div className="p-3 border rounded bg-light">
                      <div className="text-secondary small fw-semibold">Vaccination Record ID</div>
                      <div className="font-monospace text-dark">#{selectedCert.vaccinationRecordId}</div>
                    </div>
                  </div>

                  {selectedCert.verificationToken && (
                    <div className="col-12">
                      <div className="p-3 border rounded bg-light">
                        <div className="d-flex justify-content-between align-items-center mb-1">
                          <span className="text-secondary small fw-semibold">Public Verification Token</span>
                          <button
                            type="button"
                            className="btn btn-sm btn-link p-0 text-decoration-none"
                            onClick={() => {
                              navigator.clipboard.writeText(selectedCert.verificationToken);
                              setCopiedToken(true);
                              setTimeout(() => setCopiedToken(false), 2000);
                            }}
                          >
                            {copiedToken ? (
                              <span className="text-success small"><i className="bi bi-check me-1"></i>Copied</span>
                            ) : (
                              <span className="small"><i className="bi bi-clipboard me-1"></i>Copy Token</span>
                            )}
                          </button>
                        </div>
                        <div className="font-monospace small text-break p-2 bg-white rounded border">
                          {selectedCert.verificationToken}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>
              <div className="modal-footer bg-light border-top">
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm"
                  onClick={() => setSelectedCert(null)}
                >
                  Close
                </button>
                {selectedCert.id && (
                  <button
                    type="button"
                    className="btn btn-primary btn-sm fw-semibold"
                    disabled={downloadingId === selectedCert.id}
                    onClick={() => handleDownload(selectedCert.id, selectedCert.certificateReference)}
                  >
                    {downloadingId === selectedCert.id ? (
                      <>
                        <span className="spinner-border spinner-border-sm me-1" role="status"></span>
                        Downloading...
                      </>
                    ) : (
                      <>
                        <i className="bi bi-file-earmark-pdf-fill me-1"></i>Download Official PDF
                      </>
                    )}
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
