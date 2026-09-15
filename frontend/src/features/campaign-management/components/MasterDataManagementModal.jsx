import React, { useState, useEffect, useCallback } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import {
  listAllVaccineTypes,
  createVaccineType,
  updateVaccineType,
  activateVaccineType,
  deactivateVaccineType,
  listAllTargetGroups,
  createTargetGroup,
  updateTargetGroup,
  activateTargetGroup,
  deactivateTargetGroup,
} from '../api/campaignApi';

export default function MasterDataManagementModal({ onClose }) {
  const [activeTab, setActiveTab] = useState('vaccineTypes'); // 'vaccineTypes' | 'targetGroups'

  // Vaccine Types State
  const [vaccineTypes, setVaccineTypes] = useState([]);
  const [vLoading, setVLoading] = useState(false);
  const [vError, setVError] = useState(null);
  const [vSuccess, setVSuccess] = useState(null);

  // Vaccine Type Form State
  const [vFormMode, setVFormMode] = useState(null); // 'create' | 'edit' | null
  const [vFormId, setVFormId] = useState(null);
  const [vCode, setVCode] = useState('');
  const [vName, setVName] = useState('');
  const [vDoseUnit, setVDoseUnit] = useState('dose');
  const [vSubmitting, setVSubmitting] = useState(false);

  // Target Groups State
  const [targetGroups, setTargetGroups] = useState([]);
  const [tgLoading, setTgLoading] = useState(false);
  const [tgError, setTgError] = useState(null);
  const [tgSuccess, setTgSuccess] = useState(null);

  // Target Group Form State
  const [tgFormMode, setTgFormMode] = useState(null); // 'create' | 'edit' | null
  const [tgFormId, setTgFormId] = useState(null);
  const [tgCode, setTgCode] = useState('');
  const [tgDescription, setTgDescription] = useState('');
  const [tgSubmitting, setTgSubmitting] = useState(false);

  // Load Vaccine Types
  const fetchVaccineTypes = useCallback(async () => {
    setVLoading(true);
    setVError(null);
    try {
      const data = await listAllVaccineTypes();
      setVaccineTypes(data || []);
    } catch (err) {
      setVError(err);
    } finally {
      setVLoading(false);
    }
  }, []);

  // Load Target Groups
  const fetchTargetGroups = useCallback(async () => {
    setTgLoading(true);
    setTgError(null);
    try {
      const data = await listAllTargetGroups();
      setTargetGroups(data || []);
    } catch (err) {
      setTgError(err);
    } finally {
      setTgLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchVaccineTypes();
    fetchTargetGroups();
  }, [fetchVaccineTypes, fetchTargetGroups]);

  // Handle Vaccine Type Create / Update Submit
  const handleVFormSubmit = async (e) => {
    e.preventDefault();
    setVError(null);
    setVSuccess(null);
    setVSubmitting(true);
    try {
      if (vFormMode === 'create') {
        const created = await createVaccineType({
          code: vCode.trim(),
          name: vName.trim(),
          doseUnit: vDoseUnit.trim() || 'dose',
        });
        setVSuccess(`Vaccine Type "${created.name}" created successfully.`);
      } else if (vFormMode === 'edit') {
        const updated = await updateVaccineType(vFormId, {
          name: vName.trim(),
          doseUnit: vDoseUnit.trim() || 'dose',
        });
        setVSuccess(`Vaccine Type "${updated.name}" updated successfully.`);
      }
      setVFormMode(null);
      setVFormId(null);
      setVCode('');
      setVName('');
      setVDoseUnit('dose');
      await fetchVaccineTypes();
    } catch (err) {
      setVError(err);
    } finally {
      setVSubmitting(false);
    }
  };

  // Handle Vaccine Type Activate / Deactivate
  const handleVToggleStatus = async (vt) => {
    setVError(null);
    setVSuccess(null);
    try {
      if (vt.active) {
        await deactivateVaccineType(vt.id);
        setVSuccess(`Vaccine Type "${vt.name}" deactivated.`);
      } else {
        await activateVaccineType(vt.id);
        setVSuccess(`Vaccine Type "${vt.name}" activated.`);
      }
      await fetchVaccineTypes();
    } catch (err) {
      setVError(err);
    }
  };

  // Handle Target Group Create / Update Submit
  const handleTgFormSubmit = async (e) => {
    e.preventDefault();
    setTgError(null);
    setTgSuccess(null);
    setTgSubmitting(true);
    try {
      if (tgFormMode === 'create') {
        const created = await createTargetGroup({
          code: tgCode.trim(),
          description: tgDescription.trim(),
        });
        setTgSuccess(`Target Group "${created.code}" created successfully.`);
      } else if (tgFormMode === 'edit') {
        const updated = await updateTargetGroup(tgFormId, {
          description: tgDescription.trim(),
        });
        setTgSuccess(`Target Group "${updated.code}" updated successfully.`);
      }
      setTgFormMode(null);
      setTgFormId(null);
      setTgCode('');
      setTgDescription('');
      await fetchTargetGroups();
    } catch (err) {
      setTgError(err);
    } finally {
      setTgSubmitting(false);
    }
  };

  // Handle Target Group Activate / Deactivate
  const handleTgToggleStatus = async (tg) => {
    setTgError(null);
    setTgSuccess(null);
    try {
      if (tg.active) {
        await deactivateTargetGroup(tg.id);
        setTgSuccess(`Target Group "${tg.code}" deactivated.`);
      } else {
        await activateTargetGroup(tg.id);
        setTgSuccess(`Target Group "${tg.code}" activated.`);
      }
      await fetchTargetGroups();
    } catch (err) {
      setTgError(err);
    }
  };

  return (
    <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-xl modal-dialog-scrollable">
        <div className="modal-content shadow-lg border-0">
          {/* Header */}
          <div className="modal-header bg-light">
            <h5 className="modal-title fw-bold">Master Data Management</h5>
            <button
              type="button"
              className="btn-close"
              aria-label="Close"
              onClick={onClose}
            />
          </div>

          {/* Body */}
          <div className="modal-body p-4">
            <p className="text-secondary small mb-3">
              Manage shared Vaccine Types and Target Demographic Groups for vaccination campaigns.
            </p>

            {/* Navigation Tabs */}
            <ul className="nav nav-pills mb-4">
              <li className="nav-item">
                <button
                  type="button"
                  className={`nav-link fw-semibold ${activeTab === 'vaccineTypes' ? 'active' : ''}`}
                  onClick={() => {
                    setActiveTab('vaccineTypes');
                    setVError(null);
                    setVSuccess(null);
                  }}
                >
                  Vaccine Types ({vaccineTypes.length})
                </button>
              </li>
              <li className="nav-item ms-2">
                <button
                  type="button"
                  className={`nav-link fw-semibold ${activeTab === 'targetGroups' ? 'active' : ''}`}
                  onClick={() => {
                    setActiveTab('targetGroups');
                    setTgError(null);
                    setTgSuccess(null);
                  }}
                >
                  Target Groups ({targetGroups.length})
                </button>
              </li>
            </ul>

            {/* Tab 1: Vaccine Types */}
            {activeTab === 'vaccineTypes' && (
              <div>
                {vSuccess && (
                  <div className="alert alert-success alert-dismissible fade show shadow-sm mb-3" role="alert">
                    <strong className="me-2">Success:</strong> {vSuccess}
                    <button type="button" className="btn-close" onClick={() => setVSuccess(null)} />
                  </div>
                )}
                <ApiErrorAlert error={vError} onDismiss={() => setVError(null)} />

                {/* Create / Edit Form Card */}
                {vFormMode && (
                  <div className="card border mb-4 shadow-sm">
                    <div className="card-header bg-light py-2">
                      <h6 className="card-title fw-bold mb-0 text-dark">
                        {vFormMode === 'create' ? 'Add New Vaccine Type' : `Edit Vaccine Type: ${vCode}`}
                      </h6>
                    </div>
                    <div className="card-body">
                      <form onSubmit={handleVFormSubmit}>
                        <div className="row g-3">
                          {vFormMode === 'create' && (
                            <div className="col-md-4">
                              <label htmlFor="vCodeInput" className="form-label small fw-semibold">
                                Code <span className="text-danger">*</span>
                              </label>
                              <input
                                id="vCodeInput"
                                type="text"
                                className="form-control form-control-sm"
                                placeholder="e.g. VAX-MRNA-01"
                                value={vCode}
                                onChange={(e) => setVCode(e.target.value)}
                                maxLength={64}
                                required
                              />
                            </div>
                          )}
                          <div className={vFormMode === 'create' ? 'col-md-5' : 'col-md-8'}>
                            <label htmlFor="vNameInput" className="form-label small fw-semibold">
                              Name <span className="text-danger">*</span>
                            </label>
                            <input
                              id="vNameInput"
                              type="text"
                              className="form-control form-control-sm"
                              placeholder="e.g. COVID-19 mRNA Vaccine"
                              value={vName}
                              onChange={(e) => setVName(e.target.value)}
                              maxLength={128}
                              required
                            />
                          </div>
                          <div className={vFormMode === 'create' ? 'col-md-3' : 'col-md-4'}>
                            <label htmlFor="vDoseUnitInput" className="form-label small fw-semibold">
                              Dose Unit
                            </label>
                            <input
                              id="vDoseUnitInput"
                              type="text"
                              className="form-control form-control-sm"
                              placeholder="dose"
                              value={vDoseUnit}
                              onChange={(e) => setVDoseUnit(e.target.value)}
                              maxLength={32}
                            />
                          </div>
                        </div>
                        <div className="d-flex justify-content-end gap-2 mt-3">
                          <button
                            type="button"
                            className="btn btn-sm btn-outline-secondary"
                            onClick={() => {
                              setVFormMode(null);
                              setVFormId(null);
                              setVCode('');
                              setVName('');
                              setVDoseUnit('dose');
                            }}
                            disabled={vSubmitting}
                          >
                            Cancel
                          </button>
                          <button
                            type="submit"
                            className="btn btn-sm btn-primary"
                            disabled={vSubmitting}
                          >
                            {vSubmitting ? 'Saving...' : vFormMode === 'create' ? 'Create Vaccine Type' : 'Save Changes'}
                          </button>
                        </div>
                      </form>
                    </div>
                  </div>
                )}

                {/* Table Header Controls */}
                <div className="d-flex justify-content-between align-items-center mb-3">
                  <h6 className="fw-bold mb-0 text-dark">Configured Vaccine Types</h6>
                  {!vFormMode && (
                    <button
                      type="button"
                      className="btn btn-sm btn-primary shadow-sm"
                      onClick={() => {
                        setVFormMode('create');
                        setVFormId(null);
                        setVCode('');
                        setVName('');
                        setVDoseUnit('dose');
                      }}
                    >
                      + Add Vaccine Type
                    </button>
                  )}
                </div>

                {/* Vaccine Types Table */}
                {vLoading ? (
                  <LoadingSpinner message="Loading vaccine types..." />
                ) : (
                  <div className="table-responsive border rounded shadow-sm">
                    <table className="table table-hover align-middle mb-0">
                      <thead className="table-light">
                        <tr>
                          <th className="small text-secondary">Code</th>
                          <th className="small text-secondary">Name</th>
                          <th className="small text-secondary">Dose Unit</th>
                          <th className="small text-secondary">Status</th>
                          <th className="small text-secondary text-end">Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {vaccineTypes.length === 0 ? (
                          <tr>
                            <td colSpan="5" className="text-center text-muted py-4 small">
                              No vaccine types configured.
                            </td>
                          </tr>
                        ) : (
                          vaccineTypes.map((vt) => (
                            <tr key={vt.id} className={!vt.active ? 'table-light text-muted' : ''}>
                              <td className="fw-semibold small">{vt.code}</td>
                              <td className="small">{vt.name}</td>
                              <td className="small">{vt.doseUnit}</td>
                              <td>
                                {vt.active ? (
                                  <span className="badge bg-success-subtle text-success border border-success-subtle">
                                    Active
                                  </span>
                                ) : (
                                  <span className="badge bg-secondary-subtle text-secondary border border-secondary-subtle">
                                    Inactive
                                  </span>
                                )}
                              </td>
                              <td className="text-end">
                                <button
                                  type="button"
                                  className="btn btn-sm btn-outline-secondary me-2 py-0 px-2"
                                  onClick={() => {
                                    setVFormMode('edit');
                                    setVFormId(vt.id);
                                    setVCode(vt.code);
                                    setVName(vt.name);
                                    setVDoseUnit(vt.doseUnit || 'dose');
                                  }}
                                >
                                  Edit
                                </button>
                                <button
                                  type="button"
                                  className={`btn btn-sm py-0 px-2 ${
                                    vt.active ? 'btn-outline-warning' : 'btn-outline-success'
                                  }`}
                                  onClick={() => handleVToggleStatus(vt)}
                                >
                                  {vt.active ? 'Deactivate' : 'Activate'}
                                </button>
                              </td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}

            {/* Tab 2: Target Groups */}
            {activeTab === 'targetGroups' && (
              <div>
                {tgSuccess && (
                  <div className="alert alert-success alert-dismissible fade show shadow-sm mb-3" role="alert">
                    <strong className="me-2">Success:</strong> {tgSuccess}
                    <button type="button" className="btn-close" onClick={() => setTgSuccess(null)} />
                  </div>
                )}
                <ApiErrorAlert error={tgError} onDismiss={() => setTgError(null)} />

                {/* Create / Edit Form Card */}
                {tgFormMode && (
                  <div className="card border mb-4 shadow-sm">
                    <div className="card-header bg-light py-2">
                      <h6 className="card-title fw-bold mb-0 text-dark">
                        {tgFormMode === 'create' ? 'Add New Target Group' : `Edit Target Group: ${tgCode}`}
                      </h6>
                    </div>
                    <div className="card-body">
                      <form onSubmit={handleTgFormSubmit}>
                        <div className="row g-3">
                          {tgFormMode === 'create' && (
                            <div className="col-md-4">
                              <label htmlFor="tgCodeInput" className="form-label small fw-semibold">
                                Code <span className="text-danger">*</span>
                              </label>
                              <input
                                id="tgCodeInput"
                                type="text"
                                className="form-control form-control-sm"
                                placeholder="e.g. TG-ELDERLY-65"
                                value={tgCode}
                                onChange={(e) => setTgCode(e.target.value)}
                                maxLength={64}
                                required
                              />
                            </div>
                          )}
                          <div className={tgFormMode === 'create' ? 'col-md-8' : 'col-md-12'}>
                            <label htmlFor="tgDescInput" className="form-label small fw-semibold">
                              Description <span className="text-danger">*</span>
                            </label>
                            <input
                              id="tgDescInput"
                              type="text"
                              className="form-control form-control-sm"
                              placeholder="e.g. Senior Citizens aged 65 and above"
                              value={tgDescription}
                              onChange={(e) => setTgDescription(e.target.value)}
                              maxLength={255}
                              required
                            />
                          </div>
                        </div>
                        <div className="d-flex justify-content-end gap-2 mt-3">
                          <button
                            type="button"
                            className="btn btn-sm btn-outline-secondary"
                            onClick={() => {
                              setTgFormMode(null);
                              setTgFormId(null);
                              setTgCode('');
                              setTgDescription('');
                            }}
                            disabled={tgSubmitting}
                          >
                            Cancel
                          </button>
                          <button
                            type="submit"
                            className="btn btn-sm btn-primary"
                            disabled={tgSubmitting}
                          >
                            {tgSubmitting ? 'Saving...' : tgFormMode === 'create' ? 'Create Target Group' : 'Save Changes'}
                          </button>
                        </div>
                      </form>
                    </div>
                  </div>
                )}

                {/* Table Header Controls */}
                <div className="d-flex justify-content-between align-items-center mb-3">
                  <h6 className="fw-bold mb-0 text-dark">Configured Target Groups</h6>
                  {!tgFormMode && (
                    <button
                      type="button"
                      className="btn btn-sm btn-primary shadow-sm"
                      onClick={() => {
                        setTgFormMode('create');
                        setTgFormId(null);
                        setTgCode('');
                        setTgDescription('');
                      }}
                    >
                      + Add Target Group
                    </button>
                  )}
                </div>

                {/* Target Groups Table */}
                {tgLoading ? (
                  <LoadingSpinner message="Loading target groups..." />
                ) : (
                  <div className="table-responsive border rounded shadow-sm">
                    <table className="table table-hover align-middle mb-0">
                      <thead className="table-light">
                        <tr>
                          <th className="small text-secondary">Code</th>
                          <th className="small text-secondary">Description</th>
                          <th className="small text-secondary">Status</th>
                          <th className="small text-secondary text-end">Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {targetGroups.length === 0 ? (
                          <tr>
                            <td colSpan="4" className="text-center text-muted py-4 small">
                              No target groups configured.
                            </td>
                          </tr>
                        ) : (
                          targetGroups.map((tg) => (
                            <tr key={tg.id} className={!tg.active ? 'table-light text-muted' : ''}>
                              <td className="fw-semibold small">{tg.code}</td>
                              <td className="small">{tg.description}</td>
                              <td>
                                {tg.active ? (
                                  <span className="badge bg-success-subtle text-success border border-success-subtle">
                                    Active
                                  </span>
                                ) : (
                                  <span className="badge bg-secondary-subtle text-secondary border border-secondary-subtle">
                                    Inactive
                                  </span>
                                )}
                              </td>
                              <td className="text-end">
                                <button
                                  type="button"
                                  className="btn btn-sm btn-outline-secondary me-2 py-0 px-2"
                                  onClick={() => {
                                    setTgFormMode('edit');
                                    setTgFormId(tg.id);
                                    setTgCode(tg.code);
                                    setTgDescription(tg.description);
                                  }}
                                >
                                  Edit
                                </button>
                                <button
                                  type="button"
                                  className={`btn btn-sm py-0 px-2 ${
                                    tg.active ? 'btn-outline-warning' : 'btn-outline-success'
                                  }`}
                                  onClick={() => handleTgToggleStatus(tg)}
                                >
                                  {tg.active ? 'Deactivate' : 'Activate'}
                                </button>
                              </td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="modal-footer bg-light py-2">
            <button type="button" className="btn btn-sm btn-secondary" onClick={onClose}>
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
