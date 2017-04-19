package edu.stanford.aa122.bebopcontroller.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;

import edu.stanford.aa122.bebopcontroller.R;

/**
 * fragment to display and handle the preferences for the app.
 *
 * @author Adrien Perkins <adrienp@stanford.edu>
 */
public class BebopPreferenceFragment extends PreferenceFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load from XML
        addPreferencesFromResource(R.xml.preferences);
    }
}
