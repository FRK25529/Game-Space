package com.farouk.gamespace.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.farouk.gamespace.R;
import com.farouk.gamespace.boost.BoostEngine;
import com.farouk.gamespace.ui.MainActivity;
import com.farouk.gamespace.utils.AdbRunner;
import com.farouk.gamespace.utils.MetricsReader;

public class OverlayService extends Service {

    public static final String ACTION_START = "START_OVERLAY";
    public static final String ACTION_STOP  = "STOP_OVERLAY";
    private static final String CHANNEL_ID  = "gamespace_channel";
    private static final int    NOTIF_ID    = 1;

    private WindowManager windowManager;
    private View tabView;
    private View sidebarView;
    private boolean sidebarOpen = false;

    private WindowManager.LayoutParams tabParams;
    private WindowManager.LayoutParams sidebarParams;

    private MetricsReader metricsReader;
    private BoostEngine boostEngine;
    private Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvFps, tvCpu, tvRam, tvBat, tvTemp, tvNet;
    private ProgressBar pbCpu, pbRam, pbBat, pbTemp;

    private long lastFrameTime = 0;
    private int frameCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        metricsReader = new MetricsReader(this);
        boostEngine   = new BoostEngine(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID, buildNotification());
        if (intent != null) {
            if (ACTION_START.equals(intent.getAction())) showOverlay();
            else if (ACTION_STOP.equals(intent.getAction())) { hideOverlay(); stopSelf(); }
        }
        return START_STICKY;
    }

    private void showOverlay() {
        if (tabView != null) return;

        int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        tabView = LayoutInflater.from(this).inflate(R.layout.overlay_tab, null);
        tabParams = new WindowManager.LayoutParams(
                dpToPx(28), dpToPx(72), overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        tabParams.gravity = Gravity.TOP | Gravity.START;
        tabParams.x = 0;
        tabParams.y = dpToPx(200);
        setupTabDrag();
        windowManager.addView(tabView, tabParams);

        sidebarView = LayoutInflater.from(this).inflate(R.layout.overlay_sidebar, null);
        sidebarParams = new WindowManager.LayoutParams(
                dpToPx(200), WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        sidebarParams.gravity = Gravity.TOP | Gravity.START;
        sidebarParams.x = dpToPx(28);
        sidebarParams.y = dpToPx(180);
        sidebarView.setVisibility(View.GONE);
        windowManager.addView(sidebarView, sidebarParams);

        bindSidebarViews();
        startMetrics();
    }

    private void toggleSidebar() {
        if (sidebarOpen) {
            sidebarView.setVisibility(View.GONE);
            sidebarOpen = false;
        } else {
            sidebarView.setVisibility(View.VISIBLE);
            sidebarOpen = true;
        }
    }

    private void setupTabDrag() {
        tabView.setOnTouchListener(new View.OnTouchListener() {
            private int initialY;
            private float initialTouchY;
            private long touchDownTime;
            private boolean moved = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialY = tabParams.y;
                        initialTouchY = event.getRawY();
                        touchDownTime = System.currentTimeMillis();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dy = (int)(event.getRawY() - initialTouchY);
                        if (Math.abs(dy) > 5) moved = true;
                        tabParams.y = initialY + dy;
                        if (sidebarOpen) {
                            sidebarParams.y = tabParams.y - dpToPx(20);
                            windowManager.updateViewLayout(sidebarView, sidebarParams);
                        }
                        windowManager.updateViewLayout(tabView, tabParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved && System.currentTimeMillis() - touchDownTime < 300) {
                            toggleSidebar();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void bindSidebarViews() {
        tvFps  = sidebarView.findViewById(R.id.sb_fps);
        tvCpu  = sidebarView.findViewById(R.id.sb_cpu);
        tvRam  = sidebarView.findViewById(R.id.sb_ram);
        tvBat  = sidebarView.findViewById(R.id.sb_bat);
        tvTemp = sidebarView.findViewById(R.id.sb_temp);
        tvNet  = sidebarView.findViewById(R.id.sb_net);
        pbCpu  = sidebarView.findViewById(R.id.pb_cpu);
        pbRam  = sidebarView.findViewById(R.id.pb_ram);
        pbBat  = sidebarView.findViewById(R.id.pb_bat);
        pbTemp = sidebarView.findViewById(R.id.pb_temp);

        View btnBoost = sidebarView.findViewById(R.id.sb_btn_boost);
        if (btnBoost != null) btnBoost.setOnClickListener(v -> {
            Toast.makeText(this, "⚡ Boost!", Toast.LENGTH_SHORT).show();
            new Thread(() -> boostEngine.runFullBoost(msg -> {})).start();
        });

        View btnClose = sidebarView.findViewById(R.id.sb_btn_close);
        if (btnClose != null) btnClose.setOnClickListener(v -> toggleSidebar());

        View btnKill = sidebarView.findViewById(R.id.sb_btn_kill);
        if (btnKill != null) btnKill.setOnClickListener(v -> new Thread(() -> {
            AdbRunner.run(this, "am kill-all");
            handler.post(() -> Toast.makeText(this, "✓ RAM مُحرَّرة", Toast.LENGTH_SHORT).show());
        }).start());
    }

    private void startMetrics() {
        metricsReader.start(
            cpu -> handler.post(() -> {
                if (tvCpu != null) tvCpu.setText(cpu + "%");
                if (pbCpu != null) pbCpu.setProgress(cpu);
            }),
            ram -> handler.post(() -> {
                if (tvRam != null) tvRam.setText(String.format("%.1fG", ram));
                if (pbRam != null) pbRam.setProgress((int)((ram / 4.0) * 100));
            }),
            bat -> handler.post(() -> {
                if (tvBat != null) tvBat.setText(bat + "%");
                if (pbBat != null) pbBat.setProgress(bat);
            })
        );
        startFpsCounter();
    }

    private void startFpsCounter() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                frameCount++;
                if (lastFrameTime == 0) lastFrameTime = now;
                if (now - lastFrameTime >= 1000) {
                    int fps = (int)(frameCount * 1000L / (now - lastFrameTime));
                    fps = Math.min(fps, 60);
                    if (tvFps != null) tvFps.setText(fps + " FPS");
                    frameCount = 0;
                    lastFrameTime = now;
                }
                handler.postDelayed(this, 16);
            }
        });
    }

    private void hideOverlay() {
        if (tabView != null) {
            try { windowManager.removeView(tabView); } catch (Exception ignored) {}
            tabView = null;
        }
        if (sidebarView != null) {
            try { windowManager.removeView(sidebarView); } catch (Exception ignored) {}
            sidebarView = null;
        }
        if (metricsReader != null) metricsReader.stop();
        handler.removeCallbacksAndMessages(null);
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE);
        Intent stopIntent = new Intent(this, OverlayService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Farouk Game Space")
                .setContentText("Side Bar نشطة")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pi)
                .addAction(android.R.drawable.ic_delete, "إيقاف", stopPi)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Game Space", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() { hideOverlay(); super.onDestroy(); }

    private int dpToPx(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }
