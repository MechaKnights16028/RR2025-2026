package org.firstinspires.ftc.teamcode.pedro;

import com.acmerobotics.dashboard.config.Config;
import com.pedropathing.geometry.Pose;
import com.pedropathing.ivy.Scheduler;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.pedro.commands.ApproachShotCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.ClearJamCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.IndicatorDefaultCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.IntakeOneRunCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.IntakeTwoRunCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.PathToPoseCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.PushCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.ResetPoseCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.ShootClearCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.ShootFarCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.ShootNearCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.SnapToAprilTagCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.TeleOpDriveCommand;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Drivetrain;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Indicator;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Intake;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Pusher;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Shooter;
import org.firstinspires.ftc.teamcode.pedro.util.GamepadTriggers;
import org.firstinspires.ftc.teamcode.vision.LimelightVision;

/**
 * Single place where all subsystems, commands, and bindings live. If
 * you want to change what a button does, edit this file and nothing
 * else.
 *
 * TEACHING NOTE: the pattern is "construct everything, register
 * defaults, wire triggers." Nowhere else in the pedro package does
 * anything read a gamepad directly or call Scheduler.schedule.
 */
@Config
public class RobotContainer {

    // Presets. Field-tuned placeholders.
    public static Pose STARTING_POSE = new Pose(0, 0, 0);
    public static Pose SCORING_POSE  = new Pose(24, 0, 0);
    public static double APRILTAG_STANDOFF_INCHES = 24.0;
    public static double IDEAL_SHOOT_DISTANCE     = 97.0;

    public final Drivetrain drivetrain;
    public final Intake intake;
    public final Shooter shooter;
    public final Pusher pusher;
    public final Indicator indicator;
    public final LimelightVision vision;

    private final Gamepad gp1, gp2;
    private final AllianceColor alliance;
    private final GamepadTriggers triggers = new GamepadTriggers();

    private final com.pedropathing.ivy.Command teleOpDrive;
    private final com.pedropathing.ivy.Command indicatorDefault;

    public RobotContainer(HardwareMap hw, Gamepad gp1, Gamepad gp2,
                          AllianceColor alliance, Telemetry telemetry) {
        this.gp1 = gp1;
        this.gp2 = gp2;
        this.alliance = alliance;

        this.drivetrain = new Drivetrain(hw);
        this.intake     = new Intake(hw);
        this.shooter    = new Shooter(hw);
        this.pusher     = new Pusher(hw);
        this.indicator  = new Indicator(hw);
        this.vision     = new LimelightVision(hw, "limelight", telemetry);

        this.teleOpDrive       = new TeleOpDriveCommand(drivetrain, gp1);
        this.indicatorDefault  = new IndicatorDefaultCommand(indicator, alliance);

        // Trigger binding is opt-in — PedroBringUpTeleOp skips this call.
    }

    /**
     * Wire all gamepad triggers to commands. PedroBringUpTeleOp
     * (defaults-only, spec §10 step 2) does NOT call this. PedroTeleOp
     * calls it once after construction.
     *
     * TEACHING NOTE (spec §9.3): gamepad1.a changes from the toggle
     * behavior in DriveCode to hold-to-run here. This is intentional
     * and matches the spec — hold-to-run is the idiomatic command-based
     * pattern and removes the autoAlignActive state machine.
     */
    public void bindTriggers() {
        // ---- gamepad 2 (operator) ----
        triggers.whileTrue(() -> gp2.right_bumper,        () -> IntakeOneRunCommand.create(intake));
        triggers.whileTrue(() -> gp2.left_bumper,         () -> IntakeTwoRunCommand.create(intake));
        triggers.whileTrue(() -> gp2.x,                   () -> ClearJamCommand.create(intake, shooter, pusher));
        triggers.whileTrue(() -> gp2.y,                   () -> PushCommand.create(pusher));
        triggers.whileTrue(() -> gp2.right_trigger > 0.5, () -> ShootNearCommand.create(shooter));
        triggers.whileTrue(() -> gp2.left_trigger  > 0.5, () -> ShootFarCommand.create(shooter));
        triggers.whileTrue(() -> gp2.right_stick_button,  () -> ShootClearCommand.create(shooter, indicator));

        // ---- gamepad 1 (driver) ----
        triggers.onTrue   (() -> gp1.back,                () -> new ResetPoseCommand(drivetrain, STARTING_POSE));
        triggers.whileTrue(() -> gp1.y,                   () -> new PathToPoseCommand(drivetrain, SCORING_POSE, gp1));
        triggers.whileTrue(() -> gp1.a,                   () -> new SnapToAprilTagCommand(drivetrain, vision, alliance, APRILTAG_STANDOFF_INCHES, gp1));
        triggers.whileTrue(() -> gp1.b,                   () -> new ApproachShotCommand(drivetrain, vision, alliance, IDEAL_SHOOT_DISTANCE, gp1));
    }

    /** Called from PedroTeleOp once after Scheduler.reset(). */
    public void scheduleDefaults() {
        Scheduler.schedule(teleOpDrive);
        Scheduler.schedule(indicatorDefault);
    }

    /** Call every loop, before Scheduler.execute(). Re-schedules defaults
     *  when nothing else owns their subsystem. Also ticks Pedro's
     *  follower so the localizer stays current even when no path is
     *  active — the driver-assists need an up-to-date pose the moment
     *  they fire. */
    public void tick() {
        // NOTE: the spec §6.1 originally said "only update when following."
        // That would stop pose tracking during manual driving and break
        // SnapToAprilTagCommand / PathToPoseCommand at start (they read
        // a stale pose). Pedro's follower.update() ticks the localizer
        // whether or not a path is active, so we always call it.
        drivetrain.update();

        // Re-schedule defaults if they ended because something preempted.
        if (!Scheduler.isScheduled(teleOpDrive))      Scheduler.schedule(teleOpDrive);
        if (!Scheduler.isScheduled(indicatorDefault)) Scheduler.schedule(indicatorDefault);

        triggers.poll();
    }

    public void pollTelemetry(Telemetry t) {
        Pose pose = drivetrain.getPose();
        t.addData("Alliance", alliance);
        t.addData("Localizer", Drivetrain.LOCALIZER);
        t.addData("Pose", "x=%.1f y=%.1f h=%.1f°",
                pose.getX(), pose.getY(), Math.toDegrees(pose.getHeading()));
        t.addData("Following", drivetrain.isFollowing());
    }
}
