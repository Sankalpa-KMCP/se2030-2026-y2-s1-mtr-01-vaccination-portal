import { PERMISSION_DENIED_MESSAGE } from '../../services/apiClient';

// Extract response facts without choosing a component's title, fallback text, or markup.
export function readApiErrorDetails(error) {
  if (!error) return null;

  const status = typeof error === 'string' ? null : error.status;
  const message = status === 403
    ? PERMISSION_DENIED_MESSAGE
    : typeof error === 'string'
      ? error
      : error.message;
  const fieldErrors = error?.data?.fieldErrors;
  const fields = Array.isArray(fieldErrors)
    ? fieldErrors.filter((item) => item?.message)
    : !fieldErrors || typeof fieldErrors !== 'object'
      ? []
      : Object.entries(fieldErrors).flatMap(([field, messages]) => (
        (Array.isArray(messages) ? messages : [messages])
          .filter(Boolean)
          .map((fieldMessage) => ({ field, message: fieldMessage }))
      ));

  return { status, message, fieldErrors: fields };
}
