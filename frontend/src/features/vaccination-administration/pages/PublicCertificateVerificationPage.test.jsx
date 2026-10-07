import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import PublicCertificateVerificationPage from './PublicCertificateVerificationPage';
import * as certificateApi from '../api/certificateApi';

vi.mock('../api/certificateApi', () => ({
  verifyCertificatePublicly: vi.fn(),
}));

describe('PublicCertificateVerificationPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders public header, title, and verification form', () => {
    render(
      <MemoryRouter>
        <PublicCertificateVerificationPage onLoginClick={vi.fn()} />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: /Verify Digital Certificate/i })).toBeTruthy();
    expect(screen.getByPlaceholderText(/E\.g\., CERT-INF/i)).toBeTruthy();
    expect(screen.getByRole('button', { name: /Verify Credential/i })).toBeTruthy();
  });

  it('pre-populates verification token from query param', () => {
    render(
      <MemoryRouter initialEntries={['/verify-certificate?token=v_tok_query_param']}>
        <PublicCertificateVerificationPage onLoginClick={vi.fn()} />
      </MemoryRouter>,
    );

    const input = screen.getByPlaceholderText(/E\.g\., CERT-INF/i);
    expect(input.value).toBe('v_tok_query_param');
  });

  it('verifies valid credential and renders masked privacy result', async () => {
    certificateApi.verifyCertificatePublicly.mockResolvedValue({
      result: 'VALID',
      versionNumber: 1,
      certificateReference: 'CERT-PUB-VALID',
      doseNumber: 1,
      administrationDate: '2026-09-01T09:00:00Z',
      citizenDisplayName: 'Verified Citizen',
      vaccineName: 'Approved COVID-19 / Infectious Disease Formulation',
      issuingOrganization: 'National Vaccination Authority',
    });

    render(
      <MemoryRouter initialEntries={['/verify-certificate?token=v_tok_valid']}>
        <PublicCertificateVerificationPage onLoginClick={vi.fn()} />
      </MemoryRouter>,
    );

    const verifyButton = screen.getByRole('button', { name: /Verify Credential/i });
    fireEvent.click(verifyButton);

    await waitFor(() => {
      expect(screen.getByText(/Signature Check: VALID/i)).toBeTruthy();
      expect(screen.getByText('CERT-PUB-VALID')).toBeTruthy();
      expect(screen.getByText('Verified Citizen')).toBeTruthy();
      expect(screen.getByText(/NFR-PRV-01 Privacy Filter Compliance/i)).toBeTruthy();
    });
  });

  it('renders invalid signature notification when token is not found', async () => {
    certificateApi.verifyCertificatePublicly.mockResolvedValue({
      result: 'INVALID',
      message: 'No active or archived certificate records match the verification token provided.',
    });

    render(
      <MemoryRouter initialEntries={['/verify-certificate?token=bogus_token']}>
        <PublicCertificateVerificationPage onLoginClick={vi.fn()} />
      </MemoryRouter>,
    );

    const verifyButton = screen.getByRole('button', { name: /Verify Credential/i });
    fireEvent.click(verifyButton);

    await waitFor(() => {
      expect(screen.getByText(/Invalid Signature Code/i)).toBeTruthy();
      expect(screen.getByText(/No active or archived certificate records match/i)).toBeTruthy();
    });
  });
});
