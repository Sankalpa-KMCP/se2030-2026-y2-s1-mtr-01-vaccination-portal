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
  MAX_HISTORY_LENGTH,
} from '../api/assistantApi';

export const INITIAL_ASSISTANT_MESSAGE =
  'Hello. I can help you understand portal services and find the correct page.';

export const DEFAULT_DISCLAIMER =
  'This assistant explains portal services. It does not provide medical advice or emergency care.';

export const SUGGESTED_QUESTIONS = [
  'How do I register?',
  'How does screening work?',
  'How do I book an appointment?',
  'How do I verify a certificate?',
];

function generateId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
}

export function createInitialGreeting() {
  return {
    id: generateId(),
    role: 'assistant',
    content: INITIAL_ASSISTANT_MESSAGE,
    suggestedActions: [],
    isGreeting: true,
  };
}

export function formatErrorMessage(err) {
  if (!err) {
    return 'An unexpected error occurred. Please try again.';
  }

  const status = err.status ?? (err.cause?.status || 0);
  const errorCode = err.data?.code || err.cause?.data?.code || err.code;

  if (status === 503 || errorCode === 'ASSISTANT_DISABLED') {
    return 'The portal assistant is currently unavailable.';
  }
  if (status === 400) {
    return 'The request could not be completed. Please check your message.';
  }
  if (status === 401) {
    return 'Your session has expired. Please log in again to continue.';
  }
  if (status === 429) {
    return 'You have sent several requests. Please wait and try again.';
  }
  if (
    status === 0 ||
    err.message?.toLowerCase().includes('network') ||
    err.message?.toLowerCase().includes('unable to reach')
  ) {
    return 'Unable to reach the server. Please check your connection and try again.';
  }

  return 'An error occurred while communicating with the assistant. Please try again.';
}

export function useAssistantConversation() {
  const { user, isAuthenticated } = useAuth() || {};
  const location = useLocation();

  // In-memory conversation state (strictly ephemeral; never persisted in browser storage)
  const [messages, setMessages] = useState(() => [createInitialGreeting()]);

  const [status, setStatus] = useState(null);
  const [isStatusLoading, setIsStatusLoading] = useState(true);
  const [statusError, setStatusError] = useState(null);
  const [isSending, setIsSending] = useState(false);
  const [turnError, setTurnError] = useState(null);

  // Generation counters to prevent stale responses crossing privacy boundaries
  const conversationGenerationRef = useRef(0);
  const statusGenerationRef = useRef(0);

  // Track authentication boundary to clear conversation on user change or login/logout
  const authIdentityRef = useRef({
    isAuthenticated: !!isAuthenticated,
    userId: user?.id || user?.userId || null,
  });

  const invalidateConversation = useCallback(() => {
    conversationGenerationRef.current += 1;
    setIsSending(false);
    setTurnError(null);
    return conversationGenerationRef.current;
  }, []);

  const clearConversation = useCallback(() => {
    invalidateConversation();
    setMessages([createInitialGreeting()]);
  }, [invalidateConversation]);

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
      invalidateConversation();
      setMessages([createInitialGreeting()]);
    }
  }, [user, isAuthenticated, invalidateConversation]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      conversationGenerationRef.current += 1;
      statusGenerationRef.current += 1;
    };
  }, []);

  // Fetch status on initial mount or manual retry
  const fetchStatus = useCallback(async () => {
    const currentGen = ++statusGenerationRef.current;
    setIsStatusLoading(true);
    setStatusError(null);
    try {
      const result = await getAssistantStatus();
      if (currentGen === statusGenerationRef.current) {
        setStatus(result);
      }
    } catch (err) {
      if (currentGen === statusGenerationRef.current) {
        setStatus({ available: false, disclaimer: DEFAULT_DISCLAIMER });
        setStatusError(formatErrorMessage(err));
      }
    } finally {
      if (currentGen === statusGenerationRef.current) {
        setIsStatusLoading(false);
      }
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

      const currentGeneration = conversationGenerationRef.current;

      const userMessage = {
        id: generateId(),
        role: 'user',
        content: trimmed,
      };

      // Extract prior genuine turns for backend history (omitting greeting and current turn, capped at MAX_HISTORY_LENGTH)
      const priorHistory = messages
        .filter((m) => !m.isGreeting && (m.role === 'user' || m.role === 'assistant'))
        .slice(-MAX_HISTORY_LENGTH)
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

        // Ignore stale response if conversation generation changed during in-flight request
        if (currentGeneration !== conversationGenerationRef.current) {
          return;
        }

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
        // Ignore stale error if conversation generation changed
        if (currentGeneration !== conversationGenerationRef.current) {
          return;
        }

        const httpStatus = err.status ?? (err.cause?.status || 0);
        const errorCode = err.data?.code;
        if (httpStatus === 503 || errorCode === 'ASSISTANT_DISABLED') {
          setStatus((prev) => ({
            ...(prev || {}),
            available: false,
            disclaimer: prev?.disclaimer || DEFAULT_DISCLAIMER,
          }));
        }

        const message = formatErrorMessage(err);
        setTurnError(message);
      } finally {
        if (currentGeneration === conversationGenerationRef.current) {
          setIsSending(false);
        }
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
    personalizedFeaturesAvailable: status?.personalizedFeaturesAvailable ?? false,
    disclaimer: status?.disclaimer || DEFAULT_DISCLAIMER,
  };
}
