import React from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import { QUESTION_TYPE } from '../constants/screeningConstants';
import {
  ELIGIBILITY_RULE_OPERATOR_LABELS,
  getEligibilityOperators,
  validateMultiChoiceTypedValues,
} from '../utils/eligibilityRuleExpression';

function initialValueFor(question, options = []) {
  if (question?.questionType === QUESTION_TYPE.BOOLEAN) return 'true';
  if (question?.questionType === QUESTION_TYPE.SINGLE_CHOICE
      || question?.questionType === QUESTION_TYPE.MULTI_CHOICE) {
    return options[0]?.typedValue || '';
  }
  return '';
}

export default function EligibilityConditionBuilder({
  questions,
  condition,
  answerOptions = [],
  optionsLoading = false,
  optionsError = null,
  processing = false,
  onChange,
}) {
  const selectedQuestion = questions.find((question) => String(question.id) === String(condition.questionId));
  const operators = getEligibilityOperators(selectedQuestion?.questionType);
  const isChoice = selectedQuestion?.questionType === QUESTION_TYPE.SINGLE_CHOICE
    || selectedQuestion?.questionType === QUESTION_TYPE.MULTI_CHOICE;
  const multiChoiceError = selectedQuestion?.questionType === QUESTION_TYPE.MULTI_CHOICE
    ? validateMultiChoiceTypedValues(answerOptions)
    : null;

  const selectQuestion = (questionId) => {
    const question = questions.find((candidate) => String(candidate.id) === questionId);
    const nextOperators = getEligibilityOperators(question?.questionType);
    onChange({
      questionId,
      operator: nextOperators[0] || '',
      value: initialValueFor(question),
    });
  };

  const valueControl = (() => {
    if (!selectedQuestion) {
      return <input id="eligibilityConditionValue" className="form-control" value="" disabled />;
    }
    if (selectedQuestion.questionType === QUESTION_TYPE.BOOLEAN) {
      return (
        <select id="eligibilityConditionValue" className="form-select" value={condition.value} onChange={(event) => onChange({ ...condition, value: event.target.value })} disabled={processing} required>
          <option value="true">Yes</option>
          <option value="false">No</option>
        </select>
      );
    }
    if (isChoice) {
      return (
        <select id="eligibilityConditionValue" className="form-select" value={condition.value} onChange={(event) => onChange({ ...condition, value: event.target.value })} disabled={processing || optionsLoading || Boolean(optionsError) || Boolean(multiChoiceError)} required>
          <option value="">{optionsLoading ? 'Loading options…' : 'Select an answer option'}</option>
          {answerOptions.map((option) => (
            <option key={option.id ?? option.code} value={option.typedValue}>{option.label}</option>
          ))}
        </select>
      );
    }
    if (selectedQuestion.questionType === QUESTION_TYPE.NUMERIC) {
      return <input id="eligibilityConditionValue" className="form-control" type="number" step="any" value={condition.value} onChange={(event) => onChange({ ...condition, value: event.target.value })} disabled={processing} required />;
    }
    if (selectedQuestion.questionType === QUESTION_TYPE.DATE) {
      return <input id="eligibilityConditionValue" className="form-control" type="date" value={condition.value} onChange={(event) => onChange({ ...condition, value: event.target.value })} disabled={processing} required />;
    }
    return <input id="eligibilityConditionValue" className="form-control" value={condition.value} onChange={(event) => onChange({ ...condition, value: event.target.value })} disabled={processing} required />;
  })();

  return (
    <fieldset className="border rounded p-3">
      <legend className="float-none w-auto px-2 h6 mb-0">Condition</legend>
      <ApiErrorAlert error={optionsError} />
      <div className="row g-3">
        <div className="col-md-5">
          <label className="form-label fw-semibold" htmlFor="eligibilityConditionQuestion">Question <span className="text-danger">*</span></label>
          <select id="eligibilityConditionQuestion" className="form-select" value={condition.questionId} onChange={(event) => selectQuestion(event.target.value)} disabled={processing} required>
            <option value="">Select an attached question</option>
            {questions.map((question) => <option key={question.id} value={question.id}>{question.code} — {question.text}</option>)}
          </select>
        </div>
        <div className="col-md-3">
          <label className="form-label fw-semibold" htmlFor="eligibilityConditionOperator">Operator <span className="text-danger">*</span></label>
          <select id="eligibilityConditionOperator" className="form-select" value={condition.operator} onChange={(event) => onChange({ ...condition, operator: event.target.value })} disabled={processing || !selectedQuestion} required>
            {operators.map((operator) => <option key={operator} value={operator}>{ELIGIBILITY_RULE_OPERATOR_LABELS[operator]}</option>)}
          </select>
        </div>
        <div className="col-md-4">
          <label className="form-label fw-semibold" htmlFor="eligibilityConditionValue">Value <span className="text-danger">*</span></label>
          {valueControl}
        </div>
      </div>
      {selectedQuestion?.questionType === QUESTION_TYPE.MULTI_CHOICE && (
        <div className={`form-text ${multiChoiceError ? 'text-danger' : ''}`} role={multiChoiceError ? 'alert' : undefined}>
          {multiChoiceError || 'Contains uses the backend’s case-insensitive substring comparison against the selected option typed value.'}
        </div>
      )}
      {selectedQuestion?.questionType === QUESTION_TYPE.NUMERIC && <div className="form-text">The current evaluator supports exact numeric text equality only; range comparisons are unavailable.</div>}
      {selectedQuestion?.questionType === QUESTION_TYPE.DATE && <div className="form-text">Dates use YYYY-MM-DD. Before/after comparisons are unavailable.</div>}
    </fieldset>
  );
}
