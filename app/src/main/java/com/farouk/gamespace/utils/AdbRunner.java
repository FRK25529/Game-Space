package com.farouk.gamespace.utils;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class AdbRunner {

    private static final String TAG = "AdbRunner";

    public static String run(Context ctx, String command) {
        if (command.startsWith("settings put global ")) {
            String[] parts = command.replace("settings put global ", "").split(" ", 2);
            if (parts.length == 2) {
                try {
                    Settings.Global.putString(ctx.getContentResolver(), parts[0], parts[1]);
                    Log.d(TAG, "✓ Global: " + parts[0] + " = " + parts[1]);
                    return "ok";
                } catch (SecurityException e) {
                    Log.w(TAG, "Fallback to shell: " + e.getMessage());
                }
            }
        }
        if (command.startsWith("settings put system ")) {
            String[] parts = command.replace("settings put system ", "").split(" ", 2);
            if (parts.length == 2) {
                try {
                    Settings.System.putString(ctx.getContentResolver(), parts[0], parts[1]);
                    Log.d(TAG, "✓ System: " + parts[0] + " = " + parts[1]);
                    return "ok";
                } catch (SecurityException e) {
                    Log.w(TAG, "Fallback to shell");
                }
            }
        }
        return execShell(command);
    }

    public static String execShell(String cmd) {
        StringBuilder output = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
            Log.d(TAG, "$ " + cmd + " → " + output.toString().trim());
        } catch (Exception e) {
            Log.e(TAG, "Shell error: " + cmd + " → " + e.getMessage());
        }
        return output.toString().trim();
    }

    public static String readFile(String path) {
        return execShell("cat " + path);
    }

    public static boolean test(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
