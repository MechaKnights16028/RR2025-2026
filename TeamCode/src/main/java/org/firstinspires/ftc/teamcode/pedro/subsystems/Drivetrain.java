package org.firstinspires.ftc.teamcode.pedro.subsystems;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * The mecanum drivetrain. Owns the four drive motors AND (after Task 9)
 * Pedro's Follower + localizer. Only class in the Pedro package that
 * calls setPower(...) on a drive motor.
 *
 * Hardware-name crossover (spec §13): the existing Robot Controller
 * config is wired so the Java field "leftFront" maps to the hardware
 * name "rightFront" (and analogously for the other three). We keep
 * that exact mapping so the config file doesn't need to change. See
 * MecanumDrive.java:238-241 for the reference.
 *
 * TEACHING NOTE: drive(x, y, rot, slow) expects pre-signed values in
 * FTC convention (+x forward, +y left, +rot counterclockwise).
 * TeleOpDriveCommand owns the stick-to-axis sign flipping so the
 * intent lives next to the gamepad read.
 */
@Config
public class Drivetrain {

    public enum LocalizerType { THREE_DEAD_WHEEL, PINPOINT }
    public static LocalizerType LOCALIZER = LocalizerType.THREE_DEAD_WHEEL;

    /** Tunable params for the three-dead-wheel localizer. */
    public static class ThreeWheelParams {
        public double parTicksPerInch = 0.0;
        public double perpTicksPerInch = 0.0;
        public double par0_y = 0.0;
        public double par1_y = 0.0;
        public double perp_x = 0.0;
        public DcMotorSimple.Direction par0Dir  = DcMotorSimple.Direction.FORWARD;
        public DcMotorSimple.Direction par1Dir  = DcMotorSimple.Direction.FORWARD;
        public DcMotorSimple.Direction perpDir  = DcMotorSimple.Direction.FORWARD;
    }
    public static ThreeWheelParams THREE_WHEEL = new ThreeWheelParams();

    /** Tunable params for the goBILDA Pinpoint localizer. */
    public static class PinpointParams {
        public double podXOffsetMM = 0.0;
        public double podYOffsetMM = 0.0;
        public double ticksPerMM = 0.0;
        public GoBildaPinpointDriver.EncoderDirection parDir =
                GoBildaPinpointDriver.EncoderDirection.FORWARD;
        public GoBildaPinpointDriver.EncoderDirection perpDir =
                GoBildaPinpointDriver.EncoderDirection.FORWARD;
        public double yawScalar = 1.0;
    }
    public static PinpointParams PINPOINT = new PinpointParams();

    private final DcMotorEx leftFront, leftBack, rightFront, rightBack;

    public Drivetrain(HardwareMap hw) {
        // Preserve existing crossover. DO NOT normalize these names — it will
        // flip the drive direction in surprising ways.
        this.rightFront = hw.get(DcMotorEx.class, "leftFront");
        this.rightBack  = hw.get(DcMotorEx.class, "leftBack");
        this.leftBack   = hw.get(DcMotorEx.class, "rightBack");
        this.leftFront  = hw.get(DcMotorEx.class, "rightFront");

        for (DcMotorEx m : new DcMotorEx[] { leftFront, leftBack, rightFront, rightBack }) {
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }
        // Mirrors MecanumDrive.java:249
        rightFront.setDirection(DcMotorSimple.Direction.REVERSE);
    }

    /**
     * Robot-relative teleop drive. +x forward, +y left, +rot CCW.
     * {@code slow=true} halves all three axes.
     */
    public void drive(double x, double y, double rot, boolean slow) {
        double s = slow ? 0.5 : 1.0;
        x *= s; y *= s; rot *= s;

        // Standard mecanum mixing.
        double lf = x + y + rot;
        double lb = x - y + rot;
        double rf = x - y - rot;
        double rb = x + y - rot;

        double norm = Math.max(1.0,
                Math.max(Math.abs(lf),
                        Math.max(Math.abs(lb),
                                Math.max(Math.abs(rf), Math.abs(rb)))));
        leftFront.setPower(lf / norm);
        leftBack.setPower(lb / norm);
        rightFront.setPower(rf / norm);
        rightBack.setPower(rb / norm);
    }

    public void stop() { drive(0, 0, 0, false); }
}
