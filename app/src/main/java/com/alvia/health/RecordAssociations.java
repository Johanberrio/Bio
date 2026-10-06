package com.alvia.health;

import org.json.*;
import java.util.*;

/** Recomputes older symptom records without trusting their manually selected system. */
final class RecordAssociations {
    private static final Map<String,SymptomAssociator.Result> CACHE=new LinkedHashMap<String,SymptomAssociator.Result>(64,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,SymptomAssociator.Result> entry){return size()>256;}
    };
    static synchronized SymptomAssociator.Result result(JSONObject record) {
        String title=record.optString("title"),notes=record.optString("notes"),sensation=record.optString("sensation","Según descripción");
        String key=new JSONArray(Arrays.asList(title,notes,sensation)).toString();
        SymptomAssociator.Result result=CACHE.get(key);
        if(result==null){result=SymptomAssociator.associate(title,notes,sensation);CACHE.put(key,result);}
        return result;
    }
    static Set<Integer> systems(JSONObject record) {
        Set<Integer> inferred=result(record).systems();
        if(inferred.isEmpty() && !"Síntoma".equals(record.optString("kind")) && !record.has("associationVersion")) {
            int legacy=record.optInt("system",-1);
            if(legacy>=0&&legacy<8)inferred.add(legacy);
        }
        return inferred;
    }
    static String labels(JSONObject record) {
        List<String> labels=new ArrayList<>();
        for(int s:systems(record))labels.add(SymptomAssociator.SYSTEMS[s]);
        return labels.isEmpty()?"Necesita más detalles":String.join(" · ",labels);
    }
    static boolean active(JSONObject record) {
        return "Síntoma".equals(record.optString("kind"))&&record.optBoolean("active",true);
    }
    static void attach(JSONObject record) throws JSONException {
        SymptomAssociator.Result result=result(record);
        record.remove("system");
        record.put("systems",new JSONArray(result.systems()));
        record.put("associationVersion",SymptomAssociator.VERSION);
        record.put("associationMethod","local_text_rules");
        record.put("associationNeedsDetails",result.incomplete);
    }
}
