package org.firstinspires.ftc.teamcode.vision;

/**
 * Data class that encapsulates all vision target information from the Limelight.
 * Contains both raw Limelight data (tx, ty, ta) and calculated values (distance, angle).
 */
public class VisionTarget {
    private final TargetType type;
    private final double tx;              // Horizontal offset in degrees
    private final double ty;              // Vertical offset in degrees
    private final double ta;              // Target area as % of image
    private final double distance;        // Calculated distance to target (inches)
    private final double angleToTarget;   // Angle in radians to face target
    private final double targetX;         // Normalized screen X coordinate (-1 to 1)
    private final double targetY;         // Normalized screen Y coordinate (-1 to 1)
    private final int aprilTagId;         // AprilTag ID if applicable, -1 otherwise
    private final BallColor ballColor;    // Ball color if applicable, NONE otherwise
    private final boolean targetFound;    // True if target is visible
    private final long timestamp;         // System time when data was captured (ms)

    /**
     * Creates a new VisionTarget with all fields.
     *
     * @param type Type of target (APRIL_TAG, BALL, or NONE)
     * @param tx Horizontal offset in degrees (-29.8 to 29.8)
     * @param ty Vertical offset in degrees (-24.85 to 24.85)
     * @param ta Target area as percentage of image (0-100)
     * @param distance Calculated distance to target in inches
     * @param angleToTarget Angle in radians to face the target
     * @param targetX Normalized screen X coordinate (-1 to 1)
     * @param targetY Normalized screen Y coordinate (-1 to 1)
     * @param aprilTagId AprilTag ID (20-24) or -1 if not an AprilTag
     * @param ballColor Ball color (PURPLE, GREEN) or NONE if not a ball
     * @param targetFound True if target is currently visible
     * @param timestamp System time in milliseconds when data was captured
     */
    public VisionTarget(TargetType type, double tx, double ty, double ta,
                       double distance, double angleToTarget, double targetX,
                       double targetY, int aprilTagId, BallColor ballColor,
                       boolean targetFound, long timestamp) {
        this.type = type;
        this.tx = tx;
        this.ty = ty;
        this.ta = ta;
        this.distance = distance;
        this.angleToTarget = angleToTarget;
        this.targetX = targetX;
        this.targetY = targetY;
        this.aprilTagId = aprilTagId;
        this.ballColor = ballColor;
        this.targetFound = targetFound;
        this.timestamp = timestamp;
    }

    /**
     * Factory method to create a VisionTarget indicating no target was found.
     *
     * @return A VisionTarget with targetFound=false and default values
     */
    public static VisionTarget noTarget() {
        return new VisionTarget(
            TargetType.NONE,
            0.0, 0.0, 0.0,           // tx, ty, ta
            0.0, 0.0,                // distance, angle
            0.0, 0.0,                // targetX, targetY
            -1,                      // aprilTagId
            BallColor.NONE,          // ballColor
            false,                   // targetFound
            System.currentTimeMillis() // timestamp
        );
    }

    // Getters

    /**
     * @return The type of target (APRIL_TAG, BALL, or NONE)
     */
    public TargetType getType() {
        return type;
    }

    /**
     * @return Horizontal offset in degrees (-29.8 to 29.8)
     */
    public double getTx() {
        return tx;
    }

    /**
     * @return Vertical offset in degrees (-24.85 to 24.85)
     */
    public double getTy() {
        return ty;
    }

    /**
     * @return Target area as percentage of image (0-100)
     */
    public double getTa() {
        return ta;
    }

    /**
     * @return Calculated distance to target in inches
     */
    public double getDistance() {
        return distance;
    }

    /**
     * @return Angle in radians to face the target
     */
    public double getAngleToTarget() {
        return angleToTarget;
    }

    /**
     * @return Normalized screen X coordinate (-1 to 1)
     */
    public double getTargetX() {
        return targetX;
    }

    /**
     * @return Normalized screen Y coordinate (-1 to 1)
     */
    public double getTargetY() {
        return targetY;
    }

    /**
     * @return AprilTag ID (20-24) or -1 if not an AprilTag
     */
    public int getAprilTagId() {
        return aprilTagId;
    }

    /**
     * @return Ball color (PURPLE, GREEN) or NONE if not a ball
     */
    public BallColor getBallColor() {
        return ballColor;
    }

    /**
     * @return True if target is currently visible
     */
    public boolean isTargetFound() {
        return targetFound;
    }

    /**
     * @return System time in milliseconds when data was captured
     */
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        if (!targetFound) {
            return "VisionTarget{NO TARGET FOUND}";
        }

        StringBuilder sb = new StringBuilder("VisionTarget{");
        sb.append("type=").append(type);

        if (type == TargetType.APRIL_TAG) {
            sb.append(", tagId=").append(aprilTagId);
        } else if (type == TargetType.BALL) {
            sb.append(", color=").append(ballColor);
        }

        sb.append(", tx=").append(String.format("%.2f", tx));
        sb.append(", ty=").append(String.format("%.2f", ty));
        sb.append(", ta=").append(String.format("%.2f", ta));
        sb.append(", distance=").append(String.format("%.2f", distance));
        sb.append(", angle=").append(String.format("%.3f", angleToTarget));
        sb.append("}");

        return sb.toString();
    }
}
