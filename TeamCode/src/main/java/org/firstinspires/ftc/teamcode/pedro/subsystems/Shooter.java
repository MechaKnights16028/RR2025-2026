package org.firstinspires.ftc.teamcode.pedro.subsystems;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

/**
 * Two-wheel launcher ("bottom" = launcherOne, "top" = launcherTwo)
 * running under velocity control with live PIDF tuning from FTC
 * Dashboard. Mirrors the behavior currently in
 * DriveCodeCommon.shooter() and the PID_Tune / PID_Tune2 dashboard
 * classes.
 *
 * Public API intentionally hides the specific RPM numbers behind
 * shootNear / shootFar / shootClear so commands read like button labels.
 *
 * TEACHING NOTE (preset speeds): These are the three fire modes today:
 *   near:  bottom 1500, top 750   (close pillar shot)
 *   far:   bottom 950,  top 1450  (long pillar shot)
 *   clear: bottom 10000, top 10000 (desperation shot / clear-out)
 * They can be live-tuned from the dashboard via the SHOOTER_PARAMS
 * nested @Config class without redeploying.
 *
 * TEACHING NOTE (live PIDF): applyPidfIfChanged() writes the dashboard
 * coefficients to the motors. Today's DriveCodeCommon.shooter() does
 * this every loop unconditionally. We only write when coefficients
 * change, which is cheaper and doesn't cost anything in behavior.
 */
@Config
public class Shooter {

    /** Tunable fire-mode speeds. Use these with ShootNear/Far/Clear commands. */
    public static class ShooterParams {
        public double nearBottom = 1500, nearTop = 750;
        public double farBottom  = 950,  farTop  = 1450;
        public double clearBottom = 10000, clearTop = 10000;
        public double reverseBottom = -500, reverseTop = -500;
    }
    public static ShooterParams SHOOTER_PARAMS = new ShooterParams();

    /** Tunable PIDF coefficients for each motor (live-editable). */
    public static class PidfParams {
        public double oneP = 0, oneI = 0, oneD = 0, oneF = 0;
        public double twoP = 0, twoI = 0, twoD = 0, twoF = 0;
    }
    public static PidfParams PIDF = new PidfParams();

    private final DcMotorEx launcherOne, launcherTwo;
    private PIDFCoefficients lastOne, lastTwo;

    public Shooter(HardwareMap hw) {
        this.launcherOne = hw.get(DcMotorEx.class, "launcherOne");
        this.launcherTwo = hw.get(DcMotorEx.class, "launcherTwo");
        // Current MecanumDrive reverses launcherTwo; keep parity.
        launcherTwo.setDirection(DcMotorEx.Direction.REVERSE);
        applyPidfIfChanged();
    }

    /** Call every tick (e.g. from a shooter-related command's execute()). */
    public void applyPidfIfChanged() {
        PIDFCoefficients one = new PIDFCoefficients(PIDF.oneP, PIDF.oneI, PIDF.oneD, PIDF.oneF);
        PIDFCoefficients two = new PIDFCoefficients(PIDF.twoP, PIDF.twoI, PIDF.twoD, PIDF.twoF);
        if (!equals(one, lastOne)) {
            launcherOne.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, one);
            lastOne = one;
        }
        if (!equals(two, lastTwo)) {
            launcherTwo.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, two);
            lastTwo = two;
        }
    }

    public void shootNear()  { setVelocities(SHOOTER_PARAMS.nearBottom,  SHOOTER_PARAMS.nearTop); }
    public void shootFar()   { setVelocities(SHOOTER_PARAMS.farBottom,   SHOOTER_PARAMS.farTop); }
    public void shootClear() { setVelocities(SHOOTER_PARAMS.clearBottom, SHOOTER_PARAMS.clearTop); }
    public void reverse()    { setVelocities(SHOOTER_PARAMS.reverseBottom, SHOOTER_PARAMS.reverseTop); }
    public void stop()       { setVelocities(0.0, 0.0); }

    private void setVelocities(double bottom, double top) {
        launcherOne.setVelocity(bottom);
        launcherTwo.setVelocity(top);
    }

    private static boolean equals(PIDFCoefficients a, PIDFCoefficients b) {
        if (a == null || b == null) return false;
        return a.p == b.p && a.i == b.i && a.d == b.d && a.f == b.f;
    }
}
