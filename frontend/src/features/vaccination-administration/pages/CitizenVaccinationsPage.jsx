import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listMyVaccinations, listMyCertificates, downloadCertificate } from '../api/certificateApi';

export default function CitizenVaccinationsPage() {
  const [records, setRecords] = useState([]);
  const [certificates, setCertificates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [downloadingId, setDownloadingId] = useState(null);
  const [downloadError, setDownloadError] = useState('');
  const [selectedCert, setSelectedCert] = useState(null);

  const fetchData = async () => {
    setLoading(true);
    setError('');
    try {
      const [vaxRecords, certs] = await Promise.all([
        listMyVaccinations(),
        listMyCertificates(),
      ]);
      setRecords(Array.isArray(vaxRecords) ? vaxRecords : []);
      setCertificates(Array.isArray(certs) ? certs : []);
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
                          <th>Issued Date</th>
                          <th className="text-end pe-4">Actions</th>
                        </tr>
                      </thead>
                      <tbody className="small">
                        {certificates.map((cert) => (
                          <tr key={cert.id}>
                            <td className="ps-4 font-monospace fw-semibold text-dark">
                              {cert.certificateReference}
                            </td>
                            <td>Dose {cert.doseNumber || 1}</td>
                            <td>{getStatusBadge(cert.status)}</td>
                            <td className="text-secondary">
                              {cert.issuedAt ? new Date(cert.issuedAt).toLocaleDateString(undefined, { dateStyle: 'medium' }) : 'N/A'}
                            </td>
                            <td className="text-end pe-4">
                              <div className="d-flex justify-content-end gap-2">
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
                              </div>
                            </td>
                          </tr>
                        ))}
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
                      </tr>
                    </thead>
                    <tbody className="small">
                      {records.map((rec) => (
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
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
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
                      <div className="small">An updated certificate has been reissued. This earlier version is preserved for legal audit trail.</div>
                    </div>
                  </div>
                )}

                <div className="row g-3 small">
                  <div className="col-sm-6">
                    <span className="text-secondary d-block">Certificate Reference:</span>
                    <strong className="font-monospace fs-6 text-dark">{selectedCert.certificateReference}</strong>
                  </div>
                  <div className="col-sm-6">
                    <span className="text-secondary d-block">Status:</span>
                    <div>{getStatusBadge(selectedCert.status)}</div>
                  </div>
                  <div className="col-sm-6 border-top pt-2">
                    <span className="text-secondary d-block">Dose Administered:</span>
                    <strong className="text-dark">Dose {selectedCert.doseNumber || 1}</strong>
                  </div>
                  <div className="col-sm-6 border-top pt-2">
                    <span className="text-secondary d-block">Administration Date:</span>
                    <strong className="text-dark">
                      {selectedCert.administrationTime ? new Date(selectedCert.administrationTime).toLocaleString() : 'N/A'}
                    </strong>
                  </div>
                  <div className="col-sm-6 border-top pt-2">
                    <span className="text-secondary d-block">Issued Timestamp:</span>
                    <span className="text-dark">
                      {selectedCert.issuedAt ? new Date(selectedCert.issuedAt).toLocaleString() : 'N/A'}
                    </span>
                  </div>
                  <div className="col-sm-6 border-top pt-2">
                    <span className="text-secondary d-block">Vaccine Batch ID:</span>
                    <span className="text-dark font-monospace">
                      Batch #{selectedCert.vaccineBatchId || 'N/A'}
                    </span>
                  </div>
                  {selectedCert.verificationToken && (
                    <div className="col-12 border-top pt-2">
                      <span className="text-secondary d-block">Verification Token:</span>
                      <div className="p-2 bg-light rounded font-monospace small text-break border">
                        {selectedCert.verificationToken}
                      </div>
                    </div>
                  )}
                </div>
              </div>
              <div className="modal-footer bg-light border-top">
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
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
