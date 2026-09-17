import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import AddQuestionnaireItemModal from './AddQuestionnaireItemModal';

const questions = [
  { id: 1, code: 'FEVER', text: 'Do you have a fever?', questionType: 'BOOLEAN' },
  { id: 2, code: 'AGE', text: 'What is your age?', questionType: 'NUMERIC' },
];

function renderModal(overrides = {}) {
  const props = {
    show: true,
    questions,
    suggestedOrder: 4,
    processing: false,
    apiError: null,
    onClose: vi.fn(),
    onSubmit: vi.fn(),
    onCreateAndAttach: vi.fn(),
    ...overrides,
  };
  return { ...render(<AddQuestionnaireItemModal {...props} />), props };
}

function chooseCreate() {
  fireEvent.click(screen.getByRole('button', { name: 'Create New' }));
}

function fillMetadata(type = 'BOOLEAN') {
  fireEvent.change(screen.getByLabelText(/Question Code/), { target: { value: 'NEW_CODE' } });
  fireEvent.change(screen.getByLabelText(/Question Text/), { target: { value: 'New question?' } });
  fireEvent.change(screen.getByLabelText(/Question Type/), { target: { value: type } });
}

describe('AddQuestionnaireItemModal', () => {
  afterEach(() => cleanup());

  it('defaults to Select Existing and preserves its attachment contract', () => {
    const { props } = renderModal();
    expect(screen.getByRole('button', { name: 'Select Existing' }).getAttribute('aria-pressed')).toBe('true');
    expect(screen.getByRole('option', { name: /FEVER/ })).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Required Override'), { target: { value: 'true' } });
    fireEvent.click(screen.getByRole('button', { name: 'Add Question' }));
    expect(props.onSubmit).toHaveBeenCalledWith({ questionId: 1, displayOrder: 4, requiredOverride: true });
  });

  it.each(['BOOLEAN', 'NUMERIC', 'DATE', 'TEXT'])('does not show answer options for %s', (type) => {
    renderModal();
    chooseCreate();
    fillMetadata(type);
    expect(screen.queryByText('Answer Options')).toBeNull();
  });

  it.each(['SINGLE_CHOICE', 'MULTI_CHOICE'])('supports local add/remove option rows for %s', (type) => {
    renderModal();
    chooseCreate();
    fillMetadata(type);
    expect(screen.getAllByTestId('new-question-option-row')).toHaveLength(1);
    fireEvent.click(screen.getByRole('button', { name: 'Add Option' }));
    expect(screen.getAllByTestId('new-question-option-row')).toHaveLength(2);
    fireEvent.click(screen.getByRole('button', { name: 'Remove option 2' }));
    expect(screen.getAllByTestId('new-question-option-row')).toHaveLength(1);
  });

  it('validates metadata and clears mode-specific errors when switching modes', () => {
    renderModal();
    chooseCreate();
    fireEvent.click(screen.getByRole('button', { name: 'Create and Add Question' }));
    expect(screen.getByRole('alert').textContent).toContain('Question code is required');
    fireEvent.click(screen.getByRole('button', { name: 'Select Existing' }));
    expect(screen.queryByRole('alert')).toBeNull();
  });

  it('validates choice option metadata before invoking orchestration', () => {
    const { props } = renderModal();
    chooseCreate();
    fillMetadata('SINGLE_CHOICE');
    fireEvent.click(screen.getByRole('button', { name: 'Create and Add Question' }));
    expect(screen.getByRole('alert').textContent).toContain('Every answer option requires');
    expect(props.onCreateAndAttach).not.toHaveBeenCalled();
  });

  it('submits normalized question, ordered options, and required override', () => {
    const { props } = renderModal();
    chooseCreate();
    fillMetadata('MULTI_CHOICE');
    const first = screen.getByTestId('new-question-option-row');
    fireEvent.change(within(first).getByLabelText(/Option Code/), { target: { value: ' YES ' } });
    fireEvent.change(within(first).getByLabelText(/^Label/), { target: { value: ' Yes ' } });
    fireEvent.change(within(first).getByLabelText(/Typed Value/), { target: { value: ' true ' } });
    fireEvent.change(screen.getByLabelText('Required Override'), { target: { value: 'false' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create and Add Question' }));

    expect(props.onCreateAndAttach).toHaveBeenCalledWith({
      question: {
        code: 'NEW_CODE',
        text: 'New question?',
        description: null,
        questionType: 'MULTI_CHOICE',
        defaultRequired: true,
      },
      options: [{ code: 'YES', label: 'Yes', typedValue: 'true', displayOrder: 1 }],
      attachment: { displayOrder: 4, requiredOverride: false },
    });
  });

  it('shows saved progress and prevents changing already-created metadata', () => {
    renderModal({
      creationProgress: {
        question: { id: 9, code: 'CREATED', questionType: 'SINGLE_CHOICE' },
        options: [{ code: 'YES', label: 'Yes', typedValue: 'true', displayOrder: 1 }],
      },
    });
    chooseCreate();
    expect(screen.getByText(/now exists in the global catalogue/i)).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Select Existing' }).disabled).toBe(true);
    expect(screen.getByLabelText(/Question Code/).closest('fieldset').disabled).toBe(true);
  });

  describe('Display Order validation', () => {
    it('modal starts with Display Order = 1 when opened without explicit order', () => {
      renderModal({ suggestedOrder: undefined });
      const orderInput = screen.getByLabelText('Display Order *');
      expect(orderInput.value).toBe('1');
      expect(orderInput.getAttribute('min')).toBe('1');
      expect(orderInput.getAttribute('step')).toBe('1');
    });

    it('modal starts with Display Order = 1 when suggestedOrder is 1', () => {
      renderModal({ suggestedOrder: 1 });
      const orderInput = screen.getByLabelText('Display Order *');
      expect(orderInput.value).toBe('1');
    });

    it('Display Order 1 is accepted', () => {
      const { props } = renderModal({ suggestedOrder: 1 });
      fireEvent.click(screen.getByRole('button', { name: 'Add Question' }));
      expect(props.onSubmit).toHaveBeenCalledWith({
        questionId: 1,
        displayOrder: 1,
        requiredOverride: null,
      });
    });

    it.each([2, 3])('positive integer %i is accepted', (order) => {
      const { props } = renderModal({ suggestedOrder: 1 });
      fireEvent.change(screen.getByLabelText('Display Order *'), { target: { value: String(order) } });
      fireEvent.click(screen.getByRole('button', { name: 'Add Question' }));
      expect(props.onSubmit).toHaveBeenCalledWith({
        questionId: 1,
        displayOrder: order,
        requiredOverride: null,
      });
    });

    it('0 is rejected and prevents submission', () => {
      const { props } = renderModal();
      fireEvent.change(screen.getByLabelText('Display Order *'), { target: { value: '0' } });
      fireEvent.click(screen.getByRole('button', { name: 'Add Question' }));
      expect(screen.getByRole('alert').textContent).toContain('Display order must be a whole number of 1 or greater.');
      expect(props.onSubmit).not.toHaveBeenCalled();
    });

    it('-1 is rejected and prevents submission', () => {
      const { props } = renderModal();
      fireEvent.change(screen.getByLabelText('Display Order *'), { target: { value: '-1' } });
      fireEvent.click(screen.getByRole('button', { name: 'Add Question' }));
      expect(screen.getByRole('alert').textContent).toContain('Display order must be a whole number of 1 or greater.');
      expect(props.onSubmit).not.toHaveBeenCalled();
    });

    it('negative number -5 cannot be submitted', () => {
      const { props } = renderModal();
      fireEvent.change(screen.getByLabelText('Display Order *'), { target: { value: '-5' } });
      fireEvent.click(screen.getByRole('button', { name: 'Add Question' }));
      expect(screen.getByRole('alert').textContent).toContain('Display order must be a whole number of 1 or greater.');
      expect(props.onSubmit).not.toHaveBeenCalled();
    });

    it('decimal/non-integer input is rejected and prevents submission', () => {
      const { props } = renderModal();
      fireEvent.change(screen.getByLabelText('Display Order *'), { target: { value: '1.5' } });
      fireEvent.click(screen.getByRole('button', { name: 'Add Question' }));
      expect(screen.getByRole('alert').textContent).toContain('Display order must be a whole number of 1 or greater.');
      expect(props.onSubmit).not.toHaveBeenCalled();
    });

    it('blank display order is rejected and prevents submission', () => {
      const { props } = renderModal();
      fireEvent.change(screen.getByLabelText('Display Order *'), { target: { value: '' } });
      fireEvent.click(screen.getByRole('button', { name: 'Add Question' }));
      expect(screen.getByRole('alert').textContent).toContain('Display order must be a whole number of 1 or greater.');
      expect(props.onSubmit).not.toHaveBeenCalled();
    });

    it('display order < 1 in Create New mode prevents onCreateAndAttach API call', () => {
      const { props } = renderModal();
      chooseCreate();
      fillMetadata('BOOLEAN');
      fireEvent.change(screen.getByLabelText('Display Order *'), { target: { value: '0' } });
      fireEvent.click(screen.getByRole('button', { name: 'Create and Add Question' }));
      expect(screen.getByRole('alert').textContent).toContain('Display order must be a whole number of 1 or greater.');
      expect(props.onCreateAndAttach).not.toHaveBeenCalled();
    });
  });
});
