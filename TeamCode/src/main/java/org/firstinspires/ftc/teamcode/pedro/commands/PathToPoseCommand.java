package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.geometry.Pose;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.behaviors.BlockedBehavior;
import com.pedropathing.ivy.behaviors.ConflictBehavior;
import com.pedropathing.ivy.behaviors.EndCondition;
import com.pedropathing.ivy.behaviors.InterruptedBehavior;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.teamcode.pedro.subsystems.Drivetrain;

import java.util.Collections;
import java.util.Set;

/**
 * gamepad1 y — straight-line path from current pose to a target pose.
 * Cancels when either the path completes OR the driver nudges a stick.
 *
 * TEACHING NOTE: why roll our own follow-command instead of using
 * Ivy's {@code PedroCommands.follow(follower, path)}? Two reasons:
 *   1. We need the stick-override check in done(), which the built-in
 *      follow doesn't have.
 *   2. We need to own the Drivetrain requirement — so that
 *      TeleOpDriveCommand (which also requires Drivetrain) is
 *      properly preempted while we're following.
 */
public final class PathToPoseCommand implements Command {

    private final Drivetrain drivetrain;
    private final Pose target;
    private final Gamepad gp1;

    public PathToPoseCommand(Drivetrain drivetrain, Pose target, Gamepad gp1) {
        this.drivetrain = drivetrain;
        this.target = target;
        this.gp1 = gp1;
    }

    @Override public void start() {
        PathChain path = drivetrain.straightLineTo(target);
        drivetrain.followPath(path);
    }

    @Override public void execute() { /* Drivetrain.update() is ticked by RobotContainer */ }

    @Override public boolean done() {
        return DriverOverride.isActive(gp1) || !drivetrain.isFollowing();
    }

    @Override public void end(EndCondition endCondition) { drivetrain.cancel(); }

    @Override public Set<Object> requirements() { return Collections.singleton((Object) drivetrain); }
    @Override public int priority() { return 0; }
    @Override public InterruptedBehavior interruptedBehavior() { return InterruptedBehavior.END; }
    @Override public BlockedBehavior blockedBehavior() { return BlockedBehavior.CANCEL; }
    @Override public ConflictBehavior conflictBehavior() { return ConflictBehavior.OVERRIDE; }
}
