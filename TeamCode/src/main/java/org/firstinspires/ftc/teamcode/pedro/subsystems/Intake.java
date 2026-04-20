package org.firstinspires.ftc.teamcode.pedro.subsystems;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Two intake CRServos. intakeOne is the outer roller; intakeTwo is the
 * inner. Each is controlled by its own bumper today (gp2.RB / gp2.LB),
 * and both are reversed together as part of the clear-jam macro (gp2.X).
 *
 * TEACHING NOTE: We expose two independent setters plus a "stop" helper
 * rather than a combined "run" method, because today's driver controls
 * treat them as independent subsystems that just happen to share a
 * class. If that changes, it changes here.
 */
public class Intake {

    private final CRServo one, two;

    public Intake(HardwareMap hw) {
        this.one = hw.get(CRServo.class, "intakeOne");
        this.two = hw.get(CRServo.class, "intakeTwo");
    }

    public void setOnePower(double power) { one.setPower(power); }
    public void setTwoPower(double power) { two.setPower(power); }

    public void stopOne() { one.setPower(0.0); }
    public void stopTwo() { two.setPower(0.0); }
    public void stopAll() { stopOne(); stopTwo(); }
}
