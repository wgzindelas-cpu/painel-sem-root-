package com.wg.ffh4x;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView web;
    private MediaProjectionManager mpm;
    private static final int REQ_PROJ = 1001;
    private static final int REQ_OVERLAY = 1002;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())), REQ_OVERLAY);
        }

        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setAllowFileAccess(true);
        web.getSettings().setDomStorageEnabled(true);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "WGBridge");
        web.loadUrl("file:///android_asset/panel.html");
        setContentView(web);
    }

    public class Bridge {
        @JavascriptInterface public void start() {
            runOnUiThread(() -> startActivityForResult(mpm.createScreenCaptureIntent(), REQ_PROJ));
        }
        @JavascriptInterface public void stop() {
            startService(new Intent(MainActivity.this, EspService.class)
                .setAction(EspService.ACTION_STOP));
        }
        @JavascriptInterface public void setConfig(String json) {
            EspService.updateConfig(json);
        }
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_PROJ) {
            if (res == RESULT_OK && data != null) {
                Intent svc = new Intent(this, EspService.class)
                    .setAction(EspService.ACTION_START)
                    .putExtra(EspService.EXTRA_RESULT_CODE, res)
                    .putExtra(EspService.EXTRA_DATA, data);
                startForegroundService(svc);
                Toast.makeText(this, "ESP ativo", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Captura negada", Toast.LENGTH_SHORT).show();
            }
        }
        if (req == REQ_OVERLAY && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Permissão de overlay necessária", Toast.LENGTH_LONG).show();
        }
    }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}