// Read-only deployment checks; never print credentials or provider response bodies.
const auth = require('../.tools/firebase/node_modules/firebase-tools/lib/auth');
const { requireAuth } = require('../.tools/firebase/node_modules/firebase-tools/lib/requireAuth');
const { Client } = require('../.tools/firebase/node_modules/firebase-tools/lib/apiv2');
(async () => {
  const account = auth.getGlobalDefaultAccount();
  if (!account) throw new Error('No CLI session');
  await requireAuth({ project: 'bio-project-15d15', user: account.user, tokens: account.tokens, nonInteractive: true });
  const client = new Client({ urlPrefix: 'https://cloudfunctions.googleapis.com', apiVersion: 'v2' });
  const result = await client.get('/projects/bio-project-15d15/locations/us-east1/functions/bio_api', {skipLog: {resBody: true}});
  const fn = result.body, cfg = fn.serviceConfig || {}, env = cfg.environmentVariables || {};
  console.log(JSON.stringify({state: fn.state, updated: fn.updateTime, runtime: fn.buildConfig?.runtime,
    serviceAccount: cfg.serviceAccountEmail, minInstances: cfg.minInstanceCount || 0,
    maxInstances: cfg.maxInstanceCount, uri: cfg.uri,
    models: {research: env.BIO_MODEL || '(default)', extraction: env.BIO_EXTRACTION_MODEL || '(default)', review: env.BIO_REVIEW_MODEL || '(default)'},
    purchasesEnabled: env.BIO_PREMIUM_READY === 'true'}, null, 2));
})().catch(error => {
  console.error(JSON.stringify({error: 'Deployment check failed', status: error.status || null}));
  process.exitCode = 1;
});
