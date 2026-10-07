import React from 'react';
import { useSearchParams } from 'react-router-dom';
import PublicHeader from '../../public-landing/components/PublicHeader';
import PublicFooter from '../../public-landing/components/PublicFooter';
import CertificateVerificationForm from '../components/CertificateVerificationForm';
import '../../public-landing/public-landing.css';
import '../../public-landing/premium-public.css';
import '../../public-landing/clinical-public.css';
import '../../public-landing/glass-header.css';

export default function PublicCertificateVerificationPage({ onLoginClick }) {
  const [searchParams] = useSearchParams();
  const initialCode = searchParams.get('token') || searchParams.get('code') || '';

  return (
    <div className="lp-page">
      <PublicHeader onLoginClick={onLoginClick} />
      
      <main className="lp-main py-5">
        <div className="container" style={{ maxWidth: '850px' }}>
          <div className="text-center mb-4">
            <span className="badge bg-primary-subtle text-primary px-3 py-2 rounded-pill fw-semibold mb-2">
              <i className="bi bi-shield-check me-1"></i>Official Verification Portal
            </span>
            <h1 className="h2 fw-bold text-dark mb-2">Verify Digital Certificate</h1>
            <p className="text-secondary small max-width-md mx-auto">
              Confirm the validity of an official COVID-19 or national vaccination certificate issued by the National Vaccination Authority.
            </p>
          </div>

          <CertificateVerificationForm initialCode={initialCode} />
        </div>
      </main>

      <PublicFooter />
    </div>
  );
}
