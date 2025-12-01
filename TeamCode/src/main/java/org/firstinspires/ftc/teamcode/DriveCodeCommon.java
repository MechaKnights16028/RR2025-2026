package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.roadrunner.PoseVelocity2d;
import com.acmerobotics.roadrunner.Vector2d;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

@Config
public class DriveCodeCommon extends LinearOpMode {

    double paddlewaiting = 1.0;
    double padllecatch = 0.5;
    double paddlelaunch = 0.0;
    int PURPLE_RED_MIN = 50;
    int PURPLE_BLUE_MIN = 50;
    int PURPLE_GREEN_MAX = 80;

    int GREEN_GREEN_MIN = 80;
    int GREEN_RED_MAX = 70;
    int GREEN_BLUE_MAX = 70;


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
                        -gamepad1.right_stick_x*speed
                ),
                gamepad1.left_stick_x*speed
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
    public void shooter(MecanumDrive drive){
        drive.launcher.setPower(1.0);
    }
    public void holder(MecanumDrive drive){
        int red = drive.paddle1.red();
        int blue = drive.paddle1.blue();
        int green = drive.paddle1.green();
        boolean detectPurple = false;
        boolean detectGreen = false;
        telemetry.addData("red",red);
        telemetry.addData("green",green);
        telemetry.addData("blue",blue);
        telemetry.update();


        if(red > PURPLE_RED_MIN &&
                blue > PURPLE_BLUE_MIN &&
                green < PURPLE_GREEN_MAX){
            detectPurple = true;
        }
        else {
            detectPurple = false;
        }


        if(green > GREEN_GREEN_MIN &&
                red < GREEN_RED_MAX &&
                blue < GREEN_BLUE_MAX){
                detectGreen = true;
            }
        else {
            detectGreen = false;
        }

        if(detectPurple || detectGreen||gamepad2.dpad_right){
            drive.paddleOne.setPosition(padllecatch);
        }
        else {
            drive.paddleOne.setPosition(paddlewaiting);
        }
}
}
