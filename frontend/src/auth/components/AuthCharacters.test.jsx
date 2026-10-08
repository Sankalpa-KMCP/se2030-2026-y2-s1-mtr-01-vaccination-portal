import React from 'react';
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Link, MemoryRouter, Route, Routes } from 'react-router-dom';
import AuthCharacterGroup from './AuthCharacterGroup';
import AuthCharacterProvider from './AuthCharacterProvider';
import { AuthCharacterContext, useAuthCharacters, usePasswordPrivacy } from './authCharacterContext';
import LoginPage from '../pages/LoginPage';
import { useAuth } from '../context/AuthContext';

vi.mock('../context/AuthContext', () => ({ useAuth: vi.fn() }));

function PrivacyField({ id }) {
  const [revealed, setRevealed] = React.useState(false);
  const privacy = usePasswordPrivacy(revealed);
  return <div {...privacy}>
    <input aria-label={id} type={revealed ? 'text' : 'password'} />
    <button onClick={() => setRevealed((previous) => !previous)}>Reveal {id}</button>
  </div>;
}

function Harness() {
  const { reportSubmission, reportResult } = useAuthCharacters();
  return <main className="auth-workspace">
    <AuthCharacterGroup />
    <PrivacyField id="Password" /><PrivacyField id="Confirmation" />
    <input aria-label="Email" />
    <button onClick={() => reportSubmission(true)}>Submit</button>
    <button onClick={() => reportResult(false)}>Fail</button>
    <button onClick={() => reportResult(true)}>Succeed</button>
    <Link to="/other">Other route</Link>
  </main>;
}

function renderHarness() {
  return render(<MemoryRouter initialEntries={['/login']}><AuthCharacterProvider>
    <Routes><Route path="/login" element={<Harness />} /><Route path="/other" element={<AuthCharacterGroup />} /></Routes>
  </AuthCharacterProvider></MemoryRouter>);
}
const group = () => screen.getByRole('group', { name: 'Portal welcome characters' });

beforeEach(() => {
  vi.useFakeTimers();
  vi.stubGlobal('matchMedia', vi.fn((query) => ({
    matches: query.includes('pointer: fine'), addEventListener: vi.fn(), removeEventListener: vi.fn(),
  })));
});
afterEach(() => { cleanup(); vi.useRealTimers(); vi.unstubAllGlobals(); });

