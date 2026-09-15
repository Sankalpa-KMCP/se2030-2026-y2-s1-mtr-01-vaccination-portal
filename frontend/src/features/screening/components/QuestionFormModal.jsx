import React, { useEffect, useState } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import {
  QUESTION_TYPE,
  QUESTION_TYPE_LABELS,
  QUESTION_TYPES,
} from '../constants/screeningConstants';

const EMPTY_FORM = {
  code: '',
  text: '',
  description: '',
  questionType: QUESTION_TYPE.BOOLEAN,
  defaultRequired: true,
  active: true,
};

export default function QuestionFormModal({
  show,
  question = null,
  processing = false,
  apiError = null,
  onClose,
  onSubmit,
}) {
  const [form, setForm] = useState(EMPTY_FORM);
  const [clientError, setClientError] = useState(null);
  const isEditing = Boolean(question);

  useEffect(() => {
    if (!show) return;

    setForm(question
      ? {
          code: question.code,
          text: question.text,
          description: question.description || '',
          questionType: question.questionType,
          defaultRequired: question.defaultRequired,
          active: question.active,
        }
      : EMPTY_FORM);
    setClientError(null);
  }, [question, show]);

  if (!show) return null;

  const updateField = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    setClientError(null);

    const code = form.code.trim();
    const text = form.text.trim();
    const description = form.description.trim();

    if (!isEditing && !code) {
      setClientError('Question code is required.');
      return;
    }
    if (code.length > 64) {
      setClientError('Question code must not exceed 64 characters.');
      return;
    }
    if (!text) {
      setClientError('Question text is required.');
      return;
    }
    if (text.length > 1000 || description.length > 1000) {
      setClientError('Question text and description must not exceed 1000 characters.');
      return;
    }
    if (!isEditing && !QUESTION_TYPES.includes(form.questionType)) {
      setClientError('Question type is required.');
      return;
    }

    onSubmit(isEditing
      ? {
          text,
          description: description || null,
          defaultRequired: form.defaultRequired,
          active: form.active,
        }
      : {
          code,
          text,
          description: description || null,
          questionType: form.questionType,
          defaultRequired: form.defaultRequired,
        });
  };

  return (
    <div
      className="modal show d-block"
      role="dialog"
      aria-modal="true"
      aria-labelledby="questionFormTitle"
      tabIndex="-1"
      style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
    >
      <div className="modal-dialog modal-dialog-centered modal-lg">
        <div className="modal-content shadow-lg border-0">
          <div className="modal-header bg-light">
            <h2 className="modal-title h5" id="questionFormTitle">
              {isEditing ? 'Edit Screening Question' : 'Create Screening Question'}
            </h2>
            <button
              type="button"
              className="btn-close"
              aria-label="Close"
              onClick={onClose}
              disabled={processing}
            />
          </div>

          <form onSubmit={handleSubmit} noValidate>
            <div className="modal-body p-4">
              <ApiErrorAlert error={clientError || apiError} />

              <div className="row g-3">
                <div className="col-md-5">
                  <label className="form-label fw-semibold" htmlFor="questionCode">
                    Question Code <span className="text-danger">*</span>
                  </label>
                  <input
                    id="questionCode"
                    className="form-control font-monospace"
                    value={form.code}
                    onChange={(event) => updateField('code', event.target.value)}
                    maxLength={64}
                    disabled={processing || isEditing}
                    required
                  />
                  {isEditing && <div className="form-text">Question code is immutable.</div>}
                </div>

                <div className="col-md-7">
                  <label className="form-label fw-semibold" htmlFor="questionType">
                    Question Type <span className="text-danger">*</span>
                  </label>
                  <select
                    id="questionType"
                    className="form-select"
                    value={form.questionType}
                    onChange={(event) => updateField('questionType', event.target.value)}
                    disabled={processing || isEditing}
                    required
                  >
                    {QUESTION_TYPES.map((type) => (
                      <option key={type} value={type}>{QUESTION_TYPE_LABELS[type]}</option>
                    ))}
                  </select>
                  {isEditing && <div className="form-text">Question type is immutable.</div>}
                </div>

                <div className="col-12">
                  <label className="form-label fw-semibold" htmlFor="questionText">
                    Question Text <span className="text-danger">*</span>
                  </label>
                  <textarea
                    id="questionText"
                    className="form-control"
                    rows={3}
                    value={form.text}
                    onChange={(event) => updateField('text', event.target.value)}
                    maxLength={1000}
                    disabled={processing}
                    required
                  />
                </div>

                <div className="col-12">
                  <label className="form-label fw-semibold" htmlFor="questionDescription">
                    Description
                  </label>
                  <textarea
                    id="questionDescription"
                    className="form-control"
                    rows={2}
                    value={form.description}
                    onChange={(event) => updateField('description', event.target.value)}
                    maxLength={1000}
                    disabled={processing}
                  />
                </div>

                <div className="col-md-6">
                  <div className="form-check form-switch mt-2">
                    <input
                      id="questionRequired"
                      className="form-check-input"
                      type="checkbox"
                      checked={form.defaultRequired}
                      onChange={(event) => updateField('defaultRequired', event.target.checked)}
                      disabled={processing}
                    />
                    <label className="form-check-label fw-semibold" htmlFor="questionRequired">
                      Required by default
                    </label>
                  </div>
                </div>

                {isEditing && (
                  <div className="col-md-6">
                    <div className="form-check form-switch mt-2">
                      <input
                        id="questionActive"
                        className="form-check-input"
                        type="checkbox"
                        checked={form.active}
                        onChange={(event) => updateField('active', event.target.checked)}
                        disabled={processing}
                      />
                      <label className="form-check-label fw-semibold" htmlFor="questionActive">
                        Active
                      </label>
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="modal-footer bg-light">
              <button type="button" className="btn btn-outline-secondary" onClick={onClose} disabled={processing}>
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={processing}>
                {processing && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}
                {isEditing ? 'Save Changes' : 'Create Question'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
