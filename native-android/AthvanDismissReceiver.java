package com.gityajna.athvan.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AthvanDismissReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AthvanAlarmService.stopAlarm(context);
    }
}
