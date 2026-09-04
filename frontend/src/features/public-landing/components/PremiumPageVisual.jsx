import React from 'react';

const howNodes = [
  { code: '01', label: 'Register', x: 36, y: 50 },
  { code: '02', label: 'Screen', x: 126, y: 106 },
  { code: '03', label: 'Book', x: 220, y: 58 },
  { code: '04', label: 'Attend', x: 310, y: 116 },
  { code: '05', label: 'Record', x: 402, y: 66 },
];

const aboutRoles = [
  { code: 'C', label: 'Citizen', level: 0 },
  { code: 'H', label: 'Health officer', level: 1 },
  { code: 'M', label: 'Center manager', level: 2 },
  { code: 'P', label: 'Campaign officer', level: 3 },
  { code: 'O', label: 'Management', level: 4 },
  { code: 'A', label: 'Administrator', level: 5 },
];

function InstrumentCorners() {
  return (
    <>
      <i className="premium-visual__corner premium-visual__corner--tl" />
      <i className="premium-visual__corner premium-visual__corner--tr" />
      <i className="premium-visual__corner premium-visual__corner--bl" />
      <i className="premium-visual__corner premium-visual__corner--br" />
    </>
  );
}

function JourneyInstrument() {
  return (
    <div className="premium-visual premium-visual--journey" data-tilt data-tilt-strength="6">
      <div className="premium-visual__shadow" />
      <div className="premium-visual__underlay premium-visual__underlay--one" />
      <div className="premium-visual__underlay premium-visual__underlay--two" />
      <div className="premium-visual__face">
        <InstrumentCorners />
        <div className="premium-visual__toolbar">
          <span>Journey protocol</span>
          <span className="premium-visual__status"><i /> Live sequence</span>
        </div>
        <div className="premium-journey-map">
          <svg viewBox="0 0 470 180" role="presentation" focusable="false">
            <path className="premium-journey-map__guide" d="M36 50C82 50 82 106 126 106S176 58 220 58s46 58 90 58 48-50 92-50" />
            <path className="premium-journey-map__route" pathLength="1" d="M36 50C82 50 82 106 126 106S176 58 220 58s46 58 90 58 48-50 92-50" />
          </svg>
          {howNodes.map((node, index) => (
            <div
              className="premium-journey-map__node"
              key={node.code}
              style={{ '--node-x': `${node.x}px`, '--node-y': `${node.y}px`, '--node-i': index }}
            >
              <span>{node.code}</span>
              <small>{node.label}</small>
            </div>
          ))}
        </div>
        <div className="premium-visual__metrics">
          <span><b>05</b> connected moments</span>
          <span><b>02</b> coordinated journeys</span>
          <span><b>01</b> secure record</span>
        </div>
      </div>
      <div className="premium-visual__edge"><span>CARE / 01—05</span></div>
    </div>
  );
}

function RegistryInstrument() {
  return (
    <div className="premium-visual premium-visual--registry" data-tilt data-tilt-strength="5">
      <div className="premium-visual__shadow" />
      <div className="premium-visual__underlay premium-visual__underlay--one" />
      <div className="premium-visual__underlay premium-visual__underlay--two" />
      <div className="premium-visual__face">
        <InstrumentCorners />
        <div className="premium-visual__toolbar">
          <span>Responsibility matrix</span>
          <span className="premium-visual__status"><i /> Role scoped</span>
        </div>
        <div className="premium-registry-stack">
          {aboutRoles.map((role, index) => (
            <div
              className="premium-registry-card"
              key={role.code}
              style={{ '--role-i': index, '--role-level': role.level }}
            >
              <span className="premium-registry-card__code">{role.code}</span>
              <span className="premium-registry-card__label">{role.label}</span>
              <span className="premium-registry-card__scope">0{index + 1}</span>
            </div>
          ))}
          <div className="premium-registry-stack__spine" />
        </div>
        <div className="premium-visual__metrics">
          <span><b>06</b> defined roles</span>
          <span><b>05</b> domains</span>
          <span><b>100%</b> traceable</span>
        </div>
      </div>
      <div className="premium-visual__edge"><span>AUTHORITY / CONTROL</span></div>
    </div>
  );
}

export default function PremiumPageVisual({ variant = 'journey' }) {
  return variant === 'about' ? <RegistryInstrument /> : <JourneyInstrument />;
}
