export function shouldShowConnectedCommunityIntro(isInitialHomeLoad) {
  return Boolean(isInitialHomeLoad)
    && typeof window !== 'undefined'
    && !window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
}
