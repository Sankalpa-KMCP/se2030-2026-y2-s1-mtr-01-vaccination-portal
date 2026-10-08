import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import HcoDashboardView from './HcoDashboardView';
import StaffDashboardPage from '../../../../auth/pages/StaffDashboardPage';
import { SCREENING_SUBMISSION_STATUS } from '../../constants/screeningConstants';

const screeningApiState = vi.hoisted(() => ({
  queue: [],
  campaigns: [],
  queueError: null,
  campaignsError: null,
}));

vi.mock('../../api/screeningApi', () => ({
  listScreeningReviewQueue: vi.fn(async () => {
    if (screeningApiState.queueError) throw screeningApiState.queueError;
    return screeningApiState.queue;
  }),
}));

vi.mock('../../../campaign-management/api/campaignApi', () => ({
  listCampaigns: vi.fn(async () => {
    if (screeningApiState.campaignsError) throw screeningApiState.campaignsError;
    return screeningApiState.campaigns;
  }),
}));

const authState = vi.hoisted(() => ({ user: null }));
vi.mock('../../../../auth/context/AuthContext', () => ({
  useAuth: () => authState,
}));

function renderComponent(ui) {
  return render(<MemoryRouter>{ui}</MemoryRouter>);
}

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

beforeEach(() => {
  screeningApiState.queue = [];
  screeningApiState.campaigns = [];
  screeningApiState.queueError = null;
  screeningApiState.campaignsError = null;
});

