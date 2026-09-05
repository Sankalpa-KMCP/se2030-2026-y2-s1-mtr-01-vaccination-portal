import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import CitizenPageHeader from '../../citizen-ui/components/CitizenPageHeader';
import CitizenSection from '../../citizen-ui/components/CitizenSection';
import CitizenStatePanel from '../../citizen-ui/components/CitizenStatePanel';
import CitizenStatusBadge from '../../citizen-ui/components/CitizenStatusBadge';
import { listCampaigns } from '../../campaign-management/api/campaignApi';

const CITIZEN_INTAKE_STATUSES = new Set(['PUBLISHED', 'ACTIVE']);

function formatDate(value) {
  if (!value) return 'Not specified';
  const date = new Date(`${value}T00:00:00`);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(date);
}

export default function CitizenScreeningCampaignsPage() {
  const [campaigns, setCampaigns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;
    listCampaigns()
      .then((data) => {
        if (active) {
          setCampaigns((Array.isArray(data) ? data : []).filter(
            (campaign) => CITIZEN_INTAKE_STATUSES.has(campaign.status),
          ));
        }
      })
      .catch((requestError) => {
        if (active) setError(requestError);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, []);

  return (
    <section className="citizen-page" aria-labelledby="screening-campaigns-title">
      <CitizenPageHeader
        eyebrow="Pre-Vaccination Screening"
        title="Choose a campaign"
        titleId="screening-campaigns-title"
        subtitle="Select an available vaccination campaign to complete its screening questionnaire."
        backTo="/app/citizen"
        backLabel="Back to Dashboard"
      />

      {loading && (
        <CitizenStatePanel eyebrow="Campaign directory" title="Loading vaccination campaigns" tone="loading" status>
          <p>Checking which campaigns are currently open for citizen screening.</p>
        </CitizenStatePanel>
      )}

      {!loading && error && <div className="citizen-page__notice"><ApiErrorAlert error={error} /></div>}

      {!loading && !error && campaigns.length === 0 && (
        <CitizenStatePanel
          eyebrow="Campaign directory"
          title="No campaigns available"
          actions={<Link className="citizen-action citizen-action--secondary" to="/app/citizen">Return to Dashboard</Link>}
        >
          <p>No vaccination campaigns are currently available for screening.</p>
          <p>You can return later without losing any existing records.</p>
        </CitizenStatePanel>
      )}

      {!loading && !error && campaigns.length > 0 && (
        <CitizenSection
          eyebrow="Available now"
          title="Campaign directory"
          titleId="campaign-directory-title"
          description="Each screening is tied to one campaign. Review the recorded details before you continue."
          aside={<CitizenStatusBadge tone="positive">{campaigns.length} available</CitizenStatusBadge>}
        >
          <ul className="citizen-campaign-grid">
            {campaigns.map((campaign) => (
              <li key={campaign.id}>
                <article className="citizen-campaign-card" aria-labelledby={`campaign-${campaign.id}-title`}>
                  <div className="citizen-campaign-card__heading">
                    <div>
                      <h3 id={`campaign-${campaign.id}-title`}>{campaign.name}</h3>
                      <p className="citizen-campaign-card__code">Campaign code: {campaign.code}</p>
                    </div>
                    <CitizenStatusBadge tone="active">{campaign.status}</CitizenStatusBadge>
                  </div>
                  <dl className="citizen-data-grid">
                    <div><dt>Vaccine</dt><dd>{campaign.vaccineTypeCode || 'Not specified'}</dd></div>
                    <div><dt>Target group</dt><dd>{campaign.targetGroupCode || 'Not specified'}</dd></div>
                    <div><dt>Service dates</dt><dd>{formatDate(campaign.serviceStartDate)} – {formatDate(campaign.serviceEndDate)}</dd></div>
                  </dl>
                  <footer className="citizen-campaign-card__footer">
                    <Link className="citizen-action citizen-action--primary" to={`/app/screening/campaigns/${campaign.id}`}>
                      Start screening <span aria-hidden="true">→</span>
                    </Link>
                  </footer>
                </article>
              </li>
            ))}
          </ul>
        </CitizenSection>
      )}
    </section>
  );
}
