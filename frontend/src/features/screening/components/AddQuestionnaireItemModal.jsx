import React, { useEffect, useMemo, useState } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import {
  QUESTION_TYPE,
  QUESTION_TYPE_LABELS,
  QUESTION_TYPES,
  isChoiceQuestionType,
} from '../constants/screeningConstants';

const MODE = Object.freeze({ EXISTING: 'existing', CREATE: 'create' });
const REQUIRED_OVERRIDE = Object.freeze({ DEFAULT: '', REQUIRED: 'true', OPTIONAL: 'false' });
const EMPTY_QUESTION = {
  code: '',
  text: '',
  description: '',
  questionType: QUESTION_TYPE.BOOLEAN,
  defaultRequired: true,
};

function newOption(key, displayOrder = 1) {
  return { key, code: '', label: '', typedValue: '', displayOrder: String(displayOrder) };
}

export default function AddQuestionnaireItemModal({
  show,
  questions,
  suggestedOrder = 1,
  processing,
  apiError,
  creationProgress = null,
  onClose,
  onSubmit,
  onCreateAndAttach,
}) {
  const [mode, setMode] = useState(MODE.EXISTING);
  const [questionId, setQuestionId] = useState('');
  const [displayOrder, setDisplayOrder] = useState(1);
  const [requiredOverride, setRequiredOverride] = useState(REQUIRED_OVERRIDE.DEFAULT);
  const [newQuestion, setNewQuestion] = useState(EMPTY_QUESTION);
  const [options, setOptions] = useState([]);
  const [nextOptionKey, setNextOptionKey] = useState(2);
  const [clientError, setClientError] = useState(null);

  useEffect(() => {
    if (show) {
      const initialDisplayOrder = Number.isInteger(Number(suggestedOrder)) && Number(suggestedOrder) >= 1
        ? Number(suggestedOrder)
        : 1;
      setMode(MODE.EXISTING);
      setQuestionId(questions[0]?.id ? String(questions[0].id) : '');
      setDisplayOrder(initialDisplayOrder);
      setRequiredOverride(REQUIRED_OVERRIDE.DEFAULT);
      setNewQuestion(EMPTY_QUESTION);
      setOptions([]);
      setNextOptionKey(2);
      setClientError(null);
    }
  }, [questions, show, suggestedOrder]);

  const savedOptionCodes = useMemo(() => new Set(
    (creationProgress?.options || []).map((option) => option.code.trim().toLocaleLowerCase()),
  ), [creationProgress]);
  const questionAlreadyCreated = Boolean(creationProgress?.question);

  if (!show) return null;

  const updateQuestion = (field, value) => {
    setNewQuestion((current) => {
      const updated = { ...current, [field]: value };
      if (field === 'questionType') {
        if (isChoiceQuestionType(value) && !isChoiceQuestionType(current.questionType)) {
          setOptions([newOption(1)]);
        } else if (!isChoiceQuestionType(value)) {
          setOptions([]);
        }
      }
      return updated;
    });
    setClientError(null);
  };

  const addOption = () => {
    const display = options.length === 0
      ? 1
      : Math.max(...options.map((option) => Number(option.displayOrder) || 0)) + 1;
    setOptions((current) => [...current, newOption(nextOptionKey, display)]);
    setNextOptionKey((current) => current + 1);
    setClientError(null);
  };

  const updateOption = (key, field, value) => {
    setOptions((current) => current.map((option) => option.key === key ? { ...option, [field]: value } : option));
    setClientError(null);
  };

  const removeOption = (key) => {
    setOptions((current) => current.filter((option) => option.key !== key));
    setClientError(null);
  };

  const validateNewQuestion = () => {
    const code = newQuestion.code.trim();
    const text = newQuestion.text.trim();
    const description = newQuestion.description.trim();
    if (!code) return 'Question code is required.';
    if (code.length > 64) return 'Question code must not exceed 64 characters.';
    if (!text) return 'Question text is required.';
    if (text.length > 1000 || description.length > 1000) return 'Question text and description must not exceed 1000 characters.';
    if (!QUESTION_TYPES.includes(newQuestion.questionType)) return 'Question type is required.';

    if (isChoiceQuestionType(newQuestion.questionType)) {
      if (options.length === 0) return 'Add at least one answer option for a choice question.';
      const codes = new Set();
      const orders = new Set();
      for (const option of options) {
        const optionCode = option.code.trim();
        const label = option.label.trim();
        const typedValue = option.typedValue.trim();
        const order = Number(option.displayOrder);
        if (!optionCode || !label || !typedValue) return 'Every answer option requires a code, label, and typed value.';
        if (optionCode.length > 64 || label.length > 255 || typedValue.length > 255) return 'Answer option values exceed the backend field length limits.';
        if (!Number.isInteger(order)) return 'Every answer-option display order must be a whole number.';
        const normalizedCode = optionCode.toLocaleLowerCase();
        if (codes.has(normalizedCode)) return 'Answer option codes must be unique within the question.';
        if (orders.has(order)) return 'Answer-option display orders must be unique.';
        codes.add(normalizedCode);
        orders.add(order);
      }
    }
    return null;
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    const orderString = String(displayOrder ?? '').trim();
    const numericOrder = Number(orderString);
    if (!orderString || !Number.isInteger(numericOrder) || numericOrder < 1) {
      setClientError('Display order must be a whole number of 1 or greater.');
      return;
    }

    const attachment = {
      displayOrder: numericOrder,
      requiredOverride: requiredOverride === REQUIRED_OVERRIDE.DEFAULT
        ? null
        : requiredOverride === REQUIRED_OVERRIDE.REQUIRED,
    };

    if (mode === MODE.EXISTING) {
      const numericQuestionId = Number(questionId);
      if (!numericQuestionId) {
        setClientError('Select a question.');
        return;
      }
      setClientError(null);
      onSubmit({ questionId: numericQuestionId, ...attachment });
      return;
    }

    const validationError = validateNewQuestion();
    if (validationError) {
      setClientError(validationError);
      return;
    }

    setClientError(null);
    onCreateAndAttach({
      question: {
        code: newQuestion.code.trim(),
        text: newQuestion.text.trim(),
        description: newQuestion.description.trim() || null,
        questionType: newQuestion.questionType,
        defaultRequired: newQuestion.defaultRequired,
      },
      options: options
        .map((option) => ({
          code: option.code.trim(),
          label: option.label.trim(),
          typedValue: option.typedValue.trim(),
          displayOrder: Number(option.displayOrder),
        }))
        .sort((left, right) => left.displayOrder - right.displayOrder),
      attachment,
    });
  };

  return (
    <div className="modal show d-block" role="dialog" aria-modal="true" aria-labelledby="addQuestionItemTitle" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered modal-xl modal-dialog-scrollable">
        <div className="modal-content shadow-lg border-0">
          <div className="modal-header bg-light">
            <h2 className="modal-title h5" id="addQuestionItemTitle">Add Question to Version</h2>
            <button type="button" className="btn-close" aria-label="Close" onClick={onClose} disabled={processing} />
          </div>
          <form onSubmit={handleSubmit} noValidate>
            <div className="modal-body p-4">
              <ApiErrorAlert error={clientError || apiError} />
              {questionAlreadyCreated && (
                <div className="alert alert-info">
                  Question <strong>{creationProgress.question.code}</strong> now exists in the global catalogue. Retry will continue without creating it again.
                </div>
              )}

              <div className="btn-group mb-4" role="group" aria-label="Question source">
                <button type="button" className={`btn ${mode === MODE.EXISTING ? 'btn-primary' : 'btn-outline-primary'}`} aria-pressed={mode === MODE.EXISTING} onClick={() => { setMode(MODE.EXISTING); setClientError(null); }} disabled={processing || questionAlreadyCreated}>Select Existing</button>
                <button type="button" className={`btn ${mode === MODE.CREATE ? 'btn-primary' : 'btn-outline-primary'}`} aria-pressed={mode === MODE.CREATE} onClick={() => { setMode(MODE.CREATE); setClientError(null); }} disabled={processing}>Create New</button>
              </div>

              {mode === MODE.EXISTING ? (
                questions.length === 0 ? (
                  <div className="alert alert-info mb-0">All active questions are already attached. Choose Create New to add another catalogue question.</div>
                ) : (
                  <div className="mb-3">
                    <label className="form-label fw-semibold" htmlFor="questionnaireItemQuestion">Question <span className="text-danger">*</span></label>
                    <select id="questionnaireItemQuestion" className="form-select" value={questionId} onChange={(event) => setQuestionId(event.target.value)} disabled={processing} required>
                      {questions.map((question) => <option key={question.id} value={question.id}>{question.code} — {question.text}</option>)}
                    </select>
                  </div>
                )
              ) : (
                <>
                  <fieldset disabled={processing || questionAlreadyCreated}>
                    <div className="row g-3">
                      <div className="col-md-5">
                        <label className="form-label fw-semibold" htmlFor="newQuestionCode">Question Code <span className="text-danger">*</span></label>
                        <input id="newQuestionCode" className="form-control font-monospace" value={newQuestion.code} onChange={(event) => updateQuestion('code', event.target.value)} maxLength={64} required />
                      </div>
                      <div className="col-md-7">
                        <label className="form-label fw-semibold" htmlFor="newQuestionType">Question Type <span className="text-danger">*</span></label>
                        <select id="newQuestionType" className="form-select" value={newQuestion.questionType} onChange={(event) => updateQuestion('questionType', event.target.value)} required>
                          {QUESTION_TYPES.map((type) => <option key={type} value={type}>{QUESTION_TYPE_LABELS[type]}</option>)}
                        </select>
                      </div>
                      <div className="col-12">
                        <label className="form-label fw-semibold" htmlFor="newQuestionText">Question Text <span className="text-danger">*</span></label>
                        <textarea id="newQuestionText" className="form-control" rows={3} value={newQuestion.text} onChange={(event) => updateQuestion('text', event.target.value)} maxLength={1000} required />
                      </div>
                      <div className="col-12">
                        <label className="form-label fw-semibold" htmlFor="newQuestionDescription">Description</label>
                        <textarea id="newQuestionDescription" className="form-control" rows={2} value={newQuestion.description} onChange={(event) => updateQuestion('description', event.target.value)} maxLength={1000} />
                      </div>
                      <div className="col-12">
                        <div className="form-check form-switch">
                          <input id="newQuestionRequired" className="form-check-input" type="checkbox" checked={newQuestion.defaultRequired} onChange={(event) => updateQuestion('defaultRequired', event.target.checked)} />
                          <label className="form-check-label fw-semibold" htmlFor="newQuestionRequired">Required by default</label>
                        </div>
                      </div>
                    </div>
                  </fieldset>

                  {isChoiceQuestionType(newQuestion.questionType) && (
                    <fieldset className="border rounded p-3 mt-4">
                      <legend className="float-none w-auto px-2 h6">Answer Options</legend>
                      <div className="d-flex justify-content-between align-items-center mb-3">
                        <p className="small text-muted mb-0">Options are saved only when the complete Create New flow is submitted.</p>
                        <button type="button" className="btn btn-outline-primary btn-sm" onClick={addOption} disabled={processing || questionAlreadyCreated}>Add Option</button>
                      </div>
                      {options.map((option, index) => {
                        const saved = savedOptionCodes.has(option.code.trim().toLocaleLowerCase());
                        return (
                          <div className="row g-2 align-items-end mb-3" key={option.key} data-testid="new-question-option-row">
                            <div className="col-md-2">
                              <label className="form-label" htmlFor={`newOptionCode-${option.key}`}>Option Code *</label>
                              <input id={`newOptionCode-${option.key}`} className="form-control font-monospace" value={option.code} onChange={(event) => updateOption(option.key, 'code', event.target.value)} maxLength={64} disabled={processing || saved} required />
                            </div>
                            <div className="col-md-3">
                              <label className="form-label" htmlFor={`newOptionLabel-${option.key}`}>Label *</label>
                              <input id={`newOptionLabel-${option.key}`} className="form-control" value={option.label} onChange={(event) => updateOption(option.key, 'label', event.target.value)} maxLength={255} disabled={processing || saved} required />
                            </div>
                            <div className="col-md-3">
                              <label className="form-label" htmlFor={`newOptionValue-${option.key}`}>Typed Value *</label>
                              <input id={`newOptionValue-${option.key}`} className="form-control font-monospace" value={option.typedValue} onChange={(event) => updateOption(option.key, 'typedValue', event.target.value)} maxLength={255} disabled={processing || saved} required />
                            </div>
                            <div className="col-md-2">
                              <label className="form-label" htmlFor={`newOptionOrder-${option.key}`}>Display Order *</label>
                              <input id={`newOptionOrder-${option.key}`} className="form-control" type="number" step="1" value={option.displayOrder} onChange={(event) => updateOption(option.key, 'displayOrder', event.target.value)} disabled={processing || saved} required />
                            </div>
                            <div className="col-md-2 text-end">
                              {saved ? <span className="badge text-bg-success">Saved</span> : <button type="button" className="btn btn-outline-danger btn-sm" aria-label={`Remove option ${index + 1}`} onClick={() => removeOption(option.key)} disabled={processing || questionAlreadyCreated}>Remove</button>}
                            </div>
                          </div>
                        );
                      })}
                    </fieldset>
                  )}
                </>
              )}

              <div className="row g-3 mt-1">
                <div className="col-sm-6">
                  <label className="form-label fw-semibold" htmlFor="questionnaireItemOrder">Display Order <span className="text-danger">*</span></label>
                  <input id="questionnaireItemOrder" className="form-control" type="number" min="1" step="1" value={displayOrder} onChange={(event) => { setDisplayOrder(event.target.value); setClientError(null); }} disabled={processing} required />
                </div>
                <div className="col-sm-6">
                  <label className="form-label fw-semibold" htmlFor="questionnaireItemRequired">Required Override</label>
                  <select id="questionnaireItemRequired" className="form-select" value={requiredOverride} onChange={(event) => setRequiredOverride(event.target.value)} disabled={processing}>
                    <option value="">Use question default</option>
                    <option value="true">Required</option>
                    <option value="false">Optional</option>
                  </select>
                </div>
              </div>
            </div>
            <div className="modal-footer bg-light">
              <button type="button" className="btn btn-outline-secondary" onClick={onClose} disabled={processing}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={processing || (mode === MODE.EXISTING && questions.length === 0)}>
                {processing && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}
                {mode === MODE.EXISTING ? 'Add Question' : questionAlreadyCreated ? 'Retry Create and Add' : 'Create and Add Question'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