describe('Character artwork and gestures', () => {
  it('renders five distinct buttons, two glasses styles and one hijab', () => {
    const { container } = renderHarness();
    expect(group().querySelectorAll('button')).toHaveLength(5);
    expect(container.querySelectorAll('.auth-character__glasses')).toHaveLength(2);
    expect(container.querySelectorAll('.auth-character__hijab')).toHaveLength(1);
    expect(screen.getByRole('button', { name: /Sage.*girl creature.*hijab/ })).toBeTruthy();
  });
  it.each([[0, 'stretch'], [1, 'puff'], [2, 'thumb'], [3, 'tilt'], [4, 'scarf']])('character %s greets with %s', (index, gesture) => {
    renderHarness();
    const buttons = [...group().querySelectorAll('button')];
    fireEvent.focus(buttons[index]);
    expect(buttons[index].classList.contains(`auth-character--${gesture}`)).toBe(true);
    act(() => vi.advanceTimersByTime(650));
    expect(buttons[index].classList.contains(`auth-character--${gesture}`)).toBe(false);
  });
  it('starts a bounded neighbour ripple, ignores repeated activation, and cleans timers', () => {
    const { unmount } = renderHarness();
    const buttons = [...group().querySelectorAll('button')];
    fireEvent.click(buttons[2]);
    expect(group().dataset.scene).toBe('ripple');
    expect(buttons[2].dataset.gesture).toBe('thumb');
    expect(group().querySelectorAll('.auth-character--wave')).toHaveLength(4);
    expect(buttons[1].style.getPropertyValue('--reaction-delay')).toBe('110ms');
    expect(buttons[0].style.getPropertyValue('--reaction-delay')).toBe('220ms');
    fireEvent.click(buttons[0]);
    expect(buttons[2].dataset.selected).toBe('true');
    act(() => vi.advanceTimersByTime(1200));
    expect(group().dataset.scene).toBe('rest');
    fireEvent.click(group().querySelector('button'));
    unmount();
    expect(vi.getTimerCount()).toBe(0);
  });
  it('suppresses gestures when privacy takes priority', () => {
    renderHarness();
    fireEvent.click(group().querySelector('button'));
    fireEvent.focus(screen.getByLabelText('Password'));
    expect(group().dataset.mode).toBe('privacy');
    expect(group().dataset.scene).toBe('rest');
    fireEvent.click(group().querySelector('button'));
    expect(group().dataset.scene).toBe('rest');
    expect(group().querySelectorAll('.auth-creature__privacy-mittens')).toHaveLength(5);
  });
  it('tracks a fine pointer within 2px, resets on exit, and ignores touch', () => {
    renderHarness();
    const workspace = group().closest('main');
    const event = new MouseEvent('pointermove', { bubbles: true, clientX: 200, clientY: 100 });
    fireEvent(workspace, event);
    act(() => vi.advanceTimersByTime(20));
    const first = group().querySelector('button');
    const x = parseFloat(first.style.getPropertyValue('--gaze-x'));
    const y = parseFloat(first.style.getPropertyValue('--gaze-y'));
    expect(Math.hypot(x, y)).toBeCloseTo(2);
    fireEvent.pointerLeave(workspace);
    expect(first.style.getPropertyValue('--gaze-x')).toBe('');
    const touch = new MouseEvent('pointermove', { bubbles: true, clientX: 200 });
    Object.defineProperty(touch, 'pointerType', { value: 'touch' });
    fireEvent(workspace, touch);
    act(() => vi.advanceTimersByTime(20));
    expect(first.style.getPropertyValue('--gaze-x')).toBe('');
  });
  it('does not track the pointer when reduced motion is requested', () => {
    window.matchMedia.mockImplementation(() => ({ matches: true, addEventListener: vi.fn(), removeEventListener: vi.fn() }));
    renderHarness();
    fireEvent(group().closest('main'), new MouseEvent('pointermove', { bubbles: true, clientX: 200 }));
    act(() => vi.advanceTimersByTime(20));
    expect(group().querySelector('button').style.getPropertyValue('--gaze-x')).toBe('');
  });

  it('looks toward the selected neighbour and clears the glance after the greeting', () => {
    renderHarness();
    const buttons = [...group().querySelectorAll('button')];
    fireEvent.focus(buttons[2]);
    expect(buttons[1].dataset.neighbour).toBe('right');
    expect(buttons[3].dataset.neighbour).toBe('left');
    expect(buttons[0].dataset.neighbour).toBe('none');
    act(() => vi.advanceTimersByTime(650));
    expect(buttons[1].dataset.neighbour).toBe('none');
  });

  it('plays one arrival in StrictMode and does not replay after a login reaction', () => {
    render(<React.StrictMode><MemoryRouter><AuthCharacterProvider><Harness /></AuthCharacterProvider></MemoryRouter></React.StrictMode>);
    expect(group().dataset.scene).toBe('arrival');
    act(() => vi.advanceTimersByTime(900));
    expect(group().dataset.scene).toBe('rest');
    fireEvent.click(screen.getByRole('button', { name: 'Fail' }));
    act(() => vi.advanceTimersByTime(1000));
    expect(group().dataset.scene).toBe('rest');
  });

  it('uses independent, bounded gaze and lean for all five creatures', () => {
    renderHarness();
    const buttons = [...group().querySelectorAll('button')];
    buttons.forEach((button, index) => { button.getBoundingClientRect = () => ({ left: index * 44, top: 0, width: 44, height: 54 }); });
    fireEvent(group().closest('main'), new MouseEvent('pointermove', { bubbles: true, clientX: 115, clientY: 80 }));
    act(() => vi.advanceTimersByTime(20));
    buttons.forEach((button) => {
      const x = parseFloat(button.style.getPropertyValue('--gaze-x'));
      const y = parseFloat(button.style.getPropertyValue('--gaze-y'));
      expect(Math.hypot(x, y)).toBeLessThanOrEqual(2.001);
      expect(Math.abs(parseFloat(button.style.getPropertyValue('--lean')))).toBeLessThanOrEqual(3);
      expect(Math.abs(parseFloat(button.style.getPropertyValue('--lift')))).toBeLessThanOrEqual(1);
    });
    expect(parseFloat(buttons[0].style.getPropertyValue('--gaze-x'))).toBeGreaterThan(0);
    expect(parseFloat(buttons[4].style.getPropertyValue('--gaze-x'))).toBeLessThan(0);
  });

  it('pauses offscreen and hidden animation, resetting gaze and disconnecting observers', () => {
    let notify;
    const disconnect = vi.fn();
    vi.stubGlobal('IntersectionObserver', class { constructor(callback) { notify = callback; } observe() {} disconnect() { disconnect(); } });
    const hidden = vi.spyOn(document, 'hidden', 'get').mockReturnValue(false);
    const { unmount } = renderHarness();
    act(() => notify([{ isIntersecting: false }]));
    expect(group().dataset.paused).toBe('true');
    fireEvent.click(group().querySelector('button'));
    expect(group().dataset.scene).not.toBe('ripple');
    act(() => notify([{ isIntersecting: true }]));
    expect(group().dataset.paused).toBe('false');
    hidden.mockReturnValue(true);
    fireEvent(document, new Event('visibilitychange'));
    expect(group().dataset.paused).toBe('true');
    unmount();
    expect(disconnect).toHaveBeenCalled();
    expect(vi.getTimerCount()).toBe(0);
    hidden.mockRestore();
  });

  it('keeps every SVG gradient and clip ID unique across the header and success copy', () => {
    const { container } = renderHarness();
    fireEvent.click(screen.getByRole('button', { name: 'Succeed' }));
    const definitions = [...container.querySelectorAll('svg defs [id]')];
    const ids = definitions.map((element) => element.id);
    expect(new Set(ids).size).toBe(ids.length);
    container.querySelectorAll('svg [fill^="url"], svg [clip-path]').forEach((element) => {
      const reference = element.getAttribute('clip-path') || element.getAttribute('fill');
      const id = reference.slice(5, -1);
      expect([...element.closest('svg').querySelectorAll('defs [id]')].some((definition) => definition.id === id)).toBe(true);
    });
  });
});

