import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';

const apiMocks = vi.hoisted(() => ({ listAnswerOptions: vi.fn() }));
vi.mock('../api/screeningApi', () => apiMocks);

import EligibilityRuleFormModal from './EligibilityRuleFormModal';

const questions = [
  { id: 1, code: 'FEVER', text: 'Do you have a fever?', questionType: 'BOOLEAN' },
  { id: 2, code: 'REACTION', text: 'Reaction severity', questionType: 'SINGLE_CHOICE' },
  { id: 3, code: 'SYMPTOMS', text: 'Select symptoms', questionType: 'MULTI_CHOICE' },
  { id: 4, code: 'AGE', text: 'Age', questionType: 'NUMERIC' },
  { id: 5, code: 'VACCINATION_DATE', text: 'Vaccination date', questionType: 'DATE' },
  { id: 6, code: 'NOTES', text: 'Notes', questionType: 'TEXT' },
];

const options = [
  { id: 20, questionId: 2, code: 'NONE', label: 'No reaction', typedValue: 'none', displayOrder: 1 },
  { id: 21, questionId: 2, code: 'SEVERE', label: 'Severe reaction', typedValue: 'severe', displayOrder: 2 },
];

function renderModal(overrides = {}) {
  const props = {
    show: true,
    rule: null,
    questions,
    processing: false,
    apiError: null,
    onClose: vi.fn(),
    onSubmit: vi.fn(),
    ...overrides,
  };
  return { ...render(<EligibilityRuleFormModal {...props} />), props };
}

function fillRuleFields(dialog) {
  fireEvent.change(within(dialog).getByLabelText(/Rule Code/), { target: { value: 'TEST_RULE' } });
  fireEvent.change(within(dialog).getByLabelText(/Rule Name/), { target: { value: 'Test rule' } });
  fireEvent.change(within(dialog).getByLabelText(/Priority/), { target: { value: '5' } });
  fireEvent.change(within(dialog).getByLabelText(/Result Message/), { target: { value: 'Healthcare review required.' } });
}

