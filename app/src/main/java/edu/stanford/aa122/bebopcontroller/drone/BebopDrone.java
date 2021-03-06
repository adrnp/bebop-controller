package edu.stanford.aa122.bebopcontroller.drone;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_VIDEOEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_VIDEOEVENTCHANGED_EVENT_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDSTATE_PICTURESTATECHANGEDV2_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDSTATE_PICTURESTATECHANGEDV2_STATE_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDSTATE_VIDEORESOLUTIONSTATE_RECORDING_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDSTATE_VIDEOSTATECHANGEDV2_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDSTATE_VIDEOSTATECHANGEDV2_STATE_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORD_VIDEOV2_RECORD_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGEVENT_MOVEBYEND_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerArgumentDictionary;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARControllerDictionary;
import com.parrot.arsdk.arcontroller.ARControllerException;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARDeviceControllerListener;
import com.parrot.arsdk.arcontroller.ARDeviceControllerStreamListener;
import com.parrot.arsdk.arcontroller.ARFeatureARDrone3;
import com.parrot.arsdk.arcontroller.ARFeatureCommon;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_FAMILY_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDevice;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceNetService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryException;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.arutils.ARUtilsException;
import com.parrot.arsdk.arutils.ARUtilsManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.stanford.aa122.bebopcontroller.fragment.BebopPreferenceFragment;
import edu.stanford.aa122.bebopcontroller.helpers.AttitudeVector;
import edu.stanford.aa122.bebopcontroller.helpers.VelocityVector;
import edu.stanford.aa122.bebopcontroller.listener.BebopDroneListener;
import edu.stanford.aa122.bebopcontroller.listener.BebopDroneMissionListener;


/**
 * Helper class for handling the interaction with the Bebop Drone.
 *
 * Modified from Parrot SDK Samples
 *
 * @author Adrien Perkins <adrienp@stanford.edu>
 */
public class BebopDrone {

    /** tag for debugging */
    private static final String TAG = "BebopDrone";

    /** port number for downloading images */
    private static final int DEVICE_PORT = 21;

    /** list of listeners configured to listener to Bebop events */
    private final List<BebopDroneListener> mListeners;

    /** list of mission listeners */
    private final List<BebopDroneMissionListener> mMissionListeners;

    /** handler */
    private final Handler mHandler;

    /* Parrot stuff */
    private ARDeviceController mDeviceController;
    private SDCardModule mSDCardModule;

    /** Bebop controller (phone) state */
    private ARCONTROLLER_DEVICE_STATE_ENUM mState;

    /** current run id */
    private String mCurrentRunId;

    /* current vehicle information */

    /** Bebop state */
    private ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM mFlyingState;

    /** current Bebop position */
    private Location mPosition;

    /** current altitude above the ground (as measured by sonar?) */
    private float mHeight = 0;

    /** current Bebop velocity */
    private VelocityVector mVelocity;

    /** current Bebop attitude */
    private AttitudeVector mAttitude;

    /** whether or not Bebop has completed the last relative move command sent */
    private boolean mFinishedLastCommand = true;

    /** helpful state to determine if we have already done a takeoff and are currently flying */
    private boolean mCurrentlyFlying = false;

    /** whether or not the bebop is currently recording video */
    private boolean mVideoRecording = false;

    /** the preferences that contain the settings for the drone */
    private SharedPreferences mSettings;

    public BebopDrone(Context context, @NonNull ARDiscoveryDeviceService deviceService) {

        mSettings = PreferenceManager.getDefaultSharedPreferences(context);

        mListeners = new ArrayList<>();
        mMissionListeners = new ArrayList<>();

        // needed because some callbacks will be called on the main thread
        mHandler = new Handler(context.getMainLooper());

        mState = ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_STOPPED;

        // if the product type of the deviceService match with the types supported
        ARDISCOVERY_PRODUCT_ENUM productType = ARDiscoveryService.getProductFromProductID(deviceService.getProductID());
        ARDISCOVERY_PRODUCT_FAMILY_ENUM family = ARDiscoveryService.getProductFamily(productType);
        if (ARDISCOVERY_PRODUCT_FAMILY_ENUM.ARDISCOVERY_PRODUCT_FAMILY_ARDRONE.equals(family)) {

            ARDiscoveryDevice discoveryDevice = createDiscoveryDevice(deviceService, productType);
            if (discoveryDevice != null) {
                mDeviceController = createDeviceController(discoveryDevice);
                discoveryDevice.dispose();
            }

            try {
                String productIP = ((ARDiscoveryDeviceNetService)(deviceService.getDevice())).getIp();

                ARUtilsManager ftpListManager = new ARUtilsManager();
                ARUtilsManager ftpQueueManager = new ARUtilsManager();

                ftpListManager.initWifiFtp(productIP, DEVICE_PORT, ARUtilsManager.FTP_ANONYMOUS, "");
                ftpQueueManager.initWifiFtp(productIP, DEVICE_PORT, ARUtilsManager.FTP_ANONYMOUS, "");

                mSDCardModule = new SDCardModule(ftpListManager, ftpQueueManager);
                mSDCardModule.addListener(mSDCardModuleListener);
            } catch (ARUtilsException e) {
                Log.e(TAG, "Exception", e);
            }

        } else {
            Log.e(TAG, "DeviceService type is not supported by BebopDrone");
        }
    }

