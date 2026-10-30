package com.alya.music;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView web;
    private int port;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.parseColor("#0a0a12"));
        getWindow().setNavigationBarColor(Color.parseColor("#0a0a12"));

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0a0a12"));
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        web.addJavascriptInterface(new Bridge(), "AlyaApp");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("127.0.0.1".equals(u.getHost())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) { }
                return true;
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) {
                    v.loadDataWithBaseURL(null, "<body style='background:#0a0a12;color:#f5f5fb;font-family:sans-serif;text-align:center;padding:60px 20px'>"
                            + "<h3>Gagal memuat aplikasi</h3><p>Tutup aplikasi lalu buka lagi.</p></body>", "text/html", "utf-8", null);
                }
            }
        });

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        try {
            Intent i = new Intent(this, PlaybackService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        } catch (Exception e) { }

        port = AlyaServer.ensure(this);
        if (port < 0) {
            Toast.makeText(this, "Server internal gagal dimulai", Toast.LENGTH_LONG).show();
        } else {
            web.loadUrl("http://127.0.0.1:" + port + "/");
        }
    }

    class Bridge {
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
