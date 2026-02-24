package org.firstinspires.ftc.teamcode;

import com.acmerobotics.roadrunner.Pose2d;
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
            if (gamepad1.a || gamepad1.b) {
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