import React from 'react';
import { readApiErrorDetails } from '../../api/readApiErrorDetails';
import CitizenStatePanel from './CitizenStatePanel';

function errorTitle(status) {
  if (status === 0) return 'Connection unavailable';
  if (status === 403) return 'Access unavailable';
  if (status === 404) return 'Information not found';
  if (status === 409) return 'Request needs attention';
  return 'We could not complete this request';
}

export default function CitizenErrorState({ error, title, actions }) {
  const details = readApiErrorDetails(error);
  if (!details) return null;
  const { status, fieldErrors: fields } = details;
  const message = details.message || 'An unexpected error occurred.';

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
