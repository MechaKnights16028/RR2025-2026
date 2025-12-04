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
    /** Pipeline index for purple ball detection */
    public static final int PIPELINE_PURPLE = 0;

    /** Pipeline index for green ball detection */
    public static final int PIPELINE_GREEN = 1;

    /** Pipeline index for pillar AprilTag detection (tags 20 and 24 ONLY) */
    public static final int PIPELINE_PILLAR_TAGS = 2;

    /** Pipeline index for center AprilTag detection (tags 21, 22, 23 ONLY) */
    public static final int PIPELINE_CENTER_TAGS = 3;

    public static final int APRILTAG_LEFT_PILLAR = 20;
    public static final int APRILTAG_RIGHT_PILLAR = 24;
    public static final int APRILTAG_CENTER_START = 21;
    public static final int APRILTAG_CENTER_END = 23;

    // Physical measurements - FROM LimelightVision.java:56-63
    /** Height of Limelight camera from floor in inches */
    public static final double LIMELIGHT_HEIGHT_INCHES = 40.0;

    /** Angle of Limelight camera tilt in degrees (positive = angled up) */
    public static final double LIMELIGHT_ANGLE_DEGREES = 15.0;

    /** Height of AprilTag center from floor in inches */
    public static final double APRILTAG_HEIGHT_INCHES = 36.0;

    private final LimelightHttpClient httpClient;
    private int currentPipeline;

    public RealVisionWrapper(LimelightHttpClient httpClient) {
        this.httpClient = httpClient;
        this.currentPipeline = PIPELINE_PILLAR_TAGS;
        httpClient.switchPipeline(PIPELINE_PILLAR_TAGS);
    }

    /**
     * Switches the Limelight to the pillar AprilTag detection pipeline.
     * Use this when detecting pillar tags (20, 24) for alliance-based targeting.
     */
    public void switchToPillarTagPipeline() {
        if (currentPipeline != PIPELINE_PILLAR_TAGS) {
            httpClient.switchPipeline(PIPELINE_PILLAR_TAGS);
            currentPipeline = PIPELINE_PILLAR_TAGS;
        }
    }

    /**
     * Switches the Limelight to the center AprilTag detection pipeline.
     * Use this when reading center tags (21, 22, 23) for ball sequence detection.
     */
    public void switchToCenterTagPipeline() {
        if (currentPipeline != PIPELINE_CENTER_TAGS) {
            httpClient.switchPipeline(PIPELINE_CENTER_TAGS);
            currentPipeline = PIPELINE_CENTER_TAGS;
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
     * Location: LimelightVision.java:177-231
     */
    public VisionTarget getPillarTarget(boolean isRedAlliance) {
        switchToPillarTagPipeline();

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
     * Maps tag IDs to ball collection order:
     * - Tag 21: [GREEN, PURPLE, PURPLE]
     * - Tag 22: [PURPLE, GREEN, PURPLE]
     * - Tag 23: [PURPLE, PURPLE, GREEN]
     * Location: LimelightVision.java:233-305
     */
    public List<BallColor> readCenterAprilTag() {
        switchToCenterTagPipeline();

        LimelightResult result = httpClient.getLatestResult();

        if (result == null || !result.isValid()) {
            return new ArrayList<>();
        }

        List<LimelightResult.FiducialResult> fiducialResults = result.getFiducialResults();

        for (LimelightResult.FiducialResult fiducial : fiducialResults) {
            int tagId = fiducial.getFiducialId();

            if (tagId >= APRILTAG_CENTER_START && tagId <= APRILTAG_CENTER_END) {
                List<BallColor> ballSequence = new ArrayList<>();

                // EXACT COPY of mapping logic from LimelightVision.java:266-287
                switch (tagId) {
                    case 21:
                        // Tag 21: Green, Purple, Purple
                        ballSequence.add(BallColor.GREEN);
                        ballSequence.add(BallColor.PURPLE);
                        ballSequence.add(BallColor.PURPLE);
                        break;

                    case 22:
                        // Tag 22: Purple, Green, Purple
                        ballSequence.add(BallColor.PURPLE);
                        ballSequence.add(BallColor.GREEN);
                        ballSequence.add(BallColor.PURPLE);
                        break;

                    case 23:
                        // Tag 23: Purple, Purple, Green
                        ballSequence.add(BallColor.PURPLE);
                        ballSequence.add(BallColor.PURPLE);
                        ballSequence.add(BallColor.GREEN);
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
     * Location: LimelightVision.java:321-338
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
     * Location: LimelightVision.java:346-348
     */
    private double calculateAngleRadians(double tx) {
        return Math.toRadians(tx);
    }

    public boolean hasTarget() {
        LimelightResult result = httpClient.getLatestResult();
        return result != null && result.isValid();
    }
}
