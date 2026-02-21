# Auto-Align + Range Indicator Design

## Overview
During TeleOp, the driver can hold buttons to auto-align to the alliance pillar for shooting. LEDs indicate range status at all times.

## Core Behavior

### Three States
1. **Manual driving** (default) - Normal driving. Telemetry shows pillar info. LEDs update passively.
2. **Auto-aim** (hold gamepad1.a) - Robot rotates proportionally to center on pillar AprilTag.
3. **Auto-range** (hold gamepad1.b) - Robot drives forward/back to reach ideal shooting distance.

Holding both A+B does both simultaneously. Releasing returns to manual control. If pillar tag is not visible, auto-align does nothing.

### LED Indicators (REV Blinkin)
- **Green**: In range (distance AND angle within tolerance)
- **Red/Orange**: Pillar visible but NOT in range
- **Blue**: No pillar tag visible

### Tunable Constants (via FTC Dashboard)
- `IDEAL_SHOOT_DISTANCE` - target distance in inches
- `DISTANCE_TOLERANCE` - acceptable +/- inches
- `ANGLE_TOLERANCE` - acceptable degrees off-center
- `ALIGN_ROTATE_GAIN` - P gain for rotation
- `ALIGN_DRIVE_GAIN` - P gain for forward/back
- `ALIGN_MAX_POWER` - safety cap on auto-align motor power

## Files to Modify
- `DriveCodeCommon.java` - Add `autoAlign()` method, update `visionTelemetry()` with LED control
- `DriveCode.java` - Initialize Blinkin, restructure loop for auto-align override
- `MecanumDrive.java` - Add Blinkin servo to hardware map

## Loop Structure
```
while (opModeIsActive()) {
    if (gamepad1.a || gamepad1.b) {
        autoAlign(drive, limelight, blinkin);
    } else {
        drives(drive);
    }
    intake(drive);
    holder(drive);
    shooter(drive);
    visionTelemetry(limelight, blinkin);
    telemetry.update();
}
```

## Auto-Align Control
- Proportional (P) controller on both heading and distance
- Rotation power = `ALIGN_ROTATE_GAIN * angleDegrees`, capped at `ALIGN_MAX_POWER`
- Drive power = `ALIGN_DRIVE_GAIN * (distance - IDEAL_SHOOT_DISTANCE)`, capped at `ALIGN_MAX_POWER`
- Uses existing `setDrivePowers()` for motor output
