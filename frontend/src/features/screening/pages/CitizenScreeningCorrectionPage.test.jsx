import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

const mocks = vi.hoisted(() => ({
  getScreeningCorrectionForm: vi.fn(),
  resubmitScreening: vi.fn(),
  getCitizenScreeningForm: vi.fn(),
  getEligibilityAssessmentBySubmission: vi.fn(),
}));

vi.mock('../api/screeningApi', () => mocks);

import CitizenScreeningCorrectionPage from './CitizenScreeningCorrectionPage';

const singleOptions = [
  { id: 21, code: 'YES', label: 'Single yes', typedValue: 'Y', displayOrder: 2 },
  { id: 20, code: 'NO', label: 'Single no', typedValue: 'N', displayOrder: 1 },
];
const multiOptions = [
  { id: 31, code: 'ASTHMA', label: 'Asthma', typedValue: 'asthma', displayOrder: 1 },
  { id: 32, code: 'DIABETES', label: 'Diabetes', typedValue: 'diabetes', displayOrder: 2 },
];

const correctionForm = {
  submissionId: 42,
  campaignId: 22,
  questionnaireVersionId: 90,
  questionnaireVersionNumber: 4,
  status: 'CORRECTIONS_REQUIRED',
  correctionInstructions: 'Correct the date and review every answer.',
  correctionRequestedAt: '2026-09-05T02:00:00Z',
  questions: [
    { id: 106, text: 'Text question', questionType: 'TEXT', required: true, displayOrder: 6, options: [], currentAnswer: { typedAnswer: 'Original notes', selectedOptions: [] } },
    { id: 101, text: 'Boolean question', questionType: 'BOOLEAN', required: true, displayOrder: 1, options: [], currentAnswer: { typedAnswer: 'true', selectedOptions: [] } },
    { id: 102, text: 'Single question', questionType: 'SINGLE_CHOICE', required: true, displayOrder: 2, options: singleOptions, currentAnswer: { typedAnswer: 'N', selectedOptions: [singleOptions[1]] } },
    { id: 103, text: 'Multi question', questionType: 'MULTI_CHOICE', required: true, displayOrder: 3, options: multiOptions, currentAnswer: { typedAnswer: 'diabetes,asthma', selectedOptions: [multiOptions[1], multiOptions[0]] } },
    { id: 104, text: 'Numeric question', questionType: 'NUMERIC', required: true, displayOrder: 4, options: [], currentAnswer: { typedAnswer: '37.50', selectedOptions: [] } },
    { id: 105, text: 'Date question', questionType: 'DATE', required: true, displayOrder: 5, options: [], currentAnswer: { typedAnswer: '2026-09-05', selectedOptions: [] } },
    { id: 107, text: 'Optional question', questionType: 'TEXT', required: false, displayOrder: 7, options: [], currentAnswer: null },
    { id: 108, text: 'Prefilled optional question', questionType: 'TEXT', required: false, displayOrder: 8, options: [], currentAnswer: { typedAnswer: 'Keep this optional answer', selectedOptions: [] } },
  ],
};

function form(overrides = {}) {
  return { ...correctionForm, ...overrides };
}

function renderPage(submissionId = 42) {
  return render(
    <MemoryRouter initialEntries={[`/app/screening/history/${submissionId}/correct`]}>
      <Routes>
        <Route
          path="/app/screening/history/:submissionId/correct"
          element={<CitizenScreeningCorrectionPage />}
        />
      </Routes>
    </MemoryRouter>,
  );
}

async function waitForForm() {
  return screen.findByRole('heading', { name: 'Correct Screening' });
}

describe('CitizenScreeningCorrectionPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getScreeningCorrectionForm.mockResolvedValue(form());
    mocks.resubmitScreening.mockResolvedValue({
      id: 42,
      status: 'PENDING_REVIEW',
      preliminaryResult: 'PRELIMINARY_PASS',
    });
  });
  afterEach(cleanup);

  it('shows a loading state and requests only the historical correction form', async () => {
    let resolveForm;
    mocks.getScreeningCorrectionForm.mockReturnValue(new Promise((resolve) => { resolveForm = resolve; }));
    renderPage();

    expect(screen.getByRole('status').textContent).toBe('Loading correction form...');
    resolveForm(form());
    await waitForForm();
    expect(mocks.getScreeningCorrectionForm).toHaveBeenCalledExactlyOnceWith('42');
    expect(mocks.getCitizenScreeningForm).not.toHaveBeenCalled();
    expect(mocks.getEligibilityAssessmentBySubmission).not.toHaveBeenCalled();
  });

  it('handles a general correction-form load error', async () => {
    mocks.getScreeningCorrectionForm.mockRejectedValue(Object.assign(new Error('Service unavailable'), { status: 500 }));
    renderPage();
    expect(await screen.findByText('Service unavailable')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Submit Corrections' })).toBeNull();
  });

  it.each([
    [403, 'Permission Denied'],
    [404, 'The screening submission could not be found.'],
  ])('handles HTTP %s correction-form failures', async (status, expectedText) => {
    mocks.getScreeningCorrectionForm.mockRejectedValue(Object.assign(new Error('Backend message'), { status }));
    renderPage();
    expect(await screen.findByText(expectedText)).toBeTruthy();
  });

  it('shows a distinct lifecycle message and recovery actions for a 409 load conflict', async () => {
    mocks.getScreeningCorrectionForm.mockRejectedValue(Object.assign(new Error('Conflict'), { status: 409 }));
    renderPage();
    expect(await screen.findByText('This screening is no longer available for correction.')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Reload correction request' })).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Return to Screening Details' }).getAttribute('href'))
      .toBe('/app/screening/history/42');
  });

  it('treats a non-correction response as a stale lifecycle conflict', async () => {
    mocks.getScreeningCorrectionForm.mockResolvedValue(form({ status: 'PENDING_REVIEW' }));
    renderPage();
    expect(await screen.findByText('This screening is no longer available for correction.')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Submit Corrections' })).toBeNull();
  });

  it('shows instructions, request date, questionnaire version, and every historical question', async () => {
    renderPage();
    await waitForForm();

    expect(screen.getByText('Correct the date and review every answer.')).toBeTruthy();
    expect(screen.getByText(/Requested:/).textContent).toContain('2026');
    expect(screen.getByText('Questionnaire version 4')).toBeTruthy();
    for (const question of correctionForm.questions) {
      expect(screen.getByText(question.text)).toBeTruthy();
    }
    expect(screen.getAllByLabelText('required')).toHaveLength(6);
  });

  it('prefills every supported answer type and leaves an unanswered optional field empty', async () => {
    renderPage();
    await waitForForm();

    expect(screen.getByRole('radio', { name: 'Yes' }).checked).toBe(true);
    expect(screen.getByRole('radio', { name: 'Single no' }).checked).toBe(true);
    expect(screen.getByRole('checkbox', { name: 'Diabetes' }).checked).toBe(true);
    expect(screen.getByRole('checkbox', { name: 'Asthma' }).checked).toBe(true);
    expect(screen.getByRole('spinbutton', { name: /Numeric question/ }).value).toBe('37.50');
    expect(screen.getByLabelText(/Date question/).value).toBe('2026-09-05');
    expect(screen.getByRole('textbox', { name: /Text question/ }).value).toBe('Original notes');
    expect(screen.getByRole('textbox', { name: /Optional question/ }).value).toBe('');
    expect(screen.getByRole('textbox', { name: /Prefilled optional question/ }).value)
      .toBe('Keep this optional answer');
  });

  it('renders all questions as editable controls', async () => {
    renderPage();
    await waitForForm();
    fireEvent.click(screen.getByRole('radio', { name: 'No' }));
    fireEvent.click(screen.getByRole('radio', { name: 'Single yes' }));
    fireEvent.click(screen.getByRole('checkbox', { name: 'Diabetes' }));
    fireEvent.change(screen.getByRole('spinbutton', { name: /Numeric question/ }), { target: { value: '38.1' } });
    fireEvent.change(screen.getByLabelText(/Date question/), { target: { value: '2026-09-06' } });
    fireEvent.change(screen.getByRole('textbox', { name: /Text question/ }), { target: { value: 'Changed notes' } });

    expect(screen.getByRole('radio', { name: 'No' }).checked).toBe(true);
    expect(screen.getByRole('radio', { name: 'Single yes' }).checked).toBe(true);
    expect(screen.getByRole('checkbox', { name: 'Diabetes' }).checked).toBe(false);
    expect(screen.getByRole('spinbutton', { name: /Numeric question/ }).value).toBe('38.1');
    expect(screen.getByLabelText(/Date question/).value).toBe('2026-09-06');
    expect(screen.getByRole('textbox', { name: /Text question/ }).value).toBe('Changed notes');
  });

  it('blocks submission when an effective required answer is empty', async () => {
    mocks.getScreeningCorrectionForm.mockResolvedValue(form({
      questions: correctionForm.questions.map((question) => (
        question.id === 106 ? { ...question, currentAnswer: null } : question
      )),
    }));
    renderPage();
    await waitForForm();
    fireEvent.click(screen.getByRole('button', { name: 'Submit Corrections' }));

    expect(await screen.findByText('This question is required.')).toBeTruthy();
    expect(mocks.resubmitScreening).not.toHaveBeenCalled();
  });

  it('submits the complete form with unchanged, changed, and prefilled optional answers', async () => {
    renderPage();
    await waitForForm();
    fireEvent.change(screen.getByRole('textbox', { name: /Text question/ }), { target: { value: 'Changed notes' } });
    fireEvent.click(screen.getByRole('button', { name: 'Submit Corrections' }));

    await waitFor(() => expect(mocks.resubmitScreening).toHaveBeenCalledExactlyOnceWith('42', {
      101: 'true',
      102: 'NO',
      103: 'DIABETES,ASTHMA',
      104: '37.50',
      105: '2026-09-05',
      106: 'Changed notes',
      108: 'Keep this optional answer',
    }));
  });

  it('disables submission while the request is pending', async () => {
    mocks.resubmitScreening.mockReturnValue(new Promise(() => {}));
    renderPage();
    await waitForForm();
    fireEvent.click(screen.getByRole('button', { name: 'Submit Corrections' }));

    await waitFor(() => expect(screen.getByRole('button', { name: 'Submitting corrections...' }).disabled).toBe(true));
  });

  it('preserves entered values after an API error', async () => {
    mocks.resubmitScreening.mockRejectedValue(new Error('Unable to save corrections'));
    renderPage();
    await waitForForm();
    const text = screen.getByRole('textbox', { name: /Text question/ });
    fireEvent.change(text, { target: { value: 'Keep my changed value' } });
    fireEvent.click(screen.getByRole('button', { name: 'Submit Corrections' }));

    expect(await screen.findByText('Unable to save corrections')).toBeTruthy();
    expect(text.value).toBe('Keep my changed value');
  });

  it('shows a distinct 409 resubmission conflict without clearing the form', async () => {
    mocks.resubmitScreening.mockRejectedValue(Object.assign(new Error('State changed'), { status: 409 }));
    renderPage();
    await waitForForm();
    const text = screen.getByRole('textbox', { name: /Text question/ });
    fireEvent.change(text, { target: { value: 'Still here' } });
    fireEvent.click(screen.getByRole('button', { name: 'Submit Corrections' }));

    expect(await screen.findByText('This screening is no longer available for correction.')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Reload correction request' })).toBeTruthy();
    expect(text.value).toBe('Still here');
  });

  it('shows a safe success state and does not render another correction form', async () => {
    renderPage();
    await waitForForm();
    fireEvent.click(screen.getByRole('button', { name: 'Submit Corrections' }));

    expect(await screen.findByRole('heading', { name: 'Corrections submitted successfully' })).toBeTruthy();
    expect(screen.getByText('Your screening is awaiting review again.')).toBeTruthy();
    expect(screen.getByText('Pending review')).toBeTruthy();
    expect(screen.getByText('Preliminary screening passed')).toBeTruthy();
    expect(screen.getByText(/not a final eligibility decision/i)).toBeTruthy();
    expect(screen.queryByText(/^Eligible$/)).toBeNull();
    expect(screen.queryByRole('button', { name: 'Submit Corrections' })).toBeNull();
    expect(screen.getByRole('link', { name: 'View Screening Details' }).getAttribute('href'))
      .toBe('/app/screening/history/42');
    expect(screen.getByRole('link', { name: 'View My Eligibility' }).getAttribute('href'))
      .toBe('/app/screening/eligibility');
  });

  it('does not render or request a new consent flow', async () => {
    renderPage();
    await waitForForm();
    expect(screen.queryByRole('checkbox', { name: /consent|agree/i })).toBeNull();
    expect(screen.queryByText(/consent notice/i)).toBeNull();
    expect(mocks.getCitizenScreeningForm).not.toHaveBeenCalled();
  });

  it('handles an unknown legacy choice without crashing or selecting an unrelated option', async () => {
    mocks.getScreeningCorrectionForm.mockResolvedValue(form({
      questions: [{
        ...correctionForm.questions.find((question) => question.id === 102),
        currentAnswer: { typedAnswer: 'LEGACY', selectedOptions: [] },
      }],
    }));
    renderPage();
    await waitForForm();
    expect(screen.getByText(/previous answer \(LEGACY\) no longer matches/i)).toBeTruthy();
    expect(screen.getByRole('radio', { name: 'Single no' }).checked).toBe(false);
    expect(screen.getByRole('radio', { name: 'Single yes' }).checked).toBe(false);
  });
});
