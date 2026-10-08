import { useEffect } from 'react';

const REDUCED_MOTION_QUERY = '(prefers-reduced-motion: reduce)';

function resetSurface(surface) {
  surface.style.setProperty('--tilt-x', '0deg');
  surface.style.setProperty('--tilt-y', '0deg');
  surface.style.setProperty('--pointer-x', '50%');
  surface.style.setProperty('--pointer-y', '50%');
  surface.classList.remove('is-pointer-active');
}

/**
 * Adds subtle physical response to marked surfaces without owning their base
 * transform. CSS composes these custom properties into the final perspective.
 * A single animation frame is shared by all surfaces and is fully cleaned up.
 */
export default function usePremiumMotion(rootRef) {
  useEffect(() => {
    const root = rootRef?.current;
    if (!root || typeof window === 'undefined') return undefined;

    const media = typeof window.matchMedia === 'function'
      ? window.matchMedia(REDUCED_MOTION_QUERY)
      : null;
    const surfaces = Array.from(root.querySelectorAll('[data-tilt]'));
    if (media?.matches || surfaces.length === 0) {
      surfaces.forEach(resetSurface);
      return undefined;
    }

    const requestFrame = typeof window.requestAnimationFrame === 'function'
      ? window.requestAnimationFrame.bind(window)
      : (callback) => window.setTimeout(callback, 16);
    const cancelFrame = typeof window.cancelAnimationFrame === 'function'
      ? window.cancelAnimationFrame.bind(window)
      : window.clearTimeout.bind(window);

    let frameId = null;
    let pending = null;

    const paint = () => {
      frameId = null;
      if (!pending) return;

      const { surface, clientX, clientY } = pending;
      const bounds = surface.getBoundingClientRect();
      const width = Math.max(bounds.width, 1);
      const height = Math.max(bounds.height, 1);
      const relativeX = Math.min(Math.max((clientX - bounds.left) / width, 0), 1);
      const relativeY = Math.min(Math.max((clientY - bounds.top) / height, 0), 1);
      const strength = Number(surface.dataset.tiltStrength || 4);
      const rotateY = (relativeX - 0.5) * strength * 2;
      const rotateX = (0.5 - relativeY) * strength * 2;

      surface.style.setProperty('--tilt-x', `${rotateX.toFixed(2)}deg`);
      surface.style.setProperty('--tilt-y', `${rotateY.toFixed(2)}deg`);
      surface.style.setProperty('--pointer-x', `${(relativeX * 100).toFixed(1)}%`);
      surface.style.setProperty('--pointer-y', `${(relativeY * 100).toFixed(1)}%`);
      surface.classList.add('is-pointer-active');
      pending = null;
    };

    const schedule = (event) => {
      pending = {
        surface: event.currentTarget,
        clientX: event.clientX,
        clientY: event.clientY,
      };
      if (frameId === null) frameId = requestFrame(paint);
    };

    const leave = (event) => {
      if (pending?.surface === event.currentTarget) pending = null;
      resetSurface(event.currentTarget);
    };

    const resetAll = () => surfaces.forEach(resetSurface);

    surfaces.forEach((surface) => {
      resetSurface(surface);
      surface.addEventListener('pointermove', schedule, { passive: true });
      surface.addEventListener('pointerleave', leave);
      surface.addEventListener('blur', leave, true);
    });
    window.addEventListener('blur', resetAll);

    return () => {
      surfaces.forEach((surface) => {
        surface.removeEventListener('pointermove', schedule);
        surface.removeEventListener('pointerleave', leave);
        surface.removeEventListener('blur', leave, true);
        resetSurface(surface);
      });
      window.removeEventListener('blur', resetAll);
      pending = null;
      if (frameId !== null) cancelFrame(frameId);
    };
  }, [rootRef]);
}
