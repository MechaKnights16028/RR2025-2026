# Pedro Pathing + Ivy TeleOp — Design

- **Date:** 2026-04-17
- **Status:** Design approved by user; pending spec review and implementation plan
- **Scope:** Add a new command-based TeleOp to this FTC project, driven by Pedro Pathing (path following) and Ivy (command-based framework). Existing Road Runner code and `DriveCode` remain untouched for reference and continued use.

## 1. Motivation

The team's current TeleOp (`DriveCode` → `DriveCodeCommon`) is built directly on Road Runner's `MecanumDrive`. All hardware, alliance state, vision handling, and auto-align logic live as imperative methods in one 290-line base class. That works but is hard to extend — each new driver-assist feature becomes another conditional branch in `runOpMode`.

This design adds a second TeleOp built on Pedro Pathing + Ivy that reproduces every current driver behavior under a command-based architecture, preserves the existing Road Runner code for reference, and makes future driver-assist features drop-in additions instead of branch additions.

## 2. Goals

1. A new TeleOp (`@TeleOp(name = "Pedro TeleOp")`) that ports **every** behavior in today's `DriveCode`:
   - Mecanum driving with slow-mode (`gamepad1.right_bumper`).
   - Two independent intake CRServos (`gamepad2.right_bumper`, `gamepad2.left_bumper`), with an intake-reverse on `gamepad2.x`.
   - Two-wheel launcher with three fire modes (near / far / clear) driven by `gamepad2.right_trigger` / `left_trigger` / `right_stick_button`.
   - Pusher wheel on `gamepad2.y`.
   - Clear-jam macro on `gamepad2.x`: both intakes forward, both launchers reversed, pusher forward.
   - Live PIDF tuning of both launcher motors via FTC Dashboard (same surface as `PID_Tune` / `PID_Tune2`).
   - Blinkin LED set to BLUE during clear-mode shoot (preserved verbatim — see Quirks §13).
   - Alliance selection during INIT (`gamepad1.dpad_left` / `right`).
   - Pillar-tag auto-align, currently `gamepad1.a` toggle — re-implemented as a Pedro-based snap command.
   - Move-toward-pillar creep, currently `gamepad1.b` hold — re-implemented as a Pedro-based approach command.
2. Three additional driver-assist commands:
   - `PathToPoseCommand` — path to a preset field pose (e.g., scoring spot).
   - `SnapToAprilTagCommand` — Pedro snap to a standoff pose derived from a visible pillar or center tag. Supersedes the existing rotate-only auto-align.
   - `ResetPoseCommand` — one-shot re-seed of Pedro's localizer.
3. Command-based architecture using Ivy subsystems / commands / scheduler.
4. Two odometry options selectable at Init via FTC Dashboard: three dead wheels (current) and goBILDA Pinpoint (future).
5. Heavy pedagogical comments in the code so the team can learn the pattern.

## 3. Non-goals

- Migrating autonomous (`BlueAuto`, `BlueAutoClose`, `blueAutoTest`, `Auto`) from Road Runner to Pedro.
- Deleting or modifying `MecanumDrive.java`, `DriveCode.java`, `DriveCodeCommon.java`, `Old_*` files, RR localizer classes, or `PID_Tune*`.
- Unit tests. FTC code is hardware-coupled; verification happens on-robot via a bring-up TeleOp (§10).
- Paddle / color-sensor "holder" behavior — those hardware devices do not exist in the current `MecanumDrive` and the `holder()` method in `DriveCodeCommon` is commented out. Dropped from scope.
- Splines in `PathToPoseCommand` v1 — straight-line paths only. Splines are a follow-up.
- Sharing any drive class between RR and Pedro.

## 4. Dependencies and build changes

Add to `TeamCode/build.gradle` alongside (not replacing) the existing Road Runner dependencies:

```gradle
implementation "com.pedropathing:core:2.1.1"
implementation "com.pedropathing:ftc:2.1.1"
implementation "com.pedropathing:ivy:1.0.0"
```

Published to Maven Central; the project's existing `mavenCentral()` repository is sufficient.