describe('Privacy and login state', () => {
  it('keeps eyes covered between the password input and its visibility toggle', () => {
    renderHarness();
    const input = screen.getByLabelText('Password');
    const toggle = screen.getByRole('button', { name: 'Reveal Password' });
    fireEvent.focus(input);
    fireEvent.blur(input, { relatedTarget: toggle });
    expect(group().dataset.mode).toBe('privacy');
    fireEvent.click(toggle);
    fireEvent.blur(toggle, { relatedTarget: screen.getByLabelText('Email') });
    expect(group().dataset.mode).toBe('privacy');
    fireEvent.click(toggle);
    expect(group().dataset.mode).toBe('idle');
  });
  it('does not uncover eyes while another password remains revealed', () => {
    renderHarness();
    fireEvent.click(screen.getByRole('button', { name: 'Reveal Confirmation' }));
    fireEvent.focus(screen.getByLabelText('Password'));
    fireEvent.blur(screen.getByLabelText('Password'), { relatedTarget: screen.getByLabelText('Email') });
    expect(group().dataset.mode).toBe('privacy');
  });
  it('prioritises submission and failure over privacy, then restores privacy', () => {
    renderHarness();
    fireEvent.focus(screen.getByLabelText('Password'));
    fireEvent.click(screen.getByRole('button', { name: 'Submit' }));
    expect(group().dataset.mode).toBe('submitting');
    expect(group().dataset.covered).toBe('true');
    expect(group().querySelectorAll('.auth-creature__privacy-mittens')).toHaveLength(5);
    fireEvent.click(screen.getByRole('button', { name: 'Fail' }));
    expect(group().dataset.mode).toBe('failure');
    expect(group().dataset.covered).toBe('true');
    act(() => vi.advanceTimersByTime(1000));
    expect(group().dataset.mode).toBe('privacy');
  });
  it('resets route-specific privacy and reactions without losing the success overlay', () => {
    renderHarness();
    fireEvent.focus(screen.getByLabelText('Password'));
    fireEvent.click(screen.getByRole('button', { name: 'Succeed' }));
    fireEvent.click(screen.getByRole('link', { name: 'Other route' }));
    expect(group().dataset.mode).toBe('idle');
    const overlay = screen.getByTestId('character-celebration');
    expect(overlay.getAttribute('aria-hidden')).toBe('true');
    expect(overlay.querySelectorAll('button')).toHaveLength(0);
    act(() => vi.advanceTimersByTime(600));
    expect(screen.queryByTestId('character-celebration')).toBeNull();
  });
  it('reports only privacy flags to the presentation layer', () => {
    const reportPrivacy = vi.fn();
    render(<AuthCharacterContext.Provider value={{ reportPrivacy }}><PrivacyField id="Secret" /></AuthCharacterContext.Provider>);
    fireEvent.change(screen.getByLabelText('Secret'), { target: { value: 'never-share-this' } });
    fireEvent.focus(screen.getByLabelText('Secret'));
    expect(reportPrivacy.mock.calls.every(([id, value]) => typeof id === 'string' && typeof value === 'boolean')).toBe(true);
    expect(JSON.stringify(reportPrivacy.mock.calls)).not.toContain('never-share-this');
  });
  it.each([true, false])('connects real login results without delaying navigation (success=%s)', async (success) => {
    let complete;
    const login = vi.fn(() => new Promise((resolve) => { complete = resolve; }));
    useAuth.mockReturnValue({ login, loginError: '', clearLoginError: vi.fn() });
    render(<MemoryRouter initialEntries={['/login']}><AuthCharacterProvider><Routes>
      <Route path="/login" element={<main className="auth-workspace"><AuthCharacterGroup /><LoginPage /></main>} />
      <Route path="/app/citizen/welcome" element={<h1>Citizen welcome</h1>} />
    </Routes></AuthCharacterProvider></MemoryRouter>);
    fireEvent.change(screen.getByLabelText('Email Address'), { target: { value: 'citizen@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'test-password' } });
    fireEvent.submit(screen.getByRole('form', { name: 'Sign in credentials' }));
    expect(group().dataset.mode).toBe('submitting');
    await act(async () => complete({ success, user: { roles: ['ROLE_CITIZEN'] } }));
    if (success) {
      expect(screen.getByRole('heading', { name: 'Citizen welcome' })).toBeTruthy();
      expect(screen.getByTestId('character-celebration')).toBeTruthy();
    } else expect(group().dataset.mode).toBe('failure');
  });
});
