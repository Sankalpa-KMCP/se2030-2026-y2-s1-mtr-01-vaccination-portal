import React, { useState } from 'react';
import ApiErrorAlert from '../../components/common/ApiErrorAlert';
import { REQUESTABLE_STAFF_ROLES } from '../constants/authRoles';

const INITIAL_FORM = {
  fullName: '',
  email: '',
  phoneNumber: '',
  requestedRoleCode: REQUESTABLE_STAFF_ROLES[0].code,
  password: '',
  confirmPassword: '',
};

function validate(form, includeStaffRole) {
  if (!form.fullName.trim()) return 'Full name is required.';
  if (!form.email.trim()) return 'Email address is required.';
  if (!form.email.includes('@') || form.email.startsWith('@') || form.email.endsWith('@')) {
    return 'Please enter a valid email address.';
  }
  if (!form.phoneNumber.trim()) return 'Phone number is required.';
  if (includeStaffRole && !REQUESTABLE_STAFF_ROLES.some(({ code }) => code === form.requestedRoleCode)) {
    return 'Please select a valid staff role.';
  }
  if (!form.password) return 'Password is required.';
  if (form.password.length > 128) return 'Password must not exceed 128 characters.';
  if (form.password !== form.confirmPassword) return 'Passwords do not match.';
  return '';
}

export default function AccountRegistrationForm({
  accountType,
  apiError,
  isSubmitting,
  onDismissError,
  onSubmit,
}) {
  const isStaff = accountType === 'staff';
  const [form, setForm] = useState(INITIAL_FORM);
  const [clientError, setClientError] = useState('');

  const updateField = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setClientError('');
    onDismissError?.();
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    const validationError = validate(form, isStaff);
    if (validationError) {
      setClientError(validationError);
      return;
    }

    onSubmit({
      fullName: form.fullName.trim(),
      email: form.email.trim(),
      phoneNumber: form.phoneNumber.trim(),
      password: form.password,
      ...(isStaff ? { requestedRoleCode: form.requestedRoleCode } : {}),
    });
  };

  return (
    <form onSubmit={handleSubmit} noValidate>
      {clientError && <div className="alert alert-danger" role="alert">{clientError}</div>}
      <ApiErrorAlert error={apiError} onDismiss={onDismissError} />

      <div className="mb-3">
        <label className="form-label" htmlFor={`${accountType}-full-name`}>Full Name</label>
        <input
          className="form-control"
          id={`${accountType}-full-name`}
          name="fullName"
          value={form.fullName}
          onChange={updateField}
          maxLength={255}
          autoComplete="name"
          disabled={isSubmitting}
          required
        />
      </div>

      <div className="mb-3">
        <label className="form-label" htmlFor={`${accountType}-email`}>Email Address</label>
        <input
          className="form-control"
          id={`${accountType}-email`}
          name="email"
          type="email"
          value={form.email}
          onChange={updateField}
          maxLength={255}
          autoComplete="email"
          disabled={isSubmitting}
          required
        />
      </div>

      <div className="mb-3">
        <label className="form-label" htmlFor={`${accountType}-phone`}>Phone Number</label>
        <input
          className="form-control"
          id={`${accountType}-phone`}
          name="phoneNumber"
          type="tel"
          value={form.phoneNumber}
          onChange={updateField}
          maxLength={32}
          autoComplete="tel"
          disabled={isSubmitting}
          required
        />
      </div>

      {isStaff && (
        <div className="mb-3">
          <label className="form-label" htmlFor="staff-requested-role">Requested Staff Role</label>
          <select
            className="form-select"
            id="staff-requested-role"
            name="requestedRoleCode"
            value={form.requestedRoleCode}
            onChange={updateField}
            disabled={isSubmitting}
            required
          >
            {REQUESTABLE_STAFF_ROLES.map(({ code, label }) => (
              <option key={code} value={code}>{label}</option>
            ))}
          </select>
          <div className="form-text">The requested role is granted only after administrator approval.</div>
        </div>
      )}

      <div className="row g-3 mb-4">
        <div className="col-md-6">
          <label className="form-label" htmlFor={`${accountType}-password`}>Password</label>
          <input
            className="form-control"
            id={`${accountType}-password`}
            name="password"
            type="password"
            value={form.password}
            onChange={updateField}
            maxLength={128}
            autoComplete="new-password"
            disabled={isSubmitting}
            required
          />
        </div>
        <div className="col-md-6">
          <label className="form-label" htmlFor={`${accountType}-confirm-password`}>Confirm Password</label>
          <input
            className="form-control"
            id={`${accountType}-confirm-password`}
            name="confirmPassword"
            type="password"
            value={form.confirmPassword}
            onChange={updateField}
            maxLength={128}
            autoComplete="new-password"
            disabled={isSubmitting}
            required
          />
        </div>
      </div>

      <div className="d-grid">
        <button className="btn btn-primary py-2" type="submit" disabled={isSubmitting}>
          {isSubmitting && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}
          {isSubmitting
            ? (isStaff ? 'Submitting request...' : 'Creating account...')
            : (isStaff ? 'Submit Staff Account Request' : 'Create Citizen Account')}
        </button>
      </div>
    </form>
  );
}
