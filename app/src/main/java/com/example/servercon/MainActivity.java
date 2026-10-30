package com.example.servercon;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

public class MainActivity extends Activity {
    TextView out; ScrollView sv; EditText in;
    final Handler h = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout bar = new LinearLayout(this);
        bar.addView(btn("Start", () -> {
            Intent i = new Intent(this, ServerService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        }));
        bar.addView(btn("Stop", () -> stopService(new Intent(this, ServerService.class))));
        bar.addView(btn("Optimize", () -> Tools.optimize(this)));
        bar.addView(btn("Run", () -> run("sh start.sh")));
        root.addView(bar);

        sv = new ScrollView(this);
        out = new TextView(this);
        out.setTypeface(Typeface.MONOSPACE); out.setTextSize(11); out.setTextColor(Color.GREEN);
        out.setBackgroundColor(Color.BLACK); out.setPadding(12, 12, 12, 12);
        out.setTextIsSelectable(true);
        sv.addView(out);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        in = new EditText(this);
        in.setHint("cmd | dl <url> [name] | plugins | ip <addr|any> | port <n> | myip");
        in.setSingleLine(true); in.setImeOptions(EditorInfo.IME_ACTION_SEND);
        in.setOnEditorActionListener((v, a, e) -> { run(in.getText().toString()); in.setText(""); return true; });
        root.addView(in);
        setContentView(root);

        ServerService.onLog = () -> h.post(this::refresh);
        refresh();
    }

    Button btn(String t, Runnable r) {
        Button x = new Button(this); x.setText(t); x.setOnClickListener(v -> r.run());
        x.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        return x;
    }

    void run(String line) {
        line = line.trim();
        if (line.isEmpty()) return;
        ServerService.append("> " + line);
        if (line.startsWith("dl ")) {
            String[] p = line.substring(3).trim().split("\\s+");
            String name = p.length > 1 ? p[1] : p[0].replaceAll("[?#].*", "").replaceAll(".*/", "");
            Tools.download(this, p[0], name);
        } else if (line.startsWith("ip ")) {
            String v = line.substring(3).trim();
            Tools.setProp(this, "server-ip", v.equals("any") ? "" : v);
        } else if (line.startsWith("port ")) {
            Tools.setProp(this, "server-port", line.substring(5).trim());
        } else if (line.equals("myip")) {
            Tools.showIps();
        } else if (line.equals("plugins")) {
            run("ls -la plugins");
        } else if (ServerService.inst != null) {
            ServerService.inst.send(line);
        } else ServerService.append("[press Start first]");
    }

    void refresh() {
        out.setText(ServerService.text());
        sv.post(() -> sv.fullScroll(ScrollView.FOCUS_DOWN));
    }

    @Override protected void onDestroy() { ServerService.onLog = null; super.onDestroy(); }
}
