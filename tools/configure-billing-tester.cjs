// Enables checkout for one existing Firebase account, never grants Premium.
const auth = require('../.tools/firebase/node_modules/firebase-tools/lib/auth');
const { requireAuth } = require('../.tools/firebase/node_modules/firebase-tools/lib/requireAuth');
const { Client } = require('../.tools/firebase/node_modules/firebase-tools/lib/apiv2');
(async () => {
  const email = process.argv[2];
  if (!email || !email.includes('@')) throw new Error('Email required');
  const account = auth.getGlobalDefaultAccount();
  await requireAuth({project:'bio-project-15d15',user:account.user,tokens:account.tokens,nonInteractive:true});
  const identity = new Client({urlPrefix:'https://identitytoolkit.googleapis.com',apiVersion:'v1'});
  const found = await identity.post('/projects/bio-project-15d15/accounts:lookup', {email:[email]}, {skipLog:{body:true,resBody:true}});
  const users = found.body.users || [];
  if (users.length !== 1 || users[0].email?.toLowerCase() !== email.toLowerCase() || users[0].disabled) {
    console.log('NO_MATCH: an existing active Bio account with that email is required.'); return;
  }
  const db = new Client({urlPrefix:'https://firestore.googleapis.com',apiVersion:'v1'});
  const path = '/projects/bio-project-15d15/databases/(default)/documents/_billingTesters/' + encodeURIComponent(users[0].localId);
  const expiresAt = new Date(Date.now()+7*24*3600*1000).toISOString();
  await db.patch(path,{fields:{enabled:{booleanValue:true},expiresAt:{timestampValue:expiresAt}}},{skipLog:{body:true,resBody:true}});
  const saved = await db.get(path,{skipLog:{resBody:true}});
  if (saved.body.fields?.enabled?.booleanValue !== true) throw new Error('Not saved');
  console.log(JSON.stringify({checkoutEnabled:true,expiresAt,grantsPremium:false}));
})().catch(error=>{console.error(JSON.stringify({error:'Tester configuration failed',status:error.status||null}));process.exitCode=1;});
