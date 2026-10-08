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

  // Correction request modal state
  const [correctionTarget, setCorrectionTarget] = useState(null);
  const [correctionMessage, setCorrectionMessage] = useState('');
  const [submittingCorrection, setSubmittingCorrection] = useState(false);
  const [correctionError, setCorrectionError] = useState('');
  const [correctionSuccess, setCorrectionSuccess] = useState('');

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

  const openCorrectionModal = (target) => {
    setCorrectionTarget(target);
    setCorrectionMessage('');
    setCorrectionError('');
    setCorrectionSuccess('');
  };

  const closeCorrectionModal = () => {
    setCorrectionTarget(null);
    setCorrectionMessage('');
    setCorrectionError('');
    setCorrectionSuccess('');
  };

  const handleSubmitCorrection = async (e) => {
    e.preventDefault();
    if (!correctionMessage.trim()) {
      setCorrectionError('Please describe the problem you would like corrected.');
      return;
    }

    const recordId = correctionTarget.vaccinationRecordId || correctionTarget.id;
    if (!recordId) {
      setCorrectionError('Associated vaccination record ID is missing.');
      return;
    }

    setSubmittingCorrection(true);
    setCorrectionError('');
    try {
      await requestVaccinationCorrection(recordId, correctionMessage.trim());
      setCorrectionSuccess('Correction request submitted for review.');
      await fetchData();
      setTimeout(() => {
        closeCorrectionModal();
      }, 1500);
    } catch (err) {
      setCorrectionError(err.message || 'Failed to submit correction request. Please try again.');
    } finally {
      setSubmittingCorrection(false);
    }
  };

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
            View your permanent vaccination history and access officially signed digital certificates.
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
      ) : records.length === 0 ? (
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
          {/* Certificates Section */}
          <div className="col-12">
            <div className="card shadow-sm border-0 overflow-hidden">
              <div className="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                <h5 className="h6 mb-0 fw-bold text-dark">
                  <i className="bi bi-patch-check-fill text-primary me-2"></i>Digital Vaccination Certificates
                </h5>
                <span className="badge bg-primary-subtle text-primary fw-semibold">
                  {certificates.length} {certificates.length === 1 ? 'Certificate' : 'Certificates'}
                </span>
              </div>
              <div className="card-body p-0">
                {certificates.length === 0 ? (
                  <div className="p-4 text-center text-secondary small">
                    No certificates have been issued yet.
                  </div>
                ) : (
                  <div className="table-responsive">
                    <table className="table table-hover align-middle mb-0">
                      <thead className="table-light small text-uppercase text-secondary">
                        <tr>
                          <th className="ps-4">Certificate Reference</th>
                          <th>Dose</th>
                          <th>Status</th>
                          <th>Correction</th>
                          <th>Issued Date</th>
                          <th className="text-end pe-4">Actions</th>
                        </tr>
                      </thead>
                      <tbody className="small">
                        {certificates.map((cert) => {
                          const existingCorrection = findActiveCorrection(cert.vaccinationRecordId);
                          return (
                            <tr key={cert.id}>
                              <td className="ps-4 font-monospace fw-semibold text-dark">
                                {cert.certificateReference}
                              </td>
                              <td>Dose {cert.doseNumber || 1}</td>
                              <td>{getStatusBadge(cert.status)}</td>
                              <td>
                                {existingCorrection ? (
                                  getCorrectionBadge(existingCorrection.status)
                                ) : (
                                  <span className="text-muted">—</span>
                                )}
                              </td>
                              <td className="text-secondary">
                                {cert.issuedAt ? new Date(cert.issuedAt).toLocaleDateString(undefined, { dateStyle: 'medium' }) : 'N/A'}
                              </td>
                              <td className="text-end pe-4">
                                <div className="d-flex justify-content-end gap-2 flex-wrap">
                                  <button
                                    type="button"
                                    className="btn btn-outline-secondary btn-sm"
                                    onClick={() => setSelectedCert(cert)}
                                    title="View Certificate Details"
                                  >
                                    <i className="bi bi-eye me-1"></i>View
                                  </button>
                                  <button
                                    type="button"
                                    className="btn btn-outline-primary btn-sm"
                                    disabled={downloadingId === cert.id}
                                    onClick={() => handleDownload(cert.id, cert.certificateReference)}
                                    title="Download Official PDF Certificate"
                                  >
                                    {downloadingId === cert.id ? (
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
                                  {cert.verificationToken && (
                                    <Link
                                      to={`/verify-certificate?token=${encodeURIComponent(cert.verificationToken)}`}
                                      className="btn btn-outline-success btn-sm"
                                      title="Verify Publicly"
                                    >
                                      <i className="bi bi-qr-code me-1"></i>Verify
                                    </Link>
                                  )}
                                  {cert.status === 'VALID' && (
                                    <button
                                      type="button"
                                      className="btn btn-outline-warning btn-sm text-dark"
                                      onClick={() => openCorrectionModal(cert)}
                                      title="Request Correction"
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
                )}
              </div>
            </div>
          </div>

          {/* Vaccination History Records Section */}
          <div className="col-12">
            <div className="card shadow-sm border-0 overflow-hidden">
              <div className="card-header bg-white py-3 border-bottom">
                <h5 className="h6 mb-0 fw-bold text-dark">
                  <i className="bi bi-journal-medical text-primary me-2"></i>Permanent Clinical Vaccination History
                </h5>
              </div>
              <div className="card-body p-0">
                <div className="table-responsive">
                  <table className="table table-hover align-middle mb-0">
                    <thead className="table-light small text-uppercase text-secondary">
                      <tr>
                        <th className="ps-4">Administered Date</th>
                        <th>Dose</th>
                        <th>Vaccine Batch</th>
                        <th>Center</th>
                        <th>Next Dose</th>
                        <th>Certificate</th>
                        <th className="text-end pe-4">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="small">
                      {records.map((rec) => {
                        const existingCorrection = findActiveCorrection(rec.id);
                        return (
                          <tr key={rec.id}>
                            <td className="ps-4 fw-semibold text-dark">
                              {rec.administrationTime ? new Date(rec.administrationTime).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) : 'N/A'}
                            </td>
                            <td>
                              <span className="badge bg-light text-dark border">
                                Dose {rec.doseNumber}
                              </span>
                            </td>
                            <td className="font-monospace text-secondary">
                              Batch #{rec.vaccineBatchId}
                            </td>
                            <td className="text-secondary">
                              Center #{rec.centerId}
                            </td>
                            <td className="text-secondary">
                              {rec.nextDoseDate ? new Date(rec.nextDoseDate).toLocaleDateString(undefined, { dateStyle: 'medium' }) : 'None required'}
                            </td>
                            <td>
                              {rec.certificateStatus ? (
                                <div className="d-flex align-items-center gap-2">
                                  {getStatusBadge(rec.certificateStatus)}
                                  {existingCorrection && getCorrectionBadge(existingCorrection.status)}
                                  {rec.certificateId && (
                                    <button
                                      type="button"
                                      className="btn btn-link btn-sm p-0 text-decoration-none"
                                      onClick={() => handleDownload(rec.certificateId, rec.certificateReference)}
                                    >
                                      <i className="bi bi-download"></i>
                                    </button>
                                  )}
                                </div>
                              ) : (
                                <span className="text-muted fst-italic">None</span>
                              )}
                            </td>
                            <td className="text-end pe-4">
                              <button
                                type="button"
                                className="btn btn-outline-warning btn-sm text-dark"
                                onClick={() => openCorrectionModal(rec)}
                                title="Request Correction"
                              >
                                <i className="bi bi-pencil-square me-1"></i>Request Correction
                              </button>
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

      {/* Citizen Request Correction Modal */}
      {correctionTarget && (
        <div className="modal fade show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content shadow border-0 overflow-hidden">
              <div className="modal-header bg-light border-bottom">
                <h5 className="modal-title fw-bold text-dark">
                  <i className="bi bi-pencil-square text-warning me-2"></i>
                  Request Correction
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={closeCorrectionModal}
                  aria-label="Close"
                  disabled={submittingCorrection}
                ></button>
              </div>
              <form onSubmit={handleSubmitCorrection}>
                <div className="modal-body p-4">
                  {correctionSuccess && (
                    <div className="alert alert-success d-flex align-items-center mb-3" role="alert">
                      <i className="bi bi-check-circle-fill me-2"></i>
                      <div>{correctionSuccess}</div>
                    </div>
                  )}

                  {correctionError && (
                    <div className="alert alert-danger d-flex align-items-center mb-3" role="alert">
                      <i className="bi bi-exclamation-triangle-fill me-2"></i>
                      <div>{correctionError}</div>
                    </div>
                  )}

                  {/* Summary of Record */}
                  <div className="card bg-light border-0 p-3 mb-3 small">
                    <div className="row g-2">
                      {correctionTarget.certificateReference && (
                        <div className="col-12">
                          <span className="text-secondary">Certificate:</span>{' '}
                          <strong className="font-monospace text-dark">{correctionTarget.certificateReference}</strong>
                        </div>
                      )}
                      <div className="col-6">
                        <span className="text-secondary">Dose:</span>{' '}
                        <strong>Dose {correctionTarget.doseNumber || 1}</strong>
                      </div>
                      <div className="col-6">
                        <span className="text-secondary">Vaccine Batch:</span>{' '}
                        <span className="font-monospace">#{correctionTarget.vaccineBatchId || 'N/A'}</span>
                      </div>
                      {(correctionTarget.administrationTime || correctionTarget.issuedAt) && (
                        <div className="col-12">
                          <span className="text-secondary">Vaccination Date:</span>{' '}
                          <span>{new Date(correctionTarget.administrationTime || correctionTarget.issuedAt).toLocaleDateString(undefined, { dateStyle: 'medium' })}</span>
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="mb-3">
                    <label htmlFor="correctionMessage" className="form-label fw-semibold text-dark small">
                      Describe what you believe should be corrected <span className="text-danger">*</span>
                    </label>
                    <textarea
                      id="correctionMessage"
                      rows={4}
                      className="form-control"
                      placeholder="Example: My surname is displayed incorrectly, or the vaccination details do not match my records."
                      value={correctionMessage}
                      onChange={(e) => setCorrectionMessage(e.target.value)}
                      maxLength={1000}
                      required
                      disabled={submittingCorrection}
                    />
                    <div className="form-text small text-secondary">
                      Max 1000 characters. Please provide clear details so clinical staff can verify your request.
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
                    onClick={closeCorrectionModal}
                    disabled={submittingCorrection}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-warning btn-sm fw-semibold text-dark"
                    disabled={submittingCorrection || !correctionMessage.trim()}
                  >
                    {submittingCorrection ? (
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
                      <div className="small">This certificate is active, cryptographically signed, and valid for travel and health clearance.</div>
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
                {selectedCert.verificationToken && (
                  <Link
                    to={`/verify-certificate?token=${encodeURIComponent(selectedCert.verificationToken)}`}
                    className="btn btn-outline-success btn-sm"
                  >
                    <i className="bi bi-qr-code me-1"></i>Verify Publicly
                  </Link>
                )}
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
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
