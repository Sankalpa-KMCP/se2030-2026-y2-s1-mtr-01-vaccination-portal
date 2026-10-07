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
  listCitizenCampaigns: apiMocks.listCampaigns,
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

  it('treats appointments response null as a localized error rather than confirmed empty', async () => {
    apiMocks.getMyAppointments.mockResolvedValue(null);
    apiMocks.listCitizenScreeningSubmissions.mockResolvedValue([{ id: 5, status: 'PENDING_REVIEW' }]);

    const { result } = renderHook(() => useCitizenDashboardData(71));
    await waitFor(() => expect(result.current.appointments.loading).toBe(false));
    await waitFor(() => expect(result.current.screening.loading).toBe(false));

    expect(result.current.appointments.error?.message).toMatch(/malformed/i);
    expect(result.current.appointments.data).toEqual([]);
    expect(result.current.screening.submissions).toHaveLength(1);
  });

  it('treats appointments response object as a localized error rather than confirmed empty', async () => {
    apiMocks.getMyAppointments.mockResolvedValue({ unexpected: 'object' });

    const { result } = renderHook(() => useCitizenDashboardData(71));
    await waitFor(() => expect(result.current.appointments.loading).toBe(false));

    expect(result.current.appointments.error?.message).toMatch(/malformed/i);
    expect(result.current.appointments.data).toEqual([]);
  });

  it('treats screening submissions response null as a localized error rather than confirmed empty', async () => {
    apiMocks.getMyAppointments.mockResolvedValue([{ id: 80, status: 'CONFIRMED' }]);
    apiMocks.listCitizenScreeningSubmissions.mockResolvedValue(null);

    const { result } = renderHook(() => useCitizenDashboardData(71));
    await waitFor(() => expect(result.current.screening.loading).toBe(false));
    await waitFor(() => expect(result.current.appointments.loading).toBe(false));

    expect(result.current.screening.errors.submissions?.message).toMatch(/malformed/i);
    expect(result.current.screening.submissions).toEqual([]);
    expect(result.current.appointments.data).toHaveLength(1);
  });

  it('treats screening assessments malformed payload as a localized error', async () => {
    apiMocks.listCitizenEligibilityAssessments.mockResolvedValue({ notAnArray: true });
    apiMocks.listCitizenScreeningSubmissions.mockResolvedValue([{ id: 6, status: 'REVIEWED' }]);

    const { result } = renderHook(() => useCitizenDashboardData(71));
    await waitFor(() => expect(result.current.screening.loading).toBe(false));

    expect(result.current.screening.errors.assessments?.message).toMatch(/malformed/i);
    expect(result.current.screening.assessments).toEqual([]);
    expect(result.current.screening.submissions).toHaveLength(1);
  });

  it('does not block core screening loading when the optional campaign request remains unresolved', async () => {
    apiMocks.listCampaigns.mockReturnValue(new Promise(() => {}));
    apiMocks.listCitizenScreeningSubmissions.mockResolvedValue([{ id: 12, campaignId: 44, status: 'PENDING_REVIEW' }]);
    apiMocks.listCitizenEligibilityAssessments.mockResolvedValue([]);

    const { result } = renderHook(() => useCitizenDashboardData(71));
    await waitFor(() => expect(result.current.screening.loading).toBe(false));

    expect(result.current.screening.submissions).toHaveLength(1);
    expect(result.current.screening.submissions[0].id).toBe(12);
    expect(result.current.screening.campaigns).toEqual([]);
  });
});
