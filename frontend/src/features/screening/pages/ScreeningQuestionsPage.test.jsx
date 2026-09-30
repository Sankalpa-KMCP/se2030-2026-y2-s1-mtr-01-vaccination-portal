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
import { ApiError } from '../../../shared/api/apiClient';

const apiMocks = vi.hoisted(() => ({
  listQuestions: vi.fn(),
  getQuestion: vi.fn(),
  createQuestion: vi.fn(),
  updateQuestion: vi.fn(),
  deleteQuestion: vi.fn(),
  listAnswerOptions: vi.fn(),
  getAnswerOption: vi.fn(),
  createAnswerOption: vi.fn(),
  updateAnswerOption: vi.fn(),
  deleteAnswerOption: vi.fn(),
}));

vi.mock('../api/screeningApi', () => apiMocks);

import ScreeningQuestionsPage from './ScreeningQuestionsPage';

const choiceQuestion = {
  id: 1,
  code: 'SYMPTOMS',
  text: 'Which symptoms are present?',
  description: 'Select all that apply.',
  questionType: 'MULTI_CHOICE',
  defaultRequired: true,
  active: true,
};

const textQuestion = {
  id: 2,
  code: 'NOTES',
  text: 'Additional clinical notes',
  description: null,
  questionType: 'TEXT',
  defaultRequired: false,
  active: false,
};

function renderPage() {
  return render(<ScreeningQuestionsPage />);
}

