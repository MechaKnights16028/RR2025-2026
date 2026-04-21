package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.geometry.Pose;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.behaviors.BlockedBehavior;
import com.pedropathing.ivy.behaviors.ConflictBehavior;
import com.pedropathing.ivy.behaviors.EndCondition;
import com.pedropathing.ivy.behaviors.InterruptedBehavior;

import org.firstinspires.ftc.teamcode.pedro.subsystems.Drivetrain;

import java.util.Collections;
import java.util.Set;

/** gamepad1 back — one-shot. Re-seeds Pedro's localizer to target pose. */
public final class ResetPoseCommand implements Command {
    private final Drivetrain drivetrain;
    private final Pose target;

    public ResetPoseCommand(Drivetrain drivetrain, Pose target) {
        this.drivetrain = drivetrain;
        this.target = target;
    }

    @Override public void start() { drivetrain.setPose(target); }
    @Override public void execute() { }
    @Override public boolean done() { return true; }
    @Override public void end(EndCondition endCondition) { }
    @Override public Set<Object> requirements() { return Collections.singleton((Object) drivetrain); }
    @Override public int priority() { return 0; }
    @Override public InterruptedBehavior interruptedBehavior() { return InterruptedBehavior.END; }
    @Override public BlockedBehavior blockedBehavior() { return BlockedBehavior.CANCEL; }
    @Override public ConflictBehavior conflictBehavior() { return ConflictBehavior.CANCEL; }
}
