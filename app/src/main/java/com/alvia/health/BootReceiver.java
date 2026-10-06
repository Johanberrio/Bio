package com.alvia.health;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        String action=intent.getAction();
        if(!Intent.ACTION_BOOT_COMPLETED.equals(action)&&!Intent.ACTION_TIME_CHANGED.equals(action)&&!Intent.ACTION_TIMEZONE_CHANGED.equals(action)) return;
        try { Reminders.restore(context); } catch(Exception e) { android.util.Log.e("Bio","No se pudieron restaurar los recordatorios"); }
    }
}
