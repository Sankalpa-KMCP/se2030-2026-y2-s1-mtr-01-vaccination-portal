import React from 'react';
import './citizen-ui.css';

export default function CitizenWorkflowGuide({ titleId, steps }) {
  return (
    <section className="citizen-workflow-guide" aria-labelledby={titleId}>
      <p className="citizen-workflow-guide__label" id={titleId}>How it works</p>
      <ol aria-label="Service sequence">
        {steps.map((step, index) => (
          <li key={step}>
            <span aria-hidden="true">{index + 1}</span>
            <strong>{step}</strong>
          </li>
        ))}
      </ol>
    </section>
  );
}
