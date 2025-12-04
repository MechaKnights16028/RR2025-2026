package org.firstinspires.ftc.teamcode.vision;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter that supports dual connection modes for Limelight:
 * 1. HardwareMap mode - Limelight connected directly to Robot Controller Hub via USB
 * 2. HTTP mode - Limelight connected to laptop/network, accessed via HTTP API
 *
 * The adapter tries HardwareMap first, then falls back to HTTP if that fails.
 * This allows testing with Limelight connected to either location.
 */
public class LimelightVisionAdapter {

    public enum ConnectionMode {
        HARDWARE_MAP,  // Direct USB connection to Robot Controller
        HTTP_NETWORK,  // Network connection via HTTP API
        DISCONNECTED   // No connection available
    }

    private ConnectionMode connectionMode;
    private LimelightVision hardwareVision;
    private LimelightHttpClient httpClient;
    private int currentPipeline;
    private final Telemetry telemetry;

    /**
     * Creates adapter and attempts to connect via HardwareMap first, then HTTP
     *
     * @param hardwareMap Robot hardware map
     * @param deviceName Limelight device name in hardware config
     * @param telemetry Telemetry for debug output
     */
    public LimelightVisionAdapter(HardwareMap hardwareMap, String deviceName, Telemetry telemetry) {
        this.telemetry = telemetry;
        this.currentPipeline = LimelightVision.PIPELINE_PILLAR_TAGS;
        this.connectionMode = ConnectionMode.DISCONNECTED;

        // Try HardwareMap first (direct USB connection)
        try {
            this.hardwareVision = new LimelightVision(hardwareMap, deviceName, telemetry);
            this.connectionMode = ConnectionMode.HARDWARE_MAP;
            telemetry.addData("Connection", "HardwareMap (USB)");
            return;
        } catch (Exception e) {
            // HardwareMap failed, try HTTP
            telemetry.addData("HardwareMap", "Not available - trying HTTP...");
        }

        // Try HTTP connection (network mode)
        try {
            this.httpClient = new LimelightHttpClient();
            if (httpClient.testConnection()) {
                this.connectionMode = ConnectionMode.HTTP_NETWORK;
                telemetry.addData("Connection", "HTTP Network (limelight.local:5807)");
            } else {
                throw new RuntimeException("HTTP connection test failed");
            }
        } catch (Exception e) {
            // Both modes failed
            this.connectionMode = ConnectionMode.DISCONNECTED;
            telemetry.addData("Connection", "FAILED - No Limelight found");
            throw new IllegalArgumentException("Limelight not accessible via HardwareMap or HTTP");
        }
    }

    /**
     * Gets the current connection mode
     */
    public ConnectionMode getConnectionMode() {
        return connectionMode;
    }

    /**
     * Gets a pillar target based on alliance color
     */
    public VisionTarget getPillarTarget(boolean isRedAlliance) {
        if (connectionMode == ConnectionMode.HARDWARE_MAP) {
            return hardwareVision.getPillarTarget(isRedAlliance);
        } else if (connectionMode == ConnectionMode.HTTP_NETWORK) {
            return getPillarTargetHttp(isRedAlliance);
        }
        return VisionTarget.noTarget();
    }

    /**
     * Reads center AprilTag to determine ball sequence
     */
    public List<BallColor> readCenterAprilTag() {
        if (connectionMode == ConnectionMode.HARDWARE_MAP) {
            return hardwareVision.readCenterAprilTag();
        } else if (connectionMode == ConnectionMode.HTTP_NETWORK) {
            return readCenterAprilTagHttp();
        }
        return new ArrayList<>();
    }

    /**
     * Switches to pillar AprilTag detection pipeline
     */
    public void switchToPillarTagPipeline() {
        if (connectionMode == ConnectionMode.HARDWARE_MAP) {
            hardwareVision.switchToPillarTagPipeline();
        } else if (connectionMode == ConnectionMode.HTTP_NETWORK) {
            httpClient.switchPipeline(LimelightVision.PIPELINE_PILLAR_TAGS);
            currentPipeline = LimelightVision.PIPELINE_PILLAR_TAGS;
        }
    }

    /**
     * Switches to center AprilTag detection pipeline
     */
    public void switchToCenterTagPipeline() {
        if (connectionMode == ConnectionMode.HARDWARE_MAP) {
            hardwareVision.switchToCenterTagPipeline();
        } else if (connectionMode == ConnectionMode.HTTP_NETWORK) {
            httpClient.switchPipeline(LimelightVision.PIPELINE_CENTER_TAGS);
            currentPipeline = LimelightVision.PIPELINE_CENTER_TAGS;
        }
    }

    /**
     * Switches to ball color detection pipeline
     */
    public void switchToBallPipeline(BallColor color) {
        if (connectionMode == ConnectionMode.HARDWARE_MAP) {
            hardwareVision.switchToBallPipeline(color);
        } else if (connectionMode == ConnectionMode.HTTP_NETWORK) {
            int pipeline = (color == BallColor.PURPLE) ?
                LimelightVision.PIPELINE_PURPLE : LimelightVision.PIPELINE_GREEN;
            httpClient.switchPipeline(pipeline);
            currentPipeline = pipeline;
        }
    }