**FTC SDK version.** Pedro 2.x requires a recent FTC SDK. The `FtcRobotController` module version must be verified during the implementation plan and bumped if too old.

**Android/NDK.** No changes.

## 5. Package layout

All new code lives under `org.firstinspires.ftc.teamcode.pedro`:

```
TeamCode/src/main/java/org/firstinspires/ftc/teamcode/
├── (existing RR files unchanged)
└── pedro/
    ├── PedroTeleOp.java                  # @TeleOp entry point (alliance-select + scheduler loop)
    ├── PedroBringUpTeleOp.java           # debug @TeleOp: default commands only, no assists
    ├── RobotContainer.java               # one place that wires subsystems, defaults, bindings, presets
    ├── AllianceColor.java                # enum RED / BLUE; passed to RobotContainer at runtime
    ├── subsystems/
    │   ├── Drivetrain.java               # 4 drive motors + Pedro Follower + localizer
    │   ├── Intake.java                   # intakeOne + intakeTwo (CRServos)
    │   ├── Shooter.java                  # launcherOne + launcherTwo (DcMotorEx) + PIDF
    │   ├── Pusher.java                   # pusherWheel (CRServo)
    │   └── Indicator.java                # blinkin (RevBlinkinLedDriver)
    └── commands/
        ├── TeleOpDriveCommand.java       # default on Drivetrain
        ├── IndicatorDefaultCommand.java  # default on Indicator; paints alliance color
        ├── IntakeOneRunCommand.java      # gamepad2 RB
        ├── IntakeTwoRunCommand.java      # gamepad2 LB
        ├── ClearJamCommand.java          # gamepad2 X — multi-subsystem macro
        ├── PushCommand.java              # gamepad2 Y
        ├── ShootNearCommand.java         # gamepad2 RT > 0.5
        ├── ShootFarCommand.java          # gamepad2 LT > 0.5
        ├── ShootClearCommand.java        # gamepad2 right_stick_button
        ├── ResetPoseCommand.java         # gamepad1 back
        ├── PathToPoseCommand.java        # gamepad1 y  (holds)
        ├── SnapToAprilTagCommand.java    # gamepad1 a  (holds; replaces autoAlign toggle)
        └── ApproachShotCommand.java      # gamepad1 b  (holds; replaces move-toward-tag)
```

**Hardware names** in the new subsystems match the strings currently used by `MecanumDrive`:
- Drive motors: `leftFront`, `leftBack`, `rightFront`, `rightBack`.
- Intake: `intakeOne`, `intakeTwo`.
- Launchers: `launcherOne`, `launcherTwo`.
- Pusher: `pusherwheel` (yes, lowercase — `MecanumDrive.java:253` uses that exact string).
- LED: `blinkin`.
- Odometry: `par0`, `par1`, `perp` OR `pinpoint`.
- Vision: `limelight`.

See §13 for quirks that must be preserved byte-for-byte from the current config.

## 6. Subsystems

Subsystems own hardware and guarantee mutual exclusion — the Ivy scheduler will not run two commands that require the same subsystem at once. Commands never touch hardware directly.

### 6.1 `Drivetrain`

Owns the four drive motors **and** Pedro's `Follower`. Only class that directly drives wheel power in the Pedro world.

Public API:
- `drive(double x, double y, double rot, boolean slow)` — robot-relative. Sign convention mirrors `DriveCodeCommon.drives()`: `x = -gamepad1.left_stick_y`, `y = -gamepad1.left_stick_x`, `rot = -gamepad1.right_stick_x`. `slow=true` multiplies by 0.5.
- `setPose(Pose pose)` / `getPose() : Pose`.
- `followPath(PathChain path)` / `isFollowing() : boolean` / `cancel()`.
- `periodic()` — advances `follower.update()` while a path is active; no-op otherwise.

**`Drivetrain.cancel()` contract:** stops the follower, clears the active-path reference, and calls `drive(0,0,0,false)` so motors stop in one tick. After `cancel()`, `isFollowing()` returns `false` and `followPath(...)` may be called again.

**`Drivetrain.drive(x, y, rot, slow)` contract:** sign-flipping is the caller's job. The method takes pre-flipped values in the FTC convention (`+x` = forward, `+y` = left, `+rot` = counterclockwise). `TeleOpDriveCommand` owns the stick-to-axis mapping.

