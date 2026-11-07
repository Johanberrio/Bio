package com.alvia.health;

import android.app.*;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

/** Exercises the actual account dialog without signing in or deleting data. */
public class AccountMenuTests extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle(); MainActivity activity=null; AlertDialog[] menu={null};
        try {
            AuthSession.unlock();
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            MainActivity app=activity;
            runOnMainSync(()->menu[0]=app.settings()); waitForIdleSync();
            runOnMainSync(()->{
                ListView list=menu[0].getListView();
                if(list==null||!list.isAttachedToWindow()||!list.isShown())throw new AssertionError("Account actions are hidden");
                if(list.getAdapter().getCount()!=7)throw new AssertionError("Missing account actions");
                if(!"Eliminar cuenta".equals(list.getAdapter().getItem(6)))throw new AssertionError("Missing deletion option");
                list.setSelection(6);
            }); waitForIdleSync();
            runOnMainSync(()->{
                ListView list=menu[0].getListView();
                View deletion=list.getChildAt(6-list.getFirstVisiblePosition());
                if(deletion==null||!deletion.isShown()||deletion.getHeight()==0)throw new AssertionError("Deletion option not reachable");
                menu[0].dismiss();
            });
            result.putString("result","Account menu: seven actions visible; deletion option reachable; no deletion requested");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("error",e.toString());finish(Activity.RESULT_CANCELED,result);}
        finally{if(activity!=null){MainActivity app=activity;runOnMainSync(()->{if(menu[0]!=null)menu[0].dismiss();app.finish();});}}
    }
}
