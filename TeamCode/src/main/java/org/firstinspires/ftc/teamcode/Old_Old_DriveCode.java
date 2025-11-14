/*package org.firstinspires.ftc.teamcode;

import com.acmerobotics.roadrunner.Pose2d;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
@Disabled
@TeleOp
public class Old_Old_DriveCode extends Old_DriveCodeCommon {

    @Override
    public void runOpMode(){
        MecanumDrive drive = new MecanumDrive(hardwareMap, new Pose2d(0,0,0));
        waitForStart();
        declares();
        while(opModeIsActive()) {
            update(drive);
            drives(drive);
            vLift(drive);
            swingbar(drive);
            intakeServos(drive);
            hslides(drive);
            intakeBar(drive);
            head(drive);
            //plane(drive);
        }
    }
}
*/