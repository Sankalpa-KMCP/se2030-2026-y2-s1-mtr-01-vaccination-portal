import React, { useEffect, useMemo, useState } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import PageHeader from '../../../components/common/PageHeader';
import SuccessAlert from '../../../components/common/SuccessAlert';
import { questionnaireVersionStatusVariants } from '../../../components/common/statusVariants';
import StatusBadge from '../../../components/common/StatusBadge';
import {
  addQuestionnaireItem,
  createEligibilityRule,
  createQuestionnaire,
  createQuestionnaireVersion,
  deleteEligibilityRule,
  deleteQuestionnaireVersion,
  getQuestionnaire,
  getQuestionnaireVersion,
  listEligibilityRules,
  listQuestionnaires,
  listQuestionnaireVersions,
  listQuestions,
  publishQuestionnaireVersion,
  retireQuestionnaireVersion,
  updateEligibilityRule,
} from '../api/screeningApi';
import AddQuestionnaireItemModal from '../components/AddQuestionnaireItemModal';
import EligibilityRuleFormModal from '../components/EligibilityRuleFormModal';
import QuestionnaireActionModal from '../components/QuestionnaireActionModal';
import QuestionnaireFormModal from '../components/QuestionnaireFormModal';
import {
  PRELIMINARY_SCREENING_RESULT_LABELS,
  QUESTIONNAIRE_VERSION_STATUS,
  QUESTIONNAIRE_VERSION_STATUS_LABELS,
  QUESTION_TYPE_LABELS,
} from '../constants/screeningConstants';

const STATUS_BADGE = 'badge';

function sortItems(items = []) {
  return [...items].sort((a, b) => a.displayOrder - b.displayOrder);
}

function sortRules(rules = []) {
  return [...rules].sort((a, b) => a.priority - b.priority);
}

