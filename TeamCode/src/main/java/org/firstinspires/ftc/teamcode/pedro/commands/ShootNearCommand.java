package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.ivy.Command;

import org.firstinspires.ftc.teamcode.pedro.subsystems.Shooter;

/** gamepad2 right_trigger > 0.5 — launcher at 1500/750 while held. */
public final class ShootNearCommand {
    private ShootNearCommand() {}
    public static Command create(Shooter shooter) {
        return new HeldActionCommand(
                () -> { shooter.applyPidfIfChanged(); shooter.shootNear(); },
                shooter::stop,
                shooter);
    }
}
