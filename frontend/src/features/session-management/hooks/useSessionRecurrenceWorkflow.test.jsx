import { act, cleanup, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import * as api from '../api/sessionApi';
import useSessionRecurrenceWorkflow from './useSessionRecurrenceWorkflow';

vi.mock('../api/sessionApi', () => ({
  listRecurrenceRules: vi.fn(), getRecurrenceRuleById: vi.fn(), updateRecurrenceRule: vi.fn(),
  generateRecurrenceOccurrences: vi.fn(), previewSeriesChange: vi.fn(), confirmSeriesChange: vi.fn(),
  previewSeriesCancellation: vi.fn(), confirmSeriesCancellation: vi.fn(),
  isValidIanaTimezone: vi.fn(), formatLocalTimeForDto: vi.fn(),
}));

function deferred() {
  let resolve;
  const promise = new Promise((done) => { resolve = done; });
  return { promise, resolve };
}

const rule = (id) => ({ id, weekdays: ['MONDAY'], startDate: '2026-10-01',
  endDate: '2026-10-31', startTime: '09:00:15', endTime: '17:00:30' });
const props = () => ({ centersMap: {}, recurrenceOperationOwnerRef: { current: null },
  recurrenceRefreshGen: 0, onRecurrenceDataChanged: vi.fn(), onSessionDataChanged: vi.fn() });

describe('recurrence workflow coordination', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    api.listRecurrenceRules.mockResolvedValue([]);
    api.getRecurrenceRuleById.mockImplementation((id) => Promise.resolve(rule(id)));
  });
  afterEach(cleanup);

  it('ignores a late detail response for a previously selected rule and preserves schedule seconds', async () => {
    const first = deferred();
    const second = deferred();
    api.getRecurrenceRuleById.mockImplementation((id) => id === 1 ? first.promise : second.promise);
    const { result } = renderHook(() => useSessionRecurrenceWorkflow(props()));
    act(() => result.current.handleSelectRule(1));
    act(() => result.current.handleSelectRule(2));
    await act(async () => second.resolve(rule(2)));
    await act(async () => first.resolve(rule(1)));

    expect(result.current.selectedRule.id).toBe(2);
    expect(result.current.updateStartTime).toBe('09:00:15');
    expect(result.current.changeEndTime).toBe('17:00:30');
    expect(api.getRecurrenceRuleById).toHaveBeenCalledTimes(2);
  });

  it('serializes mutations and reconciles the parent without displaying a stale generation result', async () => {
    const generation = deferred();
    api.generateRecurrenceOccurrences.mockReturnValue(generation.promise);
    const options = props();
    const { result } = renderHook(() => useSessionRecurrenceWorkflow(options));
    act(() => result.current.handleSelectRule(1));
    await waitFor(() => expect(result.current.selectedRule?.id).toBe(1));
    let pending;
    act(() => {
      pending = result.current.handleGenerateOccurrences();
      void result.current.handleGenerateOccurrences();
    });
    expect(api.generateRecurrenceOccurrences).toHaveBeenCalledTimes(1);
    act(() => result.current.handleSelectRule(2));
    await act(async () => {
      generation.resolve({ createdCount: 2, existingCount: 1 });
      await pending;
    });

    expect(options.onRecurrenceDataChanged).toHaveBeenCalledTimes(1);
    expect(options.onSessionDataChanged).toHaveBeenCalledTimes(1);
    expect(result.current.genResult).toBeNull();
    expect(result.current.panelSuccessMsg).toBeNull();
    expect(options.recurrenceOperationOwnerRef.current).toBeNull();
  });

  it('rejects a partial generation window before making a mutation request', async () => {
    const { result } = renderHook(() => useSessionRecurrenceWorkflow(props()));
    act(() => result.current.handleSelectRule(1));
    await waitFor(() => expect(result.current.selectedRule?.id).toBe(1));
    act(() => result.current.setGenStartDate('2026-10-02'));
    await act(async () => result.current.handleGenerateOccurrences());

    expect(result.current.genError.message).toBe('Generation window requires both a start date and an end date, or leave both empty for the full rule horizon.');
    expect(api.generateRecurrenceOccurrences).not.toHaveBeenCalled();
  });
});
