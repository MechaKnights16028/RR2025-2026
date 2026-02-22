package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.roadrunner.Action;
import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.SequentialAction;
import com.acmerobotics.roadrunner.ftc.Actions;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import org.firstinspires.ftc.teamcode.vision.LimelightVision;
import org.firstinspires.ftc.teamcode.vision.VisionTarget;
@Config
@Autonomous(name = "Auto", group = "Autonomous")
public class Auto extends LinearOpMode {
    //configurable starting positions (tunable via FTC dashboard)
    //Blue alliance starting positions
    public static double BLUE_START_X = 0;
    public static double BLUE_START_Y = 63;
    public static double BLUE_START_HEADING_DEG = 90;

    //red alliance starting positions
    public static double RED_START_X = 0;
    public static double RED_START_Y = -63;
    public static double RED_START_HEADING_DEG = -90;

    //known field positions of pillar apriltags
    public static double BLUE_PILLAR_TAG_X = 0; //x pos of 20
    public static double BLUE_PILLAR_TAG_Y = -72; //y pos of 20

    public static double RED_PILLAR_TAG_X = 0; //x pos of 24
    public static double RED_PILLAR_TAG_Y = 72; //y pos of 24


    public void runOpMode(){
        //alliance selection variable
    boolean isRedAlliance = false;

        //Alliance selection during INIT phase
        while (!isStarted() && !isStopRequested()){
            if (gamepad1.dpad_left){
                isRedAlliance = false;
            }
            if(gamepad1.dpad_right){
                isRedAlliance = true;
            }
            telemetry.addData("Alliance", isRedAlliance ? "RED" : "Blue");
            telemetry.addLine("Press Left for Blue, Right for Red");
            telemetry.update();
        }
        //Determine initial pose based on alliance (rough estimate)
        Pose2d initialPose;
        if (isRedAlliance){
            initialPose = new Pose2d(RED_START_X, RED_START_Y, Math.toRadians(RED_START_HEADING_DEG));
        }
        else{
            initialPose = new Pose2d(BLUE_START_X, BLUE_START_Y, Math.toRadians(BLUE_START_HEADING_DEG));
        }
        //Use Limelight to refine starting position
        LimelightVision limelight = new LimelightVision(hardwareMap, "limelight", telemetry);
        telemetry.addLine("Refining position with Limelight...");
        telemetry.update();
        sleep(500); //give limelight time to stabilize

        VisionTarget pillarTag = limelight.getPillarTarget(isRedAlliance);

        if (pillarTag.isTargetFound()) {
            //get pillar tag position on field
            double pillarX = isRedAlliance ? RED_PILLAR_TAG_X : BLUE_PILLAR_TAG_X;
            double pillarY = isRedAlliance ? RED_PILLAR_TAG_Y : BLUE_PILLAR_TAG_Y;

            //get distance and angle to tag from robot
            double distanceToTag = pillarTag.getDistance();
            double angleToTag = pillarTag.getAngleToTarget();

            //calculate robot position relative to tag
            //robot's X = tag's X - (distance * cos(robot heading + angle to tag))
            //robot's Y = tag's Y - (distance * sin(robot heading + angle to tag))
            double robotHeading = initialPose.heading.log();
            double actualX = pillarX - (distanceToTag * Math.cos(robotHeading + angleToTag));
            double actualY = pillarY - (distanceToTag * Math.sin(robotHeading + angleToTag));

            //refine the pose
            initialPose = new Pose2d(actualX, actualY, initialPose.heading.log());

            telemetry.addData("Position", "REFINED using limelight");
            telemetry.addData("Distance to tag", "%.2f inches", distanceToTag);
        }else {
            telemetry.addData("position", "ESTIMATED (no tag detected)");
        }

        telemetry.addData("Final X", "%.2f", initialPose.position.x);
        telemetry.addData("Final Y", "%.2f", initialPose.position.y);
        telemetry.update();
        sleep(2000); //show refined position for 2 second
        limelight.stop(); //clean up limelight before auto starts

        //now create the drive with refined pose
        MecanumDrive drive = new MecanumDrive(hardwareMap, initialPose);

        //create turn action based on alliance
        Action moveOutOfStart;
        if (isRedAlliance){
            //Red alliance: turn LEFT 90 degrees
            moveOutOfStart = drive.actionBuilder(initialPose)
                    .turn(Math.toRadians(-90))
                    .build();
        }
        else {
            //Blue alliance: turn RIGHT 90 degress
            moveOutOfStart = drive.actionBuilder(initialPose)
                    .turn(Math.toRadians(-90))
                    .build();
        }

        waitForStart();
        if (isStopRequested()) return;
        Actions.runBlocking(
                new SequentialAction(
                        moveOutOfStart
                )
        );
    }
}
