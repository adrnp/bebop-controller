package edu.stanford.aa122.bebopcontroller.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import edu.stanford.aa122.bebopcontroller.R;

/**
 * custom preference based on a seekbar for user selection of a value.
 *
 * adapted from API level 25's default seekbar preference to be able to be used on older versions
 * @author Adrien Perkins <adrienp@stanford.edu>
 */
public class SeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {

    /** current progress (value) of the seekbar */
    private int mProgress;

    /** maximum possible value of the seekbar */
    private int mMax;

    /** flag for whether or not the user is currently sliding the bar */
    private boolean mTrackingTouch;

    /** the setting title to display */
    private String mTitle = null;

    /** the explanation of the setting to show */
    private String mDetails = null;

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setLayoutResource(R.layout.preference_seekbar);



        // get any image resources that might have been given
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SeekBarPreference, 0, 0);
        try {
            mTitle = a.getString(R.styleable.SeekBarPreference_title);
            mDetails = a.getString(R.styleable.SeekBarPreference_details);
            int max =  a.getInt(R.styleable.SeekBarPreference_max, 100);
            setMax(max);

        } finally {
            a.recycle();
        }
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeekBarPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        // configure the seekbar
        SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(mMax);
        seekBar.setProgress(mProgress);
        seekBar.setEnabled(isEnabled());

        // configure the title
        if (mTitle != null) {
            ((TextView) view.findViewById(R.id.text_preference_seekbar_title)).setText(mTitle);
        }

        // configure the details
        if (mDetails != null) {
            ((TextView) view.findViewById(R.id.text_preference_seekbar_detail)).setText(mDetails);
        } else {
            view.findViewById(R.id.text_preference_seekbar_detail).setVisibility(View.GONE);
        }

    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setProgress(restoreValue ? getPersistedInt(mProgress) : (Integer) defaultValue);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    /**
     * set the maximum value represented by the seekbar
     * @param max maximum value
     */
    public void setMax(int max) {
        if (max != mMax) {
            mMax = max;
            notifyChanged();
        }
    }

    /**
     * set a specific value of the seekbar
     * @param progress value to set the seekbar
     */
    public void setProgress(int progress) {
        setProgress(progress, true);
    }

    /**
     * handles setting the progress of the seekbar
     * @param progress value to set the seekbar
     * @param notifyChanged flag for notifying the progress change
     */
    private void setProgress(int progress, boolean notifyChanged) {
        if (progress > mMax) {
            progress = mMax;
        }
        if (progress < 0) {
            progress = 0;
        }
        if (progress != mProgress) {
            mProgress = progress;
            persistInt(progress);
            if (notifyChanged) {
                notifyChanged();
            }
        }
    }

    /**
     * retrieve the current value of the seekbar
     * @return integer value the seekbar is currently at
     */
    public int getProgress() {
        return mProgress;
    }

    /**
     * Persist the seekBar's progress value if callChangeListener
     * returns true, otherwise set the seekBar's progress to the stored value
     */
    void syncProgress(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        if (progress != mProgress) {
            if (callChangeListener(progress)) {
                setProgress(progress, false);
            } else {
                seekBar.setProgress(mProgress);
            }
        }

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && !mTrackingTouch) {
            syncProgress(seekBar);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mTrackingTouch = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mTrackingTouch = false;
        if (seekBar.getProgress() != mProgress) {
            syncProgress(seekBar);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        /*
         * Suppose a client uses this preference type without persisting. We
         * must save the instance state so it is able to, for example, survive
         * orientation changes.
         */

        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        // Save the instance state
        final SavedState myState = new SavedState(superState);
        myState.progress = mProgress;
        myState.max = mMax;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        // Restore the instance state
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        mProgress = myState.progress;
        mMax = myState.max;
        notifyChanged();
    }

    /**
     * SavedState, a subclass of {@link BaseSavedState}, will store the state
     * of MyPreference, a subclass of Preference.
     * <p>
     * It is important to always call through to super methods.
     */
    private static class SavedState extends BaseSavedState {
        int progress;
        int max;

        public SavedState(Parcel source) {
            super(source);

            // Restore the click counter
            progress = source.readInt();
            max = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            // Save the click counter
            dest.writeInt(progress);
            dest.writeInt(max);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}