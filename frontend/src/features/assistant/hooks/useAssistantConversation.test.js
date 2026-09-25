import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import {
  useAssistantConversation,
  INITIAL_ASSISTANT_MESSAGE,
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

describe('useAssistantConversation', () => {
  beforeEach(() => {
    authContextMock.current = { user: null, isAuthenticated: false };
    vi.spyOn(assistantApi, 'getAssistantStatus').mockResolvedValue({
      available: true,
      disclaimer: 'Test disclaimer',
      model: 'test-model',
    });
    vi.spyOn(assistantApi, 'sendAssistantMessage').mockResolvedValue({
      answer: 'Test answer',
      suggestedActions: [{ label: 'Book', path: '/app/appointments/book' }],
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('initializes with default greeting and requests status', async () => {
    const { result } = renderHook(() => useAssistantConversation(), { wrapper });

    expect(result.current.messages).toHaveLength(1);
    expect(result.current.messages[0].content).toBe(INITIAL_ASSISTANT_MESSAGE);
    expect(result.current.isStatusLoading).toBe(true);

    await act(async () => {
      // allow status promise to resolve
    });

    expect(result.current.isStatusLoading).toBe(false);
    expect(result.current.isAvailable).toBe(true);
    expect(result.current.disclaimer).toBe('Test disclaimer');
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
  });

  it('clears conversation automatically when authentication state changes', async () => {
    const { result, rerender } = renderHook(() => useAssistantConversation(), { wrapper });

    await act(async () => {
      await result.current.sendMessage('Anonymous question');
    });
    expect(result.current.messages.length).toBe(3);

    // Simulate login
    authContextMock.current = {
      isAuthenticated: true,
      user: { id: 42, email: 'citizen@example.com' },
    };

    act(() => {
      rerender();
    });

    expect(result.current.messages).toHaveLength(1);
    expect(result.current.messages[0].content).toBe(INITIAL_ASSISTANT_MESSAGE);
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
