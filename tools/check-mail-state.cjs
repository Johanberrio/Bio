// Read-only diagnostics. No email content, attachments, IDs or tokens are printed.
const auth = require('../.tools/firebase/node_modules/firebase-tools/lib/auth');
const {requireAuth} = require('../.tools/firebase/node_modules/firebase-tools/lib/requireAuth');
const {Client} = require('../.tools/firebase/node_modules/firebase-tools/lib/apiv2');
(async()=>{
 const account=auth.getGlobalDefaultAccount();
 await requireAuth({project:'bio-project-15d15',user:account.user,tokens:account.tokens,nonInteractive:true});
 const identity=new Client({urlPrefix:'https://identitytoolkit.googleapis.com',apiVersion:'v1'});
 const found=await identity.post('/projects/bio-project-15d15/accounts:lookup',{email:[process.argv[2]]},{skipLog:{body:true,resBody:true}});
 const uid=found.body.users?.[0]?.localId;if(!uid)throw Error('Account not found');
 const db=new Client({urlPrefix:'https://firestore.googleapis.com',apiVersion:'v1'});
 const base='/projects/bio-project-15d15/databases/(default)/documents/';
 const list=await db.get(base+'_mailUsers/'+encodeURIComponent(uid)+'/accounts?pageSize=100&mask.fieldPaths=provider',{skipLog:{resBody:true}});
 console.log(JSON.stringify({connectedAccounts:(list.body.documents||[]).map(d=>({provider:d.fields?.provider?.stringValue}))}));
 const logs=new Client({urlPrefix:'https://logging.googleapis.com',apiVersion:'v2'});
 const since=new Date(Date.now()-6*60*60*1000).toISOString();
 const result=await logs.post('/entries:list',{resourceNames:['projects/bio-project-15d15'],filter:'resource.type="cloud_run_revision" AND resource.labels.service_name="bio-api" AND timestamp>="'+since+'" AND (httpRequest.requestUrl:"/mail/" OR httpRequest.requestUrl:"/billing/" OR httpRequest.requestUrl:"/analyze-document")',orderBy:'timestamp desc',pageSize:80},{skipLog:{body:true,resBody:true}});
 console.log(JSON.stringify({serviceRequests:(result.body.entries||[]).slice(0,12).map(e=>({time:e.timestamp,status:e.httpRequest?.status,path:new URL(e.httpRequest.requestUrl).pathname,latency:e.httpRequest?.latency}))},null,2));
 const diagnostic=await logs.post('/entries:list',{resourceNames:['projects/bio-project-15d15'],filter:'resource.type="cloud_run_revision" AND resource.labels.service_name="bio-api" AND jsonPayload.event="bio_analysis_failed" AND timestamp>="'+since+'"',orderBy:'timestamp desc',pageSize:20},{skipLog:{body:true,resBody:true}});
 console.log(JSON.stringify({analysisFailures:(diagnostic.body.entries||[]).map(e=>({time:e.timestamp,stage:e.jsonPayload?.stage,providerCode:e.jsonPayload?.providerCode}))}));
})().catch(e=>{console.error(JSON.stringify({error:'Read-only mail check failed',status:e.status}));process.exitCode=1;});
