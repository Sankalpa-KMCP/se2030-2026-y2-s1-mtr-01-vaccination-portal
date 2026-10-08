import React from 'react';
import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import CitizenSection from './CitizenSection';

afterEach(cleanup);

describe('CitizenSection', () => {
  it('renders a labelled section with supporting content', () => {
    render(<CitizenSection eyebrow="Records" title="Recent activity" titleId="recent" description="Recorded activity."><p>Content</p></CitizenSection>);
    expect(screen.getByRole('region', { name: 'Recent activity' })).toBeTruthy();
    expect(screen.getByText('Recorded activity.')).toBeTruthy();
  });
});
