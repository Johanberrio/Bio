package com.alvia.health;
import org.json.*;
import java.util.*;
final class ProfileSourceChecks {
    static void run(Vault v,SmokeTests t)throws Exception{
        JSONObject profile=new JSONObject().put("id","main").put("fullName","Persona de prueba").put("birthDate","1995-01-01").put("heightCm",170).put("weightKg",65).put("bloodGroup","No lo sé");
        HealthProfile.validate(profile);t.check(true,"valid health profile including unknown blood group");
        t.check(!HealthProfile.female(profile),"Legacy profile retains previous body model");
        JSONObject female=new JSONObject(profile.toString()).put("bodyModel","female");HealthProfile.validate(female);
        t.check(HealthProfile.female(new JSONObject(female.toString())),"Female body selection survives profile serialization");
        t.check(!HealthProfile.clinical(v).has("bodyModel"),"Visual model is not inferred clinical data");
        for(int system=0;system<BodyView.SYSTEMS.length;system++){
            if(system==6)t.check(BodyView.resourceFor(system,true)==0,"Missing female skeleton never falls back to male image");
            else t.check(BodyView.resourceFor(system,true)!=0&&BodyView.resourceFor(system,true)!=BodyView.resourceFor(system,false),"Female system uses separate resource: "+system);
        }
        boolean invalidModel=false;try{HealthProfile.validate(new JSONObject(profile.toString()).put("bodyModel","invalid"));}catch(Exception e){invalidModel=true;}
        t.check(invalidModel,"Invalid model rejected");
        for(String key:new String[]{"heightCm","weightKg","birthDate","fullName","bloodGroup"}){JSONObject bad=new JSONObject(profile.toString());bad.put(key,key.equals("birthDate")?"2999-01-01":key.equals("fullName")?"":key.equals("bloodGroup")?"X":-1);boolean failed=false;try{HealthProfile.validate(bad);}catch(Exception e){failed=true;}t.check(failed,"invalid profile rejected: "+key);}
        JSONObject weighted=new JSONObject(profile.toString()).put("owner","weight-owner");
        JSONObject reading=new JSONObject().put("id","weight-1").put("owner","weight-owner").put("metric",7).put("value",68.5).put("created",200).put("origin","Health Connect");
        t.check(HealthProfile.applyWeight(weighted,reading,"weight-owner",300)&&weighted.getDouble("weightKg")==68.5,"Weight updates profile from device");
        t.check(!HealthProfile.applyWeight(weighted,reading,"weight-owner",300),"Repeated weight is idempotent");
        reading.put("value",69);
        t.check(HealthProfile.applyWeight(weighted,reading,"weight-owner",300),"Corrected source measurement updates weight");
        reading.put("created",100);
        t.check(!HealthProfile.applyWeight(weighted,reading,"weight-owner",300),"Older weight does not replace current profile");
        reading.put("created",400);
        t.check(!HealthProfile.applyWeight(weighted,reading,"weight-owner",300),"Future weight excluded");
        reading.put("created",250).put("owner","another-owner");
        t.check(!HealthProfile.applyWeight(weighted,reading,"weight-owner",300),"Weight belongs to current account");
        reading.put("owner","weight-owner").put("value",0);
        t.check(!HealthProfile.applyWeight(weighted,reading,"weight-owner",300),"Invalid weight excluded");
        weighted.put("weightMeasuredAt",290).put("weightOrigin","Registro manual");reading.put("value",70);
        t.check(HealthProfile.applyWeight(weighted,reading,"weight-owner",300),"Automatic weight replaces manual profile fallback");
        String weightId="weight-migration-"+UUID.randomUUID();
        JSONObject legacyWeight=new JSONObject().put("id",weightId).put("metric",7).put("value",65).put("created",100).put("origin","Health Connect");
        try{
            v.put("vitals",legacyWeight);HealthProfile.migrateWeights(v);
            t.check(v.list("vitals").stream().noneMatch(item->weightId.equals(item.optString("id"))),"Migrated weight is not a vital sign");
            t.check(v.list("weights").stream().anyMatch(item->weightId.equals(item.optString("id"))),"Existing weight retained separately");
            t.check(!HealthProfile.saveWeight(v,legacyWeight),"Repeated weight does not duplicate storage");
            legacyWeight.put("value",66);t.check(HealthProfile.saveWeight(v,legacyWeight),"Corrected weight updates separate storage");
        }finally{v.remove("weights",weightId);v.remove("vitals",weightId);}
        String id="hc-test-"+UUID.randomUUID();JSONObject vital=new JSONObject().put("id",id).put("metric",0).put("value",70).put("created",12345).put("origin","Health Connect");
        try{t.check(SourceSync.saveVitals(v,Arrays.asList(vital))==1,"import device measurement");t.check(SourceSync.saveVitals(v,Arrays.asList(vital))==0,"device measurement deduplicated");
            vital.put("value",71);t.check(SourceSync.saveVitals(v,Arrays.asList(vital))==1,"device correction updates original");
            v.put("deletedVitals",new JSONObject().put("id",id));v.remove("vitals",id);t.check(!SourceSync.saveVital(v,vital),"deleted imported measurement not resurrected");
        }finally{v.remove("vitals",id);v.remove("deletedVitals",id);}
        byte[] bytes=("%PDF-synthetic-"+UUID.randomUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8);String owner="test-owner";String ledger=SourceSync.hash((owner+":"+SourceSync.hash(bytes)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        DocumentPipeline pipeline=new DocumentPipeline(v,(d,b)->{throw new AssertionError("Staging must not analyze until durable");},d->{});
        try{t.check(SourceSync.stage(v,pipeline,owner,"folder-test","random.pdf","history","application/pdf",bytes),"source document durably staged");
            t.check(!SourceSync.stage(v,pipeline,owner,"folder-other","duplicate.pdf","exam","application/pdf",bytes),"same content from another source is not duplicated");
            JSONObject doc=v.list("documents").stream().filter(d->SourceSyncTestMatch(d,bytes)).findFirst().get();t.check(DocumentPipeline.pending(doc)&&"history".equals(doc.optString("category")),"source documents wait for content analysis");
            v.remove("documents",doc.getString("id"));v.deleteDocument(doc.getString("id"));
            t.check(!SourceSync.stage(v,pipeline,owner,"folder-test","random.pdf","history","application/pdf",bytes),"deleted source document not resurrected");
        }finally{for(JSONObject d:v.list("documents"))if(SourceSyncTestMatch(d,bytes)){v.remove("documents",d.getString("id"));v.deleteDocument(d.getString("id"));}v.remove("importLedger",ledger);}
    }
    static boolean SourceSyncTestMatch(JSONObject d,byte[] bytes){try{return SourceSync.hash(bytes).equals(d.optString("fingerprint"));}catch(Exception e){return false;}}
}
