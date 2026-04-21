package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.ivy.Command;
import com.qualcomm.hardware.rev.RevBlinkinLedDriver;

import org.firstinspires.ftc.teamcode.pedro.subsystems.Indicator;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Shooter;

/**
 * gamepad2 right_stick_button — launcher at 10000/10000 + blinkin BLUE
 * while held. Releases return the shooter to 0; IndicatorDefaultCommand
 * reclaims the LED.
 *
 * TEACHING NOTE: the LED is forced BLUE regardless of alliance — that's
 * today's behavior (spec §13) and we preserve it.
 */
public final class ShootClearCommand {
    private ShootClearCommand() {}
    public static Command create(Shooter shooter, Indicator indicator) {
        return new HeldActionCommand(
                () -> {
                    shooter.applyPidfIfChanged();
                    shooter.shootClear();
                    indicator.setPattern(RevBlinkinLedDriver.BlinkinPattern.BLUE);
                },
                shooter::stop,
                shooter, indicator);
    }
}
