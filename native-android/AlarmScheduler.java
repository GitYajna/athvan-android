package com.gityajna.athvan.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.getcapacitor.JSArray;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class AlarmScheduler {
    private static final String PREFS = "athvan_native_alarms";
    private static final String KEY_REMINDERS = "reminders";

    private AlarmScheduler() {}

    public static synchronized int sync(Context context, JSONArray reminders) throws Exception {
        Context app = context.getApplicationContext();
        AlarmManager manager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            throw new IllegalStateException("Android AlarmManager is unavailable.");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            throw new SecurityException("Enable Alarms and reminders for Athvan in Android Settings.");
        }

        JSONArray old = getSavedReminders(app);
        for (int i = 0; i < old.length(); i++) {
            JSONObject item = old.optJSONObject(i);
            if (item != null) cancelOne(app, manager, item.optString("id", ""));
        }

        JSONArray future = new JSONArray();
        long now = System.currentTimeMillis();

        for (int i = 0; i < reminders.length(); i++) {
            JSONObject item = reminders.optJSONObject(i);
            if (item == null || item.optBoolean("completed", false)) continue;

            String id = item.optString("id", "");
            String title = item.optString("title", "").trim();
            String date = item.optString("date", "");
            String time = item.optString("time", "");
            String body = item.optString("notes", "Your reminder is due now.");
            if (id.isEmpty() || title.isEmpty() || date.isEmpty() || time.isEmpty()) continue;

            long triggerAt;
            try {
                triggerAt = LocalDateTime.parse(
                    date + "T" + time,
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
                ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (Exception ignored) {
                continue;
            }

            if (triggerAt <= now + 5000L) continue;

            scheduleOne(app, manager, id, title, body, triggerAt);

            JSONObject saved = new JSONObject();
            saved.put("id", id);
            saved.put("title", title);
            saved.put("notes", body);
            saved.put("date", date);
            saved.put("time", time);
            saved.put("completed", false);
            future.put(saved);
        }

        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_REMINDERS, future.toString()).apply();
        return future.length();
    }

    public static synchronized void rescheduleFromPreferences(Context context) {
        Context app = context.getApplicationContext();
        try {
            JSONArray saved = getSavedReminders(app);
            sync(app, saved);
        } catch (Exception error) {
            android.util.Log.e("AthvanAlarm", "Could not restore alarms", error);
        }
    }

    private static JSONArray getSavedReminders(Context context) {
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_REMINDERS, "[]");
        try {
            return new JSONArray(raw);
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private static int requestCode(String id) {
        int value = ("athvan:" + id).hashCode() & 0x7fffffff;
        return value == 0 ? 1 : value;
    }

    private static PendingIntent alarmPendingIntent(
        Context context, String id, String title, String body
    ) {
        Intent intent = new Intent(context, AthvanAlarmReceiver.class);
        intent.setAction("com.gityajna.athvan.alarm.FIRE");
        intent.putExtra("reminderId", id);
        intent.putExtra("title", title);
        intent.putExtra("body", body);
        return PendingIntent.getBroadcast(
            context,
            requestCode(id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static void scheduleOne(
        Context context, AlarmManager manager, String id,
        String title, String body, long triggerAt
    ) {
        PendingIntent alarm = alarmPendingIntent(context, id, title, body);

        Intent open = new Intent(context, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent show = PendingIntent.getActivity(
            context,
            requestCode(id),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(triggerAt, show);
        manager.setAlarmClock(info, alarm);
    }

    private static void cancelOne(Context context, AlarmManager manager, String id) {
        if (id == null || id.isEmpty()) return;
        PendingIntent pending = alarmPendingIntent(context, id, "", "");
        manager.cancel(pending);
        pending.cancel();
    }
}
