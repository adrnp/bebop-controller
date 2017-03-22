package edu.stanford.aa122.bebopcontroller.helpers;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Helper class for logging all of the important data from the Bebop drone to file.
 *
 * Note: implements a bebopdrone listener to be able to know when all the things happen
 *
 * @author Adrien Perkins <adrienp@stanford.edu>
 */
public class DataLogger implements BebopDrone.Listener {

    /** helpful constants */
    private static final String FILE_PREFIX = "bebop";
    private static final String COMMENT_START = "# ";
    private static final char RECORD_DELIMITER = ',';

    /** context of the calling activity */
    private Context mContext;

    /** whether or not we should be logging to file */
    private boolean mLogging = false;

    /** objects needed for file handling and writing */
    private final Object mFileLock = new Object();
    private BufferedWriter mFileWriter;
    private File mFile;

    /**
     * Constructor
     * @param context  context containing this instance
     */
    public DataLogger(Context context) {
        mContext = context;
    }


    /**
     * Start the logging process from nothing.
     * Creates a new logfile and adds any desired header information to the file.
     */
    public void startNewLog() {

        // create the file and set it up to be global
        synchronized (mFileLock) {

            // get the directory to place the file
            // TODO: replace this with something a lot easier... (find on the internet!!)
            File baseDirectory;
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                baseDirectory = new File(Environment.getExternalStorageDirectory(), FILE_PREFIX);
                baseDirectory.mkdirs();
            } else {
                return;
            }

            // name, create, and open the file
            // TODO: figure out the desired naming scheme for my files
            SimpleDateFormat formatter = new SimpleDateFormat("yyy_MM_dd_HH_mm_ss");
            Date now = new Date();
            String fileName = String.format("%s_log_%s.txt", FILE_PREFIX, formatter.format(now));
            File currentFile = new File(baseDirectory, fileName);
            String currentFilePath = currentFile.getAbsolutePath();
            BufferedWriter currentFileWriter;
            try {
                currentFileWriter = new BufferedWriter(new FileWriter(currentFile));
            } catch (IOException e) {
                // unable to open the file
                return;
            }

            // initialize the contents of the file
            try {
                currentFileWriter.write(COMMENT_START);
                currentFileWriter.newLine();
                currentFileWriter.write(COMMENT_START);
                currentFileWriter.write("Bebop Drone Log Test");
                currentFileWriter.newLine();
                currentFileWriter.write(COMMENT_START);
                currentFileWriter.newLine();

            } catch (IOException e) {
                // for whatever reason this didn't work
                return;
            }

            // if for some reason we have an existing file open, make sure to close it
            if (mFileWriter != null) {
                try {
                    mFileWriter.close();
                } catch (IOException e) {
                    // couldn't close the file
                    return;
                }
            }

            // make references to file "global" for the class
            mFile = currentFile;
            mFileWriter = currentFileWriter;
            Toast.makeText(mContext, "File opened: " + currentFilePath, Toast.LENGTH_SHORT).show();

            // set boolean to the fact that we should be logging
            mLogging = true;
        }

    }

    // TODO: decide if this is necessary
    public void pauseLogging() {
        mLogging = false;
    }

    // TODO: decide if this is necessary
    public void restartLogging() {
        mLogging = true;
    }

    /**
     * Stop the logging process and close the file to which we are logging.
     */
    public void stopLogging() {
        mLogging = false;
        synchronized (mFileLock) {
            if (mFileWriter != null) {
                try {
                    mFileWriter.close();
                    mFileWriter = null;
                } catch (IOException e) {
                    // unable to close the file for some reason
                }
            }
        }
    }

    // TODO: setup the file for logging
    // TODO: add everything needed for async handling of the file


    /* Listener methods below */


    @Override
    public void onDroneConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {

    }

    @Override
    public void onBatteryChargeChanged(int batteryPercentage) {

    }

    @Override
    public void onPilotingStateChanged(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {

    }

    @Override
    public void onPictureTaken(ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error) {

    }

    @Override
    public void configureDecoder(ARControllerCodec codec) {

    }

    @Override
    public void onFrameReceived(ARFrame frame) {

    }

    @Override
    public void onMatchingMediasFound(int nbMedias) {

    }

    @Override
    public void onDownloadProgressed(String mediaName, int progress) {

    }

    @Override
    public void onDownloadComplete(String mediaName) {

    }
}
