package org.firstinspires.ftc.teamcode;

import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;
import com.acmerobotics.roadrunner.ParallelAction;
import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.SequentialAction;
import com.acmerobotics.roadrunner.ftc.Actions;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

@Autonomous
public class BlueAutoClose extends LinearOpMode {

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
                launcher1.setVelocity(1500.0);
                launcher2.setVelocity(750.0);
                return System.currentTimeMillis() - startTime < 15000;
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
        public class IntakeBalls implements Action{
            private long startTime = -1;
            public boolean run(@NonNull TelemetryPacket packet){
                if (startTime < 0) startTime = System.currentTimeMillis();
                if (launcher1.getVelocity()>1445 && launcher2.getVelocity()>745){
                    intake1.setPower(-1.0);
                    intake2.setPower(-0.5);
                    pusherWheel.setPower(-1.0);
                }
                return System.currentTimeMillis() - startTime < 15000;
            }

        }
    }

    @Override
    public void runOpMode(){
        Pose2d initialPose = new Pose2d(0, 63, Math.toRadians(90));
        MecanumDrive drive = new MecanumDrive(hardwareMap, initialPose );
        /*drive.leftFront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        drive.leftBack.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        drive.rightFront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        drive.rightBack.setMode(DcMotor.RunMode.RUN_USING_ENCODER);*/
        Action moveOutOfStart = (drive.actionBuilder(initialPose))
                .lineToY(40)
                .build();
        waitForStart();
        if (isStopRequested()) return;
        PID_Tune tuner1 = new PID_Tune();
        PID_Tune2 tuner2 = new PID_Tune2();
        Shooter shooter = new Shooter(hardwareMap, tuner1, tuner2);
        Intake intake = new Intake(hardwareMap);
        drive.leftBack.setPower(-1.0);
        drive.leftFront.setPower(-1.0);
        drive.rightBack.setPower(-1.0);
        drive.rightFront.setPower(-1.0);
        sleep(750);
        drive.leftBack.setPower(0.0);
        drive.leftFront.setPower(0.0);
        drive.rightBack.setPower(0.0);
        drive.rightFront.setPower(0.0);
        Actions.runBlocking(
                new SequentialAction(
                        new ParallelAction(
                                shooter.new Shoot(),
                                intake.new IntakeBalls()
                        ),
                        moveOutOfStart
                )

        );
        drive.leftBack.setPower(-1.0);
        drive.leftFront.setPower(1.0);
        drive.rightBack.setPower(1.0);
        drive.rightFront.setPower(-1.0);
        sleep(750);
        drive.leftBack.setPower(0.0);
        drive.leftFront.setPower(0.0);
        drive.rightBack.setPower(0.0);
        drive.rightFront.setPower(0.0);
    }
}
