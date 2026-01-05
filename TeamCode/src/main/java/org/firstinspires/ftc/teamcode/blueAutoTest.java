package org.firstinspires.ftc.teamcode;

import com.acmerobotics.roadrunner.Pose2d;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

@Autonomous
public class blueAutoTest extends LinearOpMode{

    public void runOpMode() throws InterruptedException {
        {
            MecanumDrive drive = new MecanumDrive(hardwareMap, new Pose2d(0,0,0));
            waitForStart();
            drive.leftFront.setPower(1.0);
            drive.leftBack.setPower(1.0);
            drive.rightFront.setPower(1.0);
            drive.rightBack.setPower(1.0);
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            drive.leftFront.setPower(0.0);
            drive.leftBack.setPower(0.0);
            drive.rightFront.setPower(0.0);
            drive.rightBack.setPower(0.0);
        }
    }
}
