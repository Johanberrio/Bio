package com.alvia.health;
import android.app.Instrumentation;
import android.app.Activity;
import android.os.Bundle;
import org.json.JSONObject;
public class WeightTests extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private void check(boolean value){if(!value)throw new AssertionError("Weight regression");}
    @Override public void onStart(){Bundle result=new Bundle();try{
        JSONObject p=new JSONObject().put("owner","test").put("weightKg",71).put("weightOrigin","Registro manual").put("weightMeasuredAt",300);
        JSONObject r=new JSONObject().put("owner","test").put("metric",7).put("value",70.8).put("created",200).put("origin","Health Connect").put("id","fit-record");
        check(HealthProfile.applyWeight(p,r,"test",400));check(p.getDouble("weightKg")==70.8);
        check(MainActivity.measurement(p,"weightKg","kg").equals("70,8 kg"));
        check(!HealthProfile.applyWeight(p,r,"test",400));
        r.put("created",100).put("value",72);check(!HealthProfile.applyWeight(p,r,"test",400));
        r.put("created",200).put("value",70.7);check(HealthProfile.applyWeight(p,r,"test",400));
        r.put("owner","other").put("created",350);check(!HealthProfile.applyWeight(p,r,"test",400));
        r.put("owner","test").put("created",500);check(!HealthProfile.applyWeight(p,r,"test",400));
        result.putString("result","8 weight regression checks passed");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("error",e.toString());finish(Activity.RESULT_CANCELED,result);}}
}
