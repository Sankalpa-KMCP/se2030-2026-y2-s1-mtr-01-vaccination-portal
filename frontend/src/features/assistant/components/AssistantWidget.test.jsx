import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AssistantWidget from './AssistantWidget';
import * as assistantApi from '../api/assistantApi';

const authContextMock = vi.hoisted(() => ({
  current: { user: null, isAuthenticated: false },
}));

vi.mock('../../../auth/context/AuthContext', () => ({
  useAuth: () => authContextMock.current,
}));

// Mock useNavigate to monitor routing
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

function renderWidget(path = '/') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AssistantWidget />
    </MemoryRouter>
  );
}

async function openPanel(path = '/') {
  const rendered = renderWidget(path);
  fireEvent.click(screen.getByRole('button', { name: /open portal assistant/i }));
  await screen.findByPlaceholderText('Ask about portal workflows...');
  return rendered;
}

describe('AssistantWidget component', () => {
  beforeEach(() => {
    mockNavigate.mockReset();
    authContextMock.current = { user: null, isAuthenticated: false };

    vi.spyOn(assistantApi, 'getAssistantStatus').mockResolvedValue({
      available: true,
      disclaimer: 'System disclaimer: Not medical advice.',
      personalizedFeaturesAvailable: false,
    });

    vi.spyOn(assistantApi, 'sendAssistantMessage').mockResolvedValue({
      answer: 'This is the verified assistant response.',
      suggestedActions: [
        { label: 'Book Appointment', path: '/app/appointments/book' },
        { label: 'Evil Path', path: 'https://evil.com' },
      ],
    });

    // Mock scrollIntoView
    Element.prototype.scrollIntoView = vi.fn();
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('renders launcher button on public route', () => {
    renderWidget('/how-it-works');
    const launcher = screen.getByRole('button', { name: /open portal assistant/i });
    expect(launcher).toBeTruthy();
    expect(launcher.getAttribute('aria-expanded')).toBe('false');
  });

  it('renders launcher button for authenticated users', () => {
    authContextMock.current = {
      isAuthenticated: true,
      user: { id: 10, role: 'ROLE_CITIZEN' },
    };
    renderWidget('/app/citizen');
    expect(screen.getByRole('button', { name: /open portal assistant/i })).toBeTruthy();
  });

  it('opens and closes the dialog panel via launcher button and close button', async () => {
    await openPanel();
    expect(screen.getByRole('dialog', { name: /portal assistant/i })).toBeTruthy();
    expect(screen.getByText('System disclaimer: Not medical advice.')).toBeTruthy();

    // Close panel using close button
    const closeBtn = screen.getByRole('button', { name: /close assistant/i });
    fireEvent.click(closeBtn);
    expect(screen.queryByRole('dialog', { name: /portal assistant/i })).toBeNull();
  });

  it('closes dialog panel on Escape key', async () => {
    await openPanel();
    expect(screen.getByRole('dialog', { name: /portal assistant/i })).toBeTruthy();

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(screen.queryByRole('dialog', { name: /portal assistant/i })).toBeNull();
  });

  it('disables input, shows alert, and allows Retry when assistant is unavailable', async () => {
    assistantApi.getAssistantStatus.mockResolvedValueOnce({
      available: false,
      disclaimer: 'Offline disclaimer',
    });

    renderWidget();
    fireEvent.click(screen.getByRole('button', { name: /open portal assistant/i }));

    const unavailableAlert = await screen.findByRole('alert');
    expect(unavailableAlert.textContent).toContain('The portal assistant is currently unavailable.');
    const textarea = screen.getByRole('textbox', { name: /ask the portal assistant/i });
    expect(textarea.hasAttribute('disabled')).toBe(true);

    const retryBtn = screen.getByRole('button', { name: /retry/i });
    expect(retryBtn).toBeTruthy();

    assistantApi.getAssistantStatus.mockResolvedValueOnce({
      available: true,
      disclaimer: 'Back online',
    });

    fireEvent.click(retryBtn);

    await waitFor(() => {
      expect(screen.queryByText('The portal assistant is currently unavailable.')).toBeNull();
    });
    expect(textarea.hasAttribute('disabled')).toBe(false);
  });

  it('submits valid message, displays user and assistant responses, and prevents blank submission', async () => {
    await openPanel();

    const textarea = screen.getByRole('textbox', { name: /ask the portal assistant/i });
    const sendBtn = screen.getByRole('button', { name: /send message/i });

    // Initially blank, send button is disabled
    expect(sendBtn.hasAttribute('disabled')).toBe(true);

    // Enter message
    fireEvent.change(textarea, { target: { value: 'How do I schedule?' } });
    expect(sendBtn.hasAttribute('disabled')).toBe(false);

    fireEvent.click(sendBtn);

    // User message rendered
    expect(await screen.findByText('How do I schedule?')).toBeTruthy();
    // Assistant message rendered
    const assistantMatches = await screen.findAllByText('This is the verified assistant response.');
    expect(assistantMatches.length).toBeGreaterThan(0);
  });

  it('submits message using suggested question chip', async () => {
    await openPanel();

    const questionChip = await screen.findByRole('button', { name: /how do i register\?/i });
    fireEvent.click(questionChip);

    const matches = await screen.findAllByText('How do I register?');
    expect(matches.length).toBeGreaterThan(0);
    expect(assistantApi.sendAssistantMessage).toHaveBeenCalledWith(
      expect.objectContaining({
        message: 'How do I register?',
      })
    );
  });

  it('renders safe suggested actions and navigates via React Router on click', async () => {
    await openPanel();

    const textarea = screen.getByRole('textbox', { name: /ask the portal assistant/i });
    fireEvent.change(textarea, { target: { value: 'Appointment info' } });
    fireEvent.click(screen.getByRole('button', { name: /send message/i }));

    // Book Appointment is safe
    const actionBtn = await screen.findByRole('button', { name: /book appointment/i });
    expect(actionBtn).toBeTruthy();

    // Evil Path was filtered out
    expect(screen.queryByRole('button', { name: /evil path/i })).toBeNull();

    fireEvent.click(actionBtn);
    expect(mockNavigate).toHaveBeenCalledWith('/app/appointments/book');
  });

  it('clears conversation turns when Clear conversation button is clicked', async () => {
    await openPanel();

    const textarea = screen.getByRole('textbox', { name: /ask the portal assistant/i });
    fireEvent.change(textarea, { target: { value: 'First question' } });
    fireEvent.click(screen.getByRole('button', { name: /send message/i }));

    expect(await screen.findByText('First question')).toBeTruthy();

    const clearButtons = screen.getAllByRole('button', { name: /clear conversation/i });
    fireEvent.click(clearButtons[0]);

    expect(screen.queryByText('First question')).toBeNull();
    const initialGreetings = screen.getAllByText(/hello\. i can help you understand portal services/i);
    expect(initialGreetings.length).toBeGreaterThan(0);
  });

  it('renders assistant content as safe plain text and cannot inject HTML or execute scripts', async () => {
    assistantApi.sendAssistantMessage.mockResolvedValueOnce({
      answer: '<img src=x onerror=alert(1)> <b>Bold text test</b>',
      suggestedActions: [],
    });

    await openPanel();

    const textarea = screen.getByRole('textbox', { name: /ask the portal assistant/i });
    fireEvent.change(textarea, { target: { value: 'XSS check' } });
    fireEvent.click(screen.getByRole('button', { name: /send message/i }));

    const answerMatches = await screen.findAllByText(/<img src=x onerror=alert\(1\)> <b>Bold text test<\/b>/);
    expect(answerMatches.length).toBeGreaterThan(0);
    const bubble = answerMatches[0];
    expect(bubble.innerHTML).not.toContain('<img');
    expect(bubble.innerHTML).toContain('&lt;img');
  });

  it('displays friendly error banner on 429 rate limit or 503 error', async () => {
    assistantApi.sendAssistantMessage.mockRejectedValueOnce({
      status: 429,
      message: 'Too many requests',
    });

    await openPanel();

    const textarea = screen.getByRole('textbox', { name: /ask the portal assistant/i });
    fireEvent.change(textarea, { target: { value: 'Rate limited request' } });
    fireEvent.click(screen.getByRole('button', { name: /send message/i }));

    const alertBanner = await screen.findByRole('alert');
    expect(alertBanner.textContent).toContain('You have sent several requests. Please wait and try again.');
  });
});
