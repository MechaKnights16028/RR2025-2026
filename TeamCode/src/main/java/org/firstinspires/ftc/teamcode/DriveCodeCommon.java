package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.roadrunner.PoseVelocity2d;
import com.acmerobotics.roadrunner.Vector2d;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

@Config
public class DriveCodeCommon extends LinearOpMode {

    double speed = 1.0;

    @Override
    public void runOpMode() throws InterruptedException {
    }

    public void drives(MecanumDrive drive){
        if (gamepad1.right_bumper) {
            speed = 0.5;
        } else {
            speed = 1.0;
        }
        drive.setDrivePowers(new PoseVelocity2d(
                new Vector2d(
                        -gamepad1.left_stick_y*speed,
                        gamepad1.left_stick_x*speed
                ),
                -gamepad1.right_stick_x*speed
        ));
    }
    public void intake(MecanumDrive drive){
        if(gamepad2.right_bumper){
            drive.intake.setPower(1.0);
        }
        else {
            drive.intake.setPower(0);
        }
    }
}