    /**
     * Gets current pipeline index
     */
    public int getCurrentPipeline() {
        if (connectionMode == ConnectionMode.HARDWARE_MAP) {
            return hardwareVision.getCurrentPipeline();
        } else if (connectionMode == ConnectionMode.HTTP_NETWORK) {
            // Try to get from HTTP, fall back to tracked value
            LimelightHttpClient.LimelightResult result = httpClient.getResults();
            if (result.valid) {
                currentPipeline = result.currentPipeline;
            }
            return currentPipeline;
        }
        return 0;
    }

    /**
     * Checks if a target is currently detected
     */
    public boolean hasTarget() {
        if (connectionMode == ConnectionMode.HARDWARE_MAP) {
            return hardwareVision.hasTarget();
        } else if (connectionMode == ConnectionMode.HTTP_NETWORK) {
            LimelightHttpClient.LimelightResult result = httpClient.getResults();
            return result.valid && (!result.fiducials.isEmpty() || !result.colors.isEmpty());
        }
        return false;
    }

    /**
     * Stops the Limelight
     */
    public void stop() {
        if (connectionMode == ConnectionMode.HARDWARE_MAP && hardwareVision != null) {
            hardwareVision.stop();
        }
        // HTTP mode doesn't need cleanup
    }

    // ===== HTTP MODE IMPLEMENTATIONS =====

    /**
     * Gets pillar target via HTTP
     */
    private VisionTarget getPillarTargetHttp(boolean isRedAlliance) {
        int targetTagId = isRedAlliance ?
            LimelightVision.APRILTAG_RIGHT_PILLAR : LimelightVision.APRILTAG_LEFT_PILLAR;

        LimelightHttpClient.LimelightResult result = httpClient.getResults();

        if (!result.valid || result.fiducials.isEmpty()) {
            return VisionTarget.noTarget();
        }

        // Find target tag
        for (LimelightHttpClient.FiducialDetection fiducial : result.fiducials) {
            if (fiducial.fiducialId == targetTagId) {
                // Calculate distance using same formula as LimelightVision
                double distance = calculateDistance(
                    fiducial.ty,
                    LimelightVision.APRILTAG_HEIGHT_INCHES,
                    LimelightVision.LIMELIGHT_HEIGHT_INCHES,
                    LimelightVision.LIMELIGHT_ANGLE_DEGREES
                );

                double angleToTarget = Math.toRadians(fiducial.tx);
                double targetX = fiducial.tx / 29.8;
                double targetY = fiducial.ty / 24.85;

                return new VisionTarget(
                    TargetType.APRIL_TAG,
                    fiducial.tx, fiducial.ty, fiducial.ta,
                    distance,
                    angleToTarget,
                    targetX, targetY,
                    fiducial.fiducialId,
                    BallColor.NONE,
                    true,
                    System.currentTimeMillis()
                );
            }
        }

        return VisionTarget.noTarget();
    }

    /**
     * Reads center AprilTag via HTTP
     */
    private List<BallColor> readCenterAprilTagHttp() {
        List<BallColor> sequence = new ArrayList<>();

        LimelightHttpClient.LimelightResult result = httpClient.getResults();

        if (!result.valid || result.fiducials.isEmpty()) {
            return sequence;
        }

        // Find center tag (21-23)
        for (LimelightHttpClient.FiducialDetection fiducial : result.fiducials) {
            int tagId = fiducial.fiducialId;

            if (tagId >= LimelightVision.APRILTAG_CENTER_START &&
                tagId <= LimelightVision.APRILTAG_CENTER_END) {

                // Map tag ID to ball sequence
                switch (tagId) {
                    case 21:
                        sequence.add(BallColor.GREEN);
                        sequence.add(BallColor.PURPLE);
                        sequence.add(BallColor.PURPLE);
                        break;
                    case 22:
                        sequence.add(BallColor.PURPLE);
                        sequence.add(BallColor.GREEN);
                        sequence.add(BallColor.PURPLE);
                        break;
                    case 23:
                        sequence.add(BallColor.PURPLE);
                        sequence.add(BallColor.PURPLE);
                        sequence.add(BallColor.GREEN);
                        break;
                }

                return sequence;
            }
        }

        return sequence;
    }

    /**
     * Calculates distance using Limelight geometry (same formula as LimelightVision)
     */
    private double calculateDistance(double ty, double targetHeightInches,
                                     double limelightHeightInches,
                                     double limelightAngleDegrees) {
        double angleToTargetDegrees = limelightAngleDegrees + ty;
        double angleToTargetRadians = Math.toRadians(angleToTargetDegrees);

        if (Math.abs(angleToTargetRadians) < 0.01) {
            return 1000.0;
        }

        double heightDifference = targetHeightInches - limelightHeightInches;
        double distance = heightDifference / Math.tan(angleToTargetRadians);

        return Math.max(0.0, Math.min(distance, 200.0));
    }
}
