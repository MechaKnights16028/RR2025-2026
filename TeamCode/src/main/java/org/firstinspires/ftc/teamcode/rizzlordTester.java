package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;


@TeleOp
public class rizzlordTester extends LinearOpMode {


    public DcMotor bob;
    public Servo billy;
    public Servo joe;

    public Servo loser;


    @Override
    public void runOpMode(){
        waitForStart();
        while(opModeIsActive()) {
            bob = hardwareMap.get(DcMotorEx.class, "bob");
            billy = hardwareMap.get(Servo.class, "billy");
            joe = hardwareMap.get(Servo.class, "joe");
            loser = hardwareMap.get(Servo.class, "loser");

            if (gamepad1.a) {
                bob.setPower(1);
            } else if (gamepad1.dpad_up) {
                bob.setPower(10);
            } else if (gamepad1.dpad_down) {
                bob.setPower(-1);
            } else {
                bob.setPower(0);
            }





            if (gamepad1.b) {
                billy.setPosition(2);
            }
            else {
                billy.setPosition(0);
            }

            if (gamepad1.x) {
                joe.setPosition(1);
            }
            else {
                joe.setPosition(0);
            }

            if (gamepad1.y) {
                loser.setPosition(0.5);
            }
            else {
                loser.setPosition(0);
            }
        }

    }
}
