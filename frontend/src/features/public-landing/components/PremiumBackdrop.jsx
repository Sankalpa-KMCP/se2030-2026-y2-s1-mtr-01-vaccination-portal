import React from 'react';

/**
 * A shared, non-interactive atmosphere for the public experience.
 * The layers are deliberately geometric: registration marks, a measured grid,
 * and two softly lit planes. They make the pages feel like a designed civic
 * instrument without introducing stock imagery or distracting from content.
 */
export default function PremiumBackdrop() {
  return (
    <div className="premium-atmosphere" aria-hidden="true">
      <div className="premium-atmosphere__paper" />
      <div className="premium-atmosphere__grid" />
      <div className="premium-atmosphere__plane premium-atmosphere__plane--north" />
      <div className="premium-atmosphere__plane premium-atmosphere__plane--south" />
      <div className="premium-atmosphere__registration premium-atmosphere__registration--one">
        <span />
        <span />
      </div>
      <div className="premium-atmosphere__registration premium-atmosphere__registration--two">
        <span />
        <span />
      </div>
      <div className="premium-atmosphere__grain" />
    </div>
  );
}
