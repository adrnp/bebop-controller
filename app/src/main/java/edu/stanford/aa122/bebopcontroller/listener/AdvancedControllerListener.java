package edu.stanford.aa122.bebopcontroller.listener;

/**
 * listener for the advanced controller state information.
 *
 * @author Adrien Perkins <adrienp@stanford.edu>
 */
public interface AdvancedControllerListener {

    /**
     * called when the waypoint index is changed.
     * @param wp the current waypoint index.
     */
    void onWaypointIndexChanged(int wp);

    /**
     * called when the running state of the controller changes.
     * @param running true if the main loop is currently running.
     */
    void onRunningStateChanged(boolean running);
}
