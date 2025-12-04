package org.firstinspires.ftc.teamcode.vision;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.LLStatus;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.ArrayList;
import java.util.List;

/**
 * Main vision class that interfaces with the Limelight3A camera.
 * Handles AprilTag detection for pillar targeting and center tag reading,
 * as well as colored ball detection for autonomous ball collection.
 *
 * Pipeline Configuration:
 * - Pipeline 0: Purple ball color detection
 * - Pipeline 1: Green ball color detection
 * - Pipeline 2: Pillar AprilTag detection (tags 20, 24 ONLY)
 * - Pipeline 3: Center AprilTag detection (tags 21, 22, 23 ONLY)
 */
public class LimelightVision {

    // ===== PIPELINE CONSTANTS =====
    /** Pipeline index for purple ball detection */
    public static final int PIPELINE_PURPLE = 0;

    /** Pipeline index for green ball detection */
    public static final int PIPELINE_GREEN = 1;

    /** Pipeline index for pillar AprilTag detection (tags 20 and 24 ONLY) */
    public static final int PIPELINE_PILLAR_TAGS = 2;

    /** Pipeline index for center AprilTag detection (tags 21, 22, 23 ONLY) */
    public static final int PIPELINE_CENTER_TAGS = 3;

    // ===== APRILTAG ID CONSTANTS =====
    /** AprilTag ID for left pillar (Blue alliance) */
    public static final int APRILTAG_LEFT_PILLAR = 20;

    /** AprilTag ID for right pillar (Red alliance) */
    public static final int APRILTAG_RIGHT_PILLAR = 24;

    /** First center AprilTag ID (determines ball sequence) */
    public static final int APRILTAG_CENTER_START = 21;

    /** Last center AprilTag ID (determines ball sequence) */
    public static final int APRILTAG_CENTER_END = 23;

    // ===== PHYSICAL MEASUREMENT CONSTANTS =====
    // TODO: THESE MUST BE CALIBRATED TO YOUR ROBOT!

    /** Height of Limelight camera from floor in inches */
    public static final double LIMELIGHT_HEIGHT_INCHES = 40.0;

    /** Angle of Limelight camera tilt in degrees (positive = angled up) */
    public static final double LIMELIGHT_ANGLE_DEGREES = 15.0;

    /** Height of AprilTag center from floor in inches */
    public static final double APRILTAG_HEIGHT_INCHES = 36.0;

    /** Diameter of game balls in inches */
    public static final double BALL_DIAMETER_INCHES = 3.0;

    // ===== INSTANCE FIELDS =====
    /** The Limelight3A hardware device */
    private final Limelight3A limelight;

    /** Device name from hardware configuration */
    private final String limelightName;

    /** Currently active pipeline index */
    private int currentPipeline;

    /** Ball collection sequence determined from center AprilTag */
    private List<BallColor> ballSequence;

    /** Current index in ball sequence */
    private int currentBallIndex;

    /** Telemetry for debugging output */
    private final Telemetry telemetry;

    /**
     * Creates a new LimelightVision instance.
     *
     * @param hardwareMap The robot's hardware map
     * @param name The name of the Limelight in the robot configuration
     * @param telemetry Telemetry for debug output
     */
    public LimelightVision(HardwareMap hardwareMap, String name, Telemetry telemetry) {
        this.limelightName = name;
        this.telemetry = telemetry;
        this.ballSequence = new ArrayList<>();
        this.currentBallIndex = 0;
        this.currentPipeline = PIPELINE_PILLAR_TAGS;

        // Initialize Limelight hardware
        this.limelight = hardwareMap.get(Limelight3A.class, name);

        // Set initial pipeline to pillar AprilTag detection
        limelight.pipelineSwitch(PIPELINE_PILLAR_TAGS);

        // Start Limelight polling
        limelight.start();

        telemetry.addData("LimelightVision", "Initialized on pipeline " + PIPELINE_PILLAR_TAGS);
    }

