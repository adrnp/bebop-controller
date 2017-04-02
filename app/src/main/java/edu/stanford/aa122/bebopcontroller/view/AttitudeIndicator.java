package edu.stanford.aa122.bebopcontroller.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;


/**
 * Attitude Indicator View
 * Displays a specific roll/pitch/yaw configuration visually
 *
 * adapted from https://github.com/DroidPlanner/Tower
 *
 * @author Adrien Perkins <adrienp@stanford.edu>
 */
public class AttitudeIndicator extends View {

    private static final float INTERNAL_RADIUS = 0.85f;
    private static final float YAW_ARROW_SIZE = 1.2f;
    private static final float PITCH_TICK_LINE_LENGTH = 0.4f;
    private static final int PITCH_RANGE = 45;
    private static final int PITCH_TICK_SPACING = 15;
    private static final int PITCH_TICK_PADDING = 2;
    private static final float PLANE_SIZE = 0.8f;
    private static final float PLANE_BODY_SIZE = 0.2f;
    private static final float PLANE_WING_WIDTH = 5f;

    private float halfWidth;
    private float halfHeight;
    private float radiusInternal;
    private RectF externalBounds;


    private Paint planePaint;
    private Paint planeFinPaint;
    private Paint planeCenterPaint;
    private Paint rollPaint;
    private Paint tickPaint;
    private Paint horizonTickPaint;
    private Paint yawTextPaint;


    private float yaw, roll, pitch;

    /**
     * Constructor for view
     * @param context context
     * @param attrs atributes for the view
     */
    public AttitudeIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
        setAttitude(-30, 20, 0);
    }

    /**
     * initialize all the paints and stuff to be used
     */
    private void initialize() {

        Paint fillPaint = new Paint();
        fillPaint.setAntiAlias(true);
        fillPaint.setStyle(Paint.Style.FILL);

        planePaint = new Paint(fillPaint);
        planePaint.setColor(Color.WHITE);
        planePaint.setStrokeWidth(PLANE_WING_WIDTH);
        planePaint.setStrokeCap(Paint.Cap.ROUND);
        planeCenterPaint = new Paint(planePaint);
        planeCenterPaint.setColor(Color.RED);
        planeFinPaint = new Paint(planePaint);
        planeFinPaint.setStrokeWidth(PLANE_WING_WIDTH / 2f);

        tickPaint = new Paint(fillPaint);
        tickPaint.setColor(Color.parseColor("#88ffffff"));
        tickPaint.setStrokeWidth(2f);

        horizonTickPaint = new Paint(fillPaint);
        horizonTickPaint.setColor(Color.parseColor("#88ffffff"));
        horizonTickPaint.setStrokeWidth(4f);

        rollPaint = new Paint();
        rollPaint.setAntiAlias(true);
        rollPaint.setStyle(Paint.Style.STROKE);
        rollPaint.setColor(Color.WHITE);
        rollPaint.setStrokeWidth(5f);

        yawTextPaint = new Paint(fillPaint);
        yawTextPaint.setTextSize(50f);
        yawTextPaint.setColor(Color.WHITE);
        yawTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        halfHeight = h / 2f;
        halfWidth = w / 2f;

        float radiusExternal = Math.min(halfHeight, halfWidth) / YAW_ARROW_SIZE;
        radiusInternal = radiusExternal * INTERNAL_RADIUS;
        externalBounds = new RectF(-radiusExternal, -radiusExternal, radiusExternal, radiusExternal);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.translate(halfWidth, halfHeight);
        drawRoll(canvas);
        drawPitch(canvas);
        drawYaw(canvas);
        drawPlane(canvas);
    }

    /**
     * draw partial outer arcs to represent roll motion
     * @param canvas the canvas to draw on
     */
    private void drawRoll(Canvas canvas) {
        canvas.drawArc(externalBounds, 135f - roll, 90f, false, rollPaint);
        canvas.drawArc(externalBounds, 315f - roll, 90f, false, rollPaint);
    }

    /**
     * draw the ticks to represent the pitch
     * @param canvas the canvas to draw on
     */
    private void drawPitch(Canvas canvas) {

        // coordinates for the start and end of the line - (0,0) is center of canvas
        float lineX = (float) (Math.cos(Math.toRadians(-roll)) * radiusInternal) * PITCH_TICK_LINE_LENGTH;
        float lineY = (float) (Math.sin(Math.toRadians(-roll)) * radiusInternal) * PITCH_TICK_LINE_LENGTH;

        // offset from one line to another
        float dx = (float) (Math.cos(Math.toRadians(-roll - 90))*radiusInternal / PITCH_RANGE);
        float dy = (float) (Math.sin(Math.toRadians(-roll - 90))*radiusInternal / PITCH_RANGE);

        // determine the range of ticks to put on (e.g. 0 = horizon, -1 = -15 degree, etc)
        int i = (int) ((pitch - PITCH_RANGE + PITCH_TICK_PADDING) / PITCH_TICK_SPACING);
        int loopEnd = (int) ((pitch + PITCH_RANGE - PITCH_TICK_PADDING) / PITCH_TICK_SPACING);

        // paint the different lines
        for (; i <= loopEnd; i++) {
            float degree = -pitch + i*PITCH_TICK_SPACING;
            float linedx = dx*degree;
            float linedy = dy*degree;
            if (i == 0) {
                canvas.drawLine(2.0f*lineX + linedx, 2.0f*lineY + linedy, 2.0f*-lineX + linedx, 2.0f*-lineY + linedy, horizonTickPaint);
            } else {
                canvas.drawLine(lineX + linedx, lineY + linedy, -lineX + linedx, -lineY + linedy, tickPaint);
            }
        }
    }

    /**
     * write the heading on the HUD
     * @param canvas the canvas to draw on
     */
    private void drawYaw(Canvas canvas) {
        canvas.drawText(String.format(Locale.US, "%03d\u00B0", (int)yaw), 0, -radiusInternal, yawTextPaint);
    }

    /**
     * draw a little plane silhouette for orientation referencing
     * @param canvas the canvas to draw on
     */
    private void drawPlane(Canvas canvas) {
        canvas.drawLine(radiusInternal*PLANE_SIZE, 0, -radiusInternal*PLANE_SIZE, 0, planePaint);
        canvas.drawLine(0, 0, 0, -radiusInternal*PLANE_SIZE*5/12, planeFinPaint);
        canvas.drawCircle(0, 0, radiusInternal*PLANE_SIZE*PLANE_BODY_SIZE, planePaint);
        canvas.drawCircle(0, 0, radiusInternal*PLANE_SIZE*PLANE_BODY_SIZE / 2f, planeCenterPaint);
    }

    /**
     * set the attitude to be displayed by the HUD
     * @param roll roll angle in degrees
     * @param pitch pitch angle in degrees
     * @param yaw yaw angle in degrees
     */
    public void setAttitude(float roll, float pitch, float yaw) {
        this.roll = roll;
        this.pitch = pitch;
        this.yaw = (yaw+360) % 360;
        invalidate();
    }
}
