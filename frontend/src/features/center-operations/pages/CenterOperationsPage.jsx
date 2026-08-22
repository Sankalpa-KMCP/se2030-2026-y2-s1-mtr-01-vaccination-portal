import React, { useState, useEffect, useCallback } from 'react';
import {
  listCenters,
  listOperationalResources,
  createOperationalResource,
  updateOperationalResource,
} from '../api/centerOperationsApi';
import OperationalResourcesPanel from '../components/OperationalResourcesPanel';
import OperationalResourceModal from '../components/OperationalResourceModal';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

export default function CenterOperationsPage() {
  const [centers, setCenters] = useState([]);
  const [selectedCenterId, setSelectedCenterId] = useState('');
  const [centersLoading, setCentersLoading] = useState(true);
  const [centersError, setCentersError] = useState(null);

  const [resources, setResources] = useState([]);
  const [resourcesLoading, setResourcesLoading] = useState(false);
  const [resourcesError, setResourcesError] = useState(null);

  // Modal State
  const [modalShow, setModalShow] = useState(false);
  const [modalMode, setModalMode] = useState('create'); // 'create' | 'edit'
  const [editingResource, setEditingResource] = useState(null);
  const [mutationLoading, setMutationLoading] = useState(false);
  const [mutationError, setMutationError] = useState(null);

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

  // Handle center selection change
  useEffect(() => {
    if (selectedCenterId) {
      // Clear previous resources immediately for state isolation
      setResources([]);
      setResourcesError(null);
      fetchResources(selectedCenterId);
    }
  }, [selectedCenterId, fetchResources]);

  const selectedCenter = centers.find((c) => String(c.id) === String(selectedCenterId)) || null;

  // Open Create Modal
  const handleOpenCreateModal = () => {
    setModalMode('create');
    setEditingResource(null);
    setMutationError(null);
    setModalShow(true);
  };

  // Open Edit Modal
  const handleOpenEditModal = (resource) => {
    setModalMode('edit');
    setEditingResource(resource);
    setMutationError(null);
    setModalShow(true);
  };

  // Handle Modal Submit
  const handleModalSubmit = async (payload) => {
    if (!selectedCenterId) return;

    setMutationLoading(true);
    setMutationError(null);
    try {
      if (modalMode === 'create') {
        await createOperationalResource(selectedCenterId, payload);
      } else if (modalMode === 'edit' && editingResource) {
        await updateOperationalResource(selectedCenterId, editingResource.id, payload);
      }
      setModalShow(false);
      await fetchResources(selectedCenterId);
    } catch (err) {
      setMutationError(err);
    } finally {
      setMutationLoading(false);
    }
  };

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
            Operational configuration and physical resource capacity for vaccination centers.
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
                    disabled={resourcesLoading}
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

          {/* Operational Resources Panel */}
          {selectedCenter && (
            <OperationalResourcesPanel
              center={selectedCenter}
              resources={resources}
              loading={resourcesLoading}
              error={resourcesError}
              onRefresh={() => fetchResources(selectedCenterId)}
              onAddResource={handleOpenCreateModal}
              onEditResource={handleOpenEditModal}
            />
          )}

          {/* Create / Edit Resource Modal */}
          <OperationalResourceModal
            show={modalShow}
            mode={modalMode}
            center={selectedCenter}
            resource={editingResource}
            loading={mutationLoading}
            apiError={mutationError}
            onClose={() => setModalShow(false)}
            onSubmit={handleModalSubmit}
          />
        </>
      )}
    </div>
  );
}
