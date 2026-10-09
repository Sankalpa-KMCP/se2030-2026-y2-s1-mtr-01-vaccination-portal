import ApiErrorAlert from "../../../components/common/ApiErrorAlert";
import LoadingSpinner from "../../../components/common/LoadingSpinner";
import PageHeader from "../../../components/common/PageHeader";
import SuccessAlert from "../../../components/common/SuccessAlert";
import { campaignStatusVariants, questionnaireVersionStatusVariants } from "../../../components/common/statusVariants";
import StatusBadge from "../../../components/common/StatusBadge";
import AddQuestionnaireItemModal from "../components/AddQuestionnaireItemModal";
import ChangeQuestionnaireItemModal from "../components/ChangeQuestionnaireItemModal";
import EligibilityRuleFormModal from "../components/EligibilityRuleFormModal";
import QuestionnaireActionModal from "../components/QuestionnaireActionModal";
import QuestionnaireFormModal from "../components/QuestionnaireFormModal";
import QuestionnaireAssignmentFormModal from "../../campaign-management/components/QuestionnaireAssignmentFormModal";
import { PRELIMINARY_SCREENING_RESULT_LABELS, QUESTIONNAIRE_VERSION_STATUS, QUESTIONNAIRE_VERSION_STATUS_LABELS, QUESTION_TYPE_LABELS } from "../constants/screeningConstants";
import useQuestionnaireBuilder from "../hooks/useQuestionnaireBuilder";

const STATUS_BADGE = 'badge';

