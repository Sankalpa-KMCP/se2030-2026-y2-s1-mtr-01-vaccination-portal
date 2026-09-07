import React, { useState, useEffect, useCallback } from 'react';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import StatusBadge from '../../../components/common/StatusBadge';
import { campaignStatusVariants } from '../../../components/common/statusVariants';
import { useAuth } from '../../../auth/context/AuthContext';
import {
  canManageCampaigns,
  canManageQuestionnaireAssignments,
  canViewQuestionnaireAssignments,
} from '../../../auth/utils/roleUtils';
import DoseProgrammeTab from './DoseProgrammeTab';
import DoseProgrammeFormModal from './DoseProgrammeFormModal';
import QuestionnaireTab from './QuestionnaireTab';
import QuestionnaireAssignmentFormModal from './QuestionnaireAssignmentFormModal';
import CenterAssignmentsTab from './CenterAssignmentsTab';
import CenterAssignmentModal from './CenterAssignmentModal';
import LocationTargetsTab from './LocationTargetsTab';
import LocationTargetModal from './LocationTargetModal';
import {
  getDoseProgrammes,
  createDoseProgramme,
  createDoseProgrammeVersion,
  getQuestionnaireAssignments,
  assignQuestionnaire,
  getScreeningQuestionnaires,
  getQuestionnaireVersions,
  getCenterAssignments,
  createCenterAssignment,
  updateCenterAssignment,
  endCenterAssignment,
  cancelCenterAssignment,
  getActiveCenters,
  getLocationTargets,
  createLocationTarget,
} from '../api/campaignConfigurationApi';

function formatDate(dateStr) {
  if (!dateStr) return '—';
  try {
    const d = new Date(dateStr);
    return isNaN(d.getTime()) ? dateStr : d.toLocaleDateString();
  } catch {
    return dateStr;
  }
}

function formatDateTime(dateTimeStr) {
  if (!dateTimeStr) return '—';
  try {
    const d = new Date(dateTimeStr);
    return isNaN(d.getTime()) ? dateTimeStr : d.toLocaleString();
  } catch {
    return dateTimeStr;
  }
}

