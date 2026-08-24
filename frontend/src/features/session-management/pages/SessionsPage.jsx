import React, { useState, useEffect, useCallback, useRef } from 'react';
import { listSessions } from '../api/sessionApi';
import { listCampaigns } from '../../campaign-management/api/campaignApi';
import { getActiveCenters } from '../../campaign-management/api/campaignConfigurationApi';
import SessionListPanel from '../components/SessionListPanel';
import CreateSessionModal from '../components/CreateSessionModal';
import SessionDetailsModal from '../components/SessionDetailsModal';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';

export default function SessionsPage() {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const [campaignsList, setCampaignsList] = useState([]);
  const [campaignsMap, setCampaignsMap] = useState({});
  const [centersList, setCentersList] = useState([]);
  const [centersMap, setCentersMap] = useState({});
  const [cataloguesLoading, setCataloguesLoading] = useState(true);
  const [cataloguesError, setCataloguesError] = useState(null);

  const [filters, setFilters] = useState({
    campaignId: '',
    centerId: '',
    status: '',
    dateQuery: '',
  });

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [selectedSessionId, setSelectedSessionId] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Race safety for session listing requests
  const latestSessionReqRef = useRef(null);
  const latestFiltersRef = useRef(filters);

  useEffect(() => {
    latestFiltersRef.current = filters;
  }, [filters]);

  // Load campaign & center catalogues once on mount
  useEffect(() => {
    let isMounted = true;
    setCataloguesLoading(true);
    setCataloguesError(null);

    Promise.all([listCampaigns(), getActiveCenters()])
      .then(([camps, cents]) => {
        if (!isMounted) return;
        const cList = camps || [];
        const centList = cents || [];

        const cMap = {};
        cList.forEach((c) => {
          cMap[c.id] = c;
        });

        const centMap = {};
        centList.forEach((cnt) => {
          centMap[cnt.id] = cnt;
        });

        setCampaignsList(cList);
        setCampaignsMap(cMap);
        setCentersList(centList);
        setCentersMap(centMap);
        setCataloguesLoading(false);
      })
      .catch((err) => {
        if (!isMounted) return;
        setCataloguesError(err);
        setCataloguesLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, []);

  // Fetch sessions with server-level filters using current filter snapshot
  const fetchSessions = useCallback(() => {
    const currentReqId = Symbol('sessions_req');
    latestSessionReqRef.current = currentReqId;

    setLoading(true);
    setError(null);

    const currentFilters = latestFiltersRef.current;
    const queryFilters = {
      campaignId: currentFilters.campaignId || null,
      centerId: currentFilters.centerId || null,
      status: currentFilters.status || null,
    };

    listSessions(queryFilters)
      .then((data) => {
        if (latestSessionReqRef.current !== currentReqId) return; // Stale response
        setSessions(data || []);
        setLoading(false);
      })
      .catch((err) => {
        if (latestSessionReqRef.current !== currentReqId) return; // Stale response
        setError(err);
        setLoading(false);
      });
  }, []);

  useEffect(() => {
    fetchSessions();
  }, [filters.campaignId, filters.centerId, filters.status, fetchSessions]);

  const handleFilterChange = (key, value) => {
    setFilters((prev) => ({
      ...prev,
      [key]: value,
    }));
  };

  const handleResetFilters = () => {
    setFilters({
      campaignId: '',
      centerId: '',
      status: '',
      dateQuery: '',
    });
  };

  const handleCreateSuccess = (createdSession) => {
    setSuccessMessage(`Session #${createdSession.id} created successfully in DRAFT status.`);
    fetchSessions();
    setTimeout(() => {
      setSuccessMessage(null);
    }, 5000);
  };

  // Authoritatively re-fetch sessions on lifecycle updates to preserve status-filter semantics
  const handleSessionUpdated = useCallback(() => {
    fetchSessions();
  }, [fetchSessions]);

  return (
    <div className="container-fluid px-0">
      {/* Page Header */}
      <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
        <div>
          <h1 className="h2 text-dark fw-bold mb-1">Vaccination Sessions</h1>
          <p className="text-secondary mb-0">
            Operational Session Inventory & Core Scheduling
          </p>
        </div>

        <button
          type="button"
          className="btn btn-primary d-inline-flex align-items-center gap-2"
          onClick={() => setIsCreateModalOpen(true)}
          disabled={cataloguesLoading}
        >
          <span className="fs-5 lh-1">+</span>
          Create Draft Session
        </button>
      </div>

      {/* Success Notification Alert */}
      {successMessage && (
        <div className="alert alert-success alert-dismissible fade show border-0 shadow-sm mb-4" role="status">
          <strong>Success:</strong> {successMessage}
          <button
            type="button"
            className="btn-close"
            aria-label="Close"
            onClick={() => setSuccessMessage(null)}
          />
        </div>
      )}

      {/* Catalogue Loading / Error state */}
      {cataloguesError && <ApiErrorAlert error={cataloguesError} />}

      {cataloguesLoading ? (
        <div className="card shadow-sm border-0 p-5 text-center">
          <LoadingSpinner message="Initializing campaign and center catalogues..." />
        </div>
      ) : (
        <SessionListPanel
          sessions={sessions}
          loading={loading}
          error={error}
          filters={filters}
          onFilterChange={handleFilterChange}
          onResetFilters={handleResetFilters}
          onViewDetails={(id) => setSelectedSessionId(id)}
          onOpenCreateModal={() => setIsCreateModalOpen(true)}
          campaignsList={campaignsList}
          centersList={centersList}
          campaignsMap={campaignsMap}
          centersMap={centersMap}
        />
      )}

      {/* Create Session Modal */}
      <CreateSessionModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={handleCreateSuccess}
        campaigns={campaignsList}
        centersMap={centersMap}
      />

      {/* Session Details Modal */}
      <SessionDetailsModal
        isOpen={selectedSessionId !== null}
        onClose={() => setSelectedSessionId(null)}
        sessionId={selectedSessionId}
        campaignsMap={campaignsMap}
        centersMap={centersMap}
        onSessionUpdated={handleSessionUpdated}
      />
    </div>
  );
}
