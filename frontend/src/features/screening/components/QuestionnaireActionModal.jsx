import React from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

export default function QuestionnaireActionModal({ action, processing, error, onClose, onConfirm }) {
  if (!action) return null;

  return (
    <div className="modal show d-block" role="dialog" aria-modal="true" aria-labelledby="questionnaireActionTitle" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content shadow-lg border-0">
          <div className={`modal-header ${action.danger ? 'bg-danger' : 'bg-primary'} text-white`}>
            <h2 className="modal-title h5" id="questionnaireActionTitle">{action.title}</h2>
            <button type="button" className="btn-close btn-close-white" aria-label="Close" onClick={onClose} disabled={processing} />
          </div>
          <div className="modal-body p-4">
            <ApiErrorAlert error={error} />
            <p className="mb-0">{action.message}</p>
          </div>
          <div className="modal-footer bg-light">
            <button type="button" className="btn btn-outline-secondary" onClick={onClose} disabled={processing}>Cancel</button>
            <button type="button" className={`btn ${action.danger ? 'btn-danger' : 'btn-primary'}`} onClick={onConfirm} disabled={processing}>
              {processing && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}
              {action.confirmLabel}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
