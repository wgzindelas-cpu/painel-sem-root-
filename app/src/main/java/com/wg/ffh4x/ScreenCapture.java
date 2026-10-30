package com.wg.ffh4x;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class ScreenCapture {
    public interface FrameListener { void onFrame(Bitmap bmp); }
    private final Context ctx;
    private final MediaProjection projection;
    private final FrameListener listener;
    private ImageReader reader;
    private VirtualDisplay vd;
    private HandlerThread thread;
    private Handler handler;
    private int w, h, dpi;
    private volatile long lastFrame = 0;
    private static final long MIN_INTERVAL_MS = 50;

    public ScreenCapture(Context ctx, MediaProjection p, FrameListener l) {
        this.ctx = ctx; this.projection = p; this.listener = l;
    }

    public void start() {
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(dm);
        w = dm.widthPixels; h = dm.heightPixels; dpi = dm.densityDpi;

        thread = new HandlerThread("cap"); thread.start();
        handler = new Handler(thread.getLooper());

        reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2);
        reader.setOnImageAvailableListener(r -> {
            long now = System.currentTimeMillis();
            if (now - lastFrame < MIN_INTERVAL_MS) { drain(r); return; }
            lastFrame = now;
            Image img = r.acquireLatestImage();
            if (img == null) return;
            try {
                Bitmap bmp = toBitmap(img);
                listener.onFrame(bmp);
                bmp.recycle();
            } finally { img.close(); }
        }, handler);

        vd = projection.createVirtualDisplay("wg", w, h, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.getSurface(), null, handler);
    }

    private void drain(ImageReader r) {
        Image i = r.acquireLatestImage(); if (i != null) i.close();
    }

    private Bitmap toBitmap(Image img) {
        Image.Plane plane = img.getPlanes()[0];
        int rowStride = plane.getRowStride();
        int pixelStride = plane.getPixelStride();
        int rowPadding = rowStride - pixelStride * w;
        Bitmap bmp = Bitmap.createBitmap(w + rowPadding/pixelStride, h, Bitmap.Config.ARGB_8888);
        bmp.copyPixelsFromBuffer(plane.getBuffer());
        Bitmap cropped = Bitmap.createBitmap(bmp, 0, 0, w, h);
        bmp.recycle();
        return cropped;
    }

    public void stop() {
        if (vd != null) vd.release();
        if (reader != null) reader.close();
        if (thread != null) thread.quitSafely();
    }
}