import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import QuestionnaireAssignmentFormModal from './QuestionnaireAssignmentFormModal';

const apiMocks = vi.hoisted(() => ({
  getScreeningQuestionnaires: vi.fn(),
  getQuestionnaireVersions: vi.fn(),
}));

vi.mock('../api/campaignConfigurationApi', () => apiMocks);

function dateAfter(days) {
  const date = new Date();
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().split('T')[0];
}

const campaign = {
  id: 10,
  name: 'Influenza 2026',
  serviceStartDate: dateAfter(30),
};

describe('QuestionnaireAssignmentFormModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiMocks.getScreeningQuestionnaires.mockResolvedValue([
      { id: 20, code: 'FLU-SCREENING', name: 'Influenza Screening' },
    ]);
    apiMocks.getQuestionnaireVersions.mockResolvedValue([
      { id: 201, versionNumber: 1, status: 'DRAFT' },
      { id: 202, versionNumber: 2, status: 'PUBLISHED', publishedAt: '2026-09-01T10:00:00Z' },
    ]);
  });

  afterEach(cleanup);

  it('submits a published version and effective date through the existing assignment form', async () => {
    const onSubmit = vi.fn();
    render(
      <QuestionnaireAssignmentFormModal
        campaign={campaign}
        onSubmit={onSubmit}
        onCancel={vi.fn()}
        loading={false}
        error={null}
      />,
    );

    expect(await screen.findByRole('option', { name: /v2/i })).toBeTruthy();
    expect(screen.queryByRole('option', { name: /v1/i })).toBeNull();
    const effectiveStartDate = dateAfter(45);
    fireEvent.change(screen.getByLabelText('Effective Start Date *'), {
      target: { value: effectiveStartDate },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Assign Questionnaire' }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({
        questionnaireVersionId: 202,
        effectiveStartDate,
      });
    });
  });

  it('preserves the existing replacement presentation and submit flow', async () => {
    const onSubmit = vi.fn();
    render(
      <QuestionnaireAssignmentFormModal
        campaign={campaign}
        currentAssignment={{ id: 1, questionnaireVersionId: 201 }}
        onSubmit={onSubmit}
        onCancel={vi.fn()}
        loading={false}
        error={null}
      />,
    );

    expect(await screen.findByRole('heading', { name: 'Update Questionnaire Assignment' })).toBeTruthy();
    expect(await screen.findByRole('option', { name: /v2/i })).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Effective Start Date *'), {
      target: { value: dateAfter(60) },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Update Assignment' }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
  });
});
