package com.bytefuel.drivesense;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by arunesh on 7/13/15.
 */
public class SensorService extends Service {
    private static String TAG = SensorService.class.getCanonicalName();
    HandlerThread handlerThread;

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind() called.");
        return null;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate() called.");
        // We create a separate thread because the service normally runs on the process's main
        // thread, which we don't want to block.
        handlerThread = new HandlerThread("", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
     }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }
}
