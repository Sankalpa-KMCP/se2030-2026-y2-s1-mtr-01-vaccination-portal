import { apiRequest, parseApiResponse } from '../../../services/apiClient';

const SCREENING_BASE_PATH = '/v1/screening';

async function screeningRequest(endpoint, options = {}) {
  const response = await apiRequest(`${SCREENING_BASE_PATH}${endpoint}`, options);
  return parseApiResponse(response);
}

export function listQuestions({ activeOnly = false } = {}) {
  const query = activeOnly ? '?activeOnly=true' : '';
  return screeningRequest(`/questions${query}`, { method: 'GET' });
}

export function getQuestion(questionId) {
  return screeningRequest(`/questions/${questionId}`, { method: 'GET' });
}

export function createQuestion(payload) {
  return screeningRequest('/questions', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function updateQuestion(questionId, payload) {
  return screeningRequest(`/questions/${questionId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export function deleteQuestion(questionId) {
  return screeningRequest(`/questions/${questionId}`, { method: 'DELETE' });
}

export function listAnswerOptions(questionId) {
  return screeningRequest(`/questions/${questionId}/options`, { method: 'GET' });
}

export function getAnswerOption(optionId) {
  return screeningRequest(`/options/${optionId}`, { method: 'GET' });
}

export function createAnswerOption(questionId, payload) {
  return screeningRequest(`/questions/${questionId}/options`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function updateAnswerOption(optionId, payload) {
  return screeningRequest(`/options/${optionId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export function deleteAnswerOption(optionId) {
  return screeningRequest(`/options/${optionId}`, { method: 'DELETE' });
}

// Questionnaire containers
export function listQuestionnaires() {
  return screeningRequest('/questionnaires', { method: 'GET' });
}

export function getQuestionnaire(questionnaireId) {
  return screeningRequest(`/questionnaires/${questionnaireId}`, { method: 'GET' });
}

export function createQuestionnaire(payload) {
  return screeningRequest('/questionnaires', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

// Questionnaire versions and their item snapshots
export function listQuestionnaireVersions(questionnaireId) {
  return screeningRequest(`/questionnaires/${questionnaireId}/versions`, { method: 'GET' });
}

export function getQuestionnaireVersion(versionId) {
  return screeningRequest(`/questionnaire-versions/${versionId}`, { method: 'GET' });
}

export function createQuestionnaireVersion(questionnaireId) {
  return screeningRequest(`/questionnaires/${questionnaireId}/versions`, { method: 'POST' });
}

export function addQuestionnaireItem(versionId, payload) {
  return screeningRequest(`/questionnaire-versions/${versionId}/items`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function publishQuestionnaireVersion(versionId) {
  return screeningRequest(`/questionnaire-versions/${versionId}/publish`, { method: 'POST' });
}

export function retireQuestionnaireVersion(versionId) {
  return screeningRequest(`/questionnaire-versions/${versionId}/retire`, { method: 'POST' });
}

export function deleteQuestionnaireVersion(versionId) {
  return screeningRequest(`/questionnaire-versions/${versionId}`, { method: 'DELETE' });
}

// Eligibility rules
export function listEligibilityRules(versionId) {
  return screeningRequest(`/questionnaire-versions/${versionId}/rules`, { method: 'GET' });
}

export function getEligibilityRule(ruleId) {
  return screeningRequest(`/rules/${ruleId}`, { method: 'GET' });
}

export function createEligibilityRule(versionId, payload) {
  return screeningRequest(`/questionnaire-versions/${versionId}/rules`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function updateEligibilityRule(ruleId, payload) {
  return screeningRequest(`/rules/${ruleId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export function deleteEligibilityRule(ruleId) {
  return screeningRequest(`/rules/${ruleId}`, { method: 'DELETE' });
}

// Citizen screening workflow
export function getCitizenScreeningForm(campaignId) {
  return screeningRequest(`/campaigns/${campaignId}/form`, { method: 'GET' });
}

export function getActiveScreeningSubmission(citizenId, campaignId) {
  return screeningRequest(
    `/citizens/${citizenId}/campaigns/${campaignId}/active-submission`,
    { method: 'GET' },
  );
}

export function submitScreening(payload) {
  return screeningRequest('/submissions', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function getScreeningCorrectionForm(submissionId) {
  return screeningRequest(`/submissions/${submissionId}/correction-form`, { method: 'GET' });
}

export function resubmitScreening(submissionId, answers) {
  return screeningRequest(`/submissions/${submissionId}/resubmit`, {
    method: 'POST',
    body: JSON.stringify({ answers }),
  });
}

// Citizen eligibility and historical display (read-only)
export function listCitizenScreeningSubmissions(citizenId) {
  return screeningRequest(`/citizens/${citizenId}/submissions`, { method: 'GET' });
}

export function listCitizenEligibilityAssessments(citizenId) {
  return screeningRequest(`/citizens/${citizenId}/assessments`, { method: 'GET' });
}

export function getEligibilityAssessmentBySubmission(submissionId) {
  return screeningRequest(`/assessments/submissions/${submissionId}`, { method: 'GET' });
}

export function getCitizenCampaignEligibility(citizenId, campaignId) {
  return screeningRequest(`/citizens/${citizenId}/campaigns/${campaignId}/eligibility`, { method: 'GET' });
}

export function getScreeningHistoryDetails(submissionId) {
  return screeningRequest(`/submissions/${submissionId}/history-details`, { method: 'GET' });
}

// Healthcare-officer screening review workflow
export function listScreeningReviewQueue({ campaignId } = {}) {
  const query = new URLSearchParams();
  if (campaignId !== undefined && campaignId !== null && campaignId !== '') {
    query.set('campaignId', String(campaignId));
  }
  const suffix = query.size > 0 ? `?${query.toString()}` : '';
  return screeningRequest(`/assessments/queue${suffix}`, { method: 'GET' });
}

export function getScreeningReviewDetails(submissionId) {
  return screeningRequest(`/submissions/${submissionId}/review-details`, { method: 'GET' });
}

export function requestScreeningCorrections(submissionId, instructions) {
  return screeningRequest(`/assessments/submissions/${submissionId}/corrections`, {
    method: 'POST',
    body: JSON.stringify({ instructions }),
  });
}

export function recordEligibilityDecision(submissionId, payload) {
  const decision = {
    status: payload.status,
    reason: payload.reason,
  };
  if (payload.status === 'ELIGIBLE' && payload.validUntil) {
    decision.validUntil = payload.validUntil;
  }

  return screeningRequest(`/assessments/submissions/${submissionId}/decision`, {
    method: 'POST',
    body: JSON.stringify(decision),
  });
}
