import React, { useState } from 'react';
import ApiErrorAlert from '../../components/common/ApiErrorAlert';
import { REQUESTABLE_STAFF_ROLES } from '../constants/authRoles';
import { ArrowIcon, EyeIcon } from './AuthIcons';

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
  if (!form.email.includes('@') || form.email.startsWith('@') || form.email.endsWith('@')) return 'Please enter a valid email address.';
  if (!form.phoneNumber.trim()) return 'Phone number is required.';
  if (includeStaffRole && !REQUESTABLE_STAFF_ROLES.some(({ code }) => code === form.requestedRoleCode)) return 'Please select a valid staff role.';
  if (!form.password) return 'Password is required.';
  if (form.password.length > 128) return 'Password must not exceed 128 characters.';
  if (form.password !== form.confirmPassword) return 'Passwords do not match.';
  return '';
}

function PasswordField({ accountType, confirmation = false, disabled, invalid, onChange, value, visible, onToggle }) {
  const label = confirmation ? 'Confirm Password' : 'Password';
  const name = confirmation ? 'confirmPassword' : 'password';
  const id = `${accountType}-${confirmation ? 'confirm-password' : 'password'}`;
  const toggleLabel = `${visible ? 'Hide' : 'Show'} ${confirmation ? 'confirm password' : 'password'}`;

  return (
    <div className="auth-field">
      <label htmlFor={id}>{label}</label>
      <div className="auth-password">
        <input
          className="auth-input"
          id={id}
          name={name}
          type={visible ? 'text' : 'password'}
          value={value}
          onChange={onChange}
          maxLength={128}
          autoComplete="new-password"
          disabled={disabled}
          aria-invalid={invalid}
          aria-describedby={invalid ? `${accountType}-registration-error` : undefined}
          placeholder={confirmation ? 'Enter it once more' : 'Create your password'}
          required
        />
        <button type="button" className="auth-password__toggle" onClick={onToggle} aria-label={toggleLabel} disabled={disabled}>
          <EyeIcon hidden={visible} />
        </button>
      </div>
    </div>
  );
}

export default function AccountRegistrationForm({ accountType, apiError, isSubmitting, onDismissError, onSubmit }) {
  const isStaff = accountType === 'staff';
  const [form, setForm] = useState(INITIAL_FORM);
  const [clientError, setClientError] = useState('');
  const [passwordVisibility, setPasswordVisibility] = useState({ password: false, confirmPassword: false });

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

  const hasError = Boolean(clientError || apiError);
  const togglePassword = (field) => setPasswordVisibility((current) => ({ ...current, [field]: !current[field] }));

  return (
    <form className="auth-form auth-registration-form" onSubmit={handleSubmit} noValidate aria-label={`${isStaff ? 'Staff' : 'Citizen'} registration details`}>
      {clientError && (
        <div className="auth-alert auth-alert--error" id={`${accountType}-registration-error`} role="alert">
          <p>{clientError}</p>
        </div>
      )}
      <ApiErrorAlert error={apiError} onDismiss={onDismissError} />

      <div className="auth-form-steps" aria-hidden="true">
        <span className="auth-form-step auth-form-step--current">
          <span className="auth-form-step__num">1</span>
          <span>{isStaff ? 'Identity' : 'Personal'}</span>
        </span>
        <span className="auth-form-step__divider" />
        {isStaff && (
          <>
            <span className="auth-form-step auth-form-step--current">
              <span className="auth-form-step__num">2</span>
              <span>Role</span>
            </span>
            <span className="auth-form-step__divider" />
          </>
        )}
        <span className="auth-form-step auth-form-step--current">
          <span className="auth-form-step__num">{isStaff ? '3' : '2'}</span>
          <span>Security</span>
        </span>
      </div>

      <fieldset className="auth-form-group">
        <legend className="auth-form-group__legend">
          <span className="auth-form-group__num">01</span>
          <span>{isStaff ? 'Identity & Contact Details' : 'Personal Details'}</span>
        </legend>
        <div className="auth-registration-grid">
          <div className="auth-field">
            <label htmlFor={`${accountType}-full-name`}>Full Name</label>
            <input className="auth-input" id={`${accountType}-full-name`} name="fullName" value={form.fullName} onChange={updateField} maxLength={255} autoComplete="name" disabled={isSubmitting} aria-invalid={hasError} placeholder="Your full name" required />
          </div>

          <div className="auth-field">
            <label htmlFor={`${accountType}-email`}>Email Address</label>
            <input className="auth-input" id={`${accountType}-email`} name="email" type="email" value={form.email} onChange={updateField} maxLength={255} autoComplete="email" inputMode="email" disabled={isSubmitting} aria-invalid={hasError} placeholder="name@example.gov" required />
          </div>

          <div className="auth-field auth-field--full">
            <label htmlFor={`${accountType}-phone`}>Phone Number</label>
            <input className="auth-input" id={`${accountType}-phone`} name="phoneNumber" type="tel" value={form.phoneNumber} onChange={updateField} maxLength={32} autoComplete="tel" inputMode="tel" disabled={isSubmitting} aria-invalid={hasError} placeholder="Your contact number" required />
          </div>
        </div>
      </fieldset>

      {isStaff && (
        <fieldset className="auth-form-group">
          <legend className="auth-form-group__legend">
            <span className="auth-form-group__num">02</span>
            <span>Role Assignment</span>
          </legend>
          <div className="auth-registration-grid">
            <div className="auth-field auth-field--full">
              <label htmlFor="staff-requested-role">Requested Staff Role</label>
              <select className="auth-input auth-select" id="staff-requested-role" name="requestedRoleCode" value={form.requestedRoleCode} onChange={updateField} disabled={isSubmitting} required>
                {REQUESTABLE_STAFF_ROLES.map(({ code, label }) => <option key={code} value={code}>{label}</option>)}
              </select>
            </div>
          </div>
        </fieldset>
      )}

      <fieldset className="auth-form-group">
        <legend className="auth-form-group__legend">
          <span className="auth-form-group__num">{isStaff ? '03' : '02'}</span>
          <span>Account Security</span>
        </legend>
        <div className="auth-registration-grid">
          <PasswordField accountType={accountType} value={form.password} onChange={updateField} disabled={isSubmitting} invalid={hasError} visible={passwordVisibility.password} onToggle={() => togglePassword('password')} />
          <PasswordField accountType={accountType} confirmation value={form.confirmPassword} onChange={updateField} disabled={isSubmitting} invalid={hasError} visible={passwordVisibility.confirmPassword} onToggle={() => togglePassword('confirmPassword')} />
        </div>
      </fieldset>

      <button className="auth-primary-action" type="submit" disabled={isSubmitting}>
        <span className="auth-primary-action__label">
          {isSubmitting && <span className="auth-spinner" aria-hidden="true" />}
          {isSubmitting ? (isStaff ? 'Submitting request…' : 'Creating account…') : (isStaff ? 'Submit Staff Account Request' : 'Create Citizen Account')}
        </span>
        {!isSubmitting && <ArrowIcon />}
      </button>
    </form>
  );
}
