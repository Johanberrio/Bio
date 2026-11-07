package com.alvia.health;
import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.graphics.*;
import java.io.*;
import java.util.*;

/** Opt-in disposable emulator account. Successful final submission clears this app's data. */
public class DeletionTests extends Instrumentation {
    void report(String s){Bundle b=new Bundle();b.putString("stream",s+"\n");sendStatus(0,b);}
    void check(boolean ok,String s){if(!ok)throw new AssertionError(s);report("PASS "+s);}
    public void onCreate(Bundle args){super.onCreate(args);start();}
    public void onStart(){Bundle result=new Bundle();
        try{
            Context c=getTargetContext();FirebaseCloud cloud=FirebaseCloud.create(c);
            if(cloud.account()&&cloud.email().startsWith("bio-erasure-device-")&&cloud.email().endsWith("@example.invalid")){
                try{com.google.android.gms.tasks.Tasks.await(com.google.firebase.auth.FirebaseAuth.getInstance(com.google.firebase.FirebaseApp.getInstance("bio")).getCurrentUser().reload(),60,java.util.concurrent.TimeUnit.SECONDS);throw new IllegalStateException("Clean previous synthetic account using cleanup-deletion-fixture first");}
                catch(java.util.concurrent.ExecutionException e){if(!(e.getCause() instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException))throw e;}
                cloud.signOut();
            }
            check(!cloud.account(),"requires isolated emulator without a registered account");
            String email="bio-erasure-device-"+UUID.randomUUID()+"@example.invalid",secret="Bio!"+UUID.randomUUID();
            cloud.signIn(email,secret,true);String uid=cloud.owner().split(":",2)[1];report("SYNTHETIC_UID "+uid);
            try(FileOutputStream out=new FileOutputStream(new File(c.getFilesDir(),"deletion-fixture.txt"))){out.write("disposable fixture".getBytes());}
            AccountDeletionActivity activity=null;
            for(boolean dark:new boolean[]{false,true}){
                c.getSharedPreferences("MainActivity",Context.MODE_PRIVATE).edit().putBoolean("darkMode",dark).commit();AuthSession.unlock();
                activity=(AccountDeletionActivity)startActivitySync(new Intent(c,AccountDeletionActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
                AccountDeletionActivity screen=activity;
                java.util.List<EditText> fields=new ArrayList<>();java.util.List<CheckBox> boxes=new ArrayList<>();
                runOnMainSync(()->{collect(screen.getWindow().getDecorView(),EditText.class,fields);collect(screen.getWindow().getDecorView(),CheckBox.class,boxes);});
                check(fields.size()==1&&boxes.size()==1,"password and explicit confirmation in "+(dark?"dark":"light")+" mode");
                check(fields.get(0).getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod,"password masked");
                runOnMainSync(screen::request);waitForIdleSync();
                java.util.List<TextView> text=new ArrayList<>();runOnMainSync(()->collect(screen.getWindow().getDecorView(),TextView.class,text));
                check(text.stream().anyMatch(v->v.getText().toString().equals("Introduce tu contraseña y confirma la eliminación.")),"empty request rejected locally");
                runOnMainSync(()->{View view=screen.getWindow().getDecorView();Bitmap bitmap=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);bitmap.setHasAlpha(false);view.draw(new Canvas(bitmap));try(FileOutputStream out=new FileOutputStream(new File(c.getExternalFilesDir(null),"deletion-"+(dark?"dark":"light")+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}catch(Exception e){throw new RuntimeException(e);}bitmap.recycle();});
                if(!dark){runOnMainSync(screen::finish);waitForIdleSync();}
            }
            AccountDeletionActivity screen=activity;
            List<EditText> fields=new ArrayList<>();List<CheckBox> boxes=new ArrayList<>();
            runOnMainSync(()->{collect(screen.getWindow().getDecorView(),EditText.class,fields);collect(screen.getWindow().getDecorView(),CheckBox.class,boxes);fields.get(0).setText("wrong-password");boxes.get(0).setChecked(true);screen.request();});
            java.lang.reflect.Field busy=AccountDeletionActivity.class.getDeclaredField("busy");busy.setAccessible(true);
            long until=System.currentTimeMillis()+100000;while((boolean)busy.get(screen)&&System.currentTimeMillis()<until)Thread.sleep(200);
            check(!(boolean)busy.get(screen)&&cloud.account(),"wrong password cannot request deletion");
            check(fields.get(0).length()==0,"password cleared after verification");
            report("READY_TO_DELETE "+uid);
            // Allow host to copy only synthetic UI screenshots before local wipe removes them.
            Thread.sleep(15000);
            AuthSession.unlock();runOnMainSync(()->{fields.get(0).setText(secret);screen.request();});
            report("SUBMITTED_DEVICE_ERASURE");
            Thread.sleep(100000);
            throw new AssertionError("Expected process termination from clearApplicationUserData");
        }catch(Throwable e){result.putString("stream","FAIL "+e.getClass().getSimpleName()+": "+e.getMessage());finish(Activity.RESULT_CANCELED,result);}
    }
    static <T>void collect(View v,Class<T> type,List<T> list){if(type.isInstance(v))list.add(type.cast(v));if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)collect(((ViewGroup)v).getChildAt(i),type,list);}
}
