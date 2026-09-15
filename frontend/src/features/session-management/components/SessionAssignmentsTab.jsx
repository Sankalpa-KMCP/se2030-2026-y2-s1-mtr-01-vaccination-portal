/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 */

import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  listResourceAssignments,
  deleteResourceAssignment,
  listStaffAssignments,
  deleteStaffAssignment,
} from '../api/sessionAssignmentsApi';
import { formatTimeInTimezone } from '../api/sessionApi';
import AssignResourceModal from './AssignResourceModal';
import AssignStaffModal from './AssignStaffModal';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';

export default function SessionAssignmentsTab({ session }) {
  const [resourceAssignments, setResourceAssignments] = useState([]);
  const [loadingResources, setLoadingResources] = useState(false);
  const [resourceError, setResourceError] = useState(null);

  const [staffAssignments, setStaffAssignments] = useState([]);
  const [loadingStaff, setLoadingStaff] = useState(false);
  const [staffError, setStaffError] = useState(null);

  const [isAssignResourceOpen, setIsAssignResourceOpen] = useState(false);
  const [isAssignStaffOpen, setIsAssignStaffOpen] = useState(false);

  const [deletingResourceId, setDeletingResourceId] = useState(null);
  const [deletingStaffId, setDeletingStaffId] = useState(null);
  const [actionError, setActionError] = useState(null);

  const activeSessionIdRef = useRef(session?.id);

  const tz = session?.scheduleTimezone || 'UTC';
  const isDraft = session?.lifecycleStatus === 'DRAFT';

  const fetchResources = useCallback((sessionId) => {
    if (!sessionId) return;
    setLoadingResources(true);
    setResourceError(null);

    listResourceAssignments(sessionId)
      .then((data) => {
        if (activeSessionIdRef.current === sessionId) {
          setResourceAssignments(data || []);
          setLoadingResources(false);
        }
      })
      .catch((err) => {
        if (activeSessionIdRef.current === sessionId) {
          setResourceError(err);
          setLoadingResources(false);
        }
      });
  }, []);

  const fetchStaff = useCallback((sessionId) => {
    if (!sessionId) return;
    setLoadingStaff(true);
    setStaffError(null);

    listStaffAssignments(sessionId)
      .then((data) => {
        if (activeSessionIdRef.current === sessionId) {
          setStaffAssignments(data || []);
          setLoadingStaff(false);
        }
      })
      .catch((err) => {
        if (activeSessionIdRef.current === sessionId) {
          setStaffError(err);
          setLoadingStaff(false);
        }
      });
  }, []);

  useEffect(() => {
    activeSessionIdRef.current = session?.id;
    if (session?.id) {
      fetchResources(session.id);
      fetchStaff(session.id);
    } else {
      setResourceAssignments([]);
      setStaffAssignments([]);
    }
  }, [session?.id, fetchResources, fetchStaff]);

  const handleRemoveResource = async (assignmentId) => {
    if (!window.confirm('Are you sure you want to remove this operational resource assignment?')) {
      return;
    }

    setDeletingResourceId(assignmentId);
    setActionError(null);

    try {
      await deleteResourceAssignment(session.id, assignmentId);
      setDeletingResourceId(null);
      fetchResources(session.id);
    } catch (err) {
      setActionError(err);
      setDeletingResourceId(null);
    }
  };

  const handleRemoveStaff = async (assignmentId) => {
    if (!window.confirm('Are you sure you want to remove this staff assignment?')) {
      return;
    }

    setDeletingStaffId(assignmentId);
    setActionError(null);

    try {
      await deleteStaffAssignment(session.id, assignmentId);
      setDeletingStaffId(null);
      fetchStaff(session.id);
    } catch (err) {
      setActionError(err);
      setDeletingStaffId(null);
    }
  };

  const countStations = resourceAssignments.filter(
    (r) => r.resourceType === 'VACCINATION_STATION'
  ).length;

  const countVaccinators = staffAssignments.filter(
    (s) => s.role === 'VACCINATOR'
  ).length;

  return (
    <div className="d-flex flex-column gap-4">
      {actionError && <ApiErrorAlert error={actionError} />}

      {/* Informational Guidance */}
      <div className="alert alert-info py-2 px-3 small d-flex align-items-center justify-content-between mb-0">
        <div>
          <strong>Operational Prerequisite Notice:</strong> Changes to vaccination-station or vaccinator assignments can make an approved Capacity Plan stale. Capacity validity is verified by the backend upon session scheduling.
        </div>
        <div className="text-nowrap ms-3">
          <span className="badge bg-primary me-1">{countStations} Station{countStations !== 1 ? 's' : ''}</span>
          <span className="badge bg-success">{countVaccinators} Vaccinator{countVaccinators !== 1 ? 's' : ''}</span>
        </div>
      </div>

      {!isDraft && (
        <div className="alert alert-secondary py-2 small mb-0">
          Assignments are read-only because session is in <strong>{session?.lifecycleStatus}</strong> status. Operational assignments can only be modified for sessions in DRAFT status.
        </div>
      )}

      {/* Resource Assignments Section */}
      <div className="card border">
        <div className="card-header bg-light d-flex align-items-center justify-content-between py-2 px-3">
          <div className="d-flex align-items-center gap-2">
            <h6 className="fw-bold text-dark mb-0">Assigned Operational Resources</h6>
            <span className="badge bg-secondary">{resourceAssignments.length}</span>
          </div>
          {isDraft && (
            <button
              type="button"
              className="btn btn-sm btn-outline-primary"
              onClick={() => {
                setActionError(null);
                setIsAssignResourceOpen(true);
              }}
            >
              + Assign Resource
            </button>
          )}
        </div>

        <div className="card-body p-0">
          {resourceError && (
            <div className="p-3">
              <ApiErrorAlert error={resourceError} />
            </div>
          )}

          {loadingResources && (
            <div className="p-3">
              <LoadingSpinner message="Loading resource assignments..." />
            </div>
          )}

          {!loadingResources && !resourceError && resourceAssignments.length === 0 && (
            <div className="text-center text-muted py-4 small">
              No operational resources currently assigned to this session.
            </div>
          )}

          {!loadingResources && !resourceError && resourceAssignments.length > 0 && (
            <div className="table-responsive">
              <table className="table table-hover table-striped align-middle mb-0" style={{ fontSize: '0.875rem' }}>
                <thead className="table-light">
                  <tr>
                    <th scope="col" style={{ width: '15%' }}>Code</th>
                    <th scope="col" style={{ width: '22%' }}>Type</th>
                    <th scope="col" style={{ width: '25%' }}>Resource Name</th>
                    <th scope="col" style={{ width: '15%' }}>Start Time</th>
                    <th scope="col" style={{ width: '15%' }}>End Time</th>
                    {isDraft && <th scope="col" style={{ width: '8%', textAlign: 'right' }}>Actions</th>}
                  </tr>
                </thead>
                <tbody>
                  {resourceAssignments.map((a) => (
                    <tr key={a.id}>
                      <td className="fw-semibold font-monospace">{a.resourceCode || `Res #${a.resourceId}`}</td>
                      <td>
                        {a.resourceType === 'VACCINATION_STATION' ? (
                          <span className="badge bg-primary">VACCINATION_STATION</span>
                        ) : (
                          <span className="badge bg-light text-dark border">{a.resourceType || 'RESOURCE'}</span>
                        )}
                      </td>
                      <td>{a.resourceName || '—'}</td>
                      <td>{formatTimeInTimezone(a.startAt, tz)}</td>
                      <td>{formatTimeInTimezone(a.endAt, tz)}</td>
                      {isDraft && (
                        <td className="text-end">
                          <button
                            type="button"
                            className="btn btn-sm btn-outline-danger py-0 px-2"
                            disabled={deletingResourceId === a.id}
                            onClick={() => handleRemoveResource(a.id)}
                            title="Remove resource assignment"
                          >
                            {deletingResourceId === a.id ? '...' : 'Remove'}
                          </button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Staff Assignments Section */}
      <div className="card border">
        <div className="card-header bg-light d-flex align-items-center justify-content-between py-2 px-3">
          <div className="d-flex align-items-center gap-2">
            <h6 className="fw-bold text-dark mb-0">Assigned Operational Staff</h6>
            <span className="badge bg-secondary">{staffAssignments.length}</span>
          </div>
          {isDraft && (
            <button
              type="button"
              className="btn btn-sm btn-outline-primary"
              onClick={() => {
                setActionError(null);
                setIsAssignStaffOpen(true);
              }}
            >
              + Assign Staff
            </button>
          )}
        </div>

        <div className="card-body p-0">
          {staffError && (
            <div className="p-3">
              <ApiErrorAlert error={staffError} />
            </div>
          )}

          {loadingStaff && (
            <div className="p-3">
              <LoadingSpinner message="Loading staff assignments..." />
            </div>
          )}

          {!loadingStaff && !staffError && staffAssignments.length === 0 && (
            <div className="text-center text-muted py-4 small">
              No staff members currently assigned to this session.
            </div>
          )}

          {!loadingStaff && !staffError && staffAssignments.length > 0 && (
            <div className="table-responsive">
              <table className="table table-hover table-striped align-middle mb-0" style={{ fontSize: '0.875rem' }}>
                <thead className="table-light">
                  <tr>
                    <th scope="col" style={{ width: '20%' }}>Staff Identity</th>
                    <th scope="col" style={{ width: '25%' }}>Assigned Role</th>
                    <th scope="col" style={{ width: '22%' }}>Start Time</th>
                    <th scope="col" style={{ width: '22%' }}>End Time</th>
                    {isDraft && <th scope="col" style={{ width: '11%', textAlign: 'right' }}>Actions</th>}
                  </tr>
                </thead>
                <tbody>
                  {staffAssignments.map((s) => (
                    <tr key={s.id}>
                      <td className="fw-semibold">User #{s.userId}</td>
                      <td>
                        {s.role === 'VACCINATOR' ? (
                          <span className="badge bg-success">VACCINATOR</span>
                        ) : (
                          <span className="badge bg-light text-dark border">{s.role}</span>
                        )}
                      </td>
                      <td>{formatTimeInTimezone(s.startAt, tz)}</td>
                      <td>{formatTimeInTimezone(s.endAt, tz)}</td>
                      {isDraft && (
                        <td className="text-end">
                          <button
                            type="button"
                            className="btn btn-sm btn-outline-danger py-0 px-2"
                            disabled={deletingStaffId === s.id}
                            onClick={() => handleRemoveStaff(s.id)}
                            title="Remove staff assignment"
                          >
                            {deletingStaffId === s.id ? '...' : 'Remove'}
                          </button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Assign Resource Modal */}
      {isAssignResourceOpen && (
        <AssignResourceModal
          isOpen={isAssignResourceOpen}
          onClose={() => setIsAssignResourceOpen(false)}
          session={session}
          onSuccess={() => fetchResources(session.id)}
        />
      )}

      {/* Assign Staff Modal */}
      {isAssignStaffOpen && (
        <AssignStaffModal
          isOpen={isAssignStaffOpen}
          onClose={() => setIsAssignStaffOpen(false)}
          session={session}
          onSuccess={() => fetchStaff(session.id)}
        />
      )}
    </div>
  );
}
