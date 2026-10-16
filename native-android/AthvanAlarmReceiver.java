package com.gityajna.athvan.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AthvanAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, AthvanAlarmService.class);
        service.putExtra("reminderId", intent.getStringExtra("reminderId"));
        service.putExtra("title", intent.getStringExtra("title"));
        service.putExtra("body", intent.getStringExtra("body"));

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
        } catch (Exception error) {
            android.util.Log.e("AthvanAlarm", "Could not start alarm service", error);
        }
    }
}
