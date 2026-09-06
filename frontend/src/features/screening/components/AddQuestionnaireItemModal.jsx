import React, { useEffect, useState } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

const REQUIRED_OVERRIDE = Object.freeze({ DEFAULT: '', REQUIRED: 'true', OPTIONAL: 'false' });

export default function AddQuestionnaireItemModal({ show, questions, suggestedOrder, processing, apiError, onClose, onSubmit }) {
  const [questionId, setQuestionId] = useState('');
  const [displayOrder, setDisplayOrder] = useState(1);
  const [requiredOverride, setRequiredOverride] = useState(REQUIRED_OVERRIDE.DEFAULT);
  const [clientError, setClientError] = useState(null);

  useEffect(() => {
    if (show) {
      setQuestionId(questions[0]?.id ? String(questions[0].id) : '');
      setDisplayOrder(suggestedOrder);
      setRequiredOverride(REQUIRED_OVERRIDE.DEFAULT);
      setClientError(null);
    }
  }, [questions, show, suggestedOrder]);

  if (!show) return null;

  const handleSubmit = (event) => {
    event.preventDefault();
    const numericQuestionId = Number(questionId);
    const numericOrder = Number(displayOrder);
    if (!numericQuestionId || !Number.isInteger(numericOrder)) {
      setClientError('Select a question and enter a whole-number display order.');
      return;
    }

    setClientError(null);
    onSubmit({
      questionId: numericQuestionId,
      displayOrder: numericOrder,
      requiredOverride: requiredOverride === REQUIRED_OVERRIDE.DEFAULT
        ? null
        : requiredOverride === REQUIRED_OVERRIDE.REQUIRED,
    });
  };

  return (
    <div className="modal show d-block" role="dialog" aria-modal="true" aria-labelledby="addQuestionItemTitle" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content shadow-lg border-0">
          <div className="modal-header bg-light">
            <h2 className="modal-title h5" id="addQuestionItemTitle">Add Question to Version</h2>
            <button type="button" className="btn-close" aria-label="Close" onClick={onClose} disabled={processing} />
          </div>
          <form onSubmit={handleSubmit} noValidate>
            <div className="modal-body p-4">
              <ApiErrorAlert error={clientError || apiError} />
              {questions.length === 0 ? (
                <div className="alert alert-info mb-0">All active questions are already attached to this version.</div>
              ) : (
                <>
                  <div className="mb-3">
                    <label className="form-label fw-semibold" htmlFor="questionnaireItemQuestion">Question <span className="text-danger">*</span></label>
                    <select id="questionnaireItemQuestion" className="form-select" value={questionId} onChange={(event) => setQuestionId(event.target.value)} disabled={processing} required>
                      {questions.map((question) => <option key={question.id} value={question.id}>{question.code} — {question.text}</option>)}
                    </select>
                  </div>
                  <div className="row g-3">
                    <div className="col-sm-6">
                      <label className="form-label fw-semibold" htmlFor="questionnaireItemOrder">Display Order <span className="text-danger">*</span></label>
                      <input id="questionnaireItemOrder" className="form-control" type="number" step="1" value={displayOrder} onChange={(event) => setDisplayOrder(event.target.value)} disabled={processing} required />
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
                  <p className="form-text mt-3 mb-0">Items are stored in this order. The backend does not expose edit, removal, or reorder operations.</p>
                </>
              )}
            </div>
            <div className="modal-footer bg-light">
              <button type="button" className="btn btn-outline-secondary" onClick={onClose} disabled={processing}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={processing || questions.length === 0}>
                {processing && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}
                Add Question
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
