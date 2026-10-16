package com.gityajna.athvan.alarm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AthvanAlarmActivity extends Activity {
    private BroadcastReceiver dismissedReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        String title = getIntent().getStringExtra("title");
        String body = getIntent().getStringExtra("body");
        if (title == null || title.trim().isEmpty()) title = "Reminder";
        if (body == null || body.trim().isEmpty()) body = "Your reminder is due now.";

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(28), dp(28), dp(28));
        root.setBackgroundColor(Color.rgb(24, 62, 43));

        TextView brand = new TextView(this);
        brand.setText("Athvan");
        brand.setTextColor(Color.rgb(222, 236, 215));
        brand.setTextSize(19);
        brand.setLetterSpacing(0.12f);
        brand.setGravity(Gravity.CENTER);
        root.addView(brand, matchWrap());

        TextView alarm = new TextView(this);
        alarm.setText("REMINDER");
        alarm.setTextColor(Color.rgb(202, 220, 197));
        alarm.setTextSize(14);
        alarm.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams alarmParams = matchWrap();
        alarmParams.topMargin = dp(48);
        root.addView(alarm, alarmParams);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(30);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = matchWrap();
        titleParams.topMargin = dp(14);
        root.addView(titleView, titleParams);

        TextView bodyView = new TextView(this);
        bodyView.setText(body);
        bodyView.setTextColor(Color.rgb(224, 231, 221));
        bodyView.setTextSize(17);
        bodyView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams bodyParams = matchWrap();
        bodyParams.topMargin = dp(14);
        root.addView(bodyView, bodyParams);

        Button dismiss = new Button(this);
        dismiss.setText("DISMISS");
        dismiss.setTextColor(Color.rgb(24, 62, 43));
        dismiss.setTextSize(16);
        dismiss.setAllCaps(true);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(58)
        );
        buttonParams.topMargin = dp(72);
        dismiss.setOnClickListener(view -> {
            AthvanAlarmService.stopAlarm(this);
            finish();
        });
        root.addView(dismiss, buttonParams);

        setContentView(root);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dismissedReceiver == null) {
            dismissedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    finish();
                }
            };
            IntentFilter filter = new IntentFilter(AthvanAlarmService.ACTION_DISMISSED);
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(dismissedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(dismissedReceiver, filter);
            }
        }
    }

    @Override
    protected void onPause() {
        if (dismissedReceiver != null) {
            try { unregisterReceiver(dismissedReceiver); } catch (Exception ignored) {}
            dismissedReceiver = null;
        }
        super.onPause();
    }
}
