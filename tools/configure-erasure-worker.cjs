// Provision only Bio's dedicated erasure identity; no keys, no permissions on other buckets.
const auth=require('../.tools/firebase/node_modules/firebase-tools/lib/auth');
const {requireAuth}=require('../.tools/firebase/node_modules/firebase-tools/lib/requireAuth');
const {Client}=require('../.tools/firebase/node_modules/firebase-tools/lib/apiv2');
const project='bio-project-15d15', email='bio-erasure@'+project+'.iam.gserviceaccount.com';
(async()=>{
 if(!process.argv.includes('--apply'))throw new Error('Explicit --apply required');
 const a=auth.getGlobalDefaultAccount();await requireAuth({project,user:a.user,tokens:a.tokens,nonInteractive:true});
 const opts=()=>({skipLog:{resBody:true,reqBody:true}});
 const iam=new Client({urlPrefix:'https://iam.googleapis.com',apiVersion:'v1'});
 console.log('Checking dedicated service identity');
 try{await iam.get('/projects/'+project+'/serviceAccounts/'+email,opts());}catch(e){if(e.status!==404)throw e;await iam.post('/projects/'+project+'/serviceAccounts',{accountId:'bio-erasure',serviceAccount:{displayName:'Bio account erasure worker'}},opts());}
 const member='serviceAccount:'+email;
 const add=(p,role)=>{p.bindings=p.bindings||[];let b=p.bindings.find(x=>x.role===role&&!x.condition);if(!b){b={role,members:[]};p.bindings.push(b);}if(!b.members.includes(member))b.members.push(member);};
 const crm=new Client({urlPrefix:'https://cloudresourcemanager.googleapis.com',apiVersion:'v1'});
 console.log('Configuring project roles');
 const p=(await crm.post('/projects/'+project+':getIamPolicy',{},opts())).body;
 add(p,'roles/datastore.user');add(p,'roles/firebaseauth.admin');
 await crm.post('/projects/'+project+':setIamPolicy',{policy:p},opts());
 const storage=new Client({urlPrefix:'https://storage.googleapis.com',apiVersion:'storage/v1'});
 const path='/b/'+project+'.firebasestorage.app/iam';
 console.log('Configuring Bio bucket role');
 const policy=(await storage.get(path,opts())).body;add(policy,'roles/storage.objectAdmin');
 await storage.put(path,policy,opts());
 console.log('Configured dedicated Bio erasure worker: Firestore, Firebase Auth and ONLY Bio storage bucket. No service-account keys created.');
})().catch(e=>{console.error('Worker setup failed: '+(e.status||e.message||'unknown'));process.exitCode=1;});
