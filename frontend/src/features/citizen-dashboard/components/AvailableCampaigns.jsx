import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import DashboardIcon from './DashboardIcon';
import { listCitizenCampaigns } from '../../campaign-management/api/campaignApi';

function formatDisplayDate(value) {
  if (!value) return null;
  const date = new Date(value.includes('T') ? value : `${value}T00:00:00`);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }).format(date);
}

function resolveCitizenStatus(campaign) {
  if (campaign.currentlyAcceptingScreening) {
    return { label: 'Screening available', tone: 'positive' };
  }
  if (campaign.status === 'UPCOMING') {
    return { label: 'Upcoming', tone: 'attention' };
  }
  if (campaign.status === 'ACTIVE') {
    return { label: 'Screening available', tone: 'positive' };
  }
  return { label: 'Screening closed', tone: 'neutral' };
}

export default function AvailableCampaigns({ dashboardData }) {
  const campaignsFromDashboard = dashboardData?.screening?.campaigns;
  const fallbackCampaigns = useMemo(
    () => (Array.isArray(campaignsFromDashboard) ? campaignsFromDashboard : []),
    [campaignsFromDashboard],
  );
  const [campaigns, setCampaigns] = useState(fallbackCampaigns);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let active = true;
    setLoading(true);

    listCitizenCampaigns()
      .then((data) => {
        if (active && Array.isArray(data) && data.length > 0) {
          setCampaigns(data);
        }
      })
      .catch(() => {
        // Fallback to dashboardData campaigns if discovery endpoint fails or is unmocked
        if (active && fallbackCampaigns.length > 0) {
          setCampaigns(fallbackCampaigns);
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [fallbackCampaigns]);

  const displayList = campaigns.length > 0 ? campaigns : fallbackCampaigns;

  return (
    <section className="citizen-campaigns citizen-panel" aria-labelledby="citizen-campaigns-title">
      <header className="citizen-section-heading">
        <div className="citizen-section-heading__title-group">
          <span className="citizen-section-heading__icon" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            </svg>
          </span>
          <div>
            <h2 id="citizen-campaigns-title">Available Campaigns</h2>
            <p className="citizen-section-heading__subtitle">Explore vaccination campaigns available for you.</p>
          </div>
        </div>
        <Link to="/app/screening" className="citizen-campaigns__header-link">
          View All Campaigns <DashboardIcon name="arrow" size={13} />
        </Link>
      </header>

      {loading && displayList.length === 0 && (
        <div className="citizen-panel__state">
          <span className="citizen-panel__pulse" aria-hidden="true" />
          <p><strong>Loading campaigns</strong><span>Checking campaigns open for citizen screening.</span></p>
        </div>
      )}

      {displayList.length === 0 && !loading && (
        <div className="citizen-campaigns__empty">
          <span className="citizen-campaigns__empty-icon" aria-hidden="true">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            </svg>
          </span>
          <p>
            <strong>No active campaigns open right now</strong>
            <span>Check back soon for upcoming vaccination campaigns, or view the campaign directory.</span>
          </p>
          <Link to="/app/screening" className="citizen-action-link citizen-action-link--secondary">
            View Campaign Directory <DashboardIcon name="arrow" size={14} />
          </Link>
        </div>
      )}

      {displayList.length > 0 && (
        <div className="citizen-campaigns__grid">
          {displayList.map((campaign) => {
            const statusInfo = resolveCitizenStatus(campaign);
            const isAccepting = statusInfo.label === 'Screening available';
            const startDate = formatDisplayDate(campaign.serviceStartDate || campaign.startDate);
            const endDate = formatDisplayDate(campaign.serviceEndDate || campaign.endDate);
            const dateText = startDate && endDate ? `${startDate} – ${endDate}` : startDate || 'Dates on request';

            return (
              <article key={campaign.id} className="citizen-campaign-card">
                <div className="citizen-campaign-card__top">
                  <div className="citizen-campaign-card__icon" aria-hidden="true">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z" />
                    </svg>
                  </div>
                  <span className={`citizen-status citizen-status--${statusInfo.tone}`}>
                    {statusInfo.label}
                  </span>
                </div>

                <div className="citizen-campaign-card__content">
                  <strong className="citizen-campaign-card__title">{campaign.name}</strong>
                  {campaign.description && (
                    <p className="citizen-campaign-card__desc">{campaign.description}</p>
                  )}
                  <div className="citizen-campaign-card__meta">
                    <span className="citizen-campaign-card__meta-item">
                      <span aria-hidden="true">📅</span> {dateText}
                    </span>
                    {campaign.vaccineTypeCode && (
                      <span className="citizen-campaign-card__meta-item">
                        <span aria-hidden="true">💉</span> {campaign.vaccineTypeCode}
                      </span>
                    )}
                  </div>
                </div>

                <div className="citizen-campaign-card__actions">
                  {isAccepting ? (
                    <Link
                      to={`/app/screening/campaigns/${campaign.id}`}
                      className="citizen-action-link citizen-action-link--primary citizen-campaign-card__cta"
                    >
                      Start Screening <DashboardIcon name="arrow" size={14} />
                    </Link>
                  ) : (
                    <Link
                      to="/app/screening"
                      className="citizen-action-link citizen-action-link--secondary citizen-campaign-card__cta"
                    >
                      View Campaign <DashboardIcon name="arrow" size={14} />
                    </Link>
                  )}
                </div>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}
