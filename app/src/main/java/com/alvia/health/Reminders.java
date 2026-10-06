package com.alvia.health;
import android.app.*;
import android.content.*;
import org.json.JSONObject;
import java.time.ZoneId;

final class Reminders {
    static final String CHANNEL="appointments";
    static void channel(Context c) {
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Recordatorios de citas",NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("El día anterior, el mismo día y una hora antes");
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        c.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
    static PendingIntent intent(Context c,String id,int slot) {
        Intent intent=new Intent(c,ReminderReceiver.class).setData(android.net.Uri.parse("alvia://reminder/"+id+"/"+slot)).putExtra("id",id).putExtra("slot",slot);
        return PendingIntent.getBroadcast(c,0,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static void cancel(Context c,String id) { for(int slot=0;slot<3;slot++) c.getSystemService(AlarmManager.class).cancel(intent(c,id,slot)); }
    static void schedule(Context c,JSONObject appointment) {
        String id=appointment.optString("id"); cancel(c,id);
        long[] times=ReminderTimes.forAppointment(appointment.optLong("time"),ZoneId.systemDefault());
        for(int i=0;i<times.length;i++) if(times[i]>System.currentTimeMillis()) c.getSystemService(AlarmManager.class).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,times[i],intent(c,id,i));
    }
    static void restore(Context c) throws Exception { channel(c); for(JSONObject item:new Vault(c).list("appointments")) schedule(c,item); }
}