export default function ScreeningQuestionnairesPage() {
  const {
    canManageAssignments,
    questionnaires,
    loading,
    loadError,
    setLoadError,
    selectedQuestionnaire,
    versions,
    versionsLoading,
    selectedVersion,
    versionLoading,
    builderError,
    setBuilderError,
    successMessage,
    setSuccessMessage,
    questionnaireFormOpen,
    setQuestionnaireFormOpen,
    itemFormOpen,
    setItemFormOpen,
    changeItemTarget,
    setChangeItemTarget,
    creationProgress,
    setCreationProgress,
    ruleFormTarget,
    setRuleFormTarget,
    formError,
    setFormError,
    processing,
    reordering,
    confirmation,
    setConfirmation,
    activeTab,
    handleSelectTab,
    campaigns,
    publishedVersions,
    campaignsLoading,
    campaignsError,
    setCampaignsError,
    selectedCampaignForAssignment,
    setSelectedCampaignForAssignment,
    assignmentLoading,
    assignmentError,
    setAssignmentError,
    fetchCampaignAssignments,
    handleAssignSubmit,
    questionById,
    availableQuestions,
    attachedQuestions,
    loadVersion,
    selectQuestionnaire,
    handleCreateQuestionnaire,
    handleCreateVersion,
    handleAddItem,
    handleReplaceItem,
    handleMoveItem,
    handleInitiateRemoveItem,
    handleCreateAndAttach,
    handleSaveRule,
    openConfirmation,
    handleConfirmedAction,
    isDraft,
    sortedVersionItems,
    sortedVersionRules,
    suggestedOrder
  } = useQuestionnaireBuilder();


return (
    <section aria-labelledby="screeningQuestionnairesTitle">
      <PageHeader
        id="screeningQuestionnairesTitle"
        title="Screening Questionnaires"
        subtitle="Build versioned screening questionnaires and assign them to vaccination campaigns."
        actions={activeTab === 'library' ? (
          <button type="button" className="btn btn-primary btn-sm fw-semibold" onClick={() => { setFormError(null); setQuestionnaireFormOpen(true); }}>Create Questionnaire</button>
        ) : null}
      />

      <ul className="nav nav-tabs mb-4" role="tablist">
        <li className="nav-item" role="presentation">
          <button
            type="button"
            className={`nav-link fw-semibold ${activeTab === 'library' ? 'active' : ''}`}
            onClick={() => handleSelectTab('library')}
          >
            Questionnaire Library
          </button>
        </li>
        <li className="nav-item" role="presentation">
          <button
            type="button"
            className={`nav-link fw-semibold ${activeTab === 'campaigns' ? 'active' : ''}`}
            onClick={() => handleSelectTab('campaigns')}
          >
            Campaign Assignments
          </button>
        </li>
      </ul>

      {successMessage && (
        <SuccessAlert message={successMessage} onDismiss={() => setSuccessMessage(null)} />
      )}

      {activeTab === 'library' && (
        <>
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
                    <div>
                      <button
                        type="button"
                        className="btn btn-outline-danger btn-sm"
                        onClick={() => openConfirmation('delete-questionnaire', selectedQuestionnaire)}
                        disabled={processing || versionsLoading || versions.length > 0}
                      >
                        Delete Questionnaire
                      </button>
                      {!versionsLoading && versions.length > 0 && (
                        <div className="small text-muted mt-1">Questionnaires with existing versions cannot be deleted.</div>
                      )}
                    </div>
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
                        {isDraft && <button type="button" className="btn btn-outline-primary btn-sm" onClick={() => { setFormError(null); setCreationProgress(null); setItemFormOpen(true); }}>Add Question</button>}
                      </div>
                      {sortedVersionItems.length === 0 ? (
                        <div className="border rounded bg-light text-center text-muted py-4 mb-4">No questions are attached to this version.</div>
                      ) : (
                        <div className="table-responsive mb-4">
                          <table className="table table-sm table-hover align-middle mb-0">
                            <thead className="table-light">
                              <tr>
                                <th scope="col" style={{ width: isDraft ? '110px' : '70px' }}>Order</th>
                                <th scope="col">Code</th>
                                <th scope="col">Question</th>
                                <th scope="col">Type</th>
                                <th scope="col">Required</th>
                                {isDraft && <th scope="col" className="text-end">Actions</th>}
                              </tr>
                            </thead>
                            <tbody>
                              {sortedVersionItems.map((item, index) => {
                                const question = questionById.get(item.questionId);
                                const required = item.requiredOverride ?? question?.defaultRequired;
                                return (
                                  <tr key={item.id}>
                                    <td>
                                      <div className="d-flex align-items-center gap-1">
                                        <span className="badge bg-secondary-subtle text-secondary-emphasis rounded-pill px-2 py-1 fw-bold">
                                          {item.displayOrder}
                                        </span>
                                        {isDraft && (
                                          <div className="btn-group-vertical btn-group-sm ms-1" role="group" aria-label={`Reorder ${question?.code || item.displayOrder}`}>
                                            <button
                                              type="button"
                                              className="btn btn-outline-secondary py-0 px-1"
                                              title="Move Up"
                                              aria-label={`Move up ${question?.code || item.displayOrder}`}
                                              disabled={index === 0 || reordering || processing}
                                              onClick={() => handleMoveItem(index, index - 1)}
                                            >
                                              ▲
                                            </button>
                                            <button
                                              type="button"
                                              className="btn btn-outline-secondary py-0 px-1"
                                              title="Move Down"
                                              aria-label={`Move down ${question?.code || item.displayOrder}`}
                                              disabled={index === sortedVersionItems.length - 1 || reordering || processing}
                                              onClick={() => handleMoveItem(index, index + 1)}
                                            >
                                              ▼
                                            </button>
                                          </div>
                                        )}
                                      </div>
                                    </td>
                                    <td className="font-monospace fw-semibold">{question?.code || `#${item.questionId}`}</td>
                                    <td>{question?.text || 'Question details unavailable'}</td>
                                    <td>{QUESTION_TYPE_LABELS[question?.questionType] || question?.questionType || '—'}</td>
                                    <td>{required ? 'Required' : 'Optional'}{item.requiredOverride == null && <span className="small text-muted"> (default)</span>}</td>
                                    {isDraft && (
                                      <td className="text-end">
                                        <div className="d-inline-flex gap-1">
                                          <button
                                            type="button"
                                            className="btn btn-outline-primary btn-sm"
                                            aria-label={`Change question ${question?.code || item.displayOrder}`}
                                            disabled={processing || reordering}
                                            onClick={() => {
                                              setFormError(null);
                                              setChangeItemTarget(item);
                                            }}
                                          >
                                            Change
                                          </button>
                                          <button
                                            type="button"
                                            className="btn btn-outline-danger btn-sm"
                                            aria-label={`Remove question ${question?.code || item.displayOrder}`}
                                            disabled={processing || reordering}
                                            onClick={() => handleInitiateRemoveItem(item)}
                                          >
                                            Remove
                                          </button>
                                        </div>
                                      </td>
                                    )}
                                  </tr>
                                );
                              })}
                            </tbody>
                          </table>
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
        </>
      )}

      {activeTab === 'campaigns' && (
        <div className="card border-0 shadow-sm">
          <div className="card-header bg-white py-3 d-flex justify-content-between align-items-center">
            <h2 className="h5 mb-0">Campaign Questionnaire Assignments</h2>
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm"
              onClick={fetchCampaignAssignments}
              disabled={campaignsLoading}
            >
              Refresh
            </button>
          </div>
          <div className="card-body p-0">
            <ApiErrorAlert error={campaignsError} onDismiss={() => setCampaignsError(null)} />

            {campaignsLoading ? (
              <div className="py-5">
                <LoadingSpinner message="Loading campaign assignments..." />
              </div>
            ) : campaigns.length === 0 ? (
              <div className="text-center py-5 text-muted">
                No vaccination campaigns found.
              </div>
            ) : (
              <div className="table-responsive">
                <table className="table table-hover align-middle mb-0">
                  <thead className="table-light">
                    <tr>
                      <th scope="col">Campaign</th>
                      <th scope="col">Vaccine</th>
                      <th scope="col">Target Group</th>
                      <th scope="col">Service Window</th>
                      <th scope="col">Status</th>
                      <th scope="col">Screening Questionnaire</th>
                      <th scope="col" className="text-end">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {campaigns.map((camp) => {
                      const assignedQv = camp.questionnaireVersionId
                        ? publishedVersions.find((v) => v.id === camp.questionnaireVersionId)
                        : null;
                      const isDraftStatus = camp.status === 'DRAFT';

                      return (
                        <tr key={camp.id}>
                          <td>
                            <div className="fw-semibold text-dark">{camp.name}</div>
                            <div className="small font-monospace text-muted">{camp.code}</div>
                          </td>
                          <td>
                            {camp.vaccineTypeName
                              ? `${camp.vaccineTypeName} (${camp.vaccineTypeCode || ''})`
                              : camp.vaccineTypeCode || '—'}
                          </td>
                          <td>{camp.targetGroupCode || '—'}</td>
                          <td className="small">
                            {camp.serviceStartDate} &rarr; {camp.serviceEndDate}
                          </td>
                          <td>
                            <StatusBadge status={camp.status} variants={campaignStatusVariants} />
                          </td>
                          <td>
                            {assignedQv ? (
                              <div>
                                <span className="fw-semibold text-dark">
                                  {assignedQv.questionnaireName}
                                </span>{' '}
                                <span className="text-muted small">
                                  (v{assignedQv.versionNumber})
                                </span>
                              </div>
                            ) : camp.questionnaireVersionId ? (
                              <span className="text-dark small">
                                Version #{camp.questionnaireVersionId} · Published
                              </span>
                            ) : (
                              <span className="text-muted small fst-italic">
                                Not assigned yet
                              </span>
                            )}
                          </td>
                          <td className="text-end">
                            {canManageAssignments && isDraftStatus ? (
                              camp.questionnaireVersionId ? (
                                <button
                                  type="button"
                                  className="btn btn-outline-primary btn-sm"
                                  onClick={() => {
                                    setAssignmentError(null);
                                    setSelectedCampaignForAssignment(camp);
                                  }}
                                >
                                  Change
                                </button>
                              ) : (
                                <button
                                  type="button"
                                  className="btn btn-primary btn-sm"
                                  onClick={() => {
                                    setAssignmentError(null);
                                    setSelectedCampaignForAssignment(camp);
                                  }}
                                >
                                  Assign Questionnaire
                                </button>
                              )
                            ) : !isDraftStatus ? (
                              <span className="small text-muted">
                                Assignment locked ({camp.status})
                              </span>
                            ) : (
                              <span className="small text-muted">—</span>
                            )}
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
      )}

      <QuestionnaireFormModal show={questionnaireFormOpen} processing={processing} apiError={formError} onClose={() => !processing && setQuestionnaireFormOpen(false)} onSubmit={handleCreateQuestionnaire} />
      <AddQuestionnaireItemModal
        show={itemFormOpen}
        questions={availableQuestions}
        suggestedOrder={suggestedOrder}
        processing={processing}
        apiError={formError}
        creationProgress={creationProgress}
        onClose={() => {
          if (!processing) {
            setItemFormOpen(false);
            setCreationProgress(null);
          }
        }}
        onSubmit={handleAddItem}
        onCreateAndAttach={handleCreateAndAttach}
      />
      <EligibilityRuleFormModal show={ruleFormTarget !== undefined} rule={ruleFormTarget || null} questions={attachedQuestions} processing={processing} apiError={formError} onClose={() => !processing && setRuleFormTarget(undefined)} onSubmit={handleSaveRule} />
      <QuestionnaireActionModal action={confirmation} processing={processing} error={formError} onClose={() => !processing && setConfirmation(null)} onConfirm={handleConfirmedAction} />
      <ChangeQuestionnaireItemModal
        show={changeItemTarget !== null}
        item={changeItemTarget}
        currentQuestion={changeItemTarget ? questionById.get(changeItemTarget.questionId) : null}
        availableQuestions={availableQuestions}
        rules={selectedVersion?.rules || []}
        processing={processing}
        apiError={formError}
        onClose={() => !processing && setChangeItemTarget(null)}
        onSubmit={handleReplaceItem}
      />

      {canManageAssignments && selectedCampaignForAssignment && (
        <QuestionnaireAssignmentFormModal
          campaign={selectedCampaignForAssignment}
          currentAssignment={
            selectedCampaignForAssignment.questionnaireVersionId
              ? { questionnaireVersionId: selectedCampaignForAssignment.questionnaireVersionId }
              : null
          }
          onSubmit={handleAssignSubmit}
          onCancel={() => {
            setSelectedCampaignForAssignment(null);
            setAssignmentError(null);
          }}
          loading={assignmentLoading}
          error={assignmentError}
        />
      )}
    </section>
  );
}
