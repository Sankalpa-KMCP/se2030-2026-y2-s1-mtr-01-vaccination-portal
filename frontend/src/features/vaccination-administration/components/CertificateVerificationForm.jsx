import React, { useState } from 'react';
import { verifyCertificatePublicly } from '../api/certificateApi';

export default function CertificateVerificationForm({ initialCode = '' }) {
  const [tokenInput, setTokenInput] = useState(initialCode);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  const handleVerify = async (e) => {
    e.preventDefault();
    if (!tokenInput.trim()) {
      setError('Please enter a certificate reference or verification token.');
      return;
    }

    setLoading(true);
    setError('');
    setResult(null);

    try {
      const data = await verifyCertificatePublicly(tokenInput.trim());
      setResult(data);
    } catch (err) {
      setError(err.message || 'Verification request failed.');
    } finally {
      setLoading(false);
    }
  };

  const getStatusColorClass = (status) => {
    switch (status) {
      case 'VALID': return 'text-success border-success bg-success-subtle';
      case 'REVOKED': return 'text-danger border-danger bg-danger-subtle';
      case 'SUPERSEDED': return 'text-warning border-warning bg-warning-subtle';
      default: return 'text-secondary border-secondary bg-secondary-subtle';
    }
  };

  return (
    <div className="card shadow border-0 overflow-hidden mb-4">
      <div className="card-header bg-gradient-primary text-white p-3 d-flex align-items-center" style={{ background: 'linear-gradient(135deg, #1e3c72 0%, #2a5298 100%)' }}>
        <h3 className="h5 mb-0 fw-bold">
          <i className="bi bi-shield-lock-fill me-2"></i>Public Credential Verification
        </h3>
      </div>

      <div className="card-body p-4 p-md-5">
        <p className="text-secondary small mb-4">
          Verify digital check-in signatures of vaccination certificates. Enter the certificate reference code or decrypt token printed on the document below to authenticate the credential.
        </p>

        <form onSubmit={handleVerify} className="mb-4">
          <div className="row g-3">
            <div className="col-md-9">
              <div className="input-group shadow-sm rounded">
                <span className="input-group-text bg-light border-end-0 text-secondary">
                  <i className="bi bi-qr-code-scan"></i>
                </span>
                <input 
                  type="text" 
                  className="form-control border-start-0 py-2.5 focus-ring font-monospace" 
                  placeholder="E.g., CERT-INF-2025-88A9B2 or v_tok_..." 
                  value={tokenInput}
                  onChange={(e) => {
                    setTokenInput(e.target.value);
                    setError('');
                  }}
                  required
                />
              </div>
            </div>
            <div className="col-md-3">
              <button 
                type="submit" 
                className="btn btn-primary w-100 py-2.5 fw-bold shadow-sm"
                disabled={loading}
              >
                {loading ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                    Verifying...
                  </>
                ) : (
                  <>
                    <i className="bi bi-patch-check-fill me-1"></i>Verify Credential
                  </>
                )}
              </button>
            </div>
          </div>
          {error && <div className="text-danger small mt-2 ps-1"><i className="bi bi-x-circle me-1"></i>{error}</div>}
        </form>

        {/* Verification Result Display */}
        {result && (
          <div className="col-12 mt-4 animate-fade-in">
            {result.result === 'INVALID' ? (
              <div className="card border-danger bg-danger-subtle shadow-sm p-4 text-center">
                <div className="mb-3 text-danger">
                  <i className="bi bi-shield-fill-x" style={{ fontSize: '3.5rem' }}></i>
                </div>
                <h4 className="h4 text-danger fw-bold mb-2">Invalid Signature Code</h4>
                <p className="text-dark small mb-0 max-width-md mx-auto">
                  {result.message || 'No active or archived certificate records match the signature token provided. This credential may be forged or entered incorrectly.'}
                </p>
              </div>
            ) : (
              <div className={`card border-3 shadow-sm overflow-hidden`}>
                {/* Result header banner */}
                <div className={`d-flex justify-content-between align-items-center p-3 border-bottom border-3 ${getStatusColorClass(result.result)}`}>
                  <div className="d-flex align-items-center gap-2">
                    <i className={`bi bi-${result.result === 'VALID' ? 'patch-check-fill' : result.result === 'REVOKED' ? 'shield-fill-x' : 'exclamation-circle-fill'}`} style={{ fontSize: '1.25rem' }}></i>
                    <strong className="h6 mb-0 text-uppercase">Signature Check: {result.result}</strong>
                  </div>
                  <span className="font-monospace small">v{result.versionNumber}</span>
                </div>

                <div className="card-body p-4 bg-white">
                  <div className="row g-3 small text-secondary">
                    {/* Privacy Filter Minimal Disclosure Check */}
                    <div className="col-sm-6">
                      <span className="d-block text-muted text-uppercase fw-semibold" style={{ fontSize: '0.75rem' }}>Subject identity</span>
                      <strong className="text-dark fs-6 d-block mt-0.5">{result.citizenDisplayName}</strong>
                      <span className="text-muted small" style={{ fontSize: '0.7rem' }}>(First Name & Surname Masked)</span>
                    </div>
                    <div className="col-sm-6">
                      <span className="d-block text-muted text-uppercase fw-semibold" style={{ fontSize: '0.75rem' }}>Credential Reference</span>
                      <strong className="text-dark font-monospace d-block mt-0.5">{result.certificateReference}</strong>
                    </div>
                    <div className="col-sm-6 border-top pt-2">
                      <span className="d-block text-muted text-uppercase fw-semibold" style={{ fontSize: '0.75rem' }}>Vaccine Formulation</span>
                      <span className="text-dark fw-bold d-block mt-0.5">{result.vaccineName}</span>
                    </div>
                    <div className="col-sm-6 border-top pt-2">
                      <span className="d-block text-muted text-uppercase fw-semibold" style={{ fontSize: '0.75rem' }}>Administered Dose</span>
                      <span className="text-dark fw-bold d-block mt-0.5">Dose {result.doseNumber}</span>
                    </div>
                    <div className="col-sm-6 border-top pt-2">
                      <span className="d-block text-muted text-uppercase fw-semibold" style={{ fontSize: '0.75rem' }}>Administration Date</span>
                      <span className="text-dark d-block mt-0.5">{new Date(result.administrationDate).toLocaleDateString(undefined, { dateStyle: 'medium' })}</span>
                    </div>
                    <div className="col-sm-6 border-top pt-2">
                      <span className="d-block text-muted text-uppercase fw-semibold" style={{ fontSize: '0.75rem' }}>Authority Publisher</span>
                      <span className="text-dark d-block mt-0.5">{result.issuingOrganization}</span>
                    </div>
                  </div>

                  <hr className="my-3" />
                  
                  {/* Privacy note */}
                  <div className="text-muted small d-flex gap-2 align-items-start">
                    <i className="bi bi-info-circle-fill text-primary mt-0.5"></i>
                    <span style={{ fontSize: '0.75rem' }}>
                      <strong>NFR-PRV-01 Privacy Filter Compliance:</strong> This verification is restricted to minimal identity disclosure. No national registers (NIC/Passport), residential addresses, screening answers, or diagnostic medical records are exposed in this public validation response.
                    </span>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
