/**
 * Assistant Conversation Hook.
 * Manages in-memory chat turns, operational status fetching, safe suggested actions,
 * and automatic conversation clearing on authentication state changes.
 */

import { useState, useEffect, useCallback, useRef } from 'react';
import { useLocation } from 'react-router-dom';
import { useAuth } from '../../../auth/context/AuthContext';
import {
  getAssistantStatus,
  sendAssistantMessage,
  isSafeInternalPath,
  MAX_MESSAGE_LENGTH,
} from '../api/assistantApi';

export const INITIAL_ASSISTANT_MESSAGE =
  'Hello. I can help you understand portal services and find the correct page.';

export const DEFAULT_DISCLAIMER =
  'AI-generated information for portal navigation. Not medical advice. For emergencies, contact local emergency services.';

export const SUGGESTED_QUESTIONS = [
  'How do I register?',
  'How does screening work?',
  'How do I book an appointment?',
  'How do I verify a certificate?',
];

function generateId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
}

export function formatErrorMessage(err) {
  if (!err) {
    return 'An unexpected error occurred. Please try again.';
  }

  const status = err.status ?? (err.cause?.status || 0);

  if (status === 400) {
    return 'The request could not be completed. Please check your message.';
  }
  if (status === 401) {
    return 'Your session has expired. Please log in again to continue.';
  }
  if (status === 429) {
    return 'You have sent several requests. Please wait and try again.';
  }
  if (status === 503) {
    return 'The portal assistant is currently unavailable.';
  }
  if (status === 0 || err.message?.toLowerCase().includes('network') || err.message?.toLowerCase().includes('unable to reach')) {
    return 'Unable to reach the server. Please check your connection and try again.';
  }

  return 'An error occurred while communicating with the assistant. Please try again.';
}

export function useAssistantConversation() {
  const { user, isAuthenticated } = useAuth() || {};
  const location = useLocation();

  // In-memory conversation state (strictly ephemeral; never persisted in browser storage)
  const [messages, setMessages] = useState(() => [
    {
      id: 'initial-greeting',
      role: 'assistant',
      content: INITIAL_ASSISTANT_MESSAGE,
      suggestedActions: [],
    },
  ]);

  const [status, setStatus] = useState(null);
  const [isStatusLoading, setIsStatusLoading] = useState(true);
  const [statusError, setStatusError] = useState(null);
  const [isSending, setIsSending] = useState(false);
  const [turnError, setTurnError] = useState(null);

  // Track authentication boundary to clear conversation on user change or login/logout
  const authIdentityRef = useRef({
    isAuthenticated: !!isAuthenticated,
    userId: user?.id || user?.userId || null,
  });

  const clearConversation = useCallback(() => {
    setMessages([
      {
        id: generateId(),
        role: 'assistant',
        content: INITIAL_ASSISTANT_MESSAGE,
        suggestedActions: [],
      },
    ]);
    setTurnError(null);
  }, []);

  // Detect authentication state changes
  useEffect(() => {
    const prevAuth = authIdentityRef.current;
    const currentUserId = user?.id || user?.userId || null;
    const currentIsAuth = !!isAuthenticated;

    if (
      prevAuth.isAuthenticated !== currentIsAuth ||
      prevAuth.userId !== currentUserId
    ) {
      authIdentityRef.current = {
        isAuthenticated: currentIsAuth,
        userId: currentUserId,
      };
      clearConversation();
    }
  }, [user, isAuthenticated, clearConversation]);

  // Fetch status on initial mount or manual retry
  const fetchStatus = useCallback(async () => {
    setIsStatusLoading(true);
    setStatusError(null);
    try {
      const result = await getAssistantStatus();
      setStatus(result);
    } catch (err) {
      setStatus({ available: false, disclaimer: DEFAULT_DISCLAIMER });
      setStatusError(formatErrorMessage(err));
    } finally {
      setIsStatusLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchStatus();
  }, [fetchStatus]);

  const sendMessage = useCallback(
    async (text) => {
      if (typeof text !== 'string' || !text.trim() || isSending) {
        return;
      }

      const trimmed = text.trim();
      if (trimmed.length > MAX_MESSAGE_LENGTH) {
        setTurnError(`Message exceeds maximum limit of ${MAX_MESSAGE_LENGTH} characters.`);
        return;
      }

      if (status && !status.available) {
        setTurnError('The portal assistant is currently unavailable.');
        return;
      }

      const userMessage = {
        id: generateId(),
        role: 'user',
        content: trimmed,
      };

      // Extract prior turns for backend history (strictly prior messages, omitting greeting and current turn)
      const priorHistory = messages
        .filter((m) => m.id !== 'initial-greeting' && (m.role === 'user' || m.role === 'assistant'))
        .map((m) => ({
          role: m.role,
          content: m.content,
        }));

      setMessages((prev) => [...prev, userMessage]);
      setIsSending(true);
      setTurnError(null);

      try {
        const response = await sendAssistantMessage({
          message: trimmed,
          history: priorHistory,
          currentPath: location.pathname || '/',
        });

        // Defensively sanitize suggested action routes
        const safeActions = Array.isArray(response?.suggestedActions)
          ? response.suggestedActions.filter(
              (act) => act && typeof act.label === 'string' && isSafeInternalPath(act.path)
            )
          : [];

        const assistantMessage = {
          id: generateId(),
          role: 'assistant',
          content: response?.answer || '',
          suggestedActions: safeActions,
        };

        setMessages((prev) => [...prev, assistantMessage]);
      } catch (err) {
        const message = formatErrorMessage(err);
        setTurnError(message);
      } finally {
        setIsSending(false);
      }
    },
    [isSending, status, messages, location.pathname]
  );

  return {
    messages,
    status,
    isStatusLoading,
    statusError,
    isSending,
    turnError,
    sendMessage,
    clearConversation,
    retryStatus: fetchStatus,
    isAvailable: status?.available ?? false,
    disclaimer: status?.disclaimer || DEFAULT_DISCLAIMER,
  };
}
