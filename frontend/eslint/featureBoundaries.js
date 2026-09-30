import path from 'node:path';

// These are provider-owned presentation entry points, not generic shared UI.
// The appointment badge owns appointment status presentation; the campaign modal
// owns questionnaire assignment; the landing header/footer and styles own public chrome.
// Keep this list explicit so it cannot turn into an exemption for whole directories.
const publicPresentation = new Set([
  'features/appointment-management/components/AppointmentStatusBadge',
  'features/campaign-management/components/QuestionnaireAssignmentFormModal',
  'features/public-landing/components/PublicHeader',
  'features/public-landing/components/PublicFooter',
  'features/public-landing/public-landing.css',
  'features/public-landing/premium-public.css',
  'features/public-landing/clinical-public.css',
  'features/public-landing/glass-header.css',
]);

function relativeToSource(root, file) {
  return path.relative(root, file).split(path.sep).join('/').replace(/\.(?:js|jsx)$/, '');
}

export default {
  meta: {
    type: 'problem',
    docs: { description: 'Enforce frontend ownership through resolved local module paths.' },
    schema: [],
    messages: {
      shared: 'Shared infrastructure/presentation must not depend on business features or Auth workflows.',
      feature: 'Use the other feature’s API/constants or an explicitly approved presentation entry point, not its internals.',
      route: 'Route composition belongs in src/app.',
    },
  },
  create(context) {
    const root = path.resolve(context.cwd, 'src');
    const source = relativeToSource(root, context.filename);
    const parts = source.split('/');

    function inspect(node) {
      const specifier = node.source?.value;
      if (typeof specifier !== 'string') return;
      if (specifier === 'react-router-dom' && parts[0] !== 'app'
          && node.specifiers?.some((item) => ['Routes', 'Route', 'useRoutes'].includes(item.imported?.name))) {
        context.report({ node, messageId: 'route' });
      }
      if (!specifier.startsWith('.')) return;
      const target = relativeToSource(root, path.resolve(path.dirname(context.filename), specifier));
      const targetParts = target.split('/');
      if (parts[0] === 'shared' && ['features', 'auth', 'app'].includes(targetParts[0])) {
        context.report({ node, messageId: 'shared' });
      }
      if (parts[0] === 'features' && targetParts[0] === 'features' && parts[1] !== targetParts[1]
          && !['api', 'constants'].includes(targetParts[2]) && !publicPresentation.has(target)) {
        context.report({ node, messageId: 'feature' });
      }
    }
    return {
      ImportDeclaration: inspect,
      ExportNamedDeclaration: inspect,
      ExportAllDeclaration: inspect,
      ImportExpression(node) { inspect({ ...node, source: node.source }); },
    };
  },
};
