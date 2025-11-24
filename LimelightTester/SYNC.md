# Keeping Test Code Synchronized with TeamCode

## Why Synchronization Matters

The LimelightTester module tests your **ACTUAL** robot vision code by using **EXACT COPIES** of the formulas from `LimelightVision.java`. This ensures that:
- If tests pass, the math works on the robot
- Distance calculations are validated before deployment
- Calibration can be tuned without deploying to hardware

## Files That Must Stay In Sync

### Critical: Distance Calculation Formula

**TeamCode Location:**
```
TeamCode/src/main/java/org/firstinspires/ftc/teamcode/vision/LimelightVision.java
Lines 305-322: calculateDistance() method
```

**Test Location:**
```
LimelightTester/src/main/java/tester/RealVisionWrapper.java
Lines 198-217: calculateDistance() method
```

**Formula:**
```java
distance = (targetHeight - limelightHeight) / Math.tan(Math.toRadians(limelightAngle + ty))
```

### Critical: Calibration Constants

**TeamCode Location:**
```
LimelightVision.java lines 52-59:
- LIMELIGHT_HEIGHT_INCHES = 8.0
- LIMELIGHT_ANGLE_DEGREES = 15.0
- APRILTAG_HEIGHT_INCHES = 12.0
```

**Test Location:**
```
RealVisionWrapper.java lines 25-27:
Same constants
```

### Important: Vision Logic

**TeamCode Location:**
```
LimelightVision.java:
- getPillarTarget() - lines 161-215
- readCenterAprilTag() - lines 226-289
```

**Test Location:**
```
RealVisionWrapper.java:
- getPillarTarget() - lines 75-124
- readCenterAprilTag() - lines 132-166
```

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
// TeamCode/vision/LimelightVision.java:52
public static final double LIMELIGHT_HEIGHT_INCHES = 10.0;  // Changed from 8.0
```

You **MUST** update RealVisionWrapper:

```java
// LimelightTester/RealVisionWrapper.java:25
public static final double LIMELIGHT_HEIGHT_INCHES = 10.0;  // Changed from 8.0
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
