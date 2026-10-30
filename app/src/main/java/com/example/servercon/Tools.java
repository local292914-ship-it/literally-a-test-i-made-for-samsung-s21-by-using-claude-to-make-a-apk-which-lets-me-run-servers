package com.example.servercon;

import android.content.Context;
import java.io.*;
import java.net.*;
import java.util.Properties;

public class Tools {
    static File serverDir(Context c) {
        File d = new File(c.getFilesDir(), "server");
        new File(d, "plugins").mkdirs();
        return d;
    }

    static void download(Context c, String url, String name) {
        new Thread(() -> {
            try {
                if (name == null || name.isEmpty()) { ServerService.append("[download] bad name"); return; }
                HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
                h.setInstanceFollowRedirects(true);
                h.setRequestProperty("User-Agent", "ServerCon/1.0");
                File out = new File(new File(serverDir(c), "plugins"), name);
                ServerService.append("[download] " + url);
                try (InputStream in = h.getInputStream(); FileOutputStream o = new FileOutputStream(out)) {
                    byte[] b = new byte[16384]; int n; long t = 0;
                    while ((n = in.read(b)) > 0) { o.write(b, 0, n); t += n; }
                    ServerService.append("[download] saved plugins/" + name + " (" + t / 1024 + " KB)");
                }
            } catch (Exception e) { ServerService.append("[download failed] " + e); }
        }).start();
    }

    static void setProp(Context c, String key, String val) {
        try {
            File pf = new File(serverDir(c), "server.properties");
            Properties p = new Properties();
            if (pf.exists()) try (FileInputStream i = new FileInputStream(pf)) { p.load(i); }
            p.setProperty(key, val);
            try (FileOutputStream o = new FileOutputStream(pf)) { p.store(o, "edited by ServerCon"); }
            ServerService.append("[set] " + key + "=" + val + " (restart the server to apply)");
        } catch (Exception e) { ServerService.append("[set failed] " + e); }
    }

    static void showIps() {
        try {
            for (NetworkInterface n : java.util.Collections.list(NetworkInterface.getNetworkInterfaces()))
                for (InetAddress a : java.util.Collections.list(n.getInetAddresses()))
                    if (a instanceof Inet4Address && !a.isLoopbackAddress())
                        ServerService.append("[ip] " + n.getName() + " " + a.getHostAddress());
        } catch (Exception e) { ServerService.append("[ip failed] " + e); }
    }

    // Tuned server.properties + Aikar-style start.sh (expects `java` on PATH or edit JAVA below)
    static void optimize(Context c) {
        try {
            File d = serverDir(c);
            File pf = new File(d, "server.properties");
            Properties p = new Properties();
            if (pf.exists()) try (FileInputStream i = new FileInputStream(pf)) { p.load(i); }
            if (p.getProperty("server-port") == null) p.setProperty("server-port", "25565");
            p.setProperty("view-distance", "6");
            p.setProperty("simulation-distance", "4");
            p.setProperty("network-compression-threshold", "256");
            try (FileOutputStream o = new FileOutputStream(pf)) { p.store(o, "tuned by ServerCon"); }
            try (FileWriter w = new FileWriter(new File(d, "eula.txt"))) { w.write("eula=true\n"); }
            File sh = new File(d, "start.sh");
            try (FileWriter w = new FileWriter(sh)) {
                w.write("#!/system/bin/sh\nJAVA=java\n"
                  + "$JAVA -Xms1G -Xmx2G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 "
                  + "-XX:+DisableExplicitGC -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 "
                  + "-jar server.jar nogui\n");
            }
            sh.setExecutable(true);
            ServerService.append("[optimize] wrote server.properties + start.sh (put server.jar in the server folder)");
            ServerService.append("[optimize] eula.txt set to eula=true; you are accepting the Minecraft EULA");
        } catch (Exception e) { ServerService.append("[optimize failed] " + e); }
    }
}
