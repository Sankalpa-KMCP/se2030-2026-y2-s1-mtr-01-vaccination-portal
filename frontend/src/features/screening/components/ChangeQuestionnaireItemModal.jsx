import React, { useEffect, useMemo, useState } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import { parseEligibilityRuleExpression } from '../utils/eligibilityRuleExpression';

const REQUIRED_OVERRIDE = Object.freeze({
  DEFAULT: '',
  REQUIRED: 'true',
  OPTIONAL: 'false',
});

export default function ChangeQuestionnaireItemModal({
  show,
  item,
  currentQuestion,
  availableQuestions = [],
  rules = [],
  processing = false,
  apiError = null,
  onClose,
  onSubmit,
}) {
  const [selectedQuestionId, setSelectedQuestionId] = useState('');
  const [requiredOverride, setRequiredOverride] = useState(REQUIRED_OVERRIDE.DEFAULT);
  const [clientError, setClientError] = useState(null);

  useEffect(() => {
    if (show && item) {
      setSelectedQuestionId(item.questionId ? String(item.questionId) : '');
      if (item.requiredOverride === null || item.requiredOverride === undefined) {
        setRequiredOverride(REQUIRED_OVERRIDE.DEFAULT);
      } else {
        setRequiredOverride(item.requiredOverride ? REQUIRED_OVERRIDE.REQUIRED : REQUIRED_OVERRIDE.OPTIONAL);
      }
      setClientError(null);
    }
  }, [show, item]);

  const candidateQuestions = useMemo(() => {
    if (!currentQuestion) return availableQuestions;
    const exists = availableQuestions.some((q) => q.id === currentQuestion.id);
    if (exists) return availableQuestions;
    return [currentQuestion, ...availableQuestions];
  }, [availableQuestions, currentQuestion]);

  const referencingRule = useMemo(() => {
    if (!currentQuestion?.code || !Array.isArray(rules)) return null;
    const targetCode = currentQuestion.code.trim().toUpperCase();
    return rules.find((rule) => {
      const parsed = parseEligibilityRuleExpression(rule.conditionExpression);
      return parsed?.questionCode?.toUpperCase() === targetCode;
    }) || null;
  }, [currentQuestion, rules]);

  if (!show || !item) return null;

  const handleSubmit = (event) => {
    event.preventDefault();
    if (!selectedQuestionId) {
      setClientError('Please select a replacement question.');
      return;
    }

    setClientError(null);
    const parsedQuestionId = Number(selectedQuestionId);
    const parsedOverride = requiredOverride === REQUIRED_OVERRIDE.DEFAULT
      ? null
      : requiredOverride === REQUIRED_OVERRIDE.REQUIRED;

    onSubmit({
      questionId: parsedQuestionId,
      requiredOverride: parsedOverride,
    });
  };

  return (
    <div
      className="modal show d-block"
      tabIndex="-1"
      role="dialog"
      aria-modal="true"
      aria-labelledby="changeQuestionModalTitle"
      style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }}
    >
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content shadow">
          <div className="modal-header bg-light">
            <h2 className="modal-title h5 mb-0" id="changeQuestionModalTitle">
              Change Question
            </h2>
            <button
              type="button"
              className="btn-close"
              aria-label="Close"
              disabled={processing}
              onClick={onClose}
            />
          </div>

          <form onSubmit={handleSubmit} noValidate>
            <div className="modal-body p-4">
              <ApiErrorAlert error={apiError} onDismiss={() => {}} />

              {clientError && (
                <div className="alert alert-danger py-2 mb-3" role="alert">
                  {clientError}
                </div>
              )}

              {referencingRule && (
                <div className="alert alert-warning py-2 mb-3" role="alert">
                  <div className="fw-semibold">Rule Dependency Notice:</div>
                  <div className="small">
                    This question is currently referenced by eligibility rule{' '}
                    <strong>{referencingRule.ruleCode}</strong>.
                    Replacing it will be blocked by the server until that rule is updated or removed.
                  </div>
                </div>
              )}

              {/* Current Question Display */}
              <div className="mb-3 p-3 bg-light rounded border">
                <div className="small text-muted mb-1">
                  Current Question (Position #{item.displayOrder}):
                </div>
                <div className="fw-semibold">
                  {currentQuestion?.text || `Question #${item.questionId}`}
                </div>
                <div className="font-monospace small text-muted">
                  Code: {currentQuestion?.code || '—'}
                </div>
              </div>

              {/* Replacement Question Selector */}
              <div className="mb-3">
                <label className="form-label fw-semibold" htmlFor="replacementQuestionSelect">
                  Replacement Question <span className="text-danger">*</span>
                </label>
                <select
                  id="replacementQuestionSelect"
                  className="form-select"
                  value={selectedQuestionId}
                  onChange={(e) => {
                    setSelectedQuestionId(e.target.value);
                    setClientError(null);
                  }}
                  disabled={processing}
                  required
                >
                  <option value="" disabled>-- Select replacement question --</option>
                  {candidateQuestions.map((q) => (
                    <option key={q.id} value={q.id}>
                      {q.code} — {q.text} {q.id === item.questionId ? ' (current)' : ''}
                    </option>
                  ))}
                </select>
                <div className="form-text text-muted small">
                  The replacement question will keep position #{item.displayOrder} in this questionnaire version.
                </div>
              </div>

              {/* Required Override */}
              <div className="mb-3">
                <label className="form-label fw-semibold" htmlFor="requiredOverrideSelect">
                  Requirement Setting
                </label>
                <select
                  id="requiredOverrideSelect"
                  className="form-select"
                  value={requiredOverride}
                  onChange={(e) => setRequiredOverride(e.target.value)}
                  disabled={processing}
                >
                  <option value={REQUIRED_OVERRIDE.DEFAULT}>Default (Inherit question setting)</option>
                  <option value={REQUIRED_OVERRIDE.REQUIRED}>Always Required</option>
                  <option value={REQUIRED_OVERRIDE.OPTIONAL}>Optional</option>
                </select>
              </div>
            </div>

            <div className="modal-footer bg-light">
              <button
                type="button"
                className="btn btn-outline-secondary"
                disabled={processing}
                onClick={onClose}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={processing}
              >
                {processing ? 'Saving...' : 'Change Question'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
