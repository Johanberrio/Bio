package com.alvia.health;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Durable import: extract first, publish only with an identified name. */
final class DocumentPipeline {
    interface Analyzer {JSONObject analyze(JSONObject doc,byte[] bytes)throws Exception;}
    interface Uploader {void upload(JSONObject doc)throws Exception;}
    static final Object PROCESS_LOCK=new Object();
    final Vault vault;
    final Analyzer analyzer;
    final Uploader uploader;
    DocumentPipeline(Vault vault,Analyzer analyzer,Uploader uploader){this.vault=vault;this.analyzer=analyzer;this.uploader=uploader;}
    static boolean pending(JSONObject doc){return doc.optBoolean("importPending")||(!"completed".equals(doc.optString("analysisState"))&&!doc.optBoolean("metadataReviewed"));}
    JSONObject stage(String name,String category,String mime,byte[] bytes)throws Exception{
        if(!Arrays.asList("application/pdf","image/png","image/jpeg").contains(mime)||bytes.length==0||bytes.length>10*1024*1024)throw new IOException("Usa un PDF, JPG o PNG de hasta 10 MB.");
        JSONObject doc=Vault.item().put("name",name).put("originalName",name).put("category",category).put("mime",mime).put("size",bytes.length).put("fingerprint",SourceSync.hash(bytes)).put("analysisState","pending").put("importPending",true);
        String id=doc.getString("id");vault.storeDocument(id,bytes);
        try{vault.put("documents",doc);}catch(Exception e){vault.deleteDocument(id);throw e;}
        return doc;
    }
    JSONObject process(JSONObject original)throws Exception{
        synchronized(PROCESS_LOCK){
            for(JSONObject latest:vault.list("documents"))if(latest.optString("id").equals(original.optString("id"))){original=latest;break;}
            if(!pending(original)&&"synced".equals(original.optString("cloudState")))return original;
            JSONObject doc=new JSONObject(original.toString());
            try{
                if(!"completed".equals(doc.optString("analysisState"))){
                    JSONObject result=analyzer.analyze(doc,vault.readDocument(doc.getString("id")));
                    if(Thread.currentThread().isInterrupted())throw new InterruptedException();
                    if(!result.optBoolean("readable")||result.isNull("name")||result.optString("name").trim().isEmpty())throw new ImportException("No se pudo identificar el contenido. Usa un archivo legible y completo.",false,0);
                    ClinicalDocuments.apply(doc,result);
                    doc.put("importPending",true).put("cloudState","pending");vault.put("documents",doc);
                }
                // Stage is retained if upload fails; the next attempt reuses the analysis.
                JSONObject ready=new JSONObject(doc.toString()).put("importPending",false).put("cloudState","pending");
                ready.remove("importError");ready.remove("retryAt");ready.remove("attempts");
                uploader.upload(ready);
                if(Thread.currentThread().isInterrupted())throw new InterruptedException();
                for(JSONObject stored:vault.list("documents"))if(stored.optString("id").equals(ready.optString("id"))&&!stored.optString("cloudOwner").isEmpty())ready.put("cloudOwner",stored.optString("cloudOwner"));
                ready.put("cloudState","synced").remove("importError");ready.remove("retryAt");ready.remove("attempts");
                vault.put("documents",ready);return ready;
            }catch(InterruptedException e){Thread.currentThread().interrupt();throw e;}
            catch(Exception e){
                // Upload may have persisted its owner after the binary reached Storage.
                for(JSONObject stored:vault.list("documents"))if(stored.optString("id").equals(doc.optString("id"))&&!stored.optString("cloudOwner").isEmpty())doc.put("cloudOwner",stored.optString("cloudOwner"));
                int attempts=doc.optInt("attempts")+1;long retry=System.currentTimeMillis()+Math.min(900000L,60000L*(1L<<Math.min(attempts-1,4)));
                if(e instanceof ImportException){ImportException failure=(ImportException)e;retry=failure.retryable?(failure.retryAt>0?failure.retryAt:retry):Long.MAX_VALUE;}
                doc.put("importPending",true).put("importError",message(e)).put("retryAt",retry).put("attempts",attempts);
                vault.put("documents",doc);throw e;
            }
        }
    }
    static String message(Exception e){
        if(e instanceof ImportException)return e.getMessage();
        if(e instanceof SocketTimeoutException)return "El análisis tardó demasiado. Se reintentará automáticamente.";
        if(e instanceof UnknownHostException||e instanceof ConnectException)return "Sin conexión. La carga se reanudará automáticamente.";
        if(e instanceof com.google.firebase.auth.FirebaseAuthException)return "No se pudo preparar el acceso automático. Se reintentará sin perder el archivo.";
        return "No se completó la carga. Se reintentará automáticamente sin perder el archivo.";
    }
    static class ImportException extends IOException {final boolean retryable;final long retryAt;ImportException(String text,boolean retryable,long retryAt){super(text);this.retryable=retryable;this.retryAt=retryAt;}}
    static JSONObject request(String endpoint,String token,JSONObject doc,byte[] bytes)throws Exception{return request(endpoint,token,doc,bytes,new JSONObject());}
    static JSONObject request(String endpoint,String token,JSONObject doc,byte[] bytes,JSONObject profile)throws Exception{
        HttpURLConnection http=(HttpURLConnection)new URL(endpoint+"/analyze-document").openConnection();
        try{
            http.setConnectTimeout(20000);http.setReadTimeout(290000);http.setInstanceFollowRedirects(false);http.setRequestMethod("POST");http.setRequestProperty("Content-Type","application/json");http.setRequestProperty("Authorization","Bearer "+token);http.setDoOutput(true);
            JSONObject payload=new JSONObject().put("profile",profile).put("document",new JSONObject().put("mime",doc.getString("mime")).put("category",ClinicalDocuments.category(doc)).put("data",android.util.Base64.encodeToString(bytes,android.util.Base64.NO_WRAP)));
            byte[] encoded=payload.toString().getBytes(StandardCharsets.UTF_8);http.setFixedLengthStreamingMode(encoded.length);try(OutputStream out=http.getOutputStream()){out.write(encoded);}
            int status=http.getResponseCode();
            if(status!=200){
                if(status==429){long tomorrow=java.time.LocalDate.now(java.time.ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC).toEpochMilli();throw new ImportException("Se alcanzó el límite de análisis. La carga se reanudará mañana; tu archivo está conservado.",true,tomorrow);}
                if(status==401||status==403)throw new ImportException("No se pudo validar el acceso automático. Se reintentará la conexión.",true,0);
                if(status==400||status==413||status==415)throw new ImportException("El archivo no se pudo leer. Usa un PDF, JPG o PNG válido de hasta 10 MB.",false,0);
                throw new ImportException("El servicio de análisis no respondió correctamente ("+status+"). Se reintentará automáticamente.",true,0);
            }
            try(InputStream in=http.getInputStream()){return new JSONObject(new String(MainActivity.readLimited(in,2*1024*1024),StandardCharsets.UTF_8));}
        }finally{http.disconnect();}
    }
}
