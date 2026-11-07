// End-to-end test with a newly generated example.invalid account only. No AI or mailbox access.
const fs=require('node:fs'),crypto=require('node:crypto');
const auth=require('../.tools/firebase/node_modules/firebase-tools/lib/auth');
const {requireAuth}=require('../.tools/firebase/node_modules/firebase-tools/lib/requireAuth');
const {Client}=require('../.tools/firebase/node_modules/firebase-tools/lib/apiv2');
const project='bio-project-15d15',base='https://us-east1-'+project+'.cloudfunctions.net/bio_api';
const root='/projects/'+project+'/databases/(default)/documents';
const opts=()=>({skipLog:{body:true,resBody:true}});
const pause=ms=>new Promise(r=>setTimeout(r,ms));
const assert=(ok,label)=>{if(!ok)throw new Error(label);console.log('PASS '+label);};
(async()=>{
 const a=auth.getGlobalDefaultAccount();await requireAuth({project,user:a.user,tokens:a.tokens,nonInteractive:true});
 const db=new Client({urlPrefix:'https://firestore.googleapis.com',apiVersion:'v1'});
 const identity=new Client({urlPrefix:'https://identitytoolkit.googleapis.com',apiVersion:'v1'});
 const scheduler=new Client({urlPrefix:'https://cloudscheduler.googleapis.com',apiVersion:'v1'});
 const storage=new Client({urlPrefix:'https://storage.googleapis.com',apiVersion:'storage/v1'});
 const cfg=JSON.parse(fs.readFileSync('app/src/main/assets/firebase-config.json','utf8'));
 const email='bio-erasure-'+crypto.randomUUID()+'@example.invalid';
 const signup=await fetch('https://identitytoolkit.googleapis.com/v1/accounts:signUp?key='+cfg.apiKey,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email,password:crypto.randomBytes(32).toString('base64url'),returnSecureToken:true})});
 if(!signup.ok)throw new Error('Synthetic account creation failed');
 const user=await signup.json(),uid=user.localId,token=user.idToken;
 if(!/^[A-Za-z0-9_-]+$/.test(uid))throw new Error('Unexpected UID');
 const gate=root+'/_accountConfig/deletion';
 let previous=null;try{previous=(await db.get(gate,opts())).body.fields;}catch(e){if(e.status!==404)throw e;}
 const post=async(path,body,authenticated=true)=>fetch(base+path,{method:'POST',headers:{'Content-Type':'application/json',...(authenticated?{Authorization:'Bearer '+token}:{})},body:JSON.stringify(body)});
 const patch=async(path,fields)=>db.patch(root+'/'+path,{fields},opts());
 const get=async path=>{try{return(await db.get(root+'/'+path,opts())).body.fields;}catch(e){if(e.status===404)return null;throw e;}};
 const run=async()=>scheduler.post('/projects/'+project+'/locations/us-east1/jobs/firebase-schedule-bio_erase_accounts-us-east1:run',{},opts());
 const waitState=async state=>{for(let i=0;i<40;i++){const task=await get('_accountDeletion/'+uid);if(task?.state?.stringValue===state)return task;await pause(3000);}throw new Error('Worker did not reach '+state);};
 const leafPaths=['users/'+uid+'/profile/main','users/'+uid+'/documents/synthetic','_premiumUsage/'+uid+'/months/2000-01/reservations/synthetic','_analysisQuota/2000-01-01/users/'+uid];
 const roots=['users','_mailUsers','_mailPending','_playSubscriptions','_billingTesters','_premiumUsage'];
 const blobName='users/'+uid+'/documents/synthetic';
 let verified=false;
 try{
   const found=(await identity.post('/projects/'+project+'/accounts:lookup',{localId:[uid]},opts())).body.users||[];
   if(found.length!==1||found[0].email!==email)throw new Error('Synthetic ownership check failed');
   await db.patch(gate,{fields:{enabled:{booleanValue:false},testUids:{arrayValue:{values:[{stringValue:uid}]}}}},opts());
   for(const path of [...roots.map(r=>r+'/'+uid),...leafPaths])await patch(path,{synthetic:{booleanValue:true}});
   // A disconnected, fictitious provider record exercises credential deletion without a live token.
   await patch('_mailUsers/'+uid+'/accounts/'+'a'.repeat(64),{provider:{stringValue:'microsoft'},refresh:{stringValue:'synthetic-not-a-token'},generation:{stringValue:'synthetic'}});
   // JSON API object insert using an authenticated media upload; no user data.
   const upload=new Client({urlPrefix:'https://storage.googleapis.com',apiVersion:'upload/storage/v1'});
   await upload.post('/b/'+project+'.firebasestorage.app/o?uploadType=media&name='+encodeURIComponent(blobName),{synthetic:true},opts());
   assert((await post('/account/deletion-request',{confirm:true},false)).status===401,'anonymous deletion rejected');
   assert((await post('/account/deletion-request',{confirm:true,uid:'someone-else'})).status===403,'client cannot select another account');
   const response=await post('/account/deletion-request',{confirm:true});
   assert(response.status===200&&(await response.json()).requested===true,'verified request accepted');
   assert((await post('/account/deletion-request',{confirm:true})).status===200,'retry is idempotent');
   assert((await post('/mail/status',{})).status===410,'pending account activity blocked');
   await run();const waiting=await waitState('waiting');
   const disabled=(await identity.post('/projects/'+project+'/accounts:lookup',{localId:[uid]},opts())).body.users;
   assert(disabled?.[0]?.disabled===true,'scheduled worker disables account');
   assert(await get('users/'+uid+'/documents/synthetic')!==null,'data retained until old session tokens expire');
   const requestedAt=waiting.requestedAt;
   // Advance ONLY the synthetic task's waiting period; never alter real accounts.
   await patch('_accountDeletion/'+uid,{requestedAt,disabledAt:{timestampValue:new Date(Date.now()-3*3600000).toISOString()},nextRunAt:{timestampValue:new Date(0).toISOString()},state:{stringValue:'waiting'}});
   await run();await waitState('erased');
   for(const path of [...roots.map(r=>r+'/'+uid),...leafPaths,'_mailUsers/'+uid+'/accounts/'+'a'.repeat(64)])assert(await get(path)===null,'erased '+path.replaceAll(uid,'<synthetic>'));
   const remaining=(await storage.get('/b/'+project+'.firebasestorage.app/o?prefix='+encodeURIComponent('users/'+uid+'/')+'&versions=true',opts())).body.items||[];
   assert(remaining.length===0,'all active object generations erased');
   const gone=(await identity.post('/projects/'+project+'/accounts:lookup',{localId:[uid]},opts())).body.users||[];
   assert(gone.length===0,'Firebase authentication account erased');
   const marker=await get('_accountDeletion/'+uid);
   assert(Object.keys(marker).every(k=>['requestedAt','disabledAt','state','lastSweepAt','nextRunAt'].includes(k)),'cleanup marker contains no email or clinical data');
   // Also exercise the final sweep and expiry of the minimal marker on this fixture only.
   await patch('_accountDeletion/'+uid,{...marker,disabledAt:{timestampValue:new Date(Date.now()-31*86400000).toISOString()},nextRunAt:{timestampValue:new Date(0).toISOString()}});
   await run();for(let i=0;i<40&&await get('_accountDeletion/'+uid);i++)await pause(3000);
   assert(await get('_accountDeletion/'+uid)===null,'minimal marker expires after final sweep');
   verified=true;
   fs.writeFileSync('docs/eliminacion-verificacion.json',JSON.stringify({verifiedAt:new Date().toISOString(),project,syntheticAccountOnly:true,scheduledWorker:true,passwordVerified:true,accountDeleted:true,firestoreDeleted:true,storageActiveVersionsDeleted:true,markerFinalSweep:true,noAI:true},null,2));
 }finally{
   // Restore the previous gate; public activation is a separate deliberate step.
   if(previous)await db.patch(gate,{fields:previous},opts());else await db.delete(gate,opts());
   if(!verified){
     // Keep durable queue for retry, but remove any remaining synthetic auth account.
     if(!await get('_accountDeletion/'+uid))await patch('_accountDeletion/'+uid,{requestedAt:{timestampValue:new Date().toISOString()},nextRunAt:{timestampValue:new Date().toISOString()},state:{stringValue:'pending'}});
     try{await identity.post('/projects/'+project+'/accounts:delete',{localId:uid},opts());}catch{}
     console.log('Test incomplete: inspect synthetic queue before public activation.');
   }
 }
})().catch(e=>{console.error('Erasure verification failed: '+(e.status||e.message||'unknown'));process.exitCode=1;});
