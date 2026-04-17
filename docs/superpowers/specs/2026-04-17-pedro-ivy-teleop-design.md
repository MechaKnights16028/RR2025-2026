# Pedro Pathing + Ivy TeleOp — Design

- **Date:** 2026-04-17
- **Status:** Design approved by user; pending spec review and implementation plan
- **Scope:** Add a new command-based TeleOp to this FTC project, driven by Pedro Pathing (path following) and Ivy (command-based framework). Existing Road Runner code remains untouched for reference and continued autonomous use.

## 1. Motivation

The team currently has one TeleOp (`DriveCode` → `DriveCodeCommon`) built directly on Road Runner's `MecanumDrive`. All hardware (drive motors, intake, launcher, paddle servo, color sensor) is owned by a single class and controlled by imperative methods in the OpMode. That works, but it:

- Couples unrelated subsystems (drive + game-piece handling) in one class.
- Has no abstraction for "a button runs a short automated action," making driver-assist features awkward to add.
- Doesn't take advantage of pose-based path following during teleop.

This design introduces a second TeleOp built on Pedro Pathing + Ivy, intentionally preserving the existing Road Runner code so the team can compare the two approaches side-by-side for teaching purposes. The new TeleOp reproduces today's driver behavior and adds three driver-assist actions.

## 2. Goals

