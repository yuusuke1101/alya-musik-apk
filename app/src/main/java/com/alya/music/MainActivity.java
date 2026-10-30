package com.alya.music;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

public class MainActivity extends Activity {
    private WebView web;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("alya", MODE_PRIVATE);
        getWindow().setStatusBarColor(Color.parseColor("#0a0a12"));
        getWindow().setNavigationBarColor(Color.parseColor("#0a0a12"));

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0a0a12"));
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        web.addJavascriptInterface(new Bridge(), "AlyaApp");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                String base = prefs.getString("server", "");
                if (!base.isEmpty() && u.getHost() != null && u.getHost().equals(Uri.parse(base).getHost())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) { }
                return true;
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) showError();
            }
        });

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        try {
            Intent i = new Intent(this, PlaybackService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        } catch (Exception e) { }

        String server = prefs.getString("server", "");
        if (server.isEmpty()) askServer(false); else web.loadUrl(server);
    }

    private String normalize(String u) {
        u = u.trim();
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://" + u;
        return u;
    }

    private void askServer(final boolean cancelable) {
        final EditText et = new EditText(this);
        et.setHint("https://alamat-server-kamu");
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        et.setText(prefs.getString("server", ""));
        AlertDialog.Builder d = new AlertDialog.Builder(this)
                .setTitle("Alamat server")
                .setMessage("Contoh: https://xxxx.trycloudflare.com atau http://192.168.1.5:3000")
                .setView(et)
                .setCancelable(cancelable)
                .setPositiveButton("Simpan", (dlg, w) -> {
                    String u = et.getText().toString().trim();
                    if (u.isEmpty()) { askServer(cancelable); return; }
                    u = normalize(u);
                    prefs.edit().putString("server", u).apply();
                    web.loadUrl(u);
                });
        if (cancelable) d.setNegativeButton("Batal", null);
        d.show();
    }

    private void showError() {
        String html = "<body style='background:#0a0a12;color:#f5f5fb;font-family:sans-serif;text-align:center;padding:48px 20px'>"
                + "<h3>Tidak bisa terhubung ke server</h3>"
                + "<p style='opacity:.65;word-break:break-all'>" + TextUtils.htmlEncode(prefs.getString("server", "")) + "</p>"
                + "<p><button style='padding:12px 22px;border-radius:12px;border:0;background:#8b5cf6;color:#fff;font-size:16px' onclick='AlyaApp.retry()'>Coba lagi</button></p>"
                + "<p><button style='padding:12px 22px;border-radius:12px;border:1px solid #555;background:none;color:#fff;font-size:16px' onclick='AlyaApp.changeServer()'>Ganti alamat</button></p></body>";
        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    class Bridge {
        @JavascriptInterface
        public void changeServer() { runOnUiThread(() -> askServer(true)); }

        @JavascriptInterface
        public void retry() { runOnUiThread(() -> web.loadUrl(prefs.getString("server", ""))); }

        @JavascriptInterface
        public void setPlaying(boolean p) {
            try {
                Intent i = new Intent(MainActivity.this, PlaybackService.class);
                i.putExtra("play", p);
                startService(i);
            } catch (Exception e) { }
        }
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(this, PlaybackService.class));
        web.destroy();
        super.onDestroy();
    }
}
