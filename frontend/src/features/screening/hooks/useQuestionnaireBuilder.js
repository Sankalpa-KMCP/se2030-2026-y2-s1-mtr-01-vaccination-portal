import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { addQuestionnaireItem, createAnswerOption, createEligibilityRule, createQuestion, createQuestionnaire, createQuestionnaireVersion, deleteEligibilityRule, deleteQuestionnaire, deleteQuestionnaireVersion, getQuestionnaire, getQuestionnaireVersion, listAnswerOptions, listEligibilityRules, listQuestionnaires, listQuestionnaireVersions, listQuestions, publishQuestionnaireVersion, removeQuestionnaireItem, reorderQuestionnaireItems, retireQuestionnaireVersion, updateEligibilityRule, updateQuestionnaireItem } from "../api/screeningApi";
import { listCampaigns } from "../../campaign-management/api/campaignApi";
import { assignQuestionnaire, getPublishedQuestionnaireVersions } from "../../campaign-management/api/campaignConfigurationApi";
import { useAuth } from "../../../auth/context/AuthContext";
import { canManageQuestionnaireAssignments } from "../../../auth/utils/roleUtils";
import { parseEligibilityRuleExpression } from "../utils/eligibilityRuleExpression";
import { QUESTIONNAIRE_VERSION_STATUS, isChoiceQuestionType } from "../constants/screeningConstants";

function sortItems(items = []) {
  return [...items].sort((a, b) => a.displayOrder - b.displayOrder);
}

function sortRules(rules = []) {
  return [...rules].sort((a, b) => a.priority - b.priority);
}

export default function useQuestionnaireBuilder() {
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
      'delete-questionnaire': {
        kind,
        target,
        title: 'Delete Questionnaire?',
        message: `Are you sure you want to permanently delete '${target.name}'? This action cannot be undone.`,
        confirmLabel: 'Delete Questionnaire',
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
      } else if (confirmation.kind === 'delete-questionnaire') {
        await deleteQuestionnaire(confirmation.target.id);
        setQuestionnaires((current) => current.filter((questionnaire) => questionnaire.id !== confirmation.target.id));
        setSelectedQuestionnaire(null);
        setVersions([]);
        setSelectedVersion(null);
        setSuccessMessage('Questionnaire deleted successfully.');
        try {
          const refreshed = await listQuestionnaires();
          setQuestionnaires(Array.isArray(refreshed) ? refreshed : []);
        } catch (refreshError) {
          setLoadError(refreshError);
        }
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
      if (confirmation.kind === 'delete-questionnaire' && error?.status === 409) {
        await selectQuestionnaire(confirmation.target);
        setConfirmation(null);
        setFormError(null);
        setBuilderError(error);
      }
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


  return {
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
  };
}
