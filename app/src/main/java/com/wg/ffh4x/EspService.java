package com.wg.ffh4x;

import android.app.*;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;

public class EspService extends Service {
    public static final String ACTION_START = "start";
    public static final String ACTION_STOP  = "stop";
    public static final String EXTRA_RESULT_CODE = "rc";
    public static final String EXTRA_DATA = "data";

    private static volatile String cfgJson = "{\"esp\":false,\"fov\":200}";
    private MediaProjection projection;
    private ScreenCapture capture;
    private OverlayView overlay;

    public static void updateConfig(String json) { cfgJson = json; }
    public static String getConfig() { return cfgJson; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(action)) { stopSelf(); return START_NOT_STICKY; }
        if (ACTION_START.equals(action) && intent != null) {
            startForeground(1, buildNotification());
            int rc = intent.getIntExtra(EXTRA_RESULT_CODE, -1);
            Intent data = intent.getParcelableExtra(EXTRA_DATA);
            MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            if (rc != -1 && data != null) {
                projection = mpm.getMediaProjection(rc, data);
                overlay = new OverlayView(this);
                overlay.show();
                capture = new ScreenCapture(this, projection, bitmap -> {
                    // sem modelo: o overlay desenha só o FOV; ESP depende de detector
                    overlay.updateBoxes(new float[0][]);
                });
                capture.start();
            }
        }
        return START_STICKY;
    }

    private Notification buildNotification() {
        String ch = "vt";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(ch, "FFH4X WG",
                NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
        return new Notification.Builder(this, ch)
            .setContentTitle("FFH4X WG")
            .setContentText("ESP ativo")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build();
    }

    @Override public void onDestroy() {
        if (capture != null) capture.stop();
        if (overlay != null) overlay.hide();
        if (projection != null) projection.stop();
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}