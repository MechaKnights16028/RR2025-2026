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
 * gamepad1 b — path to a standoff pose at IDEAL_SHOOT_DISTANCE from the
 * nearest pillar. Replaces today's proportional creep-forward
 * (DriveCodeCommon.java:42-46) with a proper Pedro path.
 *
 * Same math as SnapToAprilTagCommand but with the shooter's ideal
 * distance as standoff. Aborts if tag not visible.
 */
public final class ApproachShotCommand implements Command {

    private final Drivetrain drivetrain;
    private final LimelightVision vision;
    private final AllianceColor alliance;
    private final double idealShootDistance;
    private final Gamepad gp1;

    private boolean abort = false;

    public ApproachShotCommand(Drivetrain drivetrain, LimelightVision vision,
                               AllianceColor alliance, double idealShootDistance,
                               Gamepad gp1) {
        this.drivetrain = drivetrain;
        this.vision = vision;
        this.alliance = alliance;
        this.idealShootDistance = idealShootDistance;
        this.gp1 = gp1;
    }

    @Override public void start() {
        VisionTarget tag = vision.getPillarTarget(alliance == AllianceColor.RED);
        if (!tag.isTargetFound()) {
            abort = true;
            return;
        }
        Pose current = drivetrain.getPose();
        double a = tag.getAngleToTarget();
        double d = tag.getDistance();
        double x0 = current.getX();
        double y0 = current.getY();
        double h0 = current.getHeading();
        double targetHeading = h0 + a;
        double targetX = x0 + (d - idealShootDistance) * Math.cos(targetHeading);
        double targetY = y0 + (d - idealShootDistance) * Math.sin(targetHeading);
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
