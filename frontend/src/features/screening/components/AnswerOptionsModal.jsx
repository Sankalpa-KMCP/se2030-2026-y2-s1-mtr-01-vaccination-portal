import React, { useEffect, useMemo, useState } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import {
  createAnswerOption,
  deleteAnswerOption,
  listAnswerOptions,
  updateAnswerOption,
} from '../api/screeningApi';

const EMPTY_OPTION = {
  code: '',
  label: '',
  typedValue: '',
  displayOrder: '0',
};

function sortOptions(options) {
  return [...options].sort((left, right) => left.displayOrder - right.displayOrder);
}

export default function AnswerOptionsModal({ question, onClose }) {
  const [options, setOptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [editingOption, setEditingOption] = useState(null);
  const [form, setForm] = useState(EMPTY_OPTION);
  const [formVisible, setFormVisible] = useState(false);
  const [formError, setFormError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [successMessage, setSuccessMessage] = useState(null);

  useEffect(() => {
    if (!question) return undefined;

    let active = true;
    setLoading(true);
    setLoadError(null);
    setOptions([]);
    setFormVisible(false);
    setPendingDelete(null);
    setSuccessMessage(null);

    listAnswerOptions(question.id)
      .then((data) => {
        if (active) setOptions(sortOptions(Array.isArray(data) ? data : []));
      })
      .catch((error) => {
        if (active) setLoadError(error);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [question]);

  const nextDisplayOrder = useMemo(() => (
    options.length === 0 ? 0 : Math.max(...options.map((option) => option.displayOrder)) + 1
  ), [options]);

  if (!question) return null;

  const updateField = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const startCreate = () => {
    setEditingOption(null);
    setForm({ ...EMPTY_OPTION, displayOrder: String(nextDisplayOrder) });
    setFormError(null);
    setSuccessMessage(null);
    setFormVisible(true);
  };

  const startEdit = (option) => {
    setEditingOption(option);
    setForm({
      code: option.code,
      label: option.label,
      typedValue: option.typedValue,
      displayOrder: String(option.displayOrder),
    });
    setFormError(null);
    setSuccessMessage(null);
    setFormVisible(true);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setFormError(null);

    const code = form.code.trim();
    const label = form.label.trim();
    const typedValue = form.typedValue.trim();
    const displayOrder = Number(form.displayOrder);

    if (!code || !label || !typedValue) {
      setFormError('Option code, label, and typed value are required.');
      return;
    }
    if (code.length > 64 || label.length > 255 || typedValue.length > 255) {
      setFormError('Option values exceed the backend field length limits.');
      return;
    }
    if (!Number.isInteger(displayOrder)) {
      setFormError('Display order must be an integer.');
      return;
    }

    const payload = { code, label, typedValue, displayOrder };
    setSaving(true);
    try {
      const saved = editingOption
        ? await updateAnswerOption(editingOption.id, payload)
        : await createAnswerOption(question.id, payload);
      setOptions((current) => sortOptions(editingOption
        ? current.map((option) => option.id === saved.id ? saved : option)
        : [...current, saved]));
      setFormVisible(false);
      setEditingOption(null);
      setSuccessMessage(editingOption ? 'Answer option updated.' : 'Answer option created.');
    } catch (error) {
      setFormError(error);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!pendingDelete) return;
    setDeleting(true);
    setLoadError(null);
    try {
      await deleteAnswerOption(pendingDelete.id);
      setOptions((current) => current.filter((option) => option.id !== pendingDelete.id));
      setPendingDelete(null);
      setSuccessMessage('Answer option deleted.');
    } catch (error) {
      setLoadError(error);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div
      className="modal show d-block"
      role="dialog"
      aria-modal="true"
      aria-labelledby="answerOptionsTitle"
      tabIndex="-1"
      style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
    >
      <div className="modal-dialog modal-dialog-centered modal-xl modal-dialog-scrollable">
        <div className="modal-content shadow-lg border-0">
          <div className="modal-header bg-light">
            <div>
              <h2 className="modal-title h5" id="answerOptionsTitle">Answer Options</h2>
              <div className="small opacity-75">{question.code} — {question.text}</div>
            </div>
            <button
              type="button"
              className="btn-close"
              aria-label="Close"
              onClick={onClose}
              disabled={saving || deleting}
            />
          </div>

          <div className="modal-body p-4">
            {successMessage && (
              <div className="alert alert-success alert-dismissible" role="status">
                {successMessage}
                <button type="button" className="btn-close" aria-label="Dismiss success" onClick={() => setSuccessMessage(null)} />
              </div>
            )}
            <ApiErrorAlert error={loadError} onDismiss={() => setLoadError(null)} />

            <div className="d-flex justify-content-between align-items-center mb-3">
              <div>
                <h3 className="h6 mb-1">Configured Options</h3>
                <p className="text-muted small mb-0">Options are displayed by ascending backend display order.</p>
              </div>
              <button type="button" className="btn btn-primary btn-sm" onClick={startCreate} disabled={loading || saving}>
                Add Option
              </button>
            </div>

            {formVisible && (
              <form className="card border-primary-subtle bg-light mb-4" onSubmit={handleSubmit} noValidate>
                <div className="card-body">
                  <h3 className="h6">{editingOption ? 'Edit Answer Option' : 'Add Answer Option'}</h3>
                  <ApiErrorAlert error={formError} />
                  <div className="row g-3">
                    <div className="col-md-3">
                      <label className="form-label" htmlFor="optionCode">Code</label>
                      <input id="optionCode" className="form-control font-monospace" value={form.code} onChange={(event) => updateField('code', event.target.value)} maxLength={64} disabled={saving} required />
                    </div>
                    <div className="col-md-3">
                      <label className="form-label" htmlFor="optionLabel">Label</label>
                      <input id="optionLabel" className="form-control" value={form.label} onChange={(event) => updateField('label', event.target.value)} maxLength={255} disabled={saving} required />
                    </div>
                    <div className="col-md-4">
                      <label className="form-label" htmlFor="optionTypedValue">Typed Value</label>
                      <input id="optionTypedValue" className="form-control font-monospace" value={form.typedValue} onChange={(event) => updateField('typedValue', event.target.value)} maxLength={255} disabled={saving} required />
                    </div>
                    <div className="col-md-2">
                      <label className="form-label" htmlFor="optionDisplayOrder">Display Order</label>
                      <input id="optionDisplayOrder" type="number" step="1" className="form-control" value={form.displayOrder} onChange={(event) => updateField('displayOrder', event.target.value)} disabled={saving} required />
                    </div>
                  </div>
                </div>
                <div className="card-footer d-flex justify-content-end gap-2">
                  <button type="button" className="btn btn-outline-secondary btn-sm" onClick={() => setFormVisible(false)} disabled={saving}>Cancel</button>
                  <button type="submit" className="btn btn-primary btn-sm" disabled={saving}>
                    {saving && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}
                    {editingOption ? 'Save Option' : 'Create Option'}
                  </button>
                </div>
              </form>
            )}

            {loading ? (
              <LoadingSpinner message="Loading answer options..." />
            ) : options.length === 0 ? (
              <div className="alert alert-light border text-center text-muted">
                No answer options have been configured for this question.
              </div>
            ) : (
              <div className="table-responsive">
                <table className="table table-hover align-middle">
                  <thead className="table-light">
                    <tr>
                      <th scope="col">Order</th>
                      <th scope="col">Code</th>
                      <th scope="col">Label</th>
                      <th scope="col">Typed Value</th>
                      <th scope="col" className="text-end">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {options.map((option) => (
                      <tr key={option.id}>
                        <td>{option.displayOrder}</td>
                        <td className="font-monospace">{option.code}</td>
                        <td>{option.label}</td>
                        <td className="font-monospace small">{option.typedValue}</td>
                        <td className="text-end">
                          {pendingDelete?.id === option.id ? (
                            <div className="d-inline-flex align-items-center gap-2">
                              <span className="small text-danger">Delete?</span>
                              <button type="button" className="btn btn-danger btn-sm" onClick={handleDelete} disabled={deleting}>Confirm</button>
                              <button type="button" className="btn btn-outline-secondary btn-sm" onClick={() => setPendingDelete(null)} disabled={deleting}>Cancel</button>
                            </div>
                          ) : (
                            <div className="btn-group btn-group-sm">
                              <button type="button" className="btn btn-outline-primary" aria-label={`Edit option ${option.code}`} onClick={() => startEdit(option)}>Edit</button>
                              <button type="button" className="btn btn-outline-danger" aria-label={`Delete option ${option.code}`} onClick={() => setPendingDelete(option)}>Delete</button>
                            </div>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          <div className="modal-footer bg-light">
            <button type="button" className="btn btn-outline-secondary" onClick={onClose} disabled={saving || deleting}>Close</button>
          </div>
        </div>
      </div>
    </div>
  );
}
