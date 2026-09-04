import { useEffect } from 'react';

const REVEAL_MEDIA_QUERY = '(prefers-reduced-motion: reduce)';
const CARE_CURSOR_RATIO = 0.45;
const CARE_RAIL_ENTER_RATIO = 0.72;
const CARE_RAIL_EXIT_RATIO = 0.4;

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max);
}

/**
 * Progressively enhances `[data-reveal]` elements by toggling `.is-revealed`
 * when they enter the viewport.
 *
 * Content is fully visible by default in CSS. The `.lp-reveal-ready` class is
 * only added to the root container when IntersectionObserver is supported and
 * the user has not requested reduced motion.
 */
export default function useLandingReveal(rootRef) {
  useEffect(() => {
    const root = rootRef?.current;
    if (!root || typeof window === 'undefined') {
      return undefined;
    }

    const supportsMatchMedia = typeof window.matchMedia === 'function';
    const prefersReducedMotion = supportsMatchMedia && window.matchMedia(REVEAL_MEDIA_QUERY).matches;
    const targets = Array.from(root.querySelectorAll('[data-reveal]'));

    let revealObserver;
    if (targets.length === 0 || prefersReducedMotion) {
      targets.forEach((el) => el.classList.add('is-revealed'));
    } else if (typeof IntersectionObserver !== 'undefined') {
      root.classList.add('lp-reveal-ready');

      revealObserver = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting) {
              entry.target.classList.add('is-revealed');
              revealObserver.unobserve(entry.target);
            }
          });
        },
        { threshold: 0.08, rootMargin: '0px 0px -4% 0px' },
      );

      targets.forEach((el) => revealObserver.observe(el));
    }

    const markers = Array.from(root.querySelectorAll('[data-care-marker]'));
    const careSteps = markers
      .map((marker) => {
        const targetSelector = marker.getAttribute('href');
        const target = targetSelector?.startsWith('#')
          ? root.querySelector(targetSelector)
          : null;
        return target ? { marker, target } : null;
      })
      .filter(Boolean);
    const footer = root.querySelector('.lp-footer');

    let frameId = null;
    const requestFrame = typeof window.requestAnimationFrame === 'function'
      ? window.requestAnimationFrame.bind(window)
      : (callback) => window.setTimeout(callback, 16);
    const cancelFrame = typeof window.cancelAnimationFrame === 'function'
      ? window.cancelAnimationFrame.bind(window)
      : window.clearTimeout.bind(window);

    const updateCareProgress = () => {
      frameId = null;
      if (careSteps.length < 2) return;

      const viewportHeight = window.innerHeight || document.documentElement.clientHeight || 1;
      const scrollTop = window.scrollY || window.pageYOffset || 0;
      const cursorPosition = scrollTop + (viewportHeight * CARE_CURSOR_RATIO);
      const stepPositions = careSteps.map(({ target }) => (
        target.getBoundingClientRect().top + scrollTop
      ));
      const firstPosition = stepPositions[0];
      const lastPosition = stepPositions[stepPositions.length - 1];
      const progressRange = Math.max(lastPosition - firstPosition, 1);
      const progress = clamp((cursorPosition - firstPosition) / progressRange, 0, 1);

      let activeIndex = -1;
      stepPositions.forEach((position, index) => {
        if (cursorPosition >= position) activeIndex = index;
      });

      const firstStepTop = firstPosition - scrollTop;
      const footerTop = footer?.getBoundingClientRect().top ?? Number.POSITIVE_INFINITY;
      const railVisible = firstStepTop <= viewportHeight * CARE_RAIL_ENTER_RATIO
        && footerTop > viewportHeight * CARE_RAIL_EXIT_RATIO;

      root.style.setProperty('--home-care-progress', progress.toFixed(4));
      root.classList.toggle('is-care-rail-visible', railVisible);
      root.dataset.activeCareStep = activeIndex >= 0 ? String(activeIndex + 1) : '0';

      careSteps.forEach(({ marker }, index) => {
        const isActive = index === activeIndex;
        marker.classList.toggle('is-active', isActive);
        marker.classList.toggle('is-complete', index < activeIndex);
        if (isActive) {
          marker.setAttribute('aria-current', 'step');
        } else {
          marker.removeAttribute('aria-current');
        }
      });
    };

    const scheduleCareProgress = () => {
      if (frameId === null) frameId = requestFrame(updateCareProgress);
    };

    if (careSteps.length >= 2) {
      window.addEventListener('scroll', scheduleCareProgress, { passive: true });
      window.addEventListener('resize', scheduleCareProgress);
      scheduleCareProgress();
    }

    return () => {
      revealObserver?.disconnect();
      window.removeEventListener('scroll', scheduleCareProgress);
      window.removeEventListener('resize', scheduleCareProgress);
      if (frameId !== null) cancelFrame(frameId);
      root.classList.remove('lp-reveal-ready');
      root.classList.remove('is-care-rail-visible');
      root.style.removeProperty('--home-care-progress');
      delete root.dataset.activeCareStep;
    };
  }, [rootRef]);
}
