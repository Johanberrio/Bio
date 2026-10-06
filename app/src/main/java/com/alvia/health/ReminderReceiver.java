package com.alvia.health;
import android.app.*;
import android.content.*;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import org.json.JSONObject;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return;
        try {
            JSONObject match=null;
            for(JSONObject a:new Vault(context).list("appointments")) if(a.optString("id").equals(intent.getStringExtra("id"))) match=a;
            if(match==null || match.optLong("time")<System.currentTimeMillis()-600000) return;
            Reminders.channel(context);
            PendingIntent open=PendingIntent.getActivity(context,0,new Intent(context,MainActivity.class).putExtra("screen",1),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            String[] labels={"Tienes una cita mañana","Tienes una cita hoy","Tu cita está cerca"};
            int slot=Math.max(0,Math.min(2,intent.getIntExtra("slot",0)));
            Notification notification=new Notification.Builder(context,Reminders.CHANNEL).setSmallIcon(com.alvia.health.R.drawable.ic_bio).setContentTitle(labels[slot]).setContentText("Abre Bio para consultar los detalles.").setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build();
            context.getSystemService(NotificationManager.class).notify(intent.getDataString(),slot,notification);
        } catch(Exception e) { android.util.Log.e("Bio","No se pudo leer el recordatorio"); }
    }
}
