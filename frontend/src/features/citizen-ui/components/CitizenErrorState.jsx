import React from 'react';
import { PERMISSION_DENIED_MESSAGE } from '../../../services/apiClient';
import CitizenStatePanel from '../../../shared/ui/citizen/CitizenStatePanel';

function errorTitle(status) {
  if (status === 0) return 'Connection unavailable';
  if (status === 403) return 'Access unavailable';
  if (status === 404) return 'Information not found';
  if (status === 409) return 'Request needs attention';
  return 'We could not complete this request';
}

function fieldMessages(fieldErrors) {
  if (Array.isArray(fieldErrors)) return fieldErrors.filter((item) => item?.message);
  if (!fieldErrors || typeof fieldErrors !== 'object') return [];
  return Object.entries(fieldErrors).flatMap(([field, messages]) => (
    (Array.isArray(messages) ? messages : [messages])
      .filter(Boolean)
      .map((message) => ({ field, message }))
  ));
}

export default function CitizenErrorState({ error, title, actions }) {
  if (!error) return null;
  const status = typeof error === 'string' ? null : error.status;
  const message = status === 403
    ? PERMISSION_DENIED_MESSAGE
    : typeof error === 'string'
      ? error
      : error.message || 'An unexpected error occurred.';
  const fields = fieldMessages(error?.data?.fieldErrors);

  return (
    <CitizenStatePanel
      eyebrow="Unable to load"
      title={title || errorTitle(status)}
      tone="error"
      actions={actions}
    >
      <p>{message}</p>
      {fields.length > 0 && (
        <ul className="citizen-error-fields">
          {fields.map((item, index) => (
            <li key={`${item.field}-${index}`}><strong>{item.field}:</strong> {item.message}</li>
          ))}
        </ul>
      )}
    </CitizenStatePanel>
  );
}