describe('EligibilityRuleFormModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiMocks.listAnswerOptions.mockResolvedValue(options);
  });

  afterEach(() => cleanup());

  it('offers attached questions only and generates a Boolean equality condition', () => {
    const attached = [questions[0]];
    const { props } = renderModal({ questions: attached });
    const dialog = screen.getByRole('dialog');
    expect(within(dialog).queryByLabelText(/Condition Expression/)).toBeNull();
    expect(within(dialog).getAllByRole('option').some((option) => option.textContent.includes('REACTION'))).toBe(false);
    fillRuleFields(dialog);
    fireEvent.change(within(dialog).getByLabelText(/Question/), { target: { value: '1' } });
    expect(within(dialog).getByLabelText(/Operator/).textContent).toContain('Equals');
    expect(within(dialog).getByLabelText(/Operator/).textContent).toContain('Not equals');
    expect(within(dialog).getByLabelText(/Value/).value).toBe('true');
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create Rule' }));
    expect(props.onSubmit).toHaveBeenCalledWith({
      ruleCode: 'TEST_RULE',
      ruleName: 'Test rule',
      conditionExpression: 'FEVER == true',
      preliminaryResult: 'PRELIMINARY_REVIEW_REQUIRED',
      priority: 5,
      message: 'Healthcare review required.',
    });
  });

  it('loads single-choice labels and generates against typedValue', async () => {
    const { props } = renderModal();
    const dialog = screen.getByRole('dialog');
    fillRuleFields(dialog);
    fireEvent.change(within(dialog).getByLabelText(/Question/), { target: { value: '2' } });
    expect(await within(dialog).findByRole('option', { name: 'Severe reaction' })).toBeTruthy();
    expect(apiMocks.listAnswerOptions).toHaveBeenCalledWith(2);
    fireEvent.change(within(dialog).getByLabelText(/Value/), { target: { value: 'severe' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create Rule' }));
    expect(props.onSubmit).toHaveBeenCalledWith(expect.objectContaining({ conditionExpression: 'REACTION == severe' }));
  });

  it('offers only Contains for multiple choice and reports ambiguous typed values', async () => {
    apiMocks.listAnswerOptions.mockResolvedValue([
      { id: 30, label: 'Pain', typedValue: 'pain' },
      { id: 31, label: 'Chest pain', typedValue: 'chest pain' },
    ]);
    renderModal();
    const dialog = screen.getByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText(/Question/), { target: { value: '3' } });
    expect(await within(dialog).findByText(/substring match would be ambiguous/i)).toBeTruthy();
    const operator = within(dialog).getByLabelText(/Operator/);
    expect(within(operator).getAllByRole('option')).toHaveLength(1);
    expect(operator.value).toBe('CONTAINS');
  });

  it('generates a safe multiple-choice Contains expression from typedValue', async () => {
    apiMocks.listAnswerOptions.mockResolvedValue([
      { id: 30, label: 'Headache', typedValue: 'headache' },
      { id: 31, label: 'Fever', typedValue: 'fever' },
    ]);
    const { props } = renderModal();
    const dialog = screen.getByRole('dialog');
    fillRuleFields(dialog);
    fireEvent.change(within(dialog).getByLabelText(/Question/), { target: { value: '3' } });
    await within(dialog).findByRole('option', { name: 'Fever' });
    fireEvent.change(within(dialog).getByLabelText(/Value/), { target: { value: 'fever' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create Rule' }));
    expect(props.onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      conditionExpression: 'SYMPTOMS CONTAINS fever',
    }));
  });

  it.each([
    ['4', 'number', 'AGE != 39'],
    ['5', 'date', 'VACCINATION_DATE != 2026-09-08'],
  ])('limits numeric/date rules to equality operators for question %s', (questionId, inputType, expected) => {
    const { props } = renderModal();
    const dialog = screen.getByRole('dialog');
    fillRuleFields(dialog);
    fireEvent.change(within(dialog).getByLabelText(/Question/), { target: { value: questionId } });
    const operator = within(dialog).getByLabelText(/Operator/);
    expect(operator.textContent).not.toMatch(/greater|less|before|after/i);
    fireEvent.change(operator, { target: { value: '!=' } });
    const value = within(dialog).getByLabelText(/Value/);
    expect(value.type).toBe(inputType);
    fireEvent.change(value, { target: { value: inputType === 'date' ? '2026-09-08' : '39' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create Rule' }));
    expect(props.onSubmit).toHaveBeenCalledWith(expect.objectContaining({ conditionExpression: expected }));
  });

  it('supports text Contains and rejects quote characters', () => {
    const { props } = renderModal();
    const dialog = screen.getByRole('dialog');
    fillRuleFields(dialog);
    fireEvent.change(within(dialog).getByLabelText(/Question/), { target: { value: '6' } });
    fireEvent.change(within(dialog).getByLabelText(/Operator/), { target: { value: 'CONTAINS' } });
    fireEvent.change(within(dialog).getByLabelText(/Value/), { target: { value: "can't" } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create Rule' }));
    expect(screen.getByRole('alert').textContent).toMatch(/quote/i);
    expect(props.onSubmit).not.toHaveBeenCalled();
  });

  it('parses and updates a supported existing expression', () => {
    const rule = {
      id: 50,
      ruleCode: 'FEVER_RULE',
      ruleName: 'Fever rule',
      conditionExpression: 'FEVER != false',
      preliminaryResult: 'PRELIMINARY_PASS',
      priority: 10,
      message: 'Fever response recorded.',
    };
    const { props } = renderModal({ rule });
    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByLabelText(/Question/).value).toBe('1');
    expect(within(dialog).getByLabelText(/Operator/).value).toBe('!=');
    expect(within(dialog).getByLabelText(/Value/).value).toBe('false');
    fireEvent.change(within(dialog).getByLabelText(/Value/), { target: { value: 'true' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save Rule' }));
    expect(props.onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      conditionExpression: 'FEVER != true',
      preliminaryResult: 'PRELIMINARY_PASS',
      priority: 10,
      message: 'Fever response recorded.',
    }));
  });

  it.each(['AGE < 18', 'AGE >= 65'])('preserves unsupported expression %s unless explicitly replaced', (expression) => {
    const rule = {
      id: 60,
      ruleCode: 'LEGACY',
      ruleName: 'Legacy rule',
      conditionExpression: expression,
      preliminaryResult: 'PRELIMINARY_REVIEW_REQUIRED',
      priority: 1,
      message: 'Legacy review.',
    };
    const { props } = renderModal({ rule });
    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText(/not supported by the current screening evaluator/i)).toBeTruthy();
    expect(within(dialog).getByText(expression)).toBeTruthy();
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save Rule' }));
    expect(props.onSubmit).toHaveBeenCalledWith(expect.objectContaining({ conditionExpression: expression }));
    fireEvent.click(within(dialog).getByRole('button', { name: 'Replace with supported condition' }));
    expect(within(dialog).getByLabelText(/Question/)).toBeTruthy();
    fireEvent.change(within(dialog).getByLabelText(/Question/), { target: { value: '1' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save Rule' }));
    expect(props.onSubmit).toHaveBeenLastCalledWith(expect.objectContaining({ conditionExpression: 'FEVER == true' }));
  });
});
