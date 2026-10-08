import React, { useCallback, useEffect, useRef } from 'react';
import {
  Navigate,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from 'react-router-dom';
import { useAuth } from '../auth/context/AuthContext';
import LoginPage from '../auth/pages/LoginPage';
import CitizenRegistrationPage from '../auth/pages/CitizenRegistrationPage';
import StaffRegistrationRequestPage from '../auth/pages/StaffRegistrationRequestPage';
import ForgotPasswordPage from '../auth/pages/ForgotPasswordPage';
import ResetPasswordPage from '../auth/pages/ResetPasswordPage';
import VerifyEmailPage from '../auth/pages/VerifyEmailPage';
import CitizenDashboardPage from '../features/citizen-dashboard/pages/CitizenDashboardPage';
import StaffDashboardPage from '../auth/pages/StaffDashboardPage';
import AdminDashboardPage from '../auth/pages/AdminDashboardPage';
import PendingStaffRequestsPage from '../auth/pages/PendingStaffRequestsPage';
import AuthExperience from '../auth/components/AuthExperience';
import {
  ADMIN_ROLE,
  CITIZEN_ROLE,
  STAFF_DASHBOARD_ROLES,
} from '../auth/constants/authRoles';
import { getRoleHomePath } from '../auth/utils/roleRedirect';
import LandingPage from '../features/public-landing/pages/LandingPage';
import HowItWorksPage from '../features/public-landing/pages/HowItWorksPage';
import AboutPage from '../features/public-landing/pages/AboutPage';
import CampaignManagementPage from '../features/campaign-management/pages/CampaignManagementPage';
import CenterOperationsPage from '../features/center-operations/pages/CenterOperationsPage';
import SessionsPage from '../features/session-management/pages/SessionsPage';
import OperationalAnalyticsView from '../features/analytics/components/OperationalAnalyticsView';
import ScreeningQuestionsPage from '../features/screening/pages/ScreeningQuestionsPage';
import ScreeningQuestionnairesPage from '../features/screening/pages/ScreeningQuestionnairesPage';
import CitizenScreeningCampaignsPage from '../features/screening/pages/CitizenScreeningCampaignsPage';
import CitizenScreeningPage from '../features/screening/pages/CitizenScreeningPage';
import CitizenEligibilityPage from '../features/screening/pages/CitizenEligibilityPage';
import CitizenScreeningHistoryPage from '../features/screening/pages/CitizenScreeningHistoryPage';
import CitizenScreeningHistoryDetailsPage from '../features/screening/pages/CitizenScreeningHistoryDetailsPage';
import CitizenScreeningCorrectionPage from '../features/screening/pages/CitizenScreeningCorrectionPage';
import CitizenProfilePage from '../features/citizen-profile/pages/CitizenProfilePage';
import HealthcareOfficerScreeningReviewQueuePage from '../features/screening/pages/HealthcareOfficerScreeningReviewQueuePage';
import HealthcareOfficerScreeningReviewDetailsPage from '../features/screening/pages/HealthcareOfficerScreeningReviewDetailsPage';
import HealthcareOfficerScreeningHistoryDetailsPage from '../features/screening/pages/HealthcareOfficerScreeningHistoryDetailsPage';
import AppointmentBookingPage from '../features/appointment-management/pages/AppointmentBookingPage';
import MyAppointmentsPage from '../features/appointment-management/pages/MyAppointmentsPage';
import WaitlistPage from '../features/appointment-management/pages/WaitlistPage';
import CheckInManagementPage from '../features/appointment-management/pages/CheckInManagementPage';
import InventoryManagementPage from '../features/inventory-management/pages/InventoryManagementPage';
import VaccinationDraftsPage from '../features/vaccination-administration/pages/VaccinationDraftsPage';
import VaccinatorVaccinationRecordsPage from '../features/vaccination-administration/pages/VaccinatorVaccinationRecordsPage';
import VaccinatorCorrectionRequestsPage from '../features/vaccination-administration/pages/VaccinatorCorrectionRequestsPage';
import AdminCorrectionRequestsPage from '../features/vaccination-administration/pages/AdminCorrectionRequestsPage';
import CitizenVaccinationsPage from '../features/vaccination-administration/pages/CitizenVaccinationsPage';
import PublicCertificateVerificationPage from '../features/vaccination-administration/pages/PublicCertificateVerificationPage';
import AuthenticatedLayout from './AuthenticatedLayout';
import ProtectedRoute from './ProtectedRoute';
import LoadingSpinner from '../components/common/LoadingSpinner';
import NotAuthorizedPage from './pages/NotAuthorizedPage';
import NotFoundPage from './pages/NotFoundPage';

const CAMPAIGN_VIEW_ROLES = [
  'ROLE_ADMIN',
  'ROLE_CAMPAIGN_OFFICER',
  'ROLE_CENTER_MANAGER',
  'ROLE_MANAGEMENT_USER',
];

const CENTER_VIEW_ROLES = CAMPAIGN_VIEW_ROLES;

const SESSION_VIEW_ROLES = [
  'ROLE_ADMIN',
  'ROLE_CENTER_MANAGER',
  'ROLE_CAMPAIGN_OFFICER',
];

const ANALYTICS_VIEW_ROLES = [
  'ROLE_ADMIN',
  'ROLE_CAMPAIGN_OFFICER',
  'ROLE_CENTER_MANAGER',
];

const CHECK_IN_STAFF_ROLES = [
  'ROLE_ADMIN',
  'ROLE_CENTER_MANAGER',
  'ROLE_VACCINATOR',
];

const INVENTORY_ROLES = ['ROLE_ADMIN', 'ROLE_INVENTORY_OFFICER'];
const SCREENING_CONFIGURATION_ROLES = ['ROLE_HEALTHCARE_OFFICER'];
const SCREENING_REVIEW_ROLES = ['ROLE_HEALTHCARE_OFFICER'];

function RoleHomeRedirect({ user }) {
  return <Navigate to={getRoleHomePath(user)} replace />;
}

function InitializingPage() {
  return (
    <div className="d-flex justify-content-center align-items-center min-vh-100 bg-light">
      <LoadingSpinner message="Initializing application session..." />
    </div>
  );
}

function InitializationErrorPage({ error, onRetry, onLogout }) {
  return (
    <div className="container py-5">
      <div className="row justify-content-center">
        <div className="col-12 col-md-8 col-lg-6">
          <div className="card border-0 shadow-sm">
            <div className="card-body p-4 text-center">
              <div className="text-danger mb-3">
                <i className="bi bi-exclamation-triangle display-6" aria-hidden="true"></i>
              </div>
              <h1 className="h4 text-danger mb-2">Service Temporarily Unavailable</h1>
              <p className="text-muted small mb-4">{error}</p>
              <div className="d-flex justify-content-center gap-2">
                <button type="button" className="btn btn-primary" onClick={onRetry}>
                  Retry Connection
                </button>
                <button type="button" className="btn btn-outline-secondary" onClick={onLogout}>
                  Sign Out &amp; Return to Login
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function PortalRoutes() {
  const {
    isAuthenticated,
    isInitializing,
    initializationError,
    retryBootstrap,
    logout,
    user,
  } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const hadSessionRef = useRef(false);

  useEffect(() => {
    if (isAuthenticated) {
      hadSessionRef.current = true;
      return;
    }

    if (hadSessionRef.current && !isInitializing) {
      hadSessionRef.current = false;
      navigate('/login', {
        replace: true,
        state: { sessionExpired: true, from: location.pathname },
      });
    }
  }, [isAuthenticated, isInitializing, location.pathname, navigate]);

  const handleVoluntaryLogout = useCallback(() => {
    hadSessionRef.current = false;
    logout();
    navigate('/', { replace: true });
  }, [logout, navigate]);

  if (isInitializing) {
    return <InitializingPage />;
  }

  if (initializationError) {
    return (
      <InitializationErrorPage
        error={initializationError}
        onRetry={retryBootstrap}
        onLogout={handleVoluntaryLogout}
      />
    );
  }

  return (
    <Routes>
      <Route
        path="/"
        element={<LandingPage onLoginClick={() => navigate('/login')} />}
      />
      <Route
        path="/how-it-works"
        element={<HowItWorksPage onLoginClick={() => navigate('/login')} />}
      />
      <Route
        path="/about"
        element={<AboutPage onLoginClick={() => navigate('/login')} />}
      />
      <Route
        path="/verify-certificate"
        element={<PublicCertificateVerificationPage onLoginClick={() => navigate('/login')} />}
      />
      <Route element={isAuthenticated ? <RoleHomeRedirect user={user} /> : <AuthExperience />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/login/citizen" element={<Navigate to="/login" replace />} />
        <Route path="/login/staff" element={<Navigate to="/login" replace />} />
        <Route path="/register/citizen" element={<CitizenRegistrationPage />} />
        <Route path="/register/staff" element={<StaffRegistrationRequestPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route path="/app" element={<AuthenticatedLayout onLogout={handleVoluntaryLogout} />}>
          <Route index element={<RoleHomeRedirect user={user} />} />
          <Route path="overview" element={<RoleHomeRedirect user={user} />} />
          <Route path="not-authorized" element={<NotAuthorizedPage />} />

          <Route element={<ProtectedRoute allowedRoles={[CITIZEN_ROLE]} />}>
            <Route path="citizen" element={<CitizenDashboardPage />} />
            <Route path="citizen/profile" element={<CitizenProfilePage />} />
            <Route path="citizen/vaccinations" element={<CitizenVaccinationsPage />} />
            <Route path="vaccinations" element={<CitizenVaccinationsPage />} />
            <Route path="screening" element={<CitizenScreeningCampaignsPage />} />
            <Route path="screening/campaigns/:campaignId" element={<CitizenScreeningPage />} />
            <Route path="screening/eligibility" element={<CitizenEligibilityPage />} />
            <Route path="screening/history" element={<CitizenScreeningHistoryPage />} />
            <Route path="screening/history/:submissionId" element={<CitizenScreeningHistoryDetailsPage />} />
            <Route path="screening/history/:submissionId/correct" element={<CitizenScreeningCorrectionPage />} />
            <Route path="appointments/book" element={<AppointmentBookingPage />} />
            <Route path="appointments/mine" element={<MyAppointmentsPage />} />
            <Route path="appointments/waitlist" element={<WaitlistPage />} />
          </Route>

          <Route element={<ProtectedRoute allowedRoles={STAFF_DASHBOARD_ROLES} />}>
            <Route path="staff" element={<StaffDashboardPage />} />
          </Route>

          <Route element={<ProtectedRoute allowedRoles={['ROLE_VACCINATOR']} />}>
            <Route path="staff/vaccination-drafts" element={<VaccinationDraftsPage />} />
            <Route path="staff/vaccination-records" element={<VaccinatorVaccinationRecordsPage />} />
            <Route path="staff/correction-requests" element={<VaccinatorCorrectionRequestsPage />} />
          </Route>

          <Route element={<ProtectedRoute allowedRoles={SCREENING_REVIEW_ROLES} />}>
            <Route path="staff/screening/reviews" element={<HealthcareOfficerScreeningReviewQueuePage />} />
            <Route path="staff/screening/reviews/:submissionId" element={<HealthcareOfficerScreeningReviewDetailsPage />} />
            <Route path="staff/screening/history/:submissionId" element={<HealthcareOfficerScreeningHistoryDetailsPage />} />
          </Route>

          <Route element={<ProtectedRoute allowedRoles={[ADMIN_ROLE]} />}>
            <Route path="admin" element={<AdminDashboardPage />} />
            <Route path="admin/staff-requests" element={<PendingStaffRequestsPage />} />
            <Route path="admin/correction-requests" element={<AdminCorrectionRequestsPage />} />
          </Route>

          <Route element={<ProtectedRoute allowedRoles={CAMPAIGN_VIEW_ROLES} />}>
            <Route path="campaigns" element={<CampaignManagementPage />} />
          </Route>

          <Route element={<ProtectedRoute allowedRoles={CENTER_VIEW_ROLES} />}>
            <Route path="center-operations" element={<CenterOperationsPage />} />
          </Route>

          <Route element={<ProtectedRoute allowedRoles={SESSION_VIEW_ROLES} />}>
            <Route path="sessions" element={<SessionsPage />} />
          </Route>

          <Route element={<ProtectedRoute allowedRoles={ANALYTICS_VIEW_ROLES} />}>
            <Route path="analytics" element={<OperationalAnalyticsView />} />
          </Route>

          <Route element={<ProtectedRoute allowedRoles={CHECK_IN_STAFF_ROLES} />}>
            <Route path="checkin/queue" element={<CheckInManagementPage />} />
          </Route>

          <Route element={<ProtectedRoute allowedRoles={INVENTORY_ROLES} />}>
            <Route path="inventory" element={<InventoryManagementPage />} />
          </Route>

          <Route element={<ProtectedRoute allowedRoles={SCREENING_CONFIGURATION_ROLES} />}>
            <Route path="screening/questions" element={<ScreeningQuestionsPage />} />
            <Route path="screening/questionnaires" element={<ScreeningQuestionnairesPage />} />
          </Route>

          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
