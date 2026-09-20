import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import AnswerOptionsModal from './AnswerOptionsModal';

const apiMocks = vi.hoisted(() => ({
  listAnswerOptions: vi.fn(),
  createAnswerOption: vi.fn(),
  updateAnswerOption: vi.fn(),
  deleteAnswerOption: vi.fn(),
}));

vi.mock('../api/screeningApi', () => apiMocks);

const baseQuestion = {
  id: 1,
  code: 'SYMPTOMS',
  text: 'Select your symptoms',
  questionType: 'MULTI_CHOICE',
};

describe('AnswerOptionsModal', () => {
  afterEach(() => {
    cleanup();
  });

  beforeEach(() => {
    vi.resetAllMocks();
    let nextId = 100;
    apiMocks.listAnswerOptions.mockResolvedValue([]);
    apiMocks.createAnswerOption.mockImplementation((questionId, payload) =>
      Promise.resolve({ id: nextId++, questionId, ...payload })
    );
    apiMocks.updateAnswerOption.mockImplementation((optionId, payload) =>
      Promise.resolve({ id: optionId, questionId: 1, ...payload })
    );
    apiMocks.deleteAnswerOption.mockResolvedValue(null);
  });

  const renderModal = (props = {}) => render(
    <AnswerOptionsModal question={baseQuestion} onClose={vi.fn()} {...props} />
  );

  it('allows HCO to create an answer option by entering ONLY Label with auto-generated fields', async () => {
    renderModal();
    expect(await screen.findByText('No answer options have been configured for this question.')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Add Option' }));

    // Only Label input is required for the HCO
    const labelInput = screen.getByLabelText('Label');
    fireEvent.change(labelInput, { target: { value: 'No symptoms' } });

    // Submit the form
    fireEvent.click(screen.getByRole('button', { name: 'Create Option' }));

    await waitFor(() => {
      expect(apiMocks.createAnswerOption).toHaveBeenCalledWith(1, {
        code: 'NO_SYMPTOMS',
        label: 'No symptoms',
        typedValue: 'No symptoms',
        displayOrder: 1,
      });
    });

    expect(await screen.findByText('Answer option created.')).toBeTruthy();
  });

  it('increments display order starting at 1 based on max(existing displayOrder) + 1', async () => {
    apiMocks.listAnswerOptions.mockResolvedValue([
      { id: 10, questionId: 1, code: 'FEVER', label: 'Fever', typedValue: 'Fever', displayOrder: 1 },
      { id: 11, questionId: 1, code: 'COUGH', label: 'Cough', typedValue: 'Cough', displayOrder: 5 },
    ]);
    renderModal();

    expect(await screen.findByText('Fever')).toBeTruthy();
    expect(screen.getByText('Cough')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Add Option' }));
    fireEvent.change(screen.getByLabelText('Label'), { target: { value: 'Headache' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create Option' }));

    await waitFor(() => {
      expect(apiMocks.createAnswerOption).toHaveBeenCalledWith(1, {
        code: 'HEADACHE',
        label: 'Headache',
        typedValue: 'Headache',
        displayOrder: 6, // max(1, 5) + 1 = 6
      });
    });
  });

  it('handles code conflicts by appending a deterministic suffix', async () => {
    apiMocks.listAnswerOptions.mockResolvedValue([
      { id: 10, questionId: 1, code: 'OTHER', label: 'Other symptoms', typedValue: 'Other', displayOrder: 1 },
    ]);
    renderModal();

    expect(await screen.findByText('Other symptoms')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Add Option' }));
    fireEvent.change(screen.getByLabelText('Label'), { target: { value: 'Other' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create Option' }));

    await waitFor(() => {
      expect(apiMocks.createAnswerOption).toHaveBeenCalledWith(1, {
        code: 'OTHER_2',
        label: 'Other',
        typedValue: 'Other',
        displayOrder: 2,
      });
    });
  });

  it('shows friendly inline validation for duplicate labels', async () => {
    apiMocks.listAnswerOptions.mockResolvedValue([
      { id: 10, questionId: 1, code: 'FEVER', label: 'Fever', typedValue: 'Fever', displayOrder: 1 },
    ]);
    renderModal();

    expect(await screen.findByText('Fever')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Add Option' }));
    fireEvent.change(screen.getByLabelText('Label'), { target: { value: '  fever  ' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create Option' }));

    expect(await screen.findByText('An option with this label already exists.')).toBeTruthy();
    expect(apiMocks.createAnswerOption).not.toHaveBeenCalled();
  });

  it('shows friendly inline validation for blank label', async () => {
    renderModal();
    await screen.findByText('No answer options have been configured for this question.');

    fireEvent.click(screen.getByRole('button', { name: 'Add Option' }));
    fireEvent.change(screen.getByLabelText('Label'), { target: { value: '   ' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create Option' }));

    expect(await screen.findByText('Option label is required.')).toBeTruthy();
    expect(apiMocks.createAnswerOption).not.toHaveBeenCalled();
  });

  it('allows customizing Code, Typed Value, and Display Order when Advanced Settings is expanded', async () => {
    renderModal();
    await screen.findByText('No answer options have been configured for this question.');

    fireEvent.click(screen.getByRole('button', { name: 'Add Option' }));
    fireEvent.change(screen.getByLabelText('Label'), { target: { value: 'No symptoms' } });

    // Expand Advanced Settings
    const advancedToggle = screen.getByRole('button', { name: /Advanced settings/ });
    expect(advancedToggle.getAttribute('aria-expanded')).toBe('false');
    fireEvent.click(advancedToggle);
    expect(advancedToggle.getAttribute('aria-expanded')).toBe('true');

    // Override advanced values
    fireEvent.change(screen.getByLabelText('Code'), { target: { value: 'CUSTOM_CODE' } });
    fireEvent.change(screen.getByLabelText('Typed Value'), { target: { value: 'none' } });
    fireEvent.change(screen.getByLabelText('Display Order'), { target: { value: '10' } });

    fireEvent.click(screen.getByRole('button', { name: 'Create Option' }));

    await waitFor(() => {
      expect(apiMocks.createAnswerOption).toHaveBeenCalledWith(1, {
        code: 'CUSTOM_CODE',
        label: 'No symptoms',
        typedValue: 'none',
        displayOrder: 10,
      });
    });
  });

  it('preserves existing code and typed value when editing an option label', async () => {
    const existing = { id: 10, questionId: 1, code: 'OLD_CODE', label: 'Old Label', typedValue: 'old_value', displayOrder: 1 };
    apiMocks.listAnswerOptions.mockResolvedValue([existing]);
    renderModal();

    expect(await screen.findByText('Old Label')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Edit option OLD_CODE' }));

    // Change label only
    fireEvent.change(screen.getByLabelText('Label'), { target: { value: 'New Label' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save Option' }));

    await waitFor(() => {
      expect(apiMocks.updateAnswerOption).toHaveBeenCalledWith(10, {
        code: 'OLD_CODE', // Must NOT be overwritten to NEW_LABEL
        label: 'New Label',
        typedValue: 'old_value', // Must NOT be overwritten to New Label
        displayOrder: 1,
      });
    });
  });

  it('supports confirmed option deletion', async () => {
    const existing = { id: 10, questionId: 1, code: 'FEVER', label: 'Fever', typedValue: 'Fever', displayOrder: 1 };
    apiMocks.listAnswerOptions.mockResolvedValue([existing]);
    renderModal();

    expect(await screen.findByText('Fever')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Delete option FEVER' }));
    expect(screen.getByText('Delete?')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Confirm' }));

    await waitFor(() => {
      expect(apiMocks.deleteAnswerOption).toHaveBeenCalledWith(10);
    });
    expect(await screen.findByText('Answer option deleted.')).toBeTruthy();
  });

  it('supports reordering options with Move Up and Move Down buttons', async () => {
    const optionA = { id: 10, questionId: 1, code: 'A', label: 'Option A', typedValue: 'A', displayOrder: 1 };
    const optionB = { id: 20, questionId: 1, code: 'B', label: 'Option B', typedValue: 'B', displayOrder: 2 };
    apiMocks.listAnswerOptions.mockResolvedValue([optionA, optionB]);
    renderModal();

    expect(await screen.findByText('Option A')).toBeTruthy();

    // First item cannot move up
    expect(screen.getByRole('button', { name: 'Move up Option A' }).hasAttribute('disabled')).toBe(true);

    // Move Option A down (swaps with Option B)
    fireEvent.click(screen.getByRole('button', { name: 'Move down Option A' }));

    await waitFor(() => {
      expect(apiMocks.updateAnswerOption).toHaveBeenCalledWith(20, {
        code: 'B', label: 'Option B', typedValue: 'B', displayOrder: 1,
      });
      expect(apiMocks.updateAnswerOption).toHaveBeenCalledWith(10, {
        code: 'A', label: 'Option A', typedValue: 'A', displayOrder: 2,
      });
    });
    expect(await screen.findByText('Option order updated.')).toBeTruthy();
  });

  it('supports Add Yes / No Options template for empty choice questions', async () => {
    renderModal();
    expect(await screen.findByText('No answer options have been configured for this question.')).toBeTruthy();

    const yesNoBtn = screen.getByRole('button', { name: '+ Add Yes / No Options' });
    fireEvent.click(yesNoBtn);

    await waitFor(() => {
      expect(apiMocks.createAnswerOption).toHaveBeenCalledWith(1, {
        code: 'YES',
        label: 'Yes',
        typedValue: 'Yes',
        displayOrder: 1,
      });
      expect(apiMocks.createAnswerOption).toHaveBeenCalledWith(1, {
        code: 'NO',
        label: 'No',
        typedValue: 'No',
        displayOrder: 2,
      });
    });

    expect(await screen.findByText('Added Yes and No options.')).toBeTruthy();
  });

  it('supports Add Multiple options via multiline input', async () => {
    renderModal();
    await screen.findByText('No answer options have been configured for this question.');

    fireEvent.click(screen.getByRole('button', { name: 'Add Multiple' }));

    const textarea = screen.getByLabelText('Option labels per line');
    fireEvent.change(textarea, {
      target: { value: 'No symptoms\nHigh fever\nDry cough' },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Add Options' }));

    await waitFor(() => {
      expect(apiMocks.createAnswerOption).toHaveBeenCalledWith(1, {
        code: 'NO_SYMPTOMS',
        label: 'No symptoms',
        typedValue: 'No symptoms',
        displayOrder: 1,
      });
      expect(apiMocks.createAnswerOption).toHaveBeenCalledWith(1, {
        code: 'HIGH_FEVER',
        label: 'High fever',
        typedValue: 'High fever',
        displayOrder: 2,
      });
      expect(apiMocks.createAnswerOption).toHaveBeenCalledWith(1, {
        code: 'DRY_COUGH',
        label: 'Dry cough',
        typedValue: 'Dry cough',
        displayOrder: 3,
      });
    });

    expect(await screen.findByText('Added 3 answer options.')).toBeTruthy();
  });

  it('halts multiple option creation safely when an API error occurs and reports partial progress', async () => {
    apiMocks.createAnswerOption
      .mockResolvedValueOnce({ id: 101, questionId: 1, code: 'NO_SYMPTOMS', label: 'No symptoms', typedValue: 'No symptoms', displayOrder: 1 })
      .mockRejectedValueOnce(new Error('Network error on second item'));

    renderModal();
    await screen.findByText('No answer options have been configured for this question.');

    fireEvent.click(screen.getByRole('button', { name: 'Add Multiple' }));
    fireEvent.change(screen.getByLabelText('Option labels per line'), {
      target: { value: 'No symptoms\nFever\nCough' },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Add Options' }));

    expect(await screen.findByText(/Created 1 options before error: Network error on second item/)).toBeTruthy();
    // Third option should not have been attempted
    expect(apiMocks.createAnswerOption).toHaveBeenCalledTimes(2);
    // First option was added to table
    expect(screen.getByText('No symptoms')).toBeTruthy();
  });
});
