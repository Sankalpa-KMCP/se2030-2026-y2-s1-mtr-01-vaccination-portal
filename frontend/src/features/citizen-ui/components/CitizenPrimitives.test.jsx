import React from 'react';
import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import CitizenSection from './CitizenSection';
import CitizenStatePanel from './CitizenStatePanel';
import CitizenStatusBadge from './CitizenStatusBadge';

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
});
