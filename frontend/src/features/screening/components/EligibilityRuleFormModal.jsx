import React, { useEffect, useState } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import {
  PRELIMINARY_SCREENING_RESULT,
  PRELIMINARY_SCREENING_RESULT_LABELS,
  PRELIMINARY_SCREENING_RESULTS,
} from '../constants/screeningConstants';

const EMPTY_FORM = {
  ruleCode: '',
  ruleName: '',
  conditionExpression: '',
  preliminaryResult: PRELIMINARY_SCREENING_RESULT.REVIEW_REQUIRED,
  priority: 0,
  message: '',
};

export default function EligibilityRuleFormModal({ show, rule, processing, apiError, onClose, onSubmit }) {
  const [form, setForm] = useState(EMPTY_FORM);
  const [clientError, setClientError] = useState(null);
  const isEditing = Boolean(rule);

  useEffect(() => {
    if (!show) return;
    setForm(rule
      ? {
          ruleCode: rule.ruleCode,
          ruleName: rule.ruleName,
          conditionExpression: rule.conditionExpression,
          preliminaryResult: rule.preliminaryResult,
          priority: rule.priority,
          message: rule.message,
        }
      : EMPTY_FORM);
    setClientError(null);
  }, [rule, show]);

  if (!show) return null;

  const updateField = (field, value) => setForm((current) => ({ ...current, [field]: value }));

  const handleSubmit = (event) => {
    event.preventDefault();
    const payload = {
      ruleCode: form.ruleCode.trim(),
      ruleName: form.ruleName.trim(),
      conditionExpression: form.conditionExpression.trim(),
      preliminaryResult: form.preliminaryResult,
      priority: Number(form.priority),
      message: form.message.trim(),
    };

    if (!payload.ruleCode || !payload.ruleName || !payload.conditionExpression || !payload.message) {
      setClientError('Rule code, name, condition expression, and message are required.');
      return;
    }
    if (!Number.isInteger(payload.priority)) {
      setClientError('Priority must be a whole number.');
      return;
    }

    setClientError(null);
    onSubmit(payload);
  };

  return (
    <div className="modal show d-block" role="dialog" aria-modal="true" aria-labelledby="eligibilityRuleFormTitle" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered modal-lg">
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
                  <label className="form-label fw-semibold" htmlFor="eligibilityRuleCondition">Condition Expression <span className="text-danger">*</span></label>
                  <textarea id="eligibilityRuleCondition" className="form-control font-monospace" rows={3} value={form.conditionExpression} onChange={(event) => updateField('conditionExpression', event.target.value)} disabled={processing} required />
                  <div className="form-text">Use the expression syntax accepted by the screening backend.</div>
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
                </div>
                <div className="col-12">
                  <label className="form-label fw-semibold" htmlFor="eligibilityRuleMessage">Result Message <span className="text-danger">*</span></label>
                  <textarea id="eligibilityRuleMessage" className="form-control" rows={2} value={form.message} onChange={(event) => updateField('message', event.target.value)} maxLength={500} disabled={processing} required />
                </div>
              </div>
            </div>
            <div className="modal-footer bg-light">
              <button type="button" className="btn btn-outline-secondary" onClick={onClose} disabled={processing}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={processing}>
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
