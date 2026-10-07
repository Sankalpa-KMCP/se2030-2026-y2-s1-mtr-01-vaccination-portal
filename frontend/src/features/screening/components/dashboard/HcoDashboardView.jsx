import React, { useEffect, useState } from 'react';
import { listScreeningReviewQueue } from '../../api/screeningApi';
import { listCampaigns } from '../../../campaign-management/api/campaignApi';
import { SCREENING_SUBMISSION_STATUS } from '../../constants/screeningConstants';
import HcoDashboardHeader from './HcoDashboardHeader';
import HcoDashboardMetrics from './HcoDashboardMetrics';
import HcoReviewQueuePreview from './HcoReviewQueuePreview';
import HcoTodayFocus from './HcoTodayFocus';
import HcoPortalAreas from './HcoPortalAreas';
import HcoWorkflowGuide from './HcoWorkflowGuide';
import '../../styles/hco-dashboard.css';

function sortOldestFirst(submissions = []) {
  return [...submissions].sort((a, b) => {
    const timeA = Date.parse(a.submittedAt || a.createdAt) || 0;
    const timeB = Date.parse(b.submittedAt || b.createdAt) || 0;
    return timeA - timeB || Number(a.id || 0) - Number(b.id || 0);
  });
}

export default function HcoDashboardView({ user }) {
  const [submissions, setSubmissions] = useState([]);
  const [queueLoading, setQueueLoading] = useState(true);
  const [awaitingQuestionnaireCount, setAwaitingQuestionnaireCount] = useState(null);
  const [campaignsLoading, setCampaignsLoading] = useState(true);

  useEffect(() => {
    let active = true;

    // 1. Fetch Review Queue
    setQueueLoading(true);
    listScreeningReviewQueue()
      .then((data) => {
        if (active) {
          const list = Array.isArray(data) ? data : [];
          setSubmissions(sortOldestFirst(list));
        }
      })
      .catch(() => {
        if (active) {
          setSubmissions([]);
        }
      })
      .finally(() => {
        if (active) setQueueLoading(false);
      });

    // 2. Fetch Campaigns and count DRAFT campaigns awaiting screening questionnaire
    setCampaignsLoading(true);
    listCampaigns()
      .then((campaigns) => {
        if (!active) return;
        const list = Array.isArray(campaigns) ? campaigns : [];
        const count = list.filter(
          (c) => c.status === 'DRAFT' && c.questionnaireVersionId == null
        ).length;
        setAwaitingQuestionnaireCount(count);
      })
      .catch(() => {
        if (active) {
          setAwaitingQuestionnaireCount(null);
        }
      })
      .finally(() => {
        if (active) setCampaignsLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  // Compute pending reviews strictly from domain status
  const pendingCount = submissions.filter(
    (s) => s.status === SCREENING_SUBMISSION_STATUS.PENDING_REVIEW
  ).length;

  return (
    <div className="hco-dashboard">
      <div className="container-xl">
        {/* Welcome & Compact Account Card */}
        <HcoDashboardHeader user={user} />

        {/* 4 Summary Cards */}
        <HcoDashboardMetrics
          pendingCount={pendingCount}
          awaitingQuestionnaireCount={awaitingQuestionnaireCount}
          loading={queueLoading || campaignsLoading}
        />

        {/* Main Grid: Review Queue (68%) + Today's Focus (32%) */}
        <div className="row g-3 mb-3">
          <div className="col-12 col-lg-8">
            <HcoReviewQueuePreview
              submissions={submissions}
              loading={queueLoading}
            />
          </div>
          <div className="col-12 col-lg-4">
            <HcoTodayFocus />
          </div>
        </div>

        {/* Portal Areas (3 compact cards) */}
        <HcoPortalAreas />

        {/* Static HCO Workflow & Guidance */}
        <HcoWorkflowGuide />
      </div>
    </div>
  );
}
