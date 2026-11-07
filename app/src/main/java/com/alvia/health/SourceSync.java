package com.alvia.health;

import android.content.*;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Explicitly opted-in sources, encrypted configuration, owner-bound and idempotent. */
final class SourceSync {
    static final Object LOCK=new Object();
    static final Object RUN_LOCK=new Object();
    static volatile boolean running;
    static JSONObject setting(Vault v,String id){for(JSONObject s:v.list("sources"))if(id.equals(s.optString("id")))return s;return new JSONObject();}
    static String hash(byte[] bytes)throws Exception{byte[] result=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder out=new StringBuilder();for(byte b:result)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
    static boolean saveVital(Vault v,JSONObject item)throws Exception{
        synchronized(LOCK){for(JSONObject deleted:v.list("deletedVitals"))if(item.getString("id").equals(deleted.optString("id")))return false;
        for(JSONObject old:v.list("vitals"))if(item.getString("id").equals(old.optString("id"))&&old.optDouble("value")==item.optDouble("value"))return false;
        v.put("vitals",item);return true;}
    }
    static int saveVitals(Vault v,List<JSONObject> items)throws Exception{
        synchronized(LOCK){Set<String> removed=new HashSet<>();for(JSONObject d:v.list("deletedVitals"))removed.add(d.optString("id"));
        Map<String,JSONObject> existing=new HashMap<>();for(JSONObject o:v.list("vitals"))existing.put(o.optString("id"),o);
        List<JSONObject> updates=new ArrayList<>();for(JSONObject item:items){String id=item.getString("id");JSONObject old=existing.get(id);if(!removed.contains(id)&&(old==null||old.optDouble("value")!=item.optDouble("value")))updates.add(item);}
        if(!updates.isEmpty())v.putAll("vitals",updates);return updates.size();}
    }
    static void schedule(Context c){WorkManager.getInstance(c).enqueueUniquePeriodicWork("bio-sources",ExistingPeriodicWorkPolicy.KEEP,new PeriodicWorkRequest.Builder(SourceWorker.class,15,TimeUnit.MINUTES).setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build());}
    static DocumentPipeline pipeline(Context c,Vault v,FirebaseCloud cloud)throws Exception{
        JSONObject settings;try(InputStream in=c.getAssets().open("backend-config.json")){settings=new JSONObject(new String(MainActivity.readLimited(in,16384),StandardCharsets.UTF_8));}
        String endpoint=settings.getString("url");return new DocumentPipeline(v,(d,b)->{if(!cloud.account())throw new IOException("Inicia sesión");return DocumentPipeline.request(endpoint,cloud.idToken(),d,b,HealthProfile.clinical(v));},d->{if(!cloud.account())throw new IOException("Inicia sesión");cloud.upload(v,d);});
    }
    static boolean stage(Vault v,DocumentPipeline pipeline,String owner,String source,String name,String category,String mime,byte[] bytes)throws Exception{
        synchronized(DocumentPipeline.PROCESS_LOCK){String fingerprint=hash(bytes);String ledger=hash((owner+":"+fingerprint).getBytes(StandardCharsets.UTF_8));
        for(JSONObject seen:v.list("importLedger"))if(ledger.equals(seen.optString("id")))return false;
        for(JSONObject doc:v.list("documents"))if(fingerprint.equals(doc.optString("fingerprint"))){v.put("importLedger",new JSONObject().put("id",ledger));return false;}
        JSONObject doc=pipeline.stage(name,category,mime,bytes).put("fingerprint",fingerprint).put("importSource",source).put("importOwner",owner);v.put("documents",doc);
        v.put("importLedger",new JSONObject().put("id",ledger).put("document",doc.getString("id")));return true;}
    }
    static String run(Context c,boolean background)throws Exception{return run(c,background,()->{});}
    static String run(Context c,boolean background,Runnable healthUpdated)throws Exception{
        synchronized(RUN_LOCK){running=true;try{Vault v=new Vault(c);FirebaseCloud cloud=FirebaseCloud.create(c);if(!cloud.account())return "Inicia sesión para sincronizar.";
        String owner=cloud.owner();DocumentPipeline pipeline=pipeline(c,v,cloud);int imported=0;String health="";
        try{int n=HealthBridge.sync(c,background);if(n>0)health=" · "+n+" mediciones actualizadas";}catch(Exception e){health=" · Revisa los permisos de Health Connect";}
        healthUpdated.run();
        imported+=MailSync.stage(c,v,cloud,pipeline);
        for(JSONObject source:v.list("sources")){
            if(!cloud.account()||!owner.equals(cloud.owner()))break;
            if(!source.optBoolean("enabled")||!owner.equals(source.optString("owner"))||!source.optString("id").startsWith("folder-"))continue;
            try{DocumentFile root=DocumentFile.fromTreeUri(c,Uri.parse(source.getString("uri")));if(root==null||!root.canRead())throw new SecurityException();
                int staged=0;for(DocumentFile f:root.listFiles()){
                    if(staged>=10)break; // Bound work per run; remaining files are picked up next time.
                    if(!cloud.account()||!SourceSync.setting(v,source.getString("id")).optBoolean("enabled"))break;
                    String mime=f.getType();if(!f.isFile()||!Arrays.asList("application/pdf","image/jpeg","image/png").contains(mime)||f.length()>10*1024*1024)continue;
                    try{byte[] bytes;try(InputStream in=c.getContentResolver().openInputStream(f.getUri())){if(in==null)continue;bytes=MainActivity.readLimited(in,10*1024*1024);}
                    if(stage(v,pipeline,owner,source.getString("id"),f.getName(),source.getString("category"),mime,bytes)){imported++;staged++;}}
                    catch(IOException unreadable){/* Other files must still be considered; this file is retried next run. */}
                }
                source.put("status","Revisada: "+new java.text.SimpleDateFormat("dd MMM HH:mm",new Locale("es")).format(new Date()));
            }catch(SecurityException e){source.put("status","Permiso perdido. Vuelve a elegir la carpeta.");}catch(Exception e){source.put("status","No se pudo leer la carpeta. Se reintentará.");}
            // Do not overwrite a disconnect made while the provider was being read.
            JSONObject current=setting(v,source.getString("id"));if(current.optBoolean("enabled")){current.put("status",source.optString("status"));v.put("sources",current);}
        }
        long deadline=System.currentTimeMillis()+7*60*1000;for(JSONObject doc:v.list("documents")){
            if(System.currentTimeMillis()>deadline||!cloud.account()||!owner.equals(cloud.owner()))break;
            if(!doc.has("importSource")||!DocumentPipeline.pending(doc)||doc.optLong("retryAt")>System.currentTimeMillis())continue;
            try{pipeline.process(doc);}catch(Exception ignored){} // Durable error is shown on the document card.
        }
        boolean mailFailed=false;for(JSONObject source:v.list("sources"))if(source.optString("id").startsWith(MailSync.PREFIX)&&owner.equals(source.optString("owner"))&&source.optBoolean("enabled")&&source.optBoolean("syncError")){mailFailed=true;break;}
        return (mailFailed?"Sincronización incompleta. Revisa el aviso de tu cuenta de correo.\n":"")+imported+" documentos nuevos"+health;}finally{running=false;}}
    }
}

