package edu.stanford.aa122.bebopcontroller.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import edu.stanford.aa122.bebopcontroller.R;

/**
 * joystick adapted from https://github.com/zerokol/JoystickView
 *
 * @author Adrien Perkins <adrienp@stanford.edu>
 */
public class JoystickView extends View implements Runnable {

    // Constants

    /** default interval between loop updates [ms] */
    public final static long DEFAULT_LOOP_INTERVAL = 100;

    /** default maximum control value */
    public final static int DEFAULT_MAX_CONTROL = 50;

    /** default deadband for joystick [% of travel] */
    public final static int DEFAULT_DEADBAND = 5;

    // configurable parameters

    /** loop interval for control updates */
    private long mLoopInterval = DEFAULT_LOOP_INTERVAL;

    /** maximum control value */
    private int mMaxXControl = DEFAULT_MAX_CONTROL;
    private int mMaxYControl = DEFAULT_MAX_CONTROL;

    /** joystick deadband */
    private int mDeadband = DEFAULT_DEADBAND;


    // Variables

    /** listener for joystick movement */
    private OnJoystickMoveListener mListener;

    /** thread for updating listener when joystick is moving */
    private Thread mThread = new Thread(this);

    /** current button x position */
    private float mPositionX = 0;

    /** current button y position */
    private float mPositionY = 0;

    /** X coordinate of the center of the view */
    private float mCenterX = 0;

    /** Y coordinate of the center of the view */
    private float mCenterY = 0;

    /** paint to be used for the main joystick circle */
    private Paint mMainCircle;

    /** paint to be used for the button */
    private Paint mButton;

    /** radius of the joystick */
    private int mJoystickRadius;

    /** radius of the button */
    private int mButtonRadius;

    /** forward direction icon */
    private Bitmap mBitmapForward = null;

    /** backward direction icon */
    private Bitmap mBitmapBackward = null;

    /** left direction icon */
    private Bitmap mBitmapLeft = null;

    /** right direction icon */
    private Bitmap mBitmapRight = null;

    /** matrix to help with displaying icons on joystick */
    private Matrix mMatrix = new Matrix();

    /**
     * interface for a listener for joystick motion
     */
    public interface OnJoystickMoveListener {
        /**
         * called when a user changes the control input on the joystick
         * @param x control in the X direction from 0 to MAX_POWER
         * @param y control in the Y direction from 0 to MAX_POWER
         */
        public void onControlChanged(int x, int y);
    }

    public JoystickView(Context context) {
        super(context);
    }

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initJoystickView();

