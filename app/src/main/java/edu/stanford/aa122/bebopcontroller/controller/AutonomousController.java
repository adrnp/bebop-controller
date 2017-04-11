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
                controlDrone();
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

        // kick off the mission
        controlDrone();
    }

    /**
     * stop running the mission.
     */
    public void stopMission() {
        mRunning = false;
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
     * function that is called whenever a move is completed by the drone.
     * handles the autonomous control of the Bebop Drone.
     */
    private void controlDrone() {
        // if mission is not running, return
        if (!mRunning) {
            return;
        }

        // TODO: write your control code here

        // ----------------------------------- EXAMPLE ------------------------------------------ //

        // send a takeoff command if on the ground
        if (mBebopDrone.isLanded()) {
            Toast.makeText(mContext, "Taking Off!", Toast.LENGTH_LONG).show();
            mBebopDrone.takeOff();
        }

        // send a different command based on the current index of motion
        switch (wpIndex) {
            case 0:
                // example: move forwards
                notifyMissionSegmentCompleted();
                Toast.makeText(mContext, "Moving Forward!", Toast.LENGTH_LONG).show();
                mBebopDrone.relativeMove(5, 0, -2, 0);  // move 10 meters forward and 10 meters up
                break;

            case 1:
                // example: move backwards
                notifyMissionSegmentCompleted();
                Toast.makeText(mContext, "Moving Backward!", Toast.LENGTH_LONG).show();
                mBebopDrone.relativeMove(-2, 0, 0, 0); // move 10 meters backwards
                break;

            case 2:
                // example: land
                notifyMissionSegmentCompleted();
                Toast.makeText(mContext, "Landing!", Toast.LENGTH_LONG).show();
                mBebopDrone.land();
                break;

            default:
                // go back to manual control
                wpIndex = -1;
                break;
        }

        // increment to the next waypoint index for the next time around
        wpIndex++;

        // -------------------------------------------------------------------------------------- //
    }

}
