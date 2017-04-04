package edu.stanford.aa122.bebopcontroller;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import java.util.Date;
import java.util.Locale;

import edu.stanford.aa122.bebopcontroller.controller.AdvancedController;
import edu.stanford.aa122.bebopcontroller.controller.ManualController;
import edu.stanford.aa122.bebopcontroller.drone.BebopDrone;
import edu.stanford.aa122.bebopcontroller.helpers.DataLogger;
import edu.stanford.aa122.bebopcontroller.listener.BebopDroneListener;
import edu.stanford.aa122.bebopcontroller.view.AttitudeIndicator;
import edu.stanford.aa122.bebopcontroller.view.BebopVideoView;
import edu.stanford.aa122.bebopcontroller.view.JoystickView;

/**
 * Main activity that handles the video display and interaction with the Bebop drone.
 *
 * Taken from Parrot SDK Samples.
 */
public class BebopActivity extends AppCompatActivity {

    /** DEBUG tag */
    private static final String TAG = "BebopActivity";

    // control modes
    private static final int MODE_MANUAL = 0;
    private static final int MODE_AUTONOMOUS = 1;

    /** conversion from meters to feet */
    private static final double METERS_TO_FEET = 3.28084;

    /** connection to drone to be controlled */
    private BebopDrone mBebopDrone;

    /** logger for logging all of the Bebop data */
    private DataLogger mDataLogger;

    private ProgressDialog mConnectionProgressDialog;
    private ProgressDialog mDownloadProgressDialog;

    // layout view elements

    /** view that displays the video from the Bebop */
    private BebopVideoView mVideoView;

    /** text view for displaying the battery level */
    private TextView tvBattery;

    /** text view for displaying the altitude */
    private TextView tvAltitude;

    /** text view for displaying distance from the pilot */
    private TextView tvDistance;

    /** button for displaying the settings */
    private Button btnSettings;

    /** button for the main action (takeoff, landing, mission start, etc) */
    private Button btnAction;

    /** view for the manual control elements */
    private View viewManualControl;

    /** view representing the attitude of the drone */
    private AttitudeIndicator mAttitudeView;

    /** the current location of the phone - potentially only going to be determined once */
    private Location mUserLocation;

    /** location manager for getting GPS position of user */
    private LocationManager mLocationManager;

    // TODO: add download button
    private int mNbMaxDownload;
    private int mCurrentDownloadIndex;

    // state information
    private int mControlMode = MODE_MANUAL;

