import { describe, expect, it } from 'vitest';
import {
  findActiveAppointment,
  getNextAction,
  selectLatest,
} from './citizenDashboardState';

const readyAppointments = (data = []) => ({ loading: false, data, error: null });
const readyScreening = (overrides = {}) => ({
  loading: false,
  submissions: [],
  assessments: [],
  campaigns: [],
  errors: {},
  ...overrides,
});

describe('citizenDashboardState', () => {
  it('selects the latest dated record without relying on identifiers', () => {
    const older = { id: 99, submittedAt: '2026-08-01T09:00:00Z' };
    const newer = { id: 2, submittedAt: '2026-09-01T09:00:00Z' };
    expect(selectLatest([older, newer], ['submittedAt'])).toBe(newer);
  });

  it('recognizes only confirmed and checked-in appointments as active', () => {
    expect(findActiveAppointment([{ id: 1, status: 'COMPLETED' }, { id: 2, status: 'CONFIRMED' }])?.id).toBe(2);
    expect(findActiveAppointment([{ id: 3, status: 'CANCELLED' }])).toBeNull();
  });

  it('uses a deterministic next-action priority', () => {
    const appointment = { id: 80, status: 'CONFIRMED' };
    const assessment = { status: 'ELIGIBLE', reviewedAt: '2026-08-30T09:00:00Z' };
    const pending = { id: 40, status: 'PENDING_REVIEW', submittedAt: '2026-09-01T09:00:00Z' };
    const correction = { id: 41, status: 'CORRECTIONS_REQUIRED', submittedAt: '2026-09-02T09:00:00Z' };

    const action = getNextAction({
      appointments: readyAppointments([appointment]),
      screening: readyScreening({ submissions: [pending, correction], assessments: [assessment] }),
    });

    expect(action.title).toBe('Review your screening correction request');
    expect(action.to).toBe('/app/screening/history/41');
  });

  it('prioritizes pending review before a recorded decision and active appointment', () => {
    const action = getNextAction({
      appointments: readyAppointments([{ id: 80, status: 'CONFIRMED' }]),
      screening: readyScreening({
        submissions: [{ id: 40, status: 'PENDING_REVIEW', submittedAt: '2026-09-01T09:00:00Z' }],
        assessments: [{ status: 'ELIGIBLE', reviewedAt: '2026-08-30T09:00:00Z' }],
      }),
    });
    expect(action.title).toBe('Your screening is awaiting review');
  });

  it('uses a recorded assessment before an active appointment', () => {
    const action = getNextAction({
      appointments: readyAppointments([{ id: 80, status: 'CONFIRMED' }]),
      screening: readyScreening({ assessments: [{ status: 'ELIGIBLE', reviewedAt: '2026-08-30T09:00:00Z' }] }),
    });
    expect(action.title).toBe('Review your recorded eligibility');
    expect(action.to).toBe('/app/screening/eligibility');
  });

  it('uses an active appointment when no higher-priority screening record exists', () => {
    const action = getNextAction({
      appointments: readyAppointments([{ id: 80, status: 'CHECKED_IN' }]),
      screening: readyScreening({ submissions: [{ id: 4, status: 'REVIEWED' }] }),
    });
    expect(action.title).toBe('Review your active appointment');
    expect(action.description).toContain('Appointment #80');
  });

  it('does not infer personal state when data requests fail', () => {
    const action = getNextAction({
      appointments: { loading: false, data: [], error: new Error('offline') },
      screening: readyScreening({ errors: { submissions: new Error('offline'), assessments: new Error('offline') } }),
    });
    expect(action.title).toBe('Choose the service you need');
    expect(action.description).toContain('no personal status has been inferred');
  });
});
