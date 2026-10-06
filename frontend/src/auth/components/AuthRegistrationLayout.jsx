import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowIcon, SuccessSealIcon } from './AuthIcons';

export function AuthRegistrationLayout({ audience, children }) {
  const isStaff = audience === 'staff';
  return (
    <article className={`auth-panel auth-panel--registration auth-panel--${audience}`}>
      <header className="auth-panel__header">
        <p className="auth-panel__eyebrow">{isStaff ? 'Staff registration' : 'Citizen registration'}</p>
        <h1 tabIndex="-1">{isStaff ? 'Request staff access' : 'Create your account'}</h1>
        <p className="auth-panel__description">
          {isStaff ? 'Submit your details and requested role for administrator review.' : 'Register for citizen vaccination services.'}
        </p>
        {isStaff && <p className="auth-approval-note">Approval is required before your account becomes active.</p>}
      </header>

      {children}

      <footer className="auth-panel__footer">
        <span>{isStaff ? 'Already approved?' : 'Already registered?'}</span>
        <Link to="/login">Sign in</Link>
      </footer>
    </article>
  );
}

export function AuthSuccessState({ audience }) {
  const isStaff = audience === 'staff';
  return (
    <section className="auth-success" role="status">
      <div className="auth-success__seal"><SuccessSealIcon /></div>
      <p>{isStaff ? 'Request received' : 'Account created'}</p>
      <h2>{isStaff ? 'Your request is awaiting approval.' : 'A verification code has been sent to your email.'}</h2>
      <span>
        {isStaff
          ? 'An administrator must activate your requested role before you can sign in.'
          : 'Sign in, then verify your email with the code before booking an appointment.'}
      </span>
      <Link className="auth-primary-action" to="/login">
        <span>{isStaff ? 'Go to Staff Login' : 'Go to Citizen Login'}</span>
        <ArrowIcon />
      </Link>
    </section>
  );
}
