import React, { useRef } from 'react';
import { act, cleanup, render } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import useLandingReveal from './useLandingReveal';

const STEP_IDS = [
  'portal-access',
  'capabilities',
  'how-it-works',
  'accountability',
  'get-started',
];

function HookHarness() {
  const rootRef = useRef(null);
  useLandingReveal(rootRef);

  return (
    <div ref={rootRef}>
      <nav className="home-care-rail">
        {STEP_IDS.map((id, index) => (
          <a key={id} href={`#${id}`} data-care-marker={index + 1}>{id}</a>
        ))}
      </nav>
      <div data-reveal>Reveal target</div>
      {STEP_IDS.map((id) => <section id={id} key={id}>{id}</section>)}
      <footer className="lp-footer">Footer</footer>
    </div>
  );
}

describe('useLandingReveal', () => {
  let nextFrame;
  let scrollTop;
  let cancelFrame;

  beforeEach(() => {
    nextFrame = undefined;
    scrollTop = 0;
    cancelFrame = vi.fn();

    vi.stubGlobal('requestAnimationFrame', vi.fn((callback) => {
      nextFrame = callback;
      return 41;
    }));
    vi.stubGlobal('cancelAnimationFrame', cancelFrame);
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: false })));
    vi.stubGlobal('innerHeight', 1000);
    Object.defineProperty(window, 'scrollY', {
      configurable: true,
      get: () => scrollTop,
    });
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  function installGeometry(container) {
    const absolutePositions = [1000, 2000, 3000, 4000, 5000];
    STEP_IDS.forEach((id, index) => {
      vi.spyOn(container.querySelector(`#${id}`), 'getBoundingClientRect')
        .mockImplementation(() => ({ top: absolutePositions[index] - scrollTop }));
    });
    vi.spyOn(container.querySelector('.lp-footer'), 'getBoundingClientRect')
      .mockImplementation(() => ({ top: 6000 - scrollTop }));
  }

  function runScheduledFrame() {
    const callback = nextFrame;
    nextFrame = undefined;
    act(() => callback?.(0));
  }

  function scrollTo(position) {
    scrollTop = position;
    act(() => window.dispatchEvent(new Event('scroll')));
    runScheduledFrame();
  }

  it('clamps progress and updates active, completed, and visible rail states', () => {
    const { container } = render(<HookHarness />);
    const root = container.firstElementChild;
    installGeometry(container);

    runScheduledFrame();
    expect(root.style.getPropertyValue('--home-care-progress')).toBe('0.0000');
    expect(root.dataset.activeCareStep).toBe('0');
    expect(root.classList.contains('is-care-rail-visible')).toBe(false);

    scrollTo(2550);
    expect(root.style.getPropertyValue('--home-care-progress')).toBe('0.5000');
    expect(root.dataset.activeCareStep).toBe('3');
    expect(root.classList.contains('is-care-rail-visible')).toBe(true);
    expect(container.querySelector('[data-care-marker="1"]').classList.contains('is-complete')).toBe(true);
    expect(container.querySelector('[data-care-marker="2"]').classList.contains('is-complete')).toBe(true);
    expect(container.querySelector('[data-care-marker="3"]').getAttribute('aria-current')).toBe('step');

    scrollTo(5700);
    expect(root.style.getPropertyValue('--home-care-progress')).toBe('1.0000');
    expect(root.dataset.activeCareStep).toBe('5');
    expect(root.classList.contains('is-care-rail-visible')).toBe(false);
  });

  it('reveals immediately for reduced motion and cleans up scheduled work', () => {
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: true })));
    const removeListener = vi.spyOn(window, 'removeEventListener');
    const { container, unmount } = render(<HookHarness />);

    expect(container.querySelector('[data-reveal]').classList.contains('is-revealed')).toBe(true);
    expect(container.firstElementChild.classList.contains('lp-reveal-ready')).toBe(false);

    unmount();
    expect(cancelFrame).toHaveBeenCalledWith(41);
    expect(removeListener).toHaveBeenCalledWith('scroll', expect.any(Function));
    expect(removeListener).toHaveBeenCalledWith('resize', expect.any(Function));
  });
});
