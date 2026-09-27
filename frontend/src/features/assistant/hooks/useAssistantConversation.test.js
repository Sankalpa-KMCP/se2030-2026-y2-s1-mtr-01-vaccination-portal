import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import {
  useAssistantConversation,
  INITIAL_ASSISTANT_MESSAGE,
  DEFAULT_DISCLAIMER,
  STAFF_SUGGESTED_QUESTIONS,
  formatErrorMessage,
} from './useAssistantConversation';
import * as assistantApi from '../api/assistantApi';

const authContextMock = vi.hoisted(() => ({
  current: { user: null, isAuthenticated: false },
}));

vi.mock('../../../auth/context/AuthContext', () => ({
  useAuth: () => authContextMock.current,
}));

function wrapper({ children }) {
  return React.createElement(MemoryRouter, { initialEntries: ['/how-it-works'] }, children);
}

function createDeferred() {
  let resolve;
  let reject;
  const promise = new Promise((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

describe('useAssistantConversation', () => {
  beforeEach(() => {
    authContextMock.current = { user: null, isAuthenticated: false };
    vi.spyOn(assistantApi, 'getAssistantStatus').mockResolvedValue({
      available: true,
      personalizedFeaturesAvailable: false,
      disclaimer: DEFAULT_DISCLAIMER,
    });
    vi.spyOn(assistantApi, 'sendAssistantMessage').mockResolvedValue({
      answer: 'Test answer',
      suggestedActions: [{ label: 'Book', path: '/app/appointments/book' }],
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('initializes with default greeting and neutral fallback disclaimer', async () => {
    expect(DEFAULT_DISCLAIMER).toBe(
      'This assistant explains portal services. It does not provide medical advice or emergency care.'
    );

    const { result } = renderHook(() => useAssistantConversation(), { wrapper });

    expect(result.current.messages).toHaveLength(1);
    expect(result.current.messages[0].content).toBe(INITIAL_ASSISTANT_MESSAGE);
    expect(result.current.messages[0].isGreeting).toBe(true);
    expect(result.current.isStatusLoading).toBe(true);

    await act(async () => {
      // allow status promise to resolve
    });

    expect(result.current.isStatusLoading).toBe(false);
    expect(result.current.isAvailable).toBe(true);
    expect(result.current.disclaimer).toBe(DEFAULT_DISCLAIMER);
  });

  it('shows staff onboarding questions to an authenticated staff member', async () => {
    authContextMock.current = {
      user: {
        id: 45,
        email: 'staff45@example.com',
        roles: ['ROLE_CENTER_MANAGER'],
      },
      isAuthenticated: true,
    };

    const { result } = renderHook(() => useAssistantConversation(), { wrapper });

    await act(async () => {
      // allow status promise to resolve
    });

    expect(result.current.suggestedQuestions).toEqual(STAFF_SUGGESTED_QUESTIONS);
    expect(result.current.suggestedQuestions).toContain('What is my staff profile?');
    expect(result.current.suggestedQuestions).toContain('Which center am I assigned to?');
  });

  it('submits a message, appends user turn and assistant reply', async () => {
    const { result } = renderHook(() => useAssistantConversation(), { wrapper });

    await act(async () => {
      await result.current.sendMessage('How do I register?');
    });

    expect(result.current.messages).toHaveLength(3);
    expect(result.current.messages[1].role).toBe('user');
    expect(result.current.messages[1].content).toBe('How do I register?');
    expect(result.current.messages[2].role).toBe('assistant');
    expect(result.current.messages[2].content).toBe('Test answer');
    expect(result.current.messages[2].suggestedActions).toEqual([
      { label: 'Book', path: '/app/appointments/book' },
    ]);
  });

  it('filters out unsafe suggested actions from the assistant reply', async () => {
    vi.spyOn(assistantApi, 'sendAssistantMessage').mockResolvedValueOnce({
      answer: 'Malicious link attempt',
      suggestedActions: [
        { label: 'Safe', path: '/app/citizen' },
        { label: 'Evil External', path: 'https://evil.com' },
        { label: 'Evil Relative', path: '//evil.com' },
        { label: 'Evil Script', path: 'javascript:alert(1)' },
      ],
    });

    const { result } = renderHook(() => useAssistantConversation(), { wrapper });

    await act(async () => {
      await result.current.sendMessage('Test unsafe link');
    });

    const lastMessage = result.current.messages[result.current.messages.length - 1];
    expect(lastMessage.suggestedActions).toEqual([{ label: 'Safe', path: '/app/citizen' }]);
  });

  it('clears conversation when clearConversation() is called', async () => {
    const { result } = renderHook(() => useAssistantConversation(), { wrapper });

    await act(async () => {
      await result.current.sendMessage('Hello');
    });
    expect(result.current.messages.length).toBeGreaterThan(1);

    act(() => {
      result.current.clearConversation();
    });

    expect(result.current.messages).toHaveLength(1);
    expect(result.current.messages[0].content).toBe(INITIAL_ASSISTANT_MESSAGE);
    expect(result.current.messages[0].isGreeting).toBe(true);
  });

  describe('stale response protection (Correction 1)', () => {
    it('discards in-flight response when user logs in before resolution', async () => {
      const deferred = createDeferred();
      assistantApi.sendAssistantMessage.mockReturnValueOnce(deferred.promise);

      const { result, rerender } = renderHook(() => useAssistantConversation(), { wrapper });

      let sendPromise;
      act(() => {
        sendPromise = result.current.sendMessage('Anonymous sensitive question');
      });

      expect(result.current.isSending).toBe(true);

      // User logs in while request is still pending
      authContextMock.current = {
        isAuthenticated: true,
        user: { id: 101, email: 'citizen@example.com' },
      };
      act(() => {
        rerender();
      });

      // Conversation must have reset
      expect(result.current.messages).toHaveLength(1);
      expect(result.current.messages[0].isGreeting).toBe(true);
      expect(result.current.isSending).toBe(false);

      // Now resolve the old in-flight request
      await act(async () => {
        deferred.resolve({ answer: 'Stale anonymous answer', suggestedActions: [] });
        await sendPromise;
      });

      // Stale response must NOT appear in the new authenticated session
      expect(result.current.messages).toHaveLength(1);
      expect(result.current.messages[0].isGreeting).toBe(true);
      expect(result.current.messages[0].content).toBe(INITIAL_ASSISTANT_MESSAGE);
    });

    it('discards in-flight response when user logs out before resolution', async () => {
      authContextMock.current = {
        isAuthenticated: true,
        user: { id: 202, email: 'staff@example.com' },
      };

      const deferred = createDeferred();
      assistantApi.sendAssistantMessage.mockReturnValueOnce(deferred.promise);

      const { result, rerender } = renderHook(() => useAssistantConversation(), { wrapper });

      let sendPromise;
      act(() => {
        sendPromise = result.current.sendMessage('Staff query');
      });

      expect(result.current.isSending).toBe(true);

      // User logs out while request is still pending
      authContextMock.current = {
        isAuthenticated: false,
        user: null,
      };
      act(() => {
        rerender();
      });

      expect(result.current.messages).toHaveLength(1);
      expect(result.current.isSending).toBe(false);

      // Now resolve the old staff request
      await act(async () => {
        deferred.resolve({ answer: 'Confidential staff data answer', suggestedActions: [] });
        await sendPromise;
      });

      // Stale staff response must NOT appear in anonymous session
      expect(result.current.messages).toHaveLength(1);
      expect(result.current.messages[0].isGreeting).toBe(true);
    });

    it('discards in-flight response when switching user IDs before resolution', async () => {
      authContextMock.current = {
        isAuthenticated: true,
        user: { id: 111, email: 'user1@example.com' },
      };

      const deferred = createDeferred();
      assistantApi.sendAssistantMessage.mockReturnValueOnce(deferred.promise);

      const { result, rerender } = renderHook(() => useAssistantConversation(), { wrapper });

      let sendPromise;
      act(() => {
        sendPromise = result.current.sendMessage('User 1 query');
      });

      // Switch to User 2
      authContextMock.current = {
        isAuthenticated: true,
        user: { id: 222, email: 'user2@example.com' },
      };
      act(() => {
        rerender();
      });

      expect(result.current.messages).toHaveLength(1);
      expect(result.current.isSending).toBe(false);

      // Resolve old request
      await act(async () => {
        deferred.resolve({ answer: 'User 1 private reply', suggestedActions: [] });
        await sendPromise;
      });

      expect(result.current.messages).toHaveLength(1);
      expect(result.current.messages[0].content).toBe(INITIAL_ASSISTANT_MESSAGE);
    });

    it('discards in-flight response when Clear conversation is triggered before resolution', async () => {
      const deferred = createDeferred();
      assistantApi.sendAssistantMessage.mockReturnValueOnce(deferred.promise);

      const { result } = renderHook(() => useAssistantConversation(), { wrapper });

      let sendPromise;
      act(() => {
        sendPromise = result.current.sendMessage('Slow query');
      });

      expect(result.current.isSending).toBe(true);

      // Clear conversation while request is pending
      act(() => {
        result.current.clearConversation();
      });

      expect(result.current.messages).toHaveLength(1);
      expect(result.current.isSending).toBe(false);

      // Resolve deferred response
      await act(async () => {
        deferred.resolve({ answer: 'Slow answer arrived late', suggestedActions: [] });
        await sendPromise;
      });

      expect(result.current.messages).toHaveLength(1);
      expect(result.current.messages[0].content).toBe(INITIAL_ASSISTANT_MESSAGE);
    });
  });

  describe('greeting exclusion from history (Correction 2)', () => {
    it('sends empty history for first user message after initial greeting', async () => {
      const { result } = renderHook(() => useAssistantConversation(), { wrapper });

      await act(async () => {
        await result.current.sendMessage('First turn');
      });

      expect(assistantApi.sendAssistantMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          message: 'First turn',
          history: [], // greeting must NOT be in history
        })
      );
    });

    it('sends empty history on the first message after Clear conversation', async () => {
      const { result } = renderHook(() => useAssistantConversation(), { wrapper });

      await act(async () => {
        await result.current.sendMessage('Turn 1');
      });
      act(() => {
        result.current.clearConversation();
      });

      await act(async () => {
        await result.current.sendMessage('Turn after clear');
      });

      expect(assistantApi.sendAssistantMessage).toHaveBeenLastCalledWith(
        expect.objectContaining({
          message: 'Turn after clear',
          history: [],
        })
      );
    });

    it('sends empty history on the first message after auth identity change reset', async () => {
      const { result, rerender } = renderHook(() => useAssistantConversation(), { wrapper });

      await act(async () => {
        await result.current.sendMessage('Anon turn');
      });

      // Login reset
      authContextMock.current = {
        isAuthenticated: true,
        user: { id: 55, email: 'logged@example.com' },
      };
      act(() => {
        rerender();
      });

      await act(async () => {
        await result.current.sendMessage('Auth turn');
      });

      expect(assistantApi.sendAssistantMessage).toHaveBeenLastCalledWith(
        expect.objectContaining({
          message: 'Auth turn',
          history: [],
        })
      );
    });

    it('history contains genuine prior turns and caps at 10 items', async () => {
      const { result } = renderHook(() => useAssistantConversation(), { wrapper });

      // Send 6 conversation pairs (12 messages)
      for (let i = 1; i <= 6; i++) {
        assistantApi.sendAssistantMessage.mockResolvedValueOnce({
          answer: `Assistant answer ${i}`,
          suggestedActions: [],
        });
        await act(async () => {
          await result.current.sendMessage(`User question ${i}`);
        });
      }

      // 7th turn: history should contain prior turns capped at 10
      await act(async () => {
        await result.current.sendMessage('User question 7');
      });

      const lastCall = assistantApi.sendAssistantMessage.mock.calls.at(-1)[0];
      expect(lastCall.message).toBe('User question 7');
      // History should not contain question 7
      expect(lastCall.history.every((h) => h.content !== 'User question 7')).toBe(true);
      // History should not contain greeting
      expect(lastCall.history.every((h) => h.content !== INITIAL_ASSISTANT_MESSAGE)).toBe(true);
      // Maximum 10 items
      expect(lastCall.history.length).toBeLessThanOrEqual(10);
    });
  });

  describe('disabled state on 503 failure (Correction 6)', () => {
    it('marks assistant unavailable and disables submissions when message returns 503', async () => {
      assistantApi.sendAssistantMessage.mockRejectedValueOnce({
        status: 503,
        message: 'Service Unavailable',
      });

      const { result } = renderHook(() => useAssistantConversation(), { wrapper });

      await act(async () => {
        // wait for initial status
      });
      expect(result.current.isAvailable).toBe(true);

      await act(async () => {
        await result.current.sendMessage('Trigger 503');
      });

      expect(result.current.isAvailable).toBe(false);
      expect(result.current.turnError).toContain('currently unavailable');
    });

    it('marks assistant unavailable when message returns ASSISTANT_DISABLED code', async () => {
      assistantApi.sendAssistantMessage.mockRejectedValueOnce({
        status: 400,
        data: { code: 'ASSISTANT_DISABLED', message: 'Disabled' },
      });

      const { result } = renderHook(() => useAssistantConversation(), { wrapper });

      await act(async () => {
        // wait for initial status
      });

      await act(async () => {
        await result.current.sendMessage('Trigger ASSISTANT_DISABLED');
      });

      expect(result.current.isAvailable).toBe(false);
      expect(result.current.turnError).toContain('currently unavailable');
    });
  });

  it('handles 429, 503, and network errors safely', () => {
    expect(formatErrorMessage({ status: 429 })).toContain('wait and try again');
    expect(formatErrorMessage({ status: 503 })).toContain('currently unavailable');
    expect(formatErrorMessage({ status: 400 })).toContain('check your message');
    expect(formatErrorMessage({ status: 401 })).toContain('session has expired');
    expect(formatErrorMessage({ status: 0, message: 'network error' })).toContain('Unable to reach the server');
  });

  it('does not write any conversation state to localStorage or sessionStorage', async () => {
    const localSpy = vi.spyOn(Storage.prototype, 'setItem');
    const { result } = renderHook(() => useAssistantConversation(), { wrapper });

    await act(async () => {
      await result.current.sendMessage('Sensitive message query');
    });

    expect(localSpy).not.toHaveBeenCalledWith(expect.stringContaining('assistant'), expect.anything());
    expect(localSpy).not.toHaveBeenCalledWith(expect.stringContaining('conversation'), expect.anything());
  });
});
