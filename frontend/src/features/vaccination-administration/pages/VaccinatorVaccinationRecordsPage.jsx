import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { listVaccinatorRecords, downloadCertificate } from '../api/certificateApi';

export default function VaccinatorVaccinationRecordsPage() {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedRecord, setSelectedRecord] = useState(null);
  const [downloadingId, setDownloadingId] = useState(null);
  const [downloadError, setDownloadError] = useState('');

  const fetchRecords = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await listVaccinatorRecords();
      setRecords(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Failed to load your administered vaccination records.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchRecords();
  }, [fetchRecords]);

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
        return <span className="badge bg-secondary-subtle text-secondary border border-secondary fw-semibold">RECORDED</span>;
    }
  };

  const filteredRecords = useMemo(() => {
    if (!searchQuery.trim()) return records;
    const q = searchQuery.toLowerCase().trim();
    return records.filter((rec) => {
      const idMatch = String(rec.id || '').includes(q);
      const apptMatch = String(rec.appointmentId || '').includes(q);
      const citizenMatch = String(rec.citizenId || '').includes(q) || (rec.citizenName && rec.citizenName.toLowerCase().includes(q));
      const refMatch = rec.certificateReference && rec.certificateReference.toLowerCase().includes(q);
      const batchMatch = (rec.batchNumber && rec.batchNumber.toLowerCase().includes(q)) || String(rec.vaccineBatchId || '').includes(q);
      const vaccineMatch = rec.vaccineName && rec.vaccineName.toLowerCase().includes(q);
      const centerMatch = rec.centerName && rec.centerName.toLowerCase().includes(q);
      return idMatch || apptMatch || citizenMatch || refMatch || batchMatch || vaccineMatch || centerMatch;
    });
  }, [records, searchQuery]);

  return (
    <div className="container py-4">
      {/* Header section */}
      <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
        <div>
          <nav aria-label="breadcrumb">
            <ol className="breadcrumb mb-1 small">
              <li className="breadcrumb-item"><Link to="/app/staff">Staff Workspace</Link></li>
              <li className="breadcrumb-item active" aria-current="page">Vaccination Records</li>
            </ol>
          </nav>
          <div className="d-flex align-items-center gap-2">
            <span className="badge text-bg-primary">MF-05</span>
            <h1 className="h3 fw-bold text-dark mb-0">Administered Vaccination Records</h1>
          </div>
          <p className="text-secondary small mb-0 mt-1">
            Permanent, immutable records of vaccinations administered by your account with access to official certificates.
          </p>
        </div>
        <div className="d-flex gap-2">
          <Link to="/app/staff/vaccination-drafts" className="btn btn-outline-primary btn-sm fw-semibold">
            <i className="bi bi-pencil-square me-1"></i>Vaccination Drafts
          </Link>
          <button
            type="button"
            className="btn btn-outline-secondary btn-sm"
            onClick={fetchRecords}
            disabled={loading}
          >
            <i className="bi bi-arrow-clockwise me-1"></i>Refresh
          </button>
        </div>
      </div>

      {downloadError && (
        <div className="alert alert-danger alert-dismissible fade show" role="alert">
          <i className="bi bi-exclamation-triangle-fill me-2"></i>{downloadError}
          <button type="button" className="btn-close" onClick={() => setDownloadError('')} aria-label="Close"></button>
        </div>
      )}

      {/* Search and summary bar */}
      <div className="card shadow-sm border-0 mb-4 bg-light">
        <div className="card-body p-3">
          <div className="row g-2 align-items-center">
            <div className="col-12 col-md-6 col-lg-4">
              <div className="input-group input-group-sm">
                <span className="input-group-text bg-white"><i className="bi bi-search"></i></span>
                <input
                  type="text"
                  className="form-control"
                  placeholder="Filter by citizen, batch, ref, record ID..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  aria-label="Filter vaccination records"
                />
                {searchQuery && (
                  <button
                    className="btn btn-outline-secondary"
                    type="button"
                    onClick={() => setSearchQuery('')}
                    aria-label="Clear search"
                  >
                    Clear
                  </button>
                )}
              </div>
            </div>
            <div className="col-12 col-md-6 col-lg-8 text-md-end text-secondary small">
              Showing <strong>{filteredRecords.length}</strong> of <strong>{records.length}</strong> administered records
            </div>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading records...</span>
          </div>
          <p className="text-secondary small mt-2">Loading your administered vaccination records...</p>
        </div>
      ) : error ? (
        <div className="card shadow-sm border-danger p-4 text-center">
          <i className="bi bi-x-circle text-danger fs-1 mb-2"></i>
          <h5 className="text-danger fw-bold">Unable to Load Records</h5>
          <p className="text-secondary small">{error}</p>
          <div>
            <button className="btn btn-outline-danger btn-sm" onClick={fetchRecords}>
              <i className="bi bi-arrow-clockwise me-1"></i>Retry
            </button>
          </div>
        </div>
      ) : records.length === 0 ? (
        <div className="card shadow-sm border-0 p-5 text-center bg-light">
          <div className="mb-3 text-secondary">
            <i className="bi bi-journal-medical" style={{ fontSize: '3rem' }}></i>
          </div>
          <h4 className="fw-bold text-dark">No Administered Records Yet</h4>
          <p className="text-secondary small max-width-md mx-auto mb-4">
            You have not finalized any vaccination drafts yet. When you complete physical administration and finalize a draft in the vaccination drafts workspace, your permanent clinical records will be securely recorded here.
          </p>
          <div>
            <Link to="/app/staff/vaccination-drafts" className="btn btn-primary fw-semibold px-4">
              Open Vaccination Drafts Workspace
            </Link>
          </div>
        </div>
      ) : filteredRecords.length === 0 ? (
        <div className="alert alert-info text-center py-4">
          No records match your filter: <strong>"{searchQuery}"</strong>.
        </div>
      ) : (
        <>
          {/* Desktop Table View */}
          <div className="d-none d-md-block">
            <div className="card shadow-sm border-0 overflow-hidden">
              <div className="table-responsive">
                <table className="table table-hover align-middle mb-0">
                  <thead className="table-light small">
                    <tr>
                      <th scope="col" className="ps-3">Record ID</th>
                      <th scope="col">Citizen</th>
                      <th scope="col">Vaccine &amp; Dose</th>
                      <th scope="col">Batch Code</th>
                      <th scope="col">Center</th>
                      <th scope="col">Admin Date</th>
                      <th scope="col">Certificate</th>
                      <th scope="col" className="text-end pe-3">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="small">
                    {filteredRecords.map((rec) => (
                      <tr key={rec.id}>
                        <td className="ps-3 fw-bold font-monospace">#{rec.id}</td>
                        <td>
                          <div className="fw-semibold text-dark">{rec.citizenName || `Citizen #${rec.citizenId}`}</div>
                          <div className="text-secondary text-xs">ID: {rec.citizenId} · Appt #{rec.appointmentId}</div>
                        </td>
                        <td>
                          <div>{rec.vaccineName || 'Authorized Vaccine'}</div>
                          <span className="badge text-bg-light border text-dark">Dose {rec.doseNumber}</span>
                        </td>
                        <td className="font-monospace text-secondary">
                          {rec.batchNumber || `Batch #${rec.vaccineBatchId}`}
                        </td>
                        <td className="text-secondary">{rec.centerName || `Center #${rec.centerId}`}</td>
                        <td className="text-secondary">
                          {rec.administrationTime
                            ? new Date(rec.administrationTime).toLocaleDateString(undefined, { dateStyle: 'medium' })
                            : 'N/A'}
                        </td>
                        <td>
                          {rec.certificateStatus ? (
                            <div>
                              <div>{getStatusBadge(rec.certificateStatus)}</div>
                              <span className="font-monospace text-xs text-secondary d-block mt-1">
                                {rec.certificateReference}
                              </span>
                            </div>
                          ) : (
                            <span className="text-muted fst-italic">None</span>
                          )}
                        </td>
                        <td className="text-end pe-3 text-nowrap">
                          <button
                            type="button"
                            className="btn btn-outline-secondary btn-sm me-2"
                            onClick={() => setSelectedRecord(rec)}
                            aria-label={`View details for record ${rec.id}`}
                          >
                            <i className="bi bi-eye me-1"></i>View
                          </button>
                          {rec.certificateId && (
                            <button
                              type="button"
                              className="btn btn-outline-primary btn-sm"
                              disabled={downloadingId === rec.certificateId}
                              onClick={() => handleDownload(rec.certificateId, rec.certificateReference)}
                              aria-label={`Download certificate PDF for record ${rec.id}`}
                            >
                              {downloadingId === rec.certificateId ? (
                                <span className="spinner-border spinner-border-sm" role="status"></span>
                              ) : (
                                <>
                                  <i className="bi bi-file-earmark-pdf me-1"></i>PDF
                                </>
                              )}
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* Mobile Card View */}
          <div className="d-md-none vstack gap-3">
            {filteredRecords.map((rec) => (
              <div key={rec.id} className="card shadow-sm border-0">
                <div className="card-body p-3">
                  <div className="d-flex justify-content-between align-items-start mb-2">
                    <div>
                      <span className="fw-bold font-monospace">Record #{rec.id}</span>
                      <div className="small text-secondary">
                        {rec.administrationTime
                          ? new Date(rec.administrationTime).toLocaleDateString(undefined, { dateStyle: 'medium' })
                          : 'N/A'}
                      </div>
                    </div>
                    {rec.certificateStatus && getStatusBadge(rec.certificateStatus)}
                  </div>
                  <div className="mb-2">
                    <strong className="d-block text-dark">{rec.citizenName || `Citizen #${rec.citizenId}`}</strong>
                    <span className="small text-secondary">
                      {rec.vaccineName || 'Authorized Vaccine'} · Dose {rec.doseNumber}
                    </span>
                  </div>
                  <div className="small text-secondary mb-3">
                    <div><strong>Batch:</strong> <span className="font-monospace">{rec.batchNumber || `Batch #${rec.vaccineBatchId}`}</span></div>
                    <div><strong>Center:</strong> {rec.centerName || `Center #${rec.centerId}`}</div>
                    {rec.certificateReference && (
                      <div><strong>Ref:</strong> <span className="font-monospace">{rec.certificateReference}</span></div>
                    )}
                  </div>
                  <div className="d-flex gap-2">
                    <button
                      type="button"
                      className="btn btn-outline-secondary btn-sm flex-fill"
                      onClick={() => setSelectedRecord(rec)}
                    >
                      <i className="bi bi-eye me-1"></i>View Details
                    </button>
                    {rec.certificateId && (
                      <button
                        type="button"
                        className="btn btn-outline-primary btn-sm flex-fill"
                        disabled={downloadingId === rec.certificateId}
                        onClick={() => handleDownload(rec.certificateId, rec.certificateReference)}
                      >
                        {downloadingId === rec.certificateId ? (
                          <span className="spinner-border spinner-border-sm" role="status"></span>
                        ) : (
                          <>
                            <i className="bi bi-file-earmark-pdf me-1"></i>PDF
                          </>
                        )}
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      {/* Record Details Modal */}
      {selectedRecord && (
        <div className="modal fade show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }}>
          <div className="modal-dialog modal-dialog-centered modal-lg">
            <div className="modal-content shadow border-0 overflow-hidden">
              <div className="modal-header bg-light border-bottom">
                <h5 className="modal-title fw-bold text-dark">
                  <i className="bi bi-journal-medical text-primary me-2"></i>
                  Vaccination Record #{selectedRecord.id}
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setSelectedRecord(null)}
                  aria-label="Close"
                ></button>
              </div>
              <div className="modal-body p-4">
                <div className="alert alert-info py-2 small mb-3">
                  <i className="bi bi-info-circle-fill me-2"></i>
                  This is a permanent immutable clinical record. It cannot be altered or deleted.
                </div>

                <div className="row g-3 small">
                  <div className="col-sm-6">
                    <span className="text-secondary d-block">Citizen Recipient:</span>
                    <strong className="fs-6 text-dark">{selectedRecord.citizenName || `Citizen #${selectedRecord.citizenId}`}</strong>
                    <div className="text-secondary text-xs">Subject ID: {selectedRecord.citizenId}</div>
                  </div>
                  <div className="col-sm-6">
                    <span className="text-secondary d-block">Appointment ID:</span>
                    <strong className="text-dark">#{selectedRecord.appointmentId}</strong>
                  </div>

                  <div className="col-sm-6 border-top pt-2">
                    <span className="text-secondary d-block">Vaccine Formulation:</span>
                    <strong className="text-dark">{selectedRecord.vaccineName || 'Authorized Formulation'}</strong>
                  </div>
                  <div className="col-sm-6 border-top pt-2">
                    <span className="text-secondary d-block">Dose Number:</span>
                    <strong className="text-dark">Dose {selectedRecord.doseNumber}</strong>
                  </div>

                  <div className="col-sm-6 border-top pt-2">
                    <span className="text-secondary d-block">Vaccine Batch:</span>
                    <span className="text-dark font-monospace fw-semibold">
                      {selectedRecord.batchNumber || `Batch #${selectedRecord.vaccineBatchId}`}
                    </span>
                  </div>
                  <div className="col-sm-6 border-top pt-2">
                    <span className="text-secondary d-block">Vaccination Center:</span>
                    <span className="text-dark">{selectedRecord.centerName || `Center #${selectedRecord.centerId}`}</span>
                  </div>

                  <div className="col-sm-6 border-top pt-2">
                    <span className="text-secondary d-block">Administration Time:</span>
                    <span className="text-dark">
                      {selectedRecord.administrationTime ? new Date(selectedRecord.administrationTime).toLocaleString() : 'N/A'}
                    </span>
                  </div>
                  <div className="col-sm-6 border-top pt-2">
                    <span className="text-secondary d-block">Finalized Timestamp:</span>
                    <span className="text-dark">
                      {selectedRecord.finalizedAt ? new Date(selectedRecord.finalizedAt).toLocaleString() : 'N/A'}
                    </span>
                  </div>

                  {selectedRecord.nextDoseDate && (
                    <div className="col-sm-6 border-top pt-2">
                      <span className="text-secondary d-block">Next Dose Date:</span>
                      <span className="text-dark">{selectedRecord.nextDoseDate}</span>
                    </div>
                  )}

                  <div className="col-12 border-top pt-2">
                    <span className="text-secondary d-block">Authorized Clinical Notes:</span>
                    <div className="p-2 bg-light rounded text-break mt-1 border">
                      {selectedRecord.authorizedNotes || <span className="text-muted fst-italic">No clinical notes recorded.</span>}
                    </div>
                  </div>

                  {selectedRecord.certificateReference && (
                    <div className="col-12 border-top pt-3">
                      <h6 className="fw-bold text-dark mb-2">Associated Digital Certificate</h6>
                      <div className="p-3 bg-light rounded border d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-2">
                        <div>
                          <div className="font-monospace fw-bold">{selectedRecord.certificateReference}</div>
                          <div className="mt-1">{getStatusBadge(selectedRecord.certificateStatus)}</div>
                        </div>
                        {selectedRecord.certificateId && (
                          <button
                            type="button"
                            className="btn btn-outline-primary btn-sm fw-semibold"
                            disabled={downloadingId === selectedRecord.certificateId}
                            onClick={() => handleDownload(selectedRecord.certificateId, selectedRecord.certificateReference)}
                          >
                            {downloadingId === selectedRecord.certificateId ? (
                              <span className="spinner-border spinner-border-sm me-1" role="status"></span>
                            ) : (
                              <i className="bi bi-file-earmark-pdf-fill me-1"></i>
                            )}
                            Download Certificate PDF
                          </button>
                        )}
                      </div>
                    </div>
                  )}
                </div>
              </div>
              <div className="modal-footer bg-light border-top">
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => setSelectedRecord(null)}
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
