import React, { useEffect, useState } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import {
  createAnswerOption,
  deleteAnswerOption,
  listAnswerOptions,
  updateAnswerOption,
} from '../api/screeningApi';
import { isChoiceQuestionType } from '../constants/screeningConstants';
import {
  generateOptionCode,
  getNextDisplayOrder,
  parseMultipleOptions,
  validateOptionLabel,
} from '../utils/answerOptionUtils';

const EMPTY_OPTION = {
  code: '',
  label: '',
  typedValue: '',
  displayOrder: '1',
};

function sortOptions(options) {
  return [...options].sort((left, right) => Number(left.displayOrder) - Number(right.displayOrder));
}

export default function AnswerOptionsModal({ question, onClose }) {
  const [options, setOptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  // Single option form state
  const [editingOption, setEditingOption] = useState(null);
  const [form, setForm] = useState(EMPTY_OPTION);
  const [touchedFields, setTouchedFields] = useState({ code: false, typedValue: false, displayOrder: false });
  const [formVisible, setFormVisible] = useState(false);
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [formError, setFormError] = useState(null);
  const [saving, setSaving] = useState(false);

  // Bulk options entry state
  const [bulkVisible, setBulkVisible] = useState(false);
  const [bulkText, setBulkText] = useState('');
  const [bulkError, setBulkError] = useState(null);
  const [bulkSaving, setBulkSaving] = useState(false);

  // Action states
  const [pendingDelete, setPendingDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [reordering, setReordering] = useState(false);
  const [successMessage, setSuccessMessage] = useState(null);

  useEffect(() => {
    if (!question) return undefined;

    let active = true;
    setLoading(true);
    setLoadError(null);
    setOptions([]);
    setFormVisible(false);
    setBulkVisible(false);
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

  if (!question) return null;

  const nextDisplayOrder = getNextDisplayOrder(options);

  const startCreate = () => {
    setEditingOption(null);
    setTouchedFields({ code: false, typedValue: false, displayOrder: false });
    setForm({ ...EMPTY_OPTION, displayOrder: String(nextDisplayOrder) });
    setShowAdvanced(false);
    setFormError(null);
    setSuccessMessage(null);
    setBulkVisible(false);
    setFormVisible(true);
  };

  const startEdit = (option) => {
    setEditingOption(option);
    setTouchedFields({ code: true, typedValue: true, displayOrder: true });
    setForm({
      code: option.code,
      label: option.label,
      typedValue: option.typedValue,
      displayOrder: String(option.displayOrder),
    });
    setShowAdvanced(false);
    setFormError(null);
    setSuccessMessage(null);
    setBulkVisible(false);
    setFormVisible(true);
  };

  const handleLabelChange = (event) => {
    const newLabel = event.target.value;
    if (editingOption) {
      // In edit mode: preserve existing code and typed value
      setForm((current) => ({ ...current, label: newLabel }));
    } else {
      // In create mode: auto-generate code and typed value if untouched
      setForm((current) => ({
        ...current,
        label: newLabel,
        code: touchedFields.code ? current.code : generateOptionCode(newLabel, options.map((opt) => opt.code)),
        typedValue: touchedFields.typedValue ? current.typedValue : newLabel,
      }));
    }
  };

  const updateAdvancedField = (field, value) => {
    setTouchedFields((current) => ({ ...current, [field]: true }));
    setForm((current) => ({ ...current, [field]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setFormError(null);

    const label = form.label.trim();
    const labelError = validateOptionLabel(label, options, editingOption?.id);
    if (labelError) {
      setFormError(labelError);
      return;
    }

    const code = form.code.trim() || generateOptionCode(label, options.map((opt) => opt.code));
    const typedValue = form.typedValue.trim() || label;
    const displayOrder = Number(form.displayOrder || nextDisplayOrder);

    if (!code) {
      setFormError('Option code is required.');
      return;
    }
    if (code.length > 64) {
      setFormError('Option code must not exceed 64 characters.');
      return;
    }
    if (typedValue.length > 255) {
      setFormError('Option typed value must not exceed 255 characters.');
      return;
    }
    if (!Number.isInteger(displayOrder) || displayOrder < 1) {
      setFormError('Display order must be a whole number of 1 or greater.');
      return;
    }

    // Check duplicate code against other options
    const codeConflict = options.some(
      (opt) => (!editingOption || opt.id !== editingOption.id) && opt.code.trim().toUpperCase() === code.toUpperCase()
    );
    if (codeConflict) {
      setFormError('An option with this code already exists.');
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

  const handleMove = async (currentIndex, targetIndex) => {
    if (reordering || saving || deleting || currentIndex === targetIndex) return;
    if (targetIndex < 0 || targetIndex >= options.length) return;

    const reordered = [...options];
    const temp = reordered[currentIndex];
    reordered[currentIndex] = reordered[targetIndex];
    reordered[targetIndex] = temp;

    const optA = { ...reordered[currentIndex], displayOrder: currentIndex + 1 };
    const optB = { ...reordered[targetIndex], displayOrder: targetIndex + 1 };

    setReordering(true);
    setLoadError(null);
    try {
      await Promise.all([
        updateAnswerOption(optA.id, {
          code: optA.code,
          label: optA.label,
          typedValue: optA.typedValue,
          displayOrder: optA.displayOrder,
        }),
        updateAnswerOption(optB.id, {
          code: optB.code,
          label: optB.label,
          typedValue: optB.typedValue,
          displayOrder: optB.displayOrder,
        }),
      ]);
      setOptions(sortOptions(reordered.map((opt, idx) => ({ ...opt, displayOrder: idx + 1 }))));
      setSuccessMessage('Option order updated.');
    } catch (error) {
      setLoadError(error);
    } finally {
      setReordering(false);
    }
  };

  const handleBulkSubmit = async (event) => {
    event.preventDefault();
    setBulkError(null);

    const parsed = parseMultipleOptions(bulkText, options);
    if (!parsed.valid) {
      setBulkError(parsed.error);
      return;
    }

    setBulkSaving(true);
    const created = [];
    try {
      for (const item of parsed.items) {
        const saved = await createAnswerOption(question.id, item);
        created.push(saved);
      }
      setOptions((current) => sortOptions([...current, ...created]));
      setBulkVisible(false);
      setBulkText('');
      setSuccessMessage(`Added ${created.length} answer options.`);
    } catch (error) {
      if (created.length > 0) {
        setOptions((current) => sortOptions([...current, ...created]));
      }
      const msg = error?.message || 'Failed to save option.';
      setBulkError(
        created.length > 0
          ? `Created ${created.length} options before error: ${msg}`
          : msg
      );
    } finally {
      setBulkSaving(false);
    }
  };

  const handleAddYesNo = async () => {
    if (saving || deleting || reordering) return;
    setSaving(true);
    setLoadError(null);
    try {
      const yes = await createAnswerOption(question.id, {
        code: 'YES',
        label: 'Yes',
        typedValue: 'Yes',
        displayOrder: 1,
      });
      const no = await createAnswerOption(question.id, {
        code: 'NO',
        label: 'No',
        typedValue: 'No',
        displayOrder: 2,
      });
      setOptions(sortOptions([yes, no]));
      setSuccessMessage('Added Yes and No options.');
    } catch (error) {
      setLoadError(error);
    } finally {
      setSaving(false);
    }
  };

  const isChoice = isChoiceQuestionType(question.questionType);

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
              disabled={saving || deleting || reordering || bulkSaving}
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

            <div className="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
              <div>
                <h3 className="h6 mb-1">Configured Options</h3>
                <p className="text-muted small mb-0">Enter option labels. Codes and ordering are generated automatically.</p>
              </div>
              <div className="d-flex gap-2">
                {options.length === 0 && isChoice && (
                  <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm"
                    onClick={handleAddYesNo}
                    disabled={loading || saving || bulkSaving}
                  >
                    + Add Yes / No Options
                  </button>
                )}
                <button
                  type="button"
                  className="btn btn-outline-primary btn-sm"
                  onClick={() => {
                    setBulkVisible(!bulkVisible);
                    setFormVisible(false);
                    setBulkError(null);
                  }}
                  disabled={loading || saving || bulkSaving}
                >
                  {bulkVisible ? 'Cancel Multiple' : 'Add Multiple'}
                </button>
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={startCreate}
                  disabled={loading || saving || bulkSaving}
                >
                  Add Option
                </button>
              </div>
            </div>

            {/* Multiple Options Bulk Entry Form */}
            {bulkVisible && (
              <form className="card border-primary-subtle bg-light mb-4" onSubmit={handleBulkSubmit}>
                <div className="card-body">
                  <h3 className="h6 mb-1">Add Multiple Options</h3>
                  <p className="text-muted small mb-3">
                    Enter one option per line. Codes, typed values, and display orders will be generated automatically.
                  </p>
                  <ApiErrorAlert error={bulkError} />
                  <div className="mb-3">
                    <label className="form-label visually-hidden" htmlFor="bulkOptionLabels">Option labels per line</label>
                    <textarea
                      id="bulkOptionLabels"
                      className="form-control"
                      rows={5}
                      placeholder={`No symptoms\nFever\nCough\nHeadache\nOther`}
                      value={bulkText}
                      onChange={(e) => setBulkText(e.target.value)}
                      disabled={bulkSaving}
                      required
                    />
                  </div>
                </div>
                <div className="card-footer d-flex justify-content-end gap-2">
                  <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm"
                    onClick={() => setBulkVisible(false)}
                    disabled={bulkSaving}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary btn-sm"
                    disabled={bulkSaving || !bulkText.trim()}
                  >
                    {bulkSaving && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}
                    Add Options
                  </button>
                </div>
              </form>
            )}

            {/* Single Option Add / Edit Form */}
            {formVisible && (
              <form className="card border-primary-subtle bg-light mb-4" onSubmit={handleSubmit} noValidate>
                <div className="card-body">
                  <h3 className="h6 mb-3">{editingOption ? 'Edit Answer Option' : 'Add Answer Option'}</h3>
                  <ApiErrorAlert error={formError} />

                  {/* Primary Simplified Field: Option Label */}
                  <div className="mb-3">
                    <label className="form-label fw-semibold" htmlFor="optionLabel">Option Label <span className="text-danger">*</span></label>
                    <input
                      id="optionLabel"
                      aria-label="Label"
                      className="form-control form-control-lg"
                      placeholder="e.g. No symptoms"
                      value={form.label}
                      onChange={handleLabelChange}
                      maxLength={255}
                      disabled={saving}
                      required
                      autoFocus
                    />
                  </div>

                  {/* Collapsible Advanced Settings */}
                  <div className="mb-2">
                    <button
                      type="button"
                      className="btn btn-link btn-sm text-decoration-none p-0 d-inline-flex align-items-center gap-1 text-secondary"
                      onClick={() => setShowAdvanced(!showAdvanced)}
                      aria-expanded={showAdvanced}
                      aria-controls="advancedOptionSettings"
                    >
                      <span className="fw-semibold">Advanced settings</span>
                      <span aria-hidden="true">{showAdvanced ? '▴' : '▾'}</span>
                    </button>

                    <div id="advancedOptionSettings" className={showAdvanced ? 'mt-3 p-3 bg-white border rounded' : 'd-none'}>
                      <div className="row g-3">
                        <div className="col-md-4">
                          <label className="form-label small fw-semibold" htmlFor="optionCode">Code</label>
                          <input
                            id="optionCode"
                            className="form-control form-control-sm font-monospace"
                            value={form.code}
                            onChange={(event) => updateAdvancedField('code', event.target.value)}
                            maxLength={64}
                            disabled={saving}
                            required
                          />
                          <div className="form-text small text-muted">Auto-generated from label if not customized.</div>
                        </div>
                        <div className="col-md-5">
                          <label className="form-label small fw-semibold" htmlFor="optionTypedValue">Typed Value</label>
                          <input
                            id="optionTypedValue"
                            className="form-control form-control-sm font-monospace"
                            value={form.typedValue}
                            onChange={(event) => updateAdvancedField('typedValue', event.target.value)}
                            maxLength={255}
                            disabled={saving}
                            required
                          />
                          <div className="form-text small text-muted">Defaults to label if not customized.</div>
                        </div>
                        <div className="col-md-3">
                          <label className="form-label small fw-semibold" htmlFor="optionDisplayOrder">Display Order</label>
                          <input
                            id="optionDisplayOrder"
                            type="number"
                            step="1"
                            min="1"
                            className="form-control form-control-sm"
                            value={form.displayOrder}
                            onChange={(event) => updateAdvancedField('displayOrder', event.target.value)}
                            disabled={saving}
                            required
                          />
                          <div className="form-text small text-muted">Next available order: {nextDisplayOrder}</div>
                        </div>
                      </div>
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

            {/* Configured Options List */}
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
                      <th scope="col" style={{ width: '90px' }}>Order</th>
                      <th scope="col">Option Label</th>
                      <th scope="col" className="text-end">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {options.map((option, index) => (
                      <tr key={option.id}>
                        <td>
                          <div className="d-flex align-items-center gap-1">
                            <span className="badge bg-secondary-subtle text-secondary-emphasis rounded-pill px-2 py-1 fw-bold">
                              {option.displayOrder}
                            </span>
                            <div className="btn-group-vertical btn-group-sm" role="group" aria-label={`Reorder ${option.label}`}>
                              <button
                                type="button"
                                className="btn btn-outline-secondary py-0 px-1"
                                title="Move Up"
                                aria-label={`Move up ${option.label}`}
                                disabled={index === 0 || reordering}
                                onClick={() => handleMove(index, index - 1)}
                              >
                                ▲
                              </button>
                              <button
                                type="button"
                                className="btn btn-outline-secondary py-0 px-1"
                                title="Move Down"
                                aria-label={`Move down ${option.label}`}
                                disabled={index === options.length - 1 || reordering}
                                onClick={() => handleMove(index, index + 1)}
                              >
                                ▼
                              </button>
                            </div>
                          </div>
                        </td>
                        <td>
                          <div className="fw-semibold text-dark">{option.label}</div>
                          <div className="small text-muted font-monospace">
                            <span>Code: {option.code}</span>
                            {option.typedValue && option.typedValue !== option.label && (
                              <span className="ms-2">Value: {option.typedValue}</span>
                            )}
                          </div>
                        </td>
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
            <button type="button" className="btn btn-outline-secondary" onClick={onClose} disabled={saving || deleting || reordering || bulkSaving}>Close</button>
          </div>
        </div>
      </div>
    </div>
  );
}
