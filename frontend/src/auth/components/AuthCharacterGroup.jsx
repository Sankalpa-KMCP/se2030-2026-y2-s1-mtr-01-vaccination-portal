import React, { useEffect, useRef, useState } from 'react';
import { useAuthCharacters } from './authCharacterContext';
import CreatureArtwork from './CreatureArtwork';
import { CREATURES } from './creatureDefinitions';
import './auth-characters.css';

function useCreatureInteractions(groupRef, blocked, presentation) {
  const [scene, setScene] = useState(presentation ? null : { type: 'arrival', selected: null });
  const busy = useRef(false);
  const revision = useRef(0);
  const timer = useRef();
  const playable = useRef(true);
  useEffect(() => {
    clearTimeout(timer.current);
    busy.current = false;
    setScene((previous) => !blocked && !presentation && previous?.type === 'arrival' ? previous : null);
    if (!blocked && !presentation) timer.current = setTimeout(() => setScene(null), 900);
    return () => clearTimeout(timer.current);
  }, [blocked, presentation]);

  useEffect(() => {
    const group = groupRef.current;
    const workspace = group?.closest('.auth-workspace');
    if (!group) return undefined;
    const reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)');
    const fine = window.matchMedia?.('(hover: hover) and (pointer: fine)');
    let frame;
    let latest;
    let intersecting = true;
    const buttons = [...group.querySelectorAll('.auth-character')];
    const reset = () => {
      cancelAnimationFrame(frame);
      frame = undefined;
      buttons.forEach((button) => ['--gaze-x', '--gaze-y', '--lean', '--lift'].forEach((property) => button.style.removeProperty(property)));
    };
    const syncVisibility = () => {
      const paused = document.hidden || !intersecting;
      group.dataset.paused = String(paused);
      playable.current = !paused;
      if (paused || reduced?.matches || !fine?.matches) reset();
    };
    const follow = (event) => {
      if (blocked || reduced?.matches || !fine?.matches || !playable.current || event.pointerType === 'touch') return;
      latest = { x: event.clientX, y: event.clientY };
      if (frame !== undefined) return;
      frame = requestAnimationFrame(() => {
        frame = undefined;
        buttons.forEach((button) => {
          const bounds = button.getBoundingClientRect();
          const dx = latest.x - (bounds.left + bounds.width / 2);
          const dy = latest.y - (bounds.top + bounds.height / 2);
          const distance = Math.max(110, Math.hypot(dx, dy));
          button.style.setProperty('--gaze-x', `${2 * dx / distance}px`);
          button.style.setProperty('--gaze-y', `${2 * dy / distance}px`);
          button.style.setProperty('--lean', `${3 * dx / distance}deg`);
          button.style.setProperty('--lift', `${dy / distance}px`);
        });
      });
    };
    const observer = typeof IntersectionObserver !== 'undefined' ? new IntersectionObserver(([entry]) => {
      intersecting = entry.isIntersecting;
      syncVisibility();
    }) : null;
    observer?.observe(group);
    syncVisibility();
    reset();
    workspace?.addEventListener('pointermove', follow);
    workspace?.addEventListener('pointerleave', reset);
    document.addEventListener('visibilitychange', syncVisibility);
    reduced?.addEventListener?.('change', syncVisibility);
    fine?.addEventListener?.('change', syncVisibility);
    return () => {
      reset(); observer?.disconnect();
      workspace?.removeEventListener('pointermove', follow);
      workspace?.removeEventListener('pointerleave', reset);
      document.removeEventListener('visibilitychange', syncVisibility);
      reduced?.removeEventListener?.('change', syncVisibility);
      fine?.removeEventListener?.('change', syncVisibility);
    };
  }, [blocked, presentation, groupRef]);

  const trigger = (selected, ripple) => {
    if (blocked || presentation || !playable.current || busy.current) return;
    clearTimeout(timer.current);
    busy.current = ripple;
    setScene({ type: ripple ? 'ripple' : 'greeting', selected, revision: ++revision.current });
    timer.current = setTimeout(() => { busy.current = false; setScene(null); }, ripple ? 1200 : 650);
  };
  return { scene: blocked ? null : scene, trigger };
}

export default function AuthCharacterGroup({ presentation = false, mode: explicitMode }) {
  const { mode: contextMode, privacyActive = false } = useAuthCharacters();
  const mode = explicitMode || contextMode;
  const covered = !presentation && (privacyActive || mode === 'privacy');
  const groupRef = useRef(null);
  const { scene, trigger } = useCreatureInteractions(groupRef, covered || mode !== 'idle', presentation);
  return (
    <div ref={groupRef} className="auth-characters" role={presentation ? undefined : 'group'} aria-label={presentation ? undefined : 'Portal welcome characters'}
      data-mode={mode} data-covered={String(covered)} data-scene={scene?.type || 'rest'}>
      {CREATURES.map((creature, index) => {
        const distance = scene?.selected == null ? index : Math.abs(index - scene.selected);
        const signature = scene?.selected === index;
        const arrival = scene?.type === 'arrival';
        const neighbour = scene?.type === 'greeting' && distance === 1;
        const gesture = signature ? creature.gesture : scene?.type === 'ripple' || arrival ? 'wave' : 'rest';
        const props = {
          className: `auth-character auth-character--${gesture}`,
          style: { '--person-index': index, '--reaction-delay': `${signature ? 0 : distance * (arrival ? 65 : 110)}ms` },
          'data-gesture': gesture, 'data-neighbour': neighbour ? index < scene.selected ? 'right' : 'left' : 'none', 'data-selected': String(signature),
        };
        const content = <CreatureArtwork key={`${scene?.revision || 0}-${scene?.type || 'rest'}-${mode}-${covered}`} creature={creature} mode={mode} covered={covered} />;
        return presentation ? <span key={creature.name} {...props}>{content}</span> : (
          <button key={creature.name} {...props} type="button" aria-label={`Greet ${creature.name}, the ${creature.description}; wave with everyone`}
            onPointerEnter={(event) => { if (event.pointerType !== 'touch') trigger(index, false); }}
            onFocus={() => trigger(index, false)} onClick={() => trigger(index, true)}>{content}</button>
        );
      })}
    </div>
  );
}
