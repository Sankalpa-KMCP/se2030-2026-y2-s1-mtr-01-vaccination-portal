import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useAuth } from '../../../auth/context/AuthContext';
import { INVALID_PHONE_NUMBER_MESSAGE, isValidPhoneNumber } from '../../../auth/utils/contactValidation';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import CitizenPageHeader from '../../../shared/ui/citizen/CitizenPageHeader';
import CitizenStatusBadge from '../../../shared/ui/citizen/CitizenStatusBadge';
import { getCitizenProfile, updateCitizenProfile } from '../api/citizenProfileApi';
import '../citizen-profile.css';

const GENDER_OPTIONS = [
  ['MALE', 'Male'],
  ['FEMALE', 'Female'],
  ['OTHER', 'Other'],
  ['PREFER_NOT_TO_SAY', 'Prefer not to say'],
];

const EMPTY_FORM = {
  fullName: '',
  phoneNumber: '',
  dateOfBirth: '',
  gender: '',
  address: '',
};

function profileToForm(profile) {
  return {
    fullName: profile?.fullName || '',
    phoneNumber: profile?.phoneNumber || '',
    dateOfBirth: profile?.dateOfBirth || '',
    gender: profile?.gender || '',
    address: profile?.address || '',
  };
}

function localToday() {
  const today = new Date();
  const month = String(today.getMonth() + 1).padStart(2, '0');
  const day = String(today.getDate()).padStart(2, '0');
  return `${today.getFullYear()}-${month}-${day}`;
}

function formatEnum(value) {
  if (!value) return 'Not provided';
  const genderOption = GENDER_OPTIONS.find(([optionValue]) => optionValue === value);
  if (genderOption) return genderOption[1];
  return String(value)
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

function formatDateOnly(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value || '');
  if (!match) return value || 'Not provided';
  const [, year, month, day] = match;
  return new Intl.DateTimeFormat('en-US', { dateStyle: 'medium' })
    .format(new Date(Number(year), Number(month) - 1, Number(day)));
}

function formatTimestamp(value) {
  if (!value) return 'Not provided';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}

function validate(form) {
  const errors = {};
  const fullName = form.fullName.trim();
  const phoneNumber = form.phoneNumber.trim();
  const address = form.address.trim();

  if (!fullName) errors.fullName = 'Full name is required.';
  else if (fullName.length > 255) errors.fullName = 'Full name must not exceed 255 characters.';

  if (!phoneNumber) errors.phoneNumber = 'Phone number is required.';
  else if (!isValidPhoneNumber(phoneNumber)) errors.phoneNumber = INVALID_PHONE_NUMBER_MESSAGE;

  if (form.dateOfBirth && form.dateOfBirth > localToday()) {
    errors.dateOfBirth = 'Date of birth must not be in the future.';
  }
  if (address.length > 500) errors.address = 'Address must not exceed 500 characters.';

  return errors;
}

function Detail({ label, children }) {
  return <div><dt>{label}</dt><dd>{children || 'Not provided'}</dd></div>;
}

function AccountInformation({ profile }) {
  return (
    <section className="citizen-detail-panel" aria-labelledby="profile-account-title">
      <header className="citizen-detail-panel__header">
        <p className="citizen-ui-eyebrow">Account information</p>
        <h2 id="profile-account-title">Citizen account</h2>
        <p>Identity and access details are managed securely and cannot be edited here.</p>
      </header>
      <div className="citizen-detail-panel__body">
        <dl className="citizen-profile__details">
          <Detail label="Citizen ID">{profile.id != null ? `#${profile.id}` : null}</Detail>
          <Detail label="Email address">{profile.email}</Detail>
          <Detail label="Account status">{formatEnum(profile.accountStatus)}</Detail>
          <Detail label="Account type">Citizen</Detail>
          <Detail label="Member since">{formatTimestamp(profile.createdAt)}</Detail>
          <Detail label="Last profile update">{formatTimestamp(profile.updatedAt)}</Detail>
        </dl>
      </div>
    </section>
  );
}