    // ===== PIPELINE SWITCHING METHODS =====

    /**
     * Switches the Limelight to the pillar AprilTag detection pipeline.
     * Use this when detecting pillar tags (20, 24) for alliance-based targeting.
     */
    public void switchToPillarTagPipeline() {
        if (currentPipeline != PIPELINE_PILLAR_TAGS) {
            limelight.pipelineSwitch(PIPELINE_PILLAR_TAGS);
            currentPipeline = PIPELINE_PILLAR_TAGS;
            telemetry.addData("LimelightVision", "Switched to pillar AprilTag pipeline");
        }
    }

    /**
     * Switches the Limelight to the center AprilTag detection pipeline.
     * Use this when reading center tags (21, 22, 23) for ball sequence detection.
     */
    public void switchToCenterTagPipeline() {
        if (currentPipeline != PIPELINE_CENTER_TAGS) {
            limelight.pipelineSwitch(PIPELINE_CENTER_TAGS);
            currentPipeline = PIPELINE_CENTER_TAGS;
            telemetry.addData("LimelightVision", "Switched to center AprilTag pipeline");
        }
    }

    /**
     * Switches the Limelight to the appropriate ball color detection pipeline.
     *
     * @param color The ball color to detect (PURPLE or GREEN)
     * @throws IllegalArgumentException if color is NONE
     */
    public void switchToBallPipeline(BallColor color) {
        if (color == BallColor.NONE) {
            throw new IllegalArgumentException("Cannot switch to pipeline for BallColor.NONE");
        }

        int targetPipeline = (color == BallColor.PURPLE) ? PIPELINE_PURPLE : PIPELINE_GREEN;

        if (currentPipeline != targetPipeline) {
            limelight.pipelineSwitch(targetPipeline);
            currentPipeline = targetPipeline;
            telemetry.addData("LimelightVision", "Switched to " + color + " ball pipeline");
        }
    }

    /**
     * Gets the currently active pipeline index.
     *
     * @return The current pipeline index (0=Purple, 1=Green, 2=PillarTags, 3=CenterTags)
     */
    public int getCurrentPipeline() {
        return currentPipeline;
    }

    // ===== APRILTAG DETECTION METHODS =====