export default function ScreeningQuestionnairesPage() {
  const [questionnaires, setQuestionnaires] = useState([]);
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [selectedQuestionnaire, setSelectedQuestionnaire] = useState(null);
  const [versions, setVersions] = useState([]);
  const [versionsLoading, setVersionsLoading] = useState(false);
  const [selectedVersion, setSelectedVersion] = useState(null);
  const [versionLoading, setVersionLoading] = useState(false);
  const [builderError, setBuilderError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  const [questionnaireFormOpen, setQuestionnaireFormOpen] = useState(false);
  const [itemFormOpen, setItemFormOpen] = useState(false);
  const [ruleFormTarget, setRuleFormTarget] = useState(undefined);
  const [formError, setFormError] = useState(null);
  const [processing, setProcessing] = useState(false);
  const [confirmation, setConfirmation] = useState(null);

  useEffect(() => {
    let active = true;
    Promise.all([listQuestionnaires(), listQuestions({ activeOnly: true })])
      .then(([questionnaireData, questionData]) => {
        if (!active) return;
        setQuestionnaires(Array.isArray(questionnaireData) ? questionnaireData : []);
        setQuestions(Array.isArray(questionData) ? questionData : []);
      })
      .catch((error) => {
        if (active) setLoadError(error);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const questionById = useMemo(
    () => new Map(questions.map((question) => [question.id, question])),
    [questions],
  );

  const attachedQuestionIds = useMemo(
    () => new Set((selectedVersion?.items || []).map((item) => item.questionId)),
    [selectedVersion],
  );

  const availableQuestions = useMemo(
    () => questions.filter((question) => !attachedQuestionIds.has(question.id)),
    [attachedQuestionIds, questions],
  );

  const replaceVersion = (updated) => {
    setVersions((current) => current.map((version) => version.id === updated.id ? updated : version));
    setSelectedVersion(updated);
  };

  const loadVersion = async (versionId) => {
    setVersionLoading(true);
    setBuilderError(null);
    try {
      const [version, rules] = await Promise.all([
        getQuestionnaireVersion(versionId),
        listEligibilityRules(versionId),
      ]);
      const hydratedVersion = { ...version, rules: Array.isArray(rules) ? rules : [] };
      replaceVersion(hydratedVersion);
    } catch (error) {
      setBuilderError(error);
    } finally {
      setVersionLoading(false);
    }
  };

  const selectQuestionnaire = async (questionnaire) => {
    setSelectedQuestionnaire(questionnaire);
    setSelectedVersion(null);
    setVersions([]);
    setVersionsLoading(true);
    setBuilderError(null);
    setSuccessMessage(null);
    try {
      const [details, data] = await Promise.all([
        getQuestionnaire(questionnaire.id),
        listQuestionnaireVersions(questionnaire.id),
      ]);
      setSelectedQuestionnaire(details);
      const loadedVersions = Array.isArray(data) ? data : [];
      setVersions(loadedVersions);
      if (loadedVersions.length > 0) await loadVersion(loadedVersions[0].id);
    } catch (error) {
      setBuilderError(error);
    } finally {
      setVersionsLoading(false);
    }
  };

  const handleCreateQuestionnaire = async (payload) => {
    setProcessing(true);
    setFormError(null);
    try {
      const created = await createQuestionnaire(payload);
      setQuestionnaires((current) => [...current, created]);
      setQuestionnaireFormOpen(false);
      setSelectedQuestionnaire(created);
      setVersions([]);
      setSelectedVersion(null);
      setSuccessMessage('Questionnaire created. Create its first draft version to begin building.');
    } catch (error) {
      setFormError(error);
    } finally {
      setProcessing(false);
    }
  };

  const handleCreateVersion = async () => {
    if (!selectedQuestionnaire) return;
    setProcessing(true);
    setBuilderError(null);
    try {
      const created = await createQuestionnaireVersion(selectedQuestionnaire.id);
      setVersions((current) => [created, ...current]);
      setSelectedVersion(created);
      setSuccessMessage(`Draft version ${created.versionNumber} created.`);
    } catch (error) {
      setBuilderError(error);
    } finally {
      setProcessing(false);
    }
  };

  const handleAddItem = async (payload) => {
    setProcessing(true);
    setFormError(null);
    try {
      const created = await addQuestionnaireItem(selectedVersion.id, payload);
      const updated = {
        ...selectedVersion,
        items: sortItems([...(selectedVersion.items || []), created]),
      };
      replaceVersion(updated);
      setItemFormOpen(false);
      setSuccessMessage('Question added to the draft version.');
    } catch (error) {
      setFormError(error);
    } finally {
      setProcessing(false);
    }
  };

  const handleSaveRule = async (payload) => {
    const editing = Boolean(ruleFormTarget);
    setProcessing(true);
    setFormError(null);
    try {
      const saved = editing
        ? await updateEligibilityRule(ruleFormTarget.id, payload)
        : await createEligibilityRule(selectedVersion.id, payload);
      const updatedRules = editing
        ? (selectedVersion.rules || []).map((rule) => rule.id === saved.id ? saved : rule)
        : [...(selectedVersion.rules || []), saved];
      replaceVersion({ ...selectedVersion, rules: sortRules(updatedRules) });
      setRuleFormTarget(undefined);
      setSuccessMessage(editing ? 'Eligibility rule updated.' : 'Eligibility rule created.');
    } catch (error) {
      setFormError(error);
    } finally {
      setProcessing(false);
    }
  };

  const openConfirmation = (kind, target = selectedVersion) => {
    const actions = {
      publish: {
        kind,
        target,
        title: 'Publish Questionnaire Version',
        message: `Publish version ${target.versionNumber}? Published versions are immutable and may be assigned to campaigns.`,
        confirmLabel: 'Publish Version',
      },
      retire: {
        kind,
        target,
        title: 'Retire Questionnaire Version',
        message: `Retire published version ${target.versionNumber}?`,
        confirmLabel: 'Retire Version',
      },
      'delete-version': {
        kind,
        target,
        title: 'Delete Draft Version',
        message: `Permanently delete draft version ${target.versionNumber}? This is only supported for unused draft versions.`,
        confirmLabel: 'Delete Draft',
        danger: true,
      },
      'delete-rule': {
        kind,
        target,
        title: 'Delete Eligibility Rule',
        message: `Delete eligibility rule ${target.ruleCode}?`,
        confirmLabel: 'Delete Rule',
        danger: true,
      },
    };
    setFormError(null);
    setConfirmation(actions[kind]);
  };

  const handleConfirmedAction = async () => {
    if (!confirmation) return;
    setProcessing(true);
    setFormError(null);
    try {
      if (confirmation.kind === 'publish') {
        replaceVersion(await publishQuestionnaireVersion(confirmation.target.id));
        setSuccessMessage('Questionnaire version published. Its contents are now immutable.');
      } else if (confirmation.kind === 'retire') {
        replaceVersion(await retireQuestionnaireVersion(confirmation.target.id));
        setSuccessMessage('Questionnaire version retired.');
      } else if (confirmation.kind === 'delete-version') {
        await deleteQuestionnaireVersion(confirmation.target.id);
        setVersions((current) => current.filter((version) => version.id !== confirmation.target.id));
        setSelectedVersion(null);
        setSuccessMessage('Draft questionnaire version deleted.');
      } else if (confirmation.kind === 'delete-rule') {
        await deleteEligibilityRule(confirmation.target.id);
        replaceVersion({
          ...selectedVersion,
          rules: (selectedVersion.rules || []).filter((rule) => rule.id !== confirmation.target.id),
        });
        setSuccessMessage('Eligibility rule deleted.');
      }
      setConfirmation(null);
    } catch (error) {
      setFormError(error);
    } finally {
      setProcessing(false);
    }
  };

  const isDraft = selectedVersion?.status === QUESTIONNAIRE_VERSION_STATUS.DRAFT;
  const sortedVersionItems = sortItems(selectedVersion?.items);
  const sortedVersionRules = sortRules(selectedVersion?.rules);
  const suggestedOrder = sortedVersionItems.length === 0
    ? 1
    : Math.max(...sortedVersionItems.map((item) => item.displayOrder)) + 1;

  return (
    <section aria-labelledby="screeningQuestionnairesTitle">
      <PageHeader
        id="screeningQuestionnairesTitle"
        title="Screening Questionnaires"
        subtitle="Build versioned screening questionnaires and their eligibility rules."
        actions={(
          <button type="button" className="btn btn-primary btn-sm fw-semibold" onClick={() => { setFormError(null); setQuestionnaireFormOpen(true); }}>Create Questionnaire</button>
        )}
      />

      {successMessage && (
        <SuccessAlert message={successMessage} onDismiss={() => setSuccessMessage(null)} />
      )}
      <ApiErrorAlert error={loadError} onDismiss={() => setLoadError(null)} />

      {loading ? (
        <LoadingSpinner message="Loading screening questionnaires..." />
      ) : questionnaires.length === 0 ? (
        <div className="card border-0 shadow-sm">
          <div className="card-body text-center py-5">
            <h2 className="h5">No screening questionnaires have been configured yet.</h2>
            <p className="text-muted mb-3">Create a questionnaire container, then add a draft version.</p>
            <button type="button" className="btn btn-primary" onClick={() => setQuestionnaireFormOpen(true)}>Create First Questionnaire</button>
          </div>
        </div>
      ) : (
        <div className="row g-4">
          <div className="col-lg-4">
            <div className="card border-0 shadow-sm">
              <div className="card-header bg-white py-3">
                <h2 className="h5 mb-0">Questionnaires</h2>
              </div>
              <div className="list-group list-group-flush">
                {questionnaires.map((questionnaire) => (
                  <button
                    key={questionnaire.id}
                    type="button"
                    className={`list-group-item list-group-item-action p-3 ${selectedQuestionnaire?.id === questionnaire.id ? 'active' : ''}`}
                    aria-label={`Open ${questionnaire.code}`}
                    onClick={() => selectQuestionnaire(questionnaire)}
                  >
                    <span className="d-block fw-semibold">{questionnaire.name}</span>
                    <span className={`small font-monospace ${selectedQuestionnaire?.id === questionnaire.id ? 'text-white-50' : 'text-muted'}`}>{questionnaire.code}</span>
                    {questionnaire.description && <span className={`d-block small mt-1 ${selectedQuestionnaire?.id === questionnaire.id ? 'text-white-50' : 'text-muted'}`}>{questionnaire.description}</span>}
                  </button>
                ))}
              </div>
            </div>
          </div>

          <div className="col-lg-8">
            {!selectedQuestionnaire ? (
              <div className="card border-0 shadow-sm">
                <div className="card-body text-center py-5 text-muted">Select a questionnaire to view and manage its versions.</div>
              </div>
            ) : (
              <>
                <div className="card border-0 shadow-sm mb-4">
                  <div className="card-header bg-white py-3 d-flex flex-wrap justify-content-between align-items-center gap-2">
                    <div>
                      <h2 className="h5 mb-1">{selectedQuestionnaire.name}</h2>
                      <span className="small text-muted font-monospace">{selectedQuestionnaire.code}</span>
                    </div>
                    <button type="button" className="btn btn-outline-primary btn-sm" onClick={handleCreateVersion} disabled={processing}>Create Draft Version</button>
                  </div>
                  <div className="card-body">
                    <ApiErrorAlert error={builderError} onDismiss={() => setBuilderError(null)} />
                    {versionsLoading ? (
                      <LoadingSpinner message="Loading questionnaire versions..." />
                    ) : versions.length === 0 ? (
                      <div className="text-center py-3 text-muted">No versions exist for this questionnaire yet.</div>
                    ) : (
                      <div className="d-flex flex-wrap gap-2" aria-label="Questionnaire versions">
                        {versions.map((version) => (
                          <button key={version.id} type="button" className={`btn btn-sm ${selectedVersion?.id === version.id ? 'btn-primary' : 'btn-outline-secondary'}`} aria-label={`Open version ${version.versionNumber}`} onClick={() => loadVersion(version.id)}>
                            Version {version.versionNumber} · {QUESTIONNAIRE_VERSION_STATUS_LABELS[version.status] || version.status}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                </div>

                {versionLoading ? (
                  <LoadingSpinner message="Loading questionnaire builder..." />
                ) : selectedVersion && (
                  <div className="card border-0 shadow-sm">
                    <div className="card-header bg-white py-3 d-flex flex-wrap justify-content-between align-items-center gap-2">
                      <div className="d-flex align-items-center gap-2">
                        <h2 className="h5 mb-0">Version {selectedVersion.versionNumber}</h2>
                        <StatusBadge
                          status={QUESTIONNAIRE_VERSION_STATUS_LABELS[selectedVersion.status] || selectedVersion.status}
                          variants={questionnaireVersionStatusVariants}
                        />
                      </div>
                      <div className="d-flex flex-wrap gap-2">
                        {isDraft && <button type="button" className="btn btn-success btn-sm" onClick={() => openConfirmation('publish')}>Publish</button>}
                        {selectedVersion.status === QUESTIONNAIRE_VERSION_STATUS.PUBLISHED && <button type="button" className="btn btn-outline-secondary btn-sm" onClick={() => openConfirmation('retire')}>Retire</button>}
                        {isDraft && <button type="button" className="btn btn-outline-danger btn-sm" onClick={() => openConfirmation('delete-version')}>Delete Draft</button>}
                      </div>
                    </div>
                    <div className="card-body p-4">
                      {!isDraft && (
                        <div className="alert alert-info">
                          This {QUESTIONNAIRE_VERSION_STATUS_LABELS[selectedVersion.status]?.toLowerCase()} version is immutable. Create a new draft version to change questions or eligibility rules.
                        </div>
                      )}

                      <div className="d-flex justify-content-between align-items-center gap-2 mb-3">
                        <h3 className="h5 mb-0">Questionnaire Items</h3>
                        {isDraft && <button type="button" className="btn btn-outline-primary btn-sm" onClick={() => { setFormError(null); setItemFormOpen(true); }}>Add Question</button>}
                      </div>
                      {sortedVersionItems.length === 0 ? (
                        <div className="border rounded bg-light text-center text-muted py-4 mb-4">No questions are attached to this version.</div>
                      ) : (
                        <div className="table-responsive mb-4">
                          <table className="table table-sm table-hover align-middle mb-0">
                            <thead className="table-light"><tr><th scope="col">Order</th><th scope="col">Code</th><th scope="col">Question</th><th scope="col">Type</th><th scope="col">Required</th></tr></thead>
                            <tbody>
                              {sortedVersionItems.map((item) => {
                                const question = questionById.get(item.questionId);
                                const required = item.requiredOverride ?? question?.defaultRequired;
                                return (
                                  <tr key={item.id}>
                                    <td>{item.displayOrder}</td>
                                    <td className="font-monospace fw-semibold">{question?.code || `#${item.questionId}`}</td>
                                    <td>{question?.text || 'Question details unavailable'}</td>
                                    <td>{QUESTION_TYPE_LABELS[question?.questionType] || question?.questionType || '—'}</td>
                                    <td>{required ? 'Required' : 'Optional'}{item.requiredOverride == null && <span className="small text-muted"> (default)</span>}</td>
                                  </tr>
                                );
                              })}
                            </tbody>
                          </table>
                          <div className="small text-muted mt-2">Item order is persisted when each item is added. This backend has no edit, remove, or reorder endpoint.</div>
                        </div>
                      )}

                      <hr className="my-4" />
                      <div className="d-flex justify-content-between align-items-center gap-2 mb-3">
                        <h3 className="h5 mb-0">Eligibility Rules</h3>
                        {isDraft && <button type="button" className="btn btn-outline-primary btn-sm" onClick={() => { setFormError(null); setRuleFormTarget(null); }}>Create Rule</button>}
                      </div>
                      {sortedVersionRules.length === 0 ? (
                        <div className="border rounded bg-light text-center text-muted py-4">No eligibility rules are configured for this version.</div>
                      ) : (
                        <div className="table-responsive">
                          <table className="table table-sm table-hover align-middle mb-0">
                            <thead className="table-light"><tr><th scope="col">Priority</th><th scope="col">Rule</th><th scope="col">Condition</th><th scope="col">Outcome</th><th scope="col">Message</th>{isDraft && <th scope="col" className="text-end">Actions</th>}</tr></thead>
                            <tbody>
                              {sortedVersionRules.map((rule) => (
                                <tr key={rule.id}>
                                  <td>{rule.priority}</td>
                                  <td><div className="fw-semibold">{rule.ruleName}</div><div className="small text-muted font-monospace">{rule.ruleCode}</div></td>
                                  <td><code>{rule.conditionExpression}</code></td>
                                  <td>{PRELIMINARY_SCREENING_RESULT_LABELS[rule.preliminaryResult] || rule.preliminaryResult}</td>
                                  <td>{rule.message}</td>
                                  {isDraft && (
                                    <td className="text-end">
                                      <div className="d-inline-flex gap-1">
                                        <button type="button" className="btn btn-outline-primary btn-sm" aria-label={`Edit rule ${rule.ruleCode}`} onClick={() => { setFormError(null); setRuleFormTarget(rule); }}>Edit</button>
                                        <button type="button" className="btn btn-outline-danger btn-sm" aria-label={`Delete rule ${rule.ruleCode}`} onClick={() => openConfirmation('delete-rule', rule)}>Delete</button>
                                      </div>
                                    </td>
                                  )}
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      )}

      <QuestionnaireFormModal show={questionnaireFormOpen} processing={processing} apiError={formError} onClose={() => !processing && setQuestionnaireFormOpen(false)} onSubmit={handleCreateQuestionnaire} />
      <AddQuestionnaireItemModal show={itemFormOpen} questions={availableQuestions} suggestedOrder={suggestedOrder} processing={processing} apiError={formError} onClose={() => !processing && setItemFormOpen(false)} onSubmit={handleAddItem} />
      <EligibilityRuleFormModal show={ruleFormTarget !== undefined} rule={ruleFormTarget || null} processing={processing} apiError={formError} onClose={() => !processing && setRuleFormTarget(undefined)} onSubmit={handleSaveRule} />
      <QuestionnaireActionModal action={confirmation} processing={processing} error={formError} onClose={() => !processing && setConfirmation(null)} onConfirm={handleConfirmedAction} />
    </section>
  );
}
