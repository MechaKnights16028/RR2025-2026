package org.firstinspires.ftc.teamcode.pedro.subsystems;

import com.acmerobotics.dashboard.config.Config;
import com.pedropathing.follower.Follower;
import com.pedropathing.follower.FollowerConstants;
import com.pedropathing.ftc.FollowerBuilder;
import com.pedropathing.ftc.drivetrains.MecanumConstants;
import com.pedropathing.ftc.localization.constants.PinpointConstants;
import com.pedropathing.ftc.localization.constants.ThreeWheelConstants;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathBuilder;
import com.pedropathing.paths.PathChain;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * The mecanum drivetrain. Owns Pedro's {@link Follower} and the
 * localizer configuration. All drive-motor actuation (manual and
 * path-following) flows through the Follower — this class never calls
 * setPower on a motor directly.
 *
 * Hardware-name crossover (spec §13): the existing Robot Controller
 * config is wired so the Java label "leftFront" in MecanumDrive.java
 * maps to the motor at the physical front-right corner. We translate
 * that here by passing Pedro the motor names it expects at each
 * physical corner — i.e. the LEFT front motor is configured in the
 * hardware map as {@code "rightFront"}, and vice versa. See
 * MecanumDrive.java:238-241 for the reference.
 *
 * TEACHING NOTE (sign convention): drive(x, y, rot, slow) takes
 * FTC-convention pre-signed values: +x forward, +y left, +rot
 * counterclockwise. TeleOpDriveCommand owns stick-to-axis flipping
 * so intent lives next to the gamepad read.
 *
 * TEACHING NOTE (teleop vs path): the Follower has two modes. While
 * in teleop mode (entered via {@link Follower#startTeleopDrive()}),
 * {@link Follower#setTeleOpDrive} is the only way to move the bot.
 * While following a path (entered via {@link Follower#followPath}),
 * teleop inputs are ignored. {@link #cancel()} handles the
 * transition back to teleop cleanly.
 */
@Config
public class Drivetrain {

    public enum LocalizerType { THREE_DEAD_WHEEL, PINPOINT }
    public static LocalizerType LOCALIZER = LocalizerType.THREE_DEAD_WHEEL;

    /**
     * Tunable params for the three-dead-wheel localizer. Encoders share
     * motor ports on REV hubs, which is why the hardware names here
     * look like motor names.
     */
    public static class ThreeWheelParams {
        public String leftEncoderName  = "leftFront";
        public String rightEncoderName = "rightBack";
        public String strafeEncoderName = "leftBack";
        public double forwardTicksToInches = 0.002;   // TUNE
        public double strafeTicksToInches  = 0.002;   // TUNE
        public double turnTicksToInches    = 0.002;   // TUNE
        public double leftPodY  = 1.0;                // TUNE (inches from center)
        public double rightPodY = -1.0;               // TUNE
        public double strafePodX = -2.0;              // TUNE
        public double leftEncoderDirection  = 1.0;    // ±1 — flip if pose runs backwards
        public double rightEncoderDirection = 1.0;
        public double strafeEncoderDirection = 1.0;
    }
    public static ThreeWheelParams THREE_WHEEL = new ThreeWheelParams();

    /** Tunable params for the goBILDA Pinpoint localizer. */
    public static class PinpointParams {
        public String hardwareName = "pinpoint";
        public double forwardPodY = 0.0;              // TUNE (inches from center)
        public double strafePodX  = 0.0;              // TUNE
        public double yawScalar   = 1.0;              // TUNE
        public GoBildaPinpointDriver.EncoderDirection forwardDir =
                GoBildaPinpointDriver.EncoderDirection.FORWARD;
        public GoBildaPinpointDriver.EncoderDirection strafeDir =
                GoBildaPinpointDriver.EncoderDirection.FORWARD;
        public GoBildaPinpointDriver.GoBildaOdometryPods podType =
                GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD;
    }
    public static PinpointParams PINPOINT = new PinpointParams();

    private final Follower follower;

    public Drivetrain(HardwareMap hw) {
        FollowerConstants fc = new FollowerConstants();

        // Crossed mapping: LEFT-front physical motor is hardware-named
        // "rightFront" (see class javadoc); etc.
        MecanumConstants mecanum = new MecanumConstants()
                .leftFrontMotorName("rightFront")
                .leftRearMotorName("rightBack")
                .rightFrontMotorName("leftFront")
                .rightRearMotorName("leftBack")
                .rightFrontMotorDirection(DcMotorSimple.Direction.REVERSE)
                .useBrakeModeInTeleOp(true);

        FollowerBuilder builder = new FollowerBuilder(fc, hw)
                .mecanumDrivetrain(mecanum);

        switch (LOCALIZER) {
            case PINPOINT:
                builder = builder.pinpointLocalizer(buildPinpointConstants());
                break;
            case THREE_DEAD_WHEEL:
            default:
                builder = builder.threeWheelLocalizer(buildThreeWheelConstants());
                break;
        }

        this.follower = builder.build();
        this.follower.startTeleopDrive();
    }

    private static ThreeWheelConstants buildThreeWheelConstants() {
        return new ThreeWheelConstants()
                .leftEncoder_HardwareMapName(THREE_WHEEL.leftEncoderName)
                .rightEncoder_HardwareMapName(THREE_WHEEL.rightEncoderName)
                .strafeEncoder_HardwareMapName(THREE_WHEEL.strafeEncoderName)
                .forwardTicksToInches(THREE_WHEEL.forwardTicksToInches)
                .strafeTicksToInches(THREE_WHEEL.strafeTicksToInches)
                .turnTicksToInches(THREE_WHEEL.turnTicksToInches)
                .leftPodY(THREE_WHEEL.leftPodY)
                .rightPodY(THREE_WHEEL.rightPodY)
                .strafePodX(THREE_WHEEL.strafePodX)
                .leftEncoderDirection(THREE_WHEEL.leftEncoderDirection)
                .rightEncoderDirection(THREE_WHEEL.rightEncoderDirection)
                .strafeEncoderDirection(THREE_WHEEL.strafeEncoderDirection);
    }

    private static PinpointConstants buildPinpointConstants() {
        return new PinpointConstants()
                .hardwareMapName(PINPOINT.hardwareName)
                .forwardPodY(PINPOINT.forwardPodY)
                .strafePodX(PINPOINT.strafePodX)
                .yawScalar(PINPOINT.yawScalar)
                .forwardEncoderDirection(PINPOINT.forwardDir)
                .strafeEncoderDirection(PINPOINT.strafeDir)
                .encoderResolution(PINPOINT.podType);
    }

    // ---------- Teleop drive ----------

    /**
     * Robot-relative teleop drive. +x forward, +y left, +rot CCW.
     * slow=true halves all three axes.
     */
    public void drive(double x, double y, double rot, boolean slow) {
        double s = slow ? 0.5 : 1.0;
        follower.setTeleOpDrive(x * s, y * s, rot * s, true);
    }

    public void stop() { drive(0, 0, 0, false); }

    // ---------- Pose / path following ----------

    /** Call every loop from RobotContainer.tick(). Ticks localizer + any active path. */
    public void update() { follower.update(); }

    public Pose getPose() { return follower.getPose(); }

    public void setPose(Pose pose) { follower.setPose(pose); }

    /**
     * Begin following the given path. cancel() breaks follow and
     * returns to teleop drive.
     */
    public void followPath(PathChain path) { follower.followPath(path); }

    public boolean isFollowing() { return follower.isBusy(); }

    public void cancel() {
        follower.breakFollowing();
        follower.startTeleopDrive();
    }

    /** Exposes the underlying Follower for PedroCommands.follow(follower, ...). */
    public Follower getFollower() { return follower; }

    /**
     * Build a straight-line PathChain from current pose to target pose.
     * Used by PathToPose / SnapToAprilTag / ApproachShot commands.
     *
     * TEACHING NOTE: we wrap PathBuilder so commands don't import
     * Pedro's path classes directly. If Pedro's API changes, only
     * this method needs updating.
     */
    public PathChain straightLineTo(Pose target) {
        Pose current = follower.getPose();
        return follower.pathBuilder()
                .addPath(new com.pedropathing.paths.Path(
                        new com.pedropathing.geometry.BezierLine(current, target)))
                .setLinearHeadingInterpolation(current.getHeading(), target.getHeading())
                .build();
    }
}
