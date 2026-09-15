import React, { useEffect, useState } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

const EMPTY_FORM = { code: '', name: '', description: '' };

export default function QuestionnaireFormModal({ show, processing, apiError, onClose, onSubmit }) {
  const [form, setForm] = useState(EMPTY_FORM);
  const [clientError, setClientError] = useState(null);

  useEffect(() => {
    if (show) {
      setForm(EMPTY_FORM);
      setClientError(null);
    }
  }, [show]);

  if (!show) return null;

  const updateField = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    const payload = {
      code: form.code.trim(),
      name: form.name.trim(),
      description: form.description.trim() || null,
    };

    if (!payload.code || !payload.name) {
      setClientError('Questionnaire code and name are required.');
      return;
    }

    setClientError(null);
    onSubmit(payload);
  };

  return (
    <div className="modal show d-block" role="dialog" aria-modal="true" aria-labelledby="questionnaireFormTitle" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content shadow-lg border-0">
          <div className="modal-header bg-light">
            <h2 className="modal-title h5" id="questionnaireFormTitle">Create Questionnaire</h2>
            <button type="button" className="btn-close" aria-label="Close" onClick={onClose} disabled={processing} />
          </div>
          <form onSubmit={handleSubmit} noValidate>
            <div className="modal-body p-4">
              <ApiErrorAlert error={clientError || apiError} />
              <div className="mb-3">
                <label className="form-label fw-semibold" htmlFor="questionnaireCode">Code <span className="text-danger">*</span></label>
                <input id="questionnaireCode" className="form-control font-monospace" value={form.code} onChange={(event) => updateField('code', event.target.value)} maxLength={64} disabled={processing} required />
              </div>
              <div className="mb-3">
                <label className="form-label fw-semibold" htmlFor="questionnaireName">Name <span className="text-danger">*</span></label>
                <input id="questionnaireName" className="form-control" value={form.name} onChange={(event) => updateField('name', event.target.value)} maxLength={255} disabled={processing} required />
              </div>
              <div>
                <label className="form-label fw-semibold" htmlFor="questionnaireDescription">Description</label>
                <textarea id="questionnaireDescription" className="form-control" rows={3} value={form.description} onChange={(event) => updateField('description', event.target.value)} disabled={processing} />
              </div>
            </div>
            <div className="modal-footer bg-light">
              <button type="button" className="btn btn-outline-secondary" onClick={onClose} disabled={processing}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={processing}>
                {processing && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}
                Create Questionnaire
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
