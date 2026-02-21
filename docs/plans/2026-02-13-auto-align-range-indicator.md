# Auto-Align + Range Indicator Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add auto-aim (rotate) and auto-range (drive) to pillar during TeleOp with Blinkin LED status indicators.

**Architecture:** Proportional controller in DriveCodeCommon overrides manual driving when gamepad1.a/b held. Blinkin LED on MecanumDrive shows green (in range), red (visible but out of range), blue (not visible). Tunable constants exposed via FTC Dashboard.

**Tech Stack:** FTC SDK, Road Runner MecanumDrive, REV Blinkin LED Driver, Limelight3A vision

---

### Task 1: Add Blinkin LED to MecanumDrive hardware

**Files:**
- Modify: `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/MecanumDrive.java`

**Step 1: Add RevBlinkinLedDriver import**

In `MecanumDrive.java`, add this import after line 41 (`import com.qualcomm.robotcore.hardware.Servo;`):

```java
import com.qualcomm.hardware.rev.RevBlinkinLedDriver;
```

**Step 2: Add blinkin field declaration**

After line 114 (`public final ColorSensor paddle1;`), add:

```java
public final RevBlinkinLedDriver blinkin;
```

**Step 3: Initialize blinkin in constructor**

After line 243 (`launcher = hardwareMap.get(DcMotor.class,"launcher");`), add:

```java
blinkin = hardwareMap.get(RevBlinkinLedDriver.class, "blinkin");
```

**Step 4: Commit**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/MecanumDrive.java
git commit -m "feat: add Blinkin LED driver to MecanumDrive hardware map"
```

---

### Task 2: Add tunable auto-align constants to DriveCodeCommon

**Files:**
- Modify: `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/DriveCodeCommon.java`

**Step 1: Add auto-align constants**

After line 24 (`int GREEN_BLUE_MAX = 70;`), add these `public static` fields (so FTC Dashboard can tune them live via the `@Config` annotation already on the class):

```java
    public static double IDEAL_SHOOT_DISTANCE = 48.0;
    public static double DISTANCE_TOLERANCE = 3.0;
    public static double ANGLE_TOLERANCE = 2.0;
    public static double ALIGN_ROTATE_GAIN = 0.02;
    public static double ALIGN_DRIVE_GAIN = 0.03;
    public static double ALIGN_MAX_POWER = 0.4;
```

**Step 2: Commit**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/DriveCodeCommon.java
git commit -m "feat: add tunable auto-align constants for shooting range"
```

---

### Task 3: Add autoAlign() method to DriveCodeCommon

**Files:**
- Modify: `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/DriveCodeCommon.java`

**Step 1: Add import for RevBlinkinLedDriver**

After line 7 (`import com.qualcomm.robotcore.hardware.DcMotor;`), add:

```java
import com.qualcomm.hardware.rev.RevBlinkinLedDriver;
```

**Step 2: Add autoAlign() method**

Add this method after the `shooter()` method (after line 59, `drive.launcher.setPower(-1.0); }`):

```java
    public void autoAlign(MecanumDrive drive, LimelightVision limelight) {
        VisionTarget pillarTag = limelight.getPillarTarget(isRedAlliance);

        if (!pillarTag.isTargetFound()) {
            // Can't see the tag - stop moving
            drive.setDrivePowers(new PoseVelocity2d(new Vector2d(0, 0), 0));
            return;
        }

        double distance = pillarTag.getDistance();
        double angleDegrees = Math.toDegrees(pillarTag.getAngleToTarget());

        double rotatePower = 0;
        double drivePower = 0;

        // A button: auto-rotate to center on pillar
        if (gamepad1.a) {
            rotatePower = angleDegrees * ALIGN_ROTATE_GAIN;
            rotatePower = Math.max(-ALIGN_MAX_POWER, Math.min(ALIGN_MAX_POWER, rotatePower));
        }

        // B button: auto-drive to ideal shooting distance
        if (gamepad1.b) {
            double distanceError = distance - IDEAL_SHOOT_DISTANCE;
            drivePower = distanceError * ALIGN_DRIVE_GAIN;
            drivePower = Math.max(-ALIGN_MAX_POWER, Math.min(ALIGN_MAX_POWER, drivePower));
        }

        drive.setDrivePowers(new PoseVelocity2d(
                new Vector2d(drivePower, 0),
                rotatePower
        ));
    }
```

**Notes for implementer:**
- `setDrivePowers` first argument is (axial, lateral), second is rotation
- Positive axial = forward, positive rotation = left turn
- The sign on `rotatePower` may need inverting during testing depending on which direction the angle is reported. If the robot turns away from the pillar instead of toward it, negate `angleDegrees * ALIGN_ROTATE_GAIN`
- Similarly, if `drivePower` drives the wrong direction, negate `distanceError * ALIGN_DRIVE_GAIN`