function ProfileDetails({ profile }) {
  return (
    <div className="citizen-profile__sections">
      <AccountInformation profile={profile} />

      <section className="citizen-detail-panel" aria-labelledby="profile-personal-title">
        <header className="citizen-detail-panel__header">
          <p className="citizen-ui-eyebrow">Personal information</p>
          <h2 id="profile-personal-title">About you</h2>
        </header>
        <div className="citizen-detail-panel__body">
          <dl className="citizen-profile__details">
            <Detail label="Full name">{profile.fullName}</Detail>
            <Detail label="Date of birth">{formatDateOnly(profile.dateOfBirth)}</Detail>
            <Detail label="Gender">{formatEnum(profile.gender)}</Detail>
          </dl>
        </div>
      </section>

      <section className="citizen-detail-panel" aria-labelledby="profile-contact-title">
        <header className="citizen-detail-panel__header">
          <p className="citizen-ui-eyebrow">Contact information</p>
          <h2 id="profile-contact-title">How we can reach you</h2>
        </header>
        <div className="citizen-detail-panel__body">
          <dl className="citizen-profile__details">
            <Detail label="Phone number">{profile.phoneNumber}</Detail>
            <Detail label="Address">{profile.address}</Detail>
          </dl>
        </div>
      </section>
    </div>
  );
}

function FieldError({ id, message }) {
  return message ? <div className="invalid-feedback d-block" id={id}>{message}</div> : null;
}

