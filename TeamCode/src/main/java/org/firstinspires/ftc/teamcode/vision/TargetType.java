package org.firstinspires.ftc.teamcode.vision;

/**
 * Represents the type of vision target being tracked by the Limelight.
 * Used to identify whether we're detecting AprilTags or colored balls.
 */
public enum TargetType {
    /**
     * AprilTag marker detection (tags 20-24)
     */
    APRIL_TAG,

    /**
     * Colored ball detection (purple or green)
     */
    BALL,

    /**
     * No target detected
     */
    NONE
}
