import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';

const apiMocks = vi.hoisted(() => ({
  getMyAppointments: vi.fn(),
  listCitizenScreeningSubmissions: vi.fn(),
  listCitizenEligibilityAssessments: vi.fn(),
  listCampaigns: vi.fn(),
}));

vi.mock('../../appointment-management/api/appointmentApi', () => ({
  getMyAppointments: apiMocks.getMyAppointments,
}));
vi.mock('../../screening/api/screeningApi', () => ({
  listCitizenScreeningSubmissions: apiMocks.listCitizenScreeningSubmissions,
  listCitizenEligibilityAssessments: apiMocks.listCitizenEligibilityAssessments,
}));
vi.mock('../../campaign-management/api/campaignApi', () => ({
  listCampaigns: apiMocks.listCampaigns,
}));

import useCitizenDashboardData from './useCitizenDashboardData';

describe('useCitizenDashboardData', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiMocks.getMyAppointments.mockResolvedValue([]);
    apiMocks.listCitizenScreeningSubmissions.mockResolvedValue([]);
    apiMocks.listCitizenEligibilityAssessments.mockResolvedValue([]);
    apiMocks.listCampaigns.mockResolvedValue([]);
  });

  it('loads existing appointment and screening clients for the authenticated citizen', async () => {
    const { result } = renderHook(() => useCitizenDashboardData(71));
    await waitFor(() => expect(result.current.appointments.loading).toBe(false));
    await waitFor(() => expect(result.current.screening.loading).toBe(false));

    expect(apiMocks.getMyAppointments).toHaveBeenCalledExactlyOnceWith(71);
    expect(apiMocks.listCitizenScreeningSubmissions).toHaveBeenCalledExactlyOnceWith(71);
    expect(apiMocks.listCitizenEligibilityAssessments).toHaveBeenCalledExactlyOnceWith(71);
    expect(apiMocks.listCampaigns).toHaveBeenCalledExactlyOnceWith();
  });

  it('localizes an appointment failure while retaining successful screening data', async () => {
    apiMocks.getMyAppointments.mockRejectedValue(new Error('Appointments unavailable'));
    apiMocks.listCitizenScreeningSubmissions.mockResolvedValue([{ id: 4, status: 'PENDING_REVIEW' }]);

    const { result } = renderHook(() => useCitizenDashboardData(71));
    await waitFor(() => expect(result.current.screening.loading).toBe(false));
    await waitFor(() => expect(result.current.appointments.loading).toBe(false));

    expect(result.current.appointments.error?.message).toBe('Appointments unavailable');
    expect(result.current.screening.submissions).toHaveLength(1);
    expect(result.current.screening.errors).toEqual({});
  });

  it('keeps submissions when optional assessment and campaign reads fail', async () => {
    apiMocks.listCitizenScreeningSubmissions.mockResolvedValue([{ id: 4, status: 'REVIEWED' }]);
    apiMocks.listCitizenEligibilityAssessments.mockRejectedValue(new Error('Assessments unavailable'));
    apiMocks.listCampaigns.mockRejectedValue(new Error('Campaigns unavailable'));

    const { result } = renderHook(() => useCitizenDashboardData(71));
    await waitFor(() => expect(result.current.screening.loading).toBe(false));

    expect(result.current.screening.submissions).toHaveLength(1);
    expect(result.current.screening.errors.assessments?.message).toBe('Assessments unavailable');
    expect(result.current.screening.errors.campaigns?.message).toBe('Campaigns unavailable');
  });

  it('does not make API calls without a citizen identifier', async () => {
    const { result } = renderHook(() => useCitizenDashboardData(null));
    await waitFor(() => expect(result.current.appointments.loading).toBe(false));
    expect(result.current.appointments.error).toBeTruthy();
    expect(apiMocks.getMyAppointments).not.toHaveBeenCalled();
    expect(apiMocks.listCitizenScreeningSubmissions).not.toHaveBeenCalled();
  });
});
