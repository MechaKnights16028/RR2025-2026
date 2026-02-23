package org.firstinspires.ftc.teamcode;


import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

@TeleOp
public class PID_Tune extends OpMode {

    public DcMotorEx flywheell;
    public double HighVelocity = 1100;
    public double LowVelocity = 500;

    double curTargetVelocity = HighVelocity;

    double F = 13.7;
    double P = 35;

    double[] stepSizes = {10, 1 , 0.1 , 0.01 , 0.001 , 0.0001};

    int stepIndex = 0;

    @Override
    public void init() {
        flywheell = hardwareMap.get(DcMotorEx.class, "flyl");
        flywheell.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        //flywheell.setDirection(DcMotorSimple.Direction.REVERSE);

        PIDFCoefficients pidfCoefficients = new PIDFCoefficients(P, 0, 0, F);
        flywheell.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, pidfCoefficients);
        telemetry.addLine("Init complete");
    }

    @Override
    public void loop(){
        // get all our gamepad commands
        // set target velocity
        // update telemetry

        if (gamepad1.yWasPressed()) {
            if (curTargetVelocity == HighVelocity) {
                curTargetVelocity = LowVelocity;
            }else {curTargetVelocity = HighVelocity; }
        }

        if (gamepad1.bWasPressed()) {
            stepIndex = (stepIndex + 1) % stepSizes.length;
        }

        if (gamepad1.dpadLeftWasPressed()) {
            F += stepSizes[stepIndex];
        }

        if (gamepad1.dpadRightWasPressed()) {
            F -= stepSizes[stepIndex];
        }

        if (gamepad1.dpadUpWasPressed()) {
            P += stepSizes[stepIndex];
        }

        if (gamepad1.dpadDownWasPressed()) {
            P -= stepSizes[stepIndex];
        }

        //set new pidf Coefficients
        PIDFCoefficients pidfCoefficients = new PIDFCoefficients(P, 0,0 , F);
        flywheell.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, pidfCoefficients);

        //set flywheel velocity
        flywheell.setVelocity(curTargetVelocity);

        double curVelocity = flywheell.getVelocity();
        double error = curTargetVelocity - curVelocity;

        telemetry.addData("Target Velocity", curTargetVelocity);
        telemetry.addData("Current Velocity", "%.2f", curVelocity);
        telemetry.addData("Error", "%2f", error);
        telemetry.addData("Tuning P", "%4f (D-Pad U/D)", P);
        telemetry.addData("Tuning F", "%4f (D-Pad L/R)", F);
        telemetry.addData("Step Size", "%4f (B Button)", stepSizes[stepIndex]);

    }


}
