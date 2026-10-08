import React from 'react';

const PRINCIPLES = [
  {
    num: '01',
    title: 'Access follows responsibility.',
    desc: 'Citizens, Healthcare Officers, Center Managers, Campaign Officers, Inventory Officers, Management Users, and Administrators enter role-specific areas.',
  },
  {
    num: '02',
    title: 'Sessions are checked before they open.',
    desc: 'Operating hours, staffing, resources, conflicts, and calculated capacity are part of session readiness.',
  },
  {
    num: '03',
    title: 'Operational changes leave history.',
    desc: 'Key campaign and session lifecycle changes record actor, reason, timestamp, and transition details.',
  },
  {
    num: '04',
    title: 'Eligibility remains a clinical decision.',
    desc: 'Screening provides a preliminary result; an authorized healthcare officer records the final campaign-specific assessment.',
  },
];

export default function TrustSection() {
  const [activeVisual, setActiveVisual] = React.useState('coldchain');
  return (
    <section
      className="home-assurance"
      id="accountability"
      aria-labelledby="lp-trust-title"
    >
      <div className="lp-container home-assurance__layout">
        <div className="home-assurance__intro" data-reveal="section">
          <span className="home-section-mark">Operational integrity</span>
          <h2 id="lp-trust-title">Trust is built into the work, not added as a claim.</h2>
          <p>
            The portal makes responsibility visible where access, capacity,
            eligibility, and session readiness are managed.
          </p>

          <div className="home-assurance__showcase">
            <div className="assurance-tabs">
              <button
                type="button"
                className={`assurance-tab ${activeVisual === 'coldchain' ? 'is-active' : ''}`}
                onClick={() => setActiveVisual('coldchain')}
              >
                <i className="bi bi-thermometer-snow me-1" /> Active Cold Chain 4.1°C
              </button>
              <button
                type="button"
                className={`assurance-tab ${activeVisual === 'community' ? 'is-active' : ''}`}
                onClick={() => setActiveVisual('community')}
              >
                <i className="bi bi-building-check me-1" /> Accessible Clinic Centers
              </button>
            </div>

            <div className="home-assurance__media">
              {activeVisual === 'coldchain' ? (
                <>
                  <img
                    src="/images/vaccine-safety-coldchain.jpg"
                    alt="Hospital pharmaceutical cold-chain vaccine storage laboratory"
                    className="home-assurance__img"
                    loading="lazy"
                  />
                  <div className="home-assurance__caption">
                    <div className="d-flex align-items-center gap-2 mb-1 flex-wrap">
                      <span className="badge bg-white text-dark fw-bold">
                        <i className="bi bi-thermometer-snow text-primary me-1" /> Active Cold Chain 4.1°C
                      </span>
                      <span className="badge bg-warning text-dark fw-bold">
                        <i className="bi bi-shield-check me-1" /> Continuous Telemetry Certified
                      </span>
                    </div>
                    <p className="small text-white-50 mb-0">Pharmaceutical storage protocols and continuous temperature telemetry ensure vaccine viability.</p>
                  </div>
                </>
              ) : (
                <>
                  <img
                    src="/images/community-vax-clinic.jpg"
                    alt="Welcoming community clinic with friendly staff greeting senior citizen"
                    className="home-assurance__img"
                    loading="lazy"
                  />
                  <div className="home-assurance__caption">
                    <div className="d-flex align-items-center gap-2 mb-1 flex-wrap">
                      <span className="badge bg-success text-white fw-bold">
                        <i className="bi bi-heart-pulse-fill me-1" /> Universal Age Accessibility
                      </span>
                      <span className="badge bg-info text-dark fw-bold">
                        <i className="bi bi-person-wheelchair me-1" /> Step-Free Access
                      </span>
                    </div>
                    <p className="small text-white-50 mb-0">Designed for everyone: wheelchair ramps, priority seating for seniors, and child-friendly bays.</p>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>

        <ol className="home-assurance__register" data-reveal="assurance">
          {PRINCIPLES.map((item) => (
            <li key={item.num}>
              <span className="home-assurance__number" aria-hidden="true">{item.num}</span>
              <div>
                <h3>{item.title}</h3>
                <p>{item.desc}</p>
              </div>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
