package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.ivy.Command;

import org.firstinspires.ftc.teamcode.pedro.subsystems.Intake;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Pusher;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Shooter;

/**
 * gamepad2 x — multi-subsystem "unjam everything" macro. While held:
 * intakes forward, launcher reversed, pusher forward. On release: all
 * three stopped. Ports DriveCodeCommon.java:77-80 + :115-118 into one
 * command that requires all three subsystems so any conflicting
 * command preempts cleanly.
 */
public final class ClearJamCommand {
    private ClearJamCommand() {}
    public static Command create(Intake intake, Shooter shooter, Pusher pusher) {
        return new HeldActionCommand(
                () -> {
                    intake.setOnePower(1.0);
                    intake.setTwoPower(1.0);
                    shooter.reverse();
                    pusher.setPower(1.0);
                },
                () -> {
                    intake.stopAll();
                    shooter.stop();
                    pusher.stop();
                },
                intake, shooter, pusher);
    }
}
