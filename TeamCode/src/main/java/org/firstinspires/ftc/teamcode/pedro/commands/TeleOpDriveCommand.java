package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.behaviors.BlockedBehavior;
import com.pedropathing.ivy.behaviors.ConflictBehavior;
import com.pedropathing.ivy.behaviors.EndCondition;
import com.pedropathing.ivy.behaviors.InterruptedBehavior;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.teamcode.pedro.subsystems.Drivetrain;

import java.util.Collections;
import java.util.Set;

/**
 * Default Drivetrain command. Reads gamepad1 and calls drive(x, y, rot, slow)
 * every tick. Signs match DriveCodeCommon.drives():
 *   x = -left_stick_y, y = -left_stick_x, rot = -right_stick_x
 * slow = gamepad1.right_bumper.
 *
 * Uses InterruptedBehavior.END so when a driver-assist command claims
 * Drivetrain, this one ends cleanly and RobotContainer can re-schedule
 * it later.
 */
public final class TeleOpDriveCommand implements Command {

    private final Drivetrain drivetrain;
    private final Gamepad gp1;

    public TeleOpDriveCommand(Drivetrain drivetrain, Gamepad gp1) {
        this.drivetrain = drivetrain;
        this.gp1 = gp1;
    }

    @Override public void start() { }

    @Override public void execute() {
        drivetrain.drive(-gp1.left_stick_y, -gp1.left_stick_x, -gp1.right_stick_x,
                gp1.right_bumper);
    }

    @Override public boolean done() { return false; }

    @Override public void end(EndCondition endCondition) { drivetrain.stop(); }

    @Override public Set<Object> requirements() {
        return Collections.singleton((Object) drivetrain);
    }

    @Override public int priority() { return 0; }
    @Override public InterruptedBehavior interruptedBehavior() { return InterruptedBehavior.END; }
    @Override public BlockedBehavior blockedBehavior() { return BlockedBehavior.CANCEL; }
    @Override public ConflictBehavior conflictBehavior() { return ConflictBehavior.CANCEL; }
}
