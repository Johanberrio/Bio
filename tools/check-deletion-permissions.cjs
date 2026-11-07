// Read-only: inspect only the permissions needed by Bio's deletion worker.
const auth = require('../.tools/firebase/node_modules/firebase-tools/lib/auth');
const {requireAuth} = require('../.tools/firebase/node_modules/firebase-tools/lib/requireAuth');
const {Client} = require('../.tools/firebase/node_modules/firebase-tools/lib/apiv2');
(async()=>{
 const a=auth.getGlobalDefaultAccount();
 await requireAuth({project:'bio-project-15d15',user:a.user,tokens:a.tokens,nonInteractive:true});
 const c=new Client({urlPrefix:'https://cloudresourcemanager.googleapis.com',apiVersion:'v1'});
 const r=await c.post('/projects/bio-project-15d15:getIamPolicy',{}, {skipLog:{resBody:true}});
 const member='serviceAccount:bio-analysis@bio-project-15d15.iam.gserviceaccount.com';
 console.log(JSON.stringify({roles:r.body.bindings.filter(b=>b.members?.includes(member)).map(b=>b.role)}));
})().catch(e=>{console.error('Permission inspection failed: '+(e.status||'unknown'));process.exitCode=1;});
