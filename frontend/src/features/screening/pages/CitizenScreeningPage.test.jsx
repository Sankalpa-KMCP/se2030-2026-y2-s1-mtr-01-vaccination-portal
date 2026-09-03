import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

const screeningApiMocks = vi.hoisted(() => ({
  getActiveScreeningSubmission: vi.fn(),
  getCitizenScreeningForm: vi.fn(),
  submitScreening: vi.fn(),
}));
const authState = vi.hoisted(() => ({ current: { user: { id: 7, roles: ['ROLE_CITIZEN'] } } }));

vi.mock('../api/screeningApi', () => screeningApiMocks);
vi.mock('../../../auth/context/AuthContext', () => ({ useAuth: () => authState.current }));

import CitizenScreeningPage from './CitizenScreeningPage';

const screeningForm = {
  campaignId: 22,
  questionnaireVersionId: 31,
  questionnaireVersionNumber: 3,
  consentNotice: {
    id: 4,
    noticeCode: 'PRE_VACCINATION_SCREENING',
    versionNumber: 1,
    title: 'Development screening consent',
    content: 'Backend-provided consent wording.',
  },
  questions: [
    { id: 101, text: 'Have a fever?', questionType: 'BOOLEAN', required: true, displayOrder: 1, options: [] },
    { id: 102, text: 'Choose one', questionType: 'SINGLE_CHOICE', required: true, displayOrder: 2, options: [
      { id: 1, code: 'NO', label: 'No condition', displayOrder: 1 },
      { id: 2, code: 'YES', label: 'Has condition', displayOrder: 2 },
    ] },
    { id: 103, text: 'Choose conditions', questionType: 'MULTI_CHOICE', required: true, displayOrder: 3, options: [
      { id: 3, code: 'ASTHMA', label: 'Asthma', displayOrder: 1 },
      { id: 4, code: 'DIABETES', label: 'Diabetes', displayOrder: 2 },
    ] },
    { id: 104, text: 'Temperature', questionType: 'NUMERIC', required: true, displayOrder: 4, options: [] },
    { id: 105, text: 'Observation date', questionType: 'DATE', required: true, displayOrder: 5, options: [] },
    { id: 106, text: 'Additional details', questionType: 'TEXT', required: false, displayOrder: 6, options: [] },
  ],
};

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/app/screening/campaigns/22']}>
      <Routes>
        <Route path="/app/screening/campaigns/:campaignId" element={<CitizenScreeningPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

async function waitForForm() {
  return screen.findByRole('heading', { name: 'Pre-Vaccination Screening' });
}

function answerRequiredQuestions() {
  fireEvent.click(screen.getByRole('radio', { name: 'Yes' }));
  fireEvent.click(screen.getByRole('radio', { name: 'No condition' }));
  fireEvent.click(screen.getByRole('checkbox', { name: 'Asthma' }));
  fireEvent.click(screen.getByRole('checkbox', { name: 'Diabetes' }));
  fireEvent.change(screen.getByRole('spinbutton', { name: /temperature/i }), { target: { value: '37.5' } });
  fireEvent.change(document.querySelector('input[type="date"]'), { target: { value: '2026-09-03' } });
}

describe('CitizenScreeningPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    authState.current = { user: { id: 7, roles: ['ROLE_CITIZEN'] } };
    screeningApiMocks.getActiveScreeningSubmission.mockResolvedValue(null);
    screeningApiMocks.getCitizenScreeningForm.mockResolvedValue(screeningForm);
  });
  afterEach(cleanup);

  it('loads the active submission before loading and rendering the form', async () => {
    let resolveActive;
    screeningApiMocks.getActiveScreeningSubmission.mockReturnValue(new Promise((resolve) => { resolveActive = resolve; }));
    renderPage();
    expect(screen.getByRole('status')).toBeTruthy();
    resolveActive(null);

    await waitForForm();
    expect(screeningApiMocks.getActiveScreeningSubmission).toHaveBeenCalledWith(7, '22');
    expect(screeningApiMocks.getCitizenScreeningForm).toHaveBeenCalledWith('22');
  });

  it('displays all six question types and backend-provided consent without its hash', async () => {
    renderPage();
    await waitForForm();

    expect(screen.getByRole('radio', { name: 'Yes' })).toBeTruthy();
    expect(screen.getByRole('radio', { name: 'No condition' })).toBeTruthy();
    expect(screen.getByRole('checkbox', { name: 'Asthma' })).toBeTruthy();
    expect(screen.getByRole('spinbutton', { name: /temperature/i })).toBeTruthy();
    expect(document.querySelector('input[type="date"]')).toBeTruthy();
    expect(screen.getByRole('textbox', { name: /additional details/i })).toBeTruthy();
    expect(screen.getByText('Development screening consent')).toBeTruthy();
    expect(screen.getByText('Backend-provided consent wording.')).toBeTruthy();
    expect(screen.queryByText(/contentHash/i)).toBeNull();
  });

  it('rejects missing required answers and missing consent client-side', async () => {
    renderPage();
    await waitForForm();
    fireEvent.click(screen.getByRole('button', { name: 'Submit screening' }));

    expect(await screen.findAllByText('This question is required.')).toHaveLength(5);
    expect(screen.getByText('You must read and agree to the consent notice before submitting.')).toBeTruthy();
    expect(screeningApiMocks.submitScreening).not.toHaveBeenCalled();
  });

  it('submits trusted IDs and string-serialized answers while omitting an optional blank', async () => {
    screeningApiMocks.submitScreening.mockResolvedValue({
      id: 90,
      status: 'PENDING_REVIEW',
      preliminaryResult: 'PRELIMINARY_PASS',
      submittedAt: '2026-09-03T12:00:00Z',
      questionnaireVersionId: 31,
    });
    renderPage();
    await waitForForm();
    answerRequiredQuestions();
    fireEvent.click(screen.getByRole('checkbox', { name: /I have read and agree/i }));
    fireEvent.click(screen.getByRole('button', { name: 'Submit screening' }));

    await waitFor(() => expect(screeningApiMocks.submitScreening).toHaveBeenCalledWith({
      citizenId: 7,
      campaignId: 22,
      questionnaireVersionId: 31,
      consentNoticeVersionId: 4,
      answers: {
        101: 'true',
        102: 'NO',
        103: 'ASTHMA,DIABETES',
        104: '37.5',
        105: '2026-09-03',
      },
    }));
    expect(await screen.findByText('Preliminary screening passed')).toBeTruthy();
    expect(screen.getByText(/Final eligibility may depend/i)).toBeTruthy();
    expect(screen.queryByText(/^Eligible$/i)).toBeNull();
  });

  it('shows healthcare review wording without implying final ineligibility', async () => {
    screeningApiMocks.submitScreening.mockResolvedValue({
      id: 91,
      status: 'PENDING_REVIEW',
      preliminaryResult: 'PRELIMINARY_REVIEW_REQUIRED',
      submittedAt: '2026-09-03T12:00:00Z',
    });
    renderPage();
    await waitForForm();
    answerRequiredQuestions();
    fireEvent.click(screen.getByRole('checkbox', { name: /I have read and agree/i }));
    fireEvent.click(screen.getByRole('button', { name: 'Submit screening' }));

    expect(await screen.findByText('Healthcare review required')).toBeTruthy();
    expect(screen.getByText(/not a diagnosis or final ineligibility decision/i)).toBeTruthy();
  });

  it('prevents a new form for an existing pending-review submission', async () => {
    screeningApiMocks.getActiveScreeningSubmission.mockResolvedValue({
      id: 50,
      status: 'PENDING_REVIEW',
      preliminaryResult: 'PRELIMINARY_PASS',
      submittedAt: '2026-09-03T12:00:00Z',
    });
    renderPage();

    expect(await screen.findByText('Your screening has already been submitted and is awaiting review.')).toBeTruthy();
    expect(screeningApiMocks.getCitizenScreeningForm).not.toHaveBeenCalled();
    expect(screen.queryByRole('button', { name: 'Submit screening' })).toBeNull();
  });

  it('prevents a new form and displays instructions when corrections are required', async () => {
    screeningApiMocks.getActiveScreeningSubmission.mockResolvedValue({
      id: 51,
      status: 'CORRECTIONS_REQUIRED',
      correctionInstructions: 'Please clarify the date supplied.',
    });
    renderPage();

    expect(await screen.findByText('Corrections are required for this screening.')).toBeTruthy();
    expect(screen.getByText('Please clarify the date supplied.')).toBeTruthy();
    expect(screen.getByText(/upcoming correction workflow/i)).toBeTruthy();
    expect(screeningApiMocks.getCitizenScreeningForm).not.toHaveBeenCalled();
  });

  it('prevents a second screening when an active draft exists', async () => {
    screeningApiMocks.getActiveScreeningSubmission.mockResolvedValue({ id: 52, status: 'DRAFT' });
    renderPage();
    expect(await screen.findByText('A screening draft already exists.')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Submit screening' })).toBeNull();
  });

  it('displays form loading errors', async () => {
    screeningApiMocks.getCitizenScreeningForm.mockRejectedValue(new Error('No screening form is configured'));
    renderPage();
    expect(await screen.findByText('No screening form is configured')).toBeTruthy();
  });

  it('displays submission errors and offers reload for a stale consent conflict', async () => {
    const conflict = Object.assign(new Error('The consent notice changed. Reload the screening form.'), { status: 409 });
    screeningApiMocks.submitScreening.mockRejectedValue(conflict);
    renderPage();
    await waitForForm();
    answerRequiredQuestions();
    fireEvent.click(screen.getByRole('checkbox', { name: /I have read and agree/i }));
    fireEvent.click(screen.getByRole('button', { name: 'Submit screening' }));

    expect(await screen.findByText('The consent notice changed. Reload the screening form.')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Reload screening form' })).toBeTruthy();
  });
});
