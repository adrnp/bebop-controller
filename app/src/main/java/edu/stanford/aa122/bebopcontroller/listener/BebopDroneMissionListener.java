package edu.stanford.aa122.bebopcontroller.listener;

/**
 * a simple listener to only listen to the mission state changes of the drone.
 */
public interface BebopDroneMissionListener {

    /**
     * called when the last command to the drone has been completed.
     */
    void onCommandFinished();

}
