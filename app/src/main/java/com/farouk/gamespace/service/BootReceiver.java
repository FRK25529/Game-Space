package com.farouk.gamespace.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("gamespace_prefs", Context.MODE_PRIVATE);
            boolean autoBoost = prefs.getBoolean("auto_boost_on_boot", false);
            if (autoBoost) {
                com.farouk.gamespace.utils.AdbRunner.run(context,
                    "settings put global window_animation_scale 0.5");
                com.farouk.gamespace.utils.AdbRunner.run(context,
                    "settings put global transition_animation_scale 0.5");
                com.farouk.gamespace.utils.AdbRunner.run(context,
                    "settings put global animator_duration_scale 0.5");
            }
        }
    }
}
