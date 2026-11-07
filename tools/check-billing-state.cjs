// Read-only: never print purchase tokens, user IDs or log payloads.
const auth = require('../.tools/firebase/node_modules/firebase-tools/lib/auth');
const {requireAuth} = require('../.tools/firebase/node_modules/firebase-tools/lib/requireAuth');
const {Client} = require('../.tools/firebase/node_modules/firebase-tools/lib/apiv2');
(async()=>{
 const account=auth.getGlobalDefaultAccount();
 await requireAuth({project:'bio-project-15d15',user:account.user,tokens:account.tokens,nonInteractive:true});
 const id=new Client({urlPrefix:'https://identitytoolkit.googleapis.com',apiVersion:'v1'});
 const found=await id.post('/projects/bio-project-15d15/accounts:lookup',{email:[process.argv[2]]},{skipLog:{body:true,resBody:true}});
 const uid=found.body.users?.[0]?.localId;if(!uid)throw Error('Account not found');
 const db=new Client({urlPrefix:'https://firestore.googleapis.com',apiVersion:'v1'});
 try {const r=await db.get('/projects/bio-project-15d15/databases/(default)/documents/_billingTesters/'+encodeURIComponent(uid),{skipLog:{resBody:true}});const f=r.body.fields||{};const expiresAt=f.expiresAt?.timestampValue;console.log(JSON.stringify({testerEnabled:f.enabled?.booleanValue===true,testerExpiresAt:expiresAt,testerActive:f.enabled?.booleanValue===true&&Date.parse(expiresAt)>Date.now()}));}catch(e){console.log(JSON.stringify({testerFound:false,status:e.status}));}
 try {const r=await db.get('/projects/bio-project-15d15/databases/(default)/documents/_playSubscriptions/'+encodeURIComponent(uid),{skipLog:{resBody:true}});const f=r.body.fields||{};console.log(JSON.stringify({stored:true,hasToken:!!f.token?.stringValue,premium:f.premium?.booleanValue,expiresAt:f.expiresAt?.stringValue,checkedAt:f.checkedAt?.timestampValue}));}catch(e){console.log(JSON.stringify({stored:false,status:e.status}));}
 const logs=new Client({urlPrefix:'https://logging.googleapis.com',apiVersion:'v2'});
 const r=await logs.post('/entries:list',{resourceNames:['projects/bio-project-15d15'],filter:'resource.type="cloud_run_revision" AND resource.labels.service_name="bio-api" AND httpRequest.requestUrl:"billing"',orderBy:'timestamp desc',pageSize:25},{skipLog:{body:true,resBody:true}});
 console.log(JSON.stringify((r.body.entries||[]).map(e=>({time:e.timestamp,status:e.httpRequest?.status,path:new URL(e.httpRequest.requestUrl).pathname})),null,2));
})().catch(e=>{console.error(JSON.stringify({error:'Read-only billing check failed',status:e.status}));process.exitCode=1;});
