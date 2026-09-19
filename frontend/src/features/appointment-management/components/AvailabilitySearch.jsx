import React, { useState, useEffect } from 'react';
import SessionAvailabilityCard from './SessionAvailabilityCard';
import { searchAvailableAppointments } from '../api/availabilityApi';
import { listCitizenCenters, listCenters } from '../../center-operations/api/centerOperationsApi';
import { listCitizenCampaigns } from '../../campaign-management/api/campaignApi';

/**
 * Availability search component containing filter form, API fetch, and result list (US-03).
 * Center ID is automatically shown and extracted from the administrator's Vaccination Center Operations Centre code.
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-03: Search available vaccination appointment slots at nearby vaccination centers.</li>
 * </ul>
 *
 * <p><b>Business Rules Handled:</b>
 * <ul>
 *   <li>MF03-BR-01: Displays available sessions without premature eligibility gating during search.</li>
 *   <li>MF03-BR-02: Backend excludes slots where the citizen holds an existing active booking.</li>
 * </ul>
 */
export default function AvailabilitySearch({ onBookSession, bookingSessionId, citizenId }) {
  const getTodayStr = () => new Date().toISOString().split('T')[0];
  const getNextWeekStr = () => {
    const d = new Date();
    d.setDate(d.getDate() + 7);
    return d.toISOString().split('T')[0];
  };

  const [campaignId, setCampaignId] = useState('1');
  const [campaigns, setCampaigns] = useState([]);
  const [centers, setCenters] = useState([]);
  const [selectedCenterCode, setSelectedCenterCode] = useState('');
  const [centerId, setCenterId] = useState('');
  const [centersLoading, setCentersLoading] = useState(false);
  const [fromDate, setFromDate] = useState(getTodayStr());
  const [toDate, setToDate] = useState(getNextWeekStr());

  const [sessions, setSessions] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Load active Centers and Campaigns on mount for automatic extraction
  useEffect(() => {
    let isMounted = true;

    async function loadMasterData() {
      setCentersLoading(true);
      try {
        let centerList = [];
        try {
          centerList = await listCitizenCenters();
        } catch {
          try {
            centerList = await listCenters('ACTIVE');
          } catch {
            centerList = [];
          }
        }
        if (isMounted && Array.isArray(centerList)) {
          setCenters(centerList);
        }
      } catch (err) {
        console.error('Failed to load centers for citizen discovery:', err);
      } finally {
        if (isMounted) setCentersLoading(false);
      }

      try {
        let campaignList = [];
        try {
          campaignList = await listCitizenCampaigns();
        } catch {
          campaignList = [];
        }
        if (isMounted && Array.isArray(campaignList) && campaignList.length > 0) {
          setCampaigns(campaignList);
          setCampaignId(String(campaignList[0].id));
        }
      } catch (err) {
        console.error('Failed to load campaigns for citizen discovery:', err);
      }
    }

    loadMasterData();

    return () => {
      isMounted = false;
    };
  }, []);

  const handlePositiveIntegerKeyDown = (e) => {
    if (e.key === '-' || e.key === 'e' || e.key === '+' || e.key === '.') {
      e.preventDefault();
    }
  };

  const handleCampaignIdChange = (e) => {
    const val = e.target.value;
    if (val === '') {
      setCampaignId('');
      return;
    }
    const cleanVal = val.replace(/[^0-9]/g, '');
    if (cleanVal === '') {
      setCampaignId('');
      return;
    }
    const num = parseInt(cleanVal, 10);
    if (num > 0) {
      setCampaignId(String(num));
    }
  };

  /**
   * Handles selecting a center by its administrator Centre code.
   * Automatically extracts and updates the internal Center ID.
   */
  const handleCenterSelectChange = (e) => {
    const code = e.target.value;
    setSelectedCenterCode(code);
    if (!code) {
      setCenterId('');
    } else {
      const match = centers.find((c) => c.code === code);
      if (match) {
        setCenterId(String(match.id));
      } else {
        setCenterId('');
      }
    }
  };

  const handleCenterIdChange = (e) => {
    const val = e.target.value;
    if (val === '') {
      setCenterId('');
      setSelectedCenterCode('');
      return;
    }
    const cleanVal = val.replace(/[^0-9]/g, '');
    if (cleanVal === '') {
      setCenterId('');
      setSelectedCenterCode('');
      return;
    }
    const num = parseInt(cleanVal, 10);
    if (num > 0) {
      setCenterId(String(num));
      const match = centers.find((c) => String(c.id) === String(num));
      if (match) {
        setSelectedCenterCode(match.code);
      }
    }
  };

  const selectedCenter = centers.find(
    (c) => (selectedCenterCode && c.code === selectedCenterCode) || (centerId && String(c.id) === String(centerId))
  ) || null;

  const handleSearch = async (e) => {
    e.preventDefault();
    setError(null);

    if (!campaignId || Number(campaignId) <= 0) {
      setError('Campaign ID must be a positive number.');
      return;
    }

    if (centerId !== '' && Number(centerId) <= 0) {
      setError('Center ID must be a positive number.');
      return;
    }

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
                <div className="input-group">
                  <input
                    type="number"
                    id="campaignIdInput"
                    className="form-control"
                    value={campaignId}
                    onChange={handleCampaignIdChange}
                    onKeyDown={handlePositiveIntegerKeyDown}
                    placeholder="e.g. 1"
                    min="1"
                    step="1"
                    required
                  />
                  {campaigns.length > 0 && (
                    <select
                      className="form-select"
                      style={{ maxWidth: '140px' }}
                      value={campaignId}
                      onChange={(e) => setCampaignId(e.target.value)}
                      aria-label="Select Campaign"
                    >
                      {campaigns.map((camp) => (
                        <option key={camp.id} value={camp.id}>
                          [{camp.code}]
                        </option>
                      ))}
                    </select>
                  )}
                </div>
                <div className="form-text small text-muted">
                  {campaigns.find((c) => String(c.id) === String(campaignId))?.name || (campaignId ? `Campaign ID: ${campaignId}` : 'Required')}
                </div>
              </div>

              <div className="col-md-6 col-lg-3">
                <label htmlFor="centerCodeSelect" className="form-label fw-semibold">
                  Vaccination Center (Centre Code)
                </label>
                <select
                  id="centerCodeSelect"
                  className="form-select"
                  value={selectedCenter?.code || selectedCenterCode || ''}
                  onChange={handleCenterSelectChange}
                  disabled={centersLoading}
                >
                  <option value="">All Centers</option>
                  {centers.map((c) => (
                    <option key={c.id} value={c.code}>
                      [{c.code}] {c.name}
                    </option>
                  ))}
                </select>
                <div className="form-text small text-muted">
                  Extracted from Center Operations
                </div>
              </div>

              <div className="col-md-6 col-lg-3">
                <label htmlFor="centerIdInput" className="form-label fw-semibold d-flex justify-content-between align-items-center">
                  <span>Center ID</span>
                  <span className="badge bg-secondary-subtle text-secondary-emphasis font-monospace" style={{ fontSize: '0.72rem' }}>
                    Auto-Extracted
                  </span>
                </label>
                <input
                  type="number"
                  id="centerIdInput"
                  className="form-control"
                  value={centerId}
                  onChange={handleCenterIdChange}
                  onKeyDown={handlePositiveIntegerKeyDown}
                  placeholder="All Centers"
                  min="1"
                  step="1"
                />
                <div className="form-text small">
                  {selectedCenter ? (
                    <span className="text-success fw-semibold">
                      <i className="bi bi-check-circle me-1"></i>
                      Extracted from [{selectedCenter.code}]
                    </span>
                  ) : (
                    <span className="text-muted">Applies to all centers</span>
                  )}
                </div>
              </div>

              <div className="col-md-6 col-lg-3">
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

              <div className="col-md-6 col-lg-3">
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

              <div className="col-md-12 col-lg-6 d-flex align-items-end justify-content-end gap-2 mt-3 mt-lg-0">
                <button
                  type="button"
                  className="btn btn-outline-secondary px-3 fw-semibold"
                  onClick={() => {
                    setSelectedCenterCode('');
                    setCenterId('');
                    setFromDate(getTodayStr());
                    setToDate(getNextWeekStr());
                    setSessions(null);
                    setError(null);
                  }}
                  disabled={loading}
                >
                  Reset
                </button>
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
