package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.ivy.Command;

import org.firstinspires.ftc.teamcode.pedro.subsystems.Shooter;

/** gamepad2 left_trigger > 0.5 — launcher at 950/1450 while held. */
public final class ShootFarCommand {
    private ShootFarCommand() {}
    public static Command create(Shooter shooter) {
        return new HeldActionCommand(
                () -> { shooter.applyPidfIfChanged(); shooter.shootFar(); },
                shooter::stop,
                shooter);
    }
}
