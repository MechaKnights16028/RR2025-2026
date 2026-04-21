package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.ivy.Scheduler;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * The full command-based TeleOp. During INIT, gamepad1 dpad_left/right
 * selects the alliance; press START to begin. Then every behavior in
 * today's DriveCode is wired through Ivy commands, plus the three new
 * driver-assist commands (PathToPose, SnapToAprilTag, ResetPose, and
 * ApproachShot).
 */
@TeleOp(name = "Pedro TeleOp", group = "pedro")
public class PedroTeleOp extends LinearOpMode {
    @Override public void runOpMode() {
        AllianceColor alliance = AllianceColor.BLUE;
        while (!isStarted() && !isStopRequested()) {
            if (gamepad1.dpad_left)  alliance = AllianceColor.BLUE;
            if (gamepad1.dpad_right) alliance = AllianceColor.RED;
            telemetry.addData("Alliance", alliance);
            telemetry.addLine("Press LEFT for BLUE, RIGHT for RED");
            telemetry.update();
        }
        waitForStart();
        if (isStopRequested()) return;

        Scheduler.reset();
        RobotContainer robot = new RobotContainer(hardwareMap, gamepad1, gamepad2, alliance, telemetry);
        robot.bindTriggers();
        robot.scheduleDefaults();

        while (opModeIsActive()) {
            robot.tick();
            Scheduler.execute();
            robot.pollTelemetry(telemetry);
            telemetry.update();
        }
    }
}