1. New TeleOp with identical manual feel to today's `DriveCode` (mecanum driving with slow-mode, intake-while-held, launcher-always-on, color-sensor-driven paddle catch).
2. Three driver-assist actions bound to gamepad buttons:
   - **Path to a field pose** (preset scoring pose).
   - **Snap to AprilTag** (pillar tags and center tags via Limelight).
   - **Reset pose** (re-seed Pedro's localizer to a known starting pose).
3. Command-based architecture using Ivy subsystems/commands/scheduler.
4. Support two odometry configurations: three dead wheels (current) and goBILDA Pinpoint (future), selectable at runtime via FTC Dashboard.
5. Heavy pedagogical comments in the code so the architecture is teachable to the team.

## 3. Non-goals

- Migrating autonomous (`BlueAuto`, `BlueAutoClose`, `blueAutoTest`, `Auto`) from Road Runner to Pedro. Road Runner stays in place.
- Deleting or modifying existing `MecanumDrive`, `DriveCode`, `DriveCodeCommon`, `Old_*`, or RR localizer files.
- Unit tests. FTC code is hardware-coupled; verification happens on-robot via a bring-up TeleOp (see §10).
- Curved/spline paths in v1 of `PathToPoseCommand`. Straight-line paths are sufficient for teaching and initial testing; splines can be a follow-up.
- Sharing a drive class between Pedro and Road Runner. Their abstractions don't align cleanly; side-by-side is simpler.

## 4. Dependencies and build changes

Pedro Pathing and Ivy are published to Maven Central. The project's existing `mavenCentral()` repository is sufficient — no new `maven { url ... }` block needed.

Add to `TeamCode/build.gradle` alongside the existing Road Runner dependencies (do not remove any):

```gradle
implementation "com.pedropathing:core:2.1.1"
implementation "com.pedropathing:ftc:2.1.1"
implementation "com.pedropathing:ivy:1.0.0"
```

**FTC SDK version.** Pedro 2.x requires a recent FTC SDK (10.x+). The current `FtcRobotController` module version must be verified before the implementation plan is executed; if below the Pedro minimum, the FTC SDK should be upgraded first.

**Android/NDK.** No changes. `compileSdkVersion 30`, `minSdkVersion 24`, Java 8 source/target remain as-is.

## 5. Package layout

All new code under `org.firstinspires.ftc.teamcode.pedro`:

```
TeamCode/src/main/java/org/firstinspires/ftc/teamcode/
├── (existing RR files unchanged)
└── pedro/
    ├── PedroTeleOp.java                  # @TeleOp entry point
    ├── PedroBringUpTeleOp.java           # debug @TeleOp: no driver-assist bindings (verification gate)
    ├── RobotContainer.java               # single place where subsystems + bindings + presets are wired
    ├── subsystems/
    │   ├── Drivetrain.java
    │   ├── Intake.java
    │   ├── Shooter.java
    │   └── Holder.java
    └── commands/
        ├── TeleOpDriveCommand.java
        ├── IntakeCommand.java
        ├── HolderAutoCommand.java
        ├── ResetPoseCommand.java
        ├── PathToPoseCommand.java
        └── SnapToAprilTagCommand.java
```

**Hardware names** stay identical to the Road Runner `MecanumDrive`:
- Drive: `leftFront`, `leftBack`, `rightFront`, `rightBack`
- Subsystems: `intake`, `launcher`, `paddleOne`, `paddle1`
- Odometry: `par0`, `par1`, `perp` (three dead wheels) or `pinpoint` (goBILDA Pinpoint)

No Robot Controller reconfiguration is required.

## 6. Subsystems

Subsystems own hardware and guarantee mutual exclusion — the Ivy scheduler will not run two commands that require the same subsystem simultaneously. Commands never touch hardware directly; they only call subsystem methods.

### 6.1 `Drivetrain`

Owns the four drive motors *and* Pedro's `Follower`. Only class in the project (new or old) that calls `setPower(...)` on a drive motor inside the Pedro world.

Public API:

- `drive(double x, double y, double rot, boolean slow)` — robot-relative teleop drive. `slow=true` halves all magnitudes (preserves today's `gamepad1.right_bumper` behavior).
- `setPose(Pose pose)` — re-seeds Pedro's localizer.
- `getPose() : Pose` — read-only accessor.
- `followPath(PathChain path)` / `isFollowing()` / `cancel()` — start/stop an automated path.
- `periodic()` — called every scheduler tick. Advances `follower.update()` while a path is active; otherwise no-op.

**Localizer selection** (see §7) lives entirely inside this class.

### 6.2 `Intake`

Owns the `intake` DcMotor.

- `setPower(double power)`
- `periodic()` — no-op

### 6.3 `Shooter`

Owns the `launcher` DcMotor (reversed, matching current wiring).

- `setPower(double power)`
- `periodic()` — no-op

### 6.4 `Holder`

Owns the `paddleOne` Servo and the `paddle1` ColorSensor. Bundles sensor + actuator because they're physically co-located and only meaningful together.

Public API:

- `detectBall() : BallState` where `BallState ∈ {NONE, PURPLE, GREEN}`. Encapsulates the RGB-threshold logic currently inline in `DriveCodeCommon.holder()`. Thresholds (`PURPLE_RED_MIN`, `PURPLE_BLUE_MIN`, `PURPLE_GREEN_MAX`, `GREEN_GREEN_MIN`, `GREEN_RED_MAX`, `GREEN_BLUE_MAX`) become tunable `@Config` constants on this class.
- `setPaddle(PaddleState state)` where `PaddleState ∈ {WAITING, CATCH, LAUNCH}`. Wraps the three servo positions currently named `paddlewaiting`, `padllecatch`, `paddlelaunch`.
- `periodic()` — no-op. The "catch on detect" policy lives in `HolderAutoCommand`, not here.

All four subsystems are `@Config` so dashboard users can tune constants live.

## 7. Localizer abstraction (three dead wheels + Pinpoint)

One TeleOp, two localizer options, swap via FTC Dashboard at Init time:

```java
@Config
public class Drivetrain {
    public enum LocalizerType { THREE_DEAD_WHEEL, PINPOINT }
    public static LocalizerType LOCALIZER = LocalizerType.THREE_DEAD_WHEEL;
    // nested @Config structs:
    public static class ThreeWheelParams { /* par_ticks_per_inch, perp_ticks_per_inch, par0_y, par1_y, perp_x — all TODO */ }
    public static class PinpointParams   { /* pod X/Y offsets, encoder resolution — all TODO */ }
    // ...
}
```

In the `Drivetrain` constructor, a small switch builds the Pedro `Follower` with the matching Pedro localizer:

- `THREE_DEAD_WHEEL` → Pedro's `ThreeWheelLocalizer`, encoder ports `par0`, `par1`, `perp`.
- `PINPOINT` → Pedro's `PinpointLocalizer`, I²C device name `pinpoint`.

**Important:** Pedro localizers are not drop-in replacements for the Road Runner versions in this repo (`ThreeDeadWheelLocalizer.java`, `PinpointLocalizer.java`). Sign conventions and constant names differ. All tuning values start as `TODO` placeholders and must be re-derived by running Pedro's tuning routines on the robot. A teaching comment in each Pedro config points to the corresponding RR class for diff/comparison.

## 8. Commands

Commands are verbs. Each declares which subsystems it **requires** (for scheduler exclusion) and implements `initialize()` / `execute()` / `end(interrupted)` / `isFinished()`.

### 8.1 Default commands (run whenever no other command uses the subsystem)

- **`TeleOpDriveCommand`** (requires `Drivetrain`) — reads `gamepad1` sticks + `right_bumper` and calls `drivetrain.drive(x, y, rot, slow)` every tick. Never finishes. Interrupted when a driver-assist command claims `Drivetrain`.
- **`ShooterRunCommand`** (requires `Shooter`) — calls `shooter.setPower(-1.0)` every tick. Mirrors today's always-on launcher. Stops cleanly on OpMode end (scheduler calls `end(true)`).
- **`HolderAutoCommand`** (requires `Holder`) — every tick: read `holder.detectBall()`. If `PURPLE` or `GREEN` or `gamepad2.dpad_right` held → `setPaddle(CATCH)`; else `setPaddle(WAITING)`. Direct port of `DriveCodeCommon.holder()`.

### 8.2 Triggered commands

- **`IntakeCommand`** (requires `Intake`) — `initialize()` sets power to `1.0`; `end()` sets `0.0`. Bound to `gamepad2.right_bumper` with `.whileTrue(...)`.
- **`ResetPoseCommand`** (requires `Drivetrain`) — one-shot. `initialize()` calls `drivetrain.setPose(STARTING_POSE)`; `isFinished()` returns `true` immediately.
- **`PathToPoseCommand`** (requires `Drivetrain`) — constructor takes a target `Pose`.
  - `initialize()`: build a straight-line `PathChain` from `drivetrain.getPose()` to the target and call `drivetrain.followPath(path)`.
  - `execute()`: no-op (`Drivetrain.periodic()` already advances the follower).
  - `isFinished()`: returns `!drivetrain.isFollowing() || driverOverride(gamepad1)`. `driverOverride` is a small static helper that checks whether any drive stick is past a 0.1 deadband. This gives the driver a one-touch cancel even though the command holds the `Drivetrain` requirement.
  - `end(interrupted)`: `drivetrain.cancel()`.
- **`SnapToAprilTagCommand`** (requires `Drivetrain`; uses `LimelightVision` as a non-requirement dependency) — constructor takes a `TargetType` enum (`NEAREST_PILLAR` or `CENTER_TAG`).
  - `initialize()`: query `LimelightVision` for a matching visible target. If none, set `abort=true`. Otherwise compute a standoff pose (`tag_pose - STANDOFF_INCHES` along the tag normal) and start a Pedro path to it.
  - `execute()`, `isFinished()`, `end()`: identical cancel-on-stick behavior as `PathToPoseCommand`. If `abort`, `isFinished()` returns immediately.

Both path-following commands share a small `FollowToPose` helper on `Drivetrain` so the `PathChain`-building and `followPath` call live in one place.

### 8.3 Cancellation note

Because driver-assist commands require `Drivetrain`, simply touching the stick does **not** cancel them — the default `TeleOpDriveCommand` can only run when nothing else requires `Drivetrain`. That's why `PathToPoseCommand.isFinished()` explicitly checks `driverOverride`. This is an intentional design choice and will be heavily commented.

## 9. OpMode skeleton and button bindings

### 9.1 `RobotContainer`

One file. Instantiates the four subsystems, registers default commands, and binds triggers → commands. The only place the reader needs to look to answer "what does each button do?"

Preset constants at the top:

```java
public static final Pose STARTING_POSE = new Pose(0, 0, 0);           // TODO: tune on field
public static final Pose SCORING_POSE  = new Pose(24, 0, 0);          // TODO: tune on field (placeholder = 24" forward)
public static final double APRILTAG_STANDOFF_INCHES = 24.0;           // TODO: tune on field
```

### 9.2 `PedroTeleOp.runOpMode()`

```java
@TeleOp(name = "Pedro TeleOp", group = "pedro")
public class PedroTeleOp extends LinearOpMode {
    @Override public void runOpMode() {
        RobotContainer robot = new RobotContainer(hardwareMap, gamepad1, gamepad2);
        Scheduler scheduler = Scheduler.getInstance();
        telemetry.addLine("Pedro TeleOp ready"); telemetry.update();
        waitForStart();
        while (opModeIsActive() && !isStopRequested()) {
            scheduler.run();                  // evaluates triggers, runs commands + subsystem periodic()
            robot.pollTelemetry(telemetry);
            telemetry.update();
        }
        scheduler.cancelAll();                // graceful end: interrupts all commands, calls end(true)
    }
}
```

### 9.3 Gamepad bindings

| Trigger | Command | Binding type |
|---|---|---|
| `gamepad1` left stick, right stick X, right bumper | `TeleOpDriveCommand` | default on `Drivetrain` |
| `gamepad1.a` held | `PathToPoseCommand(SCORING_POSE)` | `.whileTrue(...)` |
| `gamepad1.b` held | `SnapToAprilTagCommand(NEAREST_PILLAR)` | `.whileTrue(...)` |
| `gamepad1.y` held | `SnapToAprilTagCommand(CENTER_TAG)` | `.whileTrue(...)` |
| `gamepad1.back` pressed | `ResetPoseCommand` | `.onTrue(...)` |
| `gamepad2.right_bumper` held | `IntakeCommand` | `.whileTrue(...)` |
| `gamepad2.dpad_right` | read inside `HolderAutoCommand` | — |

Students can rebind by editing `RobotContainer` only.

## 10. Verification plan

On-robot only (no unit tests). Each step is a Go/No-Go gate.

1. **Compile.** `./gradlew :TeamCode:assembleDebug` passes.
2. **`PedroBringUpTeleOp`.** A debug-only TeleOp that registers `RobotContainer` with **no driver-assist bindings** — only default commands. Confirms (a) Pedro + Ivy resolve on-device, (b) subsystems initialize, (c) manual feel matches today's `DriveCode`. *If this feels the same as `DriveCode`, the port is correct.*
3. **Localizer check.** In `PedroBringUpTeleOp`, stream `drivetrain.getPose()` to FTC Dashboard. Drive forward 24", strafe 24", turn 360°, return to start. Pose should return near origin. If not, run Pedro's tuning routines and fill in `ThreeWheelParams` values.
4. **Reset-pose check.** Enable the Back → `ResetPoseCommand(Pose(0,0,0))` binding. Drive around. Press Back. Pose snaps to origin.
5. **Path-to-pose check.** `SCORING_POSE = (24, 0, 0)`. Hold A. Robot drives 24" forward and stops. Touching the stick cancels immediately. Only after this passes, replace with real field coordinates.
6. **Snap-to-AprilTag check.** Place a pillar tag (20 or 24) in view. Hold B with `STANDOFF_INCHES = 36` so misalignment is obvious before the bot gets close. Once stable, tune standoff down.

## 11. Teaching-comment style

The user's stated goal is "good explanations in the code that take us step by step into how to build it out." Comment policy for the new `pedro/` code:

- **File-level doc-comment** on every new class: one short paragraph explaining why the file exists and what role it plays in command-based. Reading only the headers in package order should give the full architecture.
- **Inline `// TEACHING NOTE:` comments** at non-obvious decision points. Concrete planned teaching notes:
  - In `Drivetrain.followPath(...)`: why `follower.update()` is in `periodic()` and not in the path command's `execute()`.
  - In `PathToPoseCommand.isFinished()`: why `driverOverride` is needed even though the driver is "moving the stick."
  - In `RobotContainer`: the difference between `.whileTrue(...)` and `.onTrue(...)`.
  - In `Drivetrain` localizer switch: why this is a strategy pattern and when that stops scaling.
  - In `PedroTeleOp.runOpMode()`: what `scheduler.run()` actually does every tick.
- **No narration** of code that already says what it does.
- Teaching notes use the `TEACHING NOTE:` prefix so they're greppable and strippable.

## 12. Assumptions and open questions

- Only one OpMode runs at a time in FTC → Pedro + Road Runner can coexist in the project without runtime conflict. ✓
- FTC SDK version in `FtcRobotController` is compatible with Pedro 2.1.1 — must be verified during the implementation plan.
- Preset poses and standoff distance are `TODO` — tuned on the field.
- Pedro's three-wheel and Pinpoint tuning constants are `TODO` — derived on the robot.
- Gamepad assignments in §9.3 are reasonable defaults and easy to rebind in `RobotContainer`.

## 13. Out-of-scope follow-ups

Potential future work, not part of this spec:

- Migrate autonomous off Road Runner onto Pedro (separate spec).
- Spline-based `PathToPoseCommand` for curved approaches.
- Additional driver-assist commands (e.g., "shoot sequence" chaining path + launcher + paddle).
- Removing Road Runner from the project once autonomous is migrated.

## 14. Success criteria

The design is successful if, after implementation and tuning:

1. `PedroTeleOp` feels identical to `DriveCode` in manual driving, intake, launcher, and paddle-catch behavior.
2. All three driver-assist actions work: path to preset pose, snap to AprilTag, reset pose.
3. Swapping `Drivetrain.LOCALIZER` at Init time between `THREE_DEAD_WHEEL` and `PINPOINT` works without code changes.
4. A team member unfamiliar with command-based can read the `pedro/` package header comments in order and explain the architecture back.
5. The existing Road Runner code and `DriveCode` still build and run unchanged.