export default function CampaignDetailsModal({
  campaign,
  lifecycleEvents,
  loading,
  error,
  onClose,
  onInitiateAction,
}) {
  const { user } = useAuth();
  const isManager = canManageCampaigns(user);
  const canViewQuestionnaireAssignmentHistory = canViewQuestionnaireAssignments(user);
  const canManageQuestionnaireAssignment = canManageQuestionnaireAssignments(user);

  const [activeTab, setActiveTab] = useState('overview');

  // Dose Programme State
  const [doseProgrammes, setDoseProgrammes] = useState([]);
  const [doseLoading, setDoseLoading] = useState(false);
  const [doseError, setDoseError] = useState(null);

  // Dose Form Modal State
  const [doseModalMode, setDoseModalMode] = useState(null); // 'CREATE' | 'NEW_VERSION' | null
  const [doseModalLoading, setDoseModalLoading] = useState(false);
  const [doseModalError, setDoseModalError] = useState(null);

  // Questionnaire State
  const [questionnaireAssignments, setQuestionnaireAssignments] = useState([]);
  const [questionnaireLoading, setQuestionnaireLoading] = useState(false);
  const [questionnaireError, setQuestionnaireError] = useState(null);
  const [versionsMap, setVersionsMap] = useState({});

  // Questionnaire Form Modal State
  const [showQuestionnaireModal, setShowQuestionnaireModal] = useState(false);
  const [questionnaireModalLoading, setQuestionnaireModalLoading] = useState(false);
  const [questionnaireModalError, setQuestionnaireModalError] = useState(null);

  // Center Assignment State
  const [centerAssignments, setCenterAssignments] = useState([]);
  const [centerLoading, setCenterLoading] = useState(false);
  const [centerError, setCenterError] = useState(null);
  const [activeCenters, setActiveCenters] = useState([]);
  const [loadingActiveCenters, setLoadingActiveCenters] = useState(false);

  // Center Modal State
  const [centerModalMode, setCenterModalMode] = useState(null); // 'CREATE' | 'UPDATE_FUTURE' | 'END_ACTIVE' | 'CANCEL_CONFIRM' | null
  const [selectedCenterAssignment, setSelectedCenterAssignment] = useState(null);
  const [centerModalLoading, setCenterModalLoading] = useState(false);
  const [centerModalError, setCenterModalError] = useState(null);

  // Location Target State
  const [locationTargets, setLocationTargets] = useState([]);
  const [locationLoading, setLocationLoading] = useState(false);
  const [locationError, setLocationError] = useState(null);

  // Location Target Modal State
  const [showLocationModal, setShowLocationModal] = useState(false);
  const [locationModalLoading, setLocationModalLoading] = useState(false);
  const [locationModalError, setLocationModalError] = useState(null);

  const fetchDoseProgrammes = useCallback(async (campaignId) => {
    if (!campaignId) return;
    setDoseLoading(true);
    setDoseError(null);
    try {
      const data = await getDoseProgrammes(campaignId);
      setDoseProgrammes(data || []);
    } catch (err) {
      setDoseError(err);
    } finally {
      setDoseLoading(false);
    }
  }, []);

  const fetchQuestionnaireAssignments = useCallback(async (campaignId) => {
    if (!campaignId) return;
    setQuestionnaireLoading(true);
    setQuestionnaireError(null);
    try {
      const data = await getQuestionnaireAssignments(campaignId);
      setQuestionnaireAssignments(data || []);
    } catch (err) {
      setQuestionnaireError(err);
    } finally {
      setQuestionnaireLoading(false);
    }
  }, []);

  const fetchQuestionnaireCatalogue = useCallback(async () => {
    try {
      const qList = await getScreeningQuestionnaires();
      if (qList && qList.length > 0) {
        const map = {};
        await Promise.all(
          qList.map(async (q) => {
            try {
              const vList = await getQuestionnaireVersions(q.id);
              if (vList) {
                vList.forEach((v) => {
                  map[v.id] = {
                    questionnaireId: q.id,
                    questionnaireName: q.name,
                    questionnaireCode: q.code,
                    versionNumber: v.versionNumber,
                    publishedAt: v.publishedAt,
                    status: v.status,
                  };
                });
              }
            } catch {
              // Ignore individual questionnaire version lookup error
            }
          })
        );
        setVersionsMap(map);
      }
    } catch {
      // Ignore catalogue lookup error
    }
  }, []);

  const fetchCenterAssignments = useCallback(async (campaignId) => {
    if (!campaignId) return;
    setCenterLoading(true);
    setCenterError(null);
    try {
      const data = await getCenterAssignments(campaignId);
      setCenterAssignments(data || []);
    } catch (err) {
      setCenterError(err);
    } finally {
      setCenterLoading(false);
    }
  }, []);

  const fetchActiveCentersList = useCallback(async () => {
    setLoadingActiveCenters(true);
    try {
      const data = await getActiveCenters();
      setActiveCenters(data || []);
    } catch {
      // Handled gracefully in modal
    } finally {
      setLoadingActiveCenters(false);
    }
  }, []);

  const fetchLocationTargets = useCallback(async (campaignId) => {
    if (!campaignId) return;
    setLocationLoading(true);
    setLocationError(null);
    try {
      const data = await getLocationTargets(campaignId);
      setLocationTargets(data || []);
    } catch (err) {
      setLocationError(err);
    } finally {
      setLocationLoading(false);
    }
  }, []);

  useEffect(() => {
    if (campaign?.id) {
      fetchDoseProgrammes(campaign.id);
      if (canViewQuestionnaireAssignmentHistory) {
        fetchQuestionnaireAssignments(campaign.id);
      }
      if (canManageQuestionnaireAssignment) {
        fetchQuestionnaireCatalogue();
      }
      fetchCenterAssignments(campaign.id);
      fetchActiveCentersList();
      fetchLocationTargets(campaign.id);
    }
  }, [
    campaign?.id,
    canManageQuestionnaireAssignment,
    canViewQuestionnaireAssignmentHistory,
    fetchDoseProgrammes,
    fetchQuestionnaireAssignments,
    fetchQuestionnaireCatalogue,
    fetchCenterAssignments,
    fetchActiveCentersList,
    fetchLocationTargets,
  ]);

  const handleDoseFormSubmit = async (payload) => {
    if (!campaign?.id) return;
    setDoseModalLoading(true);
    setDoseModalError(null);
    try {
      if (doseModalMode === 'NEW_VERSION') {
        await createDoseProgrammeVersion(campaign.id, payload);
      } else {
        await createDoseProgramme(campaign.id, payload);
      }
      setDoseModalMode(null);
      await fetchDoseProgrammes(campaign.id);
    } catch (err) {
      setDoseModalError(err);
    } finally {
      setDoseModalLoading(false);
    }
  };

  const handleQuestionnaireSubmit = async (payload) => {
    if (!campaign?.id || !canManageQuestionnaireAssignment) return;
    setQuestionnaireModalLoading(true);
    setQuestionnaireModalError(null);
    try {
      await assignQuestionnaire(campaign.id, payload);
      setShowQuestionnaireModal(false);
      await fetchQuestionnaireAssignments(campaign.id);
      await fetchQuestionnaireCatalogue();
    } catch (err) {
      setQuestionnaireModalError(err);
    } finally {
      setQuestionnaireModalLoading(false);
    }
  };

  const handleCenterAssignmentSubmit = async (payload) => {
    if (!campaign?.id) return;
    setCenterModalLoading(true);
    setCenterModalError(null);
    try {
      if (centerModalMode === 'CREATE') {
        await createCenterAssignment(campaign.id, payload);
      } else if (centerModalMode === 'UPDATE_FUTURE' && selectedCenterAssignment) {
        await updateCenterAssignment(campaign.id, selectedCenterAssignment.id, payload);
      } else if (centerModalMode === 'END_ACTIVE' && selectedCenterAssignment) {
        await endCenterAssignment(campaign.id, selectedCenterAssignment.id, payload);
      } else if (centerModalMode === 'CANCEL_CONFIRM' && selectedCenterAssignment) {
        await cancelCenterAssignment(campaign.id, selectedCenterAssignment.id);
      }
      setCenterModalMode(null);
      setSelectedCenterAssignment(null);
      await fetchCenterAssignments(campaign.id);
    } catch (err) {
      setCenterModalError(err);
    } finally {
      setCenterModalLoading(false);
    }
  };

  const handleLocationTargetSubmit = async (payload) => {
    if (!campaign?.id) return;
    setLocationModalLoading(true);
    setLocationModalError(null);
    try {
      await createLocationTarget(campaign.id, payload);
      setShowLocationModal(false);
      await fetchLocationTargets(campaign.id);
    } catch (err) {
      setLocationModalError(err);
    } finally {
      setLocationModalLoading(false);
    }
  };

  if (!campaign && !loading && !error) return null;

  const latestDoseProgramme = doseProgrammes.length > 0 ? doseProgrammes[0] : null;
  const todayIsoStr = new Date().toISOString().split('T')[0];
  const activeQuestionnaireAssignment = questionnaireAssignments.find(
    (a) => a.activeFrom <= todayIsoStr && (a.activeTo == null || a.activeTo > todayIsoStr)
  );
  const currentCenterAssignments = centerAssignments.filter(
    (a) => a.status === 'ACTIVE' && a.activeFrom <= todayIsoStr && (a.activeTo == null || a.activeTo > todayIsoStr)
  );

  return (
    <div
      className="modal fade show d-block"
      tabIndex="-1"
      role="dialog"
      style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }}
    >
      <div className="modal-dialog modal-lg modal-dialog-scrollable" role="document">
        <div className="modal-content shadow">
          <div className="modal-header bg-light">
            <div className="d-flex align-items-center gap-2">
              <h5 className="modal-title fw-bold mb-0">
                {campaign ? `${campaign.name} (${campaign.code})` : 'Campaign Details'}
              </h5>
              {campaign?.status && (
                <StatusBadge status={campaign.status} variants={campaignStatusVariants} className="fs-7" />
              )}
            </div>
            <button
              type="button"
              className="btn-close"
              aria-label="Close"
              onClick={onClose}
            />
          </div>

          {/* Sub-Navigation Tabs */}
          {campaign && (
            <div className="border-bottom px-4 pt-2 bg-white">
              <ul className="nav nav-tabs border-0" role="tablist">
                <li className="nav-item" role="presentation">
                  <button
                    type="button"
                    className={`nav-link border-0 fw-semibold small ${activeTab === 'overview' ? 'active text-primary border-bottom border-primary border-2' : 'text-secondary'}`}
                    onClick={() => setActiveTab('overview')}
                  >
                    Overview
                  </button>
                </li>
                <li className="nav-item" role="presentation">
                  <button
                    type="button"
                    className={`nav-link border-0 fw-semibold small ${activeTab === 'dose' ? 'active text-primary border-bottom border-primary border-2' : 'text-secondary'}`}
                    onClick={() => setActiveTab('dose')}
                  >
                    Dose Programme{' '}
                    {latestDoseProgramme && (
                      <span className="badge bg-primary-subtle text-primary border border-primary-subtle ms-1 font-monospace" style={{ fontSize: '0.7rem' }}>
                        v{latestDoseProgramme.versionNumber}
                      </span>
                    )}
                  </button>
                </li>
                {canViewQuestionnaireAssignmentHistory && (
                  <li className="nav-item" role="presentation">
                    <button
                      type="button"
                      className={`nav-link border-0 fw-semibold small ${activeTab === 'questionnaire' ? 'active text-primary border-bottom border-primary border-2' : 'text-secondary'}`}
                      onClick={() => setActiveTab('questionnaire')}
                    >
                      Questionnaire{' '}
                      {activeQuestionnaireAssignment && (
                        <span className="badge bg-success-subtle text-success border border-success-subtle ms-1" style={{ fontSize: '0.7rem' }}>
                          Assigned
                        </span>
                      )}
                    </button>
                  </li>
                )}
                <li className="nav-item" role="presentation">
                  <button
                    type="button"
                    className={`nav-link border-0 fw-semibold small ${activeTab === 'centers' ? 'active text-primary border-bottom border-primary border-2' : 'text-secondary'}`}
                    onClick={() => setActiveTab('centers')}
                  >
                    Center Assignments{' '}
                    {currentCenterAssignments.length > 0 && (
                      <span className="badge bg-success-subtle text-success border border-success-subtle ms-1 font-monospace" style={{ fontSize: '0.7rem' }}>
                        {currentCenterAssignments.length}
                      </span>
                    )}
                  </button>
                </li>
                <li className="nav-item" role="presentation">
                  <button
                    type="button"
                    className={`nav-link border-0 fw-semibold small ${activeTab === 'locations' ? 'active text-primary border-bottom border-primary border-2' : 'text-secondary'}`}
                    onClick={() => setActiveTab('locations')}
                  >
                    Location Targets{' '}
                    {locationTargets.length > 0 && (
                      <span className="badge bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle ms-1 font-monospace" style={{ fontSize: '0.7rem' }}>
                        {locationTargets.length}
                      </span>
                    )}
                  </button>
                </li>
                <li className="nav-item" role="presentation">
                  <button
                    type="button"
                    className={`nav-link border-0 fw-semibold small ${activeTab === 'audit' ? 'active text-primary border-bottom border-primary border-2' : 'text-secondary'}`}
                    onClick={() => setActiveTab('audit')}
                  >
                    Audit History ({lifecycleEvents?.length || 0})
                  </button>
                </li>
              </ul>
            </div>
          )}

          <div className="modal-body p-4">
            <ApiErrorAlert error={error} />

            {loading ? (
              <LoadingSpinner message="Loading campaign details..." />
            ) : campaign ? (
              <>
                {/* Tab: Overview */}
                {activeTab === 'overview' && (
                  <div>
                    <div className="d-flex justify-content-between align-items-center mb-3">
                      <div className="small text-muted font-monospace">
                        Internal ID: #{campaign.id}
                      </div>
                      <div className="d-flex gap-2">
                        <span className="small text-muted">
                          Dose:{' '}
                          {latestDoseProgramme ? (
                            <span className="badge bg-success-subtle text-success border border-success-subtle">
                              v{latestDoseProgramme.versionNumber}
                            </span>
                          ) : (
                            <span className="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle">
                              Not Configured
                            </span>
                          )}
                        </span>
                        <span className="small text-muted">
                          Questionnaire:{' '}
                          {activeQuestionnaireAssignment ? (
                            <span className="badge bg-success-subtle text-success border border-success-subtle">
                              Linked
                            </span>
                          ) : (
                            <span className="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle">
                              Not Assigned
                            </span>
                          )}
                        </span>
                        <span className="small text-muted">
                          Centers:{' '}
                          {currentCenterAssignments.length > 0 ? (
                            <span className="badge bg-success-subtle text-success border border-success-subtle">
                              {currentCenterAssignments.length} Active
                            </span>
                          ) : (
                            <span className="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle">
                              0 Active
                            </span>
                          )}
                        </span>
                        <span className="small text-muted">
                          Locations:{' '}
                          {locationTargets.length > 0 ? (
                            <span className="badge bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle">
                              {locationTargets.length} Scoped
                            </span>
                          ) : (
                            <span className="badge bg-light text-muted border">
                              Unrestricted
                            </span>
                          )}
                        </span>
                      </div>
                    </div>

                    {campaign.description && (
                      <div className="alert alert-light border small text-secondary mb-4">
                        {campaign.description}
                      </div>
                    )}

                    <div className="row g-3 mb-4">
                      <div className="col-md-6">
                        <div className="p-3 bg-light rounded border h-100">
                          <h6 className="fw-bold text-dark mb-2">Vaccine & Target Scope</h6>
                          <ul className="list-unstyled mb-0 small">
                            <li className="mb-1">
                              <strong>Vaccine Type: </strong>
                              {campaign.vaccineType ? (
                                <span>{campaign.vaccineType.name} ({campaign.vaccineType.code})</span>
                              ) : (
                                <span className="text-muted">N/A</span>
                              )}
                            </li>
                            <li>
                              <strong>Target Group: </strong>
                              {campaign.targetGroup ? (
                                <span>{campaign.targetGroup.name} ({campaign.targetGroup.code})</span>
                              ) : (
                                <span className="text-muted">N/A</span>
                              )}
                            </li>
                          </ul>
                        </div>
                      </div>

                      <div className="col-md-6">
                        <div className="p-3 bg-light rounded border h-100">
                          <h6 className="fw-bold text-dark mb-2">Schedule & Booking Windows</h6>
                          <ul className="list-unstyled mb-0 small">
                            <li className="mb-1">
                              <strong>Service Period: </strong>
                              {formatDate(campaign.serviceStartDate)} to {formatDate(campaign.serviceEndDate)}
                            </li>
                            <li>
                              <strong>Booking Window: </strong>
                              {formatDateTime(campaign.bookingOpenAt)} to {formatDateTime(campaign.bookingCloseAt)}
                            </li>
                          </ul>
                        </div>
                      </div>
                    </div>

                    {isManager && (
                      <div className="d-flex flex-wrap gap-2 pt-2 border-top">
                        <span className="text-muted small align-self-center me-2">Lifecycle Actions:</span>
                        {campaign.status === 'DRAFT' && (
                          <button
                            type="button"
                            className="btn btn-sm btn-info text-dark"
                            onClick={() => onInitiateAction('SCHEDULE', campaign)}
                          >
                            Schedule Campaign
                          </button>
                        )}
                        {campaign.status === 'SCHEDULED' && (
                          <button
                            type="button"
                            className="btn btn-sm btn-primary"
                            onClick={() => onInitiateAction('PUBLISH', campaign)}
                          >
                            Publish Campaign
                          </button>
                        )}
                        {campaign.status === 'PUBLISHED' && (
                          <button
                            type="button"
                            className="btn btn-sm btn-success"
                            onClick={() => onInitiateAction('ACTIVATE', campaign)}
                          >
                            Activate Campaign
                          </button>
                        )}
                        {campaign.status === 'ACTIVE' && (
                          <button
                            type="button"
                            className="btn btn-sm btn-warning text-dark"
                            onClick={() => onInitiateAction('SUSPEND', campaign)}
                          >
                            Suspend Campaign
                          </button>
                        )}
                        {campaign.status === 'SUSPENDED' && (
                          <button
                            type="button"
                            className="btn btn-sm btn-success"
                            onClick={() => onInitiateAction('RESUME', campaign)}
                          >
                            Resume Campaign
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                )}

                {/* Tab: Dose Programme Configuration */}
                {activeTab === 'dose' && (
                  <DoseProgrammeTab
                    campaign={campaign}
                    doseProgrammes={doseProgrammes}
                    loading={doseLoading}
                    error={doseError}
                    onOpenCreateForm={() => {
                      setDoseModalError(null);
                      setDoseModalMode('CREATE');
                    }}
                    onOpenNewVersionForm={() => {
                      setDoseModalError(null);
                      setDoseModalMode('NEW_VERSION');
                    }}
                  />
                )}

                {/* Tab: Questionnaire Linkage */}
                {canViewQuestionnaireAssignmentHistory && activeTab === 'questionnaire' && (
                  <QuestionnaireTab
                    campaign={campaign}
                    assignments={questionnaireAssignments}
                    versionsMap={versionsMap}
                    loading={questionnaireLoading}
                    error={questionnaireError}
                    canManageAssignment={canManageQuestionnaireAssignment}
                    onOpenAssignModal={() => {
                      setQuestionnaireModalError(null);
                      setShowQuestionnaireModal(true);
                    }}
                  />
                )}

                {/* Tab: Center Assignments */}
                {activeTab === 'centers' && (
                  <CenterAssignmentsTab
                    campaign={campaign}
                    assignments={centerAssignments}
                    loading={centerLoading}
                    error={centerError}
                    onOpenCreateModal={() => {
                      setCenterModalError(null);
                      setSelectedCenterAssignment(null);
                      setCenterModalMode('CREATE');
                    }}
                    onOpenUpdateModal={(a) => {
                      setCenterModalError(null);
                      setSelectedCenterAssignment(a);
                      setCenterModalMode('UPDATE_FUTURE');
                    }}
                    onOpenEndModal={(a) => {
                      setCenterModalError(null);
                      setSelectedCenterAssignment(a);
                      setCenterModalMode('END_ACTIVE');
                    }}
                    onOpenCancelModal={(a) => {
                      setCenterModalError(null);
                      setSelectedCenterAssignment(a);
                      setCenterModalMode('CANCEL_CONFIRM');
                    }}
                  />
                )}

                {/* Tab: Location Targets */}
                {activeTab === 'locations' && (
                  <LocationTargetsTab
                    campaign={campaign}
                    targets={locationTargets}
                    loading={locationLoading}
                    error={locationError}
                    onOpenAddModal={() => {
                      setLocationModalError(null);
                      setShowLocationModal(true);
                    }}
                    onRefresh={() => fetchLocationTargets(campaign.id)}
                  />
                )}

                {/* Tab: Audit History */}
                {activeTab === 'audit' && (
                  <div>
                    <h6 className="fw-bold text-dark mb-3">
                      Lifecycle Audit History
                    </h6>

                    {lifecycleEvents && lifecycleEvents.length > 0 ? (
                      <div className="table-responsive mb-3">
                        <table className="table table-sm table-striped small mb-0">
                          <thead>
                            <tr>
                              <th>Transition</th>
                              <th>Reason</th>
                              <th>Officer / Actor</th>
                              <th>Timestamp</th>
                            </tr>
                          </thead>
                          <tbody>
                            {lifecycleEvents.map((evt) => (
                              <tr key={evt.id}>
                                <td>
                                  <span className="badge bg-secondary me-1">{evt.fromStatus}</span>
                                  &rarr;
                                  <span className="badge bg-primary ms-1">{evt.toStatus}</span>
                                </td>
                                <td>{evt.reason || '—'}</td>
                                <td className="font-monospace">#{evt.actorId || '1'}</td>
                                <td>{formatDateTime(evt.occurredAt)}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    ) : (
                      <p className="text-muted small mb-3">No lifecycle state transition events recorded yet.</p>
                    )}
                  </div>
                )}
              </>
            ) : null}
          </div>

          <div className="modal-footer bg-light">
            <button type="button" className="btn btn-secondary btn-sm" onClick={onClose}>
              Close
            </button>
          </div>
        </div>
      </div>

      {/* Dose Programme Form Modal */}
      {doseModalMode && (
        <DoseProgrammeFormModal
          mode={doseModalMode}
          currentVersion={latestDoseProgramme}
          onSubmit={handleDoseFormSubmit}
          onCancel={() => {
            setDoseModalMode(null);
            setDoseModalError(null);
          }}
          loading={doseModalLoading}
          error={doseModalError}
        />
      )}

      {/* Questionnaire Assignment Form Modal */}
      {canManageQuestionnaireAssignment && showQuestionnaireModal && (
        <QuestionnaireAssignmentFormModal
          campaign={campaign}
          currentAssignment={activeQuestionnaireAssignment}
          onSubmit={handleQuestionnaireSubmit}
          onCancel={() => {
            setShowQuestionnaireModal(false);
            setQuestionnaireModalError(null);
          }}
          loading={questionnaireModalLoading}
          error={questionnaireModalError}
        />
      )}

      {/* Center Assignment Mutation Modal */}
      {centerModalMode && (
        <CenterAssignmentModal
          mode={centerModalMode}
          assignment={selectedCenterAssignment}
          activeCenters={activeCenters}
          loadingCenters={loadingActiveCenters}
          onSubmit={handleCenterAssignmentSubmit}
          onCancel={() => {
            setCenterModalMode(null);
            setSelectedCenterAssignment(null);
            setCenterModalError(null);
          }}
          loading={centerModalLoading}
          error={centerModalError}
        />
      )}

      {/* Location Target Add Modal */}
      {showLocationModal && (
        <LocationTargetModal
          isOpen={showLocationModal}
          onClose={() => {
            setShowLocationModal(false);
            setLocationModalError(null);
          }}
          onSubmit={handleLocationTargetSubmit}
          loading={locationModalLoading}
          error={locationModalError}
        />
      )}
    </div>
  );
}
