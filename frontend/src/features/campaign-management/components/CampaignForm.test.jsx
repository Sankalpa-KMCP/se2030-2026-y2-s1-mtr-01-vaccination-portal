import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, cleanup, fireEvent, waitFor } from '@testing-library/react';
import CampaignForm from './CampaignForm';

const apiMocks = vi.hoisted(() => ({
  listVaccineTypes: vi.fn(),
  listTargetGroups: vi.fn(),
  getQuestionnaireVersion: vi.fn(),
}));

vi.mock('../api/campaignApi', () => ({
  listVaccineTypes: apiMocks.listVaccineTypes,
  listTargetGroups: apiMocks.listTargetGroups,
}));

vi.mock('../api/campaignConfigurationApi', () => ({
  getQuestionnaireVersion: apiMocks.getQuestionnaireVersion,
}));

describe('CampaignForm — Create and Edit Modes', () => {
  const sampleCampaign = {
    id: 42,
    code: 'CMP-IMMUTABLE-CODE',
    name: 'Initial Campaign Name',
    description: 'Initial Description',
    vaccineTypeId: 1,
    vaccineType: { id: 1, code: 'VAX-01', name: 'Initial Vaccine' },
    targetGroupId: 2,
    targetGroup: { id: 2, code: 'TG-01', description: 'Initial Target' },
    serviceStartDate: '2026-10-01',
    serviceEndDate: '2026-10-31',
    bookingOpenAt: '2026-09-01T09:00:00.000Z',
    bookingCloseAt: '2026-09-30T17:00:00.000Z',
    status: 'ACTIVE',
  };

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date('2026-08-31T12:00:00Z'));
    vi.clearAllMocks();
    apiMocks.listVaccineTypes.mockResolvedValue([
      { id: 1, code: 'VAX-01', name: 'Vaccine One' },
    ]);
    apiMocks.listTargetGroups.mockResolvedValue([
      { id: 2, code: 'TG-01', description: 'Target One' },
    ]);
    apiMocks.getQuestionnaireVersion.mockResolvedValue({
      id: 101,
      questionnaireId: 1,
      questionnaireCode: 'COVID-SCREEN',
      questionnaireName: '2026 General Vaccination Screening',
      versionNumber: 1,
      status: 'PUBLISHED',
      publishedAt: '2026-09-01T10:00:00Z',
    });
  });

  afterEach(() => {
    cleanup();
    vi.useRealTimers();
  });

  it('edit mode prefills approved values and displays Edit Campaign header', () => {
    render(
      <CampaignForm
        initialCampaign={sampleCampaign}
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
      />
    );

    expect(screen.getByRole('heading', { level: 5 }).textContent).toContain('Edit Campaign');
    expect(screen.getByLabelText(/Campaign Name/i).value).toBe('Initial Campaign Name');
    expect(screen.getByLabelText(/Description/i).value).toBe('Initial Description');
    expect(screen.getByLabelText(/Service Start Date/i).value).toBe('2026-10-01');
    expect(screen.getByLabelText(/Service End Date/i).value).toBe('2026-10-31');

    // Campaign code is disabled / read-only
    const codeInput = screen.getByLabelText(/Campaign Code/i);
    expect(codeInput.value).toBe('CMP-IMMUTABLE-CODE');
    expect(codeInput.disabled).toBe(true);
  });

  it('edit mode payload contains exactly approved editable fields and omits immutable fields', () => {
    const onSubmit = vi.fn();
    render(
      <CampaignForm
        initialCampaign={sampleCampaign}
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />
    );

    const nameInput = screen.getByLabelText(/Campaign Name/i);
    fireEvent.change(nameInput, { target: { value: 'Updated Campaign Name' } });

    const submitBtn = screen.getByRole('button', { name: /Save Changes/i });
    fireEvent.click(submitBtn);

    expect(onSubmit).toHaveBeenCalledTimes(1);
    const payload = onSubmit.mock.calls[0][0];

    // Approved editable fields are present
    expect(payload.name).toBe('Updated Campaign Name');
    expect(payload.description).toBe('Initial Description');
    expect(payload.serviceStartDate).toBe('2026-10-01');
    expect(payload.serviceEndDate).toBe('2026-10-31');
    expect(payload.bookingOpenAt).toBeDefined();
    expect(payload.bookingCloseAt).toBeDefined();

    // Immutable fields are strictly omitted
    expect(payload.code).toBeUndefined();
    expect(payload.vaccineTypeId).toBeUndefined();
    expect(payload.vaccineType).toBeUndefined();
    expect(payload.targetGroupId).toBeUndefined();
    expect(payload.targetGroup).toBeUndefined();
    expect(payload.status).toBeUndefined();
    expect(payload.id).toBeUndefined();
  });

  it('edit mode rejects service end date before service start date with inline feedback', () => {
    const onSubmit = vi.fn();
    render(
      <CampaignForm
        initialCampaign={sampleCampaign}
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />
    );

    // Set end date before start date
    const endDateInput = screen.getByLabelText(/Service End Date/i);
    fireEvent.change(endDateInput, { target: { value: '2026-09-01' } });

    const submitBtn = screen.getByRole('button', { name: /Save Changes/i });
    fireEvent.click(submitBtn);

    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByText('Service end date cannot be before the service start date.')).toBeTruthy();
    expect(endDateInput.classList.contains('is-invalid')).toBe(true);
  });

  it('edit mode rejects service end date equal to service start date (same-day campaign disallowed)', () => {
    const onSubmit = vi.fn();
    render(
      <CampaignForm
        initialCampaign={sampleCampaign}
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />
    );

    const endDateInput = screen.getByLabelText(/Service End Date/i);
    fireEvent.change(endDateInput, { target: { value: '2026-10-01' } });

    const submitBtn = screen.getByRole('button', { name: /Save Changes/i });
    fireEvent.click(submitBtn);

    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByText('Service end date must be after the service start date.')).toBeTruthy();
  });

  it('edit mode rejects booking close timestamp equal to booking open timestamp', () => {
    const onSubmit = vi.fn();
    render(
      <CampaignForm
        initialCampaign={sampleCampaign}
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />
    );

    const bookingCloseInput = screen.getByLabelText(/Citizen Booking Close Timestamp/i);
    // sampleCampaign bookingOpenAt is '2026-09-01T09:00:00.000Z'
    const bookingOpenInput = screen.getByLabelText(/Citizen Booking Open Timestamp/i);
    fireEvent.change(bookingCloseInput, { target: { value: bookingOpenInput.value } });

    const submitBtn = screen.getByRole('button', { name: /Save Changes/i });
    fireEvent.click(submitBtn);

    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByText('Booking close time must be after the booking open time.')).toBeTruthy();
    expect(bookingCloseInput.classList.contains('is-invalid')).toBe(true);
  });

  it('edit mode rejects booking close timestamp before booking open timestamp', () => {
    const onSubmit = vi.fn();
    render(
      <CampaignForm
        initialCampaign={sampleCampaign}
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />
    );

    const bookingCloseInput = screen.getByLabelText(/Citizen Booking Close Timestamp/i);
    fireEvent.change(bookingCloseInput, { target: { value: '2026-08-01T09:00' } });

    const submitBtn = screen.getByRole('button', { name: /Save Changes/i });
    fireEvent.click(submitBtn);

    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByText('Booking close time must be after the booking open time.')).toBeTruthy();
  });

  it('edit mode rejects booking close timestamp after service end date', () => {
    const onSubmit = vi.fn();
    render(
      <CampaignForm
        initialCampaign={sampleCampaign}
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />
    );

    // serviceEndDate is '2026-10-31', set booking close to '2026-11-01T10:00'
    const bookingCloseInput = screen.getByLabelText(/Citizen Booking Close Timestamp/i);
    fireEvent.change(bookingCloseInput, { target: { value: '2026-11-01T10:00' } });

    const submitBtn = screen.getByRole('button', { name: /Save Changes/i });
    fireEvent.click(submitBtn);

    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByText('Booking must close no later than the allowed campaign service window.')).toBeTruthy();
  });

  it('clears inline validation error as user corrects the invalid value', () => {
    render(
      <CampaignForm
        initialCampaign={sampleCampaign}
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
      />
    );

    const endDateInput = screen.getByLabelText(/Service End Date/i);
    fireEvent.change(endDateInput, { target: { value: '2026-09-01' } });

    const submitBtn = screen.getByRole('button', { name: /Save Changes/i });
    fireEvent.click(submitBtn);

    expect(screen.getByText('Service end date cannot be before the service start date.')).toBeTruthy();

    // User corrects value
    fireEvent.change(endDateInput, { target: { value: '2026-11-15' } });
    expect(screen.queryByText('Service end date cannot be before the service start date.')).toBeNull();
  });

  it('create mode flags empty required date fields with inline validation', async () => {
    const onSubmit = vi.fn();
    render(
      <CampaignForm
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/Vaccine Type/i).disabled).toBe(false);
    });

    const submitBtn = screen.getByRole('button', { name: /Save Campaign/i });
    fireEvent.click(submitBtn);

    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByText('Service start date is required.')).toBeTruthy();
    expect(screen.getByText('Service end date is required.')).toBeTruthy();
    expect(screen.getByText('Citizen booking open timestamp is required.')).toBeTruthy();
    expect(screen.getByText('Citizen booking close timestamp is required.')).toBeTruthy();
  });

  it('create mode remains functional and submits complete payload with master data', async () => {
    const onSubmit = vi.fn();
    render(
      <CampaignForm
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />
    );

    expect(screen.getByRole('heading', { level: 5 }).textContent).toContain('Create New Vaccination Campaign');

    // Wait for master data to load
    await waitFor(() => {
      expect(screen.getByLabelText(/Vaccine Type/i).disabled).toBe(false);
    });

    fireEvent.change(screen.getByLabelText(/Campaign Code/i), { target: { value: 'NEW-CMP-001' } });
    fireEvent.change(screen.getByLabelText(/Campaign Name/i), { target: { value: 'New Campaign' } });
    fireEvent.change(screen.getByLabelText(/Service Start Date/i), { target: { value: '2026-11-01' } });
    fireEvent.change(screen.getByLabelText(/Service End Date/i), { target: { value: '2026-11-30' } });
    fireEvent.change(screen.getByLabelText(/Citizen Booking Open Timestamp/i), { target: { value: '2026-10-01T08:00' } });
    fireEvent.change(screen.getByLabelText(/Citizen Booking Close Timestamp/i), { target: { value: '2026-10-31T18:00' } });

    const submitBtn = screen.getByRole('button', { name: /Save Campaign/i });
    fireEvent.click(submitBtn);

    expect(onSubmit).toHaveBeenCalledTimes(1);
    const payload = onSubmit.mock.calls[0][0];

    expect(payload.code).toBe('NEW-CMP-001');
    expect(payload.name).toBe('New Campaign');
    expect(payload.vaccineTypeId).toBe(1);
    expect(payload.targetGroupId).toBe(2);
    expect(payload.serviceStartDate).toBe('2026-11-01');
    expect(payload.serviceEndDate).toBe('2026-11-30');
    expect(payload.questionnaireVersionId).toBeUndefined();
  });

  it('create mode displays read-only unassigned questionnaire guidance', async () => {
    render(
      <CampaignForm
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
      />
    );

    expect(screen.getByText('Screening Questionnaire')).toBeTruthy();
    expect(screen.getByText('Not assigned yet')).toBeTruthy();
    expect(
      screen.getByText(/A Healthcare Officer must configure the screening questionnaire before this campaign can be scheduled/i)
    ).toBeTruthy();
    expect(screen.queryByRole('combobox', { name: /Screening Questionnaire/i })).toBeNull();
  });

  it('edit mode with unassigned campaign displays read-only unassigned guidance and omits questionnaireVersionId on submit', async () => {
    const onSubmit = vi.fn();
    render(
      <CampaignForm
        initialCampaign={sampleCampaign}
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />
    );

    expect(screen.getByText('Screening Questionnaire')).toBeTruthy();
    expect(screen.getByText('Not assigned yet')).toBeTruthy();
    expect(
      screen.getByText(/A Healthcare Officer must configure the screening questionnaire before this campaign can be scheduled/i)
    ).toBeTruthy();
    expect(screen.queryByRole('combobox', { name: /Screening Questionnaire/i })).toBeNull();

    const submitBtn = screen.getByRole('button', { name: /Save Changes/i });
    fireEvent.click(submitBtn);

    expect(onSubmit).toHaveBeenCalledTimes(1);
    const payload = onSubmit.mock.calls[0][0];
    expect(payload.questionnaireVersionId).toBeUndefined();
  });

  it('edit mode with assigned questionnaire displays published version info and HCO management guidance', async () => {
    const onSubmit = vi.fn();
    const campaignWithQ = {
      ...sampleCampaign,
      questionnaireVersionId: 101,
    };
    render(
      <CampaignForm
        initialCampaign={campaignWithQ}
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('2026 General Vaccination Screening')).toBeTruthy();
    });

    expect(screen.getByText(/Version 1 · Published/i)).toBeTruthy();
    expect(screen.getByText(/Clinical questionnaire managed by Healthcare Officer/i)).toBeTruthy();
    expect(screen.queryByRole('combobox', { name: /Screening Questionnaire/i })).toBeNull();

    const submitBtn = screen.getByRole('button', { name: /Save Changes/i });
    fireEvent.click(submitBtn);

    expect(onSubmit).toHaveBeenCalledTimes(1);
    const payload = onSubmit.mock.calls[0][0];
    expect(payload.questionnaireVersionId).toBeUndefined();
  });
});

