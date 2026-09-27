import React, { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import PageHeader from '../../../components/common/PageHeader';
import SuccessAlert from '../../../components/common/SuccessAlert';
import {
  campaignStatusVariants,
  questionnaireVersionStatusVariants,
} from '../../../components/common/statusVariants';
import StatusBadge from '../../../components/common/StatusBadge';
import {
  addQuestionnaireItem,
  createAnswerOption,
  createEligibilityRule,
  createQuestion,
  createQuestionnaire,
  createQuestionnaireVersion,
  deleteEligibilityRule,
  deleteQuestionnaireVersion,
  getQuestionnaire,
  getQuestionnaireVersion,
  listAnswerOptions,
  listEligibilityRules,
  listQuestionnaires,
  listQuestionnaireVersions,
  listQuestions,
  publishQuestionnaireVersion,
  removeQuestionnaireItem,
  reorderQuestionnaireItems,
  retireQuestionnaireVersion,
  updateEligibilityRule,
  updateQuestionnaireItem,
} from '../api/screeningApi';
import AddQuestionnaireItemModal from '../components/AddQuestionnaireItemModal';
import ChangeQuestionnaireItemModal from '../components/ChangeQuestionnaireItemModal';
import EligibilityRuleFormModal from '../components/EligibilityRuleFormModal';
import QuestionnaireActionModal from '../components/QuestionnaireActionModal';
import QuestionnaireFormModal from '../components/QuestionnaireFormModal';
import { listCampaigns } from '../../campaign-management/api/campaignApi';
import {
  assignQuestionnaire,
  getPublishedQuestionnaireVersions,
} from '../../campaign-management/api/campaignConfigurationApi';
import QuestionnaireAssignmentFormModal from '../../campaign-management/components/QuestionnaireAssignmentFormModal';
import { useAuth } from '../../../auth/context/AuthContext';
import { canManageQuestionnaireAssignments } from '../../../auth/utils/roleUtils';
import { parseEligibilityRuleExpression } from '../utils/eligibilityRuleExpression';
import {
  PRELIMINARY_SCREENING_RESULT_LABELS,
  QUESTIONNAIRE_VERSION_STATUS,
  QUESTIONNAIRE_VERSION_STATUS_LABELS,
  QUESTION_TYPE_LABELS,
  isChoiceQuestionType,
} from '../constants/screeningConstants';

const STATUS_BADGE = 'badge';

function sortItems(items = []) {
  return [...items].sort((a, b) => a.displayOrder - b.displayOrder);
}

function sortRules(rules = []) {
  return [...rules].sort((a, b) => a.priority - b.priority);
}

export default function ScreeningQuestionnairesPage() {
  const { user } = useAuth();
  const canManageAssignments = canManageQuestionnaireAssignments(user);

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
  const [changeItemTarget, setChangeItemTarget] = useState(null);
  const [creationProgress, setCreationProgress] = useState(null);
  const [ruleFormTarget, setRuleFormTarget] = useState(undefined);
  const [formError, setFormError] = useState(null);
  const [processing, setProcessing] = useState(false);
  const [reordering, setReordering] = useState(false);
  const [confirmation, setConfirmation] = useState(null);

  // Top-level tab state ('library' | 'campaigns')
  const [searchParams, setSearchParams] = useSearchParams();
  const [activeTab, setActiveTab] = useState(() =>
    searchParams.get('tab') === 'campaigns' ? 'campaigns' : 'library'
  );

  useEffect(() => {
    const tabParam = searchParams.get('tab');
    if (tabParam === 'campaigns' && activeTab !== 'campaigns') {
      setActiveTab('campaigns');
    } else if (tabParam === 'library' && activeTab !== 'library') {
      setActiveTab('library');
    }
  }, [searchParams, activeTab]);

  const handleSelectTab = (tab) => {
    setActiveTab(tab);
    setSearchParams(tab === 'campaigns' ? { tab: 'campaigns' } : {});
  };
  const [campaigns, setCampaigns] = useState([]);
  const [publishedVersions, setPublishedVersions] = useState([]);
  const [campaignsLoading, setCampaignsLoading] = useState(false);
  const [campaignsError, setCampaignsError] = useState(null);
  const [selectedCampaignForAssignment, setSelectedCampaignForAssignment] = useState(null);
  const [assignmentLoading, setAssignmentLoading] = useState(false);
  const [assignmentError, setAssignmentError] = useState(null);

  const fetchCampaignAssignments = async () => {
    setCampaignsLoading(true);
    setCampaignsError(null);
    try {
      const [campaignList, pubVers] = await Promise.all([
        listCampaigns(),
        getPublishedQuestionnaireVersions(),
      ]);
      setCampaigns(Array.isArray(campaignList) ? campaignList : []);
      setPublishedVersions(Array.isArray(pubVers) ? pubVers : []);
    } catch (err) {
      setCampaignsError(err);
    } finally {
      setCampaignsLoading(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'campaigns') {
      fetchCampaignAssignments();
    }
  }, [activeTab]);

  const handleAssignSubmit = async (payload) => {
    if (!selectedCampaignForAssignment || !canManageAssignments) return;
    setAssignmentLoading(true);
    setAssignmentError(null);
    try {
      await assignQuestionnaire(selectedCampaignForAssignment.id, payload);
      setSelectedCampaignForAssignment(null);
      setSuccessMessage('Screening questionnaire assigned successfully.');
      await fetchCampaignAssignments();
    } catch (err) {
      setAssignmentError(err);
    } finally {
      setAssignmentLoading(false);
    }
  };

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

  const attachedQuestions = useMemo(
    () => (selectedVersion?.items || [])
      .map((item) => questionById.get(item.questionId))
      .filter(Boolean),
    [questionById, selectedVersion],
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

  const handleReplaceItem = async (payload) => {
    if (!selectedVersion || !changeItemTarget) return;
    setProcessing(true);
    setFormError(null);
    try {
      await updateQuestionnaireItem(selectedVersion.id, changeItemTarget.id, payload);
      await loadVersion(selectedVersion.id);
      setChangeItemTarget(null);
      setSuccessMessage('Question updated in draft version.');
    } catch (error) {
      setFormError(error);
    } finally {
      setProcessing(false);
    }
  };

  const handleMoveItem = async (currentIndex, targetIndex) => {
    if (!selectedVersion || !isDraft || reordering || processing) return;
    const items = sortItems(selectedVersion.items || []);
    if (targetIndex < 0 || targetIndex >= items.length) return;

    const itemIds = items.map((i) => i.id);
    const temp = itemIds[currentIndex];
    itemIds[currentIndex] = itemIds[targetIndex];
    itemIds[targetIndex] = temp;

    setReordering(true);
    setBuilderError(null);
    try {
      const updatedItems = await reorderQuestionnaireItems(selectedVersion.id, itemIds);
      replaceVersion({
        ...selectedVersion,
        items: sortItems(updatedItems),
      });
      setSuccessMessage('Questionnaire item order updated.');
    } catch (error) {
      setBuilderError(error);
    } finally {
      setReordering(false);
    }
  };

  const handleInitiateRemoveItem = (item) => {
    if (!selectedVersion || !isDraft) return;
    const question = questionById.get(item.questionId);
    const targetCode = question?.code?.trim().toUpperCase();

    const referencingRule = (selectedVersion.rules || []).find((rule) => {
      const parsed = parseEligibilityRuleExpression(rule.conditionExpression);
      return parsed?.questionCode?.toUpperCase() === targetCode;
    });

    if (referencingRule) {
      setBuilderError(new Error(
        `This question is currently used by an eligibility rule ('${referencingRule.ruleName}'). Update or remove that rule before removing the question.`
      ));
      return;
    }

    setFormError(null);
    setConfirmation({
      kind: 'remove-item',
      target: item,
      title: 'Remove question?',
      message: `Are you sure you want to remove '${question?.text || question?.code || 'this question'}' from Version ${selectedVersion.versionNumber}?`,
      confirmLabel: 'Remove',
      danger: true,
    });
  };

  const finishCreateAndAttach = async (question, attachedItem = null) => {
    try {
      const [version, rules, catalogue] = await Promise.all([
        getQuestionnaireVersion(selectedVersion.id),
        listEligibilityRules(selectedVersion.id),
        listQuestions({ activeOnly: true }),
      ]);
      replaceVersion({ ...version, rules: Array.isArray(rules) ? rules : [] });
      setQuestions(Array.isArray(catalogue) ? catalogue : []);
    } catch (error) {
      if (attachedItem) {
        replaceVersion({
          ...selectedVersion,
          items: sortItems([...(selectedVersion.items || []), attachedItem]),
        });
        setQuestions((current) => current.some((item) => item.id === question.id)
          ? current
          : [...current, question]);
      }
      setBuilderError(error);
    }
    setItemFormOpen(false);
    setCreationProgress(null);
    setSuccessMessage('Question created and added to this questionnaire version.');
  };

  const handleCreateAndAttach = async ({ question: questionPayload, options, attachment }) => {
    setProcessing(true);
    setFormError(null);
    let createdQuestion = creationProgress?.question || null;
    let savedOptions = creationProgress?.options || [];

    try {
      if (!createdQuestion) {
        createdQuestion = await createQuestion(questionPayload);
        setCreationProgress({ question: createdQuestion, options: [] });
      } else {
        const currentVersion = await getQuestionnaireVersion(selectedVersion.id);
        const existingItem = (currentVersion.items || []).find((item) => item.questionId === createdQuestion.id);
        if (existingItem) {
          await finishCreateAndAttach(createdQuestion);
          return;
        }
      }

      if (isChoiceQuestionType(createdQuestion.questionType)) {
        try {
          savedOptions = await listAnswerOptions(createdQuestion.id);
        } catch (error) {
          setFormError(`Question was created, but its current answer options could not be loaded safely. It now exists in the global catalogue. ${error.message}`);
          return;
        }
        setCreationProgress({ question: createdQuestion, options: savedOptions });

        for (const optionPayload of options) {
          const existing = savedOptions.find(
            (option) => option.code.trim().toLocaleLowerCase() === optionPayload.code.trim().toLocaleLowerCase(),
          );
          if (existing) {
            const matches = existing.label === optionPayload.label
              && existing.typedValue === optionPayload.typedValue
              && existing.displayOrder === optionPayload.displayOrder;
            if (!matches) {
              setFormError(`Question was created, but answer option ${optionPayload.code} already exists with different values. Review it in the global Question Catalogue.`);
              return;
            }
            continue;
          }

          try {
            const saved = await createAnswerOption(createdQuestion.id, optionPayload);
            savedOptions = [...savedOptions, saved];
            setCreationProgress({ question: createdQuestion, options: savedOptions });
          } catch (error) {
            try {
              const reconciledOptions = await listAnswerOptions(createdQuestion.id);
              savedOptions = Array.isArray(reconciledOptions) ? reconciledOptions : savedOptions;
            } catch {
              // Keep the confirmed local progress when the reconciliation request also fails.
            }
            setCreationProgress({ question: createdQuestion, options: savedOptions });
            setFormError(`Question was created, but one or more answer options could not be saved. It now exists in the global catalogue. ${error.message}`);
            return;
          }
        }
      }

      let attachedItem;
      try {
        attachedItem = await addQuestionnaireItem(selectedVersion.id, {
          questionId: createdQuestion.id,
          ...attachment,
        });
      } catch (error) {
        setFormError(`Question was created, but could not be added to this questionnaire version. You can retry or select it later from the existing question catalogue. ${error.message}`);
        return;
      }

      await finishCreateAndAttach(createdQuestion, attachedItem);
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
      } else if (confirmation.kind === 'remove-item') {
        await removeQuestionnaireItem(selectedVersion.id, confirmation.target.id);
        await loadVersion(selectedVersion.id);
        setSuccessMessage('Question removed from draft version.');
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
