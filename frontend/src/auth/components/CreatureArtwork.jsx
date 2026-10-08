import React, { useId } from 'react';

const SHAPES = {
  sprout: 'M28 15C43 13 46 23 42 33C39 41 44 51 29 53C12 56 8 45 11 34C13 26 11 17 28 15Z',
  puff: 'M15 20C11 10 24 7 29 16C38 6 50 18 43 26C53 34 46 47 38 46C35 55 23 56 19 49C7 52 3 38 10 32C3 26 8 19 15 20Z',
  pebble: 'M17 16L35 14Q45 14 46 24L47 42Q47 51 37 52L19 53Q9 53 8 43L7 27Q7 18 17 16Z',
  dew: 'M28 10C31 18 43 24 45 35C49 57 9 60 9 39C9 27 22 20 28 10Z',
  sage: 'M27 12C40 12 46 22 45 35L48 47Q29 58 8 48L10 34C9 22 15 12 27 12Z',
};

export default function CreatureArtwork({ creature, mode, covered }) {
  const instance = useId().replace(/[^a-zA-Z0-9_-]/g, '');
  const id = (name) => `creature-${instance}-${name}`;
  const fill = (name) => `url(#${id(name)})`;
  const shape = SHAPES[creature.shape];
  return (
    <svg className="auth-creature-art" viewBox="0 0 56 64" width="100%" height="100%" fill="none" aria-hidden="true" focusable="false">
      <defs>
        <radialGradient id={id('body')} cx=".28" cy=".2" r=".85"><stop stopColor={creature.light} /><stop offset=".47" stopColor={creature.mid} /><stop offset="1" stopColor={creature.dark} /></radialGradient>
        <radialGradient id={id('mitten')} cx=".25" cy=".2" r=".9"><stop stopColor={creature.light} /><stop offset=".7" stopColor={creature.mid} /><stop offset="1" stopColor={creature.dark} /></radialGradient>
        <linearGradient id={id('shine')} x1="15" y1="16" x2="28" y2="36" gradientUnits="userSpaceOnUse"><stop stopColor="white" stopOpacity=".75" /><stop offset="1" stopColor="white" stopOpacity="0" /></linearGradient>
        <linearGradient id={id('scarf')} x1="13" y1="14" x2="43" y2="52" gradientUnits="userSpaceOnUse"><stop stopColor="#DFEFDC" /><stop offset=".42" stopColor="#96BCA5" /><stop offset="1" stopColor="#4E806D" /></linearGradient>
        <linearGradient id={id('lens')} x1="18" y1="27" x2="26" y2="35" gradientUnits="userSpaceOnUse"><stop stopColor="white" stopOpacity=".32" /><stop offset="1" stopColor="white" stopOpacity=".02" /></linearGradient>
        <radialGradient id={id('shadow')}><stop stopColor={creature.dark} stopOpacity=".22" /><stop offset="1" stopColor={creature.dark} stopOpacity="0" /></radialGradient>
        <clipPath id={id('clip')}><path d={shape} /></clipPath>
      </defs>
      <ellipse className="auth-creature__shadow" cx="28" cy="59" rx="22" ry="3.5" fill={fill('shadow')} />
      <g className="auth-creature__gaze-body"><g className="auth-creature__body">
        <g className="auth-creature__feet" fill={fill('mitten')} stroke={creature.outline} strokeWidth="1.2"><ellipse cx="20" cy="54" rx="5" ry="3.5" transform="rotate(-12 20 54)" /><ellipse cx="36" cy="54" rx="5" ry="3.5" transform="rotate(12 36 54)" /></g>
        <path d={shape} fill={fill('body')} stroke={creature.outline} strokeWidth="1.7" strokeLinejoin="round" />
        <g clipPath={`url(#${id('clip')})`}><path d="M5 45Q28 60 51 42V61H5Z" fill={creature.dark} opacity=".2" /><ellipse cx="21" cy="22" rx="12" ry="9" transform="rotate(-28 21 22)" fill={fill('shine')} /><path d="M13 29Q11 44 22 48" stroke="white" strokeOpacity=".16" strokeWidth="1.3" strokeLinecap="round" /></g>
        {creature.shape === 'sprout' && <g className="auth-creature__antenna"><path d="M28 16Q26 10 29 6" stroke="#478D66" strokeWidth="2" strokeLinecap="round" /><path d="M28 10Q18 11 18 4Q27 3 28 10" fill="#3D956E" /><path d="M29 8Q29 1 37 3Q36 10 29 8" fill="#75C790" /><path d="M20 5L26 8M31 7L35 4" stroke="#C2EDBF" strokeWidth=".8" strokeLinecap="round" /></g>}
        {creature.shape === 'sage' && <g className="auth-character__hijab"><path d="M11 34C9 6 47 5 45 34L41 38L39 25Q28 17 17 26L15 38Z" fill={fill('scarf')} stroke={creature.outline} strokeWidth="1.7" strokeLinejoin="round" /><path d="M17 25Q28 17 39 25L40 35Q28 44 16 35Z" fill="#DAE8BB" /><path d="M13 37Q24 46 43 44L47 49Q24 58 10 47Z" fill={fill('scarf')} /><path className="auth-creature__scarf-edge" d="M15 38Q27 48 39 48L34 54L21 49Z" fill={fill('scarf')} /><path d="M14 23Q15 14 25 14M16 41Q25 47 35 47" stroke="#F1F8E6" strokeOpacity=".55" strokeWidth="1" strokeLinecap="round" /></g>}
        <g className="auth-creature__face">
          <ellipse cx="15" cy="37" rx="3.4" ry="2" fill="#FFF5EB" opacity=".24" /><ellipse cx="41" cy="37" rx="3.4" ry="2" fill="#FFF5EB" opacity=".24" />
          <g className="auth-creature__eyes"><ellipse cx="21" cy="30" rx="4.4" ry="5.2" fill="#FFFEF8" stroke="#20463E" strokeWidth=".9" /><ellipse cx="35" cy="30" rx="4.4" ry="5.2" fill="#FFFEF8" stroke="#20463E" strokeWidth=".9" /><g className="auth-creature__pupils"><ellipse cx="21.3" cy="30.7" rx="2.6" ry="3.3" fill="#20463E" /><ellipse cx="34.7" cy="30.7" rx="2.6" ry="3.3" fill="#20463E" /><circle cx="20.5" cy="29.1" r="1" fill="white" /><circle cx="33.9" cy="29.1" r="1" fill="white" /></g></g>
          <g className="auth-creature__eyelids"><path d="M17 30Q21 33 25 30M31 30Q35 33 39 30" stroke="#2E554B" strokeWidth="1.6" strokeLinecap="round" /></g>
          <g className="auth-creature__mouth">{mode === 'failure' ? <path d="M24 41Q28 38 32 41" stroke="#33574B" strokeWidth="1.5" strokeLinecap="round" /> : creature.mouth === 'o' ? <ellipse className="auth-creature__mouth--o" cx="28" cy="41" rx="2.4" ry="3.1" fill="#315349" /> : creature.mouth === 'grin' ? <g className="auth-creature__mouth--grin"><path d="M21.5 39.5Q28 46 34.5 39.5Z" fill="#315349" /><path d="M25 43Q28 41 31 43" stroke="#E6A29C" strokeWidth="1.3" strokeLinecap="round" /></g> : creature.mouth === 'shy' ? <path className="auth-creature__mouth--shy" d="M25.5 40.5Q28 43 30.5 40.5" stroke="#33574B" strokeWidth="1.5" strokeLinecap="round" fill="none" /> : <g className="auth-creature__mouth--smile"><path d="M24 39Q28 44 32 39" fill="#315349" /><path d="M26 42Q28 40 30 42" stroke="#E6A29C" strokeWidth="1.4" strokeLinecap="round" /></g>}</g>
          {creature.glasses && <g className="auth-character__glasses" stroke="#364A51" strokeWidth="1.6">{creature.glasses === 'round' ? <><circle cx="21" cy="30" r="6.2" fill={fill('lens')} /><circle cx="35" cy="30" r="6.2" fill={fill('lens')} /></> : <><rect x="14.7" y="24.5" width="12.2" height="10" rx="3.3" fill={fill('lens')} /><rect x="29.1" y="24.5" width="12.2" height="10" rx="3.3" fill={fill('lens')} /></>}<path d="M27 29H29M11 28L15 29M41 29L45 28" /><path className="auth-creature__lens-glint" d="M17 28L21 25M31 28L35 25" stroke="white" strokeWidth="1.3" strokeLinecap="round" /></g>}
        </g>
        <g className="auth-creature__mittens" fill={fill('mitten')} stroke={creature.outline} strokeWidth="1.2">{covered ? <g className="auth-creature__privacy-mittens"><path d="M11 44Q10 39 17 30M45 44Q46 39 39 30" stroke={creature.mid} strokeWidth="4.5" strokeLinecap="round" /><ellipse cx="21" cy="29.5" rx="7" ry="5.8" transform="rotate(-15 21 29.5)" /><ellipse cx="35" cy="29.5" rx="7" ry="5.8" transform="rotate(15 35 29.5)" /><path d="M18 27L23 26M33 26L38 27" stroke={creature.light} strokeWidth="1.5" strokeLinecap="round" /></g> : creature.hands === 'puff' ? <g className="auth-creature__hands auth-creature__hands--puff"><g><circle cx="4.8" cy="40" r="2.4" /><circle cx="8.8" cy="38.4" r="2.9" /><circle cx="8.8" cy="41.8" r="2.6" /></g><g className="auth-creature__wave-mitten auth-creature__wave-mitten--puff"><circle cx="44.8" cy="40" r="2.4" /><circle cx="48.8" cy="38.4" r="2.9" /><circle cx="48.8" cy="41.8" r="2.6" /></g></g> : creature.hands === 'nub' ? <g className="auth-creature__hands auth-creature__hands--nub"><rect x="4.4" y="38.6" width="6.4" height="5" rx="2.4" transform="rotate(-14 7.6 41.1)" /><rect className="auth-creature__wave-mitten auth-creature__wave-mitten--nub" x="43.6" y="35.2" width="6.4" height="5" rx="2.4" transform="rotate(16 46.8 37.7)" /></g> : <><ellipse cx="9" cy="41" rx="3.8" ry="5" transform="rotate(16 9 41)" /><g className="auth-creature__wave-mitten"><ellipse className="auth-creature__open-mitten" cx="47" cy="35" rx="4.2" ry="5.5" transform="rotate(24 47 35)" /><path className="auth-creature__thumb-mitten" d="M43 35L43 28Q43 25 45 26Q47 27 46 31Q52 30 51 35L50 39Q44 42 43 35Z" /><path d="M46 32Q48 31 49 33" stroke={creature.light} strokeWidth="1" strokeLinecap="round" /></g></>}</g>
      </g></g>
      <g className="auth-creature__sparkles" fill={creature.mid}><path d="M8 12L9 8L10 12L14 13L10 14L9 18L8 14L4 13Z" /><path d="M46 9L47 6L48 9L51 10L48 11L47 14L46 11L43 10Z" /><circle cx="49" cy="20" r="1.5" /></g>
    </svg>
  );
}
