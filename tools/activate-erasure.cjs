// Activate only after the recorded live test and a private, enabled scheduler are verified.
const fs=require('node:fs');
const auth=require('../.tools/firebase/node_modules/firebase-tools/lib/auth');
const {requireAuth}=require('../.tools/firebase/node_modules/firebase-tools/lib/requireAuth');
const {Client}=require('../.tools/firebase/node_modules/firebase-tools/lib/apiv2');
(async()=>{
 const proof=JSON.parse(fs.readFileSync('docs/eliminacion-verificacion.json','utf8'));
 if(!proof.accountDeleted||!proof.storageActiveVersionsDeleted||!proof.markerFinalSweep)throw new Error('Missing verification');
 const project='bio-project-15d15',a=auth.getGlobalDefaultAccount();await requireAuth({project,user:a.user,tokens:a.tokens,nonInteractive:true});
 const opts=()=>({skipLog:{body:true,resBody:true}});
 const scheduler=new Client({urlPrefix:'https://cloudscheduler.googleapis.com',apiVersion:'v1'});
 const job=(await scheduler.get('/projects/'+project+'/locations/us-east1/jobs/firebase-schedule-bio_erase_accounts-us-east1',opts())).body;
 if(job.state!=='ENABLED'||!job.httpTarget?.oidcToken?.serviceAccountEmail)throw new Error('Scheduler is not ready');
 const functions=new Client({urlPrefix:'https://cloudfunctions.googleapis.com',apiVersion:'v2'});
 const f=(await functions.get('/projects/'+project+'/locations/us-east1/functions/bio_erase_accounts',opts())).body;
 if(f.state!=='ACTIVE'||f.serviceConfig.serviceAccountEmail!=='bio-erasure@'+project+'.iam.gserviceaccount.com')throw new Error('Incorrect worker identity');
 const run=new Client({urlPrefix:'https://run.googleapis.com',apiVersion:'v2'});
 const policy=(await run.get('/'+f.serviceConfig.service+':getIamPolicy',opts())).body;
 if((policy.bindings||[]).some(b=>(b.members||[]).some(m=>m==='allUsers'||m==='allAuthenticatedUsers')))throw new Error('Worker must not be publicly invokable');
 const db=new Client({urlPrefix:'https://firestore.googleapis.com',apiVersion:'v1'});
 const path='/projects/'+project+'/databases/(default)/documents/_accountConfig/deletion';
 await db.patch(path,{fields:{enabled:{booleanValue:true},verifiedAt:{timestampValue:proof.verifiedAt}}},opts());
 console.log('PASS private worker, authenticated hourly scheduler and deletion activation.');
})().catch(e=>{console.error('Activation failed: '+(e.status||e.message));process.exitCode=1;});
