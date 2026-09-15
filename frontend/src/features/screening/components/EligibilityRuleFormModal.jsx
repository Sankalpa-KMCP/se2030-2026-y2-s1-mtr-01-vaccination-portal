import React, { useEffect, useMemo, useState } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import { listAnswerOptions } from '../api/screeningApi';
import {
  PRELIMINARY_SCREENING_RESULT,
  PRELIMINARY_SCREENING_RESULT_LABELS,
  PRELIMINARY_SCREENING_RESULTS,
  QUESTION_TYPE,
} from '../constants/screeningConstants';
import {
  generateEligibilityRuleExpression,
  getEligibilityOperators,
  parseEligibilityRuleExpression,
  validateEligibilityRuleValue,
} from '../utils/eligibilityRuleExpression';
import EligibilityConditionBuilder from './EligibilityConditionBuilder';

const EMPTY_FORM = {
  ruleCode: '',
  ruleName: '',
  preliminaryResult: PRELIMINARY_SCREENING_RESULT.REVIEW_REQUIRED,
  priority: 0,
  message: '',
};

const EMPTY_CONDITION = { questionId: '', operator: '', value: '' };

export default function EligibilityRuleFormModal({ show, rule, questions = [], processing, apiError, onClose, onSubmit }) {
  const [form, setForm] = useState(EMPTY_FORM);
  const [condition, setCondition] = useState(EMPTY_CONDITION);
  const [unsupportedExpression, setUnsupportedExpression] = useState(false);
  const [replacingExpression, setReplacingExpression] = useState(false);
  const [answerOptions, setAnswerOptions] = useState([]);
  const [optionsLoading, setOptionsLoading] = useState(false);
  const [optionsError, setOptionsError] = useState(null);
  const [clientError, setClientError] = useState(null);
  const isEditing = Boolean(rule);

  const questionByCode = useMemo(() => new Map(
    questions.map((question) => [question.code.trim().toUpperCase(), question]),
  ), [questions]);
  const selectedQuestion = questions.find((question) => String(question.id) === String(condition.questionId));
  const choiceQuestion = selectedQuestion?.questionType === QUESTION_TYPE.SINGLE_CHOICE
    || selectedQuestion?.questionType === QUESTION_TYPE.MULTI_CHOICE;

  useEffect(() => {
    if (!show) return;

    setForm(rule
      ? {
          ruleCode: rule.ruleCode,
          ruleName: rule.ruleName,
          preliminaryResult: rule.preliminaryResult,
          priority: rule.priority,
          message: rule.message,
        }
      : EMPTY_FORM);
    setClientError(null);
    setOptionsError(null);
    setAnswerOptions([]);
    setReplacingExpression(false);

    const parsed = rule ? parseEligibilityRuleExpression(rule.conditionExpression) : null;
    const question = parsed ? questionByCode.get(parsed.questionCode.trim().toUpperCase()) : null;
    const supported = Boolean(question && getEligibilityOperators(question.questionType).includes(parsed.operator));
    setUnsupportedExpression(Boolean(rule && !supported));
    setCondition(supported
      ? { questionId: String(question.id), operator: parsed.operator, value: parsed.value }
      : EMPTY_CONDITION);
  }, [questionByCode, rule, show]);

  useEffect(() => {
    if (!show || !selectedQuestion || !choiceQuestion) {
      setAnswerOptions([]);
      setOptionsError(null);
      setOptionsLoading(false);
      return undefined;
    }

    let active = true;
    setOptionsLoading(true);
    setOptionsError(null);
    listAnswerOptions(selectedQuestion.id)
      .then((data) => {
        if (active) setAnswerOptions(Array.isArray(data) ? data : []);
      })
      .catch((error) => {
        if (active) setOptionsError(error);
      })
      .finally(() => {
        if (active) setOptionsLoading(false);
      });

    return () => {
      active = false;
    };
  }, [choiceQuestion, selectedQuestion, show]);

  if (!show) return null;

  const updateField = (field, value) => setForm((current) => ({ ...current, [field]: value }));

  const handleSubmit = (event) => {
    event.preventDefault();
    const preserveUnsupported = unsupportedExpression && !replacingExpression;
    const payload = {
      ruleCode: form.ruleCode.trim(),
      ruleName: form.ruleName.trim(),
      conditionExpression: preserveUnsupported ? rule.conditionExpression : '',
      preliminaryResult: form.preliminaryResult,
      priority: Number(form.priority),
      message: form.message.trim(),
    };

    if (!payload.ruleCode || !payload.ruleName || !payload.message) {
      setClientError('Rule code, name, and result message are required.');
      return;
    }
    if (!Number.isInteger(payload.priority)) {
      setClientError('Priority must be a whole number.');
      return;
    }

    if (!preserveUnsupported) {
      if (!selectedQuestion || !getEligibilityOperators(selectedQuestion.questionType).includes(condition.operator)) {
        setClientError('Choose an attached question and a supported operator.');
        return;
      }
      if (choiceQuestion && (optionsLoading || optionsError)) {
        setClientError('Answer options must load successfully before this rule can be saved.');
        return;
      }
      const valueError = validateEligibilityRuleValue(selectedQuestion.questionType, condition.value, answerOptions);
      if (valueError) {
        setClientError(valueError);
        return;
      }
      try {
        payload.conditionExpression = generateEligibilityRuleExpression({
          questionCode: selectedQuestion.code,
          operator: condition.operator,
          value: condition.value,
        });
      } catch (error) {
        setClientError(error.message);
        return;
      }
    }

    setClientError(null);
    onSubmit(payload);
  };

  return (
    <div className="modal show d-block" role="dialog" aria-modal="true" aria-labelledby="eligibilityRuleFormTitle" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered modal-lg modal-dialog-scrollable">
        <div className="modal-content shadow-lg border-0">
          <div className="modal-header bg-light">
            <h2 className="modal-title h5" id="eligibilityRuleFormTitle">{isEditing ? 'Edit Eligibility Rule' : 'Create Eligibility Rule'}</h2>
            <button type="button" className="btn-close" aria-label="Close" onClick={onClose} disabled={processing} />
          </div>
          <form onSubmit={handleSubmit} noValidate>
            <div className="modal-body p-4">
              <ApiErrorAlert error={clientError || apiError} />
              <div className="row g-3">
                <div className="col-md-5">
                  <label className="form-label fw-semibold" htmlFor="eligibilityRuleCode">Rule Code <span className="text-danger">*</span></label>
                  <input id="eligibilityRuleCode" className="form-control font-monospace" value={form.ruleCode} onChange={(event) => updateField('ruleCode', event.target.value)} maxLength={64} disabled={processing} required />
                </div>
                <div className="col-md-7">
                  <label className="form-label fw-semibold" htmlFor="eligibilityRuleName">Rule Name <span className="text-danger">*</span></label>
                  <input id="eligibilityRuleName" className="form-control" value={form.ruleName} onChange={(event) => updateField('ruleName', event.target.value)} maxLength={255} disabled={processing} required />
                </div>

                <div className="col-12">
                  {unsupportedExpression && !replacingExpression ? (
                    <div className="alert alert-warning mb-0" role="alert">
                      <p className="fw-semibold mb-1">This existing condition uses expression syntax that is not supported by the current screening evaluator.</p>
                      <p className="mb-2">It will remain unchanged unless you explicitly replace it with a supported condition.</p>
                      <div className="bg-white border rounded p-2 mb-3"><code>{rule.conditionExpression}</code></div>
                      <button type="button" className="btn btn-outline-warning btn-sm" onClick={() => { setReplacingExpression(true); setClientError(null); }} disabled={processing}>Replace with supported condition</button>
                    </div>
                  ) : (
                    <EligibilityConditionBuilder
                      questions={questions}
                      condition={condition}
                      answerOptions={answerOptions}
                      optionsLoading={optionsLoading}
                      optionsError={optionsError}
                      processing={processing}
                      onChange={(next) => { setCondition(next); setClientError(null); }}
                    />
                  )}
                </div>

                <div className="col-md-8">
                  <label className="form-label fw-semibold" htmlFor="eligibilityRuleResult">Preliminary Result <span className="text-danger">*</span></label>
                  <select id="eligibilityRuleResult" className="form-select" value={form.preliminaryResult} onChange={(event) => updateField('preliminaryResult', event.target.value)} disabled={processing} required>
                    {PRELIMINARY_SCREENING_RESULTS.map((result) => <option key={result} value={result}>{PRELIMINARY_SCREENING_RESULT_LABELS[result]}</option>)}
                  </select>
                </div>
                <div className="col-md-4">
                  <label className="form-label fw-semibold" htmlFor="eligibilityRulePriority">Priority <span className="text-danger">*</span></label>
                  <input id="eligibilityRulePriority" className="form-control" type="number" step="1" value={form.priority} onChange={(event) => updateField('priority', event.target.value)} disabled={processing} required />
                  <div className="form-text">Lower priority numbers are evaluated first.</div>
                </div>
                <div className="col-12">
                  <label className="form-label fw-semibold" htmlFor="eligibilityRuleMessage">Result Message <span className="text-danger">*</span></label>
                  <textarea id="eligibilityRuleMessage" className="form-control" rows={2} value={form.message} onChange={(event) => updateField('message', event.target.value)} maxLength={500} disabled={processing} required />
                  <div className="form-text">This human-readable reason is recorded when the rule triggers.</div>
                </div>
              </div>
            </div>
            <div className="modal-footer bg-light">
              <button type="button" className="btn btn-outline-secondary" onClick={onClose} disabled={processing}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={processing || optionsLoading}>
                {processing && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}
                {isEditing ? 'Save Rule' : 'Create Rule'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
