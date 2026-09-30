import path from 'node:path';
import { describe, it } from 'vitest';
import { RuleTester } from 'eslint';
import rule from './featureBoundaries';

RuleTester.describe = describe;
RuleTester.it = it;
const file = (name) => path.resolve('src', name);
const tester = new RuleTester({ languageOptions: { ecmaVersion: 'latest', sourceType: 'module' } });

tester.run('feature ownership', rule, {
  valid: [
    { filename: file('shared/api/client.js'), code: "import { token } from '../auth/storage';" },
    { filename: file('features/screening/pages/Page.jsx'), code: "import { list } from '../../campaign-management/api/campaignApi';" },
    { filename: file('features/screening/pages/Page.jsx'), code: "import Modal from '../../campaign-management/components/QuestionnaireAssignmentFormModal';" },
    { filename: file('features/vaccination-administration/pages/Page.jsx'), code: "import Header from '../../public-landing/components/PublicHeader';" },
    { filename: file('features/screening/pages/Page.jsx'), code: "import Builder from '../hooks/useQuestionnaireBuilder';" },
    { filename: file('auth/context/AuthContext.jsx'), code: "import { token } from '../../shared/auth/storage';" },
    { filename: file('app/routes.jsx'), code: "import { Routes, Route } from 'react-router-dom'; import Page from '../features/screening/pages/Page';" },
  ],
  invalid: [
    { filename: file('shared/ui/Card.jsx'), code: "import { list } from '../../features/screening/api/screeningApi';", errors: [{ messageId: 'shared' }] },
    { filename: file('shared/ui/Card.jsx'), code: "import { useAuth } from '../../auth/context/AuthContext';", errors: [{ messageId: 'shared' }] },
    { filename: file('features/screening/pages/Page.jsx'), code: "import Page from '../../citizen-dashboard/pages/CitizenDashboardPage.jsx';", errors: [{ messageId: 'feature' }] },
    { filename: file('features/screening/pages/Page.jsx'), code: "export { default } from '../../campaign-management/components/PrivatePanel';", errors: [{ messageId: 'feature' }] },
    { filename: file('features/screening/pages/Page.jsx'), code: "const load = () => import('../../campaign-management/hooks/usePrivateWorkflow');", errors: [{ messageId: 'feature' }] },
    { filename: file('features/screening/pages/Page.jsx'), code: "import { Routes as Composition } from 'react-router-dom';", errors: [{ messageId: 'route' }] },
    { filename: file('shared/ui/Card.jsx'), code: "export * from '../../app/routes';", errors: [{ messageId: 'shared' }] },
  ],
});
