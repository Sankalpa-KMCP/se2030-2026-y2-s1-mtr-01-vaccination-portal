import React from 'react';
import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import CitizenSection from './CitizenSection';
import CitizenErrorState from './CitizenErrorState';
import CitizenStatePanel from '../../../shared/ui/citizen/CitizenStatePanel';
import CitizenStatusBadge from '../../../shared/ui/citizen/CitizenStatusBadge';
import CitizenWorkflowGuide from '../../../shared/ui/citizen/CitizenWorkflowGuide';

afterEach(cleanup);

describe('citizen UI primitives', () => {
  it('renders a labelled section with supporting content', () => {
    render(<CitizenSection eyebrow="Records" title="Recent activity" titleId="recent" description="Recorded activity."><p>Content</p></CitizenSection>);
    expect(screen.getByRole('region', { name: 'Recent activity' })).toBeTruthy();
    expect(screen.getByText('Recorded activity.')).toBeTruthy();
  });

  it('uses status and alert semantics only for relevant state panels', () => {
    const view = render(<CitizenStatePanel title="Loading" status>Working</CitizenStatePanel>);
    expect(screen.getByRole('status')).toBeTruthy();
    view.rerender(<CitizenStatePanel title="Unavailable" tone="error">Try again later.</CitizenStatePanel>);
    expect(screen.getByRole('alert')).toBeTruthy();
  });

  it('renders readable status text', () => {
    render(<CitizenStatusBadge tone="positive">Eligible</CitizenStatusBadge>);
    expect(screen.getByText('Eligible')).toBeTruthy();
  });

  it('presents workflow steps as neutral instructions without invented progress', () => {
    const { container } = render(
      <CitizenWorkflowGuide titleId="guide" steps={['Search', 'Choose', 'Confirm']} />,
    );
    expect(screen.getByText('How it works')).toBeTruthy();
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
    expect(container.querySelector('[aria-current]')).toBeNull();
    expect(container.querySelector('[data-completed]')).toBeNull();
  });

  it('keeps meaningful API error text in the subdued citizen alert', () => {
    render(<CitizenErrorState error={Object.assign(new Error('Campaign unavailable'), { status: 404 })} />);
    expect(screen.getByRole('alert')).toBeTruthy();
    expect(screen.getByText('Information not found')).toBeTruthy();
    expect(screen.getByText('Campaign unavailable')).toBeTruthy();
  });
});
