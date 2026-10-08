import { beforeEach, describe, expect, it, vi } from 'vitest';

const clientMocks = vi.hoisted(() => ({
  apiRequest: vi.fn(),
  parseApiResponse: vi.fn(),
}));

vi.mock('../../../shared/api/apiClient', () => clientMocks);

import {
  addQuestionnaireItem,
  listCitizenScreeningSubmissions,
  listCitizenEligibilityAssessments,
  getEligibilityAssessmentBySubmission,
  getCitizenCampaignEligibility,
  getScreeningHistoryDetails,
  getScreeningCorrectionForm,
  getScreeningReviewDetails,
  createAnswerOption,
  createEligibilityRule,
  createQuestion,
  createQuestionnaire,
  createQuestionnaireVersion,
  deleteAnswerOption,
  deleteEligibilityRule,
  deleteQuestion,
  deleteQuestionnaireVersion,
  getAnswerOption,
  getActiveScreeningSubmission,
  getCitizenScreeningForm,
  getEligibilityRule,
  getQuestion,
  getQuestionnaire,
  getQuestionnaireVersion,
  listAnswerOptions,
  listEligibilityRules,
  listQuestions,
  listScreeningReviewQueue,
  listQuestionnaires,
  listQuestionnaireVersions,
  publishQuestionnaireVersion,
  retireQuestionnaireVersion,
  resubmitScreening,
  requestScreeningCorrections,
  recordEligibilityDecision,
  revokeAssessment,
  submitScreening,
  updateAnswerOption,
  updateEligibilityRule,
  updateQuestion,
} from './screeningApi';

