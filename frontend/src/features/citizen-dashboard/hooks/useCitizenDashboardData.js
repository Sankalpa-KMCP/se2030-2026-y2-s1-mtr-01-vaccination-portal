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
        if (active) setAppointments({ loading: false, data: Array.isArray(data) ? data : [], error: null });
      })
      .catch((error) => {
        if (active) setAppointments({ loading: false, data: [], error });
      });

    Promise.allSettled([
      listCitizenScreeningSubmissions(citizenId),
      listCitizenEligibilityAssessments(citizenId),
      listCampaigns(),
    ]).then((results) => {
      if (!active) return;
      const next = { loading: false, submissions: [], assessments: [], campaigns: [], errors: {} };
      ['submissions', 'assessments', 'campaigns'].forEach((key, index) => {
        const result = results[index];
        if (result.status === 'fulfilled') next[key] = Array.isArray(result.value) ? result.value : [];
        else next.errors[key] = result.reason;
      });
      setScreening(next);
    });

    return () => {
      active = false;
    };
  }, [citizenId]);

  return { appointments, screening };
}
