package org.firstinspires.ftc.teamcode;

import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.PoseVelocity2d;
import com.acmerobotics.roadrunner.Vector2d;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.vision.LimelightVision;
@TeleOp
public class DriveCode extends DriveCodeCommon {
    @Override
    public void runOpMode() {
        MecanumDrive drive = new MecanumDrive(hardwareMap, new Pose2d(0, 0, 0));
        //Alliance selection during INIT phase
        while (!isStarted() && !isStopRequested()){
            allianceSelect();
            telemetry.update();
        }
        waitForStart();
        LimelightVision limelight = new LimelightVision(hardwareMap, "limelight",
                telemetry);

        while (opModeIsActive()) {
            if (gamepad1.x) {
                // DIAGNOSTIC: hardcoded rotation, no limelight involved
                // If robot does NOT rotate here, the issue is motor config, not autoAlign logic
                drive.setDrivePowers(new PoseVelocity2d(new Vector2d(0, 0), 0.5));
                telemetry.addData("DIAGNOSTIC", "Hardcoded rotate 0.5 - does robot spin?");
            } else if (gamepad1.a || gamepad1.b) {
                autoAlign(drive, limelight);
            } else {
                drives(drive);
            }
            intake(drive);
            //holder(drive);
            shooter(drive);
            visionTelemetry(limelight);
            telemetry.update();
        }

    }
}