    /**
     * Detects and targets the pillar AprilTag based on alliance color.
     * Red alliance targets tag 24 (right pillar), Blue alliance targets tag 20 (left pillar).
     *
     * @param isRedAlliance true for red alliance (tag 24), false for blue alliance (tag 20)
     * @return VisionTarget containing tag data, or noTarget() if tag not visible
     */
    public VisionTarget getPillarTarget(boolean isRedAlliance) {
        // Switch to pillar AprilTag pipeline
        switchToPillarTagPipeline();

        // Determine which tag to look for
        int targetTagId = isRedAlliance ? APRILTAG_RIGHT_PILLAR : APRILTAG_LEFT_PILLAR;

        // Get latest Limelight results
        LLResult result = limelight.getLatestResult();

        if (result == null || !result.isValid()) {
            return VisionTarget.noTarget();
        }

        // Get fiducial (AprilTag) results
        List<LLResultTypes.FiducialResult> fiducialResults = result.getFiducialResults();

        // Search for our target tag
        for (LLResultTypes.FiducialResult fiducial : fiducialResults) {
            if (fiducial.getFiducialId() == targetTagId) {
                // Found our target tag!
                double tx = fiducial.getTargetXDegrees();
                double ty = fiducial.getTargetYDegrees();
                double ta = fiducial.getTargetArea();

                // Calculate distance using tag height
                double distance = calculateDistance(ty, APRILTAG_HEIGHT_INCHES,
                                                   LIMELIGHT_HEIGHT_INCHES,
                                                   LIMELIGHT_ANGLE_DEGREES);

                // Calculate angle to target
                double angleToTarget = calculateAngleRadians(tx);

                // Normalize screen coordinates (-1 to 1)
                double targetX = tx / 29.8;  // Limelight FOV is ±29.8 degrees horizontal
                double targetY = ty / 24.85; // Limelight FOV is ±24.85 degrees vertical

                // Create and return VisionTarget
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

        // Tag not found
        return VisionTarget.noTarget();
    }

    /**
     * Reads the center AprilTag (21-23) to determine the ball collection sequence.
     * Maps tag IDs to ball collection order:
     * - Tag 21: [GREEN, PURPLE, PURPLE]
     * - Tag 22: [PURPLE, GREEN, PURPLE]
     * - Tag 23: [PURPLE, PURPLE, GREEN]
     *
     * @return List of BallColor in collection order, or empty list if no center tag found
     */
    public List<BallColor> readCenterAprilTag() {
        // Switch to center AprilTag pipeline
        switchToCenterTagPipeline();

        // Get latest Limelight results
        LLResult result = limelight.getLatestResult();

        if (result == null || !result.isValid()) {
            ballSequence.clear();
            currentBallIndex = 0;
            return new ArrayList<>();
        }

        // Get fiducial (AprilTag) results
        List<LLResultTypes.FiducialResult> fiducialResults = result.getFiducialResults();

        // Search for center tags (21-23)
        for (LLResultTypes.FiducialResult fiducial : fiducialResults) {
            int tagId = fiducial.getFiducialId();

            // Check if this is a center tag
            if (tagId >= APRILTAG_CENTER_START && tagId <= APRILTAG_CENTER_END) {
                // Map tag ID to ball sequence
                ballSequence.clear();
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

                // Reset ball index to start of sequence
                currentBallIndex = 0;

                // Log to telemetry
                telemetry.addData("Center Tag", "Found tag " + tagId);
                telemetry.addData("Ball Sequence", ballSequence.toString());

                return new ArrayList<>(ballSequence);
            }
        }

        // No center tag found
        ballSequence.clear();
        currentBallIndex = 0;
        telemetry.addData("Center Tag", "Not found");
        return new ArrayList<>();
    }

    // ===== BALL DETECTION METHODS =====
    // To be implemented in Tasks 8-9

    // ===== HELPER METHODS =====

    /**
     * Calculates the distance to a target using Limelight geometry.
     *
     * @param ty Vertical offset from crosshair in degrees
     * @param targetHeightInches Height of target from floor
     * @param limelightHeightInches Height of Limelight from floor
     * @param limelightAngleDegrees Mounting angle of Limelight (positive = angled up)
     * @return Distance to target in inches
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
     * Converts horizontal offset to angle in radians.
     *
     * @param tx Horizontal offset in degrees
     * @return Angle to target in radians
     */
    private double calculateAngleRadians(double tx) {
        return Math.toRadians(tx);
    }

    /**
     * Checks if the Limelight currently has a valid target.
     *
     * @return true if a target is detected
     */
    public boolean hasTarget() {
        LLResult result = limelight.getLatestResult();
        return result != null && result.isValid();
    }

    /**
     * Updates telemetry with current vision data for debugging.
     */
    public void updateTelemetry() {
        LLStatus status = limelight.getStatus();
        telemetry.addData("Limelight Pipeline", currentPipeline);
        telemetry.addData("Limelight FPS", String.format("%.0f", status.getFps()));

        LLResult result = limelight.getLatestResult();
        if (result != null && result.isValid()) {
            telemetry.addData("Target Found", "YES");
            telemetry.addData("tx", String.format("%.2f", result.getTx()));
            telemetry.addData("ty", String.format("%.2f", result.getTy()));
            telemetry.addData("ta", String.format("%.2f", result.getTa()));
        } else {
            telemetry.addData("Target Found", "NO");
        }
    }

    /**
     * Stops the Limelight polling.
     * Call this when the OpMode is stopping.
     */
    public void stop() {
        limelight.stop();
    }
}
