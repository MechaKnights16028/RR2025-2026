package tester;

import tester.models.*;

import java.util.ArrayList;
import java.util.List;

/**
 * ***********************************************************************
 * CRITICAL: This class contains EXACT COPIES of formulas from:
 * TeamCode/src/main/java/org/firstinspires/ftc/teamcode/vision/LimelightVision.java
 *
 * ANY changes to LimelightVision.java MUST be mirrored here!
 *
 * This duplication is necessary because LimelightVision has FTC SDK dependencies
 * that cannot be used in standalone testing. The formulas are copied line-by-line
 * to ensure testing accuracy.
 *
 * To verify sync: Compare calculateDistance() and calculateAngleRadians() methods.
 * ***********************************************************************
 */
public class RealVisionWrapper {

    // ===== CONSTANTS - MUST MATCH LimelightVision.java =====
    public static final int PIPELINE_APRILTAG = 0;
    public static final int PIPELINE_PURPLE = 1;
    public static final int PIPELINE_GREEN = 2;

    public static final int APRILTAG_LEFT_PILLAR = 20;
    public static final int APRILTAG_RIGHT_PILLAR = 24;
    public static final int APRILTAG_CENTER_START = 21;
    public static final int APRILTAG_CENTER_END = 23;

    // Physical measurements - FROM LimelightVision.java:52-59
    public static final double LIMELIGHT_HEIGHT_INCHES = 8.0;
    public static final double LIMELIGHT_ANGLE_DEGREES = 15.0;
    public static final double APRILTAG_HEIGHT_INCHES = 12.0;

    private final LimelightHttpClient httpClient;
    private int currentPipeline;

    public RealVisionWrapper(LimelightHttpClient httpClient) {
        this.httpClient = httpClient;
        this.currentPipeline = PIPELINE_APRILTAG;
        httpClient.switchPipeline(PIPELINE_APRILTAG);
    }

    public void switchToAprilTagPipeline() {
        if (currentPipeline != PIPELINE_APRILTAG) {
            httpClient.switchPipeline(PIPELINE_APRILTAG);
            currentPipeline = PIPELINE_APRILTAG;
        }
    }

    public void switchToBallPipeline(BallColor color) {
        if (color == BallColor.NONE) {
            throw new IllegalArgumentException("Cannot switch to pipeline for BallColor.NONE");
        }

        int targetPipeline = (color == BallColor.PURPLE) ? PIPELINE_PURPLE : PIPELINE_GREEN;

        if (currentPipeline != targetPipeline) {
            httpClient.switchPipeline(targetPipeline);
            currentPipeline = targetPipeline;
        }
    }

    public int getCurrentPipeline() {
        return currentPipeline;
    }

    /**
     * EXACT COPY of LimelightVision.getPillarTarget()
     * Location: LimelightVision.java:161-215
     */
    public VisionTarget getPillarTarget(boolean isRedAlliance) {
        switchToAprilTagPipeline();

        int targetTagId = isRedAlliance ? APRILTAG_RIGHT_PILLAR : APRILTAG_LEFT_PILLAR;

        LimelightResult result = httpClient.getLatestResult();

        if (result == null || !result.isValid()) {
            return VisionTarget.noTarget();
        }

        List<LimelightResult.FiducialResult> fiducialResults = result.getFiducialResults();

        for (LimelightResult.FiducialResult fiducial : fiducialResults) {
            if (fiducial.getFiducialId() == targetTagId) {
                double tx = fiducial.getTx();
                double ty = fiducial.getTy();
                double ta = fiducial.getTa();

                // EXACT COPY of distance calculation from LimelightVision.java:186-189
                double distance = calculateDistance(ty, APRILTAG_HEIGHT_INCHES,
                                                   LIMELIGHT_HEIGHT_INCHES,
                                                   LIMELIGHT_ANGLE_DEGREES);

                // EXACT COPY of angle calculation from LimelightVision.java:192
                double angleToTarget = calculateAngleRadians(tx);

                // EXACT COPY of normalization from LimelightVision.java:195-196
                double targetX = tx / 29.8;
                double targetY = ty / 24.85;

                return new VisionTarget(
                    TargetType.APRIL_TAG,
                    tx, ty, ta,
                    distance,
                    angleToTarget,
                    targetX, targetY,
                    targetTagId,
                    BallColor.NONE,
                    true,
                    System.currentTimeMillis()
                );
            }
        }

        return VisionTarget.noTarget();
    }

    /**
     * EXACT COPY of LimelightVision.readCenterAprilTag()
     * Location: LimelightVision.java:226-289
     */
    public List<BallColor> readCenterAprilTag() {
        switchToAprilTagPipeline();

        LimelightResult result = httpClient.getLatestResult();

        if (result == null || !result.isValid()) {
            return new ArrayList<>();
        }

        List<LimelightResult.FiducialResult> fiducialResults = result.getFiducialResults();

        for (LimelightResult.FiducialResult fiducial : fiducialResults) {
            int tagId = fiducial.getFiducialId();

            if (tagId >= APRILTAG_CENTER_START && tagId <= APRILTAG_CENTER_END) {
                List<BallColor> ballSequence = new ArrayList<>();

                // EXACT COPY of mapping logic from LimelightVision.java:250-271
                switch (tagId) {
                    case 21:
                        ballSequence.add(BallColor.PURPLE);
                        ballSequence.add(BallColor.PURPLE);
                        ballSequence.add(BallColor.GREEN);
                        break;

                    case 22:
                        ballSequence.add(BallColor.PURPLE);
                        ballSequence.add(BallColor.GREEN);
                        ballSequence.add(BallColor.PURPLE);
                        break;

                    case 23:
                        ballSequence.add(BallColor.GREEN);
                        ballSequence.add(BallColor.PURPLE);
                        ballSequence.add(BallColor.PURPLE);
                        break;
                }

                return ballSequence;
            }
        }

        return new ArrayList<>();
    }

    /**
     * ***********************************************************************
     * EXACT COPY of LimelightVision.calculateDistance()
     * Location: LimelightVision.java:305-322
     *
     * Formula: distance = (targetHeight - limelightHeight) / tan(limelightAngle + ty)
     * ***********************************************************************
     */
    private double calculateDistance(double ty, double targetHeightInches,
                                    double limelightHeightInches,
                                    double limelightAngleDegrees) {
        // Formula: distance = (targetHeight - limelightHeight) / tan(limelightAngle + ty)
        double angleToTargetDegrees = limelightAngleDegrees + ty;
        double angleToTargetRadians = Math.toRadians(angleToTargetDegrees);

        // Handle edge cases to prevent divide by zero or extreme values
        if (Math.abs(angleToTargetRadians) < 0.01) {
            return 1000.0; // Return large distance if angle is nearly horizontal
        }

        double heightDifference = targetHeightInches - limelightHeightInches;
        double distance = heightDifference / Math.tan(angleToTargetRadians);

        // Clamp to reasonable range
        return Math.max(0.0, Math.min(distance, 200.0));
    }

    /**
     * EXACT COPY of LimelightVision.calculateAngleRadians()
     * Location: LimelightVision.java:330-332
     */
    private double calculateAngleRadians(double tx) {
        return Math.toRadians(tx);
    }

    public boolean hasTarget() {
        LimelightResult result = httpClient.getLatestResult();
        return result != null && result.isValid();
    }
}
