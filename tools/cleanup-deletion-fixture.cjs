// Only removes an Auth-only fixture left before the request phase, never a real account.
const auth=require('../.tools/firebase/node_modules/firebase-tools/lib/auth');
const {requireAuth}=require('../.tools/firebase/node_modules/firebase-tools/lib/requireAuth');
const {Client}=require('../.tools/firebase/node_modules/firebase-tools/lib/apiv2');
(async()=>{
 const uid=process.argv[2];if(!/^[A-Za-z0-9_-]{1,128}$/.test(uid||''))throw new Error('Fixture UID required');
 const a=auth.getGlobalDefaultAccount();await requireAuth({project:'bio-project-15d15',user:a.user,tokens:a.tokens,nonInteractive:true});
 const c=new Client({urlPrefix:'https://identitytoolkit.googleapis.com',apiVersion:'v1'}),options=()=>({skipLog:{body:true,resBody:true}});
 const users=(await c.post('/projects/bio-project-15d15/accounts:lookup',{localId:[uid]},options())).body.users||[];
 if(users.length!==1||!/^bio-erasure-device-[a-f0-9-]+@example\.invalid$/.test(users[0].email))throw new Error('Not an erasure test fixture');
 await c.post('/projects/bio-project-15d15/accounts:delete',{localId:uid},options());
 console.log('Removed Auth-only synthetic erasure fixture.');
})().catch(e=>{console.error('Fixture cleanup failed: '+(e.status||e.message));process.exitCode=1;});
