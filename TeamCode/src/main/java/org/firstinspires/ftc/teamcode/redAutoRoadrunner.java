package org.firstinspires.ftc.teamcode;

import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;
import com.acmerobotics.roadrunner.ParallelAction;
import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.SequentialAction;
import com.acmerobotics.roadrunner.Vector2d;
import com.acmerobotics.roadrunner.ftc.Actions;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

@Autonomous
public class redAutoRoadrunner extends LinearOpMode {

    public class Shooter{
        private DcMotorEx launcher1;
        private DcMotorEx launcher2;
        public Shooter(HardwareMap hardwareMap, PID_Tune tuner1, PID_Tune2 tuner2){
            launcher1 = hardwareMap.get(DcMotorEx.class, "launcherOne");
            launcher2 = hardwareMap.get(DcMotorEx.class, "launcherTwo");
            PIDFCoefficients pidfCoefficients1 = new PIDFCoefficients(tuner1.P, 0,0 , tuner1.F);
            launcher1.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, pidfCoefficients1);

            PIDFCoefficients pidfCoefficients2 = new PIDFCoefficients(tuner2.P, 0,0 , tuner2.F);
            launcher2.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, pidfCoefficients2);
        }
        public class Shoot implements Action{
            private long startTime = -1;
            public boolean run(@NonNull TelemetryPacket packet) {
                if (startTime < 0) startTime = System.currentTimeMillis();
                launcher1.setVelocity(1400.0);
                launcher2.setVelocity(900.0);
                return System.currentTimeMillis() - startTime < 5000;
            }
        }
    }

    public class Intake{
        private CRServo intake1;
        private CRServo intake2;
        private CRServo pusherWheel;
        private DcMotorEx launcher1;
        private DcMotorEx launcher2;

        public Intake(HardwareMap hardwareMap){
            intake1 = hardwareMap.get(CRServo.class, "intakeOne");
            intake2 = hardwareMap.get(CRServo.class,"intakeTwo");
            pusherWheel = hardwareMap.get(CRServo.class,"pusherwheel");
            launcher1 = hardwareMap.get(DcMotorEx.class, "launcherOne");
            launcher2 = hardwareMap.get(DcMotorEx.class, "launcherTwo");
        }
        public class PushBalls implements Action{
            private long startTime = -1;
            public boolean run(@NonNull TelemetryPacket packet){
                if (startTime < 0) startTime = System.currentTimeMillis();
                if (launcher1.getVelocity()>1300 && launcher2.getVelocity()>800){
                    intake1.setPower(-0.5);
                    intake2.setPower(-0.25);
                    pusherWheel.setPower(-1.0);
                }
                return System.currentTimeMillis() - startTime < 5000;
            }

        }
        public class IntakeBalls implements Action{
            private long startTime = -1;
            public boolean run(@NonNull TelemetryPacket packet){
                if (startTime < 0) startTime = System.currentTimeMillis();
                intake1.setPower(-0.5);
                intake2.setPower(-0.25);
                launcher1.setVelocity(-250);
                launcher2.setVelocity(-250);
                pusherWheel.setPower(0);
                return System.currentTimeMillis() - startTime < 2500;
            }

        }
        public class stopShooter implements Action{
            private long startTime = -1;
            public boolean run(@NonNull TelemetryPacket packet){
                if (startTime < 0) startTime = System.currentTimeMillis();
                launcher1.setVelocity(0);
                launcher2.setVelocity(0);
                pusherWheel.setPower(0);
                return System.currentTimeMillis() - startTime < 10;
            }

        }
    }

    @Override
    public void runOpMode(){
        Pose2d initialPose = new Pose2d(0, 63, Math.toRadians(0));
        MecanumDrive drive = new MecanumDrive(hardwareMap, initialPose );
        /*drive.leftFront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        drive.leftBack.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        drive.rightFront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        drive.rightBack.setMode(DcMotor.RunMode.RUN_USING_ENCODER);*/
        Action moveOutOfStart = (drive.actionBuilder(initialPose))
                .splineToLinearHeading(new Pose2d(5, 68, Math.toRadians(-7)), Math.toRadians(90))
                .build();
        Action moveToPickupOne = (drive.actionBuilder(new Pose2d(5, 68, Math.toRadians(-7))))
                .splineToLinearHeading(new Pose2d(18, 67, Math.toRadians(62)), Math.toRadians(90))
                .build();
        Action PickUpOne = (drive.actionBuilder(new Pose2d(18, 67, Math.toRadians(62))))
                .splineToLinearHeading(new Pose2d(22, 27, Math.toRadians(62)), Math.toRadians(90))
                .build();
        Action moveToSecondShot = (drive.actionBuilder(new Pose2d(22, 27, Math.toRadians(62))))
                .splineToLinearHeading(new Pose2d(2, 50, Math.toRadians(15)), Math.toRadians(90))
                .build();
        Action moveFromSecondShot = (drive.actionBuilder(new Pose2d(3, 48, Math.toRadians(15))))
                .splineToLinearHeading(new Pose2d(25, 40, Math.toRadians(15)), Math.toRadians(90))
                .build();

        waitForStart();
        if (isStopRequested()) return;
        PID_Tune tuner1 = new PID_Tune();
        PID_Tune2 tuner2 = new PID_Tune2();
        Shooter shooter = new Shooter(hardwareMap, tuner1, tuner2);
        Intake intake = new Intake(hardwareMap);
        Actions.runBlocking(
                new SequentialAction(
                        /*new ParallelAction(
                                shooter.new Shoot(),
                                intake.new PushBalls()
                        ),*/
                        moveOutOfStart,
                        new ParallelAction(
                                shooter.new Shoot(),
                                intake.new PushBalls()
                        ),
                        //intake.new stopShooter(),
                        moveToPickupOne,
                        new ParallelAction(
                                PickUpOne,
                                intake.new IntakeBalls()
                        ),
                        //intake.new stopShooter()
                        moveToSecondShot,
                        new ParallelAction(
                                shooter.new Shoot(),
                                intake.new PushBalls()
                        ),
                        moveFromSecondShot
                )

        );
    }
}
