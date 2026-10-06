package com.alvia.health;

import android.content.Context;
import org.json.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Durable per-account page cursor; acknowledge only after staging or explicit exclusion. */
final class MailSync {
    static final String PREFIX="mail-";
    static boolean admissible(JSONObject analysis){return analysis.optBoolean("readable")&&"matches".equals(analysis.optString("patient_match"))&&Arrays.asList("exam","history").contains(analysis.optString("document_kind"));}
    static boolean active(Vault v,FirebaseCloud cloud,String owner,String id)throws Exception{return cloud.account()&&owner.equals(cloud.owner())&&SourceSync.setting(v,id).optBoolean("enabled");}
    static void saveProgress(Vault v,String id,JSONObject progress)throws Exception{
        synchronized(SourceSync.LOCK){JSONObject current=SourceSync.setting(v,id);if(!current.optBoolean("enabled"))return;
            for(String key:Arrays.asList("since","until","cursor","lastComplete","lastAttempt","status"))if(progress.has(key))current.put(key,progress.get(key));
            v.put("sources",current);}
    }
    static JSONObject refresh(Context c,Vault v,FirebaseCloud cloud)throws Exception{
        JSONObject response=new MailApi(c,cloud).post("/mail/status",new JSONObject());String owner=cloud.owner();Set<String> connected=new HashSet<>();
        JSONArray list=response.getJSONArray("accounts");
        synchronized(SourceSync.LOCK){for(int i=0;i<list.length();i++){JSONObject a=list.getJSONObject(i);String id=PREFIX+a.getString("id");connected.add(id);
            JSONObject local=SourceSync.setting(v,id);if(local.length()==0)local=new JSONObject().put("id",id).put("enabled",true).put("owner",owner);
            // A server-confirmed reconnection must restore a source disabled by a prior refresh.
            // Preserve an explicit disconnect that still needs to reach the server.
            if(!"Desconexión pendiente del servidor".equals(local.optString("status"))){
                if(!local.optBoolean("enabled"))local.put("status","Conectado · listo para sincronizar");
                local.put("enabled",true).put("remoteDisconnected",false);
            }
            local.put("account",a.getString("id")).put("provider",a.getString("provider")).put("email",a.getString("email"));v.put("sources",local);}
            for(JSONObject local:v.list("sources"))if(local.optString("id").startsWith(PREFIX)&&owner.equals(local.optString("owner"))&&!connected.contains(local.optString("id"))){local.put("enabled",false).put("status","Cuenta desconectada");v.put("sources",local);}
        }
        if(list.length()>0)SourceSync.schedule(c);return response;
    }
    static void disconnect(Context c,Vault v,FirebaseCloud cloud,JSONObject source)throws Exception{
        synchronized(SourceSync.LOCK){JSONObject current=SourceSync.setting(v,source.getString("id"));current.put("enabled",false).put("status","Desconexión pendiente del servidor");v.put("sources",current);}
        new MailApi(c,cloud).post("/mail/disconnect",new JSONObject().put("account",source.getString("account")));
        synchronized(SourceSync.LOCK){JSONObject current=SourceSync.setting(v,source.getString("id"));current.put("remoteDisconnected",true).put("status","Desconectado");v.put("sources",current);}
    }
    static int stage(Context c,Vault v,FirebaseCloud cloud,DocumentPipeline pipeline)throws Exception{
        String owner=cloud.owner();MailApi api=new MailApi(c,cloud);int imported=0, analyzed=0;long deadline=System.currentTimeMillis()+4*60*1000;
        List<JSONObject> sources=v.list("sources");sources.sort(Comparator.comparingLong(s->s.optLong("lastAttempt")));
        for(JSONObject source:sources){
            if(analyzed>=1||System.currentTimeMillis()>deadline)break;
            String id=source.optString("id");if(!id.startsWith(PREFIX)||!owner.equals(source.optString("owner"))||!active(v,cloud,owner,id))continue;
            int before=imported;source.put("lastAttempt",System.currentTimeMillis());
            try{
                long now=System.currentTimeMillis()/1000;
                if(source.optLong("until")==0||source.optLong("since")<now-31*86400){source.put("since",Math.max(now-30*86400,source.optLong("lastComplete",now-30*86400)-86400)).put("until",now).put("cursor","");}
                Set<String> seen=new HashSet<>();for(JSONObject entry:v.list("mailSeen"))seen.add(entry.optString("id"));
                boolean completed=false;int excluded=0,candidates=0,duplicates=0;
                for(int page=0;page<10&&!completed&&analyzed<1&&System.currentTimeMillis()<deadline;page++){
                    if(!active(v,cloud,owner,id))break;
                    JSONObject batch=api.post("/mail/scan",new JSONObject().put("account",source.getString("account")).put("since",source.getLong("since")).put("until",source.getLong("until")).put("cursor",source.optString("cursor")));
                    JSONArray files=batch.getJSONArray("attachments");boolean consumed=true;
                    for(int i=0;i<files.length();i++){
                        JSONObject file=files.getJSONObject(i);candidates++;String key=SourceSync.hash((owner+":"+id+":"+file.getString("key")).getBytes(StandardCharsets.UTF_8));if(seen.contains(key)){duplicates++;continue;}
                        if(analyzed>=1||System.currentTimeMillis()>deadline||!active(v,cloud,owner,id)){consumed=false;break;}
                        JSONObject binary=api.post("/mail/download",new JSONObject().put("account",source.getString("account")).put("message",file.getString("message")).put("attachment",file.getString("attachment")));
                        byte[] bytes=android.util.Base64.decode(binary.getString("data"),android.util.Base64.DEFAULT);
                        if(bytes.length==0||bytes.length>10*1024*1024)throw new java.io.IOException("Adjunto demasiado grande.");
                        String fingerprint=SourceSync.hash(bytes),ledger=SourceSync.hash((owner+":"+fingerprint).getBytes(StandardCharsets.UTF_8));boolean duplicate=false;
                        for(JSONObject entry:v.list("importLedger"))if(ledger.equals(entry.optString("id"))){duplicate=true;break;}
                        for(JSONObject doc:v.list("documents"))if(fingerprint.equals(doc.optString("fingerprint"))){duplicate=true;break;}
                        String reason="Ya importado";
                        if(!duplicate){
                            if(!HealthProfile.complete(v))throw new java.io.IOException("Completa tu perfil, incluido nombre completo y fecha de nacimiento, para identificar tus documentos.");
                            JSONObject p=HealthProfile.current(v),patient=new JSONObject().put("fullName",p.getString("fullName")).put("birthDate",p.getString("birthDate"));
                            JSONObject result=api.post("/analyze-document",new JSONObject().put("mailImport",true).put("patient",patient).put("profile",HealthProfile.clinical(v)).put("document",new JSONObject().put("mime",binary.getString("mime")).put("data",binary.getString("data"))));analyzed++;
                            if(!active(v,cloud,owner,id))return imported;
                            if(admissible(result)){
                                synchronized(DocumentPipeline.PROCESS_LOCK){
                                    if(SourceSync.stage(v,pipeline,owner,id,file.getString("name"),result.getString("document_kind"),binary.getString("mime"),bytes)){
                                        for(JSONObject doc:v.list("documents"))if(fingerprint.equals(doc.optString("fingerprint"))&&id.equals(doc.optString("importSource"))){ClinicalDocuments.apply(doc,result);doc.put("importPending",true).put("cloudState","pending");v.put("documents",doc);break;}imported++;
                                    }
                                }
                                reason="Importado";
                            }else{excluded++;reason="No importado: "+(!result.optBoolean("readable")?"no se pudo leer el documento.":!"matches".equals(result.optString("patient_match"))?"el titular no coincide o no se pudo confirmar con tu perfil.":"no se identificó como examen o historia clínica.")+" Revísalo en tu correo o súbelo manualmente.";}
                        }
                        if(duplicate)duplicates++;
                        v.put("mailSeen",new JSONObject().put("id",key).put("source",id).put("owner",owner).put("fingerprint",fingerprint).put("name",file.getString("name")).put("status",reason).put("created",System.currentTimeMillis()));seen.add(key);
                    }
                    if(!consumed)break;
                    source.put("cursor",batch.optString("cursor"));completed=source.optString("cursor").isEmpty();
                    if(completed)source.put("lastComplete",source.getLong("until")).put("until",0);
                    saveProgress(v,id,source);
                }
                source.put("status",(completed?"Revisión completada":"Revisión en curso; continuará automáticamente")+" · "+(imported-before)+" documentos nuevos"+" · "+candidates+" adjuntos candidatos"+(duplicates>0?" · "+duplicates+" ya revisados o guardados":"")+(excluded>0?" · "+excluded+" para revisar en el correo":"")+(completed&&candidates==0?". No se encontraron adjuntos compatibles con palabras como historia, ayudas diagnósticas, resultados o exámenes en el nombre del archivo. Se admiten PDF, JPG y PNG de hasta 10 MB; los enlaces no se importan.":""));saveProgress(v,id,source);
            }catch(Exception e){source.put("status",e instanceof java.io.IOException?e.getMessage():"No se completó la revisión. Se reintentará automáticamente.");saveProgress(v,id,source);}
        }
        return imported;
    }
}
