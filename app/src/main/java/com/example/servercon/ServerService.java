package com.example.servercon;

import android.app.*;
import android.content.Intent;
import android.os.*;
import java.io.*;

public class ServerService extends Service {
    public static volatile ServerService inst;
    public static volatile Runnable onLog;
    static final StringBuilder log = new StringBuilder();
       java.lang.Process proc;
    OutputStream stdin;
    PowerManager.WakeLock wl;

    public static synchronized void append(String s) {
        log.append(s).append('\n');
        if (log.length() > 200000) log.delete(0, 50000);
        Runnable r = onLog;
        if (r != null) r.run();
    }
    public static synchronized String text() { return log.toString(); }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public int onStartCommand(Intent i, int f, int id) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("srv", "Server", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
        Notification n = (Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "srv") : new Notification.Builder(this))
                .setContentTitle("ServerCon running").setSmallIcon(android.R.drawable.ic_menu_manage).build();
        startForeground(1, n);
        inst = this;
        wl = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "servercon:wl");
        wl.acquire();
        if (proc == null) startShell();
        return START_STICKY;
    }

    void startShell() {
        try {
            File dir = Tools.serverDir(this);
            proc = new ProcessBuilder("/system/bin/sh").directory(dir).redirectErrorStream(true).start();
            stdin = proc.getOutputStream();
            append("[shell started in " + dir + "]");
            new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                    String l;
                    while ((l = r.readLine()) != null) append(l);
                } catch (IOException ignored) {}
                append("[shell exited]");
                proc = null;
            }).start();
        } catch (IOException e) { append("[error] " + e); }
    }

    public void send(String line) {
        try { if (stdin != null) { stdin.write((line + "\n").getBytes()); stdin.flush(); } else append("[not running]"); }
        catch (IOException e) { append("[error] " + e); }
    }

    @Override public void onDestroy() {
        if (proc != null) proc.destroy();
        proc = null; inst = null;
        if (wl != null && wl.isHeld()) wl.release();
        append("[stopped]");
    }
}
