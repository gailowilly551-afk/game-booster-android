package com.gamebooster.app.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * High-performance full-screen touch-through canvas view for rendering ESP overlays.
 * Draws bounding box, entity name, health bar, and distance indicators.
 */
public class EspOverlayView extends View {

    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint healthBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint healthBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint snaplinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<EspEntity> entities = new ArrayList<>();

    private boolean showBox = true;
    private boolean showName = true;
    private boolean showHealth = true;
    private boolean showDistance = true;
    private boolean showSnapline = false;
    private int customColor = Color.rgb(0, 255, 200); // Default Cyan

    public EspOverlayView(Context context) {
        super(context);
        init();
    }

    public EspOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(3.0f);
        boxPaint.setColor(customColor);

        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(26.0f);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setShadowLayer(4.0f, 1.0f, 1.0f, Color.BLACK);

        healthBgPaint.setStyle(Paint.Style.FILL);
        healthBgPaint.setColor(Color.argb(180, 40, 40, 40));

        healthBarPaint.setStyle(Paint.Style.FILL);
        healthBarPaint.setColor(Color.GREEN);

        snaplinePaint.setStyle(Paint.Style.STROKE);
        snaplinePaint.setStrokeWidth(1.5f);
        snaplinePaint.setColor(Color.argb(140, 0, 255, 200));
    }

    public void setOptions(boolean box, boolean name, boolean health, boolean distance, boolean snapline, int color) {
        this.showBox = box;
        this.showName = name;
        this.showHealth = health;
        this.showDistance = distance;
        this.showSnapline = snapline;
        this.customColor = color;
        this.boxPaint.setColor(color);
        this.snaplinePaint.setColor(Color.argb(140, Color.red(color), Color.green(color), Color.blue(color)));
        postInvalidate();
    }

    public void updateEntities(List<EspEntity> newEntities) {
        synchronized (entities) {
            entities.clear();
            if (newEntities != null) {
                entities.addAll(newEntities);
            }
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (canvas == null) return;

        float screenW = getWidth();
        float screenH = getHeight();

        synchronized (entities) {
            for (EspEntity e : entities) {
                if (e == null) continue;

                float left = e.x - (e.width / 2.0f);
                float top = e.y - e.height;
                float right = e.x + (e.width / 2.0f);
                float bottom = e.y;

                // 1. Draw Snapline from bottom center of screen
                if (showSnapline) {
                    canvas.drawLine(screenW / 2.0f, screenH, e.x, bottom, snaplinePaint);
                }

                // 2. Draw 2D ESP Box
                if (showBox) {
                    int boxColor = (e.color != 0) ? e.color : customColor;
                    boxPaint.setColor(boxColor);
                    canvas.drawRect(left, top, right, bottom, boxPaint);
                }

                // 3. Draw Health Bar on left side of box
                if (showHealth) {
                    float barWidth = 6.0f;
                    float barMargin = 4.0f;
                    float barLeft = left - barMargin - barWidth;
                    float barRight = left - barMargin;

                    // Background slot
                    canvas.drawRect(barLeft, top, barRight, bottom, healthBgPaint);

                    // Health fill (Green -> Yellow -> Red)
                    float hpPct = e.getHealthPercentage();
                    float hpTop = bottom - (e.height * hpPct);

                    if (hpPct > 0.5f) {
                        healthBarPaint.setColor(Color.GREEN);
                    } else if (hpPct > 0.2f) {
                        healthBarPaint.setColor(Color.YELLOW);
                    } else {
                        healthBarPaint.setColor(Color.RED);
                    }
                    canvas.drawRect(barLeft, hpTop, barRight, bottom, healthBarPaint);
                }

                // 4. Draw Entity Name above box
                if (showName && e.name != null && !e.name.isEmpty()) {
                    canvas.drawText(e.name, e.x, top - 8.0f, textPaint);
                }

                // 5. Draw Distance below box
                if (showDistance) {
                    String distText = String.format("%.0fm", e.distance);
                    canvas.drawText(distText, e.x, bottom + 26.0f, textPaint);
                }
            }
        }
    }
}
