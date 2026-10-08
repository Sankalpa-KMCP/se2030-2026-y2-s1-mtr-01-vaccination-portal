/**
 * AssistantWidget Component.
 * Accessible, responsive floating chat assistant widget.
 * Available across public and authenticated vaccination portal pages.
 */

import React, { useState, useRef, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAssistantConversation } from '../hooks/useAssistantConversation';
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
    personalizedFeaturesAvailable,
    disclaimer,
    suggestedQuestions,
  } = useAssistantConversation();

  const launcherRef = useRef(null);
  const panelRef = useRef(null);
  const inputRef = useRef(null);
  const messagesEndRef = useRef(null);
  const widgetRef = useRef(null);
  const wasOpenRef = useRef(false);
  const hasConversation = messages.some((message) => message.role === 'user');

  // Auto-scroll messages list when new messages arrive
  useEffect(() => {
    if (isOpen && hasConversation) {
      const reduceMotion = window.matchMedia?.(
        '(prefers-reduced-motion: reduce)'
      ).matches;
      messagesEndRef.current?.scrollIntoView({
        behavior: reduceMotion ? 'instant' : 'smooth',
        block: 'end',
      });
    }
  }, [messages, isSending, isOpen, hasConversation]);

  // Grow the composer with its content, then scroll within it for longer messages.
  useEffect(() => {
    const input = inputRef.current;
    if (input) {
      input.style.height = 'auto';
      input.style.height = `${Math.min(input.scrollHeight, 120)}px`;
    }
  }, [inputVal, isOpen]);

  // Keep the sheet above the on-screen keyboard on mobile browsers.
  useEffect(() => {
    const viewport = window.visualViewport;
    if (!viewport || !isOpen) return undefined;
    const updateViewport = () => {
      widgetRef.current?.style.setProperty(
        '--assistant-viewport-height',
        `${viewport.height}px`
      );
      widgetRef.current?.style.setProperty(
        '--assistant-viewport-bottom',
        `${Math.max(0, window.innerHeight - viewport.height - viewport.offsetTop)}px`
      );
    };
    updateViewport();
    viewport.addEventListener('resize', updateViewport);
    viewport.addEventListener('scroll', updateViewport);
    return () => {
      viewport.removeEventListener('resize', updateViewport);
      viewport.removeEventListener('scroll', updateViewport);
    };
  }, [isOpen]);

  // Handle focus when panel opens / closes
  useEffect(() => {
    if (isOpen && isAvailable) {
      inputRef.current?.focus();
    } else if (!isOpen && wasOpenRef.current) {
      launcherRef.current?.focus();
    }
    wasOpenRef.current = isOpen;
  }, [isOpen, isAvailable]);

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
    if (
      !inputVal.trim() ||
      inputVal.length > MAX_MESSAGE_LENGTH ||
      isSending ||
      !isAvailable
    ) {
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
  const canSend =
    inputVal.trim().length > 0 && !isOverLimit && !isSending && isAvailable;

  return (
    <div className="assistant-widget" ref={widgetRef}>
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
        <span className="assistant-launcher-label">
          {isOpen ? 'Close assistant' : 'Ask assistant'}
        </span>
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
            <div className="assistant-header-identity">
              <span className="assistant-emblem" aria-hidden="true">
                <i className="bi bi-chat-heart" />
              </span>
              <div>
                <span className="assistant-eyebrow">
                  A little guidance, a little easier
                </span>
                <h2 className="assistant-header-title">Portal Assistant</h2>
                <span className="assistant-availability">
                  <span
                    className={`assistant-status-dot ${isStatusLoading ? 'loading' : isAvailable ? '' : 'unavailable'}`}
                    aria-hidden="true"
                  />
                  {isStatusLoading
                    ? 'Connecting…'
                    : isAvailable
                      ? 'Ready to help'
                      : 'Currently unavailable'}
                </span>
              </div>
            </div>
            <div className="assistant-header-actions">
              <button
                type="button"
                className="assistant-icon-btn"
                aria-label="Clear conversation"
                title="Clear conversation"
                onClick={clearConversation}
              >
                <i
                  className="bi bi-arrow-counterclockwise"
                  aria-hidden="true"
                />
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

          {personalizedFeaturesAvailable && (
            <div className="assistant-privacy-banner" role="note">
              <i className="bi bi-shield-lock-fill" aria-hidden="true" />
              <span>
                Personal information is shown only for your verified signed-in
                account.
              </span>
            </div>
          )}

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
          <div
            className="assistant-messages-container"
            tabIndex={0}
            role="region"
            aria-label="Conversation messages"
          >
            {messages.map((msg, index) => (
              <div
                key={msg.id || index}
                className={`assistant-message-row ${msg.role}`}
              >
                {msg.isGreeting ? (
                  <div
                    className={`assistant-welcome ${hasConversation ? 'compact' : ''}`}
                  >
                    {!hasConversation && (
                      <>
                        <span className="assistant-welcome-kicker">
                          YOUR PORTAL COMPANION
                        </span>
                        <h3>How can I help?</h3>
                      </>
                    )}
                    <p>{msg.content}</p>
                  </div>
                ) : (
                  <>
                    <span className="assistant-speaker">
                      {msg.role === 'user' ? 'You' : 'Portal Assistant'}
                    </span>
                    <div className="assistant-message-bubble">
                      {msg.content}
                    </div>
                  </>
                )}

                {/* Suggested Navigation Actions */}
                {Array.isArray(msg.suggestedActions) &&
                  msg.suggestedActions.length > 0 && (
                    <div
                      className="assistant-actions-container"
                      aria-label="Suggested navigation"
                    >
                      {msg.suggestedActions.map((action, actIdx) => (
                        <button
                          key={`${msg.id}-action-${actIdx}`}
                          type="button"
                          className="assistant-action-chip"
                          onClick={() => handleActionClick(action.path)}
                        >
                          <i
                            className="bi bi-arrow-right-short"
                            aria-hidden="true"
                          />
                          {action.label}
                        </button>
                      ))}
                    </div>
                  )}

                {/* Initial suggested questions chips under initial greeting */}
                {msg.isGreeting && !hasConversation && (
                  <div
                    className="assistant-suggested-questions"
                    aria-label="Suggested questions"
                  >
                    {suggestedQuestions.map((q, qIdx) => (
                      <button
                        key={`sq-${qIdx}`}
                        type="button"
                        className="assistant-question-chip"
                        disabled={!isAvailable || isSending}
                        onClick={() => handleQuestionClick(q)}
                      >
                        <span
                          className="assistant-question-icon"
                          aria-hidden="true"
                        >
                          <i
                            className={`bi ${['bi-person-plus', 'bi-clipboard2-check', 'bi-calendar2-heart', 'bi-patch-check'][qIdx % 4]}`}
                          />
                        </span>
                        <span>{q}</span>
                        <i
                          className="bi bi-arrow-up-right assistant-question-arrow"
                          aria-hidden="true"
                        />
                      </button>
                    ))}
                  </div>
                )}
              </div>
            ))}

            {/* Sending indicator */}
            {isSending && (
              <div
                className="assistant-typing-indicator"
                role="status"
                aria-label="Assistant is thinking"
              >
                <span className="assistant-typing-dots" aria-hidden="true">
                  <span className="assistant-typing-dot" />
                  <span className="assistant-typing-dot" />
                  <span className="assistant-typing-dot" />
                </span>
                Finding your next step…
              </div>
            )}

            <div ref={messagesEndRef} aria-hidden="true" />
          </div>

          {/* Dynamic polite live region for accessibility */}
          <div
            className="visually-hidden"
            aria-live="polite"
            aria-atomic="true"
          >
            {turnError && `Error: ${turnError}`}
            {statusError && `Status error: ${statusError}`}
            {messages[messages.length - 1]?.role === 'assistant' &&
              messages[messages.length - 1]?.content}
          </div>

          {/* Turn Error Banner */}
          {turnError && (
            <div className="assistant-error-banner" role="alert">
              <i
                className="bi bi-exclamation-triangle-fill"
                aria-hidden="true"
              />
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
                  placeholder={
                    isAvailable
                      ? 'Ask about portal workflows...'
                      : 'Assistant is currently unavailable'
                  }
                  value={inputVal}
                  onChange={(e) => setInputVal(e.target.value)}
                  onKeyDown={handleTextareaKeyDown}
                  disabled={!isAvailable || isSending}
                  aria-label="Ask the portal assistant"
                  aria-describedby="assistant-input-meta"
                  aria-invalid={isOverLimit}
                  maxLength={MAX_MESSAGE_LENGTH + 50}
                />
                <button
                  type="submit"
                  className="assistant-send-btn"
                  disabled={!canSend}
                  aria-label="Send message"
                  title="Send message"
                >
                  <i className="bi bi-arrow-up" aria-hidden="true" />
                </button>
              </div>

              <div className="assistant-input-meta" id="assistant-input-meta">
                <span className="assistant-keyboard-hint">
                  Enter to send <span aria-hidden="true">·</span> Shift + Enter
                  for a new line
                </span>
                <span className={isOverLimit ? 'assistant-limit-error' : ''}>
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
