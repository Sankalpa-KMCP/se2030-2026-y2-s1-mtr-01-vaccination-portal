const ACTIVE_APPOINTMENT_STATUSES = new Set(['CONFIRMED', 'CHECKED_IN']);

function timestampOf(record, fields) {
  for (const field of fields) {
    const timestamp = Date.parse(record?.[field]);
    if (Number.isFinite(timestamp)) return timestamp;
  }
  return Number.NEGATIVE_INFINITY;
}

export function selectLatest(records, fields) {
  if (!Array.isArray(records) || records.length === 0) return null;
  return records.reduce((latest, candidate) => (
    timestampOf(candidate, fields) > timestampOf(latest, fields) ? candidate : latest
  ));
}

export function findActiveAppointment(appointments) {
  return (Array.isArray(appointments) ? appointments : [])
    .find((appointment) => ACTIVE_APPOINTMENT_STATUSES.has(appointment?.status)) || null;
}

function recordedDecisionTitle(status) {
  switch (status) {
    case 'ELIGIBLE': return 'Review your recorded eligibility';
    case 'INELIGIBLE': return 'Review your eligibility decision';
    case 'EXPIRED': return 'Review your expired eligibility record';
    case 'REVOKED': return 'Review your updated eligibility record';
    default: return 'Review your eligibility records';
  }
}

export function getNextAction({ appointments, screening }) {
  const submissionsAvailable = !screening.loading && !screening.errors?.submissions;
  const assessmentsAvailable = !screening.loading && !screening.errors?.assessments;
  const latestSubmission = submissionsAvailable
    ? selectLatest(screening.submissions, ['createdAt', 'submittedAt'])
    : null;
  const latestAssessment = assessmentsAvailable
    ? selectLatest(screening.assessments, ['reviewedAt', 'createdAt'])
    : null;

  if (latestSubmission?.status === 'CORRECTIONS_REQUIRED') {
    return {
      eyebrow: 'Action required',
      title: 'Review your screening correction request',
      description: 'A healthcare reviewer requested clarification. Review the recorded instructions; correction submission is not available from this dashboard.',
      to: `/app/screening/history/${latestSubmission.id}`,
      linkLabel: 'Review correction request',
      tone: 'attention',
    };
  }

  if (latestSubmission?.status === 'PENDING_REVIEW') {
    return {
      eyebrow: 'Screening in progress',
      title: 'Your screening is awaiting review',
      description: 'No new medical action is implied. You can review the submitted screening while the recorded decision is pending.',
      to: `/app/screening/history/${latestSubmission.id}`,
      linkLabel: 'View submitted screening',
      tone: 'pending',
    };
  }

  if (latestAssessment) {
    return {
      eyebrow: 'Recorded eligibility',
      title: recordedDecisionTitle(latestAssessment.status),
      description: 'Open My Eligibility to distinguish the recorded decision from your current campaign eligibility.',
      to: '/app/screening/eligibility',
      linkLabel: 'View My Eligibility',
      tone: 'ready',
    };
  }

  const activeAppointment = !appointments.loading && !appointments.error
    ? findActiveAppointment(appointments.data)
    : null;

  if (activeAppointment) {
    return {
      eyebrow: 'Appointment on file',
      title: 'Review your active appointment',
      description: `Appointment #${activeAppointment.id} is recorded as ${formatStatus(activeAppointment.status)}. Open your appointments for the available details and controls.`,
      to: '/app/appointments/mine',
      linkLabel: 'View My Appointments',
      tone: 'ready',
    };
  }

  if (submissionsAvailable && screening.submissions.length === 0) {
    return {
      eyebrow: 'Start here',
      title: 'Complete pre-vaccination screening',
      description: 'Choose an available campaign and submit its screening questionnaire before booking where eligibility is required.',
      to: '/app/screening',
      linkLabel: 'Choose a screening campaign',
      tone: 'default',
    };
  }

  if (appointments.loading || screening.loading) {
    return {
      eyebrow: 'Checking your records',
      title: 'Finding your next available action',
      description: 'Your citizen services remain available below while account records load.',
      to: null,
      linkLabel: null,
      tone: 'loading',
    };
  }

  return {
    eyebrow: 'Citizen services',
    title: 'Choose the service you need',
    description: 'Some records could not be confirmed, so no personal status has been inferred. Use the verified service links below.',
    to: '/app/screening',
    linkLabel: 'View screening options',
    tone: 'default',
  };
}

export function formatStatus(status) {
  if (!status) return 'Status unavailable';
  return status.toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function formatRecordedDate(value) {
  if (!value) return 'Not available';
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? 'Not available'
    : new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}
