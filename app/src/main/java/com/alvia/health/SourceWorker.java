package com.alvia.health;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.*;
public class SourceWorker extends Worker {
    public SourceWorker(@NonNull Context c,@NonNull WorkerParameters p){super(c,p);}
    @NonNull @Override public Result doWork(){try{SourceSync.run(getApplicationContext(),true);return Result.success();}catch(Exception e){return Result.retry();}}
}
