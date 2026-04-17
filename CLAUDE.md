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

- **`DriveCodeCommon`** (`@Config`, extends `LinearOpMode`): Base class containing shared teleop logic and tunable field-detection thresholds (purple/green RGB mins/maxes, paddle servo positions).
  - `drives()`: Mecanum driving; right bumper on gamepad1 halves speed.
  - `intake()`: Runs the intake motor while gamepad2 right bumper is held.
  - `shooter()`: Runs the launcher at full reverse power.
  - `holder()`: Reads `paddle1` color sensor, moves `paddleOne` servo to `padllecatch` when purple or green is detected (or gamepad2 dpad_right is pressed), otherwise holds at `paddlewaiting`.

- **`DriveCode`**: Main `@TeleOp` that extends `DriveCodeCommon` and calls `drives`/`intake`/`holder`/`shooter` each loop iteration.

Note: `Old_DriveCodeCommon`, `Old_Old_DriveCode`, `Old_BlueAuto` are prior-season references kept for comparison — prefer editing the non-`Old_` versions.

### Autonomous Structure

- Road Runner `Action` system used for autonomous sequences. Actions can be composed with `SequentialAction` / `ParallelAction` and run via `Actions.runBlocking(...)`.
- Trajectories are built from `drive.actionBuilder(initialPose)` on `MecanumDrive`.
- Active autos include `BlueAuto`, `BlueAutoClose`, `blueAutoTest`, and `Auto`. `Old_BlueAuto` demonstrates the pattern of wrapping subsystems (lift, intake bar) as custom `Action` classes.

### Vision System

Located in `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/vision/`:

- **`LimelightVision`**: Main vision interface using Limelight3A camera. Pipeline assignments (see constants in the class):
  - **Pipeline 0** `PIPELINE_PURPLE`: purple ball color detection
  - **Pipeline 1** `PIPELINE_GREEN`: green ball color detection
  - **Pipeline 2** `PIPELINE_PILLAR_TAGS`: pillar AprilTags (20 = left/Blue, 24 = right/Red)
  - **Pipeline 3** `PIPELINE_CENTER_TAGS`: center AprilTags (21–23, determine ball sequence)
  - **Pipeline 5** `PIPELINE_WHITELINE`: whiteline detection
  - Calibration constants at the top of the class must be set per-robot: `LIMELIGHT_HEIGHT_INCHES`, `LIMELIGHT_ANGLE_DEGREES`, `APRILTAG_HEIGHT_INCHES`, `BALL_DIAMETER_INCHES`.

- **`VisionTarget`**, **`BallColor`**, **`TargetType`**: target representation and classification enums.
- **`LimelightHttpClient`**, **`LimelightVisionAdapter`**: auxiliary HTTP/adapter plumbing used by `LimelightTestUI`.

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

- **Drive motors** (`MecanumDrive`): `leftFront`, `leftBack`, `rightFront`, `rightBack` — all set to BRAKE, `rightFront` reversed.
- **Subsystem hardware also owned by `MecanumDrive`** (not a separate subsystem class):
  - `intake` (DcMotor)
  - `launcher` (DcMotor, reversed)
  - `paddleOne` (Servo)
  - `paddle1` (ColorSensor) — used by `DriveCodeCommon.holder()` to auto-trigger the paddle on purple/green detection
- **IMU**: `logoFacingDirection` / `usbFacingDirection` must be set in `MecanumDrive.Params` to match physical hub mounting.
- **Limelight camera**: Accessed via `LimelightVision` constructor; configure the camera name in Robot Controller config.

Hardware names in config must match exactly. Since drive-train and game-piece hardware all live in `MecanumDrive`, adding a new mechanism currently means extending that class rather than adding a separate subsystem.

## Development Notes

- **Java 8** source/target compatibility
- **Android SDK**: minSdk 24, targetSdk 28, compileSdk 30
- **Gradle wrapper**: Use `./gradlew` (Unix) or `gradlew.bat` (Windows)
- Project structure follows FTC SDK conventions with `FtcRobotController` and `TeamCode` modules
- The `@Config` annotation (from FTC Dashboard) makes class fields editable in real-time via dashboard
- Road Runner parameters marked with `public static` in `Params` classes can be tuned live
