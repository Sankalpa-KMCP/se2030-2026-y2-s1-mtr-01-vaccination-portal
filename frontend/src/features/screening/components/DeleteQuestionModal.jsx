import React from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

export default function DeleteQuestionModal({ question, processing, error, onClose, onConfirm }) {
  if (!question) return null;

  return (
    <div
      className="modal show d-block"
      role="dialog"
      aria-modal="true"
      aria-labelledby="deleteQuestionTitle"
      tabIndex="-1"
      style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
    >
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content shadow-lg border-0">
          <div className="modal-header bg-danger text-white">
            <h2 className="modal-title h5" id="deleteQuestionTitle">Delete Screening Question</h2>
            <button
              type="button"
              className="btn-close btn-close-white"
              aria-label="Close"
              onClick={onClose}
              disabled={processing}
            />
          </div>
          <div className="modal-body p-4">
            <ApiErrorAlert error={error} />
            <p className="mb-2">Delete this question permanently?</p>
            <div className="rounded bg-light border p-3">
              <div className="font-monospace fw-semibold">{question.code}</div>
              <div className="text-muted small mt-1">{question.text}</div>
            </div>
            <p className="text-muted small mt-3 mb-0">
              Questions already used by questionnaire versions or screening answers may be protected by the backend.
            </p>
          </div>
          <div className="modal-footer bg-light">
            <button type="button" className="btn btn-outline-secondary" onClick={onClose} disabled={processing}>
              Cancel
            </button>
            <button type="button" className="btn btn-danger" onClick={onConfirm} disabled={processing}>
              {processing && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}
              Delete Question
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
