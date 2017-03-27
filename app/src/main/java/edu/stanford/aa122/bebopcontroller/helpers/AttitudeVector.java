package edu.stanford.aa122.bebopcontroller.helpers;

/**
 * Class for holding attitude vector.
 *
 * @author Adrien Perkins <adrienp@stanford.edu>
 */
public class AttitudeVector {
    /** roll [deg] */
    public float roll;

    /** pitch [deg] */
    public float pitch;

    /** yw [deg] */
    public float yaw;

    /**
     * Constructor for the attitude vector given a set of velocity components.
     * @param roll roll in degrees
     * @param pitch pitch in degrees
     * @param yaw yaw in degrees
     */
    public AttitudeVector(float roll, float pitch, float yaw) {
        this.roll = roll;
        this.pitch = pitch;
        this.yaw = yaw;
    }
}
