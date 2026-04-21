package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.geometry.Pose;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.behaviors.BlockedBehavior;
import com.pedropathing.ivy.behaviors.ConflictBehavior;
import com.pedropathing.ivy.behaviors.EndCondition;
import com.pedropathing.ivy.behaviors.InterruptedBehavior;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.teamcode.pedro.AllianceColor;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Drivetrain;
import org.firstinspires.ftc.teamcode.vision.LimelightVision;
import org.firstinspires.ftc.teamcode.vision.VisionTarget;

import java.util.Collections;
import java.util.Set;

/**
 * gamepad1 a — path to a standoff pose derived from the nearest visible
 * pillar AprilTag. Replaces today's autoAlign toggle with a proper
 * Pedro-driven path. Aborts if the tag isn't visible at start.
 *
 * Pillar-only in v1; center-tag snap is deferred (spec §14) because
 * LimelightVision.readCenterAprilTag() currently only returns ball
 * colors, not a positional target.
 *
 * Standoff-pose formula (spec §8.5, in Pedro's frame):
 *   targetHeading = h0 + a
 *   targetX = x0 + (d - s) * cos(targetHeading)
 *   targetY = y0 + (d - s) * sin(targetHeading)
 * where (x0, y0, h0) = current pose, a = getAngleToTarget (radians),
 * d = getDistance (inches), s = standoff inches.
 */
public final class SnapToAprilTagCommand implements Command {

    private final Drivetrain drivetrain;
    private final LimelightVision vision;
    private final AllianceColor alliance;
    private final double standoffInches;
    private final Gamepad gp1;

    private boolean abort = false;

    public SnapToAprilTagCommand(Drivetrain drivetrain, LimelightVision vision,
                                 AllianceColor alliance, double standoffInches, Gamepad gp1) {
        this.drivetrain = drivetrain;
        this.vision = vision;
        this.alliance = alliance;
        this.standoffInches = standoffInches;
        this.gp1 = gp1;
    }

    @Override public void start() {
        VisionTarget tag = vision.getPillarTarget(alliance == AllianceColor.RED);
        if (!tag.isTargetFound()) {
            abort = true;
            return;
        }
        Pose current = drivetrain.getPose();
        double a = tag.getAngleToTarget();   // radians
        double d = tag.getDistance();        // inches
        double x0 = current.getX();
        double y0 = current.getY();
        double h0 = current.getHeading();
        double targetHeading = h0 + a;
        double targetX = x0 + (d - standoffInches) * Math.cos(targetHeading);
        double targetY = y0 + (d - standoffInches) * Math.sin(targetHeading);
        Pose target = new Pose(targetX, targetY, targetHeading);

        PathChain path = drivetrain.straightLineTo(target);
        drivetrain.followPath(path);
    }

    @Override public void execute() { }

    @Override public boolean done() {
        if (abort) return true;
        return DriverOverride.isActive(gp1) || !drivetrain.isFollowing();
    }

    @Override public void end(EndCondition endCondition) { drivetrain.cancel(); }

    @Override public Set<Object> requirements() { return Collections.singleton((Object) drivetrain); }
    @Override public int priority() { return 0; }
    @Override public InterruptedBehavior interruptedBehavior() { return InterruptedBehavior.END; }
    @Override public BlockedBehavior blockedBehavior() { return BlockedBehavior.CANCEL; }
    @Override public ConflictBehavior conflictBehavior() { return ConflictBehavior.OVERRIDE; }
}
