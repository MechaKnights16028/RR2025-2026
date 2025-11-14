/*package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.roadrunner.PoseVelocity2d;
import com.acmerobotics.roadrunner.Vector2d;
//import com.arcrobotics.ftclib.command.button.GamepadButton;
//import com.arcrobotics.ftclib.gamepad.GamepadKeys;
//import com.arcrobotics.ftclib.gamepad.ToggleButtonReader;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
//import com.arcrobotics.ftclib.gamepad.GamepadEx;

@Disabled
@Config
public class Old_DriveCodeCommon extends LinearOpMode {
    public static double intakeBarZero = 0.0;
    //public static double intakeServoZero = 0.25;
    //public static double intakeBarHandoff = 0.35;
    public static double intakeBarPickUp = 1.0;
    //public static double intakePosOne = 0.75;
    //public static double clawPosOne = 0.0;
    //public static double clawPosTwo = 0.5;
    GamepadEx gamepadEx;
    ToggleButtonReader intakeBarToggle;
    ToggleButtonReader clawToggle;
    ToggleButtonReader handoffToggle;
    ToggleButtonReader intakeToggle;
    ToggleButtonReader intakeBarIntakeToggle;
    double speed = 1.0;
    boolean gameOver = false;

    @Override
    public void runOpMode() throws InterruptedException {
    }

    public void declares() {
        gamepadEx = new GamepadEx(gamepad2);
        intakeBarToggle = new ToggleButtonReader(gamepadEx, GamepadKeys.Button.A);
        clawToggle = new ToggleButtonReader(gamepadEx, GamepadKeys.Button.X);
        handoffToggle = new ToggleButtonReader(gamepadEx, GamepadKeys.Button.Y);
        intakeToggle = new ToggleButtonReader(gamepadEx, GamepadKeys.Button.A);
        intakeBarIntakeToggle = new ToggleButtonReader(gamepadEx, GamepadKeys.Button.B);
    }
    public void resetEncoders(MecanumDrive drive){
        drive.vlSlides.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        drive.vrSlides.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
    }
    public void telemetry(MecanumDrive drive){
        telemetry.addData(String.valueOf(drive.vlSlides.getCurrentPosition()), drive.vrSlides.getCurrentPosition());
        telemetry.update();
    }

    public void drives(MecanumDrive drive) {
        if (gamepad1.right_bumper) {
            speed = 0.5;
        } else {
            speed = 1.0;
        }
//        drive.leftBack.setPower((gamepad1.left_stick_y + gamepad1.left_stick_x + gamepad1.right_stick_x) * speed);
//        drive.leftFront.setPower((gamepad1.left_stick_y - gamepad1.left_stick_x + gamepad1.right_stick_x) * speed);
//        drive.rightBack.setPower((gamepad1.left_stick_y - gamepad1.left_stick_x - gamepad1.right_stick_x) * speed);
//        drive.rightFront.setPower((gamepad1.left_stick_y + gamepad1.left_stick_x - gamepad1.right_stick_x) * speed);
        drive.setDrivePowers(new PoseVelocity2d(
                new Vector2d(
                        gamepad1.left_stick_y*speed,
                        gamepad1.left_stick_x*speed
                ),
                -gamepad1.right_stick_x*speed
        ));

    }

    public void update(MecanumDrive drive) {
        intakeBarToggle.readValue();
        clawToggle.readValue();
        intakeToggle.readValue();
        intakeBarIntakeToggle.readValue();
        handoffToggle.readValue();
    }

    public void vLift(MecanumDrive drive) {
        if (gamepad2.right_trigger > 0.5){
            gameOver = true;
        }
        if (gamepad2.dpad_up) {
            gameOver = false;
            drive.vlSlides.setPower(1);
            drive.vrSlides.setPower(-(1));
            drive.vlSlides1.setPower(-1);
            drive.vrSlides1.setPower(1);
        } else if (gamepad2.dpad_down) {
            gameOver = false;
            drive.vlSlides.setPower(-1);
            drive.vrSlides.setPower(1);
            drive.vlSlides1.setPower(1);
            drive.vrSlides1.setPower(-1);
        } else {
            if(!gameOver){
                drive.vlSlides.setPower(0);
                drive.vrSlides.setPower(0);
                drive.vlSlides1.setPower(0);
                drive.vrSlides1.setPower(0);
            }
            else if(gameOver){
                drive.vlSlides.setPower(-1);
                drive.vrSlides.setPower(1);
                drive.vlSlides1.setPower(1);
                drive.vrSlides1.setPower(-1);
            }
        }

    }

    public void swingbar(MecanumDrive drive) {
        if (intakeBarToggle.getState()) {
            drive.intakeBar2.setPosition(intakeBarPickUp);
            drive.intakeBar1.setPosition(intakeBarZero);

        } else {
            drive.intakeBar1.setPosition(intakeBarPickUp);
            drive.intakeBar2.setPosition(intakeBarZero);
        }
    }

    public void intakeServos(MecanumDrive drive) {
        if (gamepad2.x) {
            drive.intake1.setPower(-1.0);
            drive.intake2.setPower(1.0);
        }else if (gamepad2.y){
            drive.intake1.setPower(1.0);
            drive.intake2.setPower(-1.0);
        } else {
            drive.intake1.setPower(0.0);
            drive.intake2.setPower(0.0);
        }
    }
    public void hslides(MecanumDrive drive){
        /*if(gamepad2.dpad_right){
            drive.hSlides.setPower(-1);
        }
        else if(gamepad2.dpad_left){
            drive.hSlides.setPower(1);
        }
        else{
            drive.hSlides.setPower(0);
        }*//*
        drive.hSlides.setPower(-gamepad2.left_stick_y);
    }
    public void intakeBar(MecanumDrive drive){
        if(gamepad2.left_bumper){
            drive.intakeRotate.setPosition(0);
        }
        else if(gamepad2.right_bumper){
            drive.intakeRotate.setPosition(0.4);
        }
    }
    boolean up = true;
    public void head(MecanumDrive drive){
        if(gamepad2.right_stick_button /*&& up*//*){
            drive.head1.setPosition(0.25);
            drive.head2.setPosition(0.75);
            telemetry.addData("moved","head");
            telemetry.update();
            //up = false;
        }

        else if(gamepad2.left_stick_button /*&& !up*//*){
            drive.head1.setPosition(0.5);
            drive.head2.setPosition(0.5);
            telemetry.addData("moved","head");
            telemetry.update();
            //up = true;
        }

        /*if(gamepad2.right_stick_button){
            drive.head1.setPosition(1.0);
            drive.head2.setPosition(0);
            telemetry.addData("moved","head");
            telemetry.update();
        }*//*
    }
    /*
    public void plane(MecanumDrive drive){
        if (gamepad1.a){
            drive.planeLauncher.setPosition(0);
        }
        else{
            drive.planeLauncher.setPosition(1);
        }

    }*//*
    public void intakeServos(MecanumDrive drive){
        /*if (gamepad2.a){
            drive.intakeBar.setPosition(intakeBarZero);
        }
        else if (false){
            drive.intakeBar.setPosition(0.4);
            telemetry.addData("nothing pressed", false);
        }
        else if (gamepad2.right_bumper){
            drive.intakeBar.setPosition(0.8);
            telemetry.addData("nothing pressed", false);
        } else if (gamepad2.left_bumper) {
            drive.intakeBar.setPosition(0.2);
            telemetry.addData("nothing pressed", false);
        } else if (gamepad2.x){
            drive.intake.setPosition(0.5);
            telemetry.addData("nothing pressed", false);
        }
        else if(gamepadEx.getButton(GamepadKeys.Button.B)){
            drive.intake.setPosition(intakeServoZero);
            telemetry.addData("nothing pressed", false);
        }
        else{
            telemetry.addData("nothing pressed", true);
        }
        if(clawToggle.getState()){//x button
            drive.claw.setPosition(clawPosTwo);
        }
        else{
            drive.claw.setPosition(clawPosOne);
        }
        if(handoffToggle.getState()){//y button
            drive.intakeBar.setPosition(intakeBarHandoff);
        }
        else if(intakeBarIntakeToggle.getState()){//b button
            drive.intakeBar.setPosition(intakeBarPickUp);
        }
        else{
            drive.intakeBar.setPosition(intakeBarZero);
        }
        if(intakeToggle.getState()){//a button
            drive.intake.setPosition(intakePosOne);
        }
        else {
            drive.intake.setPosition(intakeServoZero);
        }
        telemetry.update();
    }*//*
}
*/