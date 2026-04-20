package org.firstinspires.ftc.teamcode.pedro.subsystems;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * The pusher wheel that feeds balls into the launcher.
 *
 * Hardware name is "pusherwheel" (all lowercase) per the existing
 * MecanumDrive config — see spec §13 quirks.
 *
 * TEACHING NOTE: setPower takes positive and negative values. In today's
 * code, negative is "push toward launcher" and positive is the reverse
 * direction used by the ClearJamCommand. We keep that convention.
 */
public class Pusher {

    private final CRServo wheel;

    public Pusher(HardwareMap hw) {
        this.wheel = hw.get(CRServo.class, "pusherwheel");
    }

    public void setPower(double power) { wheel.setPower(power); }
    public void stop() { wheel.setPower(0.0); }
}
