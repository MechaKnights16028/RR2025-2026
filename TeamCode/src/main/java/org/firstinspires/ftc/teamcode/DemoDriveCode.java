package org.firstinspires.ftc.teamcode;

import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.PoseVelocity2d;
import com.acmerobotics.roadrunner.Vector2d;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.vision.LimelightVision;
import org.firstinspires.ftc.teamcode.vision.VisionTarget;

@TeleOp
public class DemoDriveCode extends DriveCodeCommonDemo {
    @Override
    public void runOpMode() {
        MecanumDrive drive = new MecanumDrive(hardwareMap, new Pose2d(0, 0, 0));
        PID_Tune tuner1 = new PID_Tune();
        PID_Tune2 tuner2 = new PID_Tune2();
        //Alliance selection during INIT phase
       /* while (!isStarted() && !isStopRequested()){
            allianceSelect();
            telemetry.update();
        }*/
        waitForStart();
        /*LimelightVision limelight = new LimelightVision(hardwareMap, "limelight",
                telemetry);
        sleep(1000);*/

        while (opModeIsActive()) {
            // Toggle auto-align on single press of A
            /*if (gamepad1.a && !prevButtonA) {
                autoAlignActive = !autoAlignActive;
                autoAlignStartTime = System.currentTimeMillis();
                savedTagHeading = Double.NaN;
            }
            prevButtonA = gamepad1.a;

            // Fetch vision data once per loop
            VisionTarget pillarTag = limelight.getPillarTarget(isRedAlliance);
            double[] botpose = limelight.getBotpose();

            if (autoAlignActive) {
                autoAlign(drive, pillarTag, botpose);
            } else if (gamepad1.b) {
                if (pillarTag.isTargetFound() && pillarTag.getDistance() < IDEAL_SHOOT_DISTANCE) {
                    drive.setDrivePowers(new PoseVelocity2d(new Vector2d(-0.4, 0), 0));
                } else {
                    drive.setDrivePowers(new PoseVelocity2d(new Vector2d(0, 0), 0));
                }
            } else {*/
            drives(drive);
            intake(drive);
            //holder(drive);
            shooter(drive,tuner1,tuner2);
            //visionTelemetry(pillarTag);
            telemetry.update();
        }

    }
}