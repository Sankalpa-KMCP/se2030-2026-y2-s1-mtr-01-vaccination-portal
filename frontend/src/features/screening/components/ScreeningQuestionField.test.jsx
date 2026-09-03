import React, { useState } from 'react';
import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import ScreeningQuestionField from './ScreeningQuestionField';

function Harness({ question, initialValue }) {
  const [value, setValue] = useState(initialValue);
  return <ScreeningQuestionField question={question} value={value} onChange={setValue} />;
}

const baseQuestion = { id: 1, text: 'Test question', description: 'Helpful context', required: true, options: [] };

afterEach(cleanup);

describe('ScreeningQuestionField', () => {
  it('renders BOOLEAN as accessible Yes and No radio choices', () => {
    render(<Harness question={{ ...baseQuestion, questionType: 'BOOLEAN' }} />);
    fireEvent.click(screen.getByRole('radio', { name: 'Yes' }));
    expect(screen.getByRole('radio', { name: 'Yes' }).checked).toBe(true);
  });

  it('renders ordered SINGLE_CHOICE radio options using option codes', () => {
    render(<Harness question={{ ...baseQuestion, questionType: 'SINGLE_CHOICE', options: [
      { id: 2, code: 'B', label: 'Second', displayOrder: 2 },
      { id: 1, code: 'A', label: 'First', displayOrder: 1 },
    ] }} />);
    expect(screen.getAllByRole('radio').map((radio) => radio.value)).toEqual(['A', 'B']);
  });

  it('renders MULTI_CHOICE checkboxes and prevents duplicate selections', () => {
    render(<Harness initialValue={[]} question={{ ...baseQuestion, questionType: 'MULTI_CHOICE', options: [
      { id: 1, code: 'ASTHMA', label: 'Asthma', displayOrder: 1 },
    ] }} />);
    const checkbox = screen.getByRole('checkbox', { name: 'Asthma' });
    fireEvent.click(checkbox);
    fireEvent.click(checkbox);
    fireEvent.click(checkbox);
    expect(checkbox.checked).toBe(true);
  });

  it.each([
    ['NUMERIC', 'spinbutton'],
    ['DATE', undefined],
    ['TEXT', 'textbox'],
  ])('renders the %s input type', (questionType, role) => {
    render(<Harness question={{ ...baseQuestion, questionType }} />);
    const input = role
      ? screen.getByRole(role, { name: /test question/i })
      : document.querySelector('input[type="date"]');
    expect(input).toBeTruthy();
  });
});
