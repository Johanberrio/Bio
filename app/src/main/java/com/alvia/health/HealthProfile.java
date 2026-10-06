package com.alvia.health;
import org.json.*;
import java.time.*;
import java.util.*;
final class HealthProfile {
    static final String[] BODY_LABELS={"Sin elegir","Mujer","Hombre"};
    static final String[] BODY_VALUES={"","female","male"};
    static boolean female(JSONObject profile){return "female".equals(profile.optString("bodyModel"));}
    static final String[] BLOOD={"No lo sé","O+","O−","A+","A−","B+","B−","AB+","AB−"};
    static JSONObject current(Vault vault){List<JSONObject> values=vault.list("profile");return values.isEmpty()?new JSONObject():values.get(0);}
    static boolean complete(Vault vault){try{JSONObject p=current(vault);validate(p);return true;}catch(Exception e){return false;}}
    static boolean applyWeight(JSONObject profile,JSONObject reading,String owner,long now)throws Exception{
        double weight=reading.optDouble("value");long time=reading.optLong("created");
        if(!owner.equals(profile.optString("owner"))||(!reading.optString("owner").isEmpty()&&!owner.equals(reading.optString("owner")))
            ||reading.optInt("metric",-1)!=7||!Double.isFinite(weight)||weight<0.1||weight>700
            ||time<=0||time<profile.optLong("weightMeasuredAt")||time>now
            ||(time==profile.optLong("weightMeasuredAt")&&(reading.optString("id").isEmpty()||!reading.optString("id").equals(profile.optString("weightRecord"))||Double.compare(weight,profile.optDouble("weightKg"))==0)))return false;
        profile.put("weightKg",weight).put("weightMeasuredAt",time).put("weightOrigin",reading.optString("origin","Registro manual")).put("weightRecord",reading.optString("id"))
            .put("updatedAt",now).put("cloudState","pending");return true;
    }
    static boolean saveWeight(Vault vault,JSONObject item)throws Exception{
        synchronized(SourceSync.LOCK){
            for(JSONObject deleted:vault.list("deletedVitals"))if(item.optString("id").equals(deleted.optString("id")))return false;
            for(JSONObject old:vault.list("weights"))if(item.optString("id").equals(old.optString("id"))&&Double.compare(item.optDouble("value"),old.optDouble("value"))==0)return false;
            vault.put("weights",item);return true;
        }
    }
    static void migrateWeights(Vault vault)throws Exception{
        synchronized(SourceSync.LOCK){
            for(JSONObject old:vault.list("vitals"))if(old.optInt("metric",-1)==7){saveWeight(vault,old);vault.remove("vitals",old.getString("id"));}
        }
    }
    static void syncWeight(Vault vault,String owner)throws Exception{
        synchronized(SourceSync.LOCK){JSONObject profile=current(vault);boolean changed=false;
            for(JSONObject reading:vault.list("weights"))changed=applyWeight(profile,reading,owner,System.currentTimeMillis())||changed;
            if(changed)vault.put("profile",profile);
        }
    }
    static void validate(JSONObject p)throws Exception{
        if(!Arrays.asList(BODY_VALUES).contains(p.optString("bodyModel")))throw new IllegalArgumentException("Selecciona el modelo corporal.");
        String name=p.optString("fullName").trim();if(name.length()<2||name.length()>120)throw new IllegalArgumentException("Escribe tu nombre completo (2 a 120 caracteres).");
        LocalDate born;try{born=LocalDate.parse(p.optString("birthDate"));}catch(Exception e){throw new IllegalArgumentException("Selecciona tu fecha de nacimiento.");}
        if(born.isAfter(LocalDate.now())||born.isBefore(LocalDate.now().minusYears(130)))throw new IllegalArgumentException("Revisa la fecha de nacimiento.");
        double height=p.optDouble("heightCm"),weight=p.optDouble("weightKg");
        if(!Double.isFinite(height)||height<10||height>280)throw new IllegalArgumentException("Revisa la estatura en centímetros.");
        if(!Double.isFinite(weight)||weight<0.1||weight>700)throw new IllegalArgumentException("Revisa el peso en kilogramos.");
        if(!Arrays.asList(BLOOD).contains(p.optString("bloodGroup")))throw new IllegalArgumentException("Selecciona tu grupo sanguíneo o No lo sé.");
    }
    static JSONObject clinical(Vault vault)throws Exception{
        JSONObject p=current(vault);if(!complete(vault))return new JSONObject();
        return new JSONObject().put("ageYears",Period.between(LocalDate.parse(p.getString("birthDate")),LocalDate.now()).getYears()).put("heightCm",p.getDouble("heightCm")).put("weightKg",p.getDouble("weightKg")).put("bloodGroup",p.getString("bloodGroup")).put("updatedAt",p.optLong("updatedAt")).put("asOf",LocalDate.now().toString()).put("weightSource",p.optString("weightOrigin","Registro manual")).put("weightMeasuredAt",p.optLong("weightMeasuredAt")).put("source","Datos del perfil; peso manual o sincronizado según weightSource. No verificados clínicamente");
    }
}
