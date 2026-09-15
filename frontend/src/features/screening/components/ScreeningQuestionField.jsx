import React from 'react';
import { QUESTION_TYPE } from '../constants/screeningConstants';

function orderedOptions(options = []) {
  return [...options].sort((left, right) => left.displayOrder - right.displayOrder);
}

export default function ScreeningQuestionField({ question, value, onChange, error }) {
  const fieldId = `screening-question-${question.id}`;
  const errorId = `${fieldId}-error`;
  const describedBy = [question.description ? `${fieldId}-description` : null, error ? errorId : null]
    .filter(Boolean)
    .join(' ') || undefined;
  const commonProps = {
    'aria-describedby': describedBy,
    'aria-invalid': Boolean(error),
  };

  const questionLabel = (
    <>
      {question.text}
      {question.required && <span className="text-danger ms-1" aria-label="required">*</span>}
    </>
  );

  let control;
  switch (question.questionType) {
    case QUESTION_TYPE.BOOLEAN:
      control = (
        <fieldset {...commonProps}>
          <legend className="h6 mb-2">{questionLabel}</legend>
          <div className="d-flex gap-4">
            {['true', 'false'].map((optionValue) => (
              <div className="form-check" key={optionValue}>
                <input
                  className="form-check-input"
                  type="radio"
                  id={`${fieldId}-${optionValue}`}
                  name={fieldId}
                  value={optionValue}
                  checked={value === optionValue}
                  onChange={(event) => onChange(event.target.value)}
                />
                <label className="form-check-label" htmlFor={`${fieldId}-${optionValue}`}>
                  {optionValue === 'true' ? 'Yes' : 'No'}
                </label>
              </div>
            ))}
          </div>
        </fieldset>
      );
      break;
    case QUESTION_TYPE.SINGLE_CHOICE:
      control = (
        <fieldset {...commonProps}>
          <legend className="h6 mb-2">{questionLabel}</legend>
          {orderedOptions(question.options).map((option) => (
            <div className="form-check" key={option.id}>
              <input
                className="form-check-input"
                type="radio"
                id={`${fieldId}-${option.id}`}
                name={fieldId}
                value={option.code}
                checked={value === option.code}
                onChange={(event) => onChange(event.target.value)}
              />
              <label className="form-check-label" htmlFor={`${fieldId}-${option.id}`}>{option.label}</label>
            </div>
          ))}
        </fieldset>
      );
      break;
    case QUESTION_TYPE.MULTI_CHOICE: {
      const selectedValues = Array.isArray(value) ? value : [];
      control = (
        <fieldset {...commonProps}>
          <legend className="h6 mb-2">{questionLabel}</legend>
          {orderedOptions(question.options).map((option) => (
            <div className="form-check" key={option.id}>
              <input
                className="form-check-input"
                type="checkbox"
                id={`${fieldId}-${option.id}`}
                value={option.code}
                checked={selectedValues.includes(option.code)}
                onChange={(event) => {
                  const nextValues = event.target.checked
                    ? [...selectedValues, option.code]
                    : selectedValues.filter((selected) => selected !== option.code);
                  onChange(nextValues);
                }}
              />
              <label className="form-check-label" htmlFor={`${fieldId}-${option.id}`}>{option.label}</label>
            </div>
          ))}
        </fieldset>
      );
      break;
    }
    case QUESTION_TYPE.NUMERIC:
      control = (
        <div>
          <label className="form-label fw-semibold" htmlFor={fieldId}>{questionLabel}</label>
          <input
            {...commonProps}
            className="form-control"
            id={fieldId}
            type="number"
            step="any"
            value={value ?? ''}
            onChange={(event) => onChange(event.target.value)}
          />
        </div>
      );
      break;
    case QUESTION_TYPE.DATE:
      control = (
        <div>
          <label className="form-label fw-semibold" htmlFor={fieldId}>{questionLabel}</label>
          <input
            {...commonProps}
            className="form-control"
            id={fieldId}
            type="date"
            value={value ?? ''}
            onChange={(event) => onChange(event.target.value)}
          />
        </div>
      );
      break;
    case QUESTION_TYPE.TEXT:
      control = (
        <div>
          <label className="form-label fw-semibold" htmlFor={fieldId}>{questionLabel}</label>
          <textarea
            {...commonProps}
            className="form-control"
            id={fieldId}
            rows="3"
            value={value ?? ''}
            onChange={(event) => onChange(event.target.value)}
          />
        </div>
      );
      break;
    default:
      control = <p className="text-danger mb-0">This question type is not supported.</p>;
  }

  return (
    <div className="card border-0 bg-light mb-3">
      <div className="card-body">
        {control}
        {question.description && (
          <p className="form-text mb-0" id={`${fieldId}-description`}>{question.description}</p>
        )}
        {error && <div className="invalid-feedback d-block" id={errorId}>{error}</div>}
      </div>
    </div>
  );
}
