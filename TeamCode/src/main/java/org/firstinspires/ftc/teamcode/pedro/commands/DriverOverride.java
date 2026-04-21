package org.firstinspires.ftc.teamcode.pedro.commands;

import com.qualcomm.robotcore.hardware.Gamepad;

/**
 * Returns true when any drive stick is past a small deadband — used by
 * driver-assist commands to cancel themselves so the driver can regain
 * control by bumping a stick. See spec §8.4.
 *
 * TEACHING NOTE: the default TeleOpDriveCommand can't cancel driver-
 * assist commands on its own because it doesn't run while they own the
 * Drivetrain. This helper gives every driver-assist a simple "driver
 * moved the stick -> I'm done" rule.
 */
public final class DriverOverride {
    public static final double DEADBAND = 0.1;
    private DriverOverride() {}
    public static boolean isActive(Gamepad gp1) {
        return Math.abs(gp1.left_stick_x)  > DEADBAND
            || Math.abs(gp1.left_stick_y)  > DEADBAND
            || Math.abs(gp1.right_stick_x) > DEADBAND
            || Math.abs(gp1.right_stick_y) > DEADBAND;
    }
}
