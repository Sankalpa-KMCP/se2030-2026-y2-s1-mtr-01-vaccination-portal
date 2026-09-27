import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import CampaignDetailsModal from './CampaignDetailsModal';

const authState = vi.hoisted(() => ({ user: null }));
const apiMocks = vi.hoisted(() => ({
  getDoseProgrammes: vi.fn(),
  createDoseProgramme: vi.fn(),
  createDoseProgrammeVersion: vi.fn(),
  getQuestionnaireAssignments: vi.fn(),
  assignQuestionnaire: vi.fn(),
  getScreeningQuestionnaires: vi.fn(),
  getQuestionnaireVersions: vi.fn(),
  getCenterAssignments: vi.fn(),
  createCenterAssignment: vi.fn(),
  updateCenterAssignment: vi.fn(),
  endCenterAssignment: vi.fn(),
  cancelCenterAssignment: vi.fn(),
  getActiveCenters: vi.fn(),
  getLocationTargets: vi.fn(),
  createLocationTarget: vi.fn(),
}));

vi.mock('../../../auth/context/AuthContext', () => ({
  useAuth: () => ({ user: authState.user }),
}));
vi.mock('../api/campaignConfigurationApi', () => apiMocks);

const campaign = {
  id: 10,
  code: 'FLU-2026',
  name: 'Influenza 2026',
  status: 'ACTIVE',
  serviceStartDate: '2026-10-01',
  serviceEndDate: '2026-12-31',
};

const assignmentStart = new Date();
assignmentStart.setUTCDate(assignmentStart.getUTCDate() - 1);

function renderDetails(role) {
  authState.user = { id: 1, roles: [role] };
  return render(
    <CampaignDetailsModal
      campaign={campaign}
      lifecycleEvents={[]}
      loading={false}
      error={null}
      onClose={vi.fn()}
      onInitiateAction={vi.fn()}
    />,
  );
}

describe('CampaignDetailsModal questionnaire assignment authorization', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiMocks.getDoseProgrammes.mockResolvedValue([]);
    apiMocks.getQuestionnaireAssignments.mockResolvedValue([
      {
        id: 1,
        questionnaireVersionId: 101,
        activeFrom: assignmentStart.toISOString().split('T')[0],
        activeTo: null,
      },
    ]);
    apiMocks.getScreeningQuestionnaires.mockResolvedValue([]);
    apiMocks.getQuestionnaireVersions.mockResolvedValue([]);
    apiMocks.getCenterAssignments.mockResolvedValue([]);
    apiMocks.getActiveCenters.mockResolvedValue([]);
    apiMocks.getLocationTargets.mockResolvedValue([]);
  });

  afterEach(cleanup);

  it.each([
    'ROLE_ADMIN',
    'ROLE_SUPER_ADMIN',
    'ROLE_CAMPAIGN_OFFICER',
    'ROLE_MANAGEMENT_USER',
  ])(
    'shows %s questionnaire information and assignment history without mutation controls',
    async (role) => {
      renderDetails(role);

      fireEvent.click(screen.getByRole('button', { name: /Questionnaire/ }));

      expect(await screen.findByText('Assignment Version History')).toBeTruthy();
      expect(screen.getAllByText('Questionnaire Version #101').length).toBeGreaterThanOrEqual(1);
      expect(screen.queryByRole('button', { name: '+ Reassign / Update Version' })).toBeNull();
      expect(screen.queryByRole('button', { name: '+ Assign Screening Questionnaire' })).toBeNull();
      expect(screen.queryByRole('button', { name: 'Assign Questionnaire' })).toBeNull();
      expect(screen.queryByRole('button', { name: /reassign|update version|change/i })).toBeNull();
      expect(apiMocks.getQuestionnaireAssignments).toHaveBeenCalledWith(10);
    },
  );

  it.each([
    'ROLE_CENTER_MANAGER',
    'ROLE_SYSTEM_ADMINISTRATOR',
    'ROLE_CITIZEN',
  ])('hides assignment history and controls from %s', async (role) => {
    renderDetails(role);

    expect(screen.queryByRole('button', { name: /Questionnaire/ })).toBeNull();
    await waitFor(() => expect(apiMocks.getQuestionnaireAssignments).not.toHaveBeenCalled());
  });
});
