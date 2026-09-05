import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import CitizenPageHeader from '../../citizen-ui/components/CitizenPageHeader';
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
    <section aria-labelledby="screening-campaigns-title">
      <CitizenPageHeader
        eyebrow="Pre-Vaccination Screening"
        title="Choose a campaign"
        titleId="screening-campaigns-title"
        subtitle="Select an available vaccination campaign to complete its screening questionnaire."
        backTo="/app/citizen"
        backLabel="Back to Dashboard"
      />

      {loading && <LoadingSpinner message="Loading vaccination campaigns..." />}

      {!loading && <ApiErrorAlert error={error} />}

      {!loading && !error && campaigns.length === 0 && (
        <div className="card border-0 bg-light">
          <div className="card-body p-4 text-center">
            <h2 className="h5">No campaigns available</h2>
            <p className="text-muted mb-0">No vaccination campaigns are currently available for screening.</p>
          </div>
        </div>
      )}

      {!loading && !error && campaigns.length > 0 && (
        <div className="row g-4">
          {campaigns.map((campaign) => (
            <div className="col-12 col-lg-6" key={campaign.id}>
              <div className="card h-100 shadow-sm border-primary-subtle">
                <div className="card-body p-4 d-flex flex-column">
                  <div className="d-flex justify-content-between gap-3 mb-2">
                    <h2 className="h4 mb-0">{campaign.name}</h2>
                    <span className="badge text-bg-success align-self-start">{campaign.status}</span>
                  </div>
                  <p className="text-muted small mb-3">Campaign code: {campaign.code}</p>
                  <dl className="row small mb-4">
                    <dt className="col-5">Vaccine</dt><dd className="col-7">{campaign.vaccineTypeCode || 'Not specified'}</dd>
                    <dt className="col-5">Target group</dt><dd className="col-7">{campaign.targetGroupCode || 'Not specified'}</dd>
                    <dt className="col-5">Service dates</dt>
                    <dd className="col-7">{formatDate(campaign.serviceStartDate)} – {formatDate(campaign.serviceEndDate)}</dd>
                  </dl>
                  <Link className="btn btn-primary align-self-start mt-auto" to={`/app/screening/campaigns/${campaign.id}`}>
                    Start screening
                  </Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
