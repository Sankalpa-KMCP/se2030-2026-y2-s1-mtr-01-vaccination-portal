import React from 'react';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

function formatDate(dateStr) {
  if (!dateStr) return '—';
  try {
    const d = new Date(dateStr);
    return isNaN(d.getTime()) ? dateStr : d.toLocaleDateString();
  } catch {
    return dateStr;
  }
}

export default function DoseProgrammeTab({
  doseProgrammes = [],
  loading = false,
  error = null,
  onOpenCreateForm,
  onOpenNewVersionForm,
}) {
  const latestProgramme = doseProgrammes.length > 0 ? doseProgrammes[0] : null;

  return (
    <div className="dose-programme-tab-content">
      <ApiErrorAlert error={error} />

      <div className="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h6 className="fw-bold text-dark mb-0">Clinical Dose Programme Configuration</h6>
          <p className="text-secondary small mb-0">
            Define permitted dose numbers, minimum intervals, and version history for this campaign.
          </p>
        </div>

        <div>
          {doseProgrammes.length === 0 ? (
            <button
              type="button"
              className="btn btn-primary btn-sm fw-semibold"
              onClick={onOpenCreateForm}
              disabled={loading}
            >
              + Configure Initial Programme
            </button>
          ) : (
            <button
              type="button"
              className="btn btn-outline-primary btn-sm fw-semibold"
              onClick={onOpenNewVersionForm}
              disabled={loading}
            >
              + Create New Version
            </button>
          )}
        </div>
      </div>

      {loading ? (
        <LoadingSpinner message="Loading dose programme history..." />
      ) : doseProgrammes.length === 0 ? (
        <div className="text-center py-4 bg-light rounded border border-dashed">
          <p className="text-muted mb-2">No dose programme configured for this campaign yet.</p>
          <p className="small text-secondary mb-3">
            An active dose programme is required before this campaign can be scheduled or published.
          </p>
          <button
            type="button"
            className="btn btn-primary btn-sm"
            onClick={onOpenCreateForm}
          >
            Configure Dose Programme
          </button>
        </div>
      ) : (
        <>
          {/* Active Programme Highlights */}
          {latestProgramme && (
            <div className="card bg-light border-primary border-opacity-25 mb-4 shadow-sm">
              <div className="card-body py-3">
                <div className="d-flex justify-content-between align-items-center mb-2">
                  <span className="badge bg-primary">
                    Active Version: v{latestProgramme.versionNumber}
                  </span>
                  <span className="small text-muted font-monospace">
                    Effective: {formatDate(latestProgramme.activeFrom)} &rarr; {latestProgramme.activeTo ? formatDate(latestProgramme.activeTo) : 'Current'}
                  </span>
                </div>
                <div className="row g-2 small">
                  <div className="col-sm-4">
                    <strong>Category:</strong> {latestProgramme.doseCategory}
                  </div>
                  <div className="col-sm-4">
                    <strong>Permitted Doses:</strong> {latestProgramme.permittedDoseNumbers}
                  </div>
                  <div className="col-sm-4">
                    <strong>Min Interval:</strong> {latestProgramme.minimumIntervalDays} days
                  </div>
                  <div className="col-sm-4">
                    <strong>Service Duration:</strong> {latestProgramme.expectedServiceMinutes ? `${latestProgramme.expectedServiceMinutes} min` : 'Default'}
                  </div>
                  <div className="col-sm-4">
                    <strong>Previous Proof Required:</strong> {latestProgramme.previousEvidenceRequired ? 'Yes' : 'No'}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Version History Table */}
          <h6 className="fw-bold text-secondary small text-uppercase mb-2">Version History (Audit Log)</h6>
          <div className="table-responsive">
            <table className="table table-sm table-hover align-middle small mb-0">
              <thead className="table-light">
                <tr>
                  <th scope="col">Version</th>
                  <th scope="col">Category</th>
                  <th scope="col">Dose Sequence</th>
                  <th scope="col">Min Interval</th>
                  <th scope="col">Service Min</th>
                  <th scope="col">Evidence</th>
                  <th scope="col">Effective Period</th>
                </tr>
              </thead>
              <tbody>
                {doseProgrammes.map((prog) => (
                  <tr key={prog.id || prog.versionNumber}>
                    <td className="fw-bold font-monospace">v{prog.versionNumber}</td>
                    <td>
                      <span className={`badge ${prog.doseCategory === 'PRIMARY' ? 'bg-secondary' : 'bg-info text-dark'}`}>
                        {prog.doseCategory}
                      </span>
                    </td>
                    <td>{prog.permittedDoseNumbers}</td>
                    <td>{prog.minimumIntervalDays} days</td>
                    <td>{prog.expectedServiceMinutes ? `${prog.expectedServiceMinutes} min` : '—'}</td>
                    <td>
                      <span className={`badge ${prog.previousEvidenceRequired ? 'bg-warning text-dark' : 'bg-light text-dark border'}`}>
                        {prog.previousEvidenceRequired ? 'Required' : 'Optional'}
                      </span>
                    </td>
                    <td>
                      {formatDate(prog.activeFrom)} &rarr; {prog.activeTo ? formatDate(prog.activeTo) : <span className="text-success fw-semibold">Active</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
