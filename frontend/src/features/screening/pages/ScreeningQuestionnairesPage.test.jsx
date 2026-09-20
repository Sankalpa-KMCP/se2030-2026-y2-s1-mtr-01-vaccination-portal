import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react';
import { ApiError } from '../../../services/apiClient';

const apiMocks = vi.hoisted(() => ({
  listQuestionnaires: vi.fn(),
  getQuestionnaire: vi.fn(),
  createQuestionnaire: vi.fn(),
  listQuestionnaireVersions: vi.fn(),
  getQuestionnaireVersion: vi.fn(),
  createQuestionnaireVersion: vi.fn(),
  addQuestionnaireItem: vi.fn(),
  createQuestion: vi.fn(),
  createAnswerOption: vi.fn(),
  listAnswerOptions: vi.fn(),
  publishQuestionnaireVersion: vi.fn(),
  retireQuestionnaireVersion: vi.fn(),
  deleteQuestionnaireVersion: vi.fn(),
  removeQuestionnaireItem: vi.fn(),
  updateQuestionnaireItem: vi.fn(),
  reorderQuestionnaireItems: vi.fn(),
  listEligibilityRules: vi.fn(),
  getEligibilityRule: vi.fn(),
  createEligibilityRule: vi.fn(),
  updateEligibilityRule: vi.fn(),
  deleteEligibilityRule: vi.fn(),
  listQuestions: vi.fn(),
}));

vi.mock('../api/screeningApi', () => apiMocks);

import ScreeningQuestionnairesPage from './ScreeningQuestionnairesPage';

const questionnaire = {
  id: 7,
  code: 'ADULT_SCREENING',
  name: 'Adult Screening',
  description: 'Standard adult questionnaire',
};

const ageQuestion = {
  id: 1,
  code: 'AGE',
  text: 'What is your age?',
  questionType: 'NUMERIC',
  defaultRequired: true,
  active: true,
};

const travelQuestion = {
  id: 2,
  code: 'TRAVEL',
  text: 'Have you travelled recently?',
  questionType: 'BOOLEAN',
  defaultRequired: false,
  active: true,
};

const eligibilityRule = {
  id: 31,
  questionnaireVersionId: 12,
  ruleCode: 'AGE_REVIEW',
  ruleName: 'Age review',
  conditionExpression: 'AGE == 18',
  preliminaryResult: 'PRELIMINARY_REVIEW_REQUIRED',
  priority: 1,
  message: 'Clinical review required',
};

const draftVersion = {
  id: 12,
  questionnaireId: 7,
  versionNumber: 2,
  status: 'DRAFT',
  publishedAt: null,
  retiredAt: null,
  items: [
    { id: 102, questionnaireVersionId: 12, questionId: 2, displayOrder: 2, requiredOverride: true },
    { id: 101, questionnaireVersionId: 12, questionId: 1, displayOrder: 1, requiredOverride: null },
  ],
  rules: [eligibilityRule],
};

function renderPage() {
  return render(<ScreeningQuestionnairesPage />);
}

async function openDraft() {
  renderPage();
  fireEvent.click(await screen.findByRole('button', { name: 'Open ADULT_SCREENING' }));
  await screen.findByText('What is your age?');
}

function fillNewQuestion(dialog, { code = 'NEW_QUESTION', type = 'BOOLEAN' } = {}) {
  fireEvent.click(within(dialog).getByRole('button', { name: 'Create New' }));
  fireEvent.change(within(dialog).getByLabelText(/Question Code/), { target: { value: code } });
  fireEvent.change(within(dialog).getByLabelText(/Question Text/), { target: { value: 'A newly created screening question?' } });
  fireEvent.change(within(dialog).getByLabelText(/Question Type/), { target: { value: type } });
}

function fillOption(row, { code, label, typedValue }) {
  fireEvent.change(within(row).getByLabelText(/Option Code/), { target: { value: code } });
  fireEvent.change(within(row).getByLabelText(/^Label/), { target: { value: label } });
  fireEvent.change(within(row).getByLabelText(/Typed Value/), { target: { value: typedValue } });
}

