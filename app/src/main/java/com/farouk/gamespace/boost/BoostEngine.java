package com.farouk.gamespace.boost;

import android.app.ActivityManager;
import android.content.Context;
import com.farouk.gamespace.utils.AdbRunner;

public class BoostEngine {

    private final Context context;

    public interface LogCallback {
        void log(String msg);
    }

    public BoostEngine(Context context) {
        this.context = context;
    }

    public void runFullBoost(LogCallback logger) {
        logger.log("[ BOOST ] 🚀 بدء التحسين الشامل...");
        step1_killBackground(logger);
        sleep(400);
        step2_animationScale(logger);
        sleep(300);
        step3_dns(logger);
        sleep(300);
        step4_doze(logger);
        sleep(300);
        step5_network(logger);
        sleep(300);
        step6_gpu(logger);
        sleep(300);
        step7_clearCaches(logger);
        sleep(400);
        step8_touchBoost(logger);
        logger.log("[ BOOST ] ✓ اكتمل التحسين الشامل!");
    }

    private void step1_killBackground(LogCallback log) {
        log.log("[ 1/8 ] 💀 إيقاف التطبيقات الخلفية...");
        AdbRunner.run(context, "am kill-all");
        String[] heavyApps = {
            "com.android.chrome",
            "com.google.android.youtube",
            "com.facebook.katana",
            "com.instagram.android",
            "com.whatsapp"
        };
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            for (String pkg : heavyApps) {
                am.killBackgroundProcesses(pkg);
            }
        }
        log.log("[ 1/8 ] ✓ RAM محررة");
    }

    private void step2_animationScale(LogCallback log) {
        log.log("[ 2/8 ] ⚡ تسريع الرسوم المتحركة...");
        AdbRunner.run(context, "settings put global window_animation_scale 0.5");
        AdbRunner.run(context, "settings put global transition_animation_scale 0.5");
        AdbRunner.run(context, "settings put global animator_duration_scale 0.5");
        log.log("[ 2/8 ] ✓ Animation Scale → 0.5x");
    }

    private void step3_dns(LogCallback log) {
        log.log("[ 3/8 ] 🌐 تطبيق Google DNS...");
        AdbRunner.run(context, "settings put global private_dns_mode hostname");
        AdbRunner.run(context, "settings put global private_dns_specifier dns.google");
        log.log("[ 3/8 ] ✓ DNS → dns.google");
    }

    private void step4_doze(LogCallback log) {
        log.log("[ 4/8 ] 🔋 تعطيل Doze Mode...");
        AdbRunner.run(context, "dumpsys deviceidle disable all");
        log.log("[ 4/8 ] ✓ Doze معطّل");
    }

    private void step5_network(LogCallback log) {
        log.log("[ 5/8 ] 📡 تحسين الشبكة...");
        AdbRunner.run(context, "settings put global wifi_sleep_policy 2");
        AdbRunner.run(context, "settings put global wifi_watchdog_on 0");
        AdbRunner.run(context, "settings put global wifi_scan_always_enabled 0");
        AdbRunner.run(context, "settings put global mobile_data_always_on 1");
        log.log("[ 5/8 ] ✓ WiFi Power Saving → OFF");
    }

    private void step6_gpu(LogCallback log) {
        log.log("[ 6/8 ] 🎮 تفعيل GPU Rendering...");
        AdbRunner.run(context, "setprop debug.hwui.renderer opengl");
        AdbRunner.run(context, "setprop debug.egl.hw 1");
        AdbRunner.run(context, "setprop debug.sf.hw 1");
        log.log("[ 6/8 ] ✓ GPU Acceleration → ON");
    }

    private void step7_clearCaches(LogCallback log) {
        log.log("[ 7/8 ] 🗑️ مسح الكاشات...");
        AdbRunner.run(context, "pm trim-caches 999999999");
        AdbRunner.run(context, "sync");
        log.log("[ 7/8 ] ✓ Cache مُفرَّغ");
    }

    private void step8_touchBoost(LogCallback log) {
        log.log("[ 8/8 ] 👆 تحسين اللمس...");
        AdbRunner.run(context, "settings put system pointer_speed 3");
        log.log("[ 8/8 ] ✓ Touch Response محسّن");
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
