package org.firstinspires.ftc.teamcode.pedro;

/**
 * Which alliance the robot is playing for. Selected during INIT (see PedroTeleOp)
 * and passed to RobotContainer so commands that care about alliance (e.g.
 * SnapToAprilTagCommand, IndicatorDefaultCommand) can branch on it.
 *
 * TEACHING NOTE: We pass this as an explicit constructor argument instead of a
 * global static so that future OpModes can run with a hard-coded alliance for
 * testing without touching the global state.
 */
public enum AllianceColor {
    RED,
    BLUE
}