**Step 3: Commit**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/DriveCodeCommon.java
git commit -m "feat: add autoAlign() proportional controller for pillar targeting"
```

---

### Task 4: Update visionTelemetry() with LED control and range status

**Files:**
- Modify: `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/DriveCodeCommon.java`

**Step 1: Update visionTelemetry() signature and body**

Replace the entire existing `visionTelemetry` method (lines 108-131) with:

```java
    public void visionTelemetry(LimelightVision limelight, RevBlinkinLedDriver blinkin) {
        telemetry.addData("Alliance", isRedAlliance ? "RED" : "BLUE");

        VisionTarget pillarTag = limelight.getPillarTarget(isRedAlliance);

        if (pillarTag.isTargetFound()) {
            double distance = pillarTag.getDistance();
            double angleDegrees = Math.toDegrees(pillarTag.getAngleToTarget());

            // Check if in shooting range
            boolean distanceOk = Math.abs(distance - IDEAL_SHOOT_DISTANCE) <= DISTANCE_TOLERANCE;
            boolean angleOk = Math.abs(angleDegrees) <= ANGLE_TOLERANCE;
            boolean inRange = distanceOk && angleOk;

            // Direction display
            String direction;
            if (angleDegrees > 1.0) {
                direction = String.format("%.1f\u00B0 right", angleDegrees);
            } else if (angleDegrees < -1.0) {
                direction = String.format("%.1f\u00B0 left", Math.abs(angleDegrees));
            } else {
                direction = "CENTERED";
            }

            telemetry.addData("Pillar", "VISIBLE");
            telemetry.addData("Distance", "%.1f in (target: %.1f)", distance, IDEAL_SHOOT_DISTANCE);
            telemetry.addData("Direction", direction);
            telemetry.addData("Status", inRange ? ">>> IN RANGE <<<" : "OUT OF RANGE");

            // LED: green if in range, red if visible but out of range
            if (inRange) {
                blinkin.setPattern(RevBlinkinLedDriver.BlinkinPattern.GREEN);
            } else {
                blinkin.setPattern(RevBlinkinLedDriver.BlinkinPattern.RED);
            }
        } else {
            telemetry.addData("Pillar", "NOT VISIBLE");
            telemetry.addData("Status", "Searching...");

            // LED: blue when tag not visible
            blinkin.setPattern(RevBlinkinLedDriver.BlinkinPattern.BLUE);
        }
    }
```

**Step 2: Commit**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/DriveCodeCommon.java
git commit -m "feat: add LED range indicators and shooting status to vision telemetry"
```

---

### Task 5: Wire everything together in DriveCode

**Files:**
- Modify: `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/DriveCode.java`

**Step 1: Update DriveCode.java**

Replace the entire file contents with:

```java
package org.firstinspires.ftc.teamcode;

import com.acmerobotics.roadrunner.Pose2d;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.vision.LimelightVision;

@TeleOp
public class DriveCode extends DriveCodeCommon {
    @Override
    public void runOpMode() {
        MecanumDrive drive = new MecanumDrive(hardwareMap, new Pose2d(0, 0, 0));
        //Alliance selection during INIT phase
        while (!isStarted() && !isStopRequested()){
            allianceSelect();
            telemetry.update();
        }
        waitForStart();

        LimelightVision limelight = new LimelightVision(hardwareMap, "limelight", telemetry);

        while (opModeIsActive()) {
            if (gamepad1.a || gamepad1.b) {
                autoAlign(drive, limelight);
            } else {
                drives(drive);
            }
            intake(drive);
            holder(drive);
            shooter(drive);
            visionTelemetry(limelight, drive.blinkin);
            telemetry.update();
        }

        limelight.stop();
    }
}
```

**Key changes from current file:**
- Added `if (gamepad1.a || gamepad1.b)` check that calls `autoAlign()` instead of `drives()` when held
- Changed `visionTelemetry(limelight)` to `visionTelemetry(limelight, drive.blinkin)` to pass Blinkin
- Blinkin is accessed via `drive.blinkin` (initialized in MecanumDrive constructor from Task 1)

**Step 2: Commit**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/DriveCode.java
git commit -m "feat: wire auto-align and LED indicators into TeleOp main loop"
```

---

### Task 6: Build verification

**Step 1: Build the project**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew build
```

Expected: BUILD SUCCESSFUL. If compilation errors occur, fix them before proceeding.

**Step 2: Final commit if any fixes were needed**

---

## Testing on Robot

After deploying to the robot (`./gradlew installDebug`):

1. **Configure hardware**: In the Robot Controller config, add a Blinkin device named `"blinkin"` on a servo port
2. **Select DriveCode TeleOp** from Driver Station
3. **During INIT**: Press dpad_left (Blue) or dpad_right (Red)
4. **Press START**
5. **Test LEDs**: Point at pillar tag - should be red/green. Point away - should be blue
6. **Test auto-aim**: Hold A while pillar is visible - robot should rotate toward the tag
7. **Test auto-range**: Hold B while pillar is visible - robot should drive toward/away from ideal distance
8. **Test combined**: Hold A+B - robot should do both simultaneously
9. **Tune via FTC Dashboard**: Adjust `IDEAL_SHOOT_DISTANCE`, gains, tolerances, and `ALIGN_MAX_POWER` as needed

**Tuning tips:**
- Start with low `ALIGN_MAX_POWER` (0.2) and increase gradually
- If robot oscillates around the target, reduce the corresponding gain
- If robot is too sluggish, increase the gain
- The sign of `ALIGN_ROTATE_GAIN` or `ALIGN_DRIVE_GAIN` may need to be negated if the robot moves the wrong direction
