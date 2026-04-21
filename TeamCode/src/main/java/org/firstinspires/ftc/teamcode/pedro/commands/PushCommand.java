package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.ivy.Command;

import org.firstinspires.ftc.teamcode.pedro.subsystems.Pusher;

/** gamepad2 y — pushes at -1.0 while held, stops on release. */
public final class PushCommand {
    private PushCommand() {}
    public static Command create(Pusher pusher) {
        return new HeldActionCommand(
                () -> pusher.setPower(-1.0),
                pusher::stop,
                pusher);
    }
}
