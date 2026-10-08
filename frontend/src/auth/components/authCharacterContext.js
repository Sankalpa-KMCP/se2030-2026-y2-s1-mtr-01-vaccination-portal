import { createContext, useContext, useEffect, useId, useState } from 'react';

const noop = () => {};
export const AuthCharacterContext = createContext({
  mode: 'idle', reportPrivacy: noop, reportSubmission: noop, reportResult: noop,
});
export const useAuthCharacters = () => useContext(AuthCharacterContext);

// Only booleans leave the form. Field values never enter presentation state.
export function usePasswordPrivacy(revealed = false) {
  const { reportPrivacy } = useAuthCharacters();
  const id = useId();
  const [focused, setFocused] = useState(false);
  useEffect(() => {
    reportPrivacy(id, focused || revealed);
    return () => reportPrivacy(id, false);
  }, [id, focused, revealed, reportPrivacy]);
  return {
    onFocusCapture: () => setFocused(true),
    onBlurCapture: (event) => {
      if (!event.currentTarget.contains(event.relatedTarget)) setFocused(false);
    },
  };
}
