package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.ivy.Command;

import org.firstinspires.ftc.teamcode.pedro.subsystems.Intake;

/** gamepad2 right_bumper — runs intakeOne at -1.0, stops on release. */
public final class IntakeOneRunCommand {
    private IntakeOneRunCommand() {}
    public static Command create(Intake intake) {
        return new HeldActionCommand(
                () -> intake.setOnePower(-1.0),
                intake::stopOne,
                intake);
    }
}
