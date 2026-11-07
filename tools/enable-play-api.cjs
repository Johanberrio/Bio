// Uses the existing Firebase CLI login; never prints credential values.
const auth = require('../.tools/firebase/node_modules/firebase-tools/lib/auth');
const { requireAuth } = require('../.tools/firebase/node_modules/firebase-tools/lib/requireAuth');
const service = require('../.tools/firebase/node_modules/firebase-tools/lib/ensureApiEnabled');
const { Client } = require('../.tools/firebase/node_modules/firebase-tools/lib/apiv2');
(async () => {
  const account = auth.getGlobalDefaultAccount();
  if (!account) throw new Error('Falta iniciar sesión con Firebase CLI.');
  await requireAuth({ project: 'bio-project-15d15', user: account.user, tokens: account.tokens, nonInteractive: true });
  await service.ensure('bio-project-15d15', 'androidpublisher.googleapis.com', 'Bio');
  // Direct check bypasses the CLI enablement cache.
  const client = new Client({ urlPrefix: 'https://serviceusage.googleapis.com', apiVersion: 'v1' });
  const result = await client.get('/projects/bio-project-15d15/services/androidpublisher.googleapis.com', { skipLog: { resBody: true } });
  if (result.body.state !== 'ENABLED') throw new Error('La API aún no está habilitada.');
  console.log('VERIFIED: Android Publisher API ENABLED in bio-project-15d15. Play permissions configured; purchase test still required.');
})().catch(() => { console.error('No se pudo habilitar o verificar Android Publisher API; revisa acceso del propietario del proyecto Bio.'); process.exitCode = 1; });
