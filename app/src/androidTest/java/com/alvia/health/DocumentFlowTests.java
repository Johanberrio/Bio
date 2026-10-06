package com.alvia.health;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.TextView;
import android.graphics.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.tasks.Tasks;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Opt-in live Android flow: automatic auth, actual AI, actual Storage, synthetic PDFs. */
public class DocumentFlowTests extends Instrumentation {
    int checks=0;
    void check(boolean value,String name){if(!value)throw new AssertionError(name);checks++;}
    public void onCreate(Bundle args){super.onCreate(args);start();}
    void progress(String text){Bundle b=new Bundle();b.putString("stream",text+"\n");sendStatus(0,b);}
    public void onStart(){Bundle result=new Bundle();MainActivity app=null;Vault vault=null;FirebaseCloud cloud=null;boolean ownIdentity=false;List<String> ids=new ArrayList<>();
        try{
            progress("Checking fresh installation");vault=new Vault(getTargetContext());check(vault.list("documents").isEmpty(),"Use a fresh test installation without user documents");
            cloud=FirebaseCloud.create(getTargetContext());check(!cloud.signedIn(),"Use a fresh test installation without a signed-in user");ownIdentity=true;cloud.login(vault,"bio-doc-"+UUID.randomUUID()+"@example.invalid","Bio!"+UUID.randomUUID(),true);AuthSession.unlock();
            progress("Opening activity");app=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));MainActivity ui=app;
            runOnMainSync(()->ui.go(3));waitForIdleSync();
            for(String category:new String[]{"exam","history"}){
                progress("Importing synthetic "+category);
                java.lang.reflect.Field tab=MainActivity.class.getDeclaredField("healthTab");tab.setAccessible(true);
                runOnMainSync(()->{try{tab.setInt(ui,"history".equals(category)?1:2);ui.render();}catch(Exception e){throw new RuntimeException(e);}});
                Intent picked=new Intent().setData(Uri.parse("content://com.alvia.health.test.fixtures/"+category));
                IntentFilter filter=new IntentFilter(Intent.ACTION_OPEN_DOCUMENT);filter.addCategory(Intent.CATEGORY_OPENABLE);filter.addDataType("*/*");
                ActivityMonitor picker=addMonitor(filter,new ActivityResult(Activity.RESULT_OK,picked),true);
                runOnMainSync(()->ui.importDocument(category));waitForIdleSync();
                check(picker.getHits()==1,"Upload opens file picker directly without popup");removeMonitor(picker);
                progress("Picker returned; waiting for analysis and upload");
                long until=System.currentTimeMillis()+360000;JSONObject document=null;
                while(System.currentTimeMillis()<until){
                    for(JSONObject d:vault.list("documents"))if(category.equals(ClinicalDocuments.category(d))){document=d;if(!ids.contains(d.optString("id")))ids.add(d.optString("id"));}
                    if(document!=null&&!DocumentPipeline.pending(document)&&"synced".equals(document.optString("cloudState")))break;
                    if(document!=null&&document.has("importError"))throw new AssertionError(document.optString("importError"));
                    Thread.sleep(500);
                }
                check(document!=null&&!DocumentPipeline.pending(document),"Import completes automatically");
                check(!document.optString("name").equals("scan_0001.pdf"),"Original scanner name is never the final name");
                String name=document.optString("name").toLowerCase(Locale.ROOT);
                check("exam".equals(category)?name.contains("hemograma"):name.contains("neurolog"),"Name comes from document content");
                check(document.optString("clinicalDate").equals("exam".equals(category)?"2026-09-01":"2026-08-25"),"Clinical date extracted");
                check(document.optJSONObject("analysis")!=null,"Analysis is present on completed import");
                check("synced".equals(document.optString("cloudState")),"Cloud upload completes with inferred name");
                runOnMainSync(()->ui.render());waitForIdleSync();
                List<TextView> labels=new ArrayList<>();runOnMainSync(()->collect(ui.getWindow().getDecorView(),labels));
                check(labels.stream().noneMatch(v->v.getText().toString().contains("Cuenta y sincronización")||v.getText().toString().equals("Analizar documento")),"No manual account or analysis buttons");
                final String capture=category;runOnMainSync(()->{try{View v=ui.getWindow().getDecorView();Bitmap bitmap=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(bitmap));try(FileOutputStream out=new FileOutputStream(new File(ui.getExternalFilesDir(null),"automatic-"+capture+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}catch(Exception e){throw new RuntimeException(e);}});
            }
            check(FirebaseAuth.getInstance(FirebaseApp.getInstance("bio")).getCurrentUser().isAnonymous()==false,"Authenticated account retains document access");
            result.putString("stream","PASS: "+checks+" live Android document checks; two synthetic PDFs.\n");
        }catch(Throwable e){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(e));progress(result.getString("stream"));}
        finally{
            progress("Cleaning synthetic test data");try{if(app!=null){MainActivity ui=app;runOnMainSync(ui::finish);}
                if(vault!=null&&cloud!=null){for(JSONObject d:vault.list("documents"))if(ids.contains(d.optString("id"))){if(!d.optString("cloudOwner").isEmpty())cloud.delete(vault,d);vault.remove("documents",d.getString("id"));vault.deleteDocument(d.getString("id"));}}
                if(ownIdentity&&cloud!=null&&cloud.signedIn()){Tasks.await(FirebaseAuth.getInstance(FirebaseApp.getInstance("bio")).getCurrentUser().delete(),60,TimeUnit.SECONDS);for(JSONObject b:vault.list("cloudBinding"))vault.remove("cloudBinding",b.getString("id"));}
            }catch(Exception e){result.putString("stream",result.getString("stream")+"Cleanup failed: "+e.getClass().getSimpleName());}
            finish(result.getString("stream","").startsWith("PASS")?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
        }
    }
    void collect(View view,List<TextView> labels){if(view instanceof TextView)labels.add((TextView)view);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)collect(((ViewGroup)view).getChildAt(i),labels);}
}
