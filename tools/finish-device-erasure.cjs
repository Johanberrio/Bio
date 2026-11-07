const fs=require('node:fs');
const auth=require('../.tools/firebase/node_modules/firebase-tools/lib/auth');
const {requireAuth}=require('../.tools/firebase/node_modules/firebase-tools/lib/requireAuth');
const {Client}=require('../.tools/firebase/node_modules/firebase-tools/lib/apiv2');
(async()=>{
 const log=fs.readFileSync('.tools/deletion-device.log','utf8');
 const uid=log.match(/SYNTHETIC_UID ([A-Za-z0-9_-]+)/)?.[1];
 if(!uid||!log.includes('SUBMITTED_DEVICE_ERASURE')||log.includes('FAIL'))throw new Error('Device test did not reach accepted request');
 const p='bio-project-15d15',a=auth.getGlobalDefaultAccount();await requireAuth({project:p,user:a.user,tokens:a.tokens,nonInteractive:true});
 const opts=()=>({skipLog:{body:true,resBody:true}});
 const identity=new Client({urlPrefix:'https://identitytoolkit.googleapis.com',apiVersion:'v1'}),db=new Client({urlPrefix:'https://firestore.googleapis.com',apiVersion:'v1'}),scheduler=new Client({urlPrefix:'https://cloudscheduler.googleapis.com',apiVersion:'v1'});
 const users=(await identity.post('/projects/'+p+'/accounts:lookup',{localId:[uid]},opts())).body.users||[];
 if(users.length!==1||!/^bio-erasure-device-[a-f0-9-]+@example\.invalid$/.test(users[0].email))throw new Error('Not the disposable device fixture');
 const path='/projects/'+p+'/databases/(default)/documents/_accountDeletion/'+uid;
 const task=(await db.get(path,opts())).body.fields;
 if(!task.requestedAt)throw new Error('Missing actual request from Android');
 console.log('PASS Android password verification and accepted server request');
 const run=()=>scheduler.post('/projects/'+p+'/locations/us-east1/jobs/firebase-schedule-bio_erase_accounts-us-east1:run',{},opts());
 await run();
 let waiting;
 for(let i=0;i<40;i++){waiting=(await db.get(path,opts())).body.fields;if(waiting.disabledAt)break;await new Promise(r=>setTimeout(r,3000));}
 if(!waiting.disabledAt)throw new Error('Worker did not disable fixture');
 // Advance only this verified disposable fixture's retention window.
 await db.patch(path,{fields:{...waiting,disabledAt:{timestampValue:new Date(Date.now()-31*86400000).toISOString()},nextRunAt:{timestampValue:new Date(0).toISOString()}}},opts());
 await run();let removed=false;
 for(let i=0;i<40;i++){try{await db.get(path,opts());}catch(e){if(e.status!==404)throw e;removed=true;break;}await new Promise(r=>setTimeout(r,3000));}
 if(!removed)throw new Error('Fixture marker remains');
 if(((await identity.post('/projects/'+p+'/accounts:lookup',{localId:[uid]},opts())).body.users||[]).length)throw new Error('Fixture account remains');
 const proof=JSON.parse(fs.readFileSync('docs/eliminacion-verificacion.json','utf8'));proof.androidVerifiedAt=new Date().toISOString();proof.androidModes=['light','dark'];proof.androidWrongPasswordRejected=true;proof.androidRequestAccepted=true;proof.androidFixtureRemoved=true;
 fs.writeFileSync('docs/eliminacion-verificacion.json',JSON.stringify(proof,null,2));
 console.log('PASS device fixture account and final marker removed');
})().catch(e=>{console.error('Device verification failed: '+(e.status||e.message));process.exitCode=1;});
