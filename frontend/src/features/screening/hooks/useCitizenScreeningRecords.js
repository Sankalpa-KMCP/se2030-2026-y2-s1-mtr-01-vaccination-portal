import { useEffect, useState } from 'react';
import { listCitizenCampaigns } from '../../campaign-management/api/campaignApi';
import { listCitizenEligibilityAssessments, listCitizenScreeningSubmissions } from '../api/screeningApi';

/** One shared initial load; supplementary failures must not erase a citizen's history. */
export default function useCitizenScreeningRecords(citizenId) {
  const [state, setState] = useState({ loading: true, submissions: [], assessments: [], campaigns: [], errors: {} });
  useEffect(() => {
    let active = true;
    if (!citizenId) return undefined;
    Promise.allSettled([
      listCitizenScreeningSubmissions(citizenId),
      listCitizenEligibilityAssessments(citizenId),
      listCitizenCampaigns(),
    ]).then((results) => {
      if (!active) return;
      const next = { loading: false, errors: {} };
      ['submissions', 'assessments', 'campaigns'].forEach((key, index) => {
        const result = results[index];
        next[key] = result.status === 'fulfilled' ? result.value : [];
        if (result.status === 'rejected') next.errors[key] = result.reason;
      });
      setState(next);
    });
    return () => { active = false; };
  }, [citizenId]);
  return state;
}
