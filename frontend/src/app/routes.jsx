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
import CitizenDashboardPage from '../auth/pages/CitizenDashboardPage';
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
import AppointmentBookingPage from '../features/appointment-management/pages/AppointmentBookingPage';
import MyAppointmentsPage from '../features/appointment-management/pages/MyAppointmentsPage';
import WaitlistPage from '../features/appointment-management/pages/WaitlistPage';
import CheckInManagementPage from '../features/appointment-management/pages/CheckInManagementPage';
import AuthenticatedLayout from './AuthenticatedLayout';
import ProtectedRoute from './ProtectedRoute';
import NotAuthorizedPage from './pages/NotAuthorizedPage';
import NotFoundPage from './pages/NotFoundPage';

const CAMPAIGN_VIEW_ROLES = [
  'ROLE_ADMIN',
  'ROLE_CAMPAIGN_OFFICER',
  'ROLE_CENTER_MANAGER',
  'ROLE_HEALTHCARE_OFFICER',
  'ROLE_MANAGEMENT_USER',
];

const CENTER_VIEW_ROLES = CAMPAIGN_VIEW_ROLES;

const SESSION_VIEW_ROLES = [
  'ROLE_ADMIN',
  'ROLE_CENTER_MANAGER',
  'ROLE_CAMPAIGN_OFFICER',
  'ROLE_HEALTHCARE_OFFICER',
];

const ANALYTICS_VIEW_ROLES = [
  'ROLE_ADMIN',
  'ROLE_CAMPAIGN_OFFICER',
  'ROLE_CENTER_MANAGER',
];

const CHECK_IN_STAFF_ROLES = [
  'ROLE_ADMIN',
  'ROLE_HEALTHCARE_OFFICER',
  'ROLE_CENTER_MANAGER',
];

const SCREENING_ADMIN_ROLES = ['ROLE_ADMIN'];

function RoleHomeRedirect({ user }) {
  return <Navigate to={getRoleHomePath(user)} replace />;
}

function InitializingPage() {
  return (
    <div className="d-flex justify-content-center align-items-center min-vh-100 bg-light">
      <div className="text-center p-4">
        <div className="spinner-border text-primary mb-3" role="status" style={{ width: '3rem', height: '3rem' }}>
          <span className="visually-hidden">Loading authentication state...</span>
        </div>
        <p className="text-muted small mb-0">Initializing application session...</p>
      </div>
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
                <span className="display-6">⚠️</span>
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
        element={
          isAuthenticated
            ? <RoleHomeRedirect user={user} />
            : <LandingPage onLoginClick={() => navigate('/login')} />
        }
      />
      <Route
        path="/how-it-works"
        element={
          isAuthenticated
            ? <RoleHomeRedirect user={user} />
            : <HowItWorksPage onLoginClick={() => navigate('/login')} />
        }
      />
      <Route
        path="/about"
        element={
          isAuthenticated
            ? <RoleHomeRedirect user={user} />
            : <AboutPage onLoginClick={() => navigate('/login')} />
        }
      />
      <Route element={isAuthenticated ? <RoleHomeRedirect user={user} /> : <AuthExperience />}>
        <Route path="/login" element={<LoginPage audience="generic" />} />
        <Route path="/login/citizen" element={<LoginPage audience="citizen" />} />
        <Route path="/login/staff" element={<LoginPage audience="staff" />} />
        <Route path="/register/citizen" element={<CitizenRegistrationPage />} />
        <Route path="/register/staff" element={<StaffRegistrationRequestPage />} />
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route path="/app" element={<AuthenticatedLayout onLogout={handleVoluntaryLogout} />}>
          <Route index element={<RoleHomeRedirect user={user} />} />
          <Route path="overview" element={<RoleHomeRedirect user={user} />} />
          <Route path="not-authorized" element={<NotAuthorizedPage />} />

          <Route element={<ProtectedRoute allowedRoles={[CITIZEN_ROLE]} />}>
            <Route path="citizen" element={<CitizenDashboardPage />} />
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

          <Route element={<ProtectedRoute allowedRoles={[ADMIN_ROLE]} />}>
            <Route path="admin" element={<AdminDashboardPage />} />
            <Route path="admin/staff-requests" element={<PendingStaffRequestsPage />} />
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

          <Route element={<ProtectedRoute allowedRoles={SCREENING_ADMIN_ROLES} />}>
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
