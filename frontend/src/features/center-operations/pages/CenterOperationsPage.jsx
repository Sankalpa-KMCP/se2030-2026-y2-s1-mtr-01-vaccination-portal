import React, { useState, useEffect, useCallback } from 'react';
import {
  listCenters,
  getCenterById,
  createCenter,
  updateCenter,
  activateCenter,
  deactivateCenter,
  getOperatingHours,
  createOperatingHours,
  updateOperatingHours,
  endDateOperatingHours,
  getClosures,
  createClosure,
  updateClosure,
  cancelClosure,
  listOperationalResources,
  createOperationalResource,
  updateOperationalResource,
  listStaffRoster,
  registerStaffMember,
  updateStaffRoster,
} from '../api/centerOperationsApi';
import CreateCenterModal from '../components/CreateCenterModal';
import EditCenterModal from '../components/EditCenterModal';
import OperatingHoursPanel from '../components/OperatingHoursPanel';
import OperatingHoursModal from '../components/OperatingHoursModal';
import CenterClosuresPanel from '../components/CenterClosuresPanel';
import CenterClosureModal from '../components/CenterClosureModal';
import OperationalResourcesPanel from '../components/OperationalResourcesPanel';
import OperationalResourceModal from '../components/OperationalResourceModal';
import StaffRosterPanel from '../components/StaffRosterPanel';
import StaffRosterModal from '../components/StaffRosterModal';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import { useAuth } from '../../../auth/context/AuthContext';

