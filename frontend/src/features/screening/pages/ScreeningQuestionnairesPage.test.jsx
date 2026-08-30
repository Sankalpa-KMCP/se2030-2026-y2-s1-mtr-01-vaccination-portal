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
  publishQuestionnaireVersion: vi.fn(),
  retireQuestionnaireVersion: vi.fn(),
  deleteQuestionnaireVersion: vi.fn(),
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
  conditionExpression: 'AGE < 18',
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

describe('ScreeningQuestionnairesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiMocks.listQuestionnaires.mockResolvedValue([questionnaire]);
    apiMocks.getQuestionnaire.mockResolvedValue(questionnaire);
    apiMocks.listQuestions.mockResolvedValue([ageQuestion, travelQuestion]);
    apiMocks.listQuestionnaireVersions.mockResolvedValue([draftVersion]);
    apiMocks.getQuestionnaireVersion.mockResolvedValue(draftVersion);
    apiMocks.listEligibilityRules.mockResolvedValue([eligibilityRule]);
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
    expect(within(rows[2]).getByText('Have you travelled recently?')).toBeTruthy();
    expect(screen.queryByRole('button', { name: /Remove Question/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /Reorder/i })).toBeNull();
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

  it('renders and creates eligibility rules with exact enum values', async () => {
    apiMocks.createEligibilityRule.mockResolvedValue({ ...eligibilityRule, id: 32, ruleCode: 'TRAVEL_REVIEW', ruleName: 'Travel review' });
    await openDraft();
    expect(screen.getByText('Clinical review required')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Create Rule' }));
    const dialog = screen.getByRole('dialog', { name: 'Create Eligibility Rule' });
    fireEvent.change(within(dialog).getByLabelText(/Rule Code/), { target: { value: 'TRAVEL_REVIEW' } });
    fireEvent.change(within(dialog).getByLabelText(/Rule Name/), { target: { value: 'Travel review' } });
    fireEvent.change(within(dialog).getByLabelText(/Condition Expression/), { target: { value: 'TRAVEL == true' } });
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
    fireEvent.change(within(editDialog).getByLabelText(/Rule Name/), { target: { value: 'Minor age review' } });
    fireEvent.click(within(editDialog).getByRole('button', { name: 'Save Rule' }));
    await waitFor(() => expect(apiMocks.updateEligibilityRule).toHaveBeenCalledWith(31, expect.objectContaining({ ruleName: 'Minor age review' })));

    fireEvent.click(screen.getByRole('button', { name: 'Delete rule AGE_REVIEW' }));
    const deleteDialog = screen.getByRole('dialog', { name: 'Delete Eligibility Rule' });
    expect(apiMocks.deleteEligibilityRule).not.toHaveBeenCalled();
    fireEvent.click(within(deleteDialog).getByRole('button', { name: 'Delete Rule' }));
    await waitFor(() => expect(apiMocks.deleteEligibilityRule).toHaveBeenCalledWith(31));
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
});
