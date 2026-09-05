import { useEffect, useState } from 'react';
import { getMyAppointments } from '../../appointment-management/api/appointmentApi';
import { listCampaigns } from '../../campaign-management/api/campaignApi';
import {
  listCitizenEligibilityAssessments,
  listCitizenScreeningSubmissions,
} from '../../screening/api/screeningApi';

const loadingAppointments = () => ({ loading: true, data: [], error: null });
const loadingScreening = () => ({
  loading: true,
  submissions: [],
  assessments: [],
  campaigns: [],
  errors: {},
});

export default function useCitizenDashboardData(citizenId) {
  const [appointments, setAppointments] = useState(loadingAppointments);
  const [screening, setScreening] = useState(loadingScreening);

  useEffect(() => {
    let active = true;
    setAppointments(loadingAppointments());
    setScreening(loadingScreening());

    if (!citizenId) {
      setAppointments({ loading: false, data: [], error: new Error('Your citizen account identifier is unavailable.') });
      setScreening({
        loading: false,
        submissions: [],
        assessments: [],
        campaigns: [],
        errors: { submissions: new Error('Your citizen account identifier is unavailable.') },
      });
      return undefined;
    }

    getMyAppointments(citizenId)
      .then((data) => {
        if (!active) return;
        if (Array.isArray(data)) {
          setAppointments({ loading: false, data, error: null });
        } else {
          setAppointments({
            loading: false,
            data: [],
            error: new Error('Malformed appointments response received.'),
          });
        }
      })
      .catch((error) => {
        if (active) setAppointments({ loading: false, data: [], error });
      });

    let submissionsSettled = false;
    let assessmentsSettled = false;

    const checkAndSetScreening = (updateFn) => {
      if (!active) return;
      setScreening((prev) => {
        const next = updateFn(prev);
        if (submissionsSettled && assessmentsSettled) {
          next.loading = false;
        }
        return next;
      });
    };

    listCitizenScreeningSubmissions(citizenId)
      .then((data) => {
        submissionsSettled = true;
        checkAndSetScreening((prev) => {
          const next = { ...prev, errors: { ...prev.errors } };
          if (Array.isArray(data)) {
            next.submissions = data;
          } else {
            next.submissions = [];
            next.errors.submissions = new Error('Malformed screening submissions response received.');
          }
          return next;
        });
      })
      .catch((error) => {
        submissionsSettled = true;
        checkAndSetScreening((prev) => {
          const next = { ...prev, submissions: [], errors: { ...prev.errors } };
          next.errors.submissions = error;
          return next;
        });
      });

    listCitizenEligibilityAssessments(citizenId)
      .then((data) => {
        assessmentsSettled = true;
        checkAndSetScreening((prev) => {
          const next = { ...prev, errors: { ...prev.errors } };
          if (Array.isArray(data)) {
            next.assessments = data;
          } else {
            next.assessments = [];
            next.errors.assessments = new Error('Malformed screening assessments response received.');
          }
          return next;
        });
      })
      .catch((error) => {
        assessmentsSettled = true;
        checkAndSetScreening((prev) => {
          const next = { ...prev, assessments: [], errors: { ...prev.errors } };
          next.errors.assessments = error;
          return next;
        });
      });

    listCampaigns()
      .then((data) => {
        if (!active) return;
        setScreening((prev) => {
          const next = { ...prev, errors: { ...prev.errors } };
          if (Array.isArray(data)) {
            next.campaigns = data;
          } else {
            next.campaigns = [];
            next.errors.campaigns = new Error('Malformed campaigns response received.');
          }
          return next;
        });
      })
      .catch((error) => {
        if (!active) return;
        setScreening((prev) => {
          const next = { ...prev, campaigns: [], errors: { ...prev.errors } };
          next.errors.campaigns = error;
          return next;
        });
      });

    return () => {
      active = false;
    };
  }, [citizenId]);

  return { appointments, screening };
}