describe('ScreeningQuestionnairesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiMocks.listQuestionnaires.mockResolvedValue([questionnaire]);
    apiMocks.getQuestionnaire.mockResolvedValue(questionnaire);
    apiMocks.listQuestions.mockResolvedValue([ageQuestion, travelQuestion]);
    apiMocks.listQuestionnaireVersions.mockResolvedValue([draftVersion]);
    apiMocks.getQuestionnaireVersion.mockResolvedValue(draftVersion);
    apiMocks.listEligibilityRules.mockResolvedValue([eligibilityRule]);
    apiMocks.listAnswerOptions.mockResolvedValue([]);
  });

  afterEach(() => cleanup());

  it('shows a shared loading state while initial requests are pending', () => {
    apiMocks.listQuestionnaires.mockReturnValue(new Promise(() => {}));
    renderPage();

    expect(screen.getAllByText('Loading screening questionnaires...')).toHaveLength(2);
  });

  it('renders questionnaire containers returned by the backend', async () => {
    renderPage();

    expect(await screen.findByText('Adult Screening')).toBeTruthy();
    expect(screen.getByText('ADULT_SCREENING')).toBeTruthy();
    expect(screen.getByText('Standard adult questionnaire')).toBeTruthy();
  });

  it('renders explicit empty and API error states', async () => {
    apiMocks.listQuestionnaires.mockResolvedValue([]);
    const view = renderPage();
    expect(await screen.findByText('No screening questionnaires have been configured yet.')).toBeTruthy();

    cleanup();
    apiMocks.listQuestionnaires.mockRejectedValue(new ApiError('Unable to load questionnaires', 500));
    renderPage();
    expect((await screen.findByRole('alert')).textContent).toContain('Unable to load questionnaires');
    view.unmount();
  });

  it('creates a questionnaire using only the backend DTO fields', async () => {
    const created = { id: 8, code: 'CHILD', name: 'Child Screening', description: null };
    apiMocks.createQuestionnaire.mockResolvedValue(created);
    renderPage();

    fireEvent.click((await screen.findAllByRole('button', { name: 'Create Questionnaire' }))[0]);
    const dialog = screen.getByRole('dialog', { name: 'Create Questionnaire' });
    fireEvent.change(within(dialog).getByLabelText(/Code/), { target: { value: 'CHILD' } });
    fireEvent.change(within(dialog).getByLabelText(/Name/), { target: { value: 'Child Screening' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create Questionnaire' }));

    await waitFor(() => expect(apiMocks.createQuestionnaire).toHaveBeenCalledWith({
      code: 'CHILD',
      name: 'Child Screening',
      description: null,
    }));
    expect(await screen.findByText('Questionnaire created. Create its first draft version to begin building.')).toBeTruthy();
  });

  it('selects the latest version and renders attached items in persisted order', async () => {
    await openDraft();

    expect(apiMocks.listQuestionnaireVersions).toHaveBeenCalledWith(7);
    expect(apiMocks.getQuestionnaire).toHaveBeenCalledWith(7);
    expect(apiMocks.getQuestionnaireVersion).toHaveBeenCalledWith(12);
    expect(apiMocks.listEligibilityRules).toHaveBeenCalledWith(12);
    const orderTable = screen.getByRole('columnheader', { name: 'Order' }).closest('table');
    const rows = within(orderTable).getAllByRole('row');
    expect(within(rows[1]).getByText('What is your age?')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Change question AGE' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Remove question AGE' })).toBeTruthy();
  });

  it('creates a bodyless draft version for the selected questionnaire', async () => {
    apiMocks.listQuestionnaireVersions.mockResolvedValue([]);
    apiMocks.createQuestionnaireVersion.mockResolvedValue({ ...draftVersion, versionNumber: 1, items: [], rules: [] });
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Open ADULT_SCREENING' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Create Draft Version' }));

    await waitFor(() => expect(apiMocks.createQuestionnaireVersion).toHaveBeenCalledWith(7));
    expect(await screen.findByText('Draft version 1 created.')).toBeTruthy();
  });

  it('adds an available catalogue question with display order and required override', async () => {
    const oneItemVersion = { ...draftVersion, items: [draftVersion.items[1]] };
    apiMocks.getQuestionnaireVersion.mockResolvedValue(oneItemVersion);
    apiMocks.addQuestionnaireItem.mockResolvedValue(draftVersion.items[0]);
    await openDraft();

    fireEvent.click(screen.getByRole('button', { name: 'Add Question' }));
    const dialog = screen.getByRole('dialog', { name: 'Add Question to Version' });
    expect(within(dialog).getByRole('option', { name: /TRAVEL/ })).toBeTruthy();
    fireEvent.change(within(dialog).getByLabelText('Required Override'), { target: { value: 'true' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Add Question' }));

    await waitFor(() => expect(apiMocks.addQuestionnaireItem).toHaveBeenCalledWith(12, {
      questionId: 2,
      displayOrder: 2,
      requiredOverride: true,
    }));
    expect(await screen.findByText('Question added to the draft version.')).toBeTruthy();
  });

  it('creates a global Boolean question and automatically attaches it before refreshing', async () => {
    const createdQuestion = {
      id: 3,
      code: 'NEW_QUESTION',
      text: 'A newly created screening question?',
      description: null,
      questionType: 'BOOLEAN',
      defaultRequired: true,
      active: true,
    };
    const attachedItem = { id: 103, questionnaireVersionId: 12, questionId: 3, displayOrder: 3, requiredOverride: false };
    apiMocks.createQuestion.mockResolvedValue(createdQuestion);
    apiMocks.addQuestionnaireItem.mockResolvedValue(attachedItem);
    await openDraft();

    fireEvent.click(screen.getByRole('button', { name: 'Add Question' }));
    const dialog = screen.getByRole('dialog', { name: 'Add Question to Version' });
    fillNewQuestion(dialog);
    expect(within(dialog).queryByText('Answer Options')).toBeNull();
    fireEvent.change(within(dialog).getByLabelText('Required Override'), { target: { value: 'false' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create and Add Question' }));

    await waitFor(() => expect(apiMocks.createQuestion).toHaveBeenCalledWith({
      code: 'NEW_QUESTION',
      text: 'A newly created screening question?',
      description: null,
      questionType: 'BOOLEAN',
      defaultRequired: true,
    }));
    await waitFor(() => expect(apiMocks.addQuestionnaireItem).toHaveBeenCalledWith(12, {
      questionId: 3,
      displayOrder: 3,
      requiredOverride: false,
    }));
    expect(apiMocks.createQuestion.mock.invocationCallOrder[0]).toBeLessThan(apiMocks.addQuestionnaireItem.mock.invocationCallOrder[0]);
    expect(await screen.findByText('Question created and added to this questionnaire version.')).toBeTruthy();
    expect(apiMocks.listQuestions).toHaveBeenCalledTimes(2);
    expect(apiMocks.getQuestionnaireVersion).toHaveBeenCalledTimes(2);
  });

  it('creates choice options in display order before attaching the new question', async () => {
    const createdQuestion = { id: 4, code: 'REACTION', text: 'A newly created screening question?', questionType: 'SINGLE_CHOICE', defaultRequired: true, active: true };
    apiMocks.createQuestion.mockResolvedValue(createdQuestion);
    apiMocks.listAnswerOptions.mockResolvedValue([]);
    apiMocks.createAnswerOption.mockImplementation(async (_questionId, option) => ({ id: option.displayOrder, questionId: 4, ...option }));
    apiMocks.addQuestionnaireItem.mockResolvedValue({ id: 104, questionId: 4, displayOrder: 3, requiredOverride: null });
    await openDraft();

    fireEvent.click(screen.getByRole('button', { name: 'Add Question' }));
    const dialog = screen.getByRole('dialog', { name: 'Add Question to Version' });
    fillNewQuestion(dialog, { code: 'REACTION', type: 'SINGLE_CHOICE' });
    let rows = within(dialog).getAllByTestId('new-question-option-row');
    fillOption(rows[0], { code: 'NO', label: 'No reaction', typedValue: 'none' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Add Option' }));
    rows = within(dialog).getAllByTestId('new-question-option-row');
    fillOption(rows[1], { code: 'YES', label: 'Severe reaction', typedValue: 'severe' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create and Add Question' }));

    await waitFor(() => expect(apiMocks.createAnswerOption).toHaveBeenCalledTimes(2));
    expect(apiMocks.createAnswerOption).toHaveBeenNthCalledWith(1, 4, {
      code: 'NO', label: 'No reaction', typedValue: 'none', displayOrder: 1,
    });
    expect(apiMocks.createAnswerOption).toHaveBeenNthCalledWith(2, 4, {
      code: 'YES', label: 'Severe reaction', typedValue: 'severe', displayOrder: 2,
    });
    expect(apiMocks.createAnswerOption.mock.invocationCallOrder[1]).toBeLessThan(apiMocks.addQuestionnaireItem.mock.invocationCallOrder[0]);
  });

  it('preserves partial option progress and safely retries without recreating successful records', async () => {
    const createdQuestion = { id: 4, code: 'REACTION', text: 'A newly created screening question?', questionType: 'MULTI_CHOICE', defaultRequired: true, active: true };
    const savedFirst = { id: 41, questionId: 4, code: 'NO', label: 'No reaction', typedValue: 'none', displayOrder: 1 };
    apiMocks.createQuestion.mockResolvedValue(createdQuestion);
    apiMocks.listAnswerOptions.mockResolvedValueOnce([]).mockResolvedValue([savedFirst]);
    apiMocks.createAnswerOption
      .mockResolvedValueOnce(savedFirst)
      .mockRejectedValueOnce(new ApiError('Temporary option failure', 500))
      .mockResolvedValueOnce({ id: 42, questionId: 4, code: 'YES', label: 'Severe reaction', typedValue: 'severe', displayOrder: 2 });
    apiMocks.addQuestionnaireItem.mockResolvedValue({ id: 104, questionId: 4, displayOrder: 3, requiredOverride: null });
    await openDraft();

    fireEvent.click(screen.getByRole('button', { name: 'Add Question' }));
    const dialog = screen.getByRole('dialog', { name: 'Add Question to Version' });
    fillNewQuestion(dialog, { code: 'REACTION', type: 'MULTI_CHOICE' });
    let rows = within(dialog).getAllByTestId('new-question-option-row');
    fillOption(rows[0], { code: 'NO', label: 'No reaction', typedValue: 'none' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Add Option' }));
    rows = within(dialog).getAllByTestId('new-question-option-row');
    fillOption(rows[1], { code: 'YES', label: 'Severe reaction', typedValue: 'severe' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create and Add Question' }));

    expect(await within(dialog).findByText(/one or more answer options could not be saved/i)).toBeTruthy();
    expect(apiMocks.addQuestionnaireItem).not.toHaveBeenCalled();
    expect(within(dialog).getAllByText(/now exists in the global catalogue/i)).toHaveLength(2);
    fireEvent.click(within(dialog).getByRole('button', { name: 'Retry Create and Add' }));

    await waitFor(() => expect(apiMocks.addQuestionnaireItem).toHaveBeenCalledTimes(1));
    expect(apiMocks.createQuestion).toHaveBeenCalledTimes(1);
    expect(apiMocks.createAnswerOption).toHaveBeenCalledTimes(3);
    expect(await screen.findByText('Question created and added to this questionnaire version.')).toBeTruthy();
  });

  it('does not create options or attach when existing option reconciliation fails', async () => {
    const createdQuestion = { id: 4, code: 'REACTION', text: 'A newly created screening question?', questionType: 'SINGLE_CHOICE', defaultRequired: true, active: true };
    apiMocks.createQuestion.mockResolvedValue(createdQuestion);
    apiMocks.listAnswerOptions.mockRejectedValue(new ApiError('Options unavailable', 500));
    await openDraft();

    fireEvent.click(screen.getByRole('button', { name: 'Add Question' }));
    const dialog = screen.getByRole('dialog', { name: 'Add Question to Version' });
    fillNewQuestion(dialog, { code: 'REACTION', type: 'SINGLE_CHOICE' });
    const row = within(dialog).getByTestId('new-question-option-row');
    fillOption(row, { code: 'YES', label: 'Severe reaction', typedValue: 'severe' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create and Add Question' }));

    expect(await within(dialog).findByText(/current answer options could not be loaded safely/i)).toBeTruthy();
    expect(apiMocks.createAnswerOption).not.toHaveBeenCalled();
    expect(apiMocks.addQuestionnaireItem).not.toHaveBeenCalled();
  });

  it('keeps a created catalogue question when attachment fails and offers retry', async () => {
    const createdQuestion = { id: 3, code: 'NEW_QUESTION', text: 'A newly created screening question?', questionType: 'BOOLEAN', defaultRequired: true, active: true };
    apiMocks.createQuestion.mockResolvedValue(createdQuestion);
    apiMocks.addQuestionnaireItem.mockRejectedValue(new ApiError('Display order already used', 409));
    await openDraft();

    fireEvent.click(screen.getByRole('button', { name: 'Add Question' }));
    const dialog = screen.getByRole('dialog', { name: 'Add Question to Version' });
    fillNewQuestion(dialog);
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create and Add Question' }));

    expect(await within(dialog).findByText(/could not be added to this questionnaire version/i)).toBeTruthy();
    expect(within(dialog).getByText(/now exists in the global catalogue/i)).toBeTruthy();
    expect(apiMocks.createQuestion).toHaveBeenCalledTimes(1);
  });

  it('preserves the form and makes no later calls when question creation fails', async () => {
    apiMocks.createQuestion.mockRejectedValue(new ApiError("Screening question with code 'NEW_QUESTION' already exists", 409));
    await openDraft();

    fireEvent.click(screen.getByRole('button', { name: 'Add Question' }));
    const dialog = screen.getByRole('dialog', { name: 'Add Question to Version' });
    fillNewQuestion(dialog);
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create and Add Question' }));

    expect(await within(dialog).findByText(/already exists/i)).toBeTruthy();
    expect(within(dialog).getByLabelText(/Question Code/).value).toBe('NEW_QUESTION');
    expect(apiMocks.createAnswerOption).not.toHaveBeenCalled();
    expect(apiMocks.addQuestionnaireItem).not.toHaveBeenCalled();
  });

  it('disables Create New submission while the first API call is pending', async () => {
    apiMocks.createQuestion.mockReturnValue(new Promise(() => {}));
    await openDraft();

    fireEvent.click(screen.getByRole('button', { name: 'Add Question' }));
    const dialog = screen.getByRole('dialog', { name: 'Add Question to Version' });
    fillNewQuestion(dialog);
    const submit = within(dialog).getByRole('button', { name: 'Create and Add Question' });
    fireEvent.click(submit);
    await waitFor(() => expect(submit.disabled).toBe(true));
    fireEvent.click(submit);
    expect(apiMocks.createQuestion).toHaveBeenCalledTimes(1);
  });

  it('renders and creates eligibility rules with exact enum values', async () => {
    apiMocks.createEligibilityRule.mockResolvedValue({ ...eligibilityRule, id: 32, ruleCode: 'TRAVEL_REVIEW', ruleName: 'Travel review' });
    await openDraft();
    expect(screen.getByText('Clinical review required')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Create Rule' }));
    const dialog = screen.getByRole('dialog', { name: 'Create Eligibility Rule' });
    fireEvent.change(within(dialog).getByLabelText(/Rule Code/), { target: { value: 'TRAVEL_REVIEW' } });
    fireEvent.change(within(dialog).getByLabelText(/Rule Name/), { target: { value: 'Travel review' } });
    fireEvent.change(within(dialog).getByLabelText(/Question/), { target: { value: '2' } });
    expect(within(dialog).getByLabelText(/Operator/).value).toBe('==');
    expect(within(dialog).getByLabelText(/Value/).value).toBe('true');
    fireEvent.change(within(dialog).getByLabelText(/Priority/), { target: { value: '2' } });
    fireEvent.change(within(dialog).getByLabelText(/Result Message/), { target: { value: 'Review travel history' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create Rule' }));

    await waitFor(() => expect(apiMocks.createEligibilityRule).toHaveBeenCalledWith(12, {
      ruleCode: 'TRAVEL_REVIEW',
      ruleName: 'Travel review',
      conditionExpression: 'TRAVEL == true',
      preliminaryResult: 'PRELIMINARY_REVIEW_REQUIRED',
      priority: 2,
      message: 'Review travel history',
    }));
  });

  it('supports rule update and confirmed deletion for a draft version', async () => {
    apiMocks.updateEligibilityRule.mockResolvedValue({ ...eligibilityRule, ruleName: 'Minor age review' });
    apiMocks.deleteEligibilityRule.mockResolvedValue(null);
    await openDraft();

    fireEvent.click(screen.getByRole('button', { name: 'Edit rule AGE_REVIEW' }));
    const editDialog = screen.getByRole('dialog', { name: 'Edit Eligibility Rule' });
    expect(within(editDialog).getByLabelText(/Question/).value).toBe('1');
    expect(within(editDialog).getByLabelText(/Value/).value).toBe('18');
    fireEvent.change(within(editDialog).getByLabelText(/Rule Name/), { target: { value: 'Minor age review' } });
    fireEvent.click(within(editDialog).getByRole('button', { name: 'Save Rule' }));
    await waitFor(() => expect(apiMocks.updateEligibilityRule).toHaveBeenCalledWith(31, expect.objectContaining({ ruleName: 'Minor age review' })));

    fireEvent.click(screen.getByRole('button', { name: 'Delete rule AGE_REVIEW' }));
    const deleteDialog = screen.getByRole('dialog', { name: 'Delete Eligibility Rule' });
    expect(apiMocks.deleteEligibilityRule).not.toHaveBeenCalled();
    fireEvent.click(within(deleteDialog).getByRole('button', { name: 'Delete Rule' }));
    await waitFor(() => expect(apiMocks.deleteEligibilityRule).toHaveBeenCalledWith(31));
  });

  it('preserves an unsupported existing expression until explicit replacement', async () => {
    const legacyRule = { ...eligibilityRule, conditionExpression: 'AGE < 18' };
    apiMocks.listEligibilityRules.mockResolvedValue([legacyRule]);
    apiMocks.updateEligibilityRule.mockResolvedValue({ ...legacyRule, ruleName: 'Legacy age review' });
    await openDraft();

    fireEvent.click(screen.getByRole('button', { name: 'Edit rule AGE_REVIEW' }));
    const dialog = screen.getByRole('dialog', { name: 'Edit Eligibility Rule' });
    expect(within(dialog).getByText(/not supported by the current screening evaluator/i)).toBeTruthy();
    expect(within(dialog).getByText('AGE < 18')).toBeTruthy();
    expect(within(dialog).queryByLabelText(/Question/)).toBeNull();

    fireEvent.change(within(dialog).getByLabelText(/Rule Name/), { target: { value: 'Legacy age review' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save Rule' }));
    await waitFor(() => expect(apiMocks.updateEligibilityRule).toHaveBeenCalledWith(31, expect.objectContaining({
      conditionExpression: 'AGE < 18',
    })));
  });

  it('represents published immutability and exposes only the supported retire action', async () => {
    const published = { ...draftVersion, status: 'PUBLISHED' };
    apiMocks.listQuestionnaireVersions.mockResolvedValue([published]);
    apiMocks.getQuestionnaireVersion.mockResolvedValue(published);
    await openDraft();

    expect(screen.getByText(/published version is immutable/i)).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Add Question' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Create Rule' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Edit rule AGE_REVIEW' })).toBeNull();
    expect(screen.queryByRole('button', { name: /Change question/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /Remove question/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /Move up/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /Move down/i })).toBeNull();
    expect(screen.getByRole('button', { name: 'Retire' })).toBeTruthy();
  });

  it('publishes a draft only after confirmation', async () => {
    apiMocks.publishQuestionnaireVersion.mockResolvedValue({ ...draftVersion, status: 'PUBLISHED' });
    await openDraft();

    fireEvent.click(screen.getByRole('button', { name: 'Publish' }));
    expect(apiMocks.publishQuestionnaireVersion).not.toHaveBeenCalled();
    const dialog = screen.getByRole('dialog', { name: 'Publish Questionnaire Version' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Publish Version' }));

    await waitFor(() => expect(apiMocks.publishQuestionnaireVersion).toHaveBeenCalledWith(12));
    expect(await screen.findByText(/contents are now immutable/i)).toBeTruthy();
  });

  it('supports removing an unreferenced question with confirmation', async () => {
    apiMocks.removeQuestionnaireItem.mockResolvedValue(null);
    await openDraft();

    fireEvent.click(screen.getByRole('button', { name: 'Remove question TRAVEL' }));
    const dialog = screen.getByRole('dialog', { name: 'Remove question?' });
    expect(within(dialog).getByText(/Are you sure you want to remove 'Have you travelled recently\?' from Version 2\?/)).toBeTruthy();

    fireEvent.click(within(dialog).getByRole('button', { name: 'Remove' }));

    await waitFor(() => expect(apiMocks.removeQuestionnaireItem).toHaveBeenCalledWith(12, 102));
    expect(await screen.findByText('Question removed from draft version.')).toBeTruthy();
  });

  it('blocks removing a question referenced by an eligibility rule and displays friendly error', async () => {
    apiMocks.removeQuestionnaireItem.mockResolvedValue(null);
    await openDraft();

    // AGE is referenced by AGE_REVIEW rule (conditionExpression: 'AGE == 18')
    fireEvent.click(screen.getByRole('button', { name: 'Remove question AGE' }));

    expect(apiMocks.removeQuestionnaireItem).not.toHaveBeenCalled();
    expect(await screen.findByText(/This question is currently used by an eligibility rule \('Age review'\)\. Update or remove that rule before removing the question\./)).toBeTruthy();
  });

  it('supports replacing an attached question in a draft questionnaire', async () => {
    const reactionQuestion = {
      id: 3,
      code: 'REACTION',
      text: 'Have you had a previous vaccine reaction?',
      questionType: 'BOOLEAN',
      defaultRequired: false,
      active: true,
    };
    apiMocks.listQuestions.mockResolvedValue([ageQuestion, travelQuestion, reactionQuestion]);
    apiMocks.updateQuestionnaireItem.mockResolvedValue({
      id: 102,
      questionnaireVersionId: 12,
      questionId: 3,
      displayOrder: 2,
      requiredOverride: true,
    });

    await openDraft();

    fireEvent.click(screen.getByRole('button', { name: 'Change question TRAVEL' }));
    const dialog = screen.getByRole('dialog', { name: 'Change Question' });
    expect(within(dialog).getByText(/Current Question \(Position #2\)/)).toBeTruthy();

    fireEvent.change(within(dialog).getByLabelText(/Replacement Question/), { target: { value: '3' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Change Question' }));

    await waitFor(() => expect(apiMocks.updateQuestionnaireItem).toHaveBeenCalledWith(12, 102, {
      questionId: 3,
      requiredOverride: true,
    }));
    expect(await screen.findByText('Question updated in draft version.')).toBeTruthy();
  });

  it('supports reordering draft questionnaire items using Move Up and Move Down', async () => {
    apiMocks.reorderQuestionnaireItems.mockResolvedValue([
      { id: 102, questionnaireVersionId: 12, questionId: 2, displayOrder: 1, requiredOverride: true },
      { id: 101, questionnaireVersionId: 12, questionId: 1, displayOrder: 2, requiredOverride: null },
    ]);

    await openDraft();

    // In draftVersion: AGE is index 0 (order 1), TRAVEL is index 1 (order 2)
    // Move AGE down (swaps index 0 and index 1 => [102, 101])
    fireEvent.click(screen.getByRole('button', { name: 'Move down AGE' }));

    await waitFor(() => expect(apiMocks.reorderQuestionnaireItems).toHaveBeenCalledWith(12, [102, 101]));
    expect(await screen.findByText('Questionnaire item order updated.')).toBeTruthy();
  });
});
