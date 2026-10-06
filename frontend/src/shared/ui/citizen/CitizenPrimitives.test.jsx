import React from 'react';
import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import CitizenStatePanel from './CitizenStatePanel';
import CitizenStatusBadge from './CitizenStatusBadge';
import CitizenWorkflowGuide from './CitizenWorkflowGuide';

afterEach(cleanup);

describe('citizen UI primitives', () => {
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
});
