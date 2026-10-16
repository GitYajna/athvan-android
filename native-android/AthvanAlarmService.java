package com.gityajna.athvan.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

public class AthvanAlarmService extends Service {
    public static final String CHANNEL_ID = "athvan_alarm_repeat_v1";
    public static final String ACTION_DISMISSED = "com.gityajna.athvan.alarm.ACTION_DISMISSED";
    public static final int NOTIFICATION_ID = 761204;

    private MediaPlayer player;
    private String currentTitle = "Reminder";
    private String currentBody = "Your reminder is due now.";

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel(this);
    }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;

            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Athvan Alarm", NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Repeating sound for scheduled Athvan alarms.");
            // The foreground service plays the looped alarm sound itself.
            channel.setSound(null, null);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 250, 500, 250, 700});
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        String title = intent.getStringExtra("title");
        String body = intent.getStringExtra("body");
        currentTitle = (title == null || title.trim().isEmpty()) ? "Reminder" : title;
        currentBody = (body == null || body.trim().isEmpty()) ? "Your reminder is due now." : body;

        Notification notification = buildNotification(currentTitle, currentBody);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                );
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            startLoopingSound();
        } catch (Exception error) {
            android.util.Log.e("AthvanAlarm", "Could not start audible alarm", error);
            stopSelf(startId);
        }
        return START_NOT_STICKY;
    }

    private Notification buildNotification(String title, String body) {
        Intent screen = new Intent(this, AthvanAlarmActivity.class);
        screen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
        screen.putExtra("title", title);
        screen.putExtra("body", body);
        PendingIntent fullScreen = PendingIntent.getActivity(
            this, 761205, screen,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent dismiss = new Intent(this, AthvanDismissReceiver.class);
        dismiss.setAction("com.gityajna.athvan.alarm.DISMISS");
        PendingIntent dismissPending = PendingIntent.getBroadcast(
            this, 761206, dismiss,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(Notification.PRIORITY_MAX)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                dismissPending
            );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }
        return builder.build();
    }

    private void startLoopingSound() throws Exception {
        if (player != null && player.isPlaying()) return;

        releasePlayer();
        AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.athvan_alarm);
        if (file == null) throw new IllegalStateException("Missing athvan_alarm.wav in Android resources.");

        player = new MediaPlayer();
        player.setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build());
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
        file.close();
        player.setLooping(true);
        player.setVolume(1.0f, 1.0f);
        player.prepare();
        player.start();
    }

    public static void stopAlarm(Context context) {
        Context app = context.getApplicationContext();
        app.stopService(new Intent(app, AthvanAlarmService.class));

        NotificationManager manager =
            (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.cancel(NOTIFICATION_ID);

        Intent dismissed = new Intent(ACTION_DISMISSED);
        dismissed.setPackage(app.getPackageName());
        app.sendBroadcast(dismissed);
    }

    @Override
    public void onDestroy() {
        releasePlayer();
        stopForeground(STOP_FOREGROUND_REMOVE);
        NotificationManager manager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.cancel(NOTIFICATION_ID);
        super.onDestroy();
    }

    private void releasePlayer() {
        if (player != null) {
            try {
                if (player.isPlaying()) player.stop();
            } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
