package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.ivy.Scheduler;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * Debug-only TeleOp. Binds default commands ONLY — no driver assists,
 * no intake/shoot/push buttons. Used for the spec §10 step 2 gate.
 *
 * If manual drive feels identical to today's DriveCode (mecanum +
 * slow mode), the hardware layer and Drivetrain subsystem are correct.
 */
@TeleOp(name = "Pedro BringUp", group = "pedro-debug")
public class PedroBringUpTeleOp extends LinearOpMode {
    @Override public void runOpMode() {
        AllianceColor alliance = AllianceColor.BLUE;
        telemetry.addLine("Pedro BringUp — defaults only");
        telemetry.update();
        waitForStart();
        if (isStopRequested()) return;

        Scheduler.reset();
        RobotContainer robot = new RobotContainer(hardwareMap, gamepad1, gamepad2, alliance, telemetry);
        // Intentionally NOT calling robot.bindTriggers() — this OpMode
        // exercises only the default commands (spec §10 step 2 gate).
        robot.scheduleDefaults();

        while (opModeIsActive()) {
            robot.tick();
            Scheduler.execute();
            robot.pollTelemetry(telemetry);
            telemetry.update();
        }
    }
}
