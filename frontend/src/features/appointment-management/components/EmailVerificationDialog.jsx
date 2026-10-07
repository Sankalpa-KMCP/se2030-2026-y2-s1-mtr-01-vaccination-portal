import React, { useEffect, useRef, useState } from 'react';
import { getEmailVerificationStatus, resendEmailOtp, verifyEmailOtp } from '../api/emailVerificationApi';

const VERIFICATION_MESSAGES = {
  EMAIL_VERIFICATION_INVALID_CODE: 'That verification code is not valid.',
  EMAIL_VERIFICATION_CODE_EXPIRED: 'That verification code has expired. Request a new code.',
  EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED: 'Too many incorrect attempts. Request a new verification code.',
};

const GENERIC_VERIFY_ERROR = 'We could not verify that code right now. Please try again.';
const GENERIC_RESEND_ERROR = 'We could not send a new code right now. Please try again shortly.';

/**
 * Contextual email OTP verification dialog for the authenticated citizen.
 * Opened from appointment acquisition feedback when the backend rejects an
 * action with EMAIL_VERIFICATION_REQUIRED. Verification state is decided only
 * by backend responses; the dialog never inspects cached account status.
 * Opening the dialog must not disturb any booking-page state: it renders as an
 * overlay and simply closes when done so the citizen can retry manually.
 */
export default function EmailVerificationDialog({ onClose }) {
  const [otp, setOtp] = useState('');
  const [verifying, setVerifying] = useState(false);
  const [resending, setResending] = useState(false);
  const [verified, setVerified] = useState(false);
  const [verifiedNote, setVerifiedNote] = useState('Your email is verified. Retry your appointment action.');
  const [errorText, setErrorText] = useState(null);
  const [resendNotice, setResendNotice] = useState(null);
  const [cooldownSeconds, setCooldownSeconds] = useState(0);
  const inputRef = useRef(null);

  useEffect(() => {
    inputRef.current?.focus();
    let active = true;
    getEmailVerificationStatus()
      .then((status) => {
        if (!active) return;
        if (status?.emailVerified) {
          setVerified(true);
          setVerifiedNote('Your email is already verified. Retry your appointment action.');
        }
      })
      .catch(() => {
        // Status is advisory: leave the citizen in OTP entry rather than blocking.
      });
    return () => {
      active = false;
    };
  }, []);

  const coolingDown = cooldownSeconds > 0;
  useEffect(() => {
    if (!coolingDown) return undefined;
    const interval = setInterval(() => {
      setCooldownSeconds((seconds) => {
        if (seconds <= 1) {
          clearInterval(interval);
          return 0;
        }
        return seconds - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [coolingDown]);

  const digits = otp.replace(/\D/g, '').slice(0, 6);
  const canVerify = digits.length === 6 && !verifying;

  const handleOtpChange = (event) => {
    setOtp(event.target.value.replace(/\D/g, '').slice(0, 6));
    setErrorText(null);
  };

  const handleVerify = async (event) => {
    event.preventDefault();
    if (digits.length !== 6 || verifying) return;
    setVerifying(true);
    setErrorText(null);
    try {
      await verifyEmailOtp(digits);
      setVerified(true);
      setVerifiedNote('Your email is verified. Retry your appointment action.');
    } catch (err) {
      if (err.code === 'EMAIL_ALREADY_VERIFIED') {
        setVerified(true);
        setVerifiedNote('Your email is already verified. Retry your appointment action.');
      } else {
        setErrorText(VERIFICATION_MESSAGES[err.code] || GENERIC_VERIFY_ERROR);
      }
    } finally {
      setVerifying(false);
    }
  };

  const handleResend = async () => {
    if (resending || cooldownSeconds > 0) return;
    setResending(true);
    setResendNotice(null);
    setErrorText(null);
    try {
      await resendEmailOtp();
      setOtp('');
      setResendNotice('A new code has been sent to your email.');
    } catch (err) {
      if (err.status === 429 && err.retryAfterSeconds > 0) {
        setCooldownSeconds(err.retryAfterSeconds);
        setResendNotice(`Please wait ${err.retryAfterSeconds} seconds before requesting another code.`);
      } else {
        setResendNotice(GENERIC_RESEND_ERROR);
      }
    } finally {
      setResending(false);
    }
  };

  return (
    <div className="modal show d-block" role="dialog" aria-modal="true" aria-labelledby="email-verification-title">
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content border-0 shadow-lg">
          <div className="modal-header">
            <div>
              <p className="text-uppercase text-primary small fw-bold mb-1">Appointment services</p>
              <h2 id="email-verification-title" className="modal-title h5">Verify your email</h2>
            </div>
            <button type="button" className="btn-close" aria-label="Close" onClick={onClose} />
          </div>

          {verified ? (
            <div className="modal-body">
              <div className="alert alert-success mb-3" role="status">
                <strong>Email verified successfully.</strong>
              </div>
              <p className="mb-0">{verifiedNote}</p>
            </div>
          ) : (
            <form onSubmit={handleVerify}>
              <div className="modal-body">
                <p className="mb-3">Enter the 6-digit code sent to your registered email address.</p>
                <label className="form-label fw-semibold" htmlFor="email-verification-otp">Verification code</label>
                <input
                  id="email-verification-otp"
                  ref={inputRef}
                  className="form-control form-control-lg"
                  type="text"
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  maxLength={6}
                  value={otp}
                  onChange={handleOtpChange}
                  disabled={verifying}
                  placeholder="000000"
                  required
                />
                {otp.length > 0 && digits.length < 6 && (
                  <div className="form-text">The code has exactly six digits.</div>
                )}
                {errorText && (
                  <div className="alert alert-warning mt-3 mb-0" role="alert">{errorText}</div>
                )}
                {resendNotice && (
                  <div className="alert alert-info mt-3 mb-0" role="status">{resendNotice}</div>
                )}
              </div>
              <div className="modal-footer justify-content-between">
                <div>
                  <span className="small text-secondary d-block">Didn&apos;t receive the code?</span>
                  <button
                    type="button"
                    className="btn btn-link btn-sm p-0"
                    onClick={handleResend}
                    disabled={resending || cooldownSeconds > 0}
                  >
                    {resending
                      ? 'Sending…'
                      : cooldownSeconds > 0
                        ? `Resend code in ${cooldownSeconds}s`
                        : 'Resend code'}
                  </button>
                </div>
                <button type="submit" className="btn btn-primary" disabled={!canVerify}>
                  {verifying ? 'Verifying…' : 'Verify'}
                </button>
              </div>
            </form>
          )}

          {verified && (
            <div className="modal-footer">
              <button type="button" className="btn btn-outline-secondary" onClick={onClose}>Close</button>
              <button type="button" className="btn btn-primary" onClick={onClose}>Done</button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
