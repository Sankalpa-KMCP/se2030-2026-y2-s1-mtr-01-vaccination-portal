import React from 'react';
import DashboardIcon from './DashboardIcon';

/**
 * Derives current step statuses ('completed', 'current', 'upcoming')
 * strictly from existing dashboardData without inventing new state.
 */
function deriveJourneySteps(dashboardData) {
  const { appointments, screening } = dashboardData || {};
  const submissions = Array.isArray(screening?.submissions) ? screening.submissions : [];
  const assessments = Array.isArray(screening?.assessments) ? screening.assessments : [];
  const appointmentList = Array.isArray(appointments?.data) ? appointments.data : [];

  const hasSubmission = submissions.length > 0;
  const isPendingReview = submissions.some(
    (s) => s?.status === 'PENDING_REVIEW' || s?.status === 'CORRECTIONS_REQUIRED',
  );
  const isReviewed = submissions.some((s) => s?.status === 'REVIEWED');

  const hasAssessment = assessments.length > 0;
  const isEligible = assessments.some((a) => a?.status === 'ELIGIBLE');

  const activeAppointment = appointmentList.find(
    (a) => a?.status === 'CONFIRMED' || a?.status === 'CHECKED_IN',
  );
  const hasCompletedAppointment = appointmentList.some((a) => a?.status === 'COMPLETED');

  // Step 1: Screening
  let step1Status = 'upcoming';
  let step1Detail = 'Start questionnaire';
  if (hasSubmission) {
    if (isPendingReview && !isReviewed) {
      step1Status = 'current';
      step1Detail = 'In review';
    } else {
      step1Status = 'completed';
      step1Detail = 'Completed';
    }
  } else {
    step1Status = 'current';
    step1Detail = 'Start screening';
  }

  // Step 2: Eligibility
  let step2Status = 'upcoming';
  let step2Detail = 'Awaiting screening';
  if (isEligible) {
    step2Status = 'completed';
    step2Detail = 'Verified';
  } else if (hasAssessment) {
    step2Status = 'completed';
    step2Detail = 'Decision recorded';
  } else if (hasSubmission) {
    step2Status = 'current';
    step2Detail = 'Pending review';
  } else {
    step2Status = 'upcoming';
    step2Detail = 'Awaiting screening';
  }

  // Step 3: Appointment
  let step3Status = 'upcoming';
  let step3Detail = 'Not booked';
  if (hasCompletedAppointment) {
    step3Status = 'completed';
    step3Detail = 'Attended';
  } else if (activeAppointment) {
    step3Status = 'current';
    step3Detail = 'Booked';
  } else if (isEligible) {
    step3Status = 'current';
    step3Detail = 'Ready to book';
  } else {
    step3Status = 'upcoming';
    step3Detail = 'Not booked';
  }

  // Step 4: Vaccination
  let step4Status = 'upcoming';
  let step4Detail = 'Pending';
  if (hasCompletedAppointment) {
    step4Status = 'completed';
    step4Detail = 'Administered';
  } else if (activeAppointment) {
    step4Status = 'upcoming';
    step4Detail = 'Scheduled';
  } else {
    step4Status = 'upcoming';
    step4Detail = 'Pending';
  }

  return [
    { id: 'screening', number: 1, label: 'Screening', status: step1Status, detail: step1Detail },
    { id: 'eligibility', number: 2, label: 'Eligibility', status: step2Status, detail: step2Detail },
    { id: 'appointment', number: 3, label: 'Appointment', status: step3Status, detail: step3Detail },
    { id: 'vaccination', number: 4, label: 'Vaccination', status: step4Status, detail: step4Detail },
  ];
}

export default function VaccinationJourneyStepper({ dashboardData }) {
  const steps = deriveJourneySteps(dashboardData);

  return (
    <section className="citizen-journey-card citizen-panel" aria-label="Vaccination journey pathway">
      <header className="citizen-journey-card__header">
        <div className="citizen-journey-card__heading-group">
          <span className="citizen-journey-card__icon" aria-hidden="true">
            <DashboardIcon name="clipboard" size={20} />
          </span>
          <div>
            <h2 className="citizen-journey-card__title">Your Vaccination Journey</h2>
            <p className="citizen-journey-card__subtitle">
              Track your progress from screening to vaccination.
            </p>
          </div>
        </div>
        <span className="citizen-journey-card__tagline" aria-hidden="true">
          Small steps. A healthier tomorrow.
        </span>
      </header>

      <ol className="citizen-journey-card__track">
        {steps.map((step, index) => {
          const isDone = step.status === 'completed';
          const isPrevDone = index > 0 && steps[index - 1].status === 'completed';

          return (
            <li
              key={step.id}
              className={`citizen-journey-node citizen-journey-node--${step.status}`}
              aria-current={step.status === 'current' ? 'step' : undefined}
            >
              {index > 0 && (
                <span
                  className={`citizen-journey-node__connector ${isPrevDone ? 'citizen-journey-node__connector--done' : ''}`}
                  aria-hidden="true"
                />
              )}
              <span className="citizen-journey-node__indicator" aria-hidden="true">
                {isDone ? '✓' : step.number}
              </span>
              <div className="citizen-journey-node__text">
                <strong className="citizen-journey-node__label">{step.label}</strong>
                <span className="citizen-journey-node__detail">{step.detail}</span>
              </div>
            </li>
          );
        })}
      </ol>
    </section>
  );
}
