import { useEffect } from 'react';

/**
 * Public pages are separate route destinations. Resetting an inherited scroll
 * position prevents a newly selected page from opening halfway through its
 * content, while in-page hash navigation remains untouched.
 */
export default function usePublicPageTop() {
  useEffect(() => {
    if (typeof window !== 'undefined' && window.scrollY > 0) {
      window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
    }
  }, []);
}