describe('CampaignForm current-day limits', () => {
  const localInput = (iso) => {
    const date = new Date(iso);
    const pad = (value) => String(value).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  };
  const campaign = {
    id: 10, code: 'DATE-CHECK', name: 'Date check',
    serviceStartDate: '2026-10-09', serviceEndDate: '2026-10-10',
    bookingOpenAt: '2026-10-08T18:30:00Z', bookingCloseAt: '2026-10-08T19:30:00Z',
  };
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    // It is already October 9 in Sri Lanka while UTC is still October 8.
    vi.setSystemTime(new Date('2026-10-08T20:00:00Z'));
    apiMocks.listVaccineTypes.mockResolvedValue([{ id: 1 }]);
    apiMocks.listTargetGroups.mockResolvedValue([{ id: 2 }]);
  });
  afterEach(() => { cleanup(); vi.useRealTimers(); });

  it('sets picker minimums to the start of today in Sri Lanka', () => {
    render(<CampaignForm initialCampaign={campaign} onSubmit={vi.fn()} />);
    expect(screen.getByLabelText(/Service Start Date/i).min).toBe('2026-10-09');
    expect(screen.getByLabelText(/Service End Date/i).min).toBe('2026-10-09');
    expect(screen.getByLabelText(/Citizen Booking Open Timestamp/i).min).toBe(localInput(campaign.bookingOpenAt));
    expect(screen.getByLabelText(/Citizen Booking Close Timestamp/i).min).toBe(localInput(campaign.bookingOpenAt));
  });

  it.each([
    [/Service Start Date/i, '2026-10-08'],
    [/Service End Date/i, '2026-10-08'],
    [/Citizen Booking Open Timestamp/i, '2026-10-08T18:29:00Z'],
    [/Citizen Booking Close Timestamp/i, '2026-10-08T18:29:00Z'],
  ])('rejects manually entered past values for %s', (label, value) => {
    const onSubmit = vi.fn();
    render(<CampaignForm initialCampaign={campaign} onSubmit={onSubmit} />);
    const input = screen.getByLabelText(label);
    fireEvent.change(input, { target: { value: value.includes('T') ? localInput(value) : value } });
    fireEvent.click(screen.getByRole('button', { name: /Save Changes/i }));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(input.classList.contains('is-invalid')).toBe(true);
  });

  it('allows today, including booking times earlier today', () => {
    const onSubmit = vi.fn();
    render(<CampaignForm initialCampaign={campaign} onSubmit={onSubmit} />);
    fireEvent.change(screen.getByLabelText(/Citizen Booking Open Timestamp/i), {
      target: { value: localInput('2026-10-08T18:45:00Z') },
    });
    fireEvent.click(screen.getByRole('button', { name: /Save Changes/i }));
    expect(onSubmit).toHaveBeenCalledTimes(1);
  });

  it('rechecks today on submission after midnight', () => {
    const onSubmit = vi.fn();
    render(<CampaignForm onSubmit={onSubmit} />);
    fireEvent.change(screen.getByLabelText(/Service Start Date/i), { target: { value: '2026-10-09' } });
    vi.setSystemTime(new Date('2026-10-09T18:30:00Z'));
    fireEvent.submit(screen.getByLabelText(/Service Start Date/i).closest('form'));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByText('Service start date cannot be before today.')).toBeTruthy();
  });

  it('blocks creation with past dates in all four fields', async () => {
    const onSubmit = vi.fn();
    render(<CampaignForm onSubmit={onSubmit} />);
    await waitFor(() => expect(screen.getByLabelText(/Vaccine Type/i).disabled).toBe(false));
    for (const [label, value] of [
      [/Campaign Code/i, 'PAST'], [/Campaign Name/i, 'Past dates'],
      [/Service Start Date/i, '2026-10-07'], [/Service End Date/i, '2026-10-08'],
      [/Citizen Booking Open Timestamp/i, localInput('2026-10-07T08:00:00Z')],
      [/Citizen Booking Close Timestamp/i, localInput('2026-10-08T08:00:00Z')],
    ]) fireEvent.change(screen.getByLabelText(label), { target: { value } });
    fireEvent.click(screen.getByRole('button', { name: /Save Campaign/i }));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getAllByText(/cannot be before today\./)).toHaveLength(4);
  });

  it('preserves unchanged historical dates when editing other details', () => {
    const onSubmit = vi.fn();
    render(<CampaignForm initialCampaign={{ ...campaign, serviceStartDate: '2026-10-01', bookingOpenAt: '2026-09-01T00:00:59Z' }} onSubmit={onSubmit} />);
    fireEvent.change(screen.getByLabelText(/Campaign Name/i), { target: { value: 'Updated name' } });
    fireEvent.click(screen.getByRole('button', { name: /Save Changes/i }));
    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit.mock.calls[0][0].bookingOpenAt).toBe('2026-09-01T00:00:59.000Z');
  });
});