    public void dispose() {
        if (mDeviceController != null) {
            mDeviceController.dispose();
        }
    }

    //region Listener functions
    public void addListener(BebopDroneListener listener) {
        mListeners.add(listener);
    }

    public void addMissionListener(BebopDroneMissionListener listener) {
        mMissionListeners.add(listener);
    }

    public void removeListener(BebopDroneListener listener) {
        mListeners.remove(listener);
    }

    public void removeMissionListener(BebopDroneMissionListener listener) {
        mMissionListeners.remove(listener);
    }
    //endregion Listener

    /**
     * Connect to the drone
     * @return true if operation was successful.
     *              Returning true doesn't mean that device is connected.
     *              You can be informed of the actual connection through {@link BebopDroneListener#onDroneConnectionChanged}
     */
    public boolean connect() {
        boolean success = false;
        if ((mDeviceController != null) && (ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_STOPPED.equals(mState))) {
            ARCONTROLLER_ERROR_ENUM error = mDeviceController.start();
            if (error == ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK) {
                success = true;
            }
        }
        return success;
    }

    /**
     * Disconnect from the drone
     * @return true if operation was successful.
     *              Returning true doesn't mean that device is disconnected.
     *              You can be informed of the actual disconnection through {@link BebopDroneListener#onDroneConnectionChanged}
     */
    public boolean disconnect() {
        boolean success = false;
        if ((mDeviceController != null) && (ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(mState))) {
            ARCONTROLLER_ERROR_ENUM error = mDeviceController.stop();
            if (error == ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK) {
                success = true;
            }
        }
        return success;
    }

    /**
     * Get the current connection state
     * @return the connection state of the drone
     */
    public ARCONTROLLER_DEVICE_STATE_ENUM getConnectionState() {
        return mState;
    }

    /**
     * Get the current flying state
     * @return the flying state
     */
    public ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM getFlyingState() {
        return mFlyingState;
    }

    /**
     * Determine whether or not the drone is in the landed state.
     * @return true if landed
     */
    public boolean isLanded() {
        return mFlyingState == ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED;
    }

    /**
     * Determine if the drone is ready to take move commands.
     * requires the drone either be flying or hovering.
     * @return true if flying or hovering
     */
    public boolean isReadyToFly() {
        return (mFlyingState == ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING ||
                mFlyingState == ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING);
    }

    /**
     * Determine if the drone is currently recording video.
     * @return true if currently recording video
     */
    public boolean isVideoRecording() {
        return mVideoRecording;
    }

    /**
     * Get the current GPS position
     * @return GPS position as a Location
     */
    public Location getPosition() {
        return mPosition;
    }

    /**
     * Get the height above the ground of the drone [m]
     * @return height in meters
     */
    public float getHeight() {
        return mHeight;
    }

    /**
     * Get the current velocity
     * @return NED velocity in m/s
     */
    public VelocityVector getVelocity() {
        return mVelocity;
    }

    /**
     * Get the current attitude
     * @return attitude in degrees
     */
    public AttitudeVector getAttitude() {
        return mAttitude;
    }

    /**
     * Determine whether or not Bebop has completed the last command sent to it
     * @return true if completed the last command sent
     */
    public boolean finishedLastCommand() {
        return mFinishedLastCommand;
    }