describe('ScreeningQuestionsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiMocks.listQuestions.mockResolvedValue([choiceQuestion, textQuestion]);
    apiMocks.listAnswerOptions.mockResolvedValue([]);
  });

  afterEach(() => {
    cleanup();
  });

  it('shows the shared loading state while questions are pending', () => {
    apiMocks.listQuestions.mockReturnValue(new Promise(() => {}));
    renderPage();

    expect(screen.getAllByText('Loading screening questions...')).toHaveLength(2);
  });

  it('renders the question catalogue returned by the API', async () => {
    renderPage();

    expect(await screen.findByText('Which symptoms are present?')).toBeTruthy();
    expect(screen.getByText('Additional clinical notes')).toBeTruthy();
    expect(screen.getByText('Multiple Choice')).toBeTruthy();
  });

  it('renders a clear empty state', async () => {
    apiMocks.listQuestions.mockResolvedValue([]);
    renderPage();

    expect(await screen.findByText('No screening questions have been configured yet.')).toBeTruthy();
  });

  it('renders API failures through ApiErrorAlert', async () => {
    apiMocks.listQuestions.mockRejectedValue(new ApiError('Unable to load questions', 500));
    renderPage();

    expect((await screen.findByRole('alert')).textContent).toContain('Unable to load questions');
  });

  it('creates a question from the backend DTO fields', async () => {
    apiMocks.listQuestions.mockResolvedValue([]);
    const created = {
      id: 3,
      code: 'RECENT_TRAVEL',
      text: 'Have you travelled recently?',
      description: null,
      questionType: 'BOOLEAN',
      defaultRequired: true,
      active: true,
    };
    apiMocks.createQuestion.mockResolvedValue(created);
    renderPage();

    fireEvent.click((await screen.findAllByRole('button', { name: 'Create Question' }))[0]);
    const dialog = screen.getByRole('dialog', { name: 'Create Screening Question' });
    fireEvent.change(within(dialog).getByLabelText(/Question Code/), { target: { value: 'RECENT_TRAVEL' } });
    fireEvent.change(within(dialog).getByLabelText(/Question Text/), { target: { value: 'Have you travelled recently?' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create Question' }));

    await waitFor(() => {
      expect(apiMocks.createQuestion).toHaveBeenCalledWith({
        code: 'RECENT_TRAVEL',
        text: 'Have you travelled recently?',
        description: null,
        questionType: 'BOOLEAN',
        defaultRequired: true,
      });
    });
    expect(await screen.findByText('Have you travelled recently?')).toBeTruthy();
  });

  it('edits only the mutable question fields', async () => {
    apiMocks.updateQuestion.mockResolvedValue({
      ...choiceQuestion,
      text: 'Which current symptoms are present?',
      defaultRequired: false,
    });
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Edit SYMPTOMS' }));
    const dialog = screen.getByRole('dialog', { name: 'Edit Screening Question' });
    fireEvent.change(within(dialog).getByLabelText(/Question Text/), { target: { value: 'Which current symptoms are present?' } });
    fireEvent.click(within(dialog).getByLabelText('Required by default'));
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save Changes' }));

    await waitFor(() => {
      expect(apiMocks.updateQuestion).toHaveBeenCalledWith(1, {
        text: 'Which current symptoms are present?',
        description: 'Select all that apply.',
        defaultRequired: false,
        active: true,
      });
    });
    expect(await screen.findByText('Which current symptoms are present?')).toBeTruthy();
  });

  it('requires confirmation before deleting a question', async () => {
    apiMocks.deleteQuestion.mockResolvedValue(null);
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Delete SYMPTOMS' }));
    expect(apiMocks.deleteQuestion).not.toHaveBeenCalled();

    const dialog = screen.getByRole('dialog', { name: 'Delete Screening Question' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete Question' }));

    await waitFor(() => expect(apiMocks.deleteQuestion).toHaveBeenCalledWith(1));
    expect(screen.queryByText('Which symptoms are present?')).toBeNull();
  });

  it('exposes option management only for choice questions', async () => {
    renderPage();

    expect(await screen.findByRole('button', { name: 'Manage options for SYMPTOMS' })).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Manage options for NOTES' })).toBeNull();
  });

  it('renders answer options in ascending display order', async () => {
    apiMocks.listAnswerOptions.mockResolvedValue([
      { id: 12, questionId: 1, code: 'SECOND', label: 'Second option', typedValue: 'second', displayOrder: 2 },
      { id: 11, questionId: 1, code: 'FIRST', label: 'First option', typedValue: 'first', displayOrder: 1 },
    ]);
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Manage options for SYMPTOMS' }));
    const dialog = screen.getByRole('dialog', { name: 'Answer Options' });
    await within(dialog).findByText('First option');
    const rows = within(dialog).getAllByRole('row');

    expect(within(rows[1]).getByText('First option')).toBeTruthy();
    expect(within(rows[2]).getByText('Second option')).toBeTruthy();
  });

  it('supports create, edit, and confirmed delete for answer options', async () => {
    const existing = { id: 11, questionId: 1, code: 'YES', label: 'Yes', typedValue: 'true', displayOrder: 1 };
    const created = { id: 12, questionId: 1, code: 'NO', label: 'No', typedValue: 'false', displayOrder: 2 };
    apiMocks.listAnswerOptions.mockResolvedValue([existing]);
    apiMocks.createAnswerOption.mockResolvedValue(created);
    apiMocks.updateAnswerOption.mockResolvedValue({ ...existing, label: 'Yes, confirmed' });
    apiMocks.deleteAnswerOption.mockResolvedValue(null);
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Manage options for SYMPTOMS' }));
    const dialog = screen.getByRole('dialog', { name: 'Answer Options' });
    await within(dialog).findByText('Yes');

    fireEvent.click(within(dialog).getByRole('button', { name: 'Add Option' }));
    fireEvent.change(within(dialog).getByLabelText('Code'), { target: { value: 'NO' } });
    fireEvent.change(within(dialog).getByLabelText('Label'), { target: { value: 'No' } });
    fireEvent.change(within(dialog).getByLabelText('Typed Value'), { target: { value: 'false' } });
    fireEvent.change(within(dialog).getByLabelText('Display Order'), { target: { value: '2' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create Option' }));
    await waitFor(() => expect(apiMocks.createAnswerOption).toHaveBeenCalledWith(1, {
      code: 'NO', label: 'No', typedValue: 'false', displayOrder: 2,
    }));

    fireEvent.click(within(dialog).getByRole('button', { name: 'Edit option YES' }));
    fireEvent.change(within(dialog).getByLabelText('Label'), { target: { value: 'Yes, confirmed' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save Option' }));
    await waitFor(() => expect(apiMocks.updateAnswerOption).toHaveBeenCalledWith(11, {
      code: 'YES', label: 'Yes, confirmed', typedValue: 'true', displayOrder: 1,
    }));

    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete option NO' }));
    fireEvent.click(within(dialog).getByRole('button', { name: 'Confirm' }));
    await waitFor(() => expect(apiMocks.deleteAnswerOption).toHaveBeenCalledWith(12));
  });
});