describe('screeningApi', () => {
  const rawResponse = { status: 200 };

  beforeEach(() => {
    vi.clearAllMocks();
    clientMocks.apiRequest.mockResolvedValue(rawResponse);
    clientMocks.parseApiResponse.mockResolvedValue({ result: 'parsed' });
  });

  it('lists all questions through the exact backend path', async () => {
    await expect(listQuestions()).resolves.toEqual({ result: 'parsed' });

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/questions', { method: 'GET' });
    expect(clientMocks.parseApiResponse).toHaveBeenCalledWith(rawResponse);
  });

  it.each([
    [listCitizenScreeningSubmissions, [71], '/v1/screening/citizens/71/submissions'],
    [listCitizenEligibilityAssessments, [71], '/v1/screening/citizens/71/assessments'],
    [getEligibilityAssessmentBySubmission, [42], '/v1/screening/assessments/submissions/42'],
    [getCitizenCampaignEligibility, [71, 22], '/v1/screening/citizens/71/campaigns/22/eligibility'],
    [getScreeningHistoryDetails, [42], '/v1/screening/submissions/42/history-details'],
  ])('uses the shared GET client for %s', async (request, args, path) => {
    await expect(request(...args)).resolves.toEqual({ result: 'parsed' });
    expect(clientMocks.apiRequest).toHaveBeenCalledExactlyOnceWith(path, { method: 'GET' });
    expect(clientMocks.parseApiResponse).toHaveBeenCalledExactlyOnceWith(rawResponse);
  });

  it('uses the real shared parser to return null for a 204 assessment', async () => {
    const { parseApiResponse } = await vi.importActual('../../../shared/api/apiClient');
    clientMocks.apiRequest.mockResolvedValueOnce(new Response(null, { status: 204 }));
    clientMocks.parseApiResponse.mockImplementationOnce(parseApiResponse);
    await expect(getEligibilityAssessmentBySubmission(42)).resolves.toBeNull();
  });

  it('supports the backend activeOnly question filter', async () => {
    await listQuestions({ activeOnly: true });

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/questions?activeOnly=true', { method: 'GET' });
  });

  it('gets a question by ID', async () => {
    await getQuestion(12);

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/questions/12', { method: 'GET' });
  });

  it('creates a question with the backend DTO body', async () => {
    const payload = {
      code: 'TRAVEL_HISTORY',
      text: 'Have you travelled recently?',
      description: null,
      questionType: 'BOOLEAN',
      defaultRequired: true,
    };

    await createQuestion(payload);

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/questions', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  });

  it('updates a question through PUT using only mutable fields', async () => {
    const payload = {
      text: 'Updated question?',
      description: 'Updated description',
      defaultRequired: false,
      active: true,
    };

    await updateQuestion(12, payload);

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/questions/12', {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
  });

  it('deletes a question through the exact backend path', async () => {
    await deleteQuestion(12);

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/questions/12', { method: 'DELETE' });
  });

  it('lists and gets answer options through their backend paths', async () => {
    await listAnswerOptions(12);
    expect(clientMocks.apiRequest).toHaveBeenLastCalledWith('/v1/screening/questions/12/options', { method: 'GET' });

    await getAnswerOption(44);
    expect(clientMocks.apiRequest).toHaveBeenLastCalledWith('/v1/screening/options/44', { method: 'GET' });
  });

  it('creates an answer option with the exact DTO body', async () => {
    const payload = { code: 'YES', label: 'Yes', typedValue: 'true', displayOrder: 1 };

    await createAnswerOption(12, payload);

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/questions/12/options', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  });

  it('updates an answer option through PUT', async () => {
    const payload = { code: 'NO', label: 'No', typedValue: 'false', displayOrder: 2 };

    await updateAnswerOption(44, payload);

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/options/44', {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
  });

  it('deletes an answer option through the exact backend path', async () => {
    await deleteAnswerOption(44);

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/options/44', { method: 'DELETE' });
  });

  it('lists, retrieves, and creates questionnaire containers through the exact backend routes', async () => {
    await listQuestionnaires();
    expect(clientMocks.apiRequest).toHaveBeenLastCalledWith('/v1/screening/questionnaires', { method: 'GET' });

    await getQuestionnaire(7);
    expect(clientMocks.apiRequest).toHaveBeenLastCalledWith('/v1/screening/questionnaires/7', { method: 'GET' });

    const payload = { code: 'ADULT', name: 'Adult Screening', description: null };
    await createQuestionnaire(payload);
    expect(clientMocks.apiRequest).toHaveBeenLastCalledWith('/v1/screening/questionnaires', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  });

  it('uses the version list, retrieval, and bodyless draft-creation routes', async () => {
    await listQuestionnaireVersions(7);
    expect(clientMocks.apiRequest).toHaveBeenLastCalledWith('/v1/screening/questionnaires/7/versions', { method: 'GET' });

    await getQuestionnaireVersion(12);
    expect(clientMocks.apiRequest).toHaveBeenLastCalledWith('/v1/screening/questionnaire-versions/12', { method: 'GET' });

    await createQuestionnaireVersion(7);
    expect(clientMocks.apiRequest).toHaveBeenLastCalledWith('/v1/screening/questionnaires/7/versions', { method: 'POST' });
  });

  it('adds a questionnaire item using the exact backend DTO', async () => {
    const payload = { questionId: 3, displayOrder: 2, requiredOverride: null };
    await addQuestionnaireItem(12, payload);

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/questionnaire-versions/12/items', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  });

  it('uses the supported questionnaire version lifecycle routes', async () => {
    await publishQuestionnaireVersion(12);
    expect(clientMocks.apiRequest).toHaveBeenLastCalledWith('/v1/screening/questionnaire-versions/12/publish', { method: 'POST' });

    await retireQuestionnaireVersion(12);
    expect(clientMocks.apiRequest).toHaveBeenLastCalledWith('/v1/screening/questionnaire-versions/12/retire', { method: 'POST' });

    await deleteQuestionnaireVersion(12);
    expect(clientMocks.apiRequest).toHaveBeenLastCalledWith('/v1/screening/questionnaire-versions/12', { method: 'DELETE' });
  });

  it('lists and retrieves eligibility rules through their exact backend routes', async () => {
    await listEligibilityRules(12);
    expect(clientMocks.apiRequest).toHaveBeenLastCalledWith('/v1/screening/questionnaire-versions/12/rules', { method: 'GET' });

    await getEligibilityRule(31);
    expect(clientMocks.apiRequest).toHaveBeenLastCalledWith('/v1/screening/rules/31', { method: 'GET' });
  });

  it('creates, updates, and deletes eligibility rules through supported routes', async () => {
    const payload = {
      ruleCode: 'AGE_REVIEW',
      ruleName: 'Age review',
      conditionExpression: 'AGE == 18',
      preliminaryResult: 'PRELIMINARY_REVIEW_REQUIRED',
      priority: 1,
      message: 'Clinical review required',
    };

    await createEligibilityRule(12, payload);
    expect(clientMocks.apiRequest).toHaveBeenLastCalledWith('/v1/screening/questionnaire-versions/12/rules', {
      method: 'POST',
      body: JSON.stringify(payload),
    });

    await updateEligibilityRule(31, payload);
    expect(clientMocks.apiRequest).toHaveBeenLastCalledWith('/v1/screening/rules/31', {
      method: 'PUT',
      body: JSON.stringify(payload),
    });

    await deleteEligibilityRule(31);
    expect(clientMocks.apiRequest).toHaveBeenLastCalledWith('/v1/screening/rules/31', { method: 'DELETE' });
  });

  it('loads the citizen screening form through the campaign form path', async () => {
    await getCitizenScreeningForm(22);

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/campaigns/22/form', { method: 'GET' });
  });

  it('loads an active submission using the authenticated citizen and campaign IDs', async () => {
    await getActiveScreeningSubmission(7, 22);

    expect(clientMocks.apiRequest).toHaveBeenCalledWith(
      '/v1/screening/citizens/7/campaigns/22/active-submission',
      { method: 'GET' },
    );
  });

  it('posts the complete screening payload without changing answer value types', async () => {
    const payload = {
      citizenId: 7,
      campaignId: 22,
      questionnaireVersionId: 31,
      consentNoticeVersionId: 4,
      answers: { 101: 'true', 102: '37.5', 103: 'ASTHMA,DIABETES' },
    };

    await submitScreening(payload);

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/submissions', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  });

  it('loads the historical correction form through the exact GET path', async () => {
    await getScreeningCorrectionForm(42);

    expect(clientMocks.apiRequest).toHaveBeenCalledExactlyOnceWith(
      '/v1/screening/submissions/42/correction-form',
      { method: 'GET' },
    );
    expect(clientMocks.parseApiResponse).toHaveBeenCalledExactlyOnceWith(rawResponse);
  });

  it('posts only the complete answer map when resubmitting corrections', async () => {
    const answers = { 101: 'true', 102: 'NO', 103: 'ASTHMA,DIABETES' };

    await resubmitScreening(42, answers);

    expect(clientMocks.apiRequest).toHaveBeenCalledExactlyOnceWith(
      '/v1/screening/submissions/42/resubmit',
      { method: 'POST', body: JSON.stringify({ answers }) },
    );
    expect(JSON.parse(clientMocks.apiRequest.mock.calls[0][1].body)).toEqual({ answers });
    expect(clientMocks.parseApiResponse).toHaveBeenCalledExactlyOnceWith(rawResponse);
  });

  it('loads the review queue without an unnecessary empty query string', async () => {
    await listScreeningReviewQueue();

    expect(clientMocks.apiRequest).toHaveBeenCalledExactlyOnceWith(
      '/v1/screening/assessments/queue',
      { method: 'GET' },
    );
  });

  it('loads the review queue with an encoded campaign filter', async () => {
    await listScreeningReviewQueue({ campaignId: 22 });

    expect(clientMocks.apiRequest).toHaveBeenCalledExactlyOnceWith(
      '/v1/screening/assessments/queue?campaignId=22',
      { method: 'GET' },
    );
  });

  it('loads staff review details through the exact backend route', async () => {
    await getScreeningReviewDetails(42);

    expect(clientMocks.apiRequest).toHaveBeenCalledExactlyOnceWith(
      '/v1/screening/submissions/42/review-details',
      { method: 'GET' },
    );
  });

  it('posts trimmed correction instructions through the correction route payload', async () => {
    await requestScreeningCorrections(42, 'Review the vaccination date.');

    expect(clientMocks.apiRequest).toHaveBeenCalledExactlyOnceWith(
      '/v1/screening/assessments/submissions/42/corrections',
      {
        method: 'POST',
        body: JSON.stringify({ instructions: 'Review the vaccination date.' }),
      },
    );
  });

  it('posts only the supported eligible decision fields', async () => {
    await recordEligibilityDecision(42, {
      status: 'ELIGIBLE',
      reason: 'Eligible after clinical review',
      validUntil: '2027-01-01T00:00:00.000Z',
      reviewerId: 999,
      clinicalNotes: 'must not be sent',
    });

    const [, options] = clientMocks.apiRequest.mock.calls[0];
    expect(clientMocks.apiRequest.mock.calls[0][0])
      .toBe('/v1/screening/assessments/submissions/42/decision');
    expect(options.method).toBe('POST');
    expect(JSON.parse(options.body)).toEqual({
      status: 'ELIGIBLE',
      reason: 'Eligible after clinical review',
      validUntil: '2027-01-01T00:00:00.000Z',
    });
  });

  it('omits validUntil, reviewerId, and clinicalNotes from an ineligible decision', async () => {
    await recordEligibilityDecision(42, {
      status: 'INELIGIBLE',
      reason: 'Contraindication requires deferral',
      validUntil: '2027-01-01T00:00:00.000Z',
      reviewerId: 999,
      clinicalNotes: 'must not be sent',
    });

    expect(JSON.parse(clientMocks.apiRequest.mock.calls[0][1].body)).toEqual({
      status: 'INELIGIBLE',
      reason: 'Contraindication requires deferral',
    });
  });

  it('posts only the revocation reason when revoking an assessment', async () => {
    await revokeAssessment(80, 'Eligibility withdrawn after reassessment');

    expect(clientMocks.apiRequest).toHaveBeenCalledExactlyOnceWith(
      '/v1/screening/assessments/80/revoke',
      {
        method: 'POST',
        body: JSON.stringify({ revocationReason: 'Eligibility withdrawn after reassessment' }),
      },
    );
    const payload = JSON.parse(clientMocks.apiRequest.mock.calls[0][1].body);
    expect(payload).not.toHaveProperty('reviewerId');
    expect(payload).not.toHaveProperty('revokerId');
    expect(payload).not.toHaveProperty('userId');
  });
});
