import React from 'react';

/**
 * Shared compact editorial opener for interior public pages.
 * Kicker index + label, oversized serif title, single-line lead.
 */
export default function PageHero({ index, label, title, accent, lead, children }) {
  return (
    <section className="lp-page-hero" aria-labelledby="lp-page-hero-title">
      <div className="lp-container">
        <div className="lp-page-hero__inner">
          <div className="lp-kicker">
            <span className="lp-kicker__index">{index}</span>
            <span className="lp-kicker__label">{label}</span>
            <span className="lp-kicker__rule" aria-hidden="true" />
          </div>
          <h1 className="lp-page-hero__title" id="lp-page-hero-title">
            {title}
            {accent ? (
              <>
                {' '}
                <span className="lp-hero__accent">{accent}</span>
              </>
            ) : null}
          </h1>
          {lead ? <p className="lp-page-hero__lead">{lead}</p> : null}
          {children}
        </div>
      </div>
    </section>
  );
}
