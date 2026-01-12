package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.acmerobotics.roadrunner.Vector2d;
import com.acmerobotics.roadrunner.PoseVelocity2d;

@TeleOp
public class driveCodeTest extends LinearOpMode {

    public MecanumDrive drive;
    double speed;
    @Override
    public void runOpMode() throws InterruptedException{


    waitForStart();

    while (opModeIsActive()) {
        if (gamepad1.right_bumper) {
            speed = 0.5;
        } else {
            speed = 1.0;
        }
        drive.setDrivePowers(new PoseVelocity2d(
                new Vector2d(
                        -gamepad1.left_stick_y * speed,
                        -gamepad1.right_stick_x * speed
                ),
                gamepad1.left_stick_x * speed
        ));
    }
}
}
