package edu.stanford.aa122.bebopcontroller.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;

import edu.stanford.aa122.bebopcontroller.R;
import edu.stanford.aa122.bebopcontroller.drone.BebopDrone;

/**
 * fragment to display and handle the preferences for the app.
 *
 * @author Adrien Perkins <adrienp@stanford.edu>
 */
public class BebopPreferenceFragment extends PreferenceFragment {

    public static final String KEY_HULL = "pref_bebop_cage";
    public static final String KEY_BANKED_TURN = "pref_bebop_banked_turn";
    public static final String KEY_MAX_ALTITUDE = "pref_bebop_max_alt";
    public static final String KEY_MAX_DISTANCE = "pref_bebop_max_dist";
    public static final String KEY_MAX_TILT = "pref_bebop_max_tilt";
    public static final String KEY_MAX_TILT_SPEED = "pref_bebop_max_tilt_speed";
    public static final String KEY_MAX_VERTICAL_SPEED = "pref_bebop_max_vert_speed";
    public static final String KEY_MAX_ROTATION_SPEED = "pref_bebop_max_rot_speed";
    public static final String KEY_JOYSTICK_MAX_TILT = "pref_joystick_max_pitch";
    public static final String KEY_JOYSTICK_MAX_ROTATION = "pref_joystick_max_yaw";
    public static final String KEY_JOYSTICK_MAX_THROTTLE = "pref_joystick_max_throttle";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load from XML
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

}
