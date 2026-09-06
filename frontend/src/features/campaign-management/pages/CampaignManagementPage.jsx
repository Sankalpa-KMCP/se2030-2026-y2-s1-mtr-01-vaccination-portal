import React, { useState, useEffect, useCallback } from 'react';
import {
  listCampaigns,
  getCampaignById,
  createCampaign,
  updateCampaign,
  getCampaignLifecycleEvents,
  scheduleCampaign,
  publishCampaign,
  activateCampaign,
  suspendCampaign,
  resumeCampaign,
  completeCampaign,
  cancelCampaign,
  archiveCampaign,
} from '../api/campaignApi';
import CampaignList from '../components/CampaignList';
import CampaignForm from '../components/CampaignForm';
import CampaignDetailsModal from '../components/CampaignDetailsModal';
import CampaignActionModal from '../components/CampaignActionModal';
import MasterDataManagementModal from '../components/MasterDataManagementModal';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import PageHeader from '../../../components/common/PageHeader';
import SuccessAlert from '../../../components/common/SuccessAlert';
import { useAuth } from '../../../auth/context/AuthContext';
import { canManageCampaigns } from '../../../auth/utils/roleUtils';

export default function CampaignManagementPage() {
  const { user } = useAuth();
  const isManager = canManageCampaigns(user);

  const [campaigns, setCampaigns] = useState([]);
  const [selectedStatus, setSelectedStatus] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Master Data Modal state
  const [showMasterDataModal, setShowMasterDataModal] = useState(false);

  // Create Form state
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState(null);

  // Edit Form state
  const [editingCampaign, setEditingCampaign] = useState(null);
  const [editLoading, setEditLoading] = useState(false);
  const [editError, setEditError] = useState(null);

  // Details Modal state
  const [selectedCampaign, setSelectedCampaign] = useState(null);
  const [lifecycleEvents, setLifecycleEvents] = useState([]);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [detailsError, setDetailsError] = useState(null);

  // Action Modal state
  const [pendingAction, setPendingAction] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState(null);

  const fetchCampaigns = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listCampaigns(selectedStatus || null);
      setCampaigns(data || []);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [selectedStatus]);

  useEffect(() => {
    fetchCampaigns();
  }, [fetchCampaigns]);

  // Handle Campaign Creation
  const handleCreateSubmit = async (payload) => {
    setCreateLoading(true);
    setCreateError(null);
    try {
      const created = await createCampaign(payload);
      setShowCreateForm(false);
      setSuccessMessage(`Campaign "${created.name}" created successfully with code ${created.code}.`);
      await fetchCampaigns();
    } catch (err) {
      setCreateError(err);
    } finally {
      setCreateLoading(false);
    }
  };

  // Handle Campaign Edit Initiation
  const handleInitiateEdit = async (campaign) => {
    setShowCreateForm(false);
    setCreateError(null);
    setEditError(null);
    try {
      if (!campaign.bookingOpenAt || !campaign.bookingCloseAt) {
        setLoading(true);
        const detailed = await getCampaignById(campaign.id);
        setEditingCampaign(detailed);
      } else {
        setEditingCampaign(campaign);
      }
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  // Handle Campaign Edit Submission
  const handleEditSubmit = async (payload) => {
    if (!editingCampaign) return;
    setEditLoading(true);
    setEditError(null);
    try {
      const updated = await updateCampaign(editingCampaign.id, payload);
      setEditingCampaign(null);
      setSuccessMessage(`Campaign "${updated.name}" updated successfully.`);
      await fetchCampaigns();
    } catch (err) {
      setEditError(err);
    } finally {
      setEditLoading(false);
    }
  };

  // Handle Opening Campaign Details
  const handleSelectCampaign = async (id) => {
    setDetailsLoading(true);
    setDetailsError(null);
    setSelectedCampaign({ id }); // Placeholder until loaded
    try {
      const [campaignData, eventsData] = await Promise.all([
        getCampaignById(id),
        getCampaignLifecycleEvents(id).catch(() => []),
      ]);
      setSelectedCampaign(campaignData);
      setLifecycleEvents(eventsData || []);
    } catch (err) {
      setDetailsError(err);
    } finally {
      setDetailsLoading(false);
    }
  };

  // Handle Lifecycle Action Trigger from List or Modal
  const handleInitiateAction = (actionType, campaign) => {
    setActionError(null);
    setPendingAction({ type: actionType, campaign });
  };

  // Confirm Lifecycle Action Execution
  const handleConfirmAction = async (actionType, campaignId, payload) => {
    setActionLoading(true);
    setActionError(null);
    try {
      let updated;
      switch (actionType) {
        case 'SCHEDULE':
          updated = await scheduleCampaign(campaignId);
          break;
        case 'PUBLISH':
          updated = await publishCampaign(campaignId);
          break;
        case 'ACTIVATE':
          updated = await activateCampaign(campaignId);
          break;
        case 'SUSPEND':
          updated = await suspendCampaign(campaignId, payload);
          break;
        case 'RESUME':
          updated = await resumeCampaign(campaignId, payload);
          break;
        case 'COMPLETE':
          updated = await completeCampaign(campaignId, payload);
          break;
        case 'CANCEL':
          updated = await cancelCampaign(campaignId, payload);
          break;
        case 'ARCHIVE':
          updated = await archiveCampaign(campaignId, payload);
          break;
        default:
          throw new Error(`Unsupported lifecycle action: ${actionType}`);
      }

      setPendingAction(null);
      setSuccessMessage(`Campaign "${updated.name}" successfully transitioned to ${updated.status}.`);

      // If details modal was open, refresh it
      if (selectedCampaign && selectedCampaign.id === campaignId) {
        setSelectedCampaign(updated);
        const events = await getCampaignLifecycleEvents(campaignId).catch(() => []);
        setLifecycleEvents(events);
      }

      await fetchCampaigns();
    } catch (err) {
      setActionError(err);
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div>
      <PageHeader
        title="Vaccination Campaign Management"
        subtitle="Define, schedule, activate, and manage vaccination campaigns and lifecycle states."
        actions={isManager && (
          <button
            type="button"
            className="btn btn-outline-primary btn-sm"
            onClick={() => setShowMasterDataModal(true)}
          >
            Manage Master Data
          </button>
        )}
      />

      {/* Success Notification Banner */}
      {successMessage && (
        <SuccessAlert
          message={<span><strong className="me-1">Success:</strong> {successMessage}</span>}
          onDismiss={() => setSuccessMessage(null)}
        />
      )}

      {/* Global Page Error */}
      <ApiErrorAlert error={error} onDismiss={() => setError(null)} />

      {/* Create Campaign Form Card */}
      {isManager && showCreateForm && (
        <CampaignForm
          onSubmit={handleCreateSubmit}
          onCancel={() => {
            setShowCreateForm(false);
            setCreateError(null);
          }}
          loading={createLoading}
          error={createError}
        />
      )}

      {/* Edit Campaign Form Card */}
      {isManager && editingCampaign && (
        <CampaignForm
          initialCampaign={editingCampaign}
          onSubmit={handleEditSubmit}
          onCancel={() => {
            setEditingCampaign(null);
            setEditError(null);
          }}
          loading={editLoading}
          error={editError}
        />
      )}

      {/* Campaign List View */}
      {loading ? (
        <LoadingSpinner message="Fetching vaccination campaigns..." />
      ) : (
        <CampaignList
          campaigns={campaigns}
          selectedStatus={selectedStatus}
          onStatusFilterChange={setSelectedStatus}
          onSelectCampaign={handleSelectCampaign}
          onInitiateAction={handleInitiateAction}
          onEditCampaign={handleInitiateEdit}
          onCreateNew={() => {
            setEditingCampaign(null);
            setEditError(null);
            setShowCreateForm(true);
            setCreateError(null);
          }}
        />
      )}

      {/* Master Data Management Modal */}
      {showMasterDataModal && (
        <MasterDataManagementModal
          onClose={() => setShowMasterDataModal(false)}
        />
      )}

      {/* Campaign Details Modal */}
      {selectedCampaign && (
        <CampaignDetailsModal
          campaign={selectedCampaign}
          lifecycleEvents={lifecycleEvents}
          loading={detailsLoading}
          error={detailsError}
          onClose={() => {
            setSelectedCampaign(null);
            setDetailsError(null);
          }}
          onInitiateAction={(actionType, camp) => {
            handleInitiateAction(actionType, camp);
          }}
        />
      )}

      {/* Lifecycle Action Confirmation Modal */}
      {pendingAction && (
        <CampaignActionModal
          action={pendingAction}
          loading={actionLoading}
          error={actionError}
          onConfirm={handleConfirmAction}
          onCancel={() => {
            setPendingAction(null);
            setActionError(null);
          }}
        />
      )}
    </div>
  );
}
