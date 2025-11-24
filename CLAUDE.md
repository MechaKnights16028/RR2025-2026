# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an FTC (FIRST Tech Challenge) robot controller application built on the Road Runner Quickstart template. The codebase uses Road Runner v1.0 for advanced motion planning and path following. The team code is located in `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/`.

**Key Documentation**: Road Runner tuning guide at https://rr.brott.dev/docs/v1-0/tuning/

## Build and Development Commands

### Building the Project
```bash
./gradlew build
```

### Installing to Robot Controller
The project uses Android Gradle plugin. To deploy to a connected FTC Robot Controller device:
```bash
./gradlew installDebug
```

### Clean Build
```bash
./gradlew clean
```

## Code Architecture

### Drive System Architecture

The drive system uses a **Localizer pattern** for position tracking:

- **`Localizer` interface**: Defines the contract for all localization methods (`setPose()`, `getPose()`, `update()`)
- **Localizer implementations**:
  - `OTOSLocalizer`: SparkFun OTOS sensor-based localization
  - `PinpointLocalizer`: Pinpoint odometry sensor
  - `ThreeDeadWheelLocalizer`: Three odometry wheel configuration
  - `TwoDeadWheelLocalizer`: Two odometry wheel configuration

- **`MecanumDrive`**: Primary drive class that integrates with Road Runner
  - Contains tunable parameters in `MecanumDrive.Params` (configured via FTC Dashboard)
  - Handles kinematics, motor control, and trajectory following
  - Key parameters: `inPerTick`, `lateralInPerTick`, `trackWidthTicks`, feedforward (`kS`, `kV`, `kA`), path/turn constraints, and controller gains
  - Integrates with chosen Localizer implementation

- **`TankDrive`**: Alternative drive implementation for tank drive robots

### TeleOp Structure

- **`DriveCodeCommon`**: Base class containing shared teleop logic
  - `drives()`: Main driving controls with speed modulation (right bumper for 50% speed)
  - `intake()`: Intake control logic

- **`DriveCode`**: Main TeleOp OpMode that extends `DriveCodeCommon`
  - Instantiates `MecanumDrive` with initial pose
  - Runs drive and intake methods in main loop

### Autonomous Structure

- Road Runner `Action` system used for autonomous sequences
- Actions can be composed with `SequentialAction` and `ParallelAction`
- Example autonomous: `Old_BlueAuto.java` shows pattern of creating custom Action classes for subsystems (lift, intake bar, etc.)
- Trajectories built using `TrajectoryActionBuilder` from `MecanumDrive`

### Vision System

Located in `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/vision/`:

- **`LimelightVision`**: Main vision interface using Limelight3A camera
  - **Pipeline 0**: AprilTag detection (tags 20-24)
    - Left pillar: tag 20 (Blue alliance)
    - Right pillar: tag 24 (Red alliance)
    - Center tags: 21-23 (determine ball sequence)
  - **Pipeline 1**: Purple ball detection
  - **Pipeline 2**: Green ball detection
  - **Calibration constants** (must be set for your robot):
    - `LIMELIGHT_HEIGHT_INCHES`: Camera height from floor
    - `LIMELIGHT_ANGLE_DEGREES`: Camera tilt angle
    - `APRILTAG_HEIGHT_INCHES`: AprilTag center height
    - `BALL_DIAMETER_INCHES`: Game ball diameter

- **`VisionTarget`**: Represents detected targets with position and distance
- **`BallColor`** and **`TargetType`**: Enums for classification

### Message System

The `messages/` package contains data classes for FTC Dashboard telemetry logging:
- `DriveCommandMessage`, `MecanumCommandMessage`, `TankCommandMessage`: Command logging
- `MecanumLocalizerInputsMessage`, `TankLocalizerInputsMessage`: Localizer sensor data
- `ThreeDeadWheelInputsMessage`, `TwoDeadWheelInputsMessage`: Dead wheel encoder data
- `PoseMessage`: Robot pose data

These integrate with Road Runner's `FlightRecorder` for debugging.

### Tuning OpModes

Located in `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/tuning/`:
- `LocalizationTest`: Verify localizer accuracy
- `ManualFeedbackTuner`: Tune feedforward and feedback parameters
- `SplineTest`: Test path following
- `TuningOpModes`: Collection of tuning utilities

Use FTC Dashboard (dependency: `com.acmerobotics.dashboard:dashboard:0.5.1`) to tune parameters in real-time.

## Key Dependencies

- **Road Runner FTC**: `com.acmerobotics.roadrunner:ftc:0.1.25`
- **Road Runner Core**: `com.acmerobotics.roadrunner:core:1.0.1`
- **Road Runner Actions**: `com.acmerobotics.roadrunner:actions:1.0.1`
- **FTC Dashboard**: `com.acmerobotics.dashboard:dashboard:0.5.1`

Dependencies are defined in `TeamCode/build.gradle` and pulled from `https://maven.brott.dev/`.

## Hardware Configuration

When writing code that accesses hardware:

- **Drive motors**: Configured in `MecanumDrive` constructor (check hardware map names)
- **IMU**: Requires `logoFacingDirection` and `usbFacingDirection` configuration in `MecanumDrive.Params`
- **Limelight camera**: Hardware name accessed via `LimelightVision` constructor
- **Subsystem motors/servos**: See `Old_BlueAuto.java` for examples (lift motors, intake bar servos, etc.)

Always verify hardware names match the Robot Controller configuration.

## Development Notes

- **Java 8** source/target compatibility
- **Android SDK**: minSdk 24, targetSdk 28, compileSdk 30
- **Gradle wrapper**: Use `./gradlew` (Unix) or `gradlew.bat` (Windows)
- Project structure follows FTC SDK conventions with `FtcRobotController` and `TeamCode` modules
- The `@Config` annotation (from FTC Dashboard) makes class fields editable in real-time via dashboard
- Road Runner parameters marked with `public static` in `Params` classes can be tuned live
