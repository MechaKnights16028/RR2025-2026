package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.roadrunner.PoseVelocity2d;
import com.acmerobotics.roadrunner.Vector2d;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.hardware.rev.RevBlinkinLedDriver;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.acmerobotics.roadrunner.PoseVelocity2d;
import com.acmerobotics.roadrunner.Vector2d;

import org.firstinspires.ftc.teamcode.vision.LimelightVision;
import org.firstinspires.ftc.teamcode.vision.VisionTarget;

@Config
public class DriveCodeCommon extends LinearOpMode{

    double paddlewaiting = 1.0;
    double padllecatch = 0.0;
    double paddlelaunch = -0.5;
    int PURPLE_RED_MIN = 50;
    int PURPLE_BLUE_MIN = 50;
    int PURPLE_GREEN_MAX = 80;

    int GREEN_GREEN_MIN = 80;
    int GREEN_RED_MAX = 70;
    int GREEN_BLUE_MAX = 70;

    public static double IDEAL_SHOOT_DISTANCE = 97.0;
    public static double DISTANCE_TOLERANCE = 1.0;
    public static double ANGLE_TOLERANCE = 0.90;
    public static double ALIGN_ROTATE_GAIN = 0.03;
    public static double ALIGN_DRIVE_GAIN = 0.06;
    public static double ALIGN_MAX_POWER = 0.6;
    public static double SEARCH_SPIN_POWER = 0.6;


    double speed = 1.0;
    protected boolean autoAlignActive = false;
    protected boolean prevButtonA = false;
    protected long autoAlignStartTime = 0;
    public static long AUTO_ALIGN_TIMEOUT_MS = 5000;
    protected double savedTagHeading = Double.NaN;
//Alliance selection method
    protected boolean isRedAlliance = false;

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
                        -gamepad1.left_stick_x*speed
                ),
                -gamepad1.right_stick_x*speed
        ));
    }
    public void intake(MecanumDrive drive){
        if(gamepad2.right_bumper){
            drive.intakeOne.setPower(-1.0);
        } else {
            drive.intakeOne.setPower(0);
        }
        if (gamepad2.left_bumper) {
            drive.intakeTwo.setPower(-1.0);
        } else {
            drive.intakeTwo.setPower(0);
        }
        if (gamepad2.x){
            drive.intakeOne.setPower(0.5);
            drive.intakeTwo.setPower(0.5);
        }
    }
    public void shooter(MecanumDrive drive, PID_Tune tuner1, PID_Tune2 tuner2){// launcher1 = bottom launcher2 = top
        //drive.launcherOne.setPower(gamepad2.right_trigger);
        //drive.launcherTwo.setPower(gamepad2.left_trigger);

        PIDFCoefficients pidfCoefficients1 = new PIDFCoefficients(tuner1.P, 0,0 , tuner1.F);
        drive.launcherOne.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, pidfCoefficients1);

        PIDFCoefficients pidfCoefficients2 = new PIDFCoefficients(tuner2.P, 0,0 , tuner2.F);
        drive.launcherTwo.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, pidfCoefficients2);
        if (gamepad2.right_trigger > 0.5){
            drive.launcherOne.setVelocity(1500); //bottom wheel
            drive.launcherTwo.setVelocity(750); //top wheel
        }
        else if(gamepad2.left_trigger > 0.5){
            drive.launcherOne.setVelocity(950);
            drive.launcherTwo.setVelocity(1450);
        }
        else if(gamepad2.right_stick_button){
            drive.pattern = RevBlinkinLedDriver.BlinkinPattern.BLUE;
            drive.blinkin.setPattern(drive.pattern);
            drive.launcherOne.setVelocity(10000);
            drive.launcherTwo.setVelocity(10000);
        }
        else {
            drive.launcherOne.setVelocity(0.0);
            drive.launcherTwo.setVelocity(0.0);
        }
        if (gamepad2.y){
            drive.pusherWheel.setPower(-1.0);
        }
        else {
            drive.pusherWheel.setPower(0.0);
        }
        if (gamepad2.x){
            drive.launcherOne.setVelocity(-500);
            drive.launcherTwo.setVelocity(-500);
            drive.pusherWheel.setPower(1.0);
        }
        /*if(gamepad2.left_trigger > 0.5){
            drive.launcherOne.setPower(1.0);
            drive.intakeTwo.setPower(-1.0);
        }
        else {
            drive.launcherOne.setPower(0.0);
            drive.intakeTwo.setPower(0.0);
        }*/
    }

    public void autoAlign(MecanumDrive drive, VisionTarget pillarTag, double[] botpose) {
        // Cancel if timed out
        if (System.currentTimeMillis() - autoAlignStartTime > AUTO_ALIGN_TIMEOUT_MS) {
            autoAlignActive = false;
            savedTagHeading = Double.NaN;
            drive.setDrivePowers(new PoseVelocity2d(new Vector2d(0, 0), 0));
            telemetry.addData("Auto-align", "TIMED OUT");
            return;
        }

        boolean botposeValid = botpose != null;
        double rotatePower = 0;
        double drivePower = 0;

        if (pillarTag.isTargetFound()) {
            // Camera sees the tag — use tx for rotation
            double angleDegrees = Math.toDegrees(pillarTag.getAngleToTarget());

            // If Limelight field map is active, save the world heading to this tag
            if (botposeValid) {
                double robotYawRad = Math.toRadians(botpose[5]);
                savedTagHeading = robotYawRad + pillarTag.getAngleToTarget();
            }

            if (Math.abs(angleDegrees) > ANGLE_TOLERANCE) {
                rotatePower = angleDegrees * ALIGN_ROTATE_GAIN;
                if (Math.abs(rotatePower) < 0.15) {
                    rotatePower = Math.copySign(0.15, rotatePower);
                }
                rotatePower = Math.max(-ALIGN_MAX_POWER, Math.min(ALIGN_MAX_POWER, rotatePower));
            } else {
                autoAlignActive = false;
                savedTagHeading = Double.NaN;
                telemetry.addData("Auto-align", "ALIGNED!");
            }
            telemetry.addData("Angle error", "%.1f deg", angleDegrees);

        } else if (!Double.isNaN(savedTagHeading) && botposeValid) {
            // Tag not visible but Limelight map tells us where we are —
            // rotate back toward the heading where we last saw the tag
            double robotYawRad = Math.toRadians(botpose[5]);
            double headingError = savedTagHeading - robotYawRad;
            while (headingError > Math.PI)  headingError -= 2 * Math.PI;
            while (headingError < -Math.PI) headingError += 2 * Math.PI;

            telemetry.addData("Auto-align", "RETURNING TO TAG");
            if (Math.abs(headingError) > Math.toRadians(ANGLE_TOLERANCE)) {
                rotatePower = Math.toDegrees(headingError) * ALIGN_ROTATE_GAIN;
                if (Math.abs(rotatePower) < 0.15) {
                    rotatePower = Math.copySign(0.15, rotatePower);
                }
                rotatePower = Math.max(-ALIGN_MAX_POWER, Math.min(ALIGN_MAX_POWER, rotatePower));
            }

        } else {
            // No tag, no map data — spin slowly to search
            rotatePower = SEARCH_SPIN_POWER;
            telemetry.addData("Auto-align", "SEARCHING...");
        }

        drive.setDrivePowers(new PoseVelocity2d(new Vector2d(drivePower, 0), rotatePower));
    }

    /*
    public void holder(MecanumDrive drive){
        int red = drive.paddle1.red();
        int blue = drive.paddle1.blue();
        int green = drive.paddle1.green();
        boolean detectPurple = false;
        boolean detectGreen = false;
        telemetry.addData("red",red);
        telemetry.addData("green",green);
        telemetry.addData("blue",blue);


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
        } else if (gamepad2.dpad_up) {
            drive.paddleOne.setPosition(paddlelaunch);
        } else {
            drive.paddleOne.setPosition(paddlewaiting);
        }
        if(detectPurple || detectGreen||gamepad2.dpad_left){
            drive.paddleTwo.setPosition(padllecatch);
        }else if (gamepad2.dpad_down) {
            drive.paddleTwo.setPosition(paddlelaunch);
        }
        else {
            drive.paddleTwo.setPosition(paddlewaiting);
        }*/
    //alliance selection method
    public void allianceSelect(){
        if (gamepad1.dpad_left){
            isRedAlliance = false;
        }
        if (gamepad1.dpad_right){
            isRedAlliance = true;
        }
        telemetry.addData("Alliance",isRedAlliance ? "Red" : "Blue");
        telemetry.addLine("Press LEFT for BLUE, RIGHT for RED");
    }

    public void visionTelemetry(VisionTarget pillarTag) {
        telemetry.addData("Alliance", isRedAlliance ? "RED" : "BLUE");

        if (pillarTag.isTargetFound()) {
            double distance = pillarTag.getDistance();
            double angleDegrees = Math.toDegrees(pillarTag.getAngleToTarget());

            boolean distanceOk = Math.abs(distance - IDEAL_SHOOT_DISTANCE) <=
                    DISTANCE_TOLERANCE;
            boolean angleOk = Math.abs(angleDegrees) <= ANGLE_TOLERANCE;
            boolean inRange = distanceOk && angleOk;

            String direction;
            if (angleDegrees > 1.0) {
                direction = String.format("%.1f\u00B0 right", angleDegrees);
            } else if (angleDegrees < -1.0) {
                direction = String.format("%.1f\u00B0 left",
                        Math.abs(angleDegrees));
            } else {
                direction = "CENTERED";
            }

            telemetry.addData("Pillar", "VISIBLE");
            telemetry.addData("raw ty", "%.2f", pillarTag.getTy());
            telemetry.addData("Distance", "%.1f in (target: %.1f)", distance,
                    IDEAL_SHOOT_DISTANCE);
            telemetry.addData("Direction", direction);
            telemetry.addData("Status", inRange ? ">>> IN RANGE <<<" : "OUT OF RANGE");

            //if (inRange) {
                //drive.blinkin.setPattern(RevBlinkinLedDriver.BlinkinPattern.GREEN);
            //} //else {
                //drive.blinkin.setPattern(RevBlinkinLedDriver.BlinkinPattern.RED);
            //}
        } else {
            telemetry.addData("Pillar", "NOT VISIBLE");
            telemetry.addData("Status", "Searching...");
            //blinkin.setPattern(RevBlinkinLedDriver.BlinkinPattern.BLUE);
        }
    }

}