export default function CenterOperationsPage() {
  const { user } = useAuth();
  const isAuthorizedManager = Boolean(
    user?.roles?.some((r) => r === 'ROLE_ADMIN' || r === 'ROLE_CENTER_MANAGER')
  );

  // Center Registry State
  const [centers, setCenters] = useState([]);
  const [selectedCenterId, setSelectedCenterId] = useState('');
  const [selectedCenterDetails, setSelectedCenterDetails] = useState(null);
  const [centersLoading, setCentersLoading] = useState(true);
  const [centersError, setCentersError] = useState(null);

  // Sub-tab Navigation: 'hours' | 'closures' | 'resources' | 'staff'
  const [activeTab, setActiveTab] = useState('hours');

  // Operating Hours State
  const [operatingHours, setOperatingHours] = useState([]);
  const [hoursLoading, setHoursLoading] = useState(false);
  const [hoursError, setHoursError] = useState(null);

  // Center Closures State
  const [closures, setClosures] = useState([]);
  const [closuresLoading, setClosuresLoading] = useState(false);
  const [closuresError, setClosuresError] = useState(null);

  // Operational Resources State
  const [resources, setResources] = useState([]);
  const [resourcesLoading, setResourcesLoading] = useState(false);
  const [resourcesError, setResourcesError] = useState(null);

  // Operational Staff Roster State
  const [staffRoster, setStaffRoster] = useState([]);
  const [staffLoading, setStaffLoading] = useState(false);
  const [staffError, setStaffError] = useState(null);

  // --- Center Mutation Modals & Actions State ---
  const [createCenterShow, setCreateCenterShow] = useState(false);
  const [createCenterLoading, setCreateCenterLoading] = useState(false);
  const [createCenterError, setCreateCenterError] = useState(null);

  const [editCenterShow, setEditCenterShow] = useState(false);
  const [editCenterLoading, setEditCenterLoading] = useState(false);
  const [editCenterError, setEditCenterError] = useState(null);

  const [statusConfirmAction, setStatusConfirmAction] = useState(null); // 'ACTIVATE' | 'DEACTIVATE' | null
  const [statusMutationLoading, setStatusMutationLoading] = useState(false);
  const [statusMutationError, setStatusMutationError] = useState(null);

  // --- Operating Hours Modal State ---
  const [hoursModalShow, setHoursModalShow] = useState(false);
  const [hoursModalMode, setHoursModalMode] = useState('create'); // 'create' | 'edit' | 'end-date'
  const [editingHour, setEditingHour] = useState(null);
  const [hoursMutationLoading, setHoursMutationLoading] = useState(false);
  const [hoursMutationError, setHoursMutationError] = useState(null);

  // --- Center Closure Modal State ---
  const [closureModalShow, setClosureModalShow] = useState(false);
  const [closureModalMode, setClosureModalMode] = useState('create'); // 'create' | 'edit'
  const [editingClosure, setEditingClosure] = useState(null);
  const [closureMutationLoading, setClosureMutationLoading] = useState(false);
  const [closureMutationError, setClosureMutationError] = useState(null);

  // --- Resource Modal State ---
  const [resourceModalShow, setResourceModalShow] = useState(false);
  const [resourceModalMode, setResourceModalMode] = useState('create'); // 'create' | 'edit'
  const [editingResource, setEditingResource] = useState(null);
  const [resourceMutationLoading, setResourceMutationLoading] = useState(false);
  const [resourceMutationError, setResourceMutationError] = useState(null);

  // --- Staff Modal State ---
  const [staffModalShow, setStaffModalShow] = useState(false);
  const [staffModalMode, setStaffModalMode] = useState('register'); // 'register' | 'edit'
  const [editingStaff, setEditingStaff] = useState(null);
  const [staffMutationLoading, setStaffMutationLoading] = useState(false);
  const [staffMutationError, setStaffMutationError] = useState(null);

  // Fetch Centers
  const fetchCenters = useCallback(async (preferredSelectId = null) => {
    setCentersLoading(true);
    setCentersError(null);
    try {
      const data = await listCenters();
      const centerList = Array.isArray(data) ? data : [];
      setCenters(centerList);

      if (centerList.length > 0) {
        setSelectedCenterId((prev) => {
          if (preferredSelectId && centerList.some((c) => String(c.id) === String(preferredSelectId))) {
            return String(preferredSelectId);
          }
          if (prev && centerList.some((c) => String(c.id) === String(prev))) {
            return prev;
          }
          const firstActive = centerList.find((c) => c.status === 'ACTIVE');
          return firstActive ? String(firstActive.id) : String(centerList[0].id);
        });
      } else {
        setSelectedCenterId('');
        setSelectedCenterDetails(null);
      }
    } catch (err) {
      setCentersError(err);
    } finally {
      setCentersLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchCenters();
  }, [fetchCenters]);

  // Fetch Detailed Center Record
  const fetchCenterDetails = useCallback(async (centerId) => {
    if (!centerId) {
      setSelectedCenterDetails(null);
      return;
    }
    try {
      const details = await getCenterById(centerId);
      setSelectedCenterDetails(details);
    } catch {
      // If detailed endpoint fails, fall back to summary from list
      setSelectedCenterDetails((prev) => prev);
    }
  }, []);

  // Fetch Operating Hours
  const fetchHours = useCallback(async (centerId) => {
    if (!centerId) {
      setOperatingHours([]);
      return;
    }
    setHoursLoading(true);
    setHoursError(null);
    try {
      const data = await getOperatingHours(centerId);
      setOperatingHours(Array.isArray(data) ? data : []);
    } catch (err) {
      setHoursError(err);
      setOperatingHours([]);
    } finally {
      setHoursLoading(false);
    }
  }, []);

  // Fetch Closures
  const fetchClosuresData = useCallback(async (centerId) => {
    if (!centerId) {
      setClosures([]);
      return;
    }
    setClosuresLoading(true);
    setClosuresError(null);
    try {
      const data = await getClosures(centerId);
      setClosures(Array.isArray(data) ? data : []);
    } catch (err) {
      setClosuresError(err);
      setClosures([]);
    } finally {
      setClosuresLoading(false);
    }
  }, []);

  // Fetch Resources
  const fetchResources = useCallback(async (centerId) => {
    if (!centerId) {
      setResources([]);
      return;
    }
    setResourcesLoading(true);
    setResourcesError(null);
    try {
      const data = await listOperationalResources(centerId);
      setResources(Array.isArray(data) ? data : []);
    } catch (err) {
      setResourcesError(err);
      setResources([]);
    } finally {
      setResourcesLoading(false);
    }
  }, []);

  // Fetch Staff Roster
  const fetchStaff = useCallback(async (centerId) => {
    if (!centerId) {
      setStaffRoster([]);
      return;
    }
    setStaffLoading(true);
    setStaffError(null);
    try {
      const data = await listStaffRoster(centerId);
      setStaffRoster(Array.isArray(data) ? data : []);
    } catch (err) {
      setStaffError(err);
      setStaffRoster([]);
    } finally {
      setStaffLoading(false);
    }
  }, []);

  // Handle center selection change
  useEffect(() => {
    if (selectedCenterId) {
      // Clear previous datasets immediately for state isolation
      setOperatingHours([]);
      setHoursError(null);
      setClosures([]);
      setClosuresError(null);
      setResources([]);
      setResourcesError(null);
      setStaffRoster([]);
      setStaffError(null);

      // Close all modals
      setEditCenterShow(false);
      setStatusConfirmAction(null);
      setHoursModalShow(false);
      setClosureModalShow(false);
      setResourceModalShow(false);
      setStaffModalShow(false);

      fetchCenterDetails(selectedCenterId);
      fetchHours(selectedCenterId);
      fetchClosuresData(selectedCenterId);
      fetchResources(selectedCenterId);
      fetchStaff(selectedCenterId);
    }
  }, [selectedCenterId, fetchCenterDetails, fetchHours, fetchClosuresData, fetchResources, fetchStaff]);

  // Derive selected center summary or detailed object
  const selectedCenterSummary = centers.find((c) => String(c.id) === String(selectedCenterId)) || null;
  const effectiveCenter = selectedCenterDetails || selectedCenterSummary;

  // ===============================================================================================
  // Center Creation & Edit & Status Action Handlers
  // ===============================================================================================

  const handleOpenCreateCenterModal = () => {
    setCreateCenterError(null);
    setCreateCenterShow(true);
  };

  const handleCreateCenterSubmit = async (payload) => {
    setCreateCenterLoading(true);
    setCreateCenterError(null);
    try {
      const created = await createCenter(payload);
      setCreateCenterShow(false);
      await fetchCenters(created?.id);
    } catch (err) {
      setCreateCenterError(err);
    } finally {
      setCreateCenterLoading(false);
    }
  };

  const handleOpenEditCenterModal = () => {
    setEditCenterError(null);
    setEditCenterShow(true);
  };

  const handleEditCenterSubmit = async (payload) => {
    if (!selectedCenterId) return;
    setEditCenterLoading(true);
    setEditCenterError(null);
    try {
      const updated = await updateCenter(selectedCenterId, payload);
      setSelectedCenterDetails(updated);
      setEditCenterShow(false);
      await fetchCenters(selectedCenterId);
    } catch (err) {
      setEditCenterError(err);
    } finally {
      setEditCenterLoading(false);
    }
  };

  const handleStatusActionConfirm = async () => {
    if (!selectedCenterId || !statusConfirmAction) return;
    setStatusMutationLoading(true);
    setStatusMutationError(null);
    try {
      let updated;
      if (statusConfirmAction === 'ACTIVATE') {
        updated = await activateCenter(selectedCenterId);
      } else {
        updated = await deactivateCenter(selectedCenterId);
      }
      setSelectedCenterDetails(updated);
      setStatusConfirmAction(null);
      await fetchCenters(selectedCenterId);
    } catch (err) {
      setStatusMutationError(err);
    } finally {
      setStatusMutationLoading(false);
    }
  };

  // ===============================================================================================
  // Operating Hours Handlers
  // ===============================================================================================

  const handleOpenCreateHoursModal = () => {
    setHoursModalMode('create');
    setEditingHour(null);
    setHoursMutationError(null);
    setHoursModalShow(true);
  };

  const handleOpenEditHoursModal = (hour) => {
    setHoursModalMode('edit');
    setEditingHour(hour);
    setHoursMutationError(null);
    setHoursModalShow(true);
  };

  const handleOpenEndDateHoursModal = (hour) => {
    setHoursModalMode('end-date');
    setEditingHour(hour);
    setHoursMutationError(null);
    setHoursModalShow(true);
  };

  const handleHoursModalSubmit = async (payload) => {
    if (!selectedCenterId) return;
    setHoursMutationLoading(true);
    setHoursMutationError(null);
    try {
      if (hoursModalMode === 'create') {
        await createOperatingHours(selectedCenterId, payload);
      } else if (hoursModalMode === 'edit' && editingHour) {
        await updateOperatingHours(selectedCenterId, editingHour.id, payload);
      } else if (hoursModalMode === 'end-date' && editingHour) {
        await endDateOperatingHours(selectedCenterId, editingHour.id, payload);
      }
      setHoursModalShow(false);
      await fetchHours(selectedCenterId);
    } catch (err) {
      setHoursMutationError(err);
    } finally {
      setHoursMutationLoading(false);
    }
  };

  // ===============================================================================================
  // Center Closures Handlers
  // ===============================================================================================

  const handleOpenCreateClosureModal = () => {
    setClosureModalMode('create');
    setEditingClosure(null);
    setClosureMutationError(null);
    setClosureModalShow(true);
  };

  const handleOpenEditClosureModal = (closure) => {
    setClosureModalMode('edit');
    setEditingClosure(closure);
    setClosureMutationError(null);
    setClosureModalShow(true);
  };

  const handleClosureModalSubmit = async (payload) => {
    if (!selectedCenterId) return;
    setClosureMutationLoading(true);
    setClosureMutationError(null);
    try {
      if (closureModalMode === 'create') {
        await createClosure(selectedCenterId, payload);
      } else if (closureModalMode === 'edit' && editingClosure) {
        await updateClosure(selectedCenterId, editingClosure.id, payload);
      }
      setClosureModalShow(false);
      await fetchClosuresData(selectedCenterId);
    } catch (err) {
      setClosureMutationError(err);
    } finally {
      setClosureMutationLoading(false);
    }
  };

  const handleCancelClosure = async (closureId) => {
    if (!selectedCenterId || !closureId) return;
    await cancelClosure(selectedCenterId, closureId);
    await fetchClosuresData(selectedCenterId);
  };

  // ===============================================================================================
  // Resource Modal Handlers (F3A Preserved)
  // ===============================================================================================

  const handleOpenCreateResourceModal = () => {
    setResourceModalMode('create');
    setEditingResource(null);
    setResourceMutationError(null);
    setResourceModalShow(true);
  };

  const handleOpenEditResourceModal = (resource) => {
    setResourceModalMode('edit');
    setEditingResource(resource);
    setResourceMutationError(null);
    setResourceModalShow(true);
  };

  const handleResourceModalSubmit = async (payload) => {
    if (!selectedCenterId) return;
    setResourceMutationLoading(true);
    setResourceMutationError(null);
    try {
      if (resourceModalMode === 'create') {
        await createOperationalResource(selectedCenterId, payload);
      } else if (resourceModalMode === 'edit' && editingResource) {
        await updateOperationalResource(selectedCenterId, editingResource.id, payload);
      }
      setResourceModalShow(false);
      await fetchResources(selectedCenterId);
    } catch (err) {
      setResourceMutationError(err);
    } finally {
      setResourceMutationLoading(false);
    }
  };

  // ===============================================================================================
  // Staff Modal Handlers (F3B Preserved)
  // ===============================================================================================

  const handleOpenRegisterStaffModal = () => {
    setStaffModalMode('register');
    setEditingStaff(null);
    setStaffMutationError(null);
    setStaffModalShow(true);
  };

  const handleOpenEditStaffModal = (member) => {
    setStaffModalMode('edit');
    setEditingStaff(member);
    setStaffMutationError(null);
    setStaffModalShow(true);
  };

  const handleStaffModalSubmit = async (payload) => {
    if (!selectedCenterId) return;
    setStaffMutationLoading(true);
    setStaffMutationError(null);
    try {
      if (staffModalMode === 'register') {
        await registerStaffMember(selectedCenterId, payload);
      } else if (staffModalMode === 'edit' && editingStaff) {
        await updateStaffRoster(selectedCenterId, editingStaff.id, payload);
      }
      setStaffModalShow(false);
      await fetchStaff(selectedCenterId);
    } catch (err) {
      setStaffMutationError(err);
    } finally {
      setStaffMutationLoading(false);
    }
  };

  const isDataLoading = hoursLoading || closuresLoading || resourcesLoading || staffLoading;

  return (
    <div className="container-fluid px-0 py-2">
      {/* Page Header */}
      <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center mb-4 gap-3">
        <div>
          <h1 className="h3 text-primary fw-bold mb-1">
            <i className="bi bi-hospital me-2"></i>
            Vaccination Center Operations
          </h1>
          <p className="text-secondary mb-0">
            Center lifecycle registry, operational calendar, operating hours, closures, and physical workforce management.
          </p>
        </div>

        <div className="d-flex align-items-center gap-2">
          <button
            type="button"
            className="btn btn-outline-primary btn-sm"
            onClick={() => fetchCenters(selectedCenterId)}
            disabled={centersLoading}
          >
            <i className="bi bi-arrow-clockwise me-1"></i>
            Refresh Centers
          </button>

          {isAuthorizedManager && (
            <button
              type="button"
              className="btn btn-primary btn-sm fw-semibold shadow-sm"
              onClick={handleOpenCreateCenterModal}
            >
              <i className="bi bi-plus-lg me-1"></i>
              + New Center
            </button>
          )}
        </div>
      </div>

      <ApiErrorAlert error={centersError || statusMutationError} onDismiss={() => setStatusMutationError(null)} />

      {centersLoading ? (
        <div className="card shadow-sm border-0 py-5">
          <LoadingSpinner message="Loading vaccination center registry..." />
        </div>
      ) : centers.length === 0 ? (
        <div className="card shadow-sm border-0 py-5 text-center">
          <div className="card-body p-5">
            <div className="display-4 text-muted mb-3">🏥</div>
            <h4 className="h5 text-dark fw-bold mb-2">No Vaccination Centers Registered</h4>
            <p className="text-muted small mb-4">
              There are currently no vaccination centers registered in the system.
            </p>
            {isAuthorizedManager && (
              <button
                type="button"
                className="btn btn-primary fw-semibold"
                onClick={handleOpenCreateCenterModal}
              >
                <i className="bi bi-plus-lg me-1"></i>
                Register First Center
              </button>
            )}
          </div>
        </div>
      ) : (
        <>
          {/* Center Selector and Summary Card */}
          <div className="card shadow-sm border-0 mb-4">
            <div className="card-body p-4">
              <div className="row g-3 align-items-center">
                <div className="col-lg-5 col-md-6">
                  <label htmlFor="centerSelect" className="form-label fw-bold text-dark small mb-1">
                    Select Vaccination Center:
                  </label>
                  <select
                    id="centerSelect"
                    className="form-select"
                    value={selectedCenterId}
                    onChange={(e) => setSelectedCenterId(e.target.value)}
                    disabled={isDataLoading}
                  >
                    {centers.map((c) => (
                      <option key={c.id} value={c.id}>
                        [{c.code}] {c.name} {c.status === 'INACTIVE' ? '(INACTIVE)' : ''}
                      </option>
                    ))}
                  </select>
                </div>

                {effectiveCenter && (
                  <div className="col-lg-7 col-md-6 border-start-md ps-md-4">
                    <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-2">
                      <div className="d-flex flex-wrap align-items-center gap-2">
                        <span className="fw-bold text-dark fs-5">{effectiveCenter.name}</span>
                        <span className="badge bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle font-monospace">
                          {effectiveCenter.code}
                        </span>
                        {effectiveCenter.status === 'ACTIVE' ? (
                          <span className="badge bg-success-subtle text-success-emphasis border border-success-subtle">
                            Active
                          </span>
                        ) : (
                          <span className="badge bg-danger-subtle text-danger-emphasis border border-danger-subtle">
                            Inactive (Decommissioned)
                          </span>
                        )}
                      </div>

                      {isAuthorizedManager && (
                        <div className="btn-group btn-group-sm" role="group">
                          <button
                            type="button"
                            className="btn btn-outline-secondary"
                            onClick={handleOpenEditCenterModal}
                            title="Edit center details"
                          >
                            <i className="bi bi-pencil me-1"></i>
                            Edit Details
                          </button>
                          {effectiveCenter.status === 'ACTIVE' ? (
                            <button
                              type="button"
                              className="btn btn-outline-danger"
                              onClick={() => setStatusConfirmAction('DEACTIVATE')}
                              title="Deactivate vaccination center"
                            >
                              <i className="bi bi-pause-circle me-1"></i>
                              Deactivate
                            </button>
                          ) : (
                            <button
                              type="button"
                              className="btn btn-outline-success"
                              onClick={() => setStatusConfirmAction('ACTIVATE')}
                              title="Activate vaccination center"
                            >
                              <i className="bi bi-play-circle me-1"></i>
                              Activate
                            </button>
                          )}
                        </div>
                      )}
                    </div>

                    <div className="row g-2 small text-muted">
                      {effectiveCenter.address && (
                        <div className="col-12">
                          <i className="bi bi-geo-alt me-1"></i>
                          Address: <span className="fw-semibold text-dark">{effectiveCenter.address}</span>
                        </div>
                      )}
                      {effectiveCenter.contact && (
                        <div className="col-12">
                          <i className="bi bi-telephone me-1"></i>
                          Contact: <span className="fw-semibold text-dark">{effectiveCenter.contact}</span>
                        </div>
                      )}
                      <div className="col-sm-4">
                        <i className="bi bi-clock me-1"></i>
                        Timezone: <span className="fw-semibold text-dark">{effectiveCenter.timezone || 'UTC'}</span>
                      </div>
                      <div className="col-sm-4">
                        <i className="bi bi-people me-1"></i>
                        Safe Occupancy: <span className="fw-semibold text-dark">{effectiveCenter.safeConcurrentOccupancy || 'N/A'}</span>
                      </div>
                      <div className="col-sm-4">
                        <i className="bi bi-speedometer me-1"></i>
                        Service Ceiling: <span className="fw-semibold text-dark">{effectiveCenter.sessionServiceCeiling || 'Unrestricted'}</span>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Sub-navigation Tabs */}
          {effectiveCenter && (
            <div className="mb-4">
              <ul className="nav nav-pills border-bottom pb-2">
                <li className="nav-item me-2">
                  <button
                    type="button"
                    className={`nav-link fw-semibold px-3 py-2 d-flex align-items-center gap-2 ${
                      activeTab === 'hours' ? 'active shadow-sm' : 'text-secondary'
                    }`}
                    onClick={() => setActiveTab('hours')}
                  >
                    <i className="bi bi-clock-history"></i>
                    <span>Operating Hours</span>
                    <span className={`badge rounded-pill ${activeTab === 'hours' ? 'bg-light text-primary' : 'bg-secondary-subtle text-secondary'}`}>
                      {operatingHours.length}
                    </span>
                  </button>
                </li>
                <li className="nav-item me-2">
                  <button
                    type="button"
                    className={`nav-link fw-semibold px-3 py-2 d-flex align-items-center gap-2 ${
                      activeTab === 'closures' ? 'active shadow-sm' : 'text-secondary'
                    }`}
                    onClick={() => setActiveTab('closures')}
                  >
                    <i className="bi bi-calendar-x"></i>
                    <span>Closures</span>
                    <span className={`badge rounded-pill ${activeTab === 'closures' ? 'bg-light text-primary' : 'bg-secondary-subtle text-secondary'}`}>
                      {closures.length}
                    </span>
                  </button>
                </li>
                <li className="nav-item me-2">
                  <button
                    type="button"
                    className={`nav-link fw-semibold px-3 py-2 d-flex align-items-center gap-2 ${
                      activeTab === 'resources' ? 'active shadow-sm' : 'text-secondary'
                    }`}
                    onClick={() => setActiveTab('resources')}
                  >
                    <i className="bi bi-grid-3x3-gap"></i>
                    <span>Physical Resources</span>
                    <span className={`badge rounded-pill ${activeTab === 'resources' ? 'bg-light text-primary' : 'bg-secondary-subtle text-secondary'}`}>
                      {resources.length}
                    </span>
                  </button>
                </li>
                <li className="nav-item">
                  <button
                    type="button"
                    className={`nav-link fw-semibold px-3 py-2 d-flex align-items-center gap-2 ${
                      activeTab === 'staff' ? 'active shadow-sm' : 'text-secondary'
                    }`}
                    onClick={() => setActiveTab('staff')}
                  >
                    <i className="bi bi-people"></i>
                    <span>Staff Roster</span>
                    <span className={`badge rounded-pill ${activeTab === 'staff' ? 'bg-light text-primary' : 'bg-secondary-subtle text-secondary'}`}>
                      {staffRoster.length}
                    </span>
                  </button>
                </li>
              </ul>
            </div>
          )}

          {/* Tab 1: Operating Hours Panel */}
          {effectiveCenter && activeTab === 'hours' && (
            <OperatingHoursPanel
              center={effectiveCenter}
              operatingHours={operatingHours}
              loading={hoursLoading}
              error={hoursError}
              isAuthorized={isAuthorizedManager}
              onRefresh={() => fetchHours(selectedCenterId)}
              onAddHours={handleOpenCreateHoursModal}
              onEditHours={handleOpenEditHoursModal}
              onEndDateHours={handleOpenEndDateHoursModal}
            />
          )}

          {/* Tab 2: Center Closures Panel */}
          {effectiveCenter && activeTab === 'closures' && (
            <CenterClosuresPanel
              center={effectiveCenter}
              closures={closures}
              loading={closuresLoading}
              error={closuresError}
              isAuthorized={isAuthorizedManager}
              onRefresh={() => fetchClosuresData(selectedCenterId)}
              onAddClosure={handleOpenCreateClosureModal}
              onEditClosure={handleOpenEditClosureModal}
              onCancelClosure={handleCancelClosure}
            />
          )}

          {/* Tab 3: Physical Resources Panel */}
          {effectiveCenter && activeTab === 'resources' && (
            <OperationalResourcesPanel
              center={effectiveCenter}
              resources={resources}
              loading={resourcesLoading}
              error={resourcesError}
              onRefresh={() => fetchResources(selectedCenterId)}
              onAddResource={handleOpenCreateResourceModal}
              onEditResource={handleOpenEditResourceModal}
            />
          )}

          {/* Tab 4: Staff Roster Panel */}
          {effectiveCenter && activeTab === 'staff' && (
            <StaffRosterPanel
              center={effectiveCenter}
              staffRoster={staffRoster}
              loading={staffLoading}
              error={staffError}
              onRefresh={() => fetchStaff(selectedCenterId)}
              onRegisterClick={handleOpenRegisterStaffModal}
              onEditClick={handleOpenEditStaffModal}
            />
          )}

          {/* Create Center Modal */}
          <CreateCenterModal
            show={createCenterShow}
            loading={createCenterLoading}
            apiError={createCenterError}
            onClose={() => setCreateCenterShow(false)}
            onSubmit={handleCreateCenterSubmit}
          />

          {/* Edit Center Modal */}
          <EditCenterModal
            show={editCenterShow}
            center={effectiveCenter}
            loading={editCenterLoading}
            apiError={editCenterError}
            onClose={() => setEditCenterShow(false)}
            onSubmit={handleEditCenterSubmit}
          />

          {/* Operating Hours Modal */}
          <OperatingHoursModal
            show={hoursModalShow}
            mode={hoursModalMode}
            center={effectiveCenter}
            operatingHour={editingHour}
            loading={hoursMutationLoading}
            apiError={hoursMutationError}
            onClose={() => setHoursModalShow(false)}
            onSubmit={handleHoursModalSubmit}
          />

          {/* Center Closure Modal */}
          <CenterClosureModal
            show={closureModalShow}
            mode={closureModalMode}
            center={effectiveCenter}
            closure={editingClosure}
            loading={closureMutationLoading}
            apiError={closureMutationError}
            onClose={() => setClosureModalShow(false)}
            onSubmit={handleClosureModalSubmit}
          />

          {/* Create / Edit Resource Modal */}
          <OperationalResourceModal
            show={resourceModalShow}
            mode={resourceModalMode}
            center={effectiveCenter}
            resource={editingResource}
            loading={resourceMutationLoading}
            apiError={resourceMutationError}
            onClose={() => setResourceModalShow(false)}
            onSubmit={handleResourceModalSubmit}
          />

          {/* Register / Edit Staff Modal */}
          <StaffRosterModal
            show={staffModalShow}
            mode={staffModalMode}
            center={effectiveCenter}
            rosterEntry={editingStaff}
            loading={staffMutationLoading}
            error={staffMutationError}
            onClose={() => setStaffModalShow(false)}
            onSubmit={handleStaffModalSubmit}
          />

          {/* Status Action Confirmation Modal */}
          {statusConfirmAction && (
            <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
              <div className="modal-dialog modal-dialog-centered">
                <div className="modal-content shadow-lg border-0">
                  <div className={`modal-header ${statusConfirmAction === 'DEACTIVATE' ? 'bg-danger' : 'bg-success'} text-white`}>
                    <h5 className="modal-title">
                      <i className={`bi ${statusConfirmAction === 'DEACTIVATE' ? 'bi-pause-circle' : 'bi-play-circle'} me-2`}></i>
                      Confirm Center {statusConfirmAction === 'DEACTIVATE' ? 'Deactivation' : 'Activation'}
                    </h5>
                    <button
                      type="button"
                      className="btn-close btn-close-white"
                      aria-label="Close"
                      onClick={() => setStatusConfirmAction(null)}
                      disabled={statusMutationLoading}
                    ></button>
                  </div>
                  <div className="modal-body p-4">
                    <p className="mb-2 text-dark">
                      Are you sure you want to {statusConfirmAction === 'DEACTIVATE' ? 'deactivate' : 'activate'} <strong>{effectiveCenter?.name}</strong> ({effectiveCenter?.code})?
                    </p>
                    {statusConfirmAction === 'DEACTIVATE' ? (
                      <div className="alert alert-warning small border mb-0">
                        <strong>Deactivation notice:</strong> Inactive centers cannot host active session occurrences or have new resources/staff registered. Existing schedules and history remain preserved.
                      </div>
                    ) : (
                      <div className="alert alert-info small border mb-0">
                        <strong>Activation notice:</strong> Activating this center allows session scheduling and appointment booking within approved operating hours.
                      </div>
                    )}
                  </div>
                  <div className="modal-footer bg-light">
                    <button
                      type="button"
                      className="btn btn-outline-secondary btn-sm"
                      onClick={() => setStatusConfirmAction(null)}
                      disabled={statusMutationLoading}
                    >
                      Cancel
                    </button>
                    <button
                      type="button"
                      className={`btn btn-sm fw-semibold ${statusConfirmAction === 'DEACTIVATE' ? 'btn-danger' : 'btn-success'}`}
                      onClick={handleStatusActionConfirm}
                      disabled={statusMutationLoading}
                    >
                      {statusMutationLoading ? (
                        <>
                          <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                          Processing...
                        </>
                      ) : (
                        `Yes, ${statusConfirmAction === 'DEACTIVATE' ? 'Deactivate Center' : 'Activate Center'}`
                      )}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
