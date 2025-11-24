package tester.models;

/**
 * COPY of org.firstinspires.ftc.teamcode.vision.VisionTarget
 * Must stay synchronized with TeamCode version.
 */
public class VisionTarget {
    private final TargetType type;
    private final double tx;
    private final double ty;
    private final double ta;
    private final double distance;
    private final double angleToTarget;
    private final double targetX;
    private final double targetY;
    private final int aprilTagId;
    private final BallColor ballColor;
    private final boolean targetFound;
    private final long timestamp;

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

    public static VisionTarget noTarget() {
        return new VisionTarget(
            TargetType.NONE,
            0.0, 0.0, 0.0,
            0.0, 0.0,
            0.0, 0.0,
            -1,
            BallColor.NONE,
            false,
            System.currentTimeMillis()
        );
    }

    // Getters
    public TargetType getType() { return type; }
    public double getTx() { return tx; }
    public double getTy() { return ty; }
    public double getTa() { return ta; }
    public double getDistance() { return distance; }
    public double getAngleToTarget() { return angleToTarget; }
    public double getTargetX() { return targetX; }
    public double getTargetY() { return targetY; }
    public int getAprilTagId() { return aprilTagId; }
    public BallColor getBallColor() { return ballColor; }
    public boolean isTargetFound() { return targetFound; }
    public long getTimestamp() { return timestamp; }
}
