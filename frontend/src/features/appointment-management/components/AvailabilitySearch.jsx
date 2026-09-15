import React, { useState } from 'react';
import SessionAvailabilityCard from './SessionAvailabilityCard';
import { searchAvailableAppointments } from '../api/availabilityApi';

/**
 * Availability search component containing filter form, API fetch, and result list (US-03).
 */
export default function AvailabilitySearch({ onBookSession, bookingSessionId, citizenId }) {
  const getTodayStr = () => new Date().toISOString().split('T')[0];
  const getNextWeekStr = () => {
    const d = new Date();
    d.setDate(d.getDate() + 7);
    return d.toISOString().split('T')[0];
  };

  const [campaignId, setCampaignId] = useState('8001');
  const [centerId, setCenterId] = useState('');
  const [fromDate, setFromDate] = useState(getTodayStr());
  const [toDate, setToDate] = useState(getNextWeekStr());

  const [sessions, setSessions] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSearch = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const results = await searchAvailableAppointments({
        citizenId,
        campaignId,
        centerId,
        fromDate,
        toDate,
      });
      setSessions(results || []);
    } catch (err) {
      setError(err.message || 'Failed to fetch available sessions');
      setSessions([]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="availability-search-container">
      <div className="card shadow-sm border-0 mb-4">
        <div className="card-header bg-primary text-white py-3">
          <h2 className="h5 mb-0 fw-bold">Search Available Vaccination Slots</h2>
        </div>
        <div className="card-body p-4">
          <form onSubmit={handleSearch}>
            <div className="row g-3">
              <div className="col-md-6 col-lg-3">
                <label className="form-label fw-semibold">Citizen ID</label>
                <div className="form-control bg-light text-muted" style={{ cursor: 'default' }}>
                  {citizenId}
                </div>
              </div>

              <div className="col-md-6 col-lg-3">
                <label htmlFor="campaignIdInput" className="form-label fw-semibold">
                  Campaign ID <span className="text-danger">*</span>
                </label>
                <input
                  type="number"
                  id="campaignIdInput"
                  className="form-control"
                  value={campaignId}
                  onChange={(e) => setCampaignId(e.target.value)}
                  placeholder="e.g. 8001"
                  required
                />
              </div>

              <div className="col-md-6 col-lg-2">
                <label htmlFor="centerIdInput" className="form-label fw-semibold">
                  Center ID
                </label>
                <input
                  type="number"
                  id="centerIdInput"
                  className="form-control"
                  value={centerId}
                  onChange={(e) => setCenterId(e.target.value)}
                  placeholder="All Centers"
                />
              </div>

              <div className="col-md-6 col-lg-2">
                <label htmlFor="fromDateInput" className="form-label fw-semibold">
                  From Date <span className="text-danger">*</span>
                </label>
                <input
                  type="date"
                  id="fromDateInput"
                  className="form-control"
                  value={fromDate}
                  onChange={(e) => setFromDate(e.target.value)}
                  required
                />
              </div>

              <div className="col-md-6 col-lg-2">
                <label htmlFor="toDateInput" className="form-label fw-semibold">
                  To Date <span className="text-danger">*</span>
                </label>
                <input
                  type="date"
                  id="toDateInput"
                  className="form-control"
                  value={toDate}
                  onChange={(e) => setToDate(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="mt-4 text-end">
              <button type="submit" className="btn btn-primary px-4 fw-bold shadow-sm" disabled={loading}>
                {loading ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                    Searching...
                  </>
                ) : (
                  'Search Sessions'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>

      {error && (
        <div className="alert alert-danger border-0 shadow-sm mb-4" role="alert">
          <h5 className="alert-heading fw-bold mb-1">Search Failed</h5>
          <p className="mb-0">{error}</p>
        </div>
      )}

      {loading && (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" style={{ width: '3rem', height: '3rem' }} role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="text-muted mt-2 fw-semibold">Finding bookable slots...</p>
        </div>
      )}

      {!loading && sessions !== null && (
        <div className="results-section">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h3 className="h5 fw-bold mb-0">Search Results</h3>
            <span className="badge bg-secondary">{sessions.length} Available</span>
          </div>

          {sessions.length === 0 ? (
            <div className="alert alert-warning border-0 shadow-sm p-4 text-center">
              <h4 className="h6 alert-heading fw-bold mb-1">No Available Sessions Found</h4>
              <p className="mb-0 text-muted small">
                No bookable vaccination slots match your search criteria. Try expanding your date range or selecting another campaign.
              </p>
            </div>
          ) : (
            <div className="session-list">
              {sessions.map((session) => (
                <SessionAvailabilityCard
                  key={session.slotId}
                  session={session}
                  onBook={onBookSession}
                  isBooking={bookingSessionId === session.slotId}
                />
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
