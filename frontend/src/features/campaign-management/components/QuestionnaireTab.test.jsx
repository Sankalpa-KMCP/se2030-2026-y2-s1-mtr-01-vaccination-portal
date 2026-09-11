import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import QuestionnaireTab from './QuestionnaireTab';

const campaign = { id: 10, status: 'ACTIVE' };
const today = new Date();
const yesterday = new Date(today);
yesterday.setUTCDate(yesterday.getUTCDate() - 1);
const previousStart = new Date(today);
previousStart.setUTCDate(previousStart.getUTCDate() - 30);
const assignments = [
  {
    id: 2,
    questionnaireVersionId: 102,
    activeFrom: yesterday.toISOString().split('T')[0],
    activeTo: null,
  },
  {
    id: 1,
    questionnaireVersionId: 101,
    activeFrom: previousStart.toISOString().split('T')[0],
    activeTo: yesterday.toISOString().split('T')[0],
  },
];
const versionsMap = {
  101: { questionnaireName: 'Flu Screening', versionNumber: 1 },
  102: { questionnaireName: 'Flu Screening', versionNumber: 2 },
};

describe('QuestionnaireTab assignment visibility', () => {
  afterEach(cleanup);

  it('renders assignment history and exposes replacement to an authorized manager', () => {
    const onOpenAssignModal = vi.fn();
    render(
      <QuestionnaireTab
        campaign={campaign}
        assignments={assignments}
        versionsMap={versionsMap}
        canManageAssignment
        onOpenAssignModal={onOpenAssignModal}
      />,
    );

    expect(screen.getByText('Assignment Version History')).toBeTruthy();
    expect(screen.getByText('Flu Screening — v1')).toBeTruthy();
    expect(screen.getAllByText('Flu Screening — v2')).toHaveLength(2);

    fireEvent.click(screen.getByRole('button', { name: '+ Reassign / Update Version' }));
    expect(onOpenAssignModal).toHaveBeenCalledTimes(1);
  });

  it('renders permitted history but no mutation controls for a read-only viewer', () => {
    render(
      <QuestionnaireTab
        campaign={campaign}
        assignments={assignments}
        versionsMap={versionsMap}
        canManageAssignment={false}
        onOpenAssignModal={vi.fn()}
      />,
    );

    expect(screen.getByText('Assignment Version History')).toBeTruthy();
    expect(screen.getAllByText('Flu Screening — v2')).toHaveLength(2);
    expect(screen.queryByRole('button', { name: /assign|reassign|update version/i })).toBeNull();
  });

  it('does not render a fake assignment control for an empty read-only history', () => {
    render(
      <QuestionnaireTab
        campaign={campaign}
        assignments={[]}
        canManageAssignment={false}
        onOpenAssignModal={vi.fn()}
      />,
    );

    expect(screen.getByText('No screening questionnaire assigned to this campaign yet.')).toBeTruthy();
    expect(screen.queryByRole('button', { name: /assign/i })).toBeNull();
  });
});
