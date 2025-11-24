package tester.models;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents the JSON response from the Limelight HTTP API.
 * Corresponds to the structure returned by http://limelight.local:5807/results
 */
public class LimelightResult {
    // Root-level fields (actual Limelight JSON field names)
    private int v;            // Valid target (0 or 1)
    private double tx;        // Horizontal offset in degrees
    private double ty;        // Vertical offset in degrees
    private double ta;        // Target area percentage
    private double pID;       // Pipeline ID (0.0, 1.0, 2.0)
    private String pTYPE;     // Pipeline type ("pipe_apriltag", "pipe_color", etc.)

    // Fiducial (AprilTag) results
    private List<FiducialResult> Fiducial;

    public LimelightResult() {
        this.Fiducial = new ArrayList<>();
    }

    // Getters
    public boolean isValid() { return v == 1; }
    public double getTx() { return tx; }
    public double getTy() { return ty; }
    public double getTa() { return ta; }
    public int getPipeline() { return (int) pID; }
    public List<FiducialResult> getFiducialResults() { return Fiducial; }

    /**
     * Represents a single AprilTag detection result.
     */
    public static class FiducialResult {
        private int fID;      // Fiducial/AprilTag ID
        private double tx;    // Horizontal offset in degrees
        private double ty;    // Vertical offset in degrees
        private double ta;    // Target area percentage
        private double txp;   // Normalized X position (-1 to 1)
        private double typ;   // Normalized Y position (-1 to 1)

        public int getFiducialId() { return fID; }
        public double getTx() { return tx; }
        public double getTy() { return ty; }
        public double getTa() { return ta; }
        public double getTxp() { return txp; }
        public double getTyp() { return typ; }
    }
}
