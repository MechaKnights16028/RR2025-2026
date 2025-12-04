# Keeping Test Code Synchronized with TeamCode

## Why Synchronization Matters

The LimelightTester module tests your **ACTUAL** robot vision code by using **EXACT COPIES** of the formulas from `LimelightVision.java`. This ensures that:
- If tests pass, the math works on the robot
- Distance calculations are validated before deployment
- Calibration can be tuned without deploying to hardware

## Files That Must Stay In Sync

### Critical: Pipeline Constants

**TeamCode Location:**
```
TeamCode/src/main/java/org/firstinspires/ftc/teamcode/vision/LimelightVision.java
Lines 28-38: Pipeline configuration constants
```

**Test Location:**
```
LimelightTester/src/main/java/tester/RealVisionWrapper.java
Lines 25-35: Pipeline configuration constants
```

**Pipeline Configuration:**
```java
PIPELINE_PURPLE = 0;        // Purple ball detection
PIPELINE_GREEN = 1;         // Green ball detection
PIPELINE_PILLAR_TAGS = 2;   // Pillar AprilTags (20, 24)
PIPELINE_CENTER_TAGS = 3;   // Center AprilTags (21, 22, 23)
```

### Critical: Distance Calculation Formula

**TeamCode Location:**
```
TeamCode/src/main/java/org/firstinspires/ftc/teamcode/vision/LimelightVision.java
Lines 321-338: calculateDistance() method
```

**Test Location:**
```
LimelightTester/src/main/java/tester/RealVisionWrapper.java
Lines 216-233: calculateDistance() method
```

**Formula:**
```java
distance = (targetHeight - limelightHeight) / Math.tan(Math.toRadians(limelightAngle + ty))
```

### Critical: Calibration Constants

**TeamCode Location:**
```
LimelightVision.java lines 56-63:
- LIMELIGHT_HEIGHT_INCHES = 40.0
- LIMELIGHT_ANGLE_DEGREES = 15.0
- APRILTAG_HEIGHT_INCHES = 36.0
```

**Test Location:**
```
RealVisionWrapper.java lines 43-50:
Same constants
```

**IMPORTANT:** These values must be measured on your robot. The Limelight height is measured from the floor to the camera lens center. The AprilTag height is measured from the floor to the center of the AprilTag.

### Important: Vision Logic

**TeamCode Location:**
```
LimelightVision.java:
- getPillarTarget() - lines 177-231
- readCenterAprilTag() - lines 233-305
- switchToPillarTagPipeline() - lines 119-125
- switchToCenterTagPipeline() - lines 131-137
```

**Test Location:**
```
RealVisionWrapper.java:
- getPillarTarget() - lines 104-150
- readCenterAprilTag() - lines 160-206
- switchToPillarTagPipeline() - lines 65-70
- switchToCenterTagPipeline() - lines 76-81
```

### Critical: Ball Sequence Mapping

**AprilTag to Ball Sequence:**
```
Tag 21 → [GREEN, PURPLE, PURPLE]
Tag 22 → [PURPLE, GREEN, PURPLE]
Tag 23 → [PURPLE, PURPLE, GREEN]
```

**TeamCode Location:**
```
LimelightVision.java lines 266-287: Ball sequence switch statement
```

**Test Location:**
```
RealVisionWrapper.java lines 177-199: Ball sequence switch statement
```

**IMPORTANT:** This mapping determines the order in which balls are collected during autonomous. Any change to this mapping must be updated in both files.

## How to Keep in Sync

### When You Change LimelightVision.java

1. **Note the line numbers** of what you changed
2. **Find the corresponding code** in RealVisionWrapper.java (comments show line numbers)
3. **Copy the changes exactly** - line by line
4. **Rebuild the test system**: `./gradlew :LimelightTester:build`
5. **Re-run tests** to verify: `./run-limelight-tests.sh`

### Example: Updating Calibration

If you update `LIMELIGHT_HEIGHT_INCHES` in TeamCode:

```java
// TeamCode/vision/LimelightVision.java:57
public static final double LIMELIGHT_HEIGHT_INCHES = 42.0;  // Changed from 40.0
```

You **MUST** update RealVisionWrapper:

```java
// LimelightTester/RealVisionWrapper.java:44
public static final double LIMELIGHT_HEIGHT_INCHES = 42.0;  // Changed from 40.0
```

Then rebuild:
```bash
./gradlew :LimelightTester:build
```

## Verification

After syncing, run these tests to verify:

```bash
./run-limelight-tests.sh
```

Select **Test 1: Distance Calculation** and measure actual vs calculated distance. If the formula is synced correctly, error should be < 10%.

## What Happens If They're Out of Sync?

- ❌ Test results won't match robot behavior
- ❌ Distance calculations will be wrong
- ❌ You might tune calibration for the wrong formula
- ✅ But your **robot code** is still fine - only tests are affected

## Quick Check: Are They In Sync?

Run this command to compare the key constants:

```bash
# Check TeamCode
grep -E "LIMELIGHT.*INCHES|APRILTAG.*INCHES" TeamCode/src/main/java/org/firstinspires/ftc/teamcode/vision/LimelightVision.java

# Check Test Code
grep -E "LIMELIGHT.*INCHES|APRILTAG.*INCHES" LimelightTester/src/main/java/tester/RealVisionWrapper.java
```

Values should match exactly.

## Why Not Use the Actual TeamCode Classes?

We tried! But:
- `LimelightVision.java` depends on FTC SDK classes (HardwareMap, Limelight3A, etc.)
- FTC SDK is Android-only and can't run on your laptop
- Creating full mocks of the FTC SDK is complex and error-prone
- **Copying the formulas is simpler and just as effective for testing**

The downside is manual syncing, but the benefit is **fast, reliable testing** without deploying to the robot.