    private AdvancedController mAdvancedController;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bebop_custom);

        // initializing the base buttons of the view
        initView();

        Intent intent = getIntent();
        ARDiscoveryDeviceService service = intent.getParcelableExtra(DeviceListActivity.EXTRA_DEVICE_SERVICE);
        mBebopDrone = new BebopDrone(this, service);
        mBebopDrone.addListener(mBebopListener);

        // add the data logging elements
        mDataLogger = new DataLogger(this);
        mBebopDrone.addListener(mDataLogger);

        // get a location manager
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // add the manual controller - for now will always have the manual controller
        // TODO: should be a framework for deciding which controller to initialize

        // TODO: does this even work??
        // the possible controllers
        ManualController manualController = new ManualController(mBebopDrone, findViewById(R.id.include_manual_control));
        mAdvancedController = new AdvancedController(mBebopDrone);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // stop listening for the position
        mLocationManager.removeUpdates(mLocationListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // start listening for position updates
        if (!(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // show a loading view while the bebop drone is connecting
        if ((mBebopDrone != null) && !(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(mBebopDrone.getConnectionState()))) {
            mConnectionProgressDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
            mConnectionProgressDialog.setIndeterminate(true);
            mConnectionProgressDialog.setMessage("Connecting ...");
            mConnectionProgressDialog.setCancelable(false);
            mConnectionProgressDialog.show();

            // if the connection to the Bebop fails, finish the activity
            if (!mBebopDrone.connect()) {
                finish();
            }

            // start the logger - since at this point we are connected
            mDataLogger.startNewLog();
        }
    }

    @Override
    public void onBackPressed() {
        if (mBebopDrone != null) {
            mConnectionProgressDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
            mConnectionProgressDialog.setIndeterminate(true);
            mConnectionProgressDialog.setMessage("Disconnecting ...");
            mConnectionProgressDialog.setCancelable(false);
            mConnectionProgressDialog.show();

            if (!mBebopDrone.disconnect()) {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        mBebopDrone.dispose();
        mDataLogger.stopLogging();
        super.onDestroy();
    }

    /**
     * initialize the view elements
     */
    private void initView() {
        // video view
        mVideoView = (BebopVideoView) findViewById(R.id.videoView);

        // manual control view
        viewManualControl = findViewById(R.id.include_manual_control);

        // attitude view
        mAttitudeView = (AttitudeIndicator) findViewById(R.id.view_attitude);

        // textviews
        tvBattery = (TextView) findViewById(R.id.text_battery);
        tvAltitude = (TextView) findViewById(R.id.text_altitude);
        tvDistance = (TextView) findViewById(R.id.text_distance);

        // emergency button
        findViewById(R.id.button_emergency).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBebopDrone.emergency();
            }
        });

        // action button
        btnAction = (Button) findViewById(R.id.button_action);
        btnAction.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switch (mBebopDrone.getFlyingState()) {
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:

                        // either takeoff or start the mission - depending on control mode
                        if (mControlMode == MODE_AUTONOMOUS) {
                            mAdvancedController.startMission();
                        } else {
                            mBebopDrone.takeOff();
                        }

                        break;
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:

                        // either land or stop the mission - depending on control mode
                        if (mControlMode == MODE_AUTONOMOUS) {
                            mAdvancedController.stopMission();
                        } else {
                            mBebopDrone.land();
                        }

                        break;
                    default:
                }
            }
        });

        // mode button
        findViewById(R.id.button_mode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (mControlMode) {
                    case MODE_MANUAL:
                        // switch to autonomous control mode and hide manual controls
                        mControlMode = MODE_AUTONOMOUS;
                        viewManualControl.setVisibility(View.GONE);
                        break;

                    case MODE_AUTONOMOUS:
                        // switch to manual control and display the manual controls
                        mControlMode = MODE_MANUAL;
                        viewManualControl.setVisibility(View.VISIBLE);
                        break;
                }
            }
        });

        /*
        // TODO: add a take picture button
        findViewById(R.id.takePictureBt).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBebopDrone.takePicture();
            }
        });
        */

        /*
        // TODO: get a download button back
        mDownloadBt = (Button)findViewById(R.id.downloadBt);
        mDownloadBt.setEnabled(false);
        mDownloadBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBebopDrone.getLastFlightMedias();

                mDownloadProgressDialog = new ProgressDialog(BebopActivity.this, R.style.AppCompatAlertDialogStyle);
                mDownloadProgressDialog.setIndeterminate(true);
                mDownloadProgressDialog.setMessage("Fetching medias");
                mDownloadProgressDialog.setCancelable(false);
                mDownloadProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBebopDrone.cancelGetLastFlightMedias();
                    }
                });
                mDownloadProgressDialog.show();
            }
        });
        */

        // NOTE: all the manual control buttons are now initialized and maintained by the manual controller


    }

    /** listener for the bebop drone information */
    private final BebopDroneListener mBebopListener = new BebopDroneListener() {
        @Override
        public void onDroneConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {
            switch (state)
            {
                case ARCONTROLLER_DEVICE_STATE_RUNNING:
                    mConnectionProgressDialog.dismiss();
                    break;

                case ARCONTROLLER_DEVICE_STATE_STOPPED:
                    // if the deviceController is stopped, go back to the previous activity
                    mConnectionProgressDialog.dismiss();
                    finish();
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onBatteryChargeChanged(Date timestamp, int batteryPercentage) {
            tvBattery.setText(String.format(Locale.US, "%d%%", batteryPercentage));
        }

        @Override
        public void onPilotingStateChanged(Date timestamp, ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {
            switch (state) {
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                    btnAction.setText(R.string.takeoff);
                    btnAction.setEnabled(true);
                    //mDownloadBt.setEnabled(true);
                    break;
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                    btnAction.setText(R.string.land);
                    btnAction.setEnabled(true);
                    //mDownloadBt.setEnabled(false);
                    break;
                default:
                    btnAction.setEnabled(false);
                    //mDownloadBt.setEnabled(false);
            }
        }

        @Override
        public void onPositionChanged(Date timestamp, double lat, double lon, double alt) {

            if (mUserLocation == null || lat == -500.0) {
                tvDistance.setText("ft");
                return;
            }

            // convert to a Location for easier handling
            Location dronePos = new Location("Bebop");
            dronePos.setLatitude(lat);
            dronePos.setLongitude(lon);
            dronePos.setAltitude(alt);

            // get the distance to the drone in feet
            double distanceToBebop = mUserLocation.distanceTo(dronePos)*METERS_TO_FEET;

            // display the distance
            tvDistance.setText(String.format(Locale.US, "%d ft", (int) distanceToBebop));
        }

        @Override
        public void onSpeedChanged(Date timestamp, float vx, float vy, float vz) {

        }

        @Override
        public void onAttitudeChanged(Date timestamp, float roll, float pitch, float yaw) {
            mAttitudeView.setAttitude(roll, pitch, yaw);
        }

        @Override
        public void onRelativeAltitudeChanged(Date timestamp, double alt) {
            tvAltitude.setText(String.format(Locale.US, "%d ft", (int) alt));
        }

        @Override
        public void onPictureTaken(Date timestamp, ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error) {
            Log.i(TAG, "Picture has been taken");
        }

        @Override
        public void configureDecoder(ARControllerCodec codec) {
            mVideoView.configureDecoder(codec);
        }

        @Override
        public void onFrameReceived(ARFrame frame) {
            mVideoView.displayFrame(frame);
        }

        @Override
        public void onMatchingMediasFound(int nbMedias) {
            mDownloadProgressDialog.dismiss();

            mNbMaxDownload = nbMedias;
            mCurrentDownloadIndex = 1;

            if (nbMedias > 0) {
                mDownloadProgressDialog = new ProgressDialog(BebopActivity.this, R.style.AppCompatAlertDialogStyle);
                mDownloadProgressDialog.setIndeterminate(false);
                mDownloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mDownloadProgressDialog.setMessage("Downloading medias");
                mDownloadProgressDialog.setMax(mNbMaxDownload * 100);
                mDownloadProgressDialog.setSecondaryProgress(mCurrentDownloadIndex * 100);
                mDownloadProgressDialog.setProgress(0);
                mDownloadProgressDialog.setCancelable(false);
                mDownloadProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBebopDrone.cancelGetLastFlightMedias();
                    }
                });
                mDownloadProgressDialog.show();
            }
        }

        @Override
        public void onDownloadProgressed(String mediaName, int progress) {
            mDownloadProgressDialog.setProgress(((mCurrentDownloadIndex - 1) * 100) + progress);
        }

        @Override
        public void onDownloadComplete(String mediaName) {
            mCurrentDownloadIndex++;
            mDownloadProgressDialog.setSecondaryProgress(mCurrentDownloadIndex * 100);

            if (mCurrentDownloadIndex > mNbMaxDownload) {
                mDownloadProgressDialog.dismiss();
                mDownloadProgressDialog = null;
            }
        }
    };

    /** listener for the GPS position of the phone (user) */
    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mUserLocation = location;
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };
}
