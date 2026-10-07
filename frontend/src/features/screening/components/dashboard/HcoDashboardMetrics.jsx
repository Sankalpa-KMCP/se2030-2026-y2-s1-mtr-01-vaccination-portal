import React from 'react';
import { Link } from 'react-router-dom';

export default function HcoDashboardMetrics({
  pendingCount,
  awaitingQuestionnaireCount,
  loading,
}) {
  return (
    <div className="row g-3 mb-3">
      {/* 1. Pending Reviews */}
      <div className="col-12 col-sm-6 col-xl-3">
        <Link
          to="/app/staff/screening/reviews"
          className="hco-metric-card"
          aria-label="Pending Reviews metric, navigate to screening reviews"
        >
          <div className="d-flex align-items-center gap-3 min-w-0">
            <div className="hco-metric-icon-container hco-icon-pending" aria-hidden="true">
              <i className="bi bi-file-earmark-text" />
            </div>
            <div className="min-w-0">
              <div className="hco-metric-label text-truncate">Pending Reviews</div>
              <div className="hco-metric-value">
                {loading ? (
                  <span className="spinner-border spinner-border-sm text-secondary" role="status" aria-hidden="true" />
                ) : (
                  pendingCount ?? 0
                )}
              </div>
              <div className="hco-metric-link-text">
                <span>View queue</span>
                <i className="bi bi-arrow-right" aria-hidden="true" />
              </div>
            </div>
          </div>
          <i className="bi bi-chevron-right hco-metric-chevron" aria-hidden="true" />
        </Link>
      </div>

      {/* 2. Corrections Required (Action Card) */}
      <div className="col-12 col-sm-6 col-xl-3">
        <Link
          to="/app/staff/screening/reviews"
          className="hco-metric-card"
          aria-label="Corrections Required action card, navigate to screening reviews"
        >
          <div className="d-flex align-items-center gap-3 min-w-0">
            <div className="hco-metric-icon-container hco-icon-corrections" aria-hidden="true">
              <i className="bi bi-exclamation-triangle" />
            </div>
            <div className="min-w-0">
              <div className="hco-metric-label text-truncate">Corrections Required</div>
              <div className="hco-metric-action-label">Awaiting citizen updates</div>
              <div className="hco-metric-link-text">
                <span>Review queue</span>
                <i className="bi bi-arrow-right" aria-hidden="true" />
              </div>
            </div>
          </div>
          <i className="bi bi-chevron-right hco-metric-chevron" aria-hidden="true" />
        </Link>
      </div>

      {/* 3. Eligibility Decisions (Action Card) */}
      <div className="col-12 col-sm-6 col-xl-3">
        <Link
          to="/app/staff/screening/reviews"
          className="hco-metric-card"
          aria-label="Eligibility Decisions action card, navigate to screening reviews"
        >
          <div className="d-flex align-items-center gap-3 min-w-0">
            <div className="hco-metric-icon-container hco-icon-decisions" aria-hidden="true">
              <i className="bi bi-check-circle" />
            </div>
            <div className="min-w-0">
              <div className="hco-metric-label text-truncate">Eligibility Decisions</div>
              <div className="hco-metric-action-label">Record decisions</div>
              <div className="hco-metric-link-text">
                <span>Review screenings</span>
                <i className="bi bi-arrow-right" aria-hidden="true" />
              </div>
            </div>
          </div>
          <i className="bi bi-chevron-right hco-metric-chevron" aria-hidden="true" />
        </Link>
      </div>

      {/* 4. Campaigns Awaiting Questionnaire */}
      <div className="col-12 col-sm-6 col-xl-3">
        <Link
          to="/app/screening/questionnaires?tab=campaigns"
          className="hco-metric-card"
          aria-label="Campaigns Awaiting Questionnaire metric, navigate to campaign assignments"
        >
          <div className="d-flex align-items-center gap-3 min-w-0">
            <div className="hco-metric-icon-container hco-icon-drafts" aria-hidden="true">
              <i className="bi bi-calendar-check" />
            </div>
            <div className="min-w-0">
              <div className="hco-metric-label text-truncate">Campaigns Awaiting Questionnaire</div>
              <div className="hco-metric-value">
                {loading ? (
                  <span className="spinner-border spinner-border-sm text-secondary" role="status" aria-hidden="true" />
                ) : (
                  awaitingQuestionnaireCount ?? 0
                )}
              </div>
              <div className="hco-metric-link-text">
                <span>Assign</span>
                <i className="bi bi-arrow-right" aria-hidden="true" />
              </div>
            </div>
          </div>
          <i className="bi bi-chevron-right hco-metric-chevron" aria-hidden="true" />
        </Link>
      </div>
    </div>
  );
}