export default function CitizenProfilePage() {
  const { updateCurrentUserProfile } = useAuth();
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [saveError, setSaveError] = useState(null);
  const [successMessage, setSuccessMessage] = useState('');
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const savingRef = useRef(false);

  const loadProfile = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const loadedProfile = await getCitizenProfile();
      if (!loadedProfile) throw new Error('Profile information is unavailable.');
      setProfile(loadedProfile);
      setForm(profileToForm(loadedProfile));
    } catch (error) {
      setLoadError(error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  function startEditing() {
    setForm(profileToForm(profile));
    setFieldErrors({});
    setSaveError(null);
    setSuccessMessage('');
    setEditing(true);
  }

  function cancelEditing() {
    setForm(profileToForm(profile));
    setFieldErrors({});
    setSaveError(null);
    setEditing(false);
  }

  function changeField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setFieldErrors((current) => ({ ...current, [name]: undefined }));
    setSaveError(null);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (savingRef.current) return;

    const errors = validate(form);
    setFieldErrors(errors);
    setSuccessMessage('');
    if (Object.keys(errors).length > 0) return;

    savingRef.current = true;
    setSaving(true);
    setSaveError(null);
    try {
      const updatedProfile = await updateCitizenProfile(form);
      if (!updatedProfile) throw new Error('The updated profile was not returned by the server.');
      setProfile(updatedProfile);
      setForm(profileToForm(updatedProfile));
      updateCurrentUserProfile(updatedProfile);
      setEditing(false);
      setSuccessMessage('Profile updated successfully.');
    } catch (error) {
      setSaveError(error);
    } finally {
      savingRef.current = false;
      setSaving(false);
    }
  }

  const accountStatus = formatEnum(profile?.accountStatus);

  return (
    <section className="citizen-page citizen-profile" aria-labelledby="citizen-profile-title">
      <CitizenPageHeader
        eyebrow="Citizen account"
        title="My Profile"
        titleId="citizen-profile-title"
        subtitle="Review your account information and keep your personal and contact details up to date."
        backTo="/app/citizen"
        backLabel="Back to Citizen Dashboard"
        badge={profile && <CitizenStatusBadge tone={profile.accountStatus === 'ACTIVE' ? 'positive' : 'neutral'}>{accountStatus}</CitizenStatusBadge>}
        actions={profile && !editing ? <button type="button" onClick={startEditing}>Edit Profile</button> : null}
      />

      {loading && <LoadingSpinner message="Loading your profile..." />}

      {!loading && loadError && (
        <div className="citizen-profile__load-error">
          <ApiErrorAlert error={loadError} />
          <button className="btn btn-outline-primary" type="button" onClick={loadProfile}>Retry</button>
        </div>
      )}

      {!loading && profile && (
        <>
          {successMessage && <div className="alert alert-success" role="status">{successMessage}</div>}
          {!editing && <ProfileDetails profile={profile} />}

          {editing && (
            <div className="citizen-profile__edit-layout">
              <AccountInformation profile={profile} />
              <section className="citizen-form-panel" aria-labelledby="profile-edit-title">
                <header className="citizen-form-panel__header">
                  <p className="citizen-ui-eyebrow">Editable details</p>
                  <h2 id="profile-edit-title">Edit your profile</h2>
                  <p>Update only the personal and contact fields below.</p>
                </header>
                <form className="citizen-form-panel__body citizen-profile__form" noValidate onSubmit={handleSubmit}>
                <ApiErrorAlert error={saveError} onDismiss={() => setSaveError(null)} />

                <div className="citizen-profile__form-grid">
                  <div>
                    <label className="form-label" htmlFor="profile-full-name">Full name</label>
                    <input
                      className={`form-control ${fieldErrors.fullName ? 'is-invalid' : ''}`}
                      id="profile-full-name"
                      name="fullName"
                      value={form.fullName}
                      maxLength={255}
                      aria-describedby={fieldErrors.fullName ? 'profile-full-name-error' : undefined}
                      aria-invalid={Boolean(fieldErrors.fullName)}
                      onChange={changeField}
                    />
                    <FieldError id="profile-full-name-error" message={fieldErrors.fullName} />
                  </div>

                  <div>
                    <label className="form-label" htmlFor="profile-phone-number">Phone number</label>
                    <input
                      className={`form-control ${fieldErrors.phoneNumber ? 'is-invalid' : ''}`}
                      id="profile-phone-number"
                      name="phoneNumber"
                      type="tel"
                      value={form.phoneNumber}
                      maxLength={10}
                      inputMode="numeric"
                      aria-describedby={fieldErrors.phoneNumber ? 'profile-phone-number-error' : undefined}
                      aria-invalid={Boolean(fieldErrors.phoneNumber)}
                      onChange={changeField}
                    />
                    <FieldError id="profile-phone-number-error" message={fieldErrors.phoneNumber} />
                  </div>

                  <div>
                    <label className="form-label" htmlFor="profile-date-of-birth">Date of birth <span className="text-muted">(optional)</span></label>
                    <input
                      className={`form-control ${fieldErrors.dateOfBirth ? 'is-invalid' : ''}`}
                      id="profile-date-of-birth"
                      name="dateOfBirth"
                      type="date"
                      value={form.dateOfBirth}
                      max={localToday()}
                      aria-describedby={fieldErrors.dateOfBirth ? 'profile-date-of-birth-error' : undefined}
                      aria-invalid={Boolean(fieldErrors.dateOfBirth)}
                      onChange={changeField}
                    />
                    <FieldError id="profile-date-of-birth-error" message={fieldErrors.dateOfBirth} />
                  </div>

                  <div>
                    <label className="form-label" htmlFor="profile-gender">Gender <span className="text-muted">(optional)</span></label>
                    <select className="form-select" id="profile-gender" name="gender" value={form.gender} onChange={changeField}>
                      <option value="">Not provided</option>
                      {GENDER_OPTIONS.map(([value, label]) => <option value={value} key={value}>{label}</option>)}
                    </select>
                  </div>

                  <div className="citizen-profile__full-width">
                    <label className="form-label" htmlFor="profile-address">Address <span className="text-muted">(optional)</span></label>
                    <textarea
                      className={`form-control ${fieldErrors.address ? 'is-invalid' : ''}`}
                      id="profile-address"
                      name="address"
                      rows="4"
                      value={form.address}
                      maxLength={500}
                      aria-describedby={fieldErrors.address ? 'profile-address-error' : 'profile-address-help'}
                      aria-invalid={Boolean(fieldErrors.address)}
                      onChange={changeField}
                    />
                    <div className="form-text" id="profile-address-help">Up to 500 characters.</div>
                    <FieldError id="profile-address-error" message={fieldErrors.address} />
                  </div>
                </div>

                <div className="citizen-profile__actions">
                  <button className="btn btn-primary" type="submit" disabled={saving}>
                    {saving ? 'Saving...' : 'Save Changes'}
                  </button>
                  <button className="btn btn-outline-secondary" type="button" disabled={saving} onClick={cancelEditing}>Cancel</button>
                </div>
                </form>
              </section>
            </div>
          )}
        </>
      )}
    </section>
  );
}
