import React, { useState, useEffect } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import {
  getScreeningQuestionnaires,
  getQuestionnaireVersions,
} from '../api/campaignConfigurationApi';

function formatDate(dateStr) {
  if (!dateStr) return '—';
  try {
    const d = new Date(dateStr);
    return isNaN(d.getTime()) ? dateStr : d.toLocaleDateString();
  } catch {
    return dateStr;
  }
}

export default function QuestionnaireAssignmentFormModal({
  campaign,
  currentAssignment = null,
  onSubmit,
  onCancel,
  loading,
  error,
}) {
  const [questionnaires, setQuestionnaires] = useState([]);
  const [loadingQuestionnaires, setLoadingQuestionnaires] = useState(false);
  const [selectedQuestionnaireId, setSelectedQuestionnaireId] = useState('');

  const [versions, setVersions] = useState([]);
  const [loadingVersions, setLoadingVersions] = useState(false);
  const [selectedVersionId, setSelectedVersionId] = useState('');

  const todayStr = new Date().toISOString().split('T')[0];
  const [effectiveStartDate, setEffectiveStartDate] = useState(
    campaign?.serviceStartDate && campaign.serviceStartDate >= todayStr
      ? campaign.serviceStartDate
      : todayStr
  );
  const [clientError, setClientError] = useState(null);

  // Load root questionnaires on mount
  useEffect(() => {
    let isMounted = true;
    async function loadQuestionnaires() {
      setLoadingQuestionnaires(true);
      try {
        const data = await getScreeningQuestionnaires();
        if (isMounted) {
          const list = data || [];
          setQuestionnaires(list);
          if (list.length > 0) {
            setSelectedQuestionnaireId(String(list[0].id));
          }
        }
      } catch (err) {
        if (isMounted) {
          setClientError(err.message || 'Failed to load questionnaire catalogue.');
        }
      } finally {
        if (isMounted) {
          setLoadingQuestionnaires(false);
        }
      }
    }
    loadQuestionnaires();
    return () => {
      isMounted = false;
    };
  }, []);

  // When questionnaire changes, reset version selection and fetch published versions
  useEffect(() => {
    let isMounted = true;
    if (!selectedQuestionnaireId) {
      setVersions([]);
      setSelectedVersionId('');
      return;
    }

    async function loadVersions() {
      setLoadingVersions(true);
      setSelectedVersionId(''); // HARD GATE: Reset version ID when questionnaire changes
      try {
        const data = await getQuestionnaireVersions(selectedQuestionnaireId);
        if (isMounted) {
          // Hard gate: Filter strictly to PUBLISHED versions
          const publishedList = (data || []).filter((v) => v.status === 'PUBLISHED');
          setVersions(publishedList);
          if (publishedList.length > 0) {
            // Sort by versionNumber descending and auto-select latest published version
            const sorted = [...publishedList].sort((a, b) => (b.versionNumber || 0) - (a.versionNumber || 0));
            setSelectedVersionId(String(sorted[0].id));
          }
        }
      } catch (err) {
        if (isMounted) {
          setClientError(err.message || 'Failed to load questionnaire versions.');
        }
      } finally {
        if (isMounted) {
          setLoadingVersions(false);
        }
      }
    }

    loadVersions();
    return () => {
      isMounted = false;
    };
  }, [selectedQuestionnaireId]);

  const handleSubmit = (e) => {
    e.preventDefault();
    setClientError(null);

    if (!selectedVersionId) {
      setClientError('A published questionnaire version must be selected.');
      return;
    }

    if (!effectiveStartDate) {
      setClientError('Effective start date is required.');
      return;
    }

    if (effectiveStartDate < todayStr) {
      setClientError('Questionnaire assignment effective date cannot be in the past.');
      return;
    }

    const payload = {
      questionnaireVersionId: Number(selectedVersionId),
      effectiveStartDate,
    };

    onSubmit(payload);
  };

  const isReassignment = Boolean(currentAssignment);

  return (
    <div
      className="modal fade show d-block"
      tabIndex="-1"
      role="dialog"
      style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)', zIndex: 1060 }}
    >
      <div className="modal-dialog modal-dialog-centered" role="document">
        <div className="modal-content shadow">
          <form onSubmit={handleSubmit}>
            <div className="modal-header bg-light">
              <h5 className="modal-title fw-bold">
                {isReassignment ? 'Update Questionnaire Assignment' : 'Assign Screening Questionnaire'}
              </h5>
              <button
                type="button"
                className="btn-close"
                aria-label="Close"
                onClick={onCancel}
                disabled={loading}
              />
            </div>

            <div className="modal-body p-4">
              <ApiErrorAlert error={clientError || error} onDismiss={() => setClientError(null)} />

              {isReassignment && (
                <div className="alert alert-info py-2 small mb-3">
                  Creating a new assignment will supersede the current active questionnaire version on the specified effective start date.
                </div>
              )}

              <div className="row g-3">
                {/* Questionnaire Selector */}
                <div className="col-12">
                  <label htmlFor="questionnaireSelect" className="form-label fw-semibold text-secondary small">
                    Screening Questionnaire <span className="text-danger">*</span>
                  </label>
                  {loadingQuestionnaires ? (
                    <div className="text-muted small py-2">
                      <span className="spinner-border spinner-border-sm me-2" role="status" />
                      Loading questionnaire catalogue...
                    </div>
                  ) : questionnaires.length === 0 ? (
                    <div className="alert alert-warning py-2 small mb-0">
                      No questionnaires available in the screening catalogue.
                    </div>
                  ) : (
                    <select
                      id="questionnaireSelect"
                      className="form-select form-select-sm"
                      value={selectedQuestionnaireId}
                      onChange={(e) => setSelectedQuestionnaireId(e.target.value)}
                      disabled={loading || loadingQuestionnaires}
                      required
                    >
                      {questionnaires.map((q) => (
                        <option key={q.id} value={q.id}>
                          {q.name} ({q.code})
                        </option>
                      ))}
                    </select>
                  )}
                </div>

                {/* Published Version Selector */}
                <div className="col-12">
                  <label htmlFor="versionSelect" className="form-label fw-semibold text-secondary small">
                    Published Version <span className="text-danger">*</span>
                  </label>
                  {loadingVersions ? (
                    <div className="text-muted small py-2">
                      <span className="spinner-border spinner-border-sm me-2" role="status" />
                      Loading versions...
                    </div>
                  ) : versions.length === 0 ? (
                    <div className="alert alert-warning py-2 small mb-0">
                      No published versions are available for this questionnaire.
                    </div>
                  ) : (
                    <select
                      id="versionSelect"
                      className="form-select form-select-sm"
                      value={selectedVersionId}
                      onChange={(e) => setSelectedVersionId(e.target.value)}
                      disabled={loading || loadingVersions}
                      required
                    >
                      {versions.map((v) => (
                        <option key={v.id} value={v.id}>
                          v{v.versionNumber} — Published {formatDate(v.publishedAt)}
                        </option>
                      ))}
                    </select>
                  )}
                  <div className="form-text small" style={{ fontSize: '0.75rem' }}>
                    Only versions in PUBLISHED status can be assigned to campaigns.
                  </div>
                </div>

                {/* Effective Start Date */}
                <div className="col-12">
                  <label htmlFor="effectiveDateInput" className="form-label fw-semibold text-secondary small">
                    Effective Start Date <span className="text-danger">*</span>
                  </label>
                  <input
                    id="effectiveDateInput"
                    type="date"
                    min={todayStr}
                    className="form-control form-control-sm"
                    value={effectiveStartDate}
                    onChange={(e) => setEffectiveStartDate(e.target.value)}
                    disabled={loading}
                    required
                  />
                  <div className="form-text small" style={{ fontSize: '0.75rem' }}>
                    Date when this screening questionnaire becomes clinically effective for citizen booking.
                  </div>
                </div>
              </div>
            </div>

            <div className="modal-footer bg-light">
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={onCancel}
                disabled={loading}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary btn-sm fw-semibold"
                disabled={loading || loadingQuestionnaires || loadingVersions || !selectedVersionId}
              >
                {loading ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" role="status" />
                    Saving Assignment...
                  </>
                ) : isReassignment ? (
                  'Update Assignment'
                ) : (
                  'Assign Questionnaire'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
