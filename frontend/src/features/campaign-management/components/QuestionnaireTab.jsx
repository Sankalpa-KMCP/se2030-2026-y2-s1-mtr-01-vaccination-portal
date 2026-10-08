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

export default function QuestionnaireTab({
  campaign,
  assignments = [],
  versionsMap = {},
  loading = false,
  error = null,
  canManageAssignment = false,
  onOpenAssignModal,
}) {
  const todayStr = new Date().toISOString().split('T')[0];

  // Date-aware assignment classification
  const currentAssignment = assignments.find(
    (a) => a.activeFrom <= todayStr && (a.activeTo == null || a.activeTo > todayStr)
  );

  const futureAssignments = assignments.filter((a) => a.activeFrom > todayStr);

  const isTerminal = ['CANCELLED', 'COMPLETED', 'ARCHIVED'].includes(campaign?.status);

  const activeOrLatestAssignment = currentAssignment || (assignments.length > 0 ? assignments[0] : null);
  const assignedVersionId = activeOrLatestAssignment?.questionnaireVersionId || campaign?.questionnaireVersionId;
  const assignedVersionInfo = assignedVersionId ? versionsMap[assignedVersionId] : null;

  const resolveVersionLabel = (versionId) => {
    const info = versionsMap[versionId];
    if (info) {
      return `${info.questionnaireName || info.questionnaireCode} — v${info.versionNumber}`;
    }
    return `Questionnaire Version #${versionId}`;
  };

  return (
    <div className="questionnaire-tab-content">
      <ApiErrorAlert error={error} />

      <div className="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h6 className="fw-bold text-dark mb-0">Screening Questionnaire</h6>
          <p className="text-secondary small mb-0">
            Clinical screening questionnaire assigned for citizen appointment bookings.
          </p>
        </div>

        <div>
          {canManageAssignment && !isTerminal && (
            <button
              type="button"
              className={`btn btn-sm fw-semibold ${assignments.length === 0 ? 'btn-primary' : 'btn-outline-primary'}`}
              onClick={onOpenAssignModal}
              disabled={loading}
            >
              {assignments.length === 0 ? '+ Assign Screening Questionnaire' : '+ Reassign / Update Version'}
            </button>
          )}
        </div>
      </div>

      {loading ? (
        <LoadingSpinner message="Loading questionnaire assignments..." />
      ) : assignments.length === 0 ? (
        <div className="p-3 bg-light rounded border text-start">
          <div className="fw-bold text-dark mb-1">Citizen Screening Questionnaire</div>
          <div className="fw-semibold text-muted mb-1">Not assigned yet</div>
          <div className="form-text small text-muted mb-0">
            A Healthcare Officer must configure the screening questionnaire before this campaign can be scheduled.
          </div>
          {canManageAssignment && !isTerminal && (
            <div className="mt-3">
              <button
                type="button"
                className="btn btn-primary btn-sm"
                onClick={onOpenAssignModal}
              >
                Assign Questionnaire
              </button>
            </div>
          )}
        </div>
      ) : (
        <>
          {/* Active / Current Assignment Summary */}
          {activeOrLatestAssignment && (
            <div className="card bg-light border-success border-opacity-50 mb-3 shadow-sm">
              <div className="card-body py-3">
                <div className="d-flex justify-content-between align-items-center mb-2">
                  <span className="badge bg-success">
                    {currentAssignment ? 'Active Screening Questionnaire' : 'Assigned Screening Questionnaire'}
                  </span>
                  <span className="small text-muted font-monospace">
                    Effective: {formatDate(activeOrLatestAssignment.activeFrom)} &rarr; {activeOrLatestAssignment.activeTo ? formatDate(activeOrLatestAssignment.activeTo) : 'Current'}
                  </span>
                </div>
                <h5 className="fw-bold text-dark mb-1">
                  {assignedVersionInfo?.questionnaireName ||
                    assignedVersionInfo?.questionnaireCode ||
                    `Questionnaire Version #${assignedVersionId}`}
                </h5>
                <div className="small text-secondary mb-1">
                  Version {assignedVersionInfo?.versionNumber ?? assignedVersionId} · Published
                </div>
                <div className="form-text small text-muted mb-0">
                  Clinical questionnaire managed by Healthcare Officer
                </div>
              </div>
            </div>
          )}

          {/* Scheduled Future Assignments */}
          {futureAssignments.map((fut) => (
            <div key={fut.id} className="card bg-light border-info border-opacity-50 mb-3 shadow-sm">
              <div className="card-body py-2">
                <div className="d-flex justify-content-between align-items-center">
                  <div>
                    <span className="badge bg-info text-dark me-2">Scheduled Future Version</span>
                    <span className="fw-semibold small">{resolveVersionLabel(fut.questionnaireVersionId)}</span>
                  </div>
                  <span className="small text-muted">
                    Becomes Effective: <strong>{formatDate(fut.activeFrom)}</strong>
                  </span>
                </div>
              </div>
            </div>
          ))}

          {/* Assignment Version History Audit */}
          <h6 className="fw-bold text-secondary small text-uppercase mb-2 mt-4">
            Assignment Version History
          </h6>
          <div className="table-responsive">
            <table className="table table-sm table-hover align-middle small mb-0">
              <thead className="table-light">
                <tr>
                  <th scope="col">Status</th>
                  <th scope="col">Questionnaire / Version</th>
                  <th scope="col">Version Ref ID</th>
                  <th scope="col">Effective Period</th>
                </tr>
              </thead>
              <tbody>
                {assignments.map((asgn) => {
                  const isCur = currentAssignment?.id === asgn.id;
                  const isFut = asgn.activeFrom > todayStr;
                  return (
                    <tr key={asgn.id}>
                      <td>
                        {isCur ? (
                          <span className="badge bg-success">Active</span>
                        ) : isFut ? (
                          <span className="badge bg-info text-dark">Scheduled</span>
                        ) : (
                          <span className="badge bg-secondary opacity-75">Historical</span>
                        )}
                      </td>
                      <td className="fw-semibold">
                        {resolveVersionLabel(asgn.questionnaireVersionId)}
                      </td>
                      <td className="font-monospace text-muted">#{asgn.questionnaireVersionId}</td>
                      <td>
                        {formatDate(asgn.activeFrom)} &rarr;{' '}
                        {asgn.activeTo ? (
                          formatDate(asgn.activeTo)
                        ) : (
                          <span className="text-success fw-semibold">Open-ended</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
