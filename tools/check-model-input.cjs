// Diagnostic token count only: synthetic text, no generation or user document.
const auth = require('../.tools/firebase/node_modules/firebase-tools/lib/auth');
const {requireAuth} = require('../.tools/firebase/node_modules/firebase-tools/lib/requireAuth');
const {Client} = require('../.tools/firebase/node_modules/firebase-tools/lib/apiv2');
(async()=>{
 const account=auth.getGlobalDefaultAccount();
 await requireAuth({project:'bio-project-15d15',user:account.user,tokens:account.tokens,nonInteractive:true});
 const iam=new Client({urlPrefix:'https://cloudresourcemanager.googleapis.com',apiVersion:'v1'});
 const policy=await iam.post('/projects/bio-project-15d15:getIamPolicy',{}, {skipLog:{body:true,resBody:true}});
 console.log(JSON.stringify({analysisRoles:policy.body.bindings.filter(b=>b.members?.includes('serviceAccount:bio-analysis@bio-project-15d15.iam.gserviceaccount.com')).map(b=>b.role)}));
 const api=new Client({urlPrefix:'https://aiplatform.googleapis.com',apiVersion:'v1'});
 const result=await api.post('/projects/bio-project-15d15/locations/global/publishers/google/models/gemini-3.1-flash-lite:countTokens',{contents:[{role:'user',parts:[{text:'Synthetic diagnostic input. No patient data.'}]}]},{skipLog:{body:true,resBody:true}});
 console.log(JSON.stringify({tokenCount:result.body.totalTokens}));
 const pdf=require('node:fs').readFileSync('output/pdf/resultado_prueba_bio.pdf');
 const counted=await api.post('/projects/bio-project-15d15/locations/global/publishers/google/models/gemini-3.1-flash-lite:countTokens',{contents:[{role:'user',parts:[{text:'Synthetic diagnostic document.'},{inlineData:{mimeType:'application/pdf',data:pdf.toString('base64')}}]}]},{skipLog:{body:true,resBody:true}});
 console.log(JSON.stringify({syntheticPdfTokens:counted.body.totalTokens}));
 const {getAccessToken}=require('../.tools/firebase/node_modules/firebase-tools/lib/apiv2');
 const token=await getAccessToken();
 const resultSdk=require('node:child_process').spawnSync('server/venv/Scripts/python.exe',['tools/check-model-sdk.py'],{input:token,encoding:'utf8',windowsHide:true,timeout:45000});
 if(resultSdk.stdout)process.stdout.write(resultSdk.stdout);
 if(resultSdk.status!==0){console.log(JSON.stringify({sdkCheckExit:resultSdk.status}));process.exitCode=1;}
})().catch(e=>{const body=e.context?.body;console.error(JSON.stringify({error:'Synthetic token count failed',status:e.status,details:body?.error?.message||null}));process.exitCode=1;});