        // get any image resources that might have been given
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.JoystickView, 0, 0);
        try {
            BitmapDrawable drawable = (BitmapDrawable) a.getDrawable(R.styleable.JoystickView_drawableForward);
            if (drawable != null) {
                mBitmapForward = drawable.getBitmap();
            }

            drawable = (BitmapDrawable) a.getDrawable(R.styleable.JoystickView_drawableBackward);
            if (drawable != null) {
                mBitmapBackward = drawable.getBitmap();
            }

            drawable = (BitmapDrawable) a.getDrawable(R.styleable.JoystickView_drawableLeft);
            if (drawable != null) {
                mBitmapLeft = drawable.getBitmap();
            }

            drawable = (BitmapDrawable) a.getDrawable(R.styleable.JoystickView_drawableRight);
            if (drawable != null) {
                mBitmapRight = drawable.getBitmap();
            }
        } finally {
            a.recycle();
        }

    }

    public JoystickView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        initJoystickView();
    }

    /**
     * set a listener for joystick movement
     * @param listener listener for movement
     */
    public void setOnJoystickMoveListener(OnJoystickMoveListener listener) {
        this.mListener = listener;
    }

    /**
     * set the time between listener updates when joystick is moving
     * @param interval wait interval in ms
     */
    public void setJoystickUpdateInterval(long interval) {
        mLoopInterval = interval;
    }

    /**
     * set the max x axis control for user control
     * @param maxControl integer max desired control (between 0 and 100)
     */
    public void setJoystickMaxXControl(int maxControl) {
        mMaxXControl = maxControl;
    }

    /**
     * set the max y axis control for user control
     * @param maxControl integer max desired control (between 0 and 100)
     */
    public void setJoystickMaxYControl(int maxControl) {
        mMaxYControl = maxControl;
    }

    /**
     * set the percentage of travel to be considered the deadband (region of no control)
     * @param deadband integer percentage of no control region
     */
    public void setJoystickDeadband(int deadband) {
        mDeadband = deadband;
    }

    /**
     * initialize the paint elements needed for drawing the joystick
     */
    protected void initJoystickView() {
        mMainCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMainCircle.setColor(Color.parseColor("#88FFFFFF"));
        mMainCircle.setStyle(Paint.Style.FILL);

        mButton = new Paint(Paint.ANTI_ALIAS_FLAG);
        mButton.setColor(Color.parseColor("#AAFFFFFF"));
        mButton.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        super.onSizeChanged(xNew, yNew, xOld, yOld);

        mPositionX = (getWidth()/2f);
        mPositionY = (getWidth()/2f);
        int d = Math.min(xNew, yNew);
        mButtonRadius = (int) (d / 2 * 0.25);
        mJoystickRadius = (int) (d / 2 * 0.75);

        // get the center of the view
        mCenterX = (getWidth()/2f);
        mCenterY = (getHeight()/2f);
    }

    /**
     * draw a given bitmap centered at a specific location on the canvas.
     * @param canvas canvas to draw on
     * @param bitmap bitmap of the image to draw
     * @param x x position of the center of the image
     * @param y y position of the center of the image
     */
    private void drawIcon(Canvas canvas, Bitmap bitmap, float x, float y) {
        if (bitmap != null) {
            mMatrix.reset();
            mMatrix.postTranslate(-bitmap.getWidth()/2f, -bitmap.getHeight()/2f); // Centers image
            mMatrix.postTranslate(x, y);
            canvas.drawBitmap(bitmap, mMatrix, null);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // painting the main circle
        canvas.drawCircle((int) mCenterX, (int) mCenterY, mJoystickRadius, mMainCircle);

        // painting the move button
        canvas.drawCircle(mPositionX, mPositionY, mButtonRadius, mButton);

        // add any of the icons as needed
        drawIcon(canvas, mBitmapForward, mCenterX, mCenterY/2f);
        drawIcon(canvas, mBitmapBackward, mCenterX, 3f*mCenterY/2f);
        drawIcon(canvas, mBitmapLeft, mCenterX/2f, mCenterY);
        drawIcon(canvas, mBitmapRight, 3f*mCenterX/2f, mCenterY);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mPositionX = event.getX();
        mPositionY = event.getY();

        // limit the position of the button to be within the joystick radius
        float abs = (float) Math.sqrt((mPositionX - mCenterX)*(mPositionX - mCenterX) + (mPositionY - mCenterY)*(mPositionY - mCenterY));
        if (abs > mJoystickRadius) {
            mPositionX = ((mPositionX - mCenterX)*mJoystickRadius/abs + mCenterX);
            mPositionY = ((mPositionY - mCenterY)*mJoystickRadius/abs + mCenterY);
        }
        invalidate();

        // snap the button back to center when released
        if (event.getAction() == MotionEvent.ACTION_UP) {
            mPositionX = mCenterX;
            mPositionY = mCenterY;
            mThread.interrupt();
            updateListener();
        }

        // start updating the listener if the button is moving
        if (mListener != null && event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mThread != null && mThread.isAlive()) {
                mThread.interrupt();
            }
            mThread = new Thread(this);
            mThread.start();
            updateListener();
        }
        return true;
    }

    /**
     * get the amount of control the user has input in the X direction
     * @return integer control between 0 and MAX_POWER
     */
    private int getXControl() {
        // calculate the amount of travel for the deadband
        double deadbandTravel = mJoystickRadius*mDeadband/100.0f;

        // get the amount of control being inputted
        double travel = (mPositionX - mCenterX);

        // if within the deadband, return 0, as we are ignoring this region
        if (Math.abs(travel) < deadbandTravel) {
            return 0;
        }

        // just do some sign changes as needed
        if (travel < 0) {
            deadbandTravel *= -1;
        }

        // return the control based on the travel between the deadband and max travel
        return (int) (mMaxXControl*(travel - deadbandTravel)/(mJoystickRadius - deadbandTravel));
    }

    /**
     * get the amount of control the uer has input in the Y direction
     * @return integer control between 0 and MAX_POWER
     */
    private int getYControl() {
        // calculate the amount of travel for the deadband
        double deadbandTravel = mJoystickRadius*mDeadband/100.0f;

        // get the amount of control being inputted
        double travel = (mPositionY - mCenterY);

        // if within the deadband, return 0, as we are ignoring this region
        if (Math.abs(travel) < deadbandTravel) {
            return 0;
        }

        // just do some sign changes as needed
        if (travel < 0) {
            deadbandTravel *= -1;
        }

        // return the control based on the travel between the deadband and max travel
        return (int) (mMaxYControl*(travel - deadbandTravel)/(mJoystickRadius - deadbandTravel));
    }

    /**
     * update the listener with the new control positions.
     */
    private void updateListener() {
        if (mListener != null) {
            mListener.onControlChanged(getXControl(), getYControl());
        }
    }


    @Override
    public void run() {
        while (!Thread.interrupted()) {
            post(new Runnable() {
                public void run() {
                    updateListener();
                }
            });

            try {
                Thread.sleep(mLoopInterval);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
