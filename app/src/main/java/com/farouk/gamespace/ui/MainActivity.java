package com.farouk.gamespace.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.farouk.gamespace.R;
import com.farouk.gamespace.boost.BoostEngine;
import com.farouk.gamespace.service.OverlayService;
import com.farouk.gamespace.utils.AdbRunner;
import com.farouk.gamespace.utils.MetricsReader;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY = 1001;
    private BoostEngine boostEngine;
    private SharedPreferences prefs;
    private TextView tvStatus, tvCpu, tvRam, tvBat, tvFps;
    private Button btnBoost, btnToggleOverlay;
    private Switch swDoze, swAnim, swDns, swWifi, swHaptic, swTouch;
    private MetricsReader metricsReader;
    private boolean overlayRunning = false;
    private TextView tvLog;
    private ScrollView scrollLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("gamespace_prefs", MODE_PRIVATE);
        boostEngine = new BoostEngine(this);
        metricsReader = new MetricsReader(this);
        initViews();
        checkOverlayPermission();
        startMetricsUpdate();
        loadSavedSettings();
        logLine("✓ Farouk Game Space — Tecno Spark Go 2024");
        logLine("✓ Helio A22 · Android " + Build.VERSION.RELEASE);
    }

    private void initViews() {
        tvStatus         = findViewById(R.id.tv_status);
        tvCpu            = findViewById(R.id.tv_cpu);
        tvRam            = findViewById(R.id.tv_ram);
        tvBat            = findViewById(R.id.tv_bat);
        tvFps            = findViewById(R.id.tv_fps);
        btnBoost         = findViewById(R.id.btn_boost);
        btnToggleOverlay = findViewById(R.id.btn_toggle_overlay);
        swDoze           = findViewById(R.id.sw_doze);
        swAnim           = findViewById(R.id.sw_anim);
        swDns            = findViewById(R.id.sw_dns);
        swWifi           = findViewById(R.id.sw_wifi);
        swHaptic         = findViewById(R.id.sw_haptic);
        swTouch          = findViewById(R.id.sw_touch);
        tvLog            = findViewById(R.id.tv_log);
        scrollLog        = findViewById(R.id.scroll_log);

        btnBoost.setOnClickListener(v -> runFullBoost());
        btnToggleOverlay.setOnClickListener(v -> toggleOverlay());

        findViewById(R.id.btn_profile_save).setOnClickListener(v -> applyProfile("save"));
        findViewById(R.id.btn_profile_balanced).setOnClickListener(v -> applyProfile("balanced"));
        findViewById(R.id.btn_profile_performance).setOnClickListener(v -> applyProfile("performance"));

        findViewById(R.id.btn_opt_efootball).setOnClickListener(v -> optimizeGame("com.konami.pesam", "eFootball"));
        findViewById(R.id.btn_opt_freefire).setOnClickListener(v -> optimizeGame("com.dts.freefiremax", "Free Fire MAX"));
        findViewById(R.id.btn_launch_efootball).setOnClickListener(v -> launchGame("com.konami.pesam"));
        findViewById(R.id.btn_launch_freefire).setOnClickListener(v -> launchGame("com.dts.freefiremax"));

        findViewById(R.id.btn_killbg).setOnClickListener(v -> runCmd("killbg"));
        findViewById(R.id.btn_gpu).setOnClickListener(v -> runCmd("gpu"));
        findViewById(R.id.btn_dns).setOnClickListener(v -> runCmd("dns"));
        findViewById(R.id.btn_clearcache).setOnClickListener(v -> runCmd("clearcache"));
        findViewById(R.id.btn_network).setOnClickListener(v -> runCmd("network"));
        findViewById(R.id.btn_touchboost).setOnClickListener(v -> runCmd("touchboost"));

        swDoze.setOnCheckedChangeListener((b, on) -> {
            AdbRunner.run(this, on ? "dumpsys deviceidle disable all" : "dumpsys deviceidle enable all");
            logLine((on ? "✓ تعطيل" : "✗ تفعيل") + " Doze Mode");
            prefs.edit().putBoolean("sw_doze", on).apply();
        });
        swAnim.setOnCheckedChangeListener((b, on) -> {
            String v = on ? "0.5" : "1.0";
            AdbRunner.run(this, "settings put global window_animation_scale " + v);
            AdbRunner.run(this, "settings put global transition_animation_scale " + v);
            AdbRunner.run(this, "settings put global animator_duration_scale " + v);
            logLine("✓ Animation Scale → " + v + "x");
            prefs.edit().putBoolean("sw_anim", on).apply();
        });
        swDns.setOnCheckedChangeListener((b, on) -> {
            if (on) {
                AdbRunner.run(this, "settings put global private_dns_mode hostname");
                AdbRunner.run(this, "settings put global private_dns_specifier dns.google");
                logLine("✓ DNS → Google");
            } else {
                AdbRunner.run(this, "settings put global private_dns_mode off");
                logLine("✗ DNS → افتراضي");
            }
            prefs.edit().putBoolean("sw_dns", on).apply();
        });
        swWifi.setOnCheckedChangeListener((b, on) -> {
            AdbRunner.run(this, "settings put global wifi_sleep_policy " + (on ? "2" : "0"));
            AdbRunner.run(this, "settings put global wifi_watchdog_on " + (on ? "0" : "1"));
            logLine((on ? "✓" : "✗") + " WiFi Power Saving");
            prefs.edit().putBoolean("sw_wifi", on).apply();
        });
        swHaptic.setOnCheckedChangeListener((b, on) -> {
            AdbRunner.run(this, "settings put system haptic_feedback_enabled " + (on ? "0" : "1"));
            logLine((on ? "✓ إيقاف" : "✗ تفعيل") + " Haptic");
            prefs.edit().putBoolean("sw_haptic", on).apply();
        });
        swTouch.setOnCheckedChangeListener((b, on) -> {
            AdbRunner.run(this, "settings put system pointer_speed " + (on ? "3" : "0"));
            logLine((on ? "✓" : "✗") + " Touch Boost");
            prefs.edit().putBoolean("sw_touch", on).apply();
        });
    }

    private void loadSavedSettings() {
        swDoze.setChecked(prefs.getBoolean("sw_doze", false));
        swAnim.setChecked(prefs.getBoolean("sw_anim", true));
        swDns.setChecked(prefs.getBoolean("sw_dns", false));
        swWifi.setChecked(prefs.getBoolean("sw_wifi", false));
        swHaptic.setChecked(prefs.getBoolean("sw_haptic", false));
        swTouch.setChecked(prefs.getBoolean("sw_touch", false));
    }

    private void checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            tvStatus.setText("⚠ صلاحية Overlay مطلوبة");
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, REQUEST_OVERLAY);
        } else {
            tvStatus.setText("✓ جاهز — صلاحيات كاملة");
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQUEST_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                tvStatus.setText("✓ جاهز — صلاحية Overlay ✓");
                logLine("✓ صلاحية Overlay ممنوحة");
            } else {
                logLine("✗ صلاحية Overlay مرفوضة");
            }
        }
    }

    private void toggleOverlay() {
        if (!Settings.canDrawOverlays(this)) { checkOverlayPermission(); return; }
        Intent intent = new Intent(this, OverlayService.class);
        if (!overlayRunning) {
            intent.setAction(OverlayService.ACTION_START);
            ContextCompat.startForegroundService(this, intent);
            btnToggleOverlay.setText("⛔ إخفاء Side Bar");
            overlayRunning = true;
            logLine("✓ Side Bar مفعّلة — افتح اللعبة الآن");
        } else {
            intent.setAction(OverlayService.ACTION_STOP);
            startService(intent);
            btnToggleOverlay.setText("🎮 تفعيل Side Bar");
            overlayRunning = false;
            logLine("✗ Side Bar مخفية");
        }
    }

    private void runFullBoost() {
        btnBoost.setText("⚡ BOOSTING...");
        btnBoost.setEnabled(false);
        logLine("🚀 بدء Boost الشامل...");
        new Thread(() -> {
            boostEngine.runFullBoost(msg -> runOnUiThread(() -> logLine(msg)));
            runOnUiThread(() -> {
                btnBoost.setText("✓ BOOSTED!");
                Toast.makeText(this, "✓ Boost اكتمل!", Toast.LENGTH_SHORT).show();
                btnBoost.postDelayed(() -> {
                    btnBoost.setText("⚡ BOOST");
                    btnBoost.setEnabled(true);
                }, 3000);
            });
        }).start();
    }

    private void runCmd(String key) {
        new Thread(() -> {
            String[] cmds = getCommandSet(key);
            for (String cmd : cmds) {
                AdbRunner.run(this, cmd);
                runOnUiThread(() -> logLine("$ " + cmd));
            }
            runOnUiThread(() -> logLine("✓ تم: " + key));
        }).start();
    }

    private String[] getCommandSet(String key) {
        switch (key) {
            case "killbg": return new String[]{"am kill-all"};
            case "gpu": return new String[]{"setprop debug.hwui.renderer opengl","setprop debug.egl.hw 1","setprop debug.sf.hw 1"};
            case "dns": return new String[]{"settings put global private_dns_mode hostname","settings put global private_dns_specifier dns.google"};
            case "clearcache": return new String[]{"pm trim-caches 999999999","sync"};
            case "network": return new String[]{"settings put global wifi_sleep_policy 2","settings put global wifi_watchdog_on 0"};
            case "touchboost": return new String[]{"settings put system pointer_speed 3"};
            default: return new String[]{};
        }
    }

    private void applyProfile(String profile) {
        new Thread(() -> {
            String[] cmds;
            switch (profile) {
                case "save":
                    cmds = new String[]{"settings put global window_animation_scale 1.0","settings put global low_power 1"};
                    runOnUiThread(() -> logLine("🌿 ملف التوفير مفعّل"));
                    break;
                case "performance":
                    cmds = new String[]{"settings put global window_animation_scale 0","settings put global transition_animation_scale 0","settings put global animator_duration_scale 0","settings put global low_power 0","settings put global wifi_sleep_policy 2","dumpsys deviceidle disable all","am kill-all"};
                    runOnUiThread(() -> logLine("🏆 ملف الأداء الأقصى مفعّل"));
                    break;
                default:
                    cmds = new String[]{"settings put global window_animation_scale 0.5","settings put global transition_animation_scale 0.5","settings put global animator_duration_scale 0.5"};
                    runOnUiThread(() -> logLine("⚖️ ملف المتوازن مفعّل"));
            }
            for (String cmd : cmds) {
                AdbRunner.run(this, cmd);
                String c = cmd;
                runOnUiThread(() -> logLine("$ " + c));
            }
        }).start();
    }

    private void optimizeGame(String pkg, String name) {
        new Thread(() -> {
            AdbRunner.run(this, "am force-stop " + pkg);
            AdbRunner.run(this, "am set-process-limit 0");
            runOnUiThread(() -> {
                logLine("✓ " + name + " محسّن وجاهز");
                Toast.makeText(this, "✓ " + name + " محسّن", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void launchGame(String pkg) {
        new Thread(() -> {
            AdbRunner.run(this, "am kill-all");
            runOnUiThread(() -> {
                logLine("🚀 إطلاق " + pkg);
                PackageManager pm = getPackageManager();
                Intent intent = pm.getLaunchIntentForPackage(pkg);
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    logLine("✗ اللعبة غير مثبتة");
                }
            });
        }).start();
    }

    private void startMetricsUpdate() {
        metricsReader.start(
            cpu -> runOnUiThread(() -> tvCpu.setText(cpu + "%")),
            ram -> runOnUiThread(() -> tvRam.setText(String.format("%.1f", ram) + "G")),
            bat -> runOnUiThread(() -> tvBat.setText(bat + "%"))
        );
    }

    private void logLine(String msg) {
        String current = tvLog.getText().toString();
        String[] lines = current.split("\n");
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, lines.length - 49);
        for (int i = start; i < lines.length; i++) {
            if (!lines[i].isEmpty()) sb.append(lines[i]).append("\n");
        }
        sb.append(msg);
        tvLog.setText(sb.toString());
        scrollLog.post(() -> scrollLog.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (metricsReader != null) metricsReader.stop();
    }
              }
