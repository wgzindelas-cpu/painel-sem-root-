package com.wg.ffh4x;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.WindowManager;
import org.json.JSONObject;

public class OverlayView {
    private final Context ctx;
    private final View view;
    private final WindowManager wm;
    private volatile float[][] boxes = new float[0][];
    private volatile int fov = 200;
    private volatile boolean showFov = true;

    public OverlayView(Context ctx) {
        this.ctx = ctx;
        wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        view = new View(ctx) {
            private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas c) {
                super.onDraw(c);
                try {
                    JSONObject cfg = new JSONObject(EspService.getConfig());
                    fov = cfg.optInt("fov", 200);
                    showFov = cfg.optBoolean("fovCircle", true);
                } catch (Exception ignored) {}

                // ESP caixas
                if (boxes != null) {
                    p.setColor(0xFF00E0A4);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(4f);
                    for (float[] b : boxes) c.drawRect(b[0], b[1], b[0]+b[2], b[1]+b[3], p);
                }

                // FOV círculo
                if (showFov) {
                    p.setColor(0x88FFD400);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(3f);
                    float cx = c.getWidth()/2f, cy = c.getHeight()/2f;
                    c.drawCircle(cx, cy, fov, p);
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(0xAAFFD400);
                    c.drawCircle(cx, cy, 6, p);
                }
                postInvalidateOnAnimation();
            }
        };
    }

    public void show() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        wm.addView(view, lp);
    }

    public void updateBoxes(float[][] b) { boxes = b; view.postInvalidate(); }
    public void hide() {
        try { if (view.getParent() != null) wm.removeView(view); } catch (Throwable ignored) {}
    }
}