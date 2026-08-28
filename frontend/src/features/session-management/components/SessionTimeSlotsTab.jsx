import React, { useState, useEffect, useRef, useCallback } from 'react';
import { listTimeSlots, formatTimeInTimezone } from '../api/sessionApi';
import CreateTimeSlotModal from './CreateTimeSlotModal';
import UpdateTimeSlotCapacityModal from './UpdateTimeSlotCapacityModal';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import { useAuth } from '../../../auth/context/AuthContext';

export default function SessionTimeSlotsTab({ session }) {
  const { user } = useAuth();
  const isAuthorizedManager = Boolean(
    user?.roles?.some(
      (r) =>
        r === 'ROLE_ADMIN' ||
        r === 'ROLE_CENTER_MANAGER' ||
        r === 'ADMIN' ||
        r === 'CENTER_MANAGER'
    )
  );

  const [timeSlots, setTimeSlots] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingSlot, setEditingSlot] = useState(null);

  const activeSessionIdRef = useRef(session?.id);
  const tz = session?.scheduleTimezone || 'UTC';
  const isTerminalStatus =
    session?.lifecycleStatus === 'COMPLETED' || session?.lifecycleStatus === 'CANCELLED';

  const fetchSlots = useCallback((sessionId) => {
    if (!sessionId) return;
    setLoading(true);
    setError(null);

    listTimeSlots(sessionId)
      .then((data) => {
        if (activeSessionIdRef.current === sessionId) {
          setTimeSlots(data || []);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (activeSessionIdRef.current === sessionId) {
          setError(err);
          setLoading(false);
        }
      });
  }, []);

  useEffect(() => {
    activeSessionIdRef.current = session?.id;
    if (session?.id) {
      fetchSlots(session.id);
    } else {
      setTimeSlots([]);
    }
  }, [session?.id, fetchSlots]);

  const totalCapacity = timeSlots.reduce((sum, s) => sum + (s.capacity || 0), 0);
  const totalReserved = timeSlots.reduce((sum, s) => sum + (s.reservedCount || 0), 0);
  const totalAvailable = timeSlots.reduce((sum, s) => sum + (s.availableCapacity || 0), 0);

  return (
    <div className="p-4">
      {/* Header and Controls */}
      <div className="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h6 className="fw-bold text-dark mb-0">Session Time Slots & Bookable Capacity</h6>
          <small className="text-muted">
            Configure operational appointment intervals and manage bookable capacity limits.
          </small>
        </div>
        {isAuthorizedManager && !isTerminalStatus && (
          <button
            type="button"
            className="btn btn-sm btn-primary fw-semibold"
            onClick={() => setIsCreateOpen(true)}
          >
            + Add Time Slot
          </button>
        )}
      </div>

      {error && (
        <div className="mb-3">
          <ApiErrorAlert error={error} onDismiss={() => setError(null)} />
        </div>
      )}

      {/* Summary KPI Cards */}
      <div className="row g-3 mb-4">
        <div className="col-sm-6 col-md-3">
          <div className="card bg-light border-0 p-3">
            <span className="text-muted small d-block">Configured Slots</span>
            <strong className="h5 mb-0 text-dark">{timeSlots.length}</strong>
          </div>
        </div>
        <div className="col-sm-6 col-md-3">
          <div className="card bg-light border-0 p-3">
            <span className="text-muted small d-block">Total Slot Capacity</span>
            <strong className="h5 mb-0 text-primary">{totalCapacity}</strong>
          </div>
        </div>
        <div className="col-sm-6 col-md-3">
          <div className="card bg-light border-0 p-3">
            <span className="text-muted small d-block">Reserved / Booked</span>
            <strong className="h5 mb-0 text-warning">{totalReserved}</strong>
          </div>
        </div>
        <div className="col-sm-6 col-md-3">
          <div className="card bg-light border-0 p-3">
            <span className="text-muted small d-block">Available Intake</span>
            <strong className="h5 mb-0 text-success">{totalAvailable}</strong>
          </div>
        </div>
      </div>

      {/* Slots Table */}
      {loading ? (
        <div className="py-5">
          <LoadingSpinner message="Loading operational time slots..." />
        </div>
      ) : timeSlots.length === 0 ? (
        <div className="card border p-4 text-center text-muted bg-light">
          <h6 className="fw-semibold text-secondary mb-1">No Time Slots Configured</h6>
          <p className="small mb-3">
            This session currently has no operational time slots. Add time slots to enable citizen booking appointments.
          </p>
          {isAuthorizedManager && !isTerminalStatus && (
            <div>
              <button
                type="button"
                className="btn btn-sm btn-outline-primary fw-semibold"
                onClick={() => setIsCreateOpen(true)}
              >
                Create First Time Slot
              </button>
            </div>
          )}
        </div>
      ) : (
        <div className="table-responsive">
          <table className="table table-hover align-middle border mb-0">
            <thead className="table-light">
              <tr>
                <th scope="col" style={{ width: '35%' }}>Interval ({tz})</th>
                <th scope="col" className="text-center" style={{ width: '15%' }}>Capacity</th>
                <th scope="col" className="text-center" style={{ width: '15%' }}>Reserved</th>
                <th scope="col" className="text-center" style={{ width: '15%' }}>Available</th>
                {isAuthorizedManager && !isTerminalStatus && (
                  <th scope="col" className="text-end" style={{ width: '20%' }}>Actions</th>
                )}
              </tr>
            </thead>
            <tbody>
              {timeSlots.map((slot) => {
                const fillPercent =
                  slot.capacity > 0 ? Math.round((slot.reservedCount / slot.capacity) * 100) : 0;
                return (
                  <tr key={slot.id}>
                    <td>
                      <div className="fw-semibold text-dark">
                        {formatTimeInTimezone(slot.startTime, tz)} – {formatTimeInTimezone(slot.endTime, tz)}
                      </div>
                      <div className="text-muted font-monospace" style={{ fontSize: '0.75rem' }}>
                        UTC: {slot.startTime}
                      </div>
                    </td>
                    <td className="text-center">
                      <strong className="text-dark">{slot.capacity}</strong>
                    </td>
                    <td className="text-center">
                      <span className={`badge ${slot.reservedCount > 0 ? 'bg-warning text-dark' : 'bg-light text-muted border'}`}>
                        {slot.reservedCount}
                      </span>
                    </td>
                    <td className="text-center">
                      <span className={`badge ${slot.availableCapacity > 0 ? 'bg-success text-white' : 'bg-danger text-white'}`}>
                        {slot.availableCapacity}
                      </span>
                      <div className="progress mt-1" style={{ height: '4px' }}>
                        <div
                          className={`progress-bar ${fillPercent >= 100 ? 'bg-danger' : fillPercent >= 80 ? 'bg-warning' : 'bg-primary'}`}
                          role="progressbar"
                          style={{ width: `${Math.min(100, fillPercent)}%` }}
                          aria-valuenow={fillPercent}
                          aria-valuemin="0"
                          aria-valuemax="100"
                        />
                      </div>
                    </td>
                    {isAuthorizedManager && !isTerminalStatus && (
                      <td className="text-end">
                        <button
                          type="button"
                          className="btn btn-sm btn-outline-secondary"
                          onClick={() => setEditingSlot(slot)}
                        >
                          Edit Capacity
                        </button>
                      </td>
                    )}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Modals */}
      <CreateTimeSlotModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        session={session}
        onTimeSlotCreated={() => {
          if (session?.id) {
            fetchSlots(session.id);
          }
        }}
      />

      <UpdateTimeSlotCapacityModal
        isOpen={editingSlot !== null}
        onClose={() => setEditingSlot(null)}
        session={session}
        slot={editingSlot}
        onCapacityUpdated={() => {
          if (session?.id) {
            fetchSlots(session.id);
          }
        }}
      />
    </div>
  );
}
