package com.alvia.health;
import android.app.*;
import android.os.Bundle;

/** Session follows the entire app, not the lifetime of its main screen. */
public class BioApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private int started;
    @Override public void onCreate(){super.onCreate();registerActivityLifecycleCallbacks(this);}
    @Override public void onActivityStarted(Activity activity){if(started++==0)AuthSession.foreground();}
    @Override public void onActivityStopped(Activity activity){started=Math.max(0,started-1);if(started==0&&!activity.isChangingConfigurations())AuthSession.background();}
    @Override public void onActivityCreated(Activity a,Bundle b){}
    @Override public void onActivityResumed(Activity a){}
    @Override public void onActivityPaused(Activity a){}
    @Override public void onActivitySaveInstanceState(Activity a,Bundle b){}
    @Override public void onActivityDestroyed(Activity a){}
}
