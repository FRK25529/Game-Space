package com.farouk.gamespace.utils;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MetricsReader {

    public interface CpuCallback { void onUpdate(int cpuPercent); }
    public interface RamCallback { void onUpdate(double ramGb); }
    public interface BatCallback { void onUpdate(int batPercent); }

    private final Context ctx;
    private ScheduledExecutorService executor;
    private BroadcastReceiver batReceiver;
    private long prevIdle = 0;
    private long prevTotal = 0;

    public MetricsReader(Context ctx) {
        this.ctx = ctx;
    }

    public void start(CpuCallback cpu, RamCallback ram, BatCallback bat) {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            cpu.onUpdate(readCpuUsage());
            ram.onUpdate(readRamGb());
        }, 0, 1500, TimeUnit.MILLISECONDS);

        batReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                bat.onUpdate(scale > 0 ? (level * 100 / scale) : level);
            }
        };
        ctx.registerReceiver(batReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    public void stop() {
        if (executor != null) executor.shutdownNow();
        if (batReceiver != null) {
            try { ctx.unregisterReceiver(batReceiver); } catch (Exception ignored) {}
        }
    }

    private int readCpuUsage() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"));
            String line = reader.readLine();
            reader.close();
            if (line == null || !line.startsWith("cpu")) return 0;
            String[] parts = line.trim().split("\\s+");
            long user    = Long.parseLong(parts[1]);
            long nice    = Long.parseLong(parts[2]);
            long system  = Long.parseLong(parts[3]);
            long idle    = Long.parseLong(parts[4]);
            long iowait  = parts.length > 5 ? Long.parseLong(parts[5]) : 0;
            long irq     = parts.length > 6 ? Long.parseLong(parts[6]) : 0;
            long softirq = parts.length > 7 ? Long.parseLong(parts[7]) : 0;
            long currentIdle  = idle + iowait;
            long currentTotal = user + nice + system + idle + iowait + irq + softirq;
            long diffIdle  = currentIdle  - prevIdle;
            long diffTotal = currentTotal - prevTotal;
            prevIdle  = currentIdle;
            prevTotal = currentTotal;
            if (diffTotal == 0) return 0;
            return (int)(100L * (diffTotal - diffIdle) / diffTotal);
        } catch (Exception e) {
            return readCpuFallback();
        }
    }

    private int readCpuFallback() {
        try {
            BufferedReader r = new BufferedReader(new FileReader("/proc/loadavg"));
            String line = r.readLine();
            r.close();
            if (line != null) {
                float load1m = Float.parseFloat(line.trim().split("\\s+")[0]);
                return Math.min(100, (int)(load1m / 4.0f * 100));
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private double readRamGb() {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return 0;
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        double usedMb = (mi.totalMem - mi.availMem) / (1024.0 * 1024.0);
        return usedMb / 1024.0;
    }

    public static float readCpuTemp() {
        String[] paths = {
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp"
        };
        for (String path : paths) {
            try {
                BufferedReader r = new BufferedReader(new FileReader(path));
                String val = r.readLine();
                r.close();
                if (val != null && !val.isEmpty()) {
                    float t = Float.parseFloat(val.trim());
                    if (t > 1000) t = t / 1000.0f;
                    if (t > 0 && t < 120) return t;
                }
            } catch (Exception ignored) {}
        }
        return -1f;
    }
}
