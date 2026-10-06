package com.alvia.health;

import org.json.JSONObject;
import java.util.*;

/** Notices are temporary; mailSeen remains durable to prevent repeated analysis. */
final class MailReviews {
    static final long MAX_AGE=7L*24*60*60*1000;
    static boolean visible(JSONObject entry,Set<String> sources,Set<String> imported,long now){
        return sources.contains(entry.optString("source"))
            &&entry.optString("status").startsWith("No importado")
            &&!entry.optBoolean("dismissed")&&entry.optLong("created")>now-MAX_AGE
            &&(entry.optString("fingerprint").isEmpty()||!imported.contains(entry.optString("fingerprint")));
    }
    static List<JSONObject> pending(Vault vault,String owner)throws Exception{
        Set<String> sources=new HashSet<>(),imported=new HashSet<>();
        for(JSONObject source:vault.list("sources"))if(owner.equals(source.optString("owner"))&&!source.optBoolean("remoteDisconnected"))sources.add(source.optString("id"));
        for(JSONObject doc:vault.list("documents"))if(!DocumentPipeline.pending(doc)&&!doc.optString("fingerprint").isEmpty())imported.add(doc.optString("fingerprint"));
        List<JSONObject> result=new ArrayList<>();long now=System.currentTimeMillis();
        for(JSONObject entry:vault.list("mailSeen"))if((entry.optString("owner").isEmpty()||owner.equals(entry.optString("owner")))&&visible(entry,sources,imported,now))result.add(entry);
        result.sort(Comparator.comparingLong((JSONObject e)->e.optLong("created")).reversed());return result;
    }
    static void dismiss(Vault vault,List<JSONObject> entries)throws Exception{
        Set<String> ids=new HashSet<>();for(JSONObject entry:entries)ids.add(entry.optString("id"));
        synchronized(SourceSync.LOCK){
            List<JSONObject> updates=new ArrayList<>();
            for(JSONObject current:vault.list("mailSeen"))if(ids.contains(current.optString("id")))updates.add(current.put("dismissed",true));
            vault.putAll("mailSeen",updates);
        }
    }
}