describe('Healthcare Officer Dashboard (HcoDashboardView)', () => {
  it('renders welcome heading, eyebrow badge, and real authenticated user account info', async () => {
    const user = {
      fullName: 'Local E2E Healthcare Officer',
      email: 'healthcare@email.com',
      phoneNumber: '0771000003',
      roles: ['ROLE_HEALTHCARE_OFFICER'],
      accountStatus: 'ACTIVE',
    };

    renderComponent(<HcoDashboardView user={user} />);

    expect(screen.getByText('Healthcare Officer Dashboard')).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Welcome, Local E2E Healthcare Officer' })).toBeTruthy();
    expect(screen.getByText('Your Account')).toBeTruthy();
    expect(screen.getByText('healthcare@email.com | 0771000003')).toBeTruthy();
    expect(screen.getByText('Active')).toBeTruthy();
  });

  it('omits Active indicator when accountStatus is not ACTIVE', async () => {
    const user = {
      fullName: 'Healthcare Officer',
      email: 'hco@example.gov',
      roles: ['ROLE_HEALTHCARE_OFFICER'],
      accountStatus: 'PENDING',
    };

    renderComponent(<HcoDashboardView user={user} />);

    expect(screen.queryByText('Active')).toBeNull();
  });

  it('does not render placeholder text when optional account fields like phone are missing', async () => {
    const user = {
      fullName: 'Dr. Smith',
      email: 'smith@hospital.org',
      roles: ['ROLE_HEALTHCARE_OFFICER'],
    };

    renderComponent(<HcoDashboardView user={user} />);

    expect(screen.getByText('smith@hospital.org')).toBeTruthy();
    expect(screen.queryByText('Not provided')).toBeNull();
    expect(screen.queryByText('undefined')).toBeNull();
    expect(screen.queryByText('null')).toBeNull();
  });

  it('counts Pending Reviews strictly from PENDING_REVIEW domain status', async () => {
    screeningApiState.queue = [
      { id: 101, citizenId: 50, status: SCREENING_SUBMISSION_STATUS.PENDING_REVIEW },
      { id: 102, citizenId: 51, status: SCREENING_SUBMISSION_STATUS.PENDING_REVIEW },
      { id: 103, citizenId: 52, status: SCREENING_SUBMISSION_STATUS.CORRECTIONS_REQUIRED },
      { id: 104, citizenId: 53, status: SCREENING_SUBMISSION_STATUS.REVIEWED },
    ];

    renderComponent(<HcoDashboardView user={{ fullName: 'HCO' }} />);

    await waitFor(() => {
      const metricCard = screen.getByRole('link', { name: /pending reviews metric/i });
      expect(within(metricCard).getByText('2')).toBeTruthy();
    });
  });

  it('counts Campaigns Awaiting Questionnaire strictly from DRAFT campaigns without questionnaireVersionId', async () => {
    screeningApiState.campaigns = [
      // 1. DRAFT without questionnaire -> should count
      { id: 1, name: 'Draft Campaign 1', status: 'DRAFT', questionnaireVersionId: null },
      // 2. DRAFT without questionnaire -> should count
      { id: 2, name: 'Draft Campaign 2', status: 'DRAFT', questionnaireVersionId: undefined },
      // 3. DRAFT with assigned questionnaire -> should NOT count
      { id: 3, name: 'Draft Campaign 3', status: 'DRAFT', questionnaireVersionId: 42 },
      // 4. SCHEDULED without questionnaire -> should NOT count
      { id: 4, name: 'Scheduled Campaign', status: 'SCHEDULED', questionnaireVersionId: null },
      // 5. ACTIVE without questionnaire -> should NOT count
      { id: 5, name: 'Active Campaign', status: 'ACTIVE', questionnaireVersionId: null },
      // 6. COMPLETED -> should NOT count
      { id: 6, name: 'Completed Campaign', status: 'COMPLETED', questionnaireVersionId: 10 },
    ];

    renderComponent(<HcoDashboardView user={{ fullName: 'HCO' }} />);

    await waitFor(() => {
      const metricCard = screen.getByRole('link', { name: /campaigns awaiting questionnaire metric/i });
      expect(within(metricCard).getByText('2')).toBeTruthy();
      expect(metricCard.getAttribute('href')).toBe('/app/screening/questionnaires?tab=campaigns');
    });
  });

  it('renders Corrections Required and Eligibility Decisions as action cards without fake stats', async () => {
    renderComponent(<HcoDashboardView user={{ fullName: 'HCO' }} />);

    const correctionsCard = screen.getByRole('link', { name: /corrections required action card/i });
    expect(correctionsCard.getAttribute('href')).toBe('/app/staff/screening/reviews');
    expect(within(correctionsCard).getByText('Awaiting citizen updates')).toBeTruthy();
    expect(within(correctionsCard).getByText('Review queue')).toBeTruthy();

    const decisionsCard = screen.getByRole('link', { name: /eligibility decisions action card/i });
    expect(decisionsCard.getAttribute('href')).toBe('/app/staff/screening/reviews');
    expect(within(decisionsCard).getByText('Record decisions')).toBeTruthy();
    expect(within(decisionsCard).getByText('Review screenings')).toBeTruthy();

    // Verify absolutely no fabricated comparison strings exist anywhere
    expect(screen.queryByText(/from yesterday/i)).toBeNull();
    expect(screen.queryByText(/no change/i)).toBeNull();
    expect(screen.queryByText(/last login/i)).toBeNull();
  });

  it('renders review queue table with real numeric ID and does not synthesize business references', async () => {
    screeningApiState.queue = [
      {
        id: 789,
        citizenId: 42,
        status: SCREENING_SUBMISSION_STATUS.PENDING_REVIEW,
        submittedAt: '2026-09-20T10:30:00Z',
      },
    ];

    renderComponent(<HcoDashboardView user={{ fullName: 'HCO' }} />);

    await waitFor(() => {
      expect(screen.getByText('Citizen #42')).toBeTruthy();
      expect(screen.getByText('Submission #789')).toBeTruthy();
    });

    // Verify no fake format like SCR-2026-000789 is synthesized
    expect(screen.queryByText(/SCR-/i)).toBeNull();

    const reviewBtn = screen.getByRole('link', { name: 'Review' });
    expect(reviewBtn.getAttribute('href')).toBe('/app/staff/screening/reviews/789');
  });

  it('renders compact empty state when review queue has no submissions', async () => {
    screeningApiState.queue = [];

    renderComponent(<HcoDashboardView user={{ fullName: 'HCO' }} />);

    await waitFor(() => {
      expect(screen.getByText('No screenings currently require attention.')).toBeTruthy();
    });
  });

  it('renders Today Focus quick actions pointing to genuine existing routes', async () => {
    renderComponent(<HcoDashboardView user={{ fullName: 'HCO' }} />);

    const reviewQueueLink = screen.getByRole('link', { name: /review screening queue/i });
    expect(reviewQueueLink.getAttribute('href')).toBe('/app/staff/screening/reviews');

    const createQuestionLink = screen.getByRole('link', { name: /create new question/i });
    expect(createQuestionLink.getAttribute('href')).toBe('/app/screening/questions');

    const questionnairesLink = screen.getByRole('link', { name: /manage questionnaires/i });
    expect(questionnairesLink.getAttribute('href')).toBe('/app/screening/questionnaires');

    const viewCorrectionsLink = screen.getByRole('link', { name: /view corrections/i });
    expect(viewCorrectionsLink.getAttribute('href')).toBe('/app/staff/screening/reviews');
  });

  it('renders 3 portal area cards with Open links matching existing application routes', async () => {
    renderComponent(<HcoDashboardView user={{ fullName: 'HCO' }} />);

    expect(screen.getByRole('heading', { name: 'Portal Areas' })).toBeTruthy();

    const screeningCard = screen.getByRole('heading', { name: 'Screening Review' }).closest('.card');
    expect(within(screeningCard).getByRole('link', { name: /open/i }).getAttribute('href'))
      .toBe('/app/staff/screening/reviews');

    const questionsCard = screen.getByRole('heading', { name: 'Screening Questions' }).closest('.card');
    expect(within(questionsCard).getByRole('link', { name: /open/i }).getAttribute('href'))
      .toBe('/app/screening/questions');

    const questionnairesCard = screen.getByRole('heading', { name: 'Screening Questionnaires' }).closest('.card');
    expect(within(questionnairesCard).getByRole('link', { name: /open/i }).getAttribute('href'))
      .toBe('/app/screening/questionnaires');
  });

  it('renders static HCO workflow guidance and does not render fake recent activity', async () => {
    renderComponent(<HcoDashboardView user={{ fullName: 'HCO' }} />);

    const workflowHeading = screen.getByRole('heading', { name: 'HCO Workflow & Guidance' });
    expect(workflowHeading).toBeTruthy();
    const workflowCard = workflowHeading.closest('.hco-card');
    expect(within(workflowCard).getByText('Review submissions')).toBeTruthy();
    expect(within(workflowCard).getByText('Request corrections when needed')).toBeTruthy();
    expect(within(workflowCard).getByText('Record eligibility decisions')).toBeTruthy();
    expect(within(workflowCard).getByText('Maintain screening content')).toBeTruthy();
    expect(within(workflowCard).getByText('Supporting Safe Vaccination')).toBeTruthy();

    // Verify recent activity is completely omitted
    expect(screen.queryByText(/recent activity/i)).toBeNull();
  });

  it('degrades gracefully when API calls fail without breaking dashboard navigation', async () => {
    screeningApiState.queueError = new Error('Network error');
    screeningApiState.campaignsError = new Error('Service down');

    renderComponent(<HcoDashboardView user={{ fullName: 'HCO' }} />);

    await waitFor(() => {
      expect(screen.getByText('No screenings currently require attention.')).toBeTruthy();
    });

    expect(screen.getByRole('heading', { name: 'Today\'s Focus' })).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Screening Review' })).toBeTruthy();
  });

  it('delegates to HcoDashboardView when user is Healthcare Officer on StaffDashboardPage', async () => {
    authState.user = {
      fullName: 'Officer Jane',
      email: 'jane@health.gov',
      roles: ['ROLE_HEALTHCARE_OFFICER'],
      accountStatus: 'ACTIVE',
    };

    renderComponent(<StaffDashboardPage />);

    expect(screen.getByText('Healthcare Officer', { selector: '.admin-role-pill' })).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Welcome, Officer Jane' })).toBeTruthy();
  });

  it('uses the shared staff dashboard shell for Center Manager without HCO tools', async () => {
    authState.user = {
      fullName: 'Manager Bob',
      email: 'bob@center.org',
      roles: ['ROLE_CENTER_MANAGER'],
    };

    renderComponent(<StaffDashboardPage />);

    expect(screen.getByText('Center Manager', { selector: '.admin-role-pill' })).toBeTruthy();
    expect(screen.queryByText('Healthcare Officer Dashboard')).toBeNull();
    expect(screen.getByRole('heading', { name: 'Available Portal Areas' })).toBeTruthy();
  });
});
