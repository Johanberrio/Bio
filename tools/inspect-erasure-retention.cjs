const auth = require('../.tools/firebase/node_modules/firebase-tools/lib/auth');
const {requireAuth} = require('../.tools/firebase/node_modules/firebase-tools/lib/requireAuth');
const {Client} = require('../.tools/firebase/node_modules/firebase-tools/lib/apiv2');
(async()=>{
 const a=auth.getGlobalDefaultAccount();
 await requireAuth({project:'bio-project-15d15',user:a.user,tokens:a.tokens,nonInteractive:true});
 const s=new Client({urlPrefix:'https://storage.googleapis.com',apiVersion:'storage/v1'});
 const b=(await s.get('/b/bio-project-15d15.firebasestorage.app',{skipLog:{resBody:true}})).body;
 const f=new Client({urlPrefix:'https://firestore.googleapis.com',apiVersion:'v1'});
 const d=(await f.get('/projects/bio-project-15d15/databases/(default)',{skipLog:{resBody:true}})).body;
 const schedules=(await f.get('/projects/bio-project-15d15/databases/(default)/backupSchedules',{skipLog:{resBody:true}})).body;
 console.log(JSON.stringify({storage:{softDeletePolicy:b.softDeletePolicy,retentionPolicy:b.retentionPolicy,versioning:b.versioning},firestore:{pointInTimeRecoveryEnablement:d.pointInTimeRecoveryEnablement,versionRetentionPeriod:d.versionRetentionPeriod,backupSchedules:schedules.backupSchedules||[]}},null,2));
})().catch(e=>{console.error('Retention inspection failed: '+(e.status||'unknown'));process.exitCode=1;});
