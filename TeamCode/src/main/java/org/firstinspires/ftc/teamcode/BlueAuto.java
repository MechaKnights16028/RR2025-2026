package org.firstinspires.ftc.teamcode;

import com.acmerobotics.roadrunner.Action;
import com.acmerobotics.roadrunner.Pose2d;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
@Autonomous
public class BlueAuto extends LinearOpMode {


    public void runOpMode(){
        Pose2d initialPose = new Pose2d(0, 63, Math.toRadians(90));
        MecanumDrive drive = new MecanumDrive(hardwareMap, initialPose );
        Action moveOutOfStart(drive.actionBuilder(initialPose))
                .lineToX(70)
                .build();
    }
}
