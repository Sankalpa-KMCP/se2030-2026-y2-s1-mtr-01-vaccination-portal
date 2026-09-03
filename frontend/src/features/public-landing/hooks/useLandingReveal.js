import { useEffect } from 'react';

const REVEAL_MEDIA_QUERY = '(prefers-reduced-motion: reduce)';

/**
 * Toggles `.is-revealed` on every `[data-reveal]` element inside the landing
 * page root when it scrolls into view. Elements render visible (no transform)
 * when the user prefers reduced motion.
 */
export default function useLandingReveal(rootRef) {
  useEffect(() => {
    const root = rootRef?.current;
    if (!root || typeof window === 'undefined' || typeof IntersectionObserver === 'undefined') {
      return undefined;
    }

    const supportsReducedMotion = typeof window.matchMedia === 'function';
    const targets = Array.from(root.querySelectorAll('[data-reveal]'));

    if (supportsReducedMotion && window.matchMedia(REVEAL_MEDIA_QUERY).matches) {
      targets.forEach((el) => el.classList.add('is-revealed'));
      return undefined;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-revealed');
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.16, rootMargin: '0px 0px -8% 0px' },
    );

    targets.forEach((el) => observer.observe(el));
    return () => observer.disconnect();
  }, [rootRef]);
}
