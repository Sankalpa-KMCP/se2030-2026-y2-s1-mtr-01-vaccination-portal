import React from 'react';

const WORKFLOW_STEPS = [
  {
    step: 1,
    title: 'Review submissions',
    description: 'Assess citizen screening responses and reported clinical indicators.',
  },
  {
    step: 2,
    title: 'Request corrections when needed',
    description: 'Provide clear clinical instructions if answers require clarification.',
  },
  {
    step: 3,
    title: 'Record eligibility decisions',
    description: 'Approve or record clinical ineligibility with reasoned justification.',
  },
  {
    step: 4,
    title: 'Maintain screening content',
    description: 'Ensure questions, answer options, and questionnaire rules are up to date.',
  },
];

export default function HcoWorkflowGuide() {
  return (
    <div className="hco-card">
      <div className="hco-card-header">
        <h2 className="hco-card-title">
          <i className="bi bi-journal-medical hco-card-title-icon" aria-hidden="true" />
          <span>HCO Workflow & Guidance</span>
        </h2>
      </div>

      <div className="hco-card-body">
        <div className="row g-3 align-items-center">
          {/* Left: 4 Workflow Steps */}
          <div className="col-12 col-lg-8">
            <div className="row g-2">
              {WORKFLOW_STEPS.map((item) => (
                <div key={item.step} className="col-12 col-md-6">
                  <div className="d-flex align-items-start">
                    <span className="hco-workflow-step-num" aria-hidden="true">
                      {item.step}
                    </span>
                    <div>
                      <div className="hco-workflow-step-title">{item.title}</div>
                      <div className="hco-workflow-step-desc">{item.description}</div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Right: Compact Mint Assurance Panel */}
          <div className="col-12 col-lg-4">
            <div className="hco-assurance-panel d-flex align-items-start gap-3">
              <i className="bi bi-shield-check hco-assurance-icon" aria-hidden="true" />
              <div>
                <div className="hco-assurance-title">Supporting Safe Vaccination</div>
                <p className="hco-assurance-desc">
                  Your clinical reviews uphold safe, accurate, and consistent immunization screening across all active campaigns.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
