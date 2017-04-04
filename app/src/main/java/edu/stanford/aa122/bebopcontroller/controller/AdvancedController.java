package edu.stanford.aa122.bebopcontroller.controller;

import android.location.Location;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;

import edu.stanford.aa122.bebopcontroller.drone.BebopDrone;
import edu.stanford.aa122.bebopcontroller.listener.AdvancedControllerListener;

/**
 * Class to handle advanced autonomous control of the Bebop drone.
 *
 * This is most likely the class that students in the class will be editing
 *
 * @author Adrien Perkins <adrienp@stanford.edu>
 */
public class AdvancedController {

    /** rate at which the control loop should be running */
    private static int LOOP_RATE = 5;  // [Hz]

    /** the drone to control */
    private BebopDrone mBebopDrone;

    /** the thread that will be running the control loop */
    private Thread mThread = null;

    /** the registered listener to notify changes through */
    private AdvancedControllerListener mListener;

    /**
     * constructor for advanced controller
     * @param drone BebopDrone that will be controlled
     */
    public AdvancedController(BebopDrone drone) {
        // TODO: decide if this needs to also be a listener or is there a way to be able to see when a move it done without being a listener
        mBebopDrone = drone;
    }

    /**
     * register a listener for this controller information
     * @param listener listener to register
     */
    public void registerControllerListener(AdvancedControllerListener listener) {
        mListener = listener;
    }

    /**
     * trigger the mission to begin
     */
    public void startMission() {
        // stop an existing thread, if there is one running
        if (mThread != null && mThread.isAlive()) {
            mThread.interrupt();
        }

        // start a new mission
        mThread = new Thread(mainLoop);
        mThread.start();
    }

    /**
     * trigger the mission to be completed
     */
    public void stopMission() {
        if (mThread != null && mThread.isAlive()) {
            mThread.interrupt();
        }
    }


    /** main runnable thread containing the main control loop */
    private Runnable mainLoop = new Runnable() {

        // TODO: add any classes, functions, global constants, etc you want to use here

        // example: waypoint class
        class Waypoint {
            Location gpsPos;
            int action;

            public Waypoint(Location pos, int action) {
                gpsPos = pos;
                this.action = action;
            }
        }

        /**
         * determine if the bebop has successfully taken off and is therefore ready to fly
         * @return true if ready to execute flight elements of a mission
         */
        private boolean isReadyToFly() {
            return (mBebopDrone.getFlyingState() == ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING ||
                    mBebopDrone.getFlyingState() == ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING);
        }


        @Override
        public void run() {

            // notify loop is now running
            if (mListener != null) {
                mListener.onRunningStateChanged(true);
            }

            // timestamp of last loop run - needed for loop being at a specific frequency
            long lastLoopTime = System.currentTimeMillis();

            // waypoint index (example)
            int wpIndex = 0;

            // whether or not done with the autonomous mission
            boolean finished = false;

            // this is the main control loop
            while (!Thread.interrupted() && !finished) {

                // ensure that the loop runs at the proper rate
                if ((System.currentTimeMillis() - lastLoopTime) < 1000.0/LOOP_RATE) {
                    continue;
                }
                lastLoopTime = System.currentTimeMillis();


                // TODO: write your code here

                // this is an example

                // takeoff if needed (if the current state is landed)
                if (mBebopDrone.getFlyingState() == ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED) {
                    mBebopDrone.takeOff();
                }

                // if the bebop has finished the last command sent to it, then execute the next command
                if (isReadyToFly() && mBebopDrone.finishedLastCommand()) {

                    switch (wpIndex) {
                        case 0:
                            // example: move forwards
                            mBebopDrone.relativeMove(10, 0, -10, 0);  // move 10 meters forward and 10 meters up
                            break;

                        case 1:
                            // example: to make a flip
                            mBebopDrone.flip(ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM.ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_FRONT);
                            break;

                        case 2:
                            // example: move backwards
                            mBebopDrone.relativeMove(-10, 0, 0, 0); // move 10 meters backwards
                            break;

                        case 3:
                            // example: land
                            mBebopDrone.land();
                            break;

                        default:
                            // exit out of the loop
                            finished = true;

                            // notify loop is no longer running
                            if (mListener != null) {
                                mListener.onRunningStateChanged(false);
                            }
                            break;
                    }

                    // increment the waypoint index as needed
                    wpIndex++;

                    // notify of waypoint update
                    if (mListener != null) {
                        mListener.onWaypointIndexChanged(wpIndex);
                    }
                }

            }
        }
    };


}
