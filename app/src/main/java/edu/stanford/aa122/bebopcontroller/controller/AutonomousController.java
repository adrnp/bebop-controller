package edu.stanford.aa122.bebopcontroller.controller;


import android.content.Context;
import android.widget.Toast;


import edu.stanford.aa122.bebopcontroller.drone.BebopDrone;
import edu.stanford.aa122.bebopcontroller.listener.AutonomousControllerListener;
import edu.stanford.aa122.bebopcontroller.listener.BebopDroneMissionListener;

/**
 * controller to run an autonomous mission on board a Bebop drone.
 */
public class AutonomousController {

    /** activity context */
    private Context mContext;

    /** connection to the bebop drone */
    private BebopDrone mBebopDrone;

    /** listener to send updates to main activity */
    private AutonomousControllerListener mListener = null;

    /** flag for whether or not the mission should be running */
    private boolean mRunning = false;

    public AutonomousController(Context context, BebopDrone drone) {
        mContext = context;
        mBebopDrone = drone;

        // add another listener to the drone
        mBebopDrone.addMissionListener(new BebopDroneMissionListener() {
            @Override
            public void onCommandFinished() {
                // only call the control drone function if the mission is running
                if (mRunning) {
                    notifyMissionSegmentCompleted();
                    controlDrone();
                }
            }
        });
    }

    /**
     * register a listener for this controller.
     * @param listener the listener to register
     */
    public void registerListener(AutonomousControllerListener listener) {
        mListener = listener;
    }

    /**
     * start running the mission.
     */
    public void startMission() {
        // flag the mission as running
        mRunning = true;

        // initialize the mission
        initializeMission();

        // kick off the mission
        controlDrone();
    }

    /**
     * stop running the mission.
     */
    public void stopMission() {
        mRunning = false;

        // make sure the bebop stops moving!
        if (!mBebopDrone.isLanded()) {
            mBebopDrone.relativeMove(0, 0, 0, 0);
        }

    }

    /**
     * notify the listener of the mission segment completion
     */
    private void notifyMissionSegmentCompleted() {
        if (mListener != null) {
            mListener.onMissionSegmentCompleted();
        }
    }


    // TODO: add variables needed here

    // ----------------------------------- EXAMPLE ------------------------------------------ //
    private int wpIndex = 0;  // to keep track of what should be done next

    // -------------------------------------------------------------------------------------- //


    /**
     * function called when mission start is pressed to make sure that the missin is properly initialized.
     *
     * Note: it is important you put any desired initialize here to ensure proper functionality
     * when you stop a mission and want to be able to restart the mission from the beginning.
     */
    private void initializeMission() {
        // TODO: write your code here for mission initialization

        // ----------------------------------- EXAMPLE ------------------------------------------ //
        wpIndex = 0;  // make sure waypoint index is set to 0 when mission starts

        // -------------------------------------------------------------------------------------- //
    }


    /**
     * function that is called whenever a move is completed by the drone.
     * handles the autonomous control of the Bebop Drone.
     */
    private void controlDrone() {

        // TODO: write your control code here

        // ----------------------------------- EXAMPLE ------------------------------------------ //

        // send a takeoff command if on the ground
        if (mBebopDrone.isLanded()) {
            Toast.makeText(mContext, "Taking Off!", Toast.LENGTH_SHORT).show();
            mBebopDrone.takeOff();
            return;
        }

        Toast.makeText(mContext, "wp index: " + wpIndex, Toast.LENGTH_SHORT).show();

        // send a different command based on the current index of motion
        switch (wpIndex) {
            case 0:
                // move forwards and up
                // see documentation for input details
                // (shortcut: place cursor in 'relativeMove' and press ctrl + space)
                mBebopDrone.relativeMove(10, 0, -2, 0);  // 10m forward, 2 meters up
                break;

            case 1:
                // move backwards
                mBebopDrone.relativeMove(-5, 0, 0, 0);  // 5m backwards
                break;

            case 2:
                // move forwards
                mBebopDrone.relativeMove(1, 0, 0, 0);  // 1m forward
                break;

            case 3:
                // start the video
                //mBebopDrone.startVideo();

                // rotate
                mBebopDrone.relativeMove(0, 0, 0, 90);  // rotate 90 deg
                break;

            case 4:
                // take a picture
                mBebopDrone.takePicture();
                break;

            case 5:
                // land
                mBebopDrone.land();
                break;

            default:
                // reset the waypoint index and stop running the controller
                wpIndex = -1;
                mRunning = false;
                break;
        }

        // increment to the next waypoint index for the next time around
        wpIndex++;

        // -------------------------------------------------------------------------------------- //
    }

}
