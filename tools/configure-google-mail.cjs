// Import a downloaded Google web OAuth client into Bio's server-only configuration.
// Never log the input, request/response bodies, credential values or provider errors.
const fs = require('node:fs');
const path = require('node:path');
const PROJECT = 'bio-project-15d15';
const REDIRECT = 'https://us-east1-bio-project-15d15.cloudfunctions.net/bio_api/mail/callback/google';
let phase = 'validate';
(async () => {
  const folder = path.resolve(__dirname, '../.secrets');
  const files = fs.readdirSync(folder).filter(f => f.endsWith('.json'));
  const clients = files.map(f => JSON.parse(fs.readFileSync(path.join(folder, f), 'utf8').replace(/^\uFEFF/, '')).web).filter(Boolean);
  if (clients.length !== 1) throw new Error('Debe existir exactamente un cliente OAuth web en .secrets.');
  const cfg = clients[0];
  if (cfg.project_id !== PROJECT) throw new Error('El cliente no pertenece al proyecto Bio.');
  if (!Array.isArray(cfg.redirect_uris) || !cfg.redirect_uris.includes(REDIRECT)) throw new Error('Falta la URI exacta de retorno de Gmail para Bio.');
  if (typeof cfg.client_id !== 'string' || !cfg.client_id.endsWith('.apps.googleusercontent.com') || typeof cfg.client_secret !== 'string' || cfg.client_secret.length < 16) throw new Error('El archivo no contiene un cliente completo.');
  console.log('Validated: Bio project, web client, expected HTTPS callback and secret present.');
  if (!process.argv.includes('--apply')) return;
  phase = 'load-admin-client';
  const auth = require('../.tools/firebase/node_modules/firebase-tools/lib/auth');
  const { requireAuth } = require('../.tools/firebase/node_modules/firebase-tools/lib/requireAuth');
  const { Client } = require('../.tools/firebase/node_modules/firebase-tools/lib/apiv2');
  const account = auth.getGlobalDefaultAccount();
  if (!account) throw new Error('Falta sesión de Firebase CLI.');
  phase = 'authenticate';
  await requireAuth({ project: PROJECT, user: account.user, tokens: account.tokens, nonInteractive: true });
  const client = new Client({ urlPrefix: 'https://firestore.googleapis.com', apiVersion: 'v1' });
  const resource = `/projects/${PROJECT}/databases/(default)/documents/_mailConfig/google`;
  const options = { skipLog: { body: true, resBody: true, queryParams: true } };
  phase = 'save';
  await client.patch(resource, { fields: { clientId: { stringValue: cfg.client_id }, clientSecret: { stringValue: cfg.client_secret }, enabled: { booleanValue: true } } }, options);
  phase = 'verify';
  // Firebase CLI mutates request options; a GET must not reuse PATCH's body.
  const saved = await client.get(resource, { skipLog: { body: true, resBody: true, queryParams: true } });
  const fields = saved.body.fields || {};
  if (fields.clientId?.stringValue !== cfg.client_id || fields.clientSecret?.stringValue !== cfg.client_secret || fields.enabled?.booleanValue !== true) throw new Error('La verificación remota no coincidió.');
  console.log('VERIFIED: Google mail server configuration saved and enabled for Bio. OAuth consent still requires a real user.');
})().catch(error => {
  // Only local validation errors can be shown verbatim. Never print HTTP error objects.
  const safe = ['Debe existir exactamente','El cliente no pertenece','Falta la URI','El archivo no contiene','Falta sesión','La verificación remota'];
  console.error(safe.some(s => error.message?.startsWith(s)) ? error.message : 'No se completó la configuración privada de Gmail. No se han mostrado credenciales.');
  const status = error.status || error.context?.response?.statusCode || error.context?.response?.status;
  console.error(JSON.stringify({ phase, errorType: /^[A-Za-z]+$/.test(error.name || '') ? error.name : 'Error', httpStatus: typeof status === 'number' ? status : null }));
  process.exitCode = 1;
});