    /**
     * command the drone to takeoff
     */
    public void takeOff() {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingTakeOff();
        }
    }

    /**
     * command the drone to land
     */
    public void land() {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingLanding();
        }
    }

    /**
     * command the drone to execute the emergency procedure (immediately cuts the motors)
     */
    public void emergency() {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingEmergency();
        }
    }

    /**
     * command the drone to take a picture
     */
    public void takePicture() {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendMediaRecordPictureV2();
        }
    }

    /**
     * command the drone to start recording a video
     */
    public void startVideo() {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendMediaRecordVideoV2(ARCOMMANDS_ARDRONE3_MEDIARECORD_VIDEOV2_RECORD_ENUM.ARCOMMANDS_ARDRONE3_MEDIARECORD_VIDEOV2_RECORD_START);
        }
    }

    /**
     * command the drone to stop recording a video
     */
    public void stopVideo() {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendMediaRecordVideoV2(ARCOMMANDS_ARDRONE3_MEDIARECORD_VIDEOV2_RECORD_ENUM.ARCOMMANDS_ARDRONE3_MEDIARECORD_VIDEOV2_RECORD_STOP);
        }
    }

    /**
     * Command the Bebop drone to move in the body frame.
     * @param dx body x translation (front) [m]
     * @param dy body y translation (right side) [m]
     * @param dz body z translation (down) [m]
     * @param dpsi heading change [deg]
     */
    public void relativeMove(float dx, float dy, float dz, float dpsi) {
        // convert from deg to rad
        dpsi = (float) Math.toRadians((double) dpsi);

        // send the command
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingMoveBy(dx, dy, dz, dpsi);

            // mark the command being in progress
            mFinishedLastCommand = false;
        }
    }

    /**
     * Command the drone to flip (if there is enough battery).
     * @param direction direction which the drone should flip
     */
    public void flip(ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM direction) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendAnimationsFlip(direction);

            // mark the command being in progress
            // TODO: determine if there is returned information from the Bebop for when the flip is completed
            mFinishedLastCommand = false;
        }
    }

    /**
     * Set the forward/backward angle of the drone
     * Note that {@link BebopDrone#setFlag(byte)} should be set to 1 in order to take in account the pitch value
     * @param pitch value in percentage from -100 to 100
     */
    public void setPitch(byte pitch) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().setPilotingPCMDPitch(pitch);
        }
    }

    /**
     * Set the side angle of the drone
     * Note that {@link BebopDrone#setFlag(byte)} should be set to 1 in order to take in account the roll value
     * @param roll value in percentage from -100 to 100
     */
    public void setRoll(byte roll) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().setPilotingPCMDRoll(roll);
        }
    }

    /**
     * Set the yaw rotation speed of the drone
     * @param yaw value in percentage from -100 (max ccw rate) to 100 (max cw rate)
     */
    public void setYaw(byte yaw) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().setPilotingPCMDYaw(yaw);
        }
    }

    /**
     * Set the throttle of the drone
     * @param gaz value in percentage from -100 (max descent rate) to 100 (max ascent rate)
     */
    public void setGaz(byte gaz) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().setPilotingPCMDGaz(gaz);
        }
    }

    /**
     * Take in account or not the pitch and roll values
     * @param flag 1 if the pitch and roll values should be used, 0 otherwise
     */
    public void setFlag(byte flag) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().setPilotingPCMDFlag(flag);
        }
    }

    public void setMaxTilt(int tilt) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingSettingsMaxTilt(tilt);
        }
    }

    public void setMaxTiltSpeed(int tiltSpeed) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendSpeedSettingsMaxPitchRollRotationSpeed(tiltSpeed);
        }
    }

    public void setHullPresence(boolean present) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendSpeedSettingsHullProtection(present ? ((byte)1) : ((byte)0));
        }
    }

    public void setBankedTurn(boolean banked) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingSettingsBankedTurn(banked ? ((byte)1) : ((byte)0));
        }
    }

    public void setMaxDistance(int distance) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingSettingsMaxDistance(distance);
        }
    }

    public void setMaxAltitude(int altitude) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingSettingsMaxAltitude(altitude);
        }
    }

    public void setMaxVerticalSpeed(float speed) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendSpeedSettingsMaxVerticalSpeed(speed);
        }
    }

    public void setMaxRotationSpeed(int speed) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendSpeedSettingsMaxRotationSpeed(speed);
        }
    }

    public void setAutonomousMaxHorizontalSpeed(float speed) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingSettingsSetAutonomousFlightMaxHorizontalSpeed(speed);
        }
    }

    public void setAutonomousMaxVerticalSpeed(float speed) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingSettingsSetAutonomousFlightMaxVerticalSpeed(speed);
        }
    }

    public void setAutonomousMaxRotationSpeed(float speed) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingSettingsSetAutonomousFlightMaxRotationSpeed(speed);
        }
    }

    /**
     * Download the last flight medias
     * Uses the run id to download all medias related to the last flight
     * If no run id is available, download all medias of the day
     */
    public void getLastFlightMedias() {
        String runId = mCurrentRunId;
        if ((runId != null) && !runId.isEmpty()) {
            mSDCardModule.getFlightMedias(runId);
        } else {
            Log.e(TAG, "RunID not available, fallback to the day's medias");
            mSDCardModule.getTodaysFlightMedias();
        }
    }

    public void cancelGetLastFlightMedias() {
        mSDCardModule.cancelGetFlightMedias();
    }

    /**
     * create a discovery device from a discovery device service
     * @param service the ARDiscoveryDeviceService from the discovery process
     * @param productType the type of produce that has been discovered
     * @return device that has been discovered
     */
    private ARDiscoveryDevice createDiscoveryDevice(@NonNull ARDiscoveryDeviceService service, ARDISCOVERY_PRODUCT_ENUM productType) {
        ARDiscoveryDevice device = null;
        try {
            device = new ARDiscoveryDevice();

            ARDiscoveryDeviceNetService netDeviceService = (ARDiscoveryDeviceNetService) service.getDevice();
            device.initWifi(productType, netDeviceService.getName(), netDeviceService.getIp(), netDeviceService.getPort());

        } catch (ARDiscoveryException e) {
            Log.e(TAG, "Exception", e);
            Log.e(TAG, "Error: " + e.getError());
        }

        return device;
    }

    /**
     * create a controller from a discovery device
     * will allow for controlling and listening to the state information of the device
     * @param discoveryDevice device that has been discovered
     * @return controller for the device that has been discovered
     */
    private ARDeviceController createDeviceController(@NonNull ARDiscoveryDevice discoveryDevice) {
        ARDeviceController deviceController = null;
        try {
            deviceController = new ARDeviceController(discoveryDevice);

            deviceController.addListener(mDeviceControllerListener);
            deviceController.addStreamListener(mStreamListener);
        } catch (ARControllerException e) {
            Log.e(TAG, "Exception", e);
        }

        return deviceController;
    }

    //region notify listener block
    private void notifyConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {
        List<BebopDroneListener> listenersCpy = new ArrayList<>(mListeners);
        for (BebopDroneListener listener : listenersCpy) {
            listener.onDroneConnectionChanged(state);
        }
    }

    private void notifyBatteryChanged(Date timestamp, int battery) {
        List<BebopDroneListener> listenersCpy = new ArrayList<>(mListeners);
        for (BebopDroneListener listener : listenersCpy) {
            listener.onBatteryChargeChanged(timestamp, battery);
        }
    }

    private void notifyPilotingStateChanged(Date timestamp, ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {
        List<BebopDroneListener> listenersCpy = new ArrayList<>(mListeners);
        for (BebopDroneListener listener : listenersCpy) {
            listener.onPilotingStateChanged(timestamp, state);
        }
    }

    private void notifyPositionChanged(Date timestamp, double lat, double lon, double alt) {
        List<BebopDroneListener> listenersCpy = new ArrayList<>(mListeners);
        for (BebopDroneListener listener : listenersCpy) {
            listener.onPositionChanged(timestamp, lat, lon, alt);
        }
    }

    private void notifySpeedChanged(Date timestamp, float vx, float vy, float vz) {
        List<BebopDroneListener> listenersCpy = new ArrayList<>(mListeners);
        for (BebopDroneListener listener : listenersCpy) {
            listener.onSpeedChanged(timestamp, vx, vy, vz);
        }
    }

    private void notifyAttitudeChanged(Date timestamp, float roll, float pitch, float yaw) {
        List<BebopDroneListener> listenersCpy = new ArrayList<>(mListeners);
        for (BebopDroneListener listener : listenersCpy) {
            listener.onAttitudeChanged(timestamp, roll, pitch, yaw);
        }
    }

    private void notifyRelativeAltitudeChanged(Date timestamp, double alt) {
        List<BebopDroneListener> listenersCpy = new ArrayList<>(mListeners);
        for (BebopDroneListener listener : listenersCpy) {
            listener.onRelativeAltitudeChanged(timestamp, alt);
        }
    }

    private void notifyRelativeMoveEnded(Date timestamp, float dx, float dy, float dz, float dpsi, int error) {
        List<BebopDroneListener> listenersCpy = new ArrayList<>(mListeners);
        for (BebopDroneListener listener : listenersCpy) {
            listener.onRelativeMoveEnded(timestamp, dx, dy, dz, dpsi, error);
        }
    }

    private void notifyPictureTaken(Date timestamp, ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error) {
        List<BebopDroneListener> listenersCpy = new ArrayList<>(mListeners);
        for (BebopDroneListener listener : listenersCpy) {
            listener.onPictureTaken(timestamp, error);
        }
    }

    private void notifyVideoStateChanged(Date timestamp, ARCOMMANDS_ARDRONE3_MEDIARECORDSTATE_VIDEOSTATECHANGEDV2_STATE_ENUM event, ARCOMMANDS_ARDRONE3_MEDIARECORDSTATE_VIDEOSTATECHANGEDV2_ERROR_ENUM error) {
        List<BebopDroneListener> listenersCpy = new ArrayList<>(mListeners);
        for (BebopDroneListener listener : listenersCpy) {
            listener.onVideoStateChanged(timestamp, event, error);
        }
    }

    private void notifyConfigureDecoder(ARControllerCodec codec) {
        List<BebopDroneListener> listenersCpy = new ArrayList<>(mListeners);
        for (BebopDroneListener listener : listenersCpy) {
            listener.configureDecoder(codec);
        }
    }

    private void notifyFrameReceived(ARFrame frame) {
        List<BebopDroneListener> listenersCpy = new ArrayList<>(mListeners);
        for (BebopDroneListener listener : listenersCpy) {
            listener.onFrameReceived(frame);
        }
    }

    private void notifyMatchingMediasFound(int nbMedias) {
        List<BebopDroneListener> listenersCpy = new ArrayList<>(mListeners);
        for (BebopDroneListener listener : listenersCpy) {
            listener.onMatchingMediasFound(nbMedias);
        }
    }

    private void notifyDownloadProgressed(String mediaName, int progress) {
        List<BebopDroneListener> listenersCpy = new ArrayList<>(mListeners);
        for (BebopDroneListener listener : listenersCpy) {
            listener.onDownloadProgressed(mediaName, progress);
        }
    }

    private void notifyDownloadComplete(String mediaName) {
        List<BebopDroneListener> listenersCpy = new ArrayList<>(mListeners);
        for (BebopDroneListener listener : listenersCpy) {
            listener.onDownloadComplete(mediaName);
        }
    }

    private void notifyMissionCommandFinished() {
        List<BebopDroneMissionListener> listenersCpy = new ArrayList<>(mMissionListeners);
        for (BebopDroneMissionListener listener : listenersCpy) {
            listener.onCommandFinished();
        }
    }
    //endregion notify listener block

    /** listener for the sd card information to be able to download pictures and video from the drone */
    private final SDCardModule.Listener mSDCardModuleListener = new SDCardModule.Listener() {
        @Override
        public void onMatchingMediasFound(final int nbMedias) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyMatchingMediasFound(nbMedias);
                }
            });
        }

        @Override
        public void onDownloadProgressed(final String mediaName, final int progress) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyDownloadProgressed(mediaName, progress);
                }
            });
        }

        @Override
        public void onDownloadComplete(final String mediaName) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyDownloadComplete(mediaName);
                }
            });
        }
    };

    /** listener for the state information from the Bebop drone */
    private final ARDeviceControllerListener mDeviceControllerListener = new ARDeviceControllerListener() {
        @Override
        public void onStateChanged(ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARCONTROLLER_ERROR_ENUM error) {
            mState = newState;
            if (ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(mState)) {
                mDeviceController.getFeatureARDrone3().sendMediaStreamingVideoEnable((byte) 1);
            } else if (ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_STOPPED.equals(mState)) {
                mSDCardModule.cancelGetFlightMedias();
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyConnectionChanged(mState);
                }
            });
        }

        @Override
        public void onExtensionStateChanged(ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARDISCOVERY_PRODUCT_ENUM product, String name, ARCONTROLLER_ERROR_ENUM error) {
        }

        @Override
        public void onCommandReceived(ARDeviceController deviceController, ARCONTROLLER_DICTIONARY_KEY_ENUM commandKey, ARControllerDictionary elementDictionary) {

            // this needs to not be null for there to be useful information
            if (elementDictionary == null) {
                return;
            }

            ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
            if (args == null) {
                return;
            }

            // get the current timestamp - will be used to know when the event/command was received
            final Date now = new Date();

            switch (commandKey) {

                /* battery update */
                case ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED:
                    final int battery = (Integer) args.get(ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED_PERCENT);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyBatteryChanged(now, battery);
                        }
                    });
                    break;

                /* flying state update */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED:
                    final ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state = ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.getFromValue((Integer) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE));
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mFlyingState = state;
                            notifyPilotingStateChanged(now, state);

                            // takeoff is one of the initial mission commands and doesn't trigger a move end
                            // so need to manually trigger the command finished
                            if (isReadyToFly() && !mCurrentlyFlying) {
                                mCurrentlyFlying = true;
                                notifyMissionCommandFinished();
                            }
                        }
                    });
                    break;

                /* drone position changed */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_POSITIONCHANGED:
                    final double latitude = (double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_POSITIONCHANGED_LATITUDE);
                    final double longitude = (double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_POSITIONCHANGED_LONGITUDE);
                    final double altitude = (double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_POSITIONCHANGED_ALTITUDE);

                    final Location loc = new Location("Bebop");
                    loc.setLatitude(latitude);
                    loc.setLongitude(longitude);
                    loc.setAltitude(altitude);

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mPosition = loc;
                            notifyPositionChanged(now, latitude, longitude, altitude);
                        }
                    });

                    break;

                /* speed changed */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_SPEEDCHANGED:
                    final float speedX = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_SPEEDCHANGED_SPEEDX)).doubleValue();
                    final float speedY = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_SPEEDCHANGED_SPEEDY)).doubleValue();
                    final float speedZ = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_SPEEDCHANGED_SPEEDZ)).doubleValue();

                    final VelocityVector speed = new VelocityVector(speedX, speedY, speedZ);

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mVelocity = speed;
                            notifySpeedChanged(now, speedX, speedY, speedZ);
                        }
                    });

                    break;

                /* attitude changed */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_ATTITUDECHANGED:
                    final float roll = (float) Math.toDegrees((double) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_ATTITUDECHANGED_ROLL));
                    final float pitch = (float) Math.toDegrees((double) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_ATTITUDECHANGED_PITCH));
                    final float yaw = (float) Math.toDegrees((double) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_ATTITUDECHANGED_YAW));

                    final AttitudeVector att = new AttitudeVector(roll, pitch, yaw);

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mAttitude = att;
                            notifyAttitudeChanged(now, roll, pitch, yaw);
                        }
                    });

                    break;

                /* altitude changed (above start ground) */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_ALTITUDECHANGED:
                    final double relativeAltitude = (double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_ALTITUDECHANGED_ALTITUDE);

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mHeight = (float) relativeAltitude;
                            notifyRelativeAltitudeChanged(now, relativeAltitude);
                        }
                    });

                    break;

                /* relative move ended */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGEVENT_MOVEBYEND:
                    final float dX = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGEVENT_MOVEBYEND_DX)).doubleValue();
                    final float dY = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGEVENT_MOVEBYEND_DY)).doubleValue();
                    final float dZ = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGEVENT_MOVEBYEND_DZ)).doubleValue();
                    final float dPsi = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGEVENT_MOVEBYEND_DPSI)).doubleValue();
                    final ARCOMMANDS_ARDRONE3_PILOTINGEVENT_MOVEBYEND_ERROR_ENUM relativeMoveError = ARCOMMANDS_ARDRONE3_PILOTINGEVENT_MOVEBYEND_ERROR_ENUM.getFromValue((Integer)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGEVENT_MOVEBYEND_ERROR));

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyRelativeMoveEnded(now, dX, dY, dZ, dPsi, relativeMoveError.getValue());

                            // mark as having just finished a command
                            mFinishedLastCommand = true;
                            notifyMissionCommandFinished();
                        }
                    });

                    break;

                /* picture notification */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED:
                    final ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM pictureError = ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM.getFromValue((Integer)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR));
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // notify the changes as needed - note this is mission related so notify of the event
                            notifyPictureTaken(now, pictureError);
                            notifyMissionCommandFinished();
                        }
                    });
                    break;

                /* video notification */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_MEDIARECORDSTATE_VIDEOSTATECHANGEDV2:
                    final ARCOMMANDS_ARDRONE3_MEDIARECORDSTATE_VIDEOSTATECHANGEDV2_STATE_ENUM videoState = ARCOMMANDS_ARDRONE3_MEDIARECORDSTATE_VIDEOSTATECHANGEDV2_STATE_ENUM.getFromValue((Integer)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_MEDIARECORDSTATE_VIDEOSTATECHANGEDV2_STATE));
                    final ARCOMMANDS_ARDRONE3_MEDIARECORDSTATE_VIDEOSTATECHANGEDV2_ERROR_ENUM videoError = ARCOMMANDS_ARDRONE3_MEDIARECORDSTATE_VIDEOSTATECHANGEDV2_ERROR_ENUM.getFromValue((Integer) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_MEDIARECORDSTATE_VIDEOSTATECHANGEDV2_ERROR));

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // update the local recording state
                            mVideoRecording = ARCOMMANDS_ARDRONE3_MEDIARECORDSTATE_VIDEOSTATECHANGEDV2_STATE_ENUM.ARCOMMANDS_ARDRONE3_MEDIARECORDSTATE_VIDEOSTATECHANGEDV2_STATE_STARTED.equals(videoState);

                            // notify the changes as needed - note this is mission related so notify of the event
                            notifyVideoStateChanged(now, videoState, videoError);
                        }
                    });

                    break;

                /* banked turn mode */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_BANKEDTURNCHANGED:
                    final byte bankedState = (byte)((Integer)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_BANKEDTURNCHANGED_STATE)).intValue();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mSettings.edit().putBoolean(BebopPreferenceFragment.KEY_BANKED_TURN, bankedState == 1).apply();
                        }
                    });
                    break;

                /* hull present */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_SPEEDSETTINGSSTATE_HULLPROTECTIONCHANGED:
                    final byte hullPresent = (byte)((Integer)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_SPEEDSETTINGSSTATE_HULLPROTECTIONCHANGED_PRESENT)).intValue();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mSettings.edit().putBoolean(BebopPreferenceFragment.KEY_HULL, hullPresent == 1).apply();
                        }
                    });
                    break;

                /* max tilt */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_MAXTILTCHANGED:
                    final float currentTilt = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_MAXTILTCHANGED_CURRENT)).doubleValue();
                    final float minTilt = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_MAXTILTCHANGED_MIN)).doubleValue();
                    final float maxTilt = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_MAXTILTCHANGED_MAX)).doubleValue();

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mSettings.edit().putInt(BebopPreferenceFragment.KEY_MAX_TILT, (int) currentTilt).apply();
                        }
                    });

                    break;

                /* max tilt speed */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_SPEEDSETTINGSSTATE_MAXPITCHROLLROTATIONSPEEDCHANGED:
                    final float currentTiltRate = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_SPEEDSETTINGSSTATE_MAXPITCHROLLROTATIONSPEEDCHANGED_CURRENT)).doubleValue();
                    float minTiltRate = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_SPEEDSETTINGSSTATE_MAXPITCHROLLROTATIONSPEEDCHANGED_MIN)).doubleValue();
                    final float maxTiltRate = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_SPEEDSETTINGSSTATE_MAXPITCHROLLROTATIONSPEEDCHANGED_MAX)).doubleValue();

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mSettings.edit().putInt(BebopPreferenceFragment.KEY_MAX_TILT_SPEED, (int) currentTiltRate).apply();
                        }
                    });

                    break;

                /* max altitude */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_MAXALTITUDECHANGED:
                    final float currentAlt = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_MAXALTITUDECHANGED_CURRENT)).doubleValue();
                    float minAlt = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_MAXALTITUDECHANGED_MIN)).doubleValue();
                    final float maxAlt = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_MAXALTITUDECHANGED_MAX)).doubleValue();

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mSettings.edit().putInt(BebopPreferenceFragment.KEY_MAX_ALTITUDE, (int) currentAlt).apply();
                        }
                    });

                    break;

                /* max distance */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_MAXDISTANCECHANGED:

                    final float currentDist = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_MAXDISTANCECHANGED_CURRENT)).doubleValue();
                    float minDist = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_MAXDISTANCECHANGED_MIN)).doubleValue();
                    final float maxDist = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_MAXDISTANCECHANGED_MAX)).doubleValue();

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mSettings.edit().putInt(BebopPreferenceFragment.KEY_MAX_DISTANCE, (int) currentDist).apply();
                        }
                    });

                    break;

                /* max vertical speed */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_SPEEDSETTINGSSTATE_MAXVERTICALSPEEDCHANGED:

                    final float currentVert = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_SPEEDSETTINGSSTATE_MAXVERTICALSPEEDCHANGED_CURRENT)).doubleValue();
                    float minVert = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_SPEEDSETTINGSSTATE_MAXVERTICALSPEEDCHANGED_MIN)).doubleValue();
                    final float maxVert = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_SPEEDSETTINGSSTATE_MAXVERTICALSPEEDCHANGED_MAX)).doubleValue();

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mSettings.edit().putInt(BebopPreferenceFragment.KEY_MAX_VERTICAL_SPEED, (int) (currentVert*10)).apply();
                        }
                    });

                    break;

                /* max rotation speed */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_SPEEDSETTINGSSTATE_MAXROTATIONSPEEDCHANGED:

                    final float currentRot = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_SPEEDSETTINGSSTATE_MAXROTATIONSPEEDCHANGED_CURRENT)).doubleValue();
                    final float minRot = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_SPEEDSETTINGSSTATE_MAXROTATIONSPEEDCHANGED_MIN)).doubleValue();
                    final float maxRot = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_SPEEDSETTINGSSTATE_MAXROTATIONSPEEDCHANGED_MAX)).doubleValue();

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mSettings.edit().putInt(BebopPreferenceFragment.KEY_MAX_ROTATION_SPEED, (int) currentRot).apply();
                        }
                    });

                    break;

                /* auto max vh */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_AUTONOMOUSFLIGHTMAXHORIZONTALSPEED:

                    final float maxAutoVH = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_AUTONOMOUSFLIGHTMAXHORIZONTALSPEED_VALUE)).doubleValue();

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            //Toast.makeText(mContext, "current auto vh possible: " + maxAutoVH, Toast.LENGTH_SHORT).show();
                        }
                    });

                    break;

                /* auto max vv */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_AUTONOMOUSFLIGHTMAXVERTICALSPEED:

                    final float maxAutoVV = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_AUTONOMOUSFLIGHTMAXVERTICALSPEED_VALUE)).doubleValue();

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            //Toast.makeText(mContext, "current auto vv possible: " + maxAutoVV, Toast.LENGTH_SHORT).show();
                        }
                    });

                    break;

                /* auto max ah */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_AUTONOMOUSFLIGHTMAXHORIZONTALACCELERATION:

                    final float maxAutoAH = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_AUTONOMOUSFLIGHTMAXHORIZONTALACCELERATION_VALUE)).doubleValue();

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            //Toast.makeText(mContext, "current auto ah possible: " + maxAutoAH, Toast.LENGTH_SHORT).show();
                        }
                    });

                    break;

                /* auto max av */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_AUTONOMOUSFLIGHTMAXVERTICALACCELERATION:

                    final float maxAutoAV = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_AUTONOMOUSFLIGHTMAXVERTICALACCELERATION_VALUE)).doubleValue();

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            //Toast.makeText(mContext, "current auto av possible: " + maxAutoAV, Toast.LENGTH_SHORT).show();
                        }
                    });

                    break;

                /* auto max rotation rate */
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_AUTONOMOUSFLIGHTMAXROTATIONSPEED:

                    final float maxAutoRot = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSETTINGSSTATE_AUTONOMOUSFLIGHTMAXROTATIONSPEED_VALUE)).doubleValue();

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            //Toast.makeText(mContext, "current auto rot possible: " + maxAutoRot, Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;

                /* run id */
                case ARCONTROLLER_DICTIONARY_KEY_COMMON_RUNSTATE_RUNIDCHANGED:
                    final String runID = (String) args.get(ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_RUNSTATE_RUNIDCHANGED_RUNID);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCurrentRunId = runID;
                        }
                    });
                    break;

                default:
                    // TODO: probably nothing, but think to see if there is anything desired here
                    break;
            }

        }
    };

    private final ARDeviceControllerStreamListener mStreamListener = new ARDeviceControllerStreamListener() {
        @Override
        public ARCONTROLLER_ERROR_ENUM configureDecoder(ARDeviceController deviceController, final ARControllerCodec codec) {
            notifyConfigureDecoder(codec);
            return ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK;
        }

        @Override
        public ARCONTROLLER_ERROR_ENUM onFrameReceived(ARDeviceController deviceController, final ARFrame frame) {
            notifyFrameReceived(frame);
            return ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK;
        }

        @Override
        public void onFrameTimeout(ARDeviceController deviceController) {}
    };
}
