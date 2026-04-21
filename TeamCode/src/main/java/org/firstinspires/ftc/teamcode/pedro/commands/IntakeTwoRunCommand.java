package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.ivy.Command;

import org.firstinspires.ftc.teamcode.pedro.subsystems.Intake;

/** gamepad2 left_bumper — runs intakeTwo at -1.0, stops on release. */
public final class IntakeTwoRunCommand {
    private IntakeTwoRunCommand() {}
    public static Command create(Intake intake) {
        return new HeldActionCommand(
                () -> intake.setTwoPower(-1.0),
                intake::stopTwo,
                intake);
    }
}
