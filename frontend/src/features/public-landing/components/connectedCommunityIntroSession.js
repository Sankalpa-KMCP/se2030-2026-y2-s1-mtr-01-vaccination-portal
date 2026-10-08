export const INTRO_SESSION_KEY = 'vaccination-portal-connected-community-intro-seen';

export function shouldShowConnectedCommunityIntro() {
  if (typeof window === 'undefined' || window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
    return false;
  }

  try {
    return window.sessionStorage.getItem(INTRO_SESSION_KEY) !== '1';
  } catch {
    return false;
  }
}
