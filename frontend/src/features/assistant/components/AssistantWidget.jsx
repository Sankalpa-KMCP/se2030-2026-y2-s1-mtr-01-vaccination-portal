/**
 * AssistantWidget Component.
 * Accessible, responsive floating chat assistant widget.
 * Available across public and authenticated vaccination portal pages.
 */

import React, { useState, useRef, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  useAssistantConversation,
  SUGGESTED_QUESTIONS,
} from '../hooks/useAssistantConversation';
import { isSafeInternalPath, MAX_MESSAGE_LENGTH } from '../api/assistantApi';
import '../styles/assistant.css';

export default function AssistantWidget() {
  const [isOpen, setIsOpen] = useState(false);
  const [inputVal, setInputVal] = useState('');

  const navigate = useNavigate();

  const {
    messages,
    isStatusLoading,
    statusError,
    isSending,
    turnError,
    sendMessage,
    clearConversation,
    retryStatus,
    isAvailable,
    disclaimer,
  } = useAssistantConversation();

  const launcherRef = useRef(null);
  const panelRef = useRef(null);
  const inputRef = useRef(null);
  const messagesEndRef = useRef(null);

  // Auto-scroll messages list when new messages arrive
  useEffect(() => {
    if (isOpen) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, isSending, isOpen]);

  // Handle focus when panel opens / closes
  useEffect(() => {
    if (isOpen) {
      inputRef.current?.focus();
    }
  }, [isOpen]);

  // Support Escape key to close the assistant dialog
  const handleKeyDown = useCallback(
    (e) => {
      if (e.key === 'Escape' && isOpen) {
        e.preventDefault();
        setIsOpen(false);
        launcherRef.current?.focus();
      }
    },
    [isOpen]
  );

  useEffect(() => {
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  const handleToggleOpen = () => {
    if (isOpen) {
      setIsOpen(false);
      launcherRef.current?.focus();
    } else {
      setIsOpen(true);
    }
  };

  const handleClose = () => {
    setIsOpen(false);
    launcherRef.current?.focus();
  };

  const handleSubmit = (e) => {
    e?.preventDefault();
    if (!inputVal.trim() || isSending || !isAvailable) {
      return;
    }
    const textToSend = inputVal;
    setInputVal('');
    sendMessage(textToSend);
  };

  const handleTextareaKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  const handleActionClick = (path) => {
    if (isSafeInternalPath(path)) {
      navigate(path);
      // Close panel after navigation for improved mobile experience
      setIsOpen(false);
      launcherRef.current?.focus();
    }
  };

  const handleQuestionClick = (question) => {
    if (!isSending && isAvailable) {
      sendMessage(question);
    }
  };

  const isOverLimit = inputVal.length > MAX_MESSAGE_LENGTH;
  const canSend = inputVal.trim().length > 0 && !isOverLimit && !isSending && isAvailable;

  return (
    <div className="assistant-widget">
      {/* Floating Launcher Button */}
      <button
        ref={launcherRef}
        type="button"
        className="assistant-launcher-btn"
        aria-label={isOpen ? 'Close portal assistant' : 'Open portal assistant'}
        aria-expanded={isOpen}
        aria-controls="portal-assistant-panel"
        onClick={handleToggleOpen}
      >
        <i
          className={isOpen ? 'bi bi-x-lg' : 'bi bi-chat-dots-fill'}
          aria-hidden="true"
        />
      </button>

      {/* Floating Dialog Panel */}
      {isOpen && (
        <section
          ref={panelRef}
          id="portal-assistant-panel"
          className="assistant-panel"
          role="dialog"
          aria-label="Portal Assistant"
        >
          {/* Header */}
          <header className="assistant-header">
            <h2 className="assistant-header-title">
              <span
                className={`assistant-status-dot ${isAvailable ? '' : 'unavailable'}`}
                aria-hidden="true"
              />
              Portal Assistant
            </h2>
            <div className="assistant-header-actions">
              <button
                type="button"
                className="assistant-icon-btn"
                aria-label="Clear conversation"
                title="Clear conversation"
                onClick={clearConversation}
              >
                <i className="bi bi-trash3" aria-hidden="true" />
              </button>
              <button
                type="button"
                className="assistant-icon-btn"
                aria-label="Close assistant"
                title="Close assistant"
                onClick={handleClose}
              >
                <i className="bi bi-x-lg" aria-hidden="true" />
              </button>
            </div>
          </header>

          {/* System Disclaimer */}
          <div className="assistant-disclaimer-banner" role="note">
            <i className="bi bi-info-circle-fill" aria-hidden="true" />
            <span>{disclaimer}</span>
          </div>

          {/* Availability / Status Alert */}
          {!isStatusLoading && !isAvailable && (
            <div className="assistant-unavailable-alert" role="alert">
              <span>The portal assistant is currently unavailable.</span>
              <button
                type="button"
                className="assistant-retry-btn"
                onClick={retryStatus}
              >
                Retry
              </button>
            </div>
          )}

          {/* Status Loading Indicator */}
          {isStatusLoading && (
            <div className="assistant-unavailable-alert" role="status">
              <span>Checking assistant availability...</span>
            </div>
          )}

          {/* Messages Area */}
          <div className="assistant-messages-container" tabIndex={0} role="region" aria-label="Conversation messages">
            {messages.map((msg, index) => (
              <div
                key={msg.id || index}
                className={`assistant-message-row ${msg.role}`}
              >
                <div className="assistant-message-bubble">
                  {msg.content}
                </div>

                {/* Suggested Navigation Actions */}
                {Array.isArray(msg.suggestedActions) && msg.suggestedActions.length > 0 && (
                  <div className="assistant-actions-container" aria-label="Suggested navigation">
                    {msg.suggestedActions.map((action, actIdx) => (
                      <button
                        key={`${msg.id}-action-${actIdx}`}
                        type="button"
                        className="assistant-action-chip"
                        onClick={() => handleActionClick(action.path)}
                      >
                        <i className="bi bi-arrow-right-short" aria-hidden="true" />
                        {action.label}
                      </button>
                    ))}
                  </div>
                )}

                {/* Initial suggested questions chips under initial greeting */}
                {msg.id === 'initial-greeting' && (
                  <div className="assistant-suggested-questions">
                    {SUGGESTED_QUESTIONS.map((q, qIdx) => (
                      <button
                        key={`sq-${qIdx}`}
                        type="button"
                        className="assistant-question-chip"
                        disabled={!isAvailable || isSending}
                        onClick={() => handleQuestionClick(q)}
                      >
                        <i className="bi bi-question-circle me-1" aria-hidden="true" />
                        {q}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            ))}

            {/* Sending indicator */}
            {isSending && (
              <div className="assistant-typing-indicator" aria-label="Assistant is thinking">
                <span className="assistant-typing-dot" />
                <span className="assistant-typing-dot" />
                <span className="assistant-typing-dot" />
              </div>
            )}

            <div ref={messagesEndRef} aria-hidden="true" />
          </div>

          {/* Dynamic polite live region for accessibility */}
          <div className="visually-hidden" aria-live="polite" aria-atomic="true">
            {turnError && `Error: ${turnError}`}
            {statusError && `Status error: ${statusError}`}
            {messages[messages.length - 1]?.role === 'assistant' &&
              messages[messages.length - 1]?.content}
          </div>

          {/* Turn Error Banner */}
          {turnError && (
            <div className="assistant-error-banner" role="alert">
              <i className="bi bi-exclamation-triangle-fill" aria-hidden="true" />
              <span>{turnError}</span>
            </div>
          )}

          {/* Input Footer */}
          <footer className="assistant-footer">
            <form onSubmit={handleSubmit}>
              <div className="assistant-input-row">
                <textarea
                  ref={inputRef}
                  id="assistant-user-input"
                  className="assistant-input-textarea"
                  rows={1}
                  placeholder={isAvailable ? 'Ask about portal workflows...' : 'Assistant is currently unavailable'}
                  value={inputVal}
                  onChange={(e) => setInputVal(e.target.value)}
                  onKeyDown={handleTextareaKeyDown}
                  disabled={!isAvailable || isSending}
                  aria-label="Ask the portal assistant"
                  maxLength={MAX_MESSAGE_LENGTH + 50}
                />
                <button
                  type="submit"
                  className="assistant-send-btn"
                  disabled={!canSend}
                  aria-label="Send message"
                  title="Send message"
                >
                  <i className="bi bi-send-fill" aria-hidden="true" />
                </button>
              </div>

              <div className="assistant-input-meta">
                <button
                  type="button"
                  className="assistant-clear-btn"
                  onClick={clearConversation}
                >
                  Clear conversation
                </button>
                <span className={isOverLimit ? 'text-danger fw-semibold' : ''}>
                  {inputVal.length}/{MAX_MESSAGE_LENGTH}
                </span>
              </div>
            </form>
          </footer>
        </section>
      )}
    </div>
  );
}
