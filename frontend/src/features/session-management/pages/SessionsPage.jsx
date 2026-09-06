import React, { useState, useEffect, useCallback, useRef } from 'react';
import { listSessions } from '../api/sessionApi';
import { listCampaigns } from '../../campaign-management/api/campaignApi';
import { getActiveCenters } from '../../campaign-management/api/campaignConfigurationApi';
import SessionListPanel from '../components/SessionListPanel';
import CreateSessionModal from '../components/CreateSessionModal';
import SessionDetailsModal from '../components/SessionDetailsModal';
import SessionRecurrencePanel from '../components/SessionRecurrencePanel';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import { useAuth } from '../../../auth/context/AuthContext';
import { canPlanSessions } from '../../../auth/utils/roleUtils';

export default function SessionsPage() {
  const { user } = useAuth();
  const isPlanner = canPlanSessions(user);

  const [pageTab, setPageTab] = useState('sessions'); // 'sessions' | 'recurrence'
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

  // Shared recurrence operation gate token that survives page tab unmount/remount
  const recurrenceOperationOwnerRef = useRef(null);
  // Parent-level recurrence invalidation generation to keep inventory fresh across tab switches
  const [recurrenceRefreshGen, setRecurrenceRefreshGen] = useState(0);

  // Race safety for session listing requests
  const latestSessionReqRef = useRef(null);
  const latestFiltersRef = useRef(filters);
  latestFiltersRef.current = filters;

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

  // Recurrence mutations authoritatively reconcile both recurrence inventory and operational sessions
  const handleRecurrenceDataChanged = useCallback(() => {
    setRecurrenceRefreshGen((prev) => prev + 1);
    fetchSessions();
  }, [fetchSessions]);

  return (
    <div className="container-fluid px-0">
      {/* Page Header */}
      <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
        <div>
          <h1 className="h2 text-dark fw-bold mb-1">Vaccination Sessions</h1>
          <p className="text-secondary mb-0">
            Operational Session Inventory, Core Scheduling & Recurrence Management
          </p>
        </div>

        {isPlanner && pageTab === 'sessions' && (
          <button
            type="button"
            className="btn btn-primary d-inline-flex align-items-center gap-2"
            onClick={() => setIsCreateModalOpen(true)}
            disabled={cataloguesLoading}
            id="btn-open-create-session"
          >
            <i className="bi bi-plus-lg" aria-hidden="true"></i>
            Create Draft Session
          </button>
        )}
      </div>

      {/* Main Tab Navigation */}
      <ul className="nav nav-tabs mb-4" role="tablist" aria-label="Vaccination sessions views">
        <li className="nav-item" role="presentation">
          <button
            type="button"
            className={`nav-link fw-semibold ${pageTab === 'sessions' ? 'active text-primary' : 'text-secondary'}`}
            onClick={() => setPageTab('sessions')}
            role="tab"
            aria-selected={pageTab === 'sessions'}
            aria-controls="panel-session-inventory"
            id="tab-session-inventory"
          >
            Individual Sessions Inventory
          </button>
        </li>
        <li className="nav-item" role="presentation">
          <button
            type="button"
            className={`nav-link fw-semibold ${pageTab === 'recurrence' ? 'active text-primary' : 'text-secondary'}`}
            onClick={() => setPageTab('recurrence')}
            role="tab"
            aria-selected={pageTab === 'recurrence'}
            aria-controls="panel-recurrence-management"
            id="tab-recurrence-management"
          >
            Recurrence Rules & Series Management
          </button>
        </li>
      </ul>

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
      ) : pageTab === 'sessions' ? (
        <div
          id="panel-session-inventory"
          role="tabpanel"
          aria-labelledby="tab-session-inventory"
          tabIndex="0"
        >
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
        </div>
      ) : (
        <div
          id="panel-recurrence-management"
          role="tabpanel"
          aria-labelledby="tab-recurrence-management"
          tabIndex="0"
        >
          <SessionRecurrencePanel
            campaignsList={campaignsList}
            centersList={centersList}
            campaignsMap={campaignsMap}
            centersMap={centersMap}
            recurrenceOperationOwnerRef={recurrenceOperationOwnerRef}
            recurrenceRefreshGen={recurrenceRefreshGen}
            onRecurrenceDataChanged={handleRecurrenceDataChanged}
            onSessionDataChanged={fetchSessions}
          />
        </div>
      )}

      {/* Create Session Modal */}
      {isPlanner && (
        <CreateSessionModal
          isOpen={isCreateModalOpen}
          onClose={() => setIsCreateModalOpen(false)}
          onSuccess={handleCreateSuccess}
          campaigns={campaignsList}
          centersMap={centersMap}
        />
      )}

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
