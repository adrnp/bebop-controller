package edu.stanford.aa122.bebopcontroller.controller;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

import edu.stanford.aa122.bebopcontroller.R;
import edu.stanford.aa122.bebopcontroller.drone.BebopDrone;

/**
 * Class to handle the manual control of the Bebop drone.
 *
 * @author Adrien Perkins <adrienp@stanford.edu>
 */
public class ManualController {

    /** the view containing the manual control elements */
    private View mView;

    /** the drone to control */
    private BebopDrone mBebopDrone;


    /**
     * Constructor for manual controller for the Bebop Drone
     * @param drone the Bebop drone to control
     * @param v the view containing the manual control buttons
     */
    public ManualController(BebopDrone drone, View v) {
        mView = v;
        mBebopDrone = drone;

        setupView();
    }

    /**
     * Setup the view elements.
     */
    private void setupView() {
        // add a motion touch listener to each of the motion controls
        // this listener is responsible for setting the commands for the bebop
        mView.findViewById(R.id.gazUpBt).setOnTouchListener(mMotionTouchListener);
        mView.findViewById(R.id.gazDownBt).setOnTouchListener(mMotionTouchListener);
        mView.findViewById(R.id.yawLeftBt).setOnTouchListener(mMotionTouchListener);
        mView.findViewById(R.id.yawRightBt).setOnTouchListener(mMotionTouchListener);
        mView.findViewById(R.id.forwardBt).setOnTouchListener(mMotionTouchListener);
        mView.findViewById(R.id.backBt).setOnTouchListener(mMotionTouchListener);
        mView.findViewById(R.id.rollLeftBt).setOnTouchListener(mMotionTouchListener);
        mView.findViewById(R.id.rollRightBt).setOnTouchListener(mMotionTouchListener);
    }

    /** touch listener that will send the appropriate commands to the bebop for manual motion */
    private View.OnTouchListener mMotionTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {

            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.setPressed(true);
                    // TODO: add setting to be able adjust the button sensitivity
                    setCommand(view, 50);
                    mBebopDrone.setFlag((byte) 1);
                    break;

                case MotionEvent.ACTION_UP:
                    view.setPressed(false);
                    setCommand(view, 0);
                    mBebopDrone.setFlag((byte) 0);
                    break;

                default:
                    break;
            }

            return true;
        }

        private void setCommand(View v, int magnitude) {
            switch (v.getId()) {
                case R.id.gazUpBt:
                    mBebopDrone.setGaz((byte) magnitude);
                    break;

                case R.id.gazDownBt:
                    mBebopDrone.setGaz((byte) -magnitude);
                    break;

                case R.id.yawLeftBt:
                    mBebopDrone.setYaw((byte) -magnitude);
                    break;

                case R.id.yawRightBt:
                    mBebopDrone.setYaw((byte) magnitude);
                    break;

                case R.id.forwardBt:
                    mBebopDrone.setPitch((byte) magnitude);
                    break;

                case R.id.backBt:
                    mBebopDrone.setPitch((byte) -magnitude);
                    break;

                case R.id.rollLeftBt:
                    mBebopDrone.setRoll((byte) -magnitude);
                    break;

                case R.id.rollRightBt:
                    mBebopDrone.setRoll((byte) magnitude);
                    break;
            }
        }
    };



}
