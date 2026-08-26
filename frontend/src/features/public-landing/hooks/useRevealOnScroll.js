import { useEffect } from 'react';

/**
 * Lightweight scroll-reveal for landing sections.
 * Adds .lp-is-visible to .lp-reveal elements as they enter the viewport.
 * Content remains fully visible without JS observers or under reduced motion
 * (the hidden state itself is gated behind prefers-reduced-motion in CSS).
 */
export function useRevealOnScroll() {
  useEffect(() => {
    const elements = Array.from(document.querySelectorAll('.lp-reveal'));

    if (typeof window === 'undefined' || !('IntersectionObserver' in window)) {
      return undefined;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('lp-is-visible');
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.12, rootMargin: '0px 0px -36px 0px' }
    );

    elements.forEach((element) => observer.observe(element));

    return () => {
      elements.forEach((element) => observer.unobserve(element));
      observer.disconnect();
    };
  }, []);
}