Localizer selection — see §7.

### 6.2 `Intake`

Owns `intakeOne` and `intakeTwo` (CRServo). Today's bindings set each servo to `-1.0` when its bumper is held, and both to `+1.0` when `gamepad2.x` is held (the clear-jam macro).

Public API:
- `setOnePower(double power)`
- `setTwoPower(double power)`
- `stop()` — both to 0.
- `periodic()` — no-op.

### 6.3 `Shooter`

Owns `launcherOne` and `launcherTwo` (DcMotorEx, velocity-controlled). "Smart" subsystem (user's choice, Option A): knows the three preset fire modes internally. Live PIDF tuning preserved — on every tick, `periodic()` reads the `@Config` PIDF coefficients (`SHOOTER.oneP`, `SHOOTER.oneF`, `SHOOTER.twoP`, `SHOOTER.twoF`) and re-applies them to both motors. That matches the current behavior of `DriveCodeCommon.shooter()` applying coefficients every loop from `PID_Tune`/`PID_Tune2`.

Public API:
- `shootNear()` — sets velocity to `(1500, 750)` (bottom, top).
- `shootFar()` — sets velocity to `(950, 1450)`.
- `shootClear()` — sets velocity to `(10000, 10000)`. Does **not** touch the LED; that coupling lives in `ShootClearCommand` via `Indicator` (see §8).
- `reverse()` — sets velocity to `(-500, -500)` (used by `ClearJamCommand`).
- `stop()` — sets velocity to `(0, 0)`.
- `periodic()` — applies live PIDF every tick.

Preset speeds and PIDF coefficients are `public static` fields on a nested `@Config` class so FTC Dashboard can tune them live.

### 6.4 `Pusher`

Owns `pusherWheel` (CRServo, hardware name `"pusherwheel"`).

Public API:
- `setPower(double power)` — `-1.0` to push, `+1.0` for clear-jam reverse, `0.0` for idle.
- `periodic()` — no-op.

### 6.5 `Indicator`

Owns `blinkin` (RevBlinkinLedDriver). Used both for alliance display (idle) and for the shoot-clear blue flash.

Public API:
- `setPattern(RevBlinkinLedDriver.BlinkinPattern pattern)`.
- `allianceColor(AllianceColor alliance)` — helper: sets RED or BLUE pattern per alliance.
- `periodic()` — no-op.

### 6.6 Shared constraints

All five subsystems are `@Config` so their tunable constants (preset speeds, PIDF coefficients, localizer tuning) are live-editable in FTC Dashboard.

## 7. Localizer abstraction (three dead wheels + Pinpoint)

One TeleOp, two localizer options, swap via FTC Dashboard at Init:

```java
@Config
public class Drivetrain {
    public enum LocalizerType { THREE_DEAD_WHEEL, PINPOINT }
    public static LocalizerType LOCALIZER = LocalizerType.THREE_DEAD_WHEEL;

    public static class ThreeWheelParams {
        public double parTicksPerInch, perpTicksPerInch;
        public double par0_y, par1_y, perp_x;
        public DcMotorSimple.Direction par0Dir, par1Dir, perpDir;
    }
    public static class PinpointParams {
        public double podXOffsetMM, podYOffsetMM;
        public GoBildaPinpointDriver.EncoderDirection parDir, perpDir;
        public double ticksPerMM;
        public double yawScalar;
    }
}
```

Constructor switches on `LOCALIZER` to build the Pedro `Follower` with either Pedro's `ThreeWheelLocalizer` (encoder ports `par0`, `par1`, `perp`) or Pedro's `PinpointLocalizer` (I²C device `pinpoint`).

**Important:** Pedro's localizer constants do not equal Road Runner's. All values start as placeholders and must be re-derived by running Pedro's tuning routines. A teaching comment in each parameter block points to the corresponding RR class so students can see the delta.

## 8. Commands

Commands are verbs. Each declares which subsystems it **requires**, implements `initialize()` / `execute()` / `end(interrupted)` / `isFinished()`.

### 8.1 Default commands

- **`TeleOpDriveCommand`** (requires `Drivetrain`). Every tick reads `gamepad1` and calls `drivetrain.drive(-lsY, -lsX, -rsX, rb)`. Never finishes. Interrupted when any driver-assist claims `Drivetrain`.
- **`IndicatorDefaultCommand`** (requires `Indicator`). Every tick sets alliance color. Allows momentary overrides like `ShootClearCommand` to paint blue; default resumes when override ends.

No default commands on `Intake`, `Shooter`, or `Pusher` — idle is "stopped," which is exactly what a non-running subsystem already is.

### 8.2 Triggered commands — gamepad 2 (operator)

- **`IntakeOneRunCommand`** (requires `Intake`). `initialize()` → `setOnePower(-1.0)`; `end()` → `setOnePower(0)`. `.whileTrue(gamepad2.right_bumper)`.
- **`IntakeTwoRunCommand`** (requires `Intake`). Mirror of the above for `intakeTwo`. `.whileTrue(gamepad2.left_bumper)`.
- **`ClearJamCommand`** (requires `Intake`, `Shooter`, `Pusher`). `initialize()`: both intakes `+1.0`, `shooter.reverse()`, pusher `+1.0`. `end()`: all three stopped. `.whileTrue(gamepad2.x)`.
- **`PushCommand`** (requires `Pusher`). `initialize()` → `setPower(-1.0)`; `end()` → `setPower(0)`. `.whileTrue(gamepad2.y)`.
- **`ShootNearCommand`** (requires `Shooter`). `initialize()` → `shootNear()`; `end()` → `stop()`. `.whileTrue(gamepad2.right_trigger > 0.5)`.
- **`ShootFarCommand`** — same shape, calls `shootFar()`. `.whileTrue(gamepad2.left_trigger > 0.5)`.
- **`ShootClearCommand`** (requires `Shooter`, `Indicator`). `initialize()` → `shootClear()` + `indicator.setPattern(BLUE)`; `end()` → `shooter.stop()` + indicator default resumes. `.whileTrue(gamepad2.right_stick_button)`.

**Conflict resolution.** Ivy's default is that a newly-scheduled command with overlapping requirements interrupts a running one. So if the driver goes from RT → LT, `ShootNearCommand` is interrupted (`end(true)` called, which calls `stop()`), and `ShootFarCommand` starts. This is the desired behavior and is called out as a teaching note in `RobotContainer`.

### 8.3 Triggered commands — gamepad 1 (driver)

- **`ResetPoseCommand`** (requires `Drivetrain`). One-shot. `initialize()` calls `drivetrain.setPose(STARTING_POSE)`; `isFinished()` returns `true`. `.onTrue(gamepad1.back)`.
- **`PathToPoseCommand`** (requires `Drivetrain`). Constructor takes a target `Pose`. `initialize()` builds a straight-line `PathChain` and calls `drivetrain.followPath(...)`. `execute()` is a no-op. `isFinished()` returns `!drivetrain.isFollowing() || driverOverride(gamepad1)`. `end()` calls `drivetrain.cancel()`. `.whileTrue(gamepad1.y)` → `PathToPoseCommand(SCORING_POSE)`.
- **`SnapToAprilTagCommand`** (requires `Drivetrain`; uses `LimelightVision`). Constructor takes `TargetType` (`NEAREST_PILLAR` or `CENTER_TAG`) and the current `AllianceColor`. `initialize()` queries `LimelightVision.getPillarTarget(isRedAlliance)` or the center-tag equivalent. If not found, sets `abort = true` and `isFinished()` returns true immediately. Otherwise computes a standoff pose (§8.5) and starts a Pedro path to it. `.whileTrue(gamepad1.a)` → `SnapToAprilTagCommand(NEAREST_PILLAR, alliance)`.
- **`ApproachShotCommand`** (requires `Drivetrain`; uses `LimelightVision`). Replaces today's `gamepad1.b` creep-forward behavior with a Pedro path. `initialize()` reads the pillar target and computes a path from current pose to a pose at `IDEAL_SHOOT_DISTANCE` inches standoff from the tag, heading aligned to the tag. `.whileTrue(gamepad1.b)`.

Both `SnapToAprilTagCommand` and `ApproachShotCommand` use the same `driverOverride` cancel-on-stick as `PathToPoseCommand`.

### 8.4 Cancellation semantics

Because all three Drivetrain-claiming driver-assists require `Drivetrain`, the default `TeleOpDriveCommand` cannot run while one of them is active. Stick input is invisible to the scheduler. Each driver-assist therefore explicitly checks `driverOverride(gamepad1)` (any drive stick past 0.1 deadband) and returns `true` from `isFinished()` when the driver overrides — at which point the default command resumes. Heavily commented in code.

### 8.5 Limelight → Pedro coordinate contract (important integration point)

Pedro's `Pose` and Limelight's vision data live in different frames. The contract for both `SnapToAprilTagCommand` and `ApproachShotCommand`:

1. **Source of truth for bot pose: Pedro's localizer.** We do NOT consume `limelight.getBotpose()` for driving — that would mix two localizers. Limelight is used only to produce a *relative* tag vector.
2. **Target acquisition:** call `limelight.getPillarTarget(isRedAlliance)` (returns a `VisionTarget` with distance and angle from the camera's optical axis). The existing alliance-aware selection logic in `LimelightVision` is preserved and called with the alliance captured during INIT.
3. **Compute standoff pose:** in Pedro's frame, current pose is `drivetrain.getPose()`. Target pose = current pose rotated by `pillarTag.getAngleToTarget()` and translated forward by `(pillarTag.getDistance() - STANDOFF_INCHES)`, heading = current heading + `pillarTag.getAngleToTarget()`. This keeps everything in Pedro's frame — no cross-frame transform needed.
4. **If the tag is not visible at `initialize()`**, the command aborts immediately; there's no search-spin fallback (the current code's search behavior is dropped — call that out explicitly).
5. Standoff constants (`STANDOFF_INCHES`, `IDEAL_SHOOT_DISTANCE`) live in `RobotContainer` as `public static` `@Config` fields. `IDEAL_SHOOT_DISTANCE` starts at 97.0 to match the current value in `DriveCodeCommon`.

### 8.6 Scheduler lifecycle at OpMode start / end

- In `PedroTeleOp.runOpMode()`, the first action after `waitForStart()` is `Scheduler.getInstance().reset()` (or the equivalent Ivy call — exact API TBD in implementation plan). This clears any residual subsystems, commands, or triggers registered by a previous OpMode run, which matters because FTC runs sequential OpModes in the same JVM.
- `RobotContainer` is constructed *after* the reset, so its subsystem registration happens on a clean scheduler.
- At end of the loop, `scheduler.cancelAll()` interrupts any running commands so `end(true)` is called and hardware comes to a clean stop.

## 9. OpMode skeleton and button bindings

### 9.1 `RobotContainer` API

Constructor: `RobotContainer(HardwareMap hw, Gamepad g1, Gamepad g2, AllianceColor alliance, Telemetry t)`.

Public methods:
- `void pollTelemetry(Telemetry t)` — each tick, adds pose, vision status, alliance, and any in-flight command name to telemetry.
- `Drivetrain drivetrain()` / `LimelightVision vision()` — test hooks for the bring-up TeleOp.

Preset constants at the top:
```java
public static final Pose STARTING_POSE = new Pose(0, 0, 0);       // TODO: tune on field
public static final Pose SCORING_POSE  = new Pose(24, 0, 0);      // TODO: tune on field
public static final double APRILTAG_STANDOFF_INCHES = 24.0;       // TODO: tune on field
public static final double DRIVER_OVERRIDE_DEADBAND = 0.1;
```

### 9.2 `PedroTeleOp.runOpMode()`

```java
@TeleOp(name = "Pedro TeleOp", group = "pedro")
public class PedroTeleOp extends LinearOpMode {
    @Override public void runOpMode() {
        AllianceColor alliance = AllianceColor.BLUE;
        while (!isStarted() && !isStopRequested()) {
            if (gamepad1.dpad_left)  alliance = AllianceColor.BLUE;
            if (gamepad1.dpad_right) alliance = AllianceColor.RED;
            telemetry.addData("Alliance", alliance);
            telemetry.addLine("Press LEFT for BLUE, RIGHT for RED");
            telemetry.update();
        }
        waitForStart();
        if (isStopRequested()) return;

        Scheduler scheduler = Scheduler.getInstance();
        scheduler.reset();
        RobotContainer robot = new RobotContainer(hardwareMap, gamepad1, gamepad2, alliance, telemetry);

        while (opModeIsActive()) {
            scheduler.run();
            robot.pollTelemetry(telemetry);
            telemetry.update();
        }
        scheduler.cancelAll();
    }
}
```

### 9.3 Gamepad bindings

| Trigger | Command | Binding |
|---|---|---|
| `gamepad1` sticks + RB | `TeleOpDriveCommand` | default on `Drivetrain` |
| `gamepad1.a` held | `SnapToAprilTagCommand(NEAREST_PILLAR, alliance)` | `.whileTrue` |
| `gamepad1.b` held | `ApproachShotCommand` | `.whileTrue` |
| `gamepad1.y` held | `PathToPoseCommand(SCORING_POSE)` | `.whileTrue` |
| `gamepad1.back` pressed | `ResetPoseCommand` | `.onTrue` |
| `gamepad1.dpad_left/right` | *(INIT only — alliance select)* | — |
| `gamepad2.right_bumper` held | `IntakeOneRunCommand` | `.whileTrue` |
| `gamepad2.left_bumper` held | `IntakeTwoRunCommand` | `.whileTrue` |
| `gamepad2.x` held | `ClearJamCommand` | `.whileTrue` |
| `gamepad2.y` held | `PushCommand` | `.whileTrue` |
| `gamepad2.right_trigger > 0.5` | `ShootNearCommand` | `.whileTrue` |
| `gamepad2.left_trigger > 0.5` | `ShootFarCommand` | `.whileTrue` |
| `gamepad2.right_stick_button` held | `ShootClearCommand` | `.whileTrue` |

Rebinding is done in `RobotContainer` only.

**Intentional behavior changes from current `DriveCode`:**
- `gamepad1.a` changes from **toggle** to **hold**. Hold-to-run is idiomatic in command-based and removes the need for the `autoAlignActive` / `prevButtonA` / 5000 ms timeout state machine. If toggle is required, we can add it later.
- `gamepad1.b` still holds-to-run, but now does a proper path to standoff instead of a proportional-gain creep. Feels more precise.
- The "search spin" fallback in `autoAlign` when no tag is visible is dropped — `SnapToAprilTagCommand` aborts cleanly instead of spinning the robot.

## 10. Verification plan

On-robot only. Each step is a Go/No-Go gate.

1. **Compile.** `./gradlew :TeamCode:assembleDebug` passes with Pedro + Ivy dependencies added.
2. **`PedroBringUpTeleOp`.** Debug-only `@TeleOp` that registers `RobotContainer` but **binds only the default commands** — no driver-assists, no intake/shooter buttons. Confirms Pedro + Ivy resolve on-device, subsystems initialize, and manual drive feels identical to today's `DriveCode` drive portion. *If drive feel matches, hardware layer is correct.*
3. **Localizer check.** In `PedroBringUpTeleOp`, stream `drivetrain.getPose()` to FTC Dashboard. Drive forward 24", strafe 24", turn 360°, return to start. Pose should return near origin. If not, run Pedro's tuning routines and fill in `ThreeWheelParams`.
4. **Intake / shooter / pusher / indicator.** Enable those bindings. Verify each button mirrors current `DriveCode` behavior: both intake directions, all three fire modes, clear-jam macro, pusher, blue-flash-on-clear.
5. **Reset-pose check.** Bind Back → `ResetPoseCommand(Pose(0,0,0))`. Drive around. Press Back. Pose snaps to origin.
6. **Path-to-pose check.** `SCORING_POSE = (24, 0, 0)` for the first test. Hold Y. Robot drives 24" forward and stops. Bumping the stick cancels immediately.
7. **Snap-to-AprilTag check.** Place a pillar tag in view. Hold A with `APRILTAG_STANDOFF_INCHES = 36` so misalignment is obvious before the bot gets close. Tune down once stable.
8. **Approach-shot check.** Place a pillar tag in view. Hold B. Robot paths to `IDEAL_SHOOT_DISTANCE = 97` from the tag. Verify with tape-measured distance.

## 11. Teaching-comment style

- **File-level doc-comment** on every new class: one paragraph explaining the file's role in command-based. Reading headers in package order gives the full architecture.
- **Inline `// TEACHING NOTE:` comments** at non-obvious decision points. Planned notes:
  - `Drivetrain.followPath(...)`: why `follower.update()` is in `periodic()` and not in the command's `execute()`.
  - `PathToPoseCommand.isFinished()`: why `driverOverride` is needed even though the driver is moving the stick.
  - `RobotContainer`: difference between `.whileTrue(...)` and `.onTrue(...)`.
  - `Drivetrain` localizer switch: what a strategy pattern is and when it stops scaling.
  - `PedroTeleOp.runOpMode()`: what `scheduler.run()` actually does each tick, and why `scheduler.reset()` matters for FTC's sequential-OpMode JVM lifecycle.
  - `Shooter.periodic()`: why PIDF coefficients are re-applied every tick (live tuning), mirroring the current `shooter()` behavior.
  - `SnapToAprilTagCommand`: the Limelight → Pedro coordinate contract.
- **No narration** of self-evident code.
- Teaching notes use the `TEACHING NOTE:` prefix for grep/strip.

## 12. Assumptions and open questions

- Only one OpMode runs at a time → Pedro + RR can coexist in the project. ✓
- FTC SDK version compatible with Pedro 2.1.1 — verified during implementation plan.
- All preset values (poses, standoff, PIDF, localizer constants) start as placeholders and are tuned on the field. The design enumerates the interfaces, not the tuned values.
- Ivy's `Scheduler` exposes a way to reset per-OpMode (singleton reset or new instance). Exact API confirmed in implementation plan; if reset isn't available, we create a new `RobotContainer` per OpMode run and rely on subsystem re-registration idempotency.

## 13. Quirks preserved from current `DriveCode`

Behaviors that are surprising but intentionally mirrored so the new TeleOp is a true port:

- **Crossed motor hardwareMap.** `MecanumDrive.java:238-241` assigns Java field `leftFront` from hardware name `"rightFront"`, and similarly swaps the other three. The new `Drivetrain` will reproduce this exact mapping so the existing Robot Controller configuration file needs no changes. A comment documents why.
- **Blinkin is always BLUE during shoot-clear**, regardless of alliance. Today's code hard-codes `BlinkinPattern.BLUE`; we keep that.
- **`pusherwheel`** is lowercase in the hardware config — preserved verbatim.
- **Intake reverse on `gamepad2.x`** also reverses the launchers and pushes the pusher — it's a "clear everything" macro. New `ClearJamCommand` does all three in one command.

## 14. Out-of-scope follow-ups

- Migrate autonomous off Road Runner onto Pedro (separate spec).
- Splines in `PathToPoseCommand` for curved approaches.
- Toggle-style auto-align (press-once-to-engage, press-again-to-disengage) if drivers prefer it over hold-to-run.
- Search-spin fallback if `SnapToAprilTagCommand` doesn't see a tag.
- "Shoot sequence" macros chaining path + shooter + pusher.
- Eventually removing Road Runner once autonomous is migrated.

## 15. Success criteria

1. `PedroTeleOp` reproduces every behavior in today's `DriveCode` (drive, both intakes, intake-reverse, all three fire modes, pusher, clear-jam macro, live PIDF, alliance select, blinkin behaviors).
2. The three driver-assist commands work: path to preset pose, snap to AprilTag (replacing `autoAlign`), approach-to-shot distance (replacing the creep-forward).
3. Swapping `Drivetrain.LOCALIZER` between `THREE_DEAD_WHEEL` and `PINPOINT` works without code edits.
4. A team member unfamiliar with command-based can read the `pedro/` package headers in order and explain the architecture.
5. `DriveCode`, `MecanumDrive`, `BlueAuto`, and all `Old_*` files still build and run unchanged.
