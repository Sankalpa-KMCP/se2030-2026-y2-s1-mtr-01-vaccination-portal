import React, { useState, useEffect, useCallback } from 'react';
import {
  listCenters,
  listOperationalResources,
  createOperationalResource,
  updateOperationalResource,
  listStaffRoster,
  registerStaffMember,
  updateStaffRoster,
} from '../api/centerOperationsApi';
import OperationalResourcesPanel from '../components/OperationalResourcesPanel';
import OperationalResourceModal from '../components/OperationalResourceModal';
import StaffRosterPanel from '../components/StaffRosterPanel';
import StaffRosterModal from '../components/StaffRosterModal';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

export default function CenterOperationsPage() {
  const [centers, setCenters] = useState([]);
  const [selectedCenterId, setSelectedCenterId] = useState('');
  const [centersLoading, setCentersLoading] = useState(true);
  const [centersError, setCentersError] = useState(null);

  // Sub-tab Navigation: 'resources' | 'staff'
  const [activeTab, setActiveTab] = useState('resources');

  // Operational Resources State (F3A)
  const [resources, setResources] = useState([]);
  const [resourcesLoading, setResourcesLoading] = useState(false);
  const [resourcesError, setResourcesError] = useState(null);

  // Operational Staff Roster State (F3B)
  const [staffRoster, setStaffRoster] = useState([]);
  const [staffLoading, setStaffLoading] = useState(false);
  const [staffError, setStaffError] = useState(null);

  // Resource Modal State
  const [resourceModalShow, setResourceModalShow] = useState(false);
  const [resourceModalMode, setResourceModalMode] = useState('create'); // 'create' | 'edit'
  const [editingResource, setEditingResource] = useState(null);
  const [resourceMutationLoading, setResourceMutationLoading] = useState(false);
  const [resourceMutationError, setResourceMutationError] = useState(null);

  // Staff Modal State
  const [staffModalShow, setStaffModalShow] = useState(false);
  const [staffModalMode, setStaffModalMode] = useState('register'); // 'register' | 'edit'
  const [editingStaff, setEditingStaff] = useState(null);
  const [staffMutationLoading, setStaffMutationLoading] = useState(false);
  const [staffMutationError, setStaffMutationError] = useState(null);

  // Fetch Centers
  const fetchCenters = useCallback(async () => {
    setCentersLoading(true);
    setCentersError(null);
    try {
      const data = await listCenters();
      const centerList = Array.isArray(data) ? data : [];
      setCenters(centerList);

      if (centerList.length > 0) {
        setSelectedCenterId((prev) => {
          if (prev && centerList.some((c) => String(c.id) === String(prev))) {
            return prev;
          }
          // Default to first active center, or first center
          const firstActive = centerList.find((c) => c.status === 'ACTIVE');
          return firstActive ? String(firstActive.id) : String(centerList[0].id);
        });
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

  // Fetch Resources for selected center
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

  // Fetch Staff Roster for selected center
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
      setResources([]);
      setResourcesError(null);
      setStaffRoster([]);
      setStaffError(null);

      // Close and reset modals if open
      setResourceModalShow(false);
      setStaffModalShow(false);

      fetchResources(selectedCenterId);
      fetchStaff(selectedCenterId);
    }
  }, [selectedCenterId, fetchResources, fetchStaff]);

  const selectedCenter = centers.find((c) => String(c.id) === String(selectedCenterId)) || null;

  // --- Resource Modal Handlers ---
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

  // --- Staff Modal Handlers ---
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

  const isDataLoading = resourcesLoading || staffLoading;

  return (
    <div className="container-fluid px-0 py-2">
      {/* Page Header */}
      <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center mb-4 gap-3">
        <div>
          <h1 className="h3 text-primary fw-bold mb-1">
            <i className="bi bi-hospital me-2"></i>
            Center Operations Management
          </h1>
          <p className="text-secondary mb-0">
            Operational physical resources and staff workforce rostering for vaccination centers.
          </p>
        </div>

        <div className="d-flex align-items-center gap-2">
          <button
            type="button"
            className="btn btn-outline-primary btn-sm"
            onClick={fetchCenters}
            disabled={centersLoading}
          >
            <i className="bi bi-arrow-clockwise me-1"></i>
            Refresh Centers
          </button>
        </div>
      </div>

      <ApiErrorAlert error={centersError} />

      {centersLoading ? (
        <div className="card shadow-sm border-0 py-5">
          <LoadingSpinner message="Loading vaccination center registry..." />
        </div>
      ) : centers.length === 0 ? (
        <div className="alert alert-warning border shadow-sm">
          <i className="bi bi-exclamation-triangle me-2"></i>
          <strong>No vaccination centers found.</strong> There are no registered vaccination centers available in the system.
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

                {selectedCenter && (
                  <div className="col-lg-7 col-md-6 border-start-md ps-md-4">
                    <div className="d-flex flex-wrap align-items-center gap-2 mb-2">
                      <span className="fw-bold text-dark">{selectedCenter.name}</span>
                      <span className="badge bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle font-monospace">
                        {selectedCenter.code}
                      </span>
                      {selectedCenter.status === 'ACTIVE' ? (
                        <span className="badge bg-success-subtle text-success-emphasis border border-success-subtle">
                          Active
                        </span>
                      ) : (
                        <span className="badge bg-danger-subtle text-danger-emphasis border border-danger-subtle">
                          Inactive (Decommissioned)
                        </span>
                      )}
                    </div>

                    <div className="row g-2 small text-muted">
                      <div className="col-sm-6">
                        <i className="bi bi-clock me-1"></i>
                        Timezone: <span className="fw-semibold text-dark">{selectedCenter.timezone || 'UTC'}</span>
                      </div>
                      <div className="col-sm-6">
                        <i className="bi bi-people me-1"></i>
                        Safe Occupancy: <span className="fw-semibold text-dark">{selectedCenter.safeConcurrentOccupancy || 'N/A'}</span>
                      </div>
                      <div className="col-sm-6">
                        <i className="bi bi-speedometer me-1"></i>
                        Service Ceiling: <span className="fw-semibold text-dark">{selectedCenter.sessionServiceCeiling || 'Unrestricted'}</span>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Sub-navigation Tabs */}
          {selectedCenter && (
            <div className="mb-4">
              <ul className="nav nav-pills border-bottom pb-2">
                <li className="nav-item me-2">
                  <button
                    type="button"
                    className={`nav-link fw-semibold px-4 py-2 d-flex align-items-center gap-2 ${
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
                    className={`nav-link fw-semibold px-4 py-2 d-flex align-items-center gap-2 ${
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

          {/* Active Tab Panel */}
          {selectedCenter && activeTab === 'resources' && (
            <OperationalResourcesPanel
              center={selectedCenter}
              resources={resources}
              loading={resourcesLoading}
              error={resourcesError}
              onRefresh={() => fetchResources(selectedCenterId)}
              onAddResource={handleOpenCreateResourceModal}
              onEditResource={handleOpenEditResourceModal}
            />
          )}

          {selectedCenter && activeTab === 'staff' && (
            <StaffRosterPanel
              center={selectedCenter}
              staffRoster={staffRoster}
              loading={staffLoading}
              error={staffError}
              onRefresh={() => fetchStaff(selectedCenterId)}
              onRegisterClick={handleOpenRegisterStaffModal}
              onEditClick={handleOpenEditStaffModal}
            />
          )}

          {/* Create / Edit Resource Modal */}
          <OperationalResourceModal
            show={resourceModalShow}
            mode={resourceModalMode}
            center={selectedCenter}
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
            center={selectedCenter}
            rosterEntry={editingStaff}
            loading={staffMutationLoading}
            error={staffMutationError}
            onClose={() => setStaffModalShow(false)}
            onSubmit={handleStaffModalSubmit}
          />
        </>
      )}
    </div>
  );
}
