# FTC Limelight Vision System - Design Document

**Project:** 2025-2026 FTC Challenge
**Team:** MechaKnights
**Version:** 1.0
**Date:** 2025-11-17

---

## Table of Contents
1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Class Designs](#class-designs)
4. [Data Flow](#data-flow)
5. [Configuration Requirements](#configuration-requirements)
6. [Integration Points](#integration-points)

---

## Overview

### Purpose
This vision system enables the robot to:
- Detect and navigate to AprilTags (IDs 20-24) for pillar targeting
- Read center AprilTags (21-23) to determine ball collection sequence
- Detect and navigate to colored balls (purple/green) in the correct order
- Provide distance and angle data for autonomous navigation using the Mecanum drive system

### Key Requirements
- Alliance-based pillar selection (Tag 20 for Blue, Tag 24 for Red)
- Center tag interpretation for ball sequence mapping
- Ball color detection with closest target prioritization
- Integration with existing MecanumDrive without modification
- Road Runner Action-based navigation

---

## System Architecture

### Component Diagram
```
┌─────────────────────────────────────────────────────────────┐
│                    Autonomous OpMode                         │
│  - Reads alliance color                                     │
│  - Orchestrates vision → navigation flow                    │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ├─────────────────┬──────────────────────────┐
                 ▼                 ▼                          ▼
        ┌─────────────────┐ ┌──────────────┐    ┌─────────────────┐
        │ LimelightVision │ │VisionNavigator│    │  MecanumDrive   │
        │                 │ │               │    │                 │
        │ - AprilTag      │ │ - Uses both   │    │ - Drive control │
        │   detection     │◄┤   Limelight   │◄───┤ - Localization  │
        │ - Ball color    │ │   & Drive     │    │ - Pose tracking │
        │   detection     │ │ - Navigation  │    └─────────────────┘
        │ - Pipeline mgmt │ │   logic       │
        └─────────────────┘ └───────────────┘
```

### Package Structure
```
org.firstinspires.ftc.teamcode/
├── vision/
│   ├── BallColor.java           (Enum)
│   ├── TargetType.java          (Enum)
│   ├── VisionTarget.java        (Data class)
│   ├── LimelightVision.java     (Main vision interface)
│   └── VisionNavigator.java     (Navigation helper)
└── [existing OpModes and classes]
```

---

## Class Designs

### 1. BallColor.java (Enum)

**Package:** `org.firstinspires.ftc.teamcode.vision`

**Purpose:** Represents ball colors in the 2025 FTC challenge

**Values:**
- `PURPLE` - Purple colored balls
- `GREEN` - Green colored balls
- `NONE` - No ball / unknown color

**Usage Example:**
```java
BallColor targetColor = BallColor.PURPLE;
vision.switchToBallPipeline(targetColor);
```

---

### 2. TargetType.java (Enum)

**Package:** `org.firstinspires.ftc.teamcode.vision`

**Purpose:** Identifies the type of vision target being tracked

**Values:**
- `APRIL_TAG` - AprilTag marker detection
- `BALL` - Colored ball detection
- `NONE` - No target detected

**Usage Example:**
```java
VisionTarget target = vision.getPillarTarget(isRedAlliance);
if (target.getType() == TargetType.APRIL_TAG) {
    // Navigate to april tag
}
```

---

### 3. VisionTarget.java (Data Class)

**Package:** `org.firstinspires.ftc.teamcode.vision`

**Purpose:** Encapsulates all vision target information returned from Limelight

#### Fields

| Field | Type | Description |
|-------|------|-------------|
| `type` | `TargetType` | Type of target (APRIL_TAG, BALL, NONE) |
| `tx` | `double` | Horizontal offset in degrees (-29.8 to 29.8) |
| `ty` | `double` | Vertical offset in degrees (-24.85 to 24.85) |
| `ta` | `double` | Target area as % of image (0-100) |
| `distance` | `double` | Calculated distance to target (inches) |
| `angleToTarget` | `double` | Angle in radians to face target |
| `targetX` | `double` | Normalized screen X coordinate (-1 to 1) |
| `targetY` | `double` | Normalized screen Y coordinate (-1 to 1) |
| `aprilTagId` | `int` | AprilTag ID if applicable (20-24), -1 otherwise |
| `ballColor` | `BallColor` | Ball color if applicable, NONE otherwise |
| `targetFound` | `boolean` | True if target is visible |
| `timestamp` | `long` | System time when data was captured (ms) |

#### Methods

**Constructor:**
```java
public VisionTarget(TargetType type, double tx, double ty, double ta,
                   double distance, double angleToTarget, double targetX,
                   double targetY, int aprilTagId, BallColor ballColor,
                   boolean targetFound, long timestamp)
```

**Getters:**
- All fields have standard getter methods

**Factory Methods:**
```java
public static VisionTarget noTarget()
// Returns a VisionTarget indicating no target found
```

**Usage Example:**
```java
VisionTarget target = vision.getClosestBall(BallColor.PURPLE);
if (target.isTargetFound()) {
    double distance = target.getDistance();
    double angle = target.getAngleToTarget();
    // Use for navigation
}
```

---

### 4. LimelightVision.java (Main Vision Class)

**Package:** `org.firstinspires.ftc.teamcode.vision`

**Purpose:** Main interface to Limelight hardware, manages pipelines and target detection

#### Constants

```java
// Pipeline indices (must match Limelight configuration)
public static final int PIPELINE_APRILTAG = 0;
public static final int PIPELINE_PURPLE = 1;
public static final int PIPELINE_GREEN = 2;

// AprilTag IDs
public static final int APRILTAG_LEFT_PILLAR = 20;   // Blue alliance
public static final int APRILTAG_RIGHT_PILLAR = 24;  // Red alliance
public static final int APRILTAG_CENTER_START = 21;
public static final int APRILTAG_CENTER_END = 23;

// Limelight physical constants (MUST BE CALIBRATED)
public static final double LIMELIGHT_HEIGHT_INCHES = 8.0;      // Height from floor
public static final double LIMELIGHT_ANGLE_DEGREES = 15.0;     // Tilt angle
public static final double APRILTAG_HEIGHT_INCHES = 12.0;      // Target height
public static final double BALL_DIAMETER_INCHES = 3.0;         // Ball size
```

#### Fields

| Field | Type | Description |
|-------|------|-------------|
| `limelight` | `Limelight3A` | Hardware device reference |
| `limelightName` | `String` | Device name from config |
| `currentPipeline` | `int` | Active pipeline index |
| `ballSequence` | `List<BallColor>` | Ball order from center tag |
| `currentBallIndex` | `int` | Index in ball sequence |
| `telemetry` | `Telemetry` | For debugging output |

#### Constructor

```java
public LimelightVision(HardwareMap hardwareMap, String name, Telemetry telemetry)
```
- Initializes Limelight3A from hardware map
- Sets default pipeline to APRILTAG
- Initializes ball sequence as empty list
- Starts Limelight polling

#### Pipeline Management Methods

```java
public void switchToAprilTagPipeline()
// Switches to pipeline 0 for AprilTag detection
// Updates currentPipeline field

public void switchToBallPipeline(BallColor color)
// Switches to purple (1) or green (2) pipeline based on color
// Updates currentPipeline field
// Throws IllegalArgumentException if color is NONE

public int getCurrentPipeline()
// Returns current active pipeline index
```

#### AprilTag Detection Methods

```java
public VisionTarget getPillarTarget(boolean isRedAlliance)
```
- **Purpose:** Detects the pillar AprilTag based on alliance
- **Parameters:**
  - `isRedAlliance` - true for red (tag 24), false for blue (tag 20)
- **Process:**
  1. Switch to AprilTag pipeline
  2. Poll Limelight for results
  3. Filter for correct tag ID (20 or 24)
  4. Calculate distance using tag height
  5. Calculate angle to target in radians
  6. Package data into VisionTarget
- **Returns:** VisionTarget with tag data, or noTarget() if not found

```java
public List<BallColor> readCenterAprilTag()
```
- **Purpose:** Reads center tags (21-23) and determines ball collection sequence
- **Process:**
  1. Switch to AprilTag pipeline
  2. Scan for any tag ID 21, 22, or 23
  3. Map tag ID to ball sequence:
     - Tag 21 → [PURPLE, PURPLE, GREEN]
     - Tag 22 → [PURPLE, GREEN, PURPLE]
     - Tag 23 → [GREEN, PURPLE, PURPLE]
  4. Store sequence in `ballSequence` field
  5. Reset `currentBallIndex` to 0
- **Returns:** List of BallColor in collection order
- **Note:** Returns empty list if no center tag found

#### Ball Detection Methods

```java
public VisionTarget getClosestBall(BallColor color)
```
- **Purpose:** Finds the closest ball of specified color
- **Parameters:**
  - `color` - PURPLE or GREEN
- **Process:**
  1. Switch to appropriate color pipeline
  2. Get all detected targets
  3. Select target with largest area (ta) = closest
  4. Calculate distance using ball diameter
  5. Calculate angle to target
  6. Extract X/Y screen coordinates
  7. Package into VisionTarget
- **Returns:** VisionTarget with ball data, or noTarget() if not found

```java
public VisionTarget getNextBallInSequence()
```
- **Purpose:** Gets the next ball in the predetermined sequence
- **Process:**
  1. Check if sequence exists and index is valid
  2. Get color from `ballSequence[currentBallIndex]`
  3. Call `getClosestBall(color)`
  4. Increment `currentBallIndex`
- **Returns:** VisionTarget for next ball, or noTarget() if sequence complete
- **Note:** Throws IllegalStateException if readCenterAprilTag() not called first

```java
public void resetBallSequence()
// Resets currentBallIndex to 0 to restart sequence
```

#### Helper Methods

```java
private double calculateDistance(double ty, double targetHeightInches,
                                 double limelightHeightInches,
                                 double limelightAngleDegrees)
```
- **Purpose:** Calculate distance to target using Limelight geometry
- **Formula:**
  ```
  distance = (targetHeight - limelightHeight) / tan(limelightAngle + ty)
  ```
- **Returns:** Distance in inches

```java
private double calculateAngleRadians(double tx)
```
- **Purpose:** Convert horizontal offset to radians
- **Formula:** `radians = Math.toRadians(tx)`
- **Returns:** Angle in radians

```java
public boolean hasTarget()
```
- **Purpose:** Check if any target is currently visible
- **Process:** Checks if most recent LLResult is valid
- **Returns:** true if target detected

```java
public void updateTelemetry()
```
- **Purpose:** Output current vision data to telemetry
- **Displays:** Pipeline, target status, tx, ty, ta, distance

---

### 5. VisionNavigator.java (Navigation Helper)

**Package:** `org.firstinspires.ftc.teamcode.vision`

**Purpose:** Combines vision and drive systems to navigate to targets using Road Runner actions

#### Fields

| Field | Type | Description |
|-------|------|-------------|
| `drive` | `MecanumDrive` | Robot drive system |
| `vision` | `LimelightVision` | Vision system |
| `targetDistanceTolerance` | `double` | Distance tolerance (inches) |
| `targetAngleTolerance` | `double` | Angle tolerance (radians) |
| `maxSpeed` | `double` | Maximum drive speed (0-1) |

#### Constructor

```java
public VisionNavigator(MecanumDrive drive, LimelightVision vision)
```
- Stores references to drive and vision
- Sets default tolerances (2 inches, 0.1 radians)
- Sets default max speed (0.5)

#### Configuration Methods

```java
public void setTolerances(double distanceInches, double angleRadians)
// Set custom navigation tolerances

public void setMaxSpeed(double speed)
// Set maximum drive speed (0-1)
```

#### Navigation Action Methods

```java
public Action driveToAprilTag(boolean isRedAlliance, double speedMultiplier)
```
- **Purpose:** Creates Road Runner action to navigate to pillar AprilTag
- **Parameters:**
  - `isRedAlliance` - Determines which pillar (20 or 24)
  - `speedMultiplier` - Speed scaling (0-1)
- **Process:**
  1. Create continuous action
  2. Loop: Get pillar target from vision
  3. Calculate drive vector based on tx (angle) and distance
  4. Apply proportional control for smooth approach
  5. Stop when target centered and distance within tolerance
- **Returns:** Road Runner Action
- **Usage:**
  ```java
  Actions.runBlocking(
      navigator.driveToAprilTag(isRedAlliance, 0.5)
  );
  ```

```java
public Action driveToBall(BallColor color, double speedMultiplier)
```
- **Purpose:** Creates Road Runner action to navigate to colored ball
- **Parameters:**
  - `color` - PURPLE or GREEN
  - `speedMultiplier` - Speed scaling (0-1)
- **Process:**
  1. Switch to appropriate ball pipeline
  2. Create continuous action
  3. Loop: Get closest ball of color
  4. Convert screen X/Y to robot-relative movement
  5. Drive toward ball using proportional control
  6. Stop when ball centered and close
- **Returns:** Road Runner Action

```java
public Action driveToNextBallInSequence(double speedMultiplier)
```
- **Purpose:** Navigate to the next ball in the predetermined sequence
- **Process:**
  1. Get next ball from vision.getNextBallInSequence()
  2. Call driveToBall() with that color
- **Returns:** Road Runner Action
- **Note:** Requires readCenterAprilTag() to have been called first

#### Coordinate Conversion Methods

```java
private Pose2d screenCoordsToRobotPose(double screenX, double screenY, double distance)
```
- **Purpose:** Convert Limelight screen coords to robot-relative pose
- **Process:**
  - Screen X (-1 to 1) → Robot rotation needed (radians)
  - Distance + screen Y → Forward movement adjustment
- **Returns:** Pose2d representing target position

```java
private boolean isTargetCentered(double tx, double ty)
```
- **Purpose:** Check if target is centered within tolerances
- **Process:** Compare tx and ty against angle tolerance
- **Returns:** true if centered

```java
private double calculateDriveSpeed(double error, double maxSpeed)
```
- **Purpose:** Calculate proportional drive speed based on error
- **Process:** Apply P controller with minimum speed threshold
- **Returns:** Drive speed (-maxSpeed to +maxSpeed)

---

## Data Flow

### Autonomous Sequence Flow

```
┌─────────────────────────────────────────────────────────────┐
│ 1. OpMode Start                                             │
│    - Initialize hardware                                    │
│    - Create LimelightVision and VisionNavigator            │
└──────────────────┬──────────────────────────────────────────┘
                   ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Read Center AprilTag (21-23)                            │
│    vision.readCenterAprilTag()                             │
│    → Returns ball sequence [color1, color2, color3]        │
└──────────────────┬──────────────────────────────────────────┘
                   ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Navigate to Pillar                                       │
│    VisionTarget pillar = vision.getPillarTarget(isRed)     │
│    navigator.driveToAprilTag(isRed, 0.5)                   │
└──────────────────┬──────────────────────────────────────────┘
                   ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Ball Collection Loop (for each ball in sequence)        │
│    ┌─────────────────────────────────────────────────────┐ │
│    │ 4a. Get next ball in sequence                       │ │
│    │     VisionTarget ball =                             │ │
│    │         vision.getNextBallInSequence()              │ │
│    └────────────┬────────────────────────────────────────┘ │
│                 ▼                                           │
│    ┌─────────────────────────────────────────────────────┐ │
│    │ 4b. Navigate to ball                                │ │
│    │     navigator.driveToNextBallInSequence(0.5)        │ │
│    └────────────┬────────────────────────────────────────┘ │
│                 ▼                                           │
│    ┌─────────────────────────────────────────────────────┐ │
│    │ 4c. Pick up ball (other subsystem)                  │ │
│    └────────────┬────────────────────────────────────────┘ │
│                 ▼                                           │
│    ┌─────────────────────────────────────────────────────┐ │
│    │ 4d. Return to pillar                                │ │
│    │     navigator.driveToAprilTag(isRed, 0.5)           │ │
│    └────────────┬────────────────────────────────────────┘ │
│                 ▼                                           │
│    ┌─────────────────────────────────────────────────────┐ │
│    │ 4e. Deposit ball (other subsystem)                  │ │
│    └────────────┬────────────────────────────────────────┘ │
│                 │                                           │
│    └────────────┴─ Repeat for next ball                    │
└─────────────────────────────────────────────────────────────┘
                   ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. Complete                                                 │
└─────────────────────────────────────────────────────────────┘
```

### Vision Pipeline Switching

```
Phase 1: Initial AprilTag Reading (Pipeline 0)
┌────────────────────────────────────────┐
│ vision.readCenterAprilTag()            │
│ - Switches to PIPELINE_APRILTAG (0)   │
│ - Scans for tags 21, 22, or 23        │
│ - Stores ball sequence                │
└────────────────────────────────────────┘

Phase 2: Pillar AprilTag Navigation (Pipeline 0)
┌────────────────────────────────────────┐
│ vision.getPillarTarget(isRed)          │
│ - Uses PIPELINE_APRILTAG (0)          │
│ - Filters for tag 20 or 24            │
│ - Returns distance and angle          │
└────────────────────────────────────────┘

Phase 3: Ball Collection (Pipeline 1 or 2)
┌────────────────────────────────────────┐
│ vision.getNextBallInSequence()         │
│ - Switches to PURPLE (1) or GREEN (2) │
│ - Detects closest ball                │
│ - Returns X/Y coordinates             │
└────────────────────────────────────────┘

Phase 4: Return to Pillar (Pipeline 0)
┌────────────────────────────────────────┐
│ vision.getPillarTarget(isRed)          │
│ - Switches back to PIPELINE_APRILTAG  │
│ - Navigates to deposit location       │
└────────────────────────────────────────┘
```

### Data Structure Flow

```
Limelight Hardware
      ↓
   LLResult (NetworkTables data)
      ↓
LimelightVision processing
      ↓
VisionTarget (structured data)
      ↓
VisionNavigator (navigation logic)
      ↓
MecanumDrive (motor commands)
      ↓
Robot Movement
```

---

## Configuration Requirements

### Limelight Pipeline Configuration

The Limelight must have three pipelines configured via the web interface (http://limelight.local:5801):

#### Pipeline 0: AprilTag Detection
- **Mode:** AprilTag
- **Tag Family:** 36h11 (FTC standard)
- **Tag IDs to detect:** 20-24
- **3D Pose:** Enabled
- **Targeting:**
  - Sort mode: Largest area
  - Valid target: Any tag 20-24
- **Output:**
  - Robot space: Enabled
  - Target space: Enabled

#### Pipeline 1: Purple Ball Detection
- **Mode:** Color Detection
- **HSV Thresholds:**
  - Hue: [YOUR_PURPLE_MIN, YOUR_PURPLE_MAX]
  - Saturation: [YOUR_SAT_MIN, YOUR_SAT_MAX]
  - Value: [YOUR_VAL_MIN, YOUR_VAL_MAX]
- **Contour Filtering:**
  - Area: Minimum 0.5% of image
  - Aspect ratio: 0.7 - 1.3 (roughly circular)
  - Sort: Largest area first
- **Output:**
  - tx: Horizontal offset
  - ty: Vertical offset
  - ta: Target area

#### Pipeline 2: Green Ball Detection
- **Mode:** Color Detection
- **HSV Thresholds:**
  - Hue: [YOUR_GREEN_MIN, YOUR_GREEN_MAX]
  - Saturation: [YOUR_SAT_MIN, YOUR_SAT_MAX]
  - Value: [YOUR_VAL_MIN, YOUR_VAL_MAX]
- **Contour Filtering:**
  - Same as pipeline 1
- **Output:**
  - tx: Horizontal offset
  - ty: Vertical offset
  - ta: Target area

### FTC Robot Configuration

In the FTC Driver Station → Configure Robot:

1. **Add Limelight Device:**
   - Device type: `Limelight 3A`
   - Device name: `limelight` (or customize in constructor)
   - Connection: USB or Control Hub

2. **Verify Existing Devices:**
   - MecanumDrive motors configured
   - IMU configured
   - Other subsystems as needed

### Physical Mounting

The constants in `LimelightVision.java` MUST be calibrated to your physical mounting:

```java
public static final double LIMELIGHT_HEIGHT_INCHES = [MEASURE_THIS];
public static final double LIMELIGHT_ANGLE_DEGREES = [MEASURE_THIS];
public static final double APRILTAG_HEIGHT_INCHES = [GAME_MANUAL_VALUE];
public static final double BALL_DIAMETER_INCHES = [GAME_MANUAL_VALUE];
```

Mounting recommendations:
- Mount Limelight centered on robot
- Angle upward 10-30° for best AprilTag detection
- Ensure clear field of view
- Secure against vibration

---

## Integration Points

### With MecanumDrive

The VisionNavigator uses the existing MecanumDrive class without modification:

```java
// In autonomous OpMode
MecanumDrive drive = new MecanumDrive(hardwareMap, startPose);
LimelightVision vision = new LimelightVision(hardwareMap, "limelight", telemetry);
VisionNavigator navigator = new VisionNavigator(drive, vision);

// Use Road Runner actions
Actions.runBlocking(
    new SequentialAction(
        navigator.driveToAprilTag(isRedAlliance, 0.5),
        navigator.driveToBall(BallColor.PURPLE, 0.5)
    )
);
```

### With Road Runner

All navigation methods return Road Runner `Action` objects:

- Compatible with `Actions.runBlocking()`
- Compatible with `SequentialAction` and `ParallelAction`
- Can be composed with other actions (lift, intake, etc.)

Example composition:
```java
Actions.runBlocking(
    new SequentialAction(
        navigator.driveToBall(BallColor.PURPLE, 0.5),
        intake.grab(),
        navigator.driveToAprilTag(isRed, 0.5),
        lift.raise(),
        intake.release()
    )
);
```

### With Telemetry

The LimelightVision class includes telemetry output:

```java
LimelightVision vision = new LimelightVision(hardwareMap, "limelight", telemetry);

// In loop
vision.updateTelemetry();
telemetry.update();
```

Displays:
- Current pipeline
- Target found status
- tx, ty, ta values
- Calculated distance
- AprilTag ID or ball color

---

## Testing Strategy

### Unit Testing

1. **LimelightVision Tests:**
   - Test pipeline switching
   - Test distance calculations with known tx/ty values
   - Test tag ID filtering
   - Test ball sequence mapping

2. **VisionTarget Tests:**
   - Test data encapsulation
   - Test noTarget() factory

3. **VisionNavigator Tests:**
   - Test coordinate conversion
   - Test tolerance checking

### Integration Testing

1. **Hardware Tests:**
   - Verify Limelight connection
   - Test each pipeline individually
   - Calibrate distance calculations

2. **Navigation Tests:**
   - Test AprilTag approach from different angles
   - Test ball approach accuracy
   - Test sequence execution

3. **Full Autonomous:**
   - Test complete ball collection sequence
   - Test error recovery (target lost, etc.)
   - Test time constraints

---

## Error Handling

### Vision Target Not Found

```java
VisionTarget target = vision.getPillarTarget(isRed);
if (!target.isTargetFound()) {
    // Fallback: rotate to search
    // Or: use last known position
    // Or: abort and use alternative strategy
}
```

### Pipeline Switch Failures

- LimelightVision internally handles pipeline switching
- If switch fails, maintains current pipeline
- Telemetry warnings displayed

### Distance Calculation Edge Cases

- If target too close (ty > 20°): Returns minimum distance
- If target too far (ty < -20°): Returns maximum distance
- Prevents divide-by-zero in tan() calculations

---

## Future Enhancements

### Potential Additions

1. **Kalman Filtering:**
   - Smooth target position estimates
   - Reduce jitter in navigation

2. **Multi-target Tracking:**
   - Track multiple balls simultaneously
   - Choose optimal ball based on position

3. **Field Localization:**
   - Use AprilTag 3D pose for absolute field positioning
   - Integration with Road Runner localizer

4. **Adaptive Thresholding:**
   - Auto-adjust HSV thresholds based on lighting
   - Improve reliability in different venues

5. **Vision-based Odometry:**
   - Use AprilTags for continuous pose correction
   - Reduce drift during autonomous

---

## Appendix

### AprilTag ID to Ball Sequence Mapping

| Tag ID | Ball Sequence | Description |
|--------|--------------|-------------|
| 21 | [PURPLE, PURPLE, GREEN] | Two purple, then green |
| 22 | [PURPLE, GREEN, PURPLE] | Purple, green, purple |
| 23 | [GREEN, PURPLE, PURPLE] | Green, then two purple |

### Limelight Coordinate System

- **tx:** Horizontal offset (-29.8° to +29.8°)
  - Negative = target left of crosshair
  - Positive = target right of crosshair

- **ty:** Vertical offset (-24.85° to +24.85°)
  - Negative = target below crosshair
  - Positive = target above crosshair

- **ta:** Target area (0% to 100%)
  - Larger = closer to camera

- **Robot Space (3D):**
  - X: Forward (inches)
  - Y: Left (inches)
  - Z: Up (inches)

### Reference Links

- [Limelight Documentation](https://docs.limelightvision.io/)
- [FTC AprilTag Library](https://github.com/FIRST-Tech-Challenge/FtcRobotController)
- [Road Runner Documentation](https://rr.brott.dev/)

---

**Document End**