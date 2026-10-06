package com.alvia.health;
import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.graphics.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.tasks.Tasks;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.io.*;

/** Opt-in real Firebase login against an empty emulator installation. No patient data. */
public class AuthFlowTests extends Instrumentation {
    int checks;void check(boolean c,String label){if(!c)throw new AssertionError(label);checks++;}
    void progress(String s){Bundle b=new Bundle();b.putString("stream",s+"\n");sendStatus(0,b);}
    public void onCreate(Bundle args){super.onCreate(args);start();}
    public void onStart(){Bundle result=new Bundle();FirebaseCloud cloud=null;Vault vault=null;MainActivity main=null;AuthActivity login=null;boolean own=false;
        try{
            vault=new Vault(getTargetContext());cloud=FirebaseCloud.create(getTargetContext());
            progress("Preflight: registered="+cloud.account()+", documents="+vault.list("documents").size()+", binding="+vault.list("cloudBinding").size());
            // Only our generated account may be cleaned after an interrupted prior run.
            if(cloud.account()&&cloud.email().startsWith("bio-auth-")&&cloud.email().endsWith("@example.invalid")){
                try{Tasks.await(FirebaseAuth.getInstance(FirebaseApp.getInstance("bio")).getCurrentUser().delete(),60,TimeUnit.SECONDS);}catch(Exception e){progress("Previous synthetic account already unavailable");}
                cloud.signOut();for(org.json.JSONObject d:vault.list("cloudBinding"))vault.remove("cloudBinding",d.getString("id"));
            }
            check(!cloud.account()&&vault.list("documents").isEmpty(),"Requires test installation without registered account or documents");own=true;
            cloud.ensureSession();String before=cloud.owner();check(cloud.anonymous(),"Existing anonymous session created");
            AuthSession.lock();ActivityMonitor monitor=addMonitor(AuthActivity.class.getName(),null,false);
            main=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            login=(AuthActivity)waitForMonitorWithTimeout(monitor,15000);check(login!=null&&!AuthSession.unlocked(),"Main screen gated by login");
            AuthActivity first=login;List<EditText> fields=new ArrayList<>();runOnMainSync(()->collect(first.getWindow().getDecorView(),EditText.class,fields));
            check(fields.size()==3,"Anonymous upgrade presents account registration");check(fields.get(1).getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod&&fields.get(2).getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod,"Password and confirmation are masked");
            String mail="bio-auth-"+UUID.randomUUID()+"@example.invalid",secret="Bio!"+UUID.randomUUID();
            runOnMainSync(()->{fields.get(0).setText(mail);fields.get(1).setText("short");fields.get(2).setText("short");first.submit();});check(!AuthSession.unlocked(),"Short password cannot unlock");
            capture(first,"login-register.png");
            progress("Linking synthetic account through login UI");
            ActivityMonitor profileMonitor=addMonitor(ProfileActivity.class.getName(),null,false);
            runOnMainSync(()->{fields.get(1).setText(secret);fields.get(2).setText(secret);first.submit();});
            ProfileActivity profile=(ProfileActivity)waitForMonitorWithTimeout(profileMonitor,90000);check(profile!=null&&!AuthSession.unlocked(),"New account requires health profile before entering");
            List<EditText> profileFields=new ArrayList<>();List<android.widget.Button> profileButtons=new ArrayList<>();
            runOnMainSync(()->{collect(profile.getWindow().getDecorView(),EditText.class,profileFields);collect(profile.getWindow().getDecorView(),android.widget.Button.class,profileButtons);});
            check(profileFields.size()==3,"Profile asks name, height and weight plus birth date and blood group");
            capture(profile,"health-profile.png");
            java.lang.reflect.Field birth=ProfileActivity.class.getDeclaredField("birth");birth.setAccessible(true);birth.set(profile,"1995-01-01");
            runOnMainSync(()->{profileFields.get(0).setText("Persona de prueba Bio");profileFields.get(1).setText("170");profileFields.get(2).setText("65");profileButtons.stream().filter(b->b.getText().toString().equals("Guardar y continuar")).findFirst().get().performClick();});
            removeMonitor(profileMonitor);
            awaitUnlock();check(cloud.account(),"Real account created");check(before.equals(cloud.owner()),"Anonymous UID preserved when linking");
            check(HealthProfile.complete(vault),"Health profile persisted locally");
            org.json.JSONObject clinical=HealthProfile.clinical(vault);check(!clinical.has("fullName")&&!clinical.has("birthDate")&&clinical.has("ageYears"),"AI context excludes profile identity");
            com.google.firebase.firestore.FirebaseFirestore db=com.google.firebase.firestore.FirebaseFirestore.getInstance(FirebaseApp.getInstance("bio"));
            com.google.firebase.firestore.DocumentReference profileRef=db.collection("users").document(FirebaseAuth.getInstance(FirebaseApp.getInstance("bio")).getUid()).collection("profile").document("main");
            check(Tasks.await(profileRef.get(com.google.firebase.firestore.Source.SERVER),60,TimeUnit.SECONDS).exists(),"Profile actually stored in owner Firebase path");
            check(mail.equals(cloud.email()),"Account uses entered email");
            // Recreate with a locked session: saved Android activity state must not bypass the gate.
            waitForIdleSync();removeMonitor(monitor);monitor=addMonitor(AuthActivity.class.getName(),null,false);
            AuthSession.restore=false;MainActivity screen=main;runOnMainSync(()->{AuthSession.lock();screen.recreate();});
            login=(AuthActivity)waitForMonitorWithTimeout(monitor,60000);check(login!=null&&!AuthSession.unlocked(),"Recreated activity cannot bypass lock");
            AuthActivity second=login;fields.clear();runOnMainSync(()->collect(second.getWindow().getDecorView(),EditText.class,fields));check(fields.size()==2,"Returning account uses login form");
            runOnMainSync(()->{fields.get(1).setText("incorrect-password");second.submit();});
            java.lang.reflect.Field busy=AuthActivity.class.getDeclaredField("busy");busy.setAccessible(true);long failedUntil=System.currentTimeMillis()+90000;while((boolean)busy.get(second)&&System.currentTimeMillis()<failedUntil)Thread.sleep(250);
            check(!(boolean)busy.get(second)&&!AuthSession.unlocked(),"Incorrect password cannot unlock");
            capture(second,"login-password.png");
            progress("Verifying password reauthentication");runOnMainSync(()->{fields.get(1).setText(secret);second.submit();});awaitUnlock();
            check(before.equals(cloud.owner()),"Reauthentication keeps owner");
            BiometricAccess access=new BiometricAccess(getTargetContext());check(!access.enabled(before),"Biometrics never enabled without enrollment");
            access.clear();check(!access.enabled(before),"Disabling biometrics clears binding");
            AuthSession.unlock();Activity sourceScreen=startActivitySync(new Intent(getTargetContext(),SourcesActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();capture(sourceScreen,"health-sources.png");check(!sourceScreen.isFinishing(),"Sources screen opens behind authenticated gate");
            Thread.sleep(700);check(AuthSession.unlocked(),"Internal navigation must not lock the session");
            check(HealthBridge.available(getTargetContext()),"Health Connect available for real permission-flow test");
            List<Button> buttons=new ArrayList<>();runOnMainSync(()->collect(sourceScreen.getWindow().getDecorView(),Button.class,buttons));
            runOnMainSync(()->buttons.stream().filter(b->b.getText().toString().equals("Conectar / revisar permisos")).findFirst().get().performClick());
            long permissionUntil=System.currentTimeMillis()+15000;boolean external=false;
            while(System.currentTimeMillis()<permissionUntil){android.view.accessibility.AccessibilityNodeInfo node=getUiAutomation().getRootInActiveWindow();if(node!=null&&node.getPackageName()!=null&&(node.getPackageName().toString().contains("healthconnect")||node.getPackageName().toString().contains("permissioncontroller"))){external=true;break;}Thread.sleep(200);}
            check(external,"Actual Android Health Connect screen opened");check(AuthSession.unlocked(),"Session survives external permissions screen");
            getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
            long returnUntil=System.currentTimeMillis()+15000;while(!sourceScreen.hasWindowFocus()&&System.currentTimeMillis()<returnUntil)Thread.sleep(200);
            check(!sourceScreen.isFinishing()&&AuthSession.unlocked(),"Cancel permissions returns to Sources without login");
            awaitText("Entendido",true); // Dismiss the explicit cancellation feedback.
            for(String p:HealthBridge.permissions())try(android.os.ParcelFileDescriptor fd=getUiAutomation().executeShellCommand("pm grant com.alvia.health "+p);InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
            check(HealthBridge.granted(getTargetContext()).containsAll(HealthBridge.permissions()),"Fixture: all Health Connect permissions already granted by Android");
            buttons.clear();runOnMainSync(()->collect(sourceScreen.getWindow().getDecorView(),Button.class,buttons));
            runOnMainSync(()->buttons.stream().filter(b->b.getText().toString().equals("Conectar / revisar permisos")).findFirst().get().performClick());
            awaitText("Sincronizar ahora",true);
            check(SourceSync.setting(vault,"health").optBoolean("enabled"),"Already granted permissions enable the health source");
            awaitText("Sincronización completada:",false);
            check(AuthSession.unlocked()&&!sourceScreen.isFinishing(),"Already authorized sync completes without returning to login");
            capture(sourceScreen,"health-already-authorized.png");
            runOnMainSync(sourceScreen::finish);waitForIdleSync();check(AuthSession.unlocked(),"Returning to main retains authenticated session");
            AuthSession.beginExternalFlow();java.lang.reflect.Field externalUntil=AuthSession.class.getDeclaredField("externalUntil");externalUntil.setAccessible(true);externalUntil.setLong(null,android.os.SystemClock.elapsedRealtime()-1);
            check(!AuthSession.unlocked(),"External return window expires and locks");AuthSession.unlock();AuthSession.background();check(!AuthSession.unlocked(),"Ordinary background locks session");
            removeMonitor(monitor);result.putString("stream","PASS: "+checks+" authentication checks including real Firebase linking and login.\n");
        }catch(Throwable e){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(e));progress(result.getString("stream"));}
        finally{try{if(login!=null){AuthActivity a=login;runOnMainSync(a::finish);}if(main!=null){MainActivity a=main;runOnMainSync(a::finish);}AuthSession.lock();AuthSession.restore=false;
            if(own&&cloud!=null&&cloud.signedIn()){com.google.firebase.firestore.FirebaseFirestore db=com.google.firebase.firestore.FirebaseFirestore.getInstance(FirebaseApp.getInstance("bio"));Tasks.await(db.collection("users").document(FirebaseAuth.getInstance(FirebaseApp.getInstance("bio")).getUid()).collection("profile").document("main").delete(),60,TimeUnit.SECONDS);Tasks.await(FirebaseAuth.getInstance(FirebaseApp.getInstance("bio")).getCurrentUser().delete(),60,TimeUnit.SECONDS);cloud.signOut();}
            if(own&&vault!=null)for(org.json.JSONObject d:vault.list("profile"))vault.remove("profile",d.getString("id"));
            if(own&&vault!=null)for(org.json.JSONObject d:vault.list("cloudBinding"))vault.remove("cloudBinding",d.getString("id"));
        }catch(Exception e){result.putString("stream",result.getString("stream")+"Cleanup failed: "+e.getClass().getSimpleName());}finish(result.getString("stream","").startsWith("PASS")?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);}
    }
    void awaitUnlock()throws Exception{long until=System.currentTimeMillis()+90000;while(!AuthSession.unlocked()&&System.currentTimeMillis()<until)Thread.sleep(250);check(AuthSession.unlocked(),"Password authentication completes");}
    void awaitText(String text,boolean click)throws Exception{long until=System.currentTimeMillis()+30000;while(System.currentTimeMillis()<until){android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root!=null)for(android.view.accessibility.AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByText(text)){if(n.getText()!=null&&n.getText().toString().toLowerCase(java.util.Locale.ROOT).contains(text.toLowerCase(java.util.Locale.ROOT))){if(click)n.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);check(true,"Visible feedback: "+text);return;}}Thread.sleep(200);}throw new AssertionError("Missing visible feedback: "+text);}
    <T extends View>void collect(View v,Class<T> type,List<T> list){if(type.isInstance(v))list.add(type.cast(v));if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)collect(((ViewGroup)v).getChildAt(i),type,list);}
    void capture(Activity a,String name){runOnMainSync(()->{try{View v=a.getWindow().getDecorView();Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));try(FileOutputStream out=new FileOutputStream(new File(a.getExternalFilesDir(null),name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}catch(Exception e){throw new RuntimeException(e);}});}
}
