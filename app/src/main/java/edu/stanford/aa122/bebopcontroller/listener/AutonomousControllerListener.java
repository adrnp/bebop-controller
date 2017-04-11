package edu.stanford.aa122.bebopcontroller.listener;

/**
 * Listener for the autonomous controller.
 *
 * Allows for communication between the autonomous controller and the main Activity.
 */
public interface AutonomousControllerListener {

    /**
     * called when one of the mission segments is completed.
     */
    void onMissionSegmentCompleted();
}
