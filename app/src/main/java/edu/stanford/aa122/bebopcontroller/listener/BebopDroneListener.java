package edu.stanford.aa122.bebopcontroller.listener;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;

import java.util.Date;

/**
 * interface for listening to Bebop Drone events
 * based on Parrot SDK Sample code
 *
 * @author Adrien Perkins <adrienp@stanford.edu>
 */
public interface BebopDroneListener {
    /**
     * Called when the connection to the drone changes
     * Called in the main thread
     * @param state the state of the drone
     */
    void onDroneConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state);

    /**
     * Called when the battery charge changes
     * Called in the main thread
     * @param timestamp the phone timestamp for the time this measurement came in
     * @param batteryPercentage the battery remaining (in percent)
     */
    void onBatteryChargeChanged(Date timestamp, int batteryPercentage);

    /**
     * Called when the piloting state changes
     * Called in the main thread
     * @param timestamp the phone timestamp for the time this measurement came in
     * @param state the piloting state of the drone
     */
    void onPilotingStateChanged(Date timestamp, ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state);

    /**
     * Called when the GPS position changes
     * @param timestamp the phone timestamp for the time this measurement came in
     * @param lat latitude in decimal degrees
     * @param lon longitude in decimal degrees
     * @param alt altitude in meters above sea level
     */
    void onPositionChanged(Date timestamp, double lat, double lon, double alt);

    /**
     * Called when the speed changes
     * @param timestamp the phone timestamp for the time this measurement came in
     * @param vx north component velocity [m/s]
     * @param vy east component velocity [m/s]
     * @param vz down component velocity [m/s]
     */
    void onSpeedChanged(Date timestamp, float vx, float vy, float vz);

    /**
     * Called when the attitude changes
     * @param timestamp the phone timestamp for the time this measurement came in
     * @param roll roll [deg]
     * @param pitch pitch [deg]
     * @param yaw yaw [deg]
     */
    void onAttitudeChanged(Date timestamp, float roll, float pitch, float yaw);

    /**
     * Called when the relative altitude changes
     * @param timestamp the phome timestamp for the time this measurement came in
     * @param alt the altitude above the home location [m]
     */
    void onRelativeAltitudeChanged(Date timestamp, double alt);

        /*
        // TODO: probably change this to a waypoint completion broadcast...
        void onRelativeMoveEnded();
        */

    /**
     * Called when a picture is taken
     * Called on a separate thread
     * @param timestamp the phone timestamp for the time this measurement came in
     * @param error ERROR_OK if picture has been taken, otherwise describe the error
     */
    void onPictureTaken(Date timestamp, ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error);

    /**
     * Called when the video decoder should be configured
     * Called on a separate thread
     * @param codec the codec to configure the decoder with
     */
    void configureDecoder(ARControllerCodec codec);

    /**
     * Called when a video frame has been received
     * Called on a separate thread
     * @param frame the video frame
     */
    void onFrameReceived(ARFrame frame);

    /**
     * Called before medias will be downloaded
     * Called in the main thread
     * @param nbMedias the number of medias that will be downloaded
     */
    void onMatchingMediasFound(int nbMedias);

    /**
     * Called each time the progress of a download changes
     * Called in the main thread
     * @param mediaName the name of the media
     * @param progress the progress of its download (from 0 to 100)
     */
    void onDownloadProgressed(String mediaName, int progress);

    /**
     * Called when a media download has ended
     * Called in the main thread
     * @param mediaName the name of the media
     */
    void onDownloadComplete(String mediaName);
}
