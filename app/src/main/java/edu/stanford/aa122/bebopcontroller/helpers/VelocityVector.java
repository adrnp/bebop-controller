package edu.stanford.aa122.bebopcontroller.helpers;

/**
 * Class for holding velocity vector.
 *
 * @author Adrien Perkins <adrienp@stanford.edu>
 */
public class VelocityVector {
    /** northwrd velocity [m/s] */
    public float north;

    /** eastward velocity [m/s] */
    public float east;

    /** down velocity [m/s] */
    public float down;


    /**
     * Constructor for velocity vector given a set of velocity components.
     * @param vn northward velocity [m/s]
     * @param ve eastward velocity [m/s]
     * @param vd downward velocity [m/s]
     */
    public VelocityVector(float vn, float ve, float vd) {
        north = vn;
        east = ve;
        down = vd;
    }
}
