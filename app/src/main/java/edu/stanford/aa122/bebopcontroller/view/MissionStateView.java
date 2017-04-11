package edu.stanford.aa122.bebopcontroller.view;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import edu.stanford.aa122.bebopcontroller.R;

/**
 * View to display the current mission state.
 *
 * @author Adrien Perkins <adrienp@stanford.edu>
 */
public class MissionStateView extends View {

    /** width of the mission pogress circle */
    private static final float STROKE_WIDTH = 40f;

    /** angle buffer to display between progress elements */
    private static final float ANGLE_BUFFER_OFFSET = 2;

    /** angle through which to sweep for an progress element */
    private static final float ANGLE_SWEEP = 45f - ANGLE_BUFFER_OFFSET;

    private static final int MISSION_STATE_TAKEOFF = 1;
    private static final int MISSION_STATE_MOVE_TO_1 = 2;
    private static final int MISSION_STATE_FLIP = 3;
    private static final int MISSION_STATE_MOVE_TO_2 = 4;
    private static final int MISSION_STATE_PICTURE = 5;
    private static final int MISSION_STATE_MOVE_TO_3 = 6;
    private static final int MISSION_STATE_DRONIE = 7;
    private static final int MISSION_STATE_LANDED = 8;


    // Paints to be used
    private Paint mArcBasePaint;
    private Paint mArcProgressPaint;
    private Paint mVideoPaint;

    // useful dimensions
    private float mCenterX;
    private float mCenterY;
    private float mRadius;
    private RectF mExternalRect;

    // state information
    private boolean mVideoOn = false;
    private int mMissionState = 0;

    // start angles for the progress elements
    private float[] mStartAngles = {270, 315, 0, 45, 90, 135, 180, 225};


    public MissionStateView(Context context) {
        super(context);
        initialize();
    }

    public MissionStateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    /**
     * initialize all the paints and stuff to be used
     */
    private void initialize() {

        // base for the arcs
        Paint strokePaint = new Paint();
        strokePaint.setAntiAlias(true);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(STROKE_WIDTH);

        mArcBasePaint = new Paint(strokePaint);
        mArcBasePaint.setColor(Color.parseColor("#88FFFFFF"));

        mArcProgressPaint = new Paint(strokePaint);
        mArcProgressPaint.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));

        mVideoPaint = new Paint();
        mVideoPaint.setAntiAlias(true);
        mVideoPaint.setStyle(Paint.Style.FILL);
        mVideoPaint.setColor(Color.RED);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int d = Math.min(w, h);
        mRadius = (int) (d / 2 * 0.75);

        // get the center of the view
        mCenterX = w / 2f;
        mCenterY = h / 2f;

        mExternalRect = new RectF(-mRadius, -mRadius, mRadius, mRadius);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.translate(mCenterX, mCenterY);
        drawProgress(canvas);
        drawVideo(canvas);
    }


    /**
     * draw the mission progress bar (8 increments)
     * @param canvas canvas to draw arc on
     */
    private void drawProgress(Canvas canvas) {

        // paint the progress elements
        for (int i = 0; i < mMissionState; i++) {
            canvas.drawArc(mExternalRect, mStartAngles[i] + ANGLE_BUFFER_OFFSET, ANGLE_SWEEP, false, mArcProgressPaint);
        }

        // paint the base markers for showing the number of possible progress steps
        for (int i = mMissionState; i < 8; i++) {
            canvas.drawArc(mExternalRect, mStartAngles[i] + ANGLE_BUFFER_OFFSET, ANGLE_SWEEP, false, mArcBasePaint);
        }

    }


    /**
     * draw circle to display when the video is running (or a picture is being taken)
     * @param canvas canvas to draw on
     */
    private void drawVideo(Canvas canvas) {
        if (mVideoOn) {
            // painting the move button
            canvas.drawCircle(0, 0, 0.5f*mRadius, mVideoPaint);
        }
    }


    /**
     * set a specific mission state (value between 0 and 8)
     * 0 - not started
     * 8 - completed
     * @param missionState the mission state to set
     */
    public void setMissionState(int missionState) {
        // this is invalid, so just return
        if (missionState > 8 || missionState < 0) {
            return;
        }
        mMissionState = missionState;
        invalidate();
    }

    /**
     * increments the mission state
     */
    public void nextMissionState() {
        // increase to the next mission state
        mMissionState++;

        // make sure to not exceed the bounds of the mission state
        if (mMissionState > 8) {
            mMissionState = 8;
        }
        invalidate();
    }


    /**
     * mark the video recording as having been started
     */
    public void startVideo() {
        mVideoOn = true;
        invalidate();
    }


    /**
     * mark the video recording as having been finished
     */
    public void stopVideo() {
        mVideoOn = false;
        invalidate();
    }
}
