import { act, cleanup, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import * as api from '../api/sessionApi';
import useSessionChangeControl from './useSessionChangeControl';

vi.mock('../api/sessionApi', () => ({
  previewSessionCancellation: vi.fn(), cancelSession: vi.fn(), previewSessionReschedule: vi.fn(),
  rescheduleSession: vi.fn(), previewSessionTransfer: vi.fn(), transferSession: vi.fn(),
  listSessionChangeEvents: vi.fn(), localToUtcIso: vi.fn(), utcToLocalDateAndTime: vi.fn(),
}));
vi.mock('../../campaign-management/api/campaignConfigurationApi', () => ({ getCenterAssignments: vi.fn() }));

function deferred() {
  let resolve;
  const promise = new Promise((done) => { resolve = done; });
  return { promise, resolve };
}
const session = (id) => ({ id, campaignId: 10, centerId: 20, lifecycleStatus: 'SCHEDULED', scheduleTimezone: 'Asia/Colombo' });
const props = (id) => ({ session: session(id), campaignsMap: {}, centersMap: {}, onSessionUpdated: vi.fn() });

describe('session change control coordination', () => {
  beforeEach(() => { vi.resetAllMocks(); api.listSessionChangeEvents.mockResolvedValue([]); });
  afterEach(cleanup);

  it('does not replace current-session history with a late previous-session response', async () => {
    const first = deferred();
    const second = deferred();
    api.listSessionChangeEvents.mockImplementation((id) => id === 1 ? first.promise : second.promise);
    const { result, rerender } = renderHook(useSessionChangeControl, { initialProps: props(1) });
    rerender(props(2));
    await act(async () => second.resolve([{ id: 200, changeType: 'RESCHEDULED' }]));
    await act(async () => first.resolve([{ id: 100, changeType: 'CANCELLED' }]));

    expect(result.current.history).toEqual([{ id: 200, changeType: 'RESCHEDULED' }]);
    expect(result.current.loadingHistory).toBe(false);
  });

  it('serializes previews and discards a previous-session result when the selected session changes', async () => {
    const preview = deferred();
    api.previewSessionCancellation.mockReturnValue(preview.promise);
    const { result, rerender } = renderHook(useSessionChangeControl, { initialProps: props(1) });
    await waitFor(() => expect(result.current.loadingHistory).toBe(false));
    act(() => result.current.handleSelectAction('CANCEL'));
    let pending;
    act(() => {
      pending = result.current.handleExecutePreview();
      void result.current.handleExecutePreview();
      result.current.handleSelectAction('RESCHEDULE');
    });
    expect(result.current.activeAction).toBe('CANCEL');
    expect(api.previewSessionCancellation).toHaveBeenCalledTimes(1);
    rerender(props(2));
    await act(async () => {
      preview.resolve({ token: 'previous-session-token' });
      await pending;
    });

    expect(result.current.previewResult).toBeNull();
    expect(result.current.activeAction).toBe('NONE');
    expect(result.current.loadingPreview).toBe(false);
    expect(api.cancelSession).not.toHaveBeenCalled();
  });
});
