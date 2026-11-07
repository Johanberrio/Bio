// Provision one isolated reviewer account. Never prints credentials or calls AI.
// Re-running preserves the password, profile and quota. --enable grants bounded
// Premium access after deployment; --verify performs read-only access checks.
const fs = require('node:fs');
const crypto = require('node:crypto');
const auth = require('../.tools/firebase/node_modules/firebase-tools/lib/auth');
const {requireAuth} = require('../.tools/firebase/node_modules/firebase-tools/lib/requireAuth');
const {Client} = require('../.tools/firebase/node_modules/firebase-tools/lib/apiv2');
const project = 'bio-project-15d15';
const email = 'play-review@bio-project-15d15.example.invalid';
const file = '.secrets/play-review.json';
const base = 'https://us-east1-' + project + '.cloudfunctions.net/bio_api';
const root = '/projects/' + project + '/databases/(default)/documents';
const opts = () => ({skipLog: {body: true, resBody: true}});
const assert = (ok, message) => {if (!ok) throw new Error(message); console.log('PASS ' + message);};

(async () => {
  const cfg = JSON.parse(fs.readFileSync('app/src/main/assets/firebase-config.json', 'utf8'));
  assert(cfg.projectId === project, 'Bio project confirmed');
  const account = auth.getGlobalDefaultAccount();
  await requireAuth({project, user: account.user, tokens: account.tokens, nonInteractive: true});
  const db = new Client({urlPrefix: 'https://firestore.googleapis.com', apiVersion: 'v1'});
  const identity = new Client({urlPrefix: 'https://identitytoolkit.googleapis.com', apiVersion: 'v1'});
  const get = async path => {
    try {return (await db.get(root + '/' + path, opts())).body.fields;}
    catch (e) {if (e.status === 404) return null; throw e;}
  };
  let saved;
  if (fs.existsSync(file)) {
    saved = JSON.parse(fs.readFileSync(file, 'utf8'));
    assert(saved.project === project && saved.email === email && /^[A-Za-z0-9_-]+$/.test(saved.uid), 'existing reviewer credentials scoped to Bio');
  } else {
    if (process.argv.includes('--verify')) throw new Error('Prepare the account first');
    // An exact random UID is reserved locally before the create operation, so an
    // interrupted run cannot silently create a second account or reset a password.
    saved = {project, uid: 'bio-play-review-' + crypto.randomUUID(), email,
      password: crypto.randomBytes(24).toString('base64url'), createdAt: new Date().toISOString()};
    fs.mkdirSync('.secrets', {recursive: true});
    fs.writeFileSync(file, JSON.stringify(saved, null, 2), {flag: 'wx', mode: 0o600});
  }
  let users = (await identity.post('/projects/' + project + '/accounts:lookup', {localId: [saved.uid]}, opts())).body.users || [];
  if (!users.length && !process.argv.includes('--verify')) {
    // Admin account creation API; no invitation email or mailbox is involved.
    await identity.post('/projects/' + project + '/accounts', {localId: saved.uid, email: saved.email,
      password: saved.password, displayName: 'Bio Review Test', emailVerified: false}, opts());
    users = (await identity.post('/projects/' + project + '/accounts:lookup', {localId: [saved.uid]}, opts())).body.users || [];
  }
  assert(users.length === 1 && users[0].email === email && users[0].disabled !== true, 'isolated synthetic reviewer account exists');
  if (await get('_accountDeletion/' + saved.uid)) throw new Error('Reviewer has requested deletion; do not recreate it');
  const profilePath = 'users/' + saved.uid + '/profile/main';
  if (!await get(profilePath) && !process.argv.includes('--verify')) {
    const profile = {id: 'main', owner: project + ':' + saved.uid, fullName: 'Bio Review Test', birthDate: '1995-01-01',
      heightCm: 170, weightKg: 65.5, bloodGroup: 'O+', bodyModel: 'female', updatedAt: Date.now(), cloudState: 'synced'};
    await db.patch(root + '/' + profilePath, {fields: {payload: {stringValue: JSON.stringify(profile)}, updatedAt: {timestampValue: new Date().toISOString()}}}, opts());
  }
  if (process.argv.includes('--enable')) {
    await db.patch(root + '/_reviewAccounts/' + saved.uid, {fields: {
      enabled: {booleanValue: true}, purpose: {stringValue: 'google_play_review'}, uid: {stringValue: saved.uid},
      syntheticOnly: {booleanValue: true}, grantedAt: {timestampValue: new Date().toISOString()}
    }}, opts());
    console.log('Reviewer grant enabled. Standard 30/20 quotas remain unchanged.');
  }
  const login = await fetch('https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=' + cfg.apiKey, {
    method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({email, password: saved.password, returnSecureToken: true})});
  assert(login.ok, 'reviewer password sign-in works without a one-time code');
  const session = await login.json();
  assert(session.localId === saved.uid, 'signed-in identity matches reviewer');
  const headers = {Authorization: 'Bearer ' + session.idToken, 'Content-Type': 'application/json'};
  const post = (path, data) => fetch(base + path, {method: 'POST', headers, body: JSON.stringify(data)});
  const response = await post('/billing/status', {});
  assert(response.ok, 'billing status available');
  const status = await response.json();
  if (process.argv.includes('--enable') || process.argv.includes('--verify')) {
    assert(status.premium === true && status.accessSource === 'google_play_review', 'reviewer Premium access active');
    assert(status.purchasesEnabled === false, 'reviewer does not need checkout');
    assert(status.usage.analysis.limit === 30 && status.usage.research.limit === 20, 'same monthly Premium allowances');
    const privateUrl = 'https://firestore.googleapis.com/v1' + root + '/_reviewAccounts/' + saved.uid;
    assert((await fetch(privateUrl, {headers})).status === 403, 'reviewer cannot read access grants');
    assert((await fetch(privateUrl, {method: 'PATCH', headers, body: JSON.stringify({fields: {enabled: {booleanValue: true}}})})).status === 403, 'reviewer cannot modify access grants');
    // Invalid requests must stop before tokens or generation; no model is called.
    for (const path of ['/research', '/analyze-document']) assert((await post(path, {})).status === 400, 'Premium route reachable, invalid input rejected: ' + path);
    fs.writeFileSync('docs/play-review-verification.json', JSON.stringify({verifiedAt: new Date().toISOString(),
      project, syntheticOnly: true, login: true, premium: true, purchasesEnabled: false,
      monthlyAnalysisLimit: 30, monthlyResearchLimit: 20, privateGrantProtected: true, noAIInvoked: true}, null, 2));
  }
  console.log('Credentials kept only in ignored .secrets/play-review.json; not printed or sent to Play Console.');
})().catch(e => {console.error('Reviewer preparation failed: ' + (e.status || e.message || 'unknown')); process.exitCode = 1;});
