import CreateRecurrenceRuleModal from "./CreateRecurrenceRuleModal";
import ApiErrorAlert from "../../../components/common/ApiErrorAlert";
import LoadingSpinner from "../../../components/common/LoadingSpinner";
import useSessionRecurrenceWorkflow from "../hooks/useSessionRecurrenceWorkflow";

const WEEKDAYS = [
  { id: 'MONDAY', label: 'Mon' },
  { id: 'TUESDAY', label: 'Tue' },
  { id: 'WEDNESDAY', label: 'Wed' },
  { id: 'THURSDAY', label: 'Thu' },
  { id: 'FRIDAY', label: 'Fri' },
  { id: 'SATURDAY', label: 'Sat' },
  { id: 'SUNDAY', label: 'Sun' },
];

export default function SessionRecurrencePanel({
  campaignsList = [],
  centersList = [],
  campaignsMap = {},
  centersMap = {},
  recurrenceOperationOwnerRef = null,
  recurrenceRefreshGen = 0,
  onRecurrenceDataChanged,
  onSessionDataChanged,
}) {
  const {
    rules,
    rulesLoading,
    rulesError,
    isCreateModalOpen,
    setIsCreateModalOpen,
    filters,
    setFilters,
    selectedRuleId,
    selectedRule,
    ruleDetailLoading,
    ruleDetailError,
    activeAction,
    setActiveAction,
    genStartDate,
    setGenStartDate,
    genEndDate,
    setGenEndDate,
    genLoading,
    genError,
    genResult,
    updateWeekdays,
    updateStartDate,
    setUpdateStartDate,
    updateEndDate,
    setUpdateEndDate,
    updateStartTime,
    setUpdateStartTime,
    updateEndTime,
    setUpdateEndTime,
    updateLoading,
    updateError,
    updateSuccessMsg,
    changeWeekdays,
    changeStartDate,
    setChangeStartDate,
    changeEndDate,
    setChangeEndDate,
    changeStartTime,
    setChangeStartTime,
    changeEndTime,
    setChangeEndTime,
    changeReason,
    setChangeReason,
    changePreviewLoading,
    changePreviewError,
    changePreviewData,
    setChangePreviewData,
    changeConfirmLoading,
    changeConfirmError,
    changeResultData,
    cancelReason,
    setCancelReason,
    cancelPreviewLoading,
    cancelPreviewError,
    cancelPreviewData,
    cancelConfirmLoading,
    cancelConfirmError,
    cancelResultData,
    panelSuccessMsg,
    setPanelSuccessMsg,
    changeConfirmButtonRef,
    cancelConfirmButtonRef,
    activeOpGateRef,
    fetchRules,
    handleSelectRule,
    handleGenerateOccurrences,
    handleUpdateDefinition,
    handlePreviewSeriesChange,
    handleConfirmSeriesChange,
    handlePreviewSeriesCancellation,
    handleConfirmSeriesCancellation,
    toggleUpdateWeekday,
    toggleChangeWeekday,
    handleCreateRuleSuccess,
    cancellationToCancelCount,
    cancellationPreservedCount,
    cancellationIneligibleCount
  } = useSessionRecurrenceWorkflow({ centersMap, recurrenceOperationOwnerRef, recurrenceRefreshGen, onRecurrenceDataChanged, onSessionDataChanged });


return (
    <div className="card shadow-sm border-0">
      <div className="card-header bg-white border-bottom py-3 d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
        <div>
          <h2 className="h4 text-dark fw-bold mb-1">Session Recurrence Rules & Series Management</h2>
          <p className="text-secondary small mb-0">
            Automated session occurrence materialization, schedule updates, and series-wide change controls
          </p>
        </div>
        <button
          type="button"
          className="btn btn-primary d-inline-flex align-items-center gap-2"
          onClick={() => setIsCreateModalOpen(true)}
          id="btn-create-recurrence-rule"
        >
          <i className="bi bi-plus-lg" aria-hidden="true"></i>
          Create Recurrence Rule
        </button>
      </div>

      <div className="card-body p-4">
        {/* Global Panel Success Alert */}
        {panelSuccessMsg && (
          <div className="alert alert-success alert-dismissible fade show border-0 shadow-sm mb-4" role="status" aria-live="polite">
            <strong>Success:</strong> {panelSuccessMsg}
            <button
              type="button"
              className="btn-close"
              aria-label="Close message"
              onClick={() => setPanelSuccessMsg(null)}
            />
          </div>
        )}

        {/* Filter Bar */}
        <div className="bg-light p-3 rounded mb-4 border">
          <div className="row g-3 align-items-end">
            <div className="col-12 col-md-4">
              <label htmlFor="filter-rule-campaign" className="form-label small fw-semibold text-muted mb-1">
                Filter by Campaign
              </label>
              <select
                id="filter-rule-campaign"
                className="form-select form-select-sm"
                value={filters.campaignId}
                onChange={(e) => setFilters((prev) => ({ ...prev, campaignId: e.target.value }))}
              >
                <option value="">All Campaigns</option>
                {campaignsList.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} ({c.code || c.id})
                  </option>
                ))}
              </select>
            </div>

            <div className="col-12 col-md-4">
              <label htmlFor="filter-rule-center" className="form-label small fw-semibold text-muted mb-1">
                Filter by Vaccination Center
              </label>
              <select
                id="filter-rule-center"
                className="form-select form-select-sm"
                value={filters.centerId}
                onChange={(e) => setFilters((prev) => ({ ...prev, centerId: e.target.value }))}
              >
                <option value="">All Centers</option>
                {centersList.map((cnt) => (
                  <option key={cnt.id} value={cnt.id}>
                    {cnt.name} ({cnt.code || cnt.id})
                  </option>
                ))}
              </select>
            </div>

            <div className="col-12 col-md-2">
              <label htmlFor="filter-rule-status" className="form-label small fw-semibold text-muted mb-1">
                Status
              </label>
              <select
                id="filter-rule-status"
                className="form-select form-select-sm"
                value={filters.status}
                onChange={(e) => setFilters((prev) => ({ ...prev, status: e.target.value }))}
              >
                <option value="">All Statuses</option>
                <option value="ACTIVE">ACTIVE</option>
                <option value="CANCELLED">CANCELLED</option>
              </select>
            </div>

            <div className="col-12 col-md-2 d-flex gap-2">
              <button
                type="button"
                className="btn btn-outline-secondary btn-sm flex-fill"
                onClick={() => setFilters({ campaignId: '', centerId: '', status: '' })}
              >
                Reset
              </button>
              <button
                type="button"
                className="btn btn-secondary btn-sm flex-fill"
                onClick={fetchRules}
                disabled={rulesLoading}
              >
                Refresh
              </button>
            </div>
          </div>
        </div>

        {/* Rule Inventory List */}
        <div className="mb-4">
          <h3 className="h6 text-dark fw-bold mb-2">Recurrence Rules Inventory ({rules.length})</h3>

          {rulesError && <ApiErrorAlert error={rulesError} />}

          {rulesLoading ? (
            <div className="p-4 text-center">
              <LoadingSpinner message="Loading session recurrence rules..." />
            </div>
          ) : rules.length === 0 ? (
            <div className="alert alert-info border-0 mb-0" role="status">
              No session recurrence rules found for the selected filter criteria.
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover table-bordered align-middle mb-0">
                <thead className="table-light">
                  <tr>
                    <th scope="col">Rule ID</th>
                    <th scope="col">Campaign</th>
                    <th scope="col">Center</th>
                    <th scope="col">Weekdays</th>
                    <th scope="col">Date Range</th>
                    <th scope="col">Operating Time</th>
                    <th scope="col">Timezone</th>
                    <th scope="col">Status</th>
                    <th scope="col" className="text-center">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {rules.map((rule) => {
                    const isSelected = selectedRuleId === rule.id;
                    const camp = campaignsMap[rule.campaignId];
                    const cent = centersMap[rule.centerId];
                    const isCancelled = rule.status === 'CANCELLED';

                    return (
                      <tr key={rule.id} className={isSelected ? 'table-primary' : ''}>
                        <td className="fw-bold">#{rule.id}</td>
                        <td>{camp?.name || `Campaign #${rule.campaignId}`}</td>
                        <td>{cent?.name || `Center #${rule.centerId}`}</td>
                        <td>
                          <div className="d-flex flex-wrap gap-1">
                            {rule.weekdays?.map((w) => (
                              <span key={w} className="badge bg-light text-dark border">
                                {w.substring(0, 3)}
                              </span>
                            ))}
                          </div>
                        </td>
                        <td>
                          {rule.startDate} <span className="text-muted">to</span> {rule.endDate}
                        </td>
                        <td>
                          {rule.startTime} - {rule.endTime}
                        </td>
                        <td>
                          <span className="badge bg-secondary">{rule.timezone}</span>
                        </td>
                        <td>
                          <span className={`badge ${isCancelled ? 'bg-danger' : 'bg-success'}`}>
                            {rule.status}
                          </span>
                        </td>
                        <td className="text-center">
                          <button
                            type="button"
                            className={`btn btn-sm ${isSelected ? 'btn-primary' : 'btn-outline-primary'}`}
                            onClick={() => handleSelectRule(rule.id)}
                            id={`btn-select-rule-${rule.id}`}
                          >
                            {isSelected ? 'Managing' : 'Manage Series'}
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Selected Rule Management Workspace */}
        {selectedRuleId && (
          <div className="card border shadow-sm mt-4">
            <div className="card-header bg-light d-flex justify-content-between align-items-center py-2">
              <h3 className="h6 text-dark fw-bold mb-0">
                Manage Series for Recurrence Rule #{selectedRuleId}
                {selectedRule && (
                  <span className={`badge ms-2 ${selectedRule.status === 'CANCELLED' ? 'bg-danger' : 'bg-success'}`}>
                    {selectedRule.status}
                  </span>
                )}
              </h3>
              <button
                type="button"
                className="btn-close"
                aria-label="Close rule workspace"
                onClick={() => handleSelectRule(null)}
              />
            </div>

            <div className="card-body p-4">
              {ruleDetailError && <ApiErrorAlert error={ruleDetailError} />}

              {ruleDetailLoading ? (
                <div className="p-4 text-center">
                  <LoadingSpinner message="Loading recurrence rule details..." />
                </div>
              ) : selectedRule ? (
                <div>
                  {/* Rule summary ribbon */}
                  <div className="bg-light p-3 rounded mb-4 border">
                    <div className="row g-3 small">
                      <div className="col-12 col-md-3">
                        <span className="text-muted d-block">Campaign:</span>
                        <strong className="text-dark">
                          {campaignsMap[selectedRule.campaignId]?.name || `#${selectedRule.campaignId}`}
                        </strong>
                      </div>
                      <div className="col-12 col-md-3">
                        <span className="text-muted d-block">Center:</span>
                        <strong className="text-dark">
                          {centersMap[selectedRule.centerId]?.name || `#${selectedRule.centerId}`}
                        </strong>
                      </div>
                      <div className="col-12 col-md-3">
                        <span className="text-muted d-block">Schedule Window:</span>
                        <strong className="text-dark">
                          {selectedRule.startDate} to {selectedRule.endDate} ({selectedRule.startTime} - {selectedRule.endTime})
                        </strong>
                      </div>
                      <div className="col-12 col-md-3">
                        <span className="text-muted d-block">Timezone & Generation Key:</span>
                        <span className="badge bg-secondary me-2">{selectedRule.timezone}</span>
                        <code className="small text-muted">{selectedRule.generationKey?.substring(0, 8)}...</code>
                      </div>
                    </div>
                  </div>

                  {/* Operation Nav Pills with full tab accessibility */}
                  <ul className="nav nav-pills mb-4 border-bottom pb-3 gap-2" role="tablist" aria-label="Recurrence rule operations">
                    <li className="nav-item" role="presentation">
                      <button
                        type="button"
                        className={`nav-link btn-sm ${activeAction === 'view' ? 'active' : ''}`}
                        onClick={() => setActiveAction('view')}
                        role="tab"
                        id="tab-rule-overview"
                        aria-selected={activeAction === 'view'}
                        aria-controls="panel-rule-action-content"
                      >
                        Overview
                      </button>
                    </li>
                    <li className="nav-item" role="presentation">
                      <button
                        type="button"
                        className={`nav-link btn-sm ${activeAction === 'generate' ? 'active' : ''}`}
                        onClick={() => setActiveAction('generate')}
                        disabled={selectedRule.status === 'CANCELLED'}
                        role="tab"
                        id="tab-generate-occurrences"
                        aria-selected={activeAction === 'generate'}
                        aria-controls="panel-rule-action-content"
                      >
                        Generate Occurrences
                      </button>
                    </li>
                    <li className="nav-item" role="presentation">
                      <button
                        type="button"
                        className={`nav-link btn-sm ${activeAction === 'update_definition' ? 'active' : ''}`}
                        onClick={() => setActiveAction('update_definition')}
                        disabled={selectedRule.status === 'CANCELLED'}
                        role="tab"
                        id="tab-update-definition"
                        aria-selected={activeAction === 'update_definition'}
                        aria-controls="panel-rule-action-content"
                      >
                        Update Future Definition (PUT)
                      </button>
                    </li>
                    <li className="nav-item" role="presentation">
                      <button
                        type="button"
                        className={`nav-link btn-sm ${activeAction === 'series_change' ? 'active' : ''}`}
                        onClick={() => setActiveAction('series_change')}
                        disabled={selectedRule.status === 'CANCELLED'}
                        role="tab"
                        id="tab-series-change"
                        aria-selected={activeAction === 'series_change'}
                        aria-controls="panel-rule-action-content"
                      >
                        Series Change (Preview & Confirm)
                      </button>
                    </li>
                    <li className="nav-item" role="presentation">
                      <button
                        type="button"
                        className={`nav-link btn-sm text-danger ${activeAction === 'series_cancel' ? 'active bg-danger text-white' : ''}`}
                        onClick={() => setActiveAction('series_cancel')}
                        disabled={selectedRule.status === 'CANCELLED'}
                        role="tab"
                        id="tab-series-cancel"
                        aria-selected={activeAction === 'series_cancel'}
                        aria-controls="panel-rule-action-content"
                      >
                        Series Cancellation (Preview & Confirm)
                      </button>
                    </li>
                  </ul>

                  {/* Action Content Container */}
                  <div
                    id="panel-rule-action-content"
                    role="tabpanel"
                    aria-labelledby={`tab-${activeAction === 'view' ? 'rule-overview' : activeAction === 'generate' ? 'generate-occurrences' : activeAction === 'update_definition' ? 'update-definition' : activeAction === 'series_change' ? 'series-change' : 'series-cancel'}`}
                    tabIndex="0"
                  >
                    {/* Tab 1: Overview */}
                    {activeAction === 'view' && (
                      <div>
                        <h4 className="h6 text-dark fw-bold mb-2">Recurrence Configuration Summary</h4>
                        <p className="text-secondary small">
                          This recurrence rule generates operational session instances according to its bounded date range and wall-clock times.
                        </p>
                        <dl className="row mb-0 small">
                          <dt className="col-sm-3 text-muted">Rule ID:</dt>
                          <dd className="col-sm-9">#{selectedRule.id}</dd>

                          <dt className="col-sm-3 text-muted">Active Weekdays:</dt>
                          <dd className="col-sm-9">{selectedRule.weekdays?.join(', ')}</dd>

                          <dt className="col-sm-3 text-muted">Effective Range:</dt>
                          <dd className="col-sm-9">{selectedRule.startDate} to {selectedRule.endDate}</dd>

                          <dt className="col-sm-3 text-muted">Operating Hours:</dt>
                          <dd className="col-sm-9">{selectedRule.startTime} - {selectedRule.endTime}</dd>

                          <dt className="col-sm-3 text-muted">Status:</dt>
                          <dd className="col-sm-9">
                            <span className={`badge ${selectedRule.status === 'CANCELLED' ? 'bg-danger' : 'bg-success'}`}>
                              {selectedRule.status}
                            </span>
                          </dd>
                        </dl>
                      </div>
                    )}

                    {/* Tab 2: Generate Occurrences */}
                    {activeAction === 'generate' && (
                      <div className="border rounded p-3 bg-light">
                        <h4 className="h6 text-dark fw-bold mb-1">Generate Concrete Session Occurrences</h4>
                        <p className="text-secondary small mb-3">
                          Materializes session instances into the operational inventory. Generation is idempotent: existing session records are safely preserved without creating duplicates. Provide both a start and end date to generate a specific window, or leave both empty for the full rule range.
                        </p>

                        {genError && <ApiErrorAlert error={genError} />}

                        {genResult && (
                          <div className="alert alert-success border-0 shadow-sm mb-3" role="status" aria-live="polite">
                            <h5 className="h6 fw-bold mb-1">Generation Complete</h5>
                            <p className="mb-0 small">
                              <strong>{genResult.createdCount}</strong> new session(s) created; <strong>{genResult.existingCount}</strong> existing occurrence(s) recognized.
                            </p>
                          </div>
                        )}

                        <div className="row g-3 align-items-end mb-3">
                          <div className="col-12 col-md-4">
                            <label htmlFor="gen-start-date" className="form-label small fw-semibold text-muted mb-1">
                              Optional Window Start Date
                            </label>
                            <input
                              type="date"
                              id="gen-start-date"
                              className="form-control form-control-sm"
                              value={genStartDate}
                              onChange={(e) => setGenStartDate(e.target.value)}
                              disabled={genLoading}
                            />
                          </div>
                          <div className="col-12 col-md-4">
                            <label htmlFor="gen-end-date" className="form-label small fw-semibold text-muted mb-1">
                              Optional Window End Date
                            </label>
                            <input
                              type="date"
                              id="gen-end-date"
                              className="form-control form-control-sm"
                              value={genEndDate}
                              onChange={(e) => setGenEndDate(e.target.value)}
                              disabled={genLoading}
                            />
                          </div>
                          <div className="col-12 col-md-4">
                            <button
                              type="button"
                              className="btn btn-primary btn-sm w-100"
                              onClick={handleGenerateOccurrences}
                              disabled={genLoading}
                              id="btn-run-generate"
                            >
                              {genLoading ? 'Generating Occurrences...' : 'Run Idempotent Generation'}
                            </button>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Tab 3: Update Future Rule Definition (PUT) */}
                    {activeAction === 'update_definition' && (
                      <div className="border rounded p-3 bg-light">
                        <h4 className="h6 text-dark fw-bold mb-1">Update Future Rule Definition (Unmaterialized Rules Only)</h4>
                        <p className="text-secondary small mb-3">
                          Directly replaces the recurrence rule definition for an active rule before any session occurrences have been generated. If sessions have already been generated from this rule, use the Series Change workflow instead.
                        </p>

                        {updateError && <ApiErrorAlert error={updateError} />}

                        {updateSuccessMsg && (
                          <div className="alert alert-success border-0 shadow-sm mb-3" role="status" aria-live="polite">
                            {updateSuccessMsg}
                          </div>
                        )}

                        <form onSubmit={handleUpdateDefinition}>
                          <div className="row g-3 mb-3">
                            <div className="col-12">
                              <label id="update-weekdays-label" className="form-label small fw-semibold text-muted d-block mb-1">
                                Weekdays
                              </label>
                              <div className="btn-group flex-wrap w-100" role="group" aria-labelledby="update-weekdays-label">
                                {WEEKDAYS.map((day) => {
                                  const isSel = updateWeekdays.includes(day.id);
                                  return (
                                    <button
                                      key={day.id}
                                      type="button"
                                      className={`btn btn-sm ${isSel ? 'btn-primary text-white fw-bold' : 'btn-outline-secondary'}`}
                                      onClick={() => toggleUpdateWeekday(day.id)}
                                      disabled={updateLoading}
                                    >
                                      {day.label}
                                    </button>
                                  );
                                })}
                              </div>
                            </div>

                            <div className="col-12 col-md-6">
                              <label htmlFor="update-start-date" className="form-label small fw-semibold text-muted mb-1">
                                Start Date
                              </label>
                              <input
                                type="date"
                                id="update-start-date"
                                className="form-control form-control-sm"
                                value={updateStartDate}
                                onChange={(e) => setUpdateStartDate(e.target.value)}
                                disabled={updateLoading}
                              />
                            </div>

                            <div className="col-12 col-md-6">
                              <label htmlFor="update-end-date" className="form-label small fw-semibold text-muted mb-1">
                                End Date
                              </label>
                              <input
                                type="date"
                                id="update-end-date"
                                className="form-control form-control-sm"
                                value={updateEndDate}
                                onChange={(e) => setUpdateEndDate(e.target.value)}
                                disabled={updateLoading}
                              />
                            </div>

                            <div className="col-12 col-md-6">
                              <label htmlFor="update-start-time" className="form-label small fw-semibold text-muted mb-1">
                                Start Time
                              </label>
                              <input
                                type="time"
                                step="1"
                                id="update-start-time"
                                className="form-control form-control-sm"
                                value={updateStartTime}
                                onChange={(e) => setUpdateStartTime(e.target.value)}
                                disabled={updateLoading}
                              />
                            </div>

                            <div className="col-12 col-md-6">
                              <label htmlFor="update-end-time" className="form-label small fw-semibold text-muted mb-1">
                                End Time
                              </label>
                              <input
                                type="time"
                                step="1"
                                id="update-end-time"
                                className="form-control form-control-sm"
                                value={updateEndTime}
                                onChange={(e) => setUpdateEndTime(e.target.value)}
                                disabled={updateLoading}
                              />
                            </div>
                          </div>

                          <button
                            type="submit"
                            className="btn btn-primary btn-sm"
                            disabled={updateLoading}
                            id="btn-submit-update-definition"
                          >
                            {updateLoading ? 'Saving Definition...' : 'Save Definition Update'}
                          </button>
                        </form>
                      </div>
                    )}

                    {/* Tab 4: Series Change Control (Preview -> Confirm) */}
                    {activeAction === 'series_change' && (
                      <div className="border rounded p-3 bg-light">
                        <h4 className="h6 text-dark fw-bold mb-1">Series-Wide Schedule Change Control</h4>
                        <p className="text-secondary small mb-3">
                          Two-step series change control: preview schedule shifts, appointment impacts, and cancellation categorizations across all materialized occurrences before confirming.
                        </p>

                        {changePreviewError && <ApiErrorAlert error={changePreviewError} />}
                        {changeConfirmError && <ApiErrorAlert error={changeConfirmError} />}

                        {changeResultData && (
                          <div className="alert alert-success border-0 shadow-sm mb-3" role="status" aria-live="polite">
                            <h5 className="h6 fw-bold mb-1">Series Change Confirmed & Applied</h5>
                            <p className="mb-0 small">
                              Rescheduled: <strong>{changeResultData.rescheduledCount}</strong> | Cancelled: <strong>{changeResultData.cancelledCount}</strong> | New: <strong>{changeResultData.newCount}</strong> | Preserved: <strong>{changeResultData.preservedCount}</strong> | Audit Events Created: <strong>{changeResultData.eventsCreatedCount}</strong>
                            </p>
                          </div>
                        )}

                        {/* Proposed Schedule Inputs */}
                        <div className="row g-3 mb-3">
                          <div className="col-12">
                            <label id="change-weekdays-label" className="form-label small fw-semibold text-muted d-block mb-1">
                              Proposed Weekdays
                            </label>
                            <div className="btn-group flex-wrap w-100" role="group" aria-labelledby="change-weekdays-label">
                              {WEEKDAYS.map((day) => {
                                const isSel = changeWeekdays.includes(day.id);
                                return (
                                  <button
                                    key={day.id}
                                    type="button"
                                    className={`btn btn-sm ${isSel ? 'btn-primary text-white fw-bold' : 'btn-outline-secondary'}`}
                                    onClick={() => toggleChangeWeekday(day.id)}
                                    disabled={changePreviewLoading || changeConfirmLoading}
                                  >
                                    {day.label}
                                  </button>
                                );
                              })}
                            </div>
                          </div>

                          <div className="col-12 col-md-6">
                            <label htmlFor="change-start-date" className="form-label small fw-semibold text-muted mb-1">
                              Proposed Start Date
                            </label>
                            <input
                              type="date"
                              id="change-start-date"
                              className="form-control form-control-sm"
                              value={changeStartDate}
                              onChange={(e) => {
                                setChangeStartDate(e.target.value);
                                if (changePreviewData) setChangePreviewData(null);
                              }}
                              disabled={changePreviewLoading || changeConfirmLoading}
                            />
                          </div>

                          <div className="col-12 col-md-6">
                            <label htmlFor="change-end-date" className="form-label small fw-semibold text-muted mb-1">
                              Proposed End Date
                            </label>
                            <input
                              type="date"
                              id="change-end-date"
                              className="form-control form-control-sm"
                              value={changeEndDate}
                              onChange={(e) => {
                                setChangeEndDate(e.target.value);
                                if (changePreviewData) setChangePreviewData(null);
                              }}
                              disabled={changePreviewLoading || changeConfirmLoading}
                            />
                          </div>

                          <div className="col-12 col-md-6">
                            <label htmlFor="change-start-time" className="form-label small fw-semibold text-muted mb-1">
                              Proposed Start Time ({centersMap[selectedRule.centerId]?.timezone || selectedRule.timezone})
                            </label>
                            <input
                              type="time"
                              step="1"
                              id="change-start-time"
                              className="form-control form-control-sm"
                              value={changeStartTime}
                              onChange={(e) => {
                                setChangeStartTime(e.target.value);
                                if (changePreviewData) setChangePreviewData(null);
                              }}
                              disabled={changePreviewLoading || changeConfirmLoading}
                            />
                          </div>

                          <div className="col-12 col-md-6">
                            <label htmlFor="change-end-time" className="form-label small fw-semibold text-muted mb-1">
                              Proposed End Time ({centersMap[selectedRule.centerId]?.timezone || selectedRule.timezone})
                            </label>
                            <input
                              type="time"
                              step="1"
                              id="change-end-time"
                              className="form-control form-control-sm"
                              value={changeEndTime}
                              onChange={(e) => {
                                setChangeEndTime(e.target.value);
                                if (changePreviewData) setChangePreviewData(null);
                              }}
                              disabled={changePreviewLoading || changeConfirmLoading}
                            />
                          </div>
                        </div>

                        <div className="d-flex gap-2 mb-4">
                          <button
                            type="button"
                            className="btn btn-outline-primary btn-sm"
                            onClick={handlePreviewSeriesChange}
                            disabled={changePreviewLoading || changeConfirmLoading}
                            id="btn-preview-series-change"
                          >
                            {changePreviewLoading ? 'Calculating Impact...' : 'Step 1: Preview Series Change'}
                          </button>
                        </div>

                        {/* Preview Impact Presentation & Confirmation Box */}
                        {changePreviewData && (
                          <div className="card border-primary shadow-sm mb-3" aria-live="polite">
                            <div className="card-header bg-primary text-white py-2">
                              <h5 className="h6 fw-bold mb-0">Series Change Impact Assessment</h5>
                            </div>
                            <div className="card-body p-3">
                              <div className="row g-3 mb-3 text-center">
                                <div className="col-6 col-md-3">
                                  <div className="p-2 border rounded bg-light">
                                    <span className="small text-muted d-block">Occurrences</span>
                                    <strong className="fs-5">{changePreviewData.occurrenceEntries?.length || 0}</strong>
                                  </div>
                                </div>
                                <div className="col-6 col-md-3">
                                  <div className="p-2 border rounded bg-light">
                                    <span className="small text-muted d-block">Affected Appointments</span>
                                    <strong className="fs-5 text-warning">{changePreviewData.totalAffectedAppointmentsCount}</strong>
                                  </div>
                                </div>
                                <div className="col-6 col-md-3">
                                  <div className="p-2 border rounded bg-light">
                                    <span className="small text-muted d-block">Affected Waitlist</span>
                                    <strong className="fs-5 text-secondary">{changePreviewData.totalAffectedWaitlistCount}</strong>
                                  </div>
                                </div>
                                <div className="col-6 col-md-3">
                                  <div className="p-2 border rounded bg-light">
                                    <span className="small text-muted d-block">Preview Token</span>
                                    <code className="small text-truncate d-block">{changePreviewData.previewToken?.substring(0, 12)}...</code>
                                  </div>
                                </div>
                              </div>

                              {/* Occurrence Breakdown Table */}
                              {changePreviewData.occurrenceEntries?.length > 0 && (
                                <div className="table-responsive mb-3" style={{ maxHeight: '200px' }}>
                                  <table className="table table-sm table-bordered mb-0 small">
                                    <thead className="table-light">
                                      <tr>
                                        <th>Date</th>
                                        <th>Category</th>
                                        <th>Session ID</th>
                                        <th>Appointments</th>
                                        <th>Waitlist</th>
                                      </tr>
                                    </thead>
                                    <tbody>
                                      {changePreviewData.occurrenceEntries.map((occ) => (
                                        <tr key={occ.occurrenceKey}>
                                          <td>{occ.originOccurrenceDate}</td>
                                          <td>
                                            <span className="badge bg-secondary">{occ.category}</span>
                                          </td>
                                          <td>{occ.sessionId ? `#${occ.sessionId}` : '—'}</td>
                                          <td>{occ.affectedAppointmentCount}</td>
                                          <td>{occ.affectedWaitlistCount}</td>
                                        </tr>
                                      ))}
                                    </tbody>
                                  </table>
                                </div>
                              )}

                              {/* Step 2: Reason & Confirm */}
                              <div className="border-top pt-3">
                                <label htmlFor="change-reason" className="form-label small fw-semibold">
                                  Reason for Series Change <span className="text-danger">*</span> (Max 500 chars)
                                </label>
                                <textarea
                                  id="change-reason"
                                  className="form-control form-control-sm mb-3"
                                  rows="2"
                                  placeholder="Explain why this series is being modified (e.g. Center schedule optimization, public holiday schedule)..."
                                  value={changeReason}
                                  onChange={(e) => setChangeReason(e.target.value)}
                                  maxLength={500}
                                  disabled={changeConfirmLoading}
                                />

                                <button
                                  type="button"
                                  ref={changeConfirmButtonRef}
                                  className="btn btn-primary btn-sm"
                                  onClick={handleConfirmSeriesChange}
                                  disabled={changeConfirmLoading || !changeReason.trim()}
                                  id="btn-confirm-series-change"
                                >
                                  {changeConfirmLoading ? 'Applying Changes...' : 'Step 2: Confirm Series Change'}
                                </button>
                              </div>
                            </div>
                          </div>
                        )}
                      </div>
                    )}

                    {/* Tab 5: Series Cancellation (Preview -> Confirm) */}
                    {activeAction === 'series_cancel' && (
                      <div className="border border-danger rounded p-3 bg-light">
                        <h4 className="h6 text-danger fw-bold mb-1">Series-Wide Cancellation Control</h4>
                        <p className="text-secondary small mb-3">
                          Cancels the recurrence rule and all eligible active future occurrences across the campaign. Preserves historical change records and audit provenance.
                        </p>

                        {cancelPreviewError && <ApiErrorAlert error={cancelPreviewError} />}
                        {cancelConfirmError && <ApiErrorAlert error={cancelConfirmError} />}

                        {cancelResultData && (
                          <div className="alert alert-danger border-0 shadow-sm mb-3" role="status" aria-live="polite">
                            <h5 className="h6 fw-bold mb-1">Series Cancellation Applied</h5>
                            <p className="mb-0 small">
                              Cancelled Occurrences: <strong>{cancelResultData.cancelledCount}</strong> | Audit Events Created: <strong>{cancelResultData.eventsCreatedCount}</strong>
                            </p>
                          </div>
                        )}

                        <div className="d-flex gap-2 mb-4">
                          <button
                            type="button"
                            className="btn btn-outline-danger btn-sm"
                            onClick={handlePreviewSeriesCancellation}
                            disabled={cancelPreviewLoading || cancelConfirmLoading}
                            id="btn-preview-series-cancel"
                          >
                            {cancelPreviewLoading ? 'Calculating Impact...' : 'Step 1: Preview Series Cancellation'}
                          </button>
                        </div>

                        {/* Cancellation Impact Assessment */}
                        {cancelPreviewData && (
                          <div className="card border-danger shadow-sm mb-3" aria-live="polite">
                            <div className="card-header bg-danger text-white py-2">
                              <h5 className="h6 fw-bold mb-0">Series Cancellation Impact Assessment</h5>
                            </div>
                            <div className="card-body p-3">
                              <div className="row g-3 mb-3 text-center">
                                <div className="col-6 col-md-3">
                                  <div className="p-2 border rounded bg-light">
                                    <span className="small text-muted d-block">Occurrences to Cancel</span>
                                    <strong className="fs-5 text-danger">{cancellationToCancelCount}</strong>
                                  </div>
                                </div>
                                <div className="col-6 col-md-3">
                                  <div className="p-2 border rounded bg-light">
                                    <span className="small text-muted d-block">Preserved Exceptions</span>
                                    <strong className="fs-5 text-dark">{cancellationPreservedCount}</strong>
                                  </div>
                                </div>
                                <div className="col-6 col-md-3">
                                  <div className="p-2 border rounded bg-light">
                                    <span className="small text-muted d-block">Ineligible / Elapsed</span>
                                    <strong className="fs-5 text-secondary">{cancellationIneligibleCount}</strong>
                                  </div>
                                </div>
                                <div className="col-6 col-md-3">
                                  <div className="p-2 border rounded bg-light">
                                    <span className="small text-muted d-block">Affected Appointments</span>
                                    <strong className="fs-5 text-warning">{cancelPreviewData.totalAffectedAppointmentsCount}</strong>
                                  </div>
                                </div>
                              </div>

                              {/* Step 2: Reason & Confirm */}
                              <div className="border-top pt-3">
                                <label htmlFor="cancel-reason" className="form-label small fw-semibold text-danger">
                                  Cancellation Reason <span className="text-danger">*</span> (Max 500 chars)
                                </label>
                                <textarea
                                  id="cancel-reason"
                                  className="form-control form-control-sm mb-3"
                                  rows="2"
                                  placeholder="Explain why this entire series is being cancelled (e.g. Center emergency closure, campaign scope reduction)..."
                                  value={cancelReason}
                                  onChange={(e) => setCancelReason(e.target.value)}
                                  maxLength={500}
                                  disabled={cancelConfirmLoading}
                                />

                                <button
                                  type="button"
                                  ref={cancelConfirmButtonRef}
                                  className="btn btn-danger btn-sm"
                                  onClick={handleConfirmSeriesCancellation}
                                  disabled={cancelConfirmLoading || !cancelReason.trim()}
                                  id="btn-confirm-series-cancel"
                                >
                                  {cancelConfirmLoading ? 'Cancelling Series...' : 'Step 2: Confirm Series Cancellation'}
                                </button>
                              </div>
                            </div>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              ) : null}
            </div>
          </div>
        )}
      </div>

      {/* Create Recurrence Rule Modal */}
      <CreateRecurrenceRuleModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={handleCreateRuleSuccess}
        campaigns={campaignsList}
        centersMap={centersMap}
        recurrenceOperationOwnerRef={activeOpGateRef}
      />
    </div>
  );
}
