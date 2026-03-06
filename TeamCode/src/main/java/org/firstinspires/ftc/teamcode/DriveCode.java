package org.firstinspires.ftc.teamcode;

import com.acmerobotics.roadrunner.Pose2d;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.vision.LimelightVision;
import org.firstinspires.ftc.teamcode.vision.VisionTarget;
@TeleOp
public class DriveCode extends DriveCodeCommon {
    @Override
    public void runOpMode() {
        MecanumDrive drive = new MecanumDrive(hardwareMap, new Pose2d(0, 0, 0));
        PID_Tune tuner1 = new PID_Tune();
        PID_Tune2 tuner2 = new PID_Tune2();
        //Alliance selection during INIT phase
        while (!isStarted() && !isStopRequested()){
            allianceSelect();
            telemetry.update();
        }
        waitForStart();
        LimelightVision limelight = new LimelightVision(hardwareMap, "limelight",
                telemetry);

        while (opModeIsActive()) {
            if (gamepad1.a && !prevButtonA) {
                autoAlignActive = !autoAlignActive;
                autoAlignStartTime = System.currentTimeMillis();
                savedTagHeading = Double.NaN;
            }
            prevButtonA = gamepad1.a;

            VisionTarget pillarTag = limelight.getPillarTarget(isRedAlliance);

            if (autoAlignActive || gamepad1.b) {
                autoAlign(drive, pillarTag);
            } else {
                drives(drive);
            }
            intake(drive);
            //holder(drive);
            shooter(drive,tuner1,tuner2);
            visionTelemetry(pillarTag);
            telemetry.update();
        }

    }
}