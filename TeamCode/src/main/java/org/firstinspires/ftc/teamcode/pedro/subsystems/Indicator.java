package org.firstinspires.ftc.teamcode.pedro.subsystems;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.hardware.rev.RevBlinkinLedDriver;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.pedro.AllianceColor;

/**
 * LED status indicator. Wraps the RevBlinkinLedDriver so commands never
 * poke the hardware directly.
 *
 * Default pattern shows the selected alliance color. Momentary overrides
 * (e.g. ShootClearCommand flashing BLUE) are handled by the commands
 * themselves interrupting IndicatorDefaultCommand.
 *
 * TEACHING NOTE: This is a subsystem by our own convention — Ivy
 * requirements() accepts any Object. We pass an Indicator instance as
 * a requirement to mark "this command owns the LED for as long as it
 * runs." The scheduler does the mutual-exclusion work for us.
 */
@Config
public class Indicator {

    public static RevBlinkinLedDriver.BlinkinPattern RED_PATTERN =
            RevBlinkinLedDriver.BlinkinPattern.RED;
    public static RevBlinkinLedDriver.BlinkinPattern BLUE_PATTERN =
            RevBlinkinLedDriver.BlinkinPattern.BLUE;

    private final RevBlinkinLedDriver blinkin;

    public Indicator(HardwareMap hw) {
        this.blinkin = hw.get(RevBlinkinLedDriver.class, "blinkin");
    }

    public void setPattern(RevBlinkinLedDriver.BlinkinPattern pattern) {
        blinkin.setPattern(pattern);
    }

    public void paintAlliance(AllianceColor alliance) {
        setPattern(alliance == AllianceColor.RED ? RED_PATTERN : BLUE_PATTERN);
    }
}
