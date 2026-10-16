package com.gityajna.athvan.alarm;

import android.os.Build;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "AthvanAlarm")
public class AthvanAlarmPlugin extends Plugin {

    @PluginMethod
    public void sync(PluginCall call) {
        JSArray reminders = call.getArray("reminders");
        if (reminders == null) {
            call.reject("Missing reminders array.");
            return;
        }

        try {
            int scheduled = AlarmScheduler.sync(getContext(), reminders);
            JSObject result = new JSObject();
            result.put("scheduled", scheduled);
            call.resolve(result);
        } catch (Exception error) {
            call.reject(
                "Athvan could not schedule alarms: " +
                (error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage()),
                error
            );
        }
    }
}
