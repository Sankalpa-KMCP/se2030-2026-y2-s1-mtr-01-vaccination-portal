import { useEffect } from 'react';

const REVEAL_MEDIA_QUERY = '(prefers-reduced-motion: reduce)';

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
    if (!root || typeof window === 'undefined' || typeof IntersectionObserver === 'undefined') {
      return undefined;
    }

    const supportsMatchMedia = typeof window.matchMedia === 'function';
    const prefersReducedMotion = supportsMatchMedia && window.matchMedia(REVEAL_MEDIA_QUERY).matches;

    const targets = Array.from(root.querySelectorAll('[data-reveal]'));
    if (targets.length === 0 || prefersReducedMotion) {
      // Keep everything immediately visible with zero delay
      targets.forEach((el) => el.classList.add('is-revealed'));
      return undefined;
    }

    // Enable scroll reveal styles progressively
    root.classList.add('lp-reveal-ready');

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-revealed');
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.08, rootMargin: '0px 0px -4% 0px' },
    );

    targets.forEach((el) => observer.observe(el));
    return () => {
      observer.disconnect();
      root.classList.remove('lp-reveal-ready');
    };
  }, [rootRef]);
}
