package com.alvia.health;
import org.json.*;
import java.time.*;
import java.util.*;
final class ClinicalDocuments {
    static String category(JSONObject doc){return doc.optString("category","exam");}
    static LocalDate clinicalDate(JSONObject doc){try{String value=doc.optString("clinicalDate","");return value.isEmpty()?null:LocalDate.parse(value);}catch(Exception e){return null;}}
    static void sort(List<JSONObject> docs){docs.sort((a,b)->{LocalDate x=clinicalDate(a),y=clinicalDate(b);if(x==null&&y!=null)return 1;if(x!=null&&y==null)return -1;if(x!=null){int c=y.compareTo(x);if(c!=0)return c;}return Long.compare(b.optLong("created"),a.optLong("created"));});}
    static void apply(JSONObject doc,JSONObject analysis) throws Exception {
        if(!doc.has("originalName"))doc.put("originalName",doc.optString("name","Documento"));
        boolean readable=analysis.getBoolean("readable");String name=analysis.isNull("name")?"":analysis.optString("name","");
        String day=analysis.isNull("clinical_date")?"":analysis.optString("clinical_date","");
        if(!day.isEmpty()){LocalDate parsed=LocalDate.parse(day);if(parsed.isAfter(LocalDate.now())||analysis.optString("date_evidence").trim().isEmpty())throw new IllegalArgumentException("Fecha sin evidencia");}
        if(readable&&!name.isEmpty()&&name.length()<=160){String ext=doc.optString("mime").equals("application/pdf")?".pdf":doc.optString("mime").equals("image/png")?".png":".jpg";doc.put("name",name.replaceAll("[\\/:*?\"<>|]"," ").trim()+(day.isEmpty()?"":" · "+day)+ext);}
        doc.put("clinicalDate",day).put("analysis",analysis).put("analysisState",readable?"completed":"review").put("analyzedAt",System.currentTimeMillis());
    }
}
