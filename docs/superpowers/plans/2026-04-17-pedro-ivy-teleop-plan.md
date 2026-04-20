# Pedro Pathing + Ivy TeleOp Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a new command-based `@TeleOp` on Pedro Pathing + Ivy that ports every behavior in today's `DriveCode` and adds three driver-assist actions (path-to-pose, snap-to-AprilTag, reset-pose), living side-by-side with the existing Road Runner code.

**Architecture:** A new `org.firstinspires.ftc.teamcode.pedro` package. Subsystems are plain classes (Ivy's `requirements()` returns `Set<Object>` — no base class needed). Commands implement the `com.pedropathing.ivy.Command` interface. A tiny `GamepadTriggers` helper polls buttons each tick and schedules/cancels commands on edges (Ivy has no built-in `whileTrue` binding). `Drivetrain` owns Pedro's `Follower` and a `LocalizerType` strategy flag that picks between three-wheel and Pinpoint localizers. Existing Road Runner code, `DriveCode`, `MecanumDrive`, and `BlueAuto` are untouched.

**Tech Stack:** FTC SDK (Android, Java 8), Pedro Pathing 2.1.1 (`core` + `ftc`), Ivy 1.0.0, FTC Dashboard (`@Config`), Limelight3A vision (existing `LimelightVision` class).

**References:**
- Spec: `docs/superpowers/specs/2026-04-17-pedro-ivy-teleop-design.md` — read §6 (subsystems), §7 (localizer), §8 (commands), §8.5 (Limelight↔Pedro contract), §13 (quirks) before starting.
- Ivy docs: https://pedropathing.com/docs/ivy (and sub-pages `/what-are-commands`, `/class-api`, `/command-builder`, `/requirements-and-priorities`, `/creating-opmodes`, `/pedro-commands`).
- Pedro docs: https://pedropathing.com/docs/pathing (and `/installation`, `/tuning`, `/reference/path-builder`, `/custom/localizer`).
- Javadoc: https://javadoc.io/doc/com.pedropathing

**Confirmed Ivy API (from docs as of 2026-04-17):**
- `com.pedropathing.ivy.Command` interface — implement `void start()`, `void execute()`, `boolean done()`, `void end(EndCondition endCondition)`, `Set<Object> requirements()`, `int priority()`, `InterruptedBehavior interruptedBehavior()`, `BlockedBehavior blockedBehavior()`, `ConflictBehavior conflictBehavior()`.
- `com.pedropathing.ivy.Scheduler` — static: `Scheduler.reset()`, `Scheduler.schedule(Command)`, `Scheduler.execute()` *(call once per loop)*, `Scheduler.cancel(Command)`, `Scheduler.isRunning(Command)`, `Scheduler.isScheduled(Command)`.
- Command builder: `Command.build().setExecute(Runnable).requiring(Object...).setDone(BooleanSupplier)`.
- Pedro commands: `com.pedropathing.ivy.pedro.PedroCommands.follow(follower, pathChain)`, `PedroCommands.hold(follower)`, `PedroCommands.hold(follower, pose)`.
- Enums: `InterruptedBehavior` (`END`, `SUSPEND`), `BlockedBehavior` (`CANCEL`, `QUEUE`), `ConflictBehavior` (`CANCEL`, `QUEUE`, `OVERRIDE`).

**Pedro API details the implementer must verify before Task 8:** Follower constructor, PathChain/PathBuilder, Pose class, ThreeWheelLocalizer and PinpointLocalizer configuration. Check https://pedropathing.com/docs/pathing/installation and the javadoc, then copy real signatures into the code. This plan provides skeletons with `TODO(API):` markers where exact Pedro signatures must be filled in.

**Verification approach:** Per spec §10, **no unit tests** — FTC code is hardware-coupled. Every task ends with `./gradlew :TeamCode:assembleDebug` as the compile gate, then a commit. On-robot verification (bring-up TeleOp, localizer pose check, driver-assist tests) happens after the whole plan is implemented. Verification checklist is in Task 18.

**Commit discipline:** One commit per task. Messages follow the existing repo style (see `git log --oneline -10`): short imperative subject, optional body. Commits always include the `Co-Authored-By:` trailer established in prior commits.

**`TODO(API)` hygiene:** No committed file may contain the substring `TODO(API)`. Before every `git add`, run:
```bash
! grep -rn 'TODO(API)' TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/
```
If that prints any lines, resolve them against the Pedro/Ivy javadoc before committing.

**Pre-chunk-1 API verification** (do once, after Task 1, before Task 3):

The plan's "Confirmed Ivy API" list above was assembled from the public docs; actual jar contents are the source of truth. Before writing any Ivy-dependent code, inspect the jar:

```bash
./gradlew :TeamCode:dependencies --configuration releaseRuntimeClasspath | grep pedropathing
IVY_JAR=$(find ~/.gradle/caches -name 'ivy-1.0.0.jar' 2>/dev/null | head -1)
unzip -l "$IVY_JAR" | grep -E 'Command\.class|Scheduler\.class|EndCondition\.class|pedro/PedroCommands\.class'
```

Confirm the exact package paths for `Command`, `Scheduler`, `EndCondition`, `InterruptedBehavior`, `BlockedBehavior`, `ConflictBehavior`, and `PedroCommands`. If they differ from `com.pedropathing.ivy.*` / `com.pedropathing.ivy.pedro.*`, update the imports in all files this plan creates to match.

Do the same for Pedro core (`core-2.1.1.jar`, `ftc-2.1.1.jar`) to confirm `Follower`, `Pose`, `PathChain`, `PathBuilder`, `ThreeWheelLocalizer`, and `PinpointLocalizer` packages before Task 9.

---

## File Structure

Files created by this plan (all under `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/` unless noted):

```
pedro/
├── AllianceColor.java
├── util/
│   └── GamepadTriggers.java          # polls buttons, schedules/cancels on edges
├── subsystems/
│   ├── Indicator.java
│   ├── Pusher.java
│   ├── Intake.java
│   ├── Shooter.java
│   └── Drivetrain.java               # biggest file; owns Pedro Follower + localizer abstraction
├── commands/
│   ├── TeleOpDriveCommand.java       # default on Drivetrain
│   ├── IndicatorDefaultCommand.java  # default on Indicator
│   ├── IntakeOneRunCommand.java
│   ├── IntakeTwoRunCommand.java
│   ├── ClearJamCommand.java
│   ├── PushCommand.java
│   ├── ShootNearCommand.java
│   ├── ShootFarCommand.java
│   ├── ShootClearCommand.java
│   ├── ResetPoseCommand.java
│   ├── PathToPoseCommand.java
│   ├── SnapToAprilTagCommand.java
│   └── ApproachShotCommand.java
├── RobotContainer.java               # wires it all together; one place for bindings/presets
├── PedroBringUpTeleOp.java           # @TeleOp group "pedro-debug"
└── PedroTeleOp.java                  # @TeleOp group "pedro"
```

Files modified:
- `TeamCode/build.gradle` — add three Pedro/Ivy `implementation` lines.

Nothing deleted. Nothing touched outside `pedro/` and `TeamCode/build.gradle`.

---

## Chunk 1: Dependencies and Scaffolding

### Task 1: Add Pedro and Ivy Gradle dependencies

**Files:**
- Modify: `TeamCode/build.gradle`

- [ ] **Step 1: Open `TeamCode/build.gradle` and locate the `dependencies { ... }` block.**

- [ ] **Step 2: Add the three new implementation lines at the end of the block, keeping existing Road Runner lines untouched.**

The block should end up looking like:

```gradle
dependencies {
    implementation project(':FtcRobotController')

    implementation "com.acmerobotics.roadrunner:ftc:0.1.25"
    implementation "com.acmerobotics.roadrunner:core:1.0.1"
    implementation "com.acmerobotics.roadrunner:actions:1.0.1"
    implementation "com.acmerobotics.dashboard:dashboard:0.5.1"

    implementation "com.pedropathing:core:2.1.1"
    implementation "com.pedropathing:ftc:2.1.1"
    implementation "com.pedropathing:ivy:1.0.0"
}
```

- [ ] **Step 3: Sync Gradle to pull new dependencies.**

Run: `./gradlew :TeamCode:dependencies --configuration releaseRuntimeClasspath | grep pedropathing`
Expected: three lines mentioning `com.pedropathing:core:2.1.1`, `com.pedropathing:ftc:2.1.1`, `com.pedropathing:ivy:1.0.0`.

- [ ] **Step 4: Compile.**

Run: `./gradlew :TeamCode:assembleDebug`
Expected: `BUILD SUCCESSFUL`. Deprecation warnings about Java source/target 8 are fine.

- [ ] **Step 5: Commit.**

```bash
git add TeamCode/build.gradle
git commit -m "$(cat <<'EOF'
build: add Pedro Pathing + Ivy dependencies

Pedro Pathing 2.1.1 (core + ftc) and Ivy 1.0.0 added alongside
Road Runner. Both are pulled from Maven Central; no new repo
declaration needed.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Create package directory and `AllianceColor` enum

**Files:**
- Create: `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/AllianceColor.java`

- [ ] **Step 1: Create the `pedro/` directory.**

```bash
mkdir -p TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/{subsystems,commands,util}
```

- [ ] **Step 2: Create `AllianceColor.java`.**

```java
package org.firstinspires.ftc.teamcode.pedro;

/**
 * Which alliance the robot is playing for. Selected during INIT (see PedroTeleOp)
 * and passed to RobotContainer so commands that care about alliance (e.g.
 * SnapToAprilTagCommand, IndicatorDefaultCommand) can branch on it.
 *
 * TEACHING NOTE: We pass this as an explicit constructor argument instead of a
 * global static so that future OpModes can run with a hard-coded alliance for
 * testing without touching the global state.
 */
public enum AllianceColor {
    RED,
    BLUE
}
```

- [ ] **Step 3: Compile.**

Run: `./gradlew :TeamCode:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit.**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/AllianceColor.java
git commit -m "pedro: scaffold pedro package with AllianceColor enum

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: Create `GamepadTriggers` utility

Ivy ships no `whileTrue` / `onTrue` binding helper (confirmed from docs). We build a tiny polling helper that turns gamepad state into command scheduling. Every loop, `poll()` is called; it compares current vs previous state and schedules/cancels commands on edges.

**Files:**
- Create: `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/util/GamepadTriggers.java`

- [ ] **Step 1: Create `GamepadTriggers.java`.**

```java
package org.firstinspires.ftc.teamcode.pedro.util;

import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Polling-based replacement for WPILib's Trigger / .whileTrue() / .onTrue().
 *
 * Ivy doesn't ship a button-binding utility, so we roll our own. Register
 * bindings once during RobotContainer construction; then PedroTeleOp calls
 * {@link #poll()} exactly once per loop iteration (right before
 * {@link Scheduler#execute()}). Each binding compares this tick's boolean
 * against the previous tick's and reacts to the edge.
 *
 * TEACHING NOTE: This file is small on purpose. It is the one class that
 * translates raw gamepad state into Ivy command scheduling — everything
 * upstream of it should never read a gamepad directly, and everything
 * downstream of it should never call Scheduler.schedule() directly.
 * That one-way data flow is what makes the rebinding table in
 * RobotContainer readable.
 */
public final class GamepadTriggers {

    /** Lazily supplied to allow for parameterized commands (e.g. PathToPoseCommand with a dynamic target). */
    public interface CommandFactory extends Supplier<Command> { }

    private final List<Binding> bindings = new ArrayList<>();

    /**
     * Run the command while the condition is true. Schedules on false→true
     * edge, cancels on true→false edge. If the command completes on its own
     * (e.g. a one-shot that returns done()==true), the binding does not
     * re-schedule it until the condition goes false and then true again.
     */
    public void whileTrue(BooleanSupplier condition, CommandFactory factory) {
        bindings.add(new WhileBinding(condition, factory));
    }

    /**
     * Schedule the command once on a false→true edge. Never cancels.
     */
    public void onTrue(BooleanSupplier condition, CommandFactory factory) {
        bindings.add(new OnTrueBinding(condition, factory));
    }

    /** Call once per OpMode loop, before Scheduler.execute(). */
    public void poll() {
        for (Binding b : bindings) b.poll();
    }

    /** Implementation details below. */

    private static abstract class Binding {
        final BooleanSupplier cond;
        final CommandFactory factory;
        boolean prev = false;
        Binding(BooleanSupplier cond, CommandFactory factory) {
            this.cond = cond;
            this.factory = factory;
        }
        abstract void poll();
    }

    private static final class WhileBinding extends Binding {
        Command active;
        WhileBinding(BooleanSupplier cond, CommandFactory factory) { super(cond, factory); }
        @Override void poll() {
            boolean now = cond.getAsBoolean();
            if (now && !prev) {
                active = factory.get();
                Scheduler.schedule(active);
            } else if (!now && prev && active != null) {
                Scheduler.cancel(active);
                active = null;
            }
            prev = now;
        }
    }

    private static final class OnTrueBinding extends Binding {
        OnTrueBinding(BooleanSupplier cond, CommandFactory factory) { super(cond, factory); }
        @Override void poll() {
            boolean now = cond.getAsBoolean();
            if (now && !prev) Scheduler.schedule(factory.get());
            prev = now;
        }
    }
}
```

- [ ] **Step 2: Compile.**

Run: `./gradlew :TeamCode:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

If compilation fails because of the `com.pedropathing.ivy.*` imports, the Ivy package names may differ from what the docs implied. Use the IDE's auto-import or check `./gradlew :TeamCode:dependencies` to inspect the jar's contents. Update the imports to match what's actually published; the API method names (`schedule`, `cancel`, etc.) are the source of truth.

- [ ] **Step 3: Commit.**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/util/GamepadTriggers.java
git commit -m "pedro: add GamepadTriggers polling helper

Ivy has no built-in .whileTrue/.onTrue binding, so roll a tiny helper
that schedules/cancels commands on button edges. Used by RobotContainer
to wire gamepad → commands in a single readable table.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Chunk 2: Simple Subsystems

Tasks 4–7 build the non-drivetrain subsystems. Each is a plain class with `hardwareMap` injection in the constructor and a small public API. No base class; Ivy's `requirements()` just returns `Set<Object>` where the objects are our subsystem instances.

Hardware names come from `MecanumDrive.java` (spec §5 and §13) — reuse the existing Robot Controller configuration.

### Task 4: `Indicator` subsystem (blinkin LED)

**Files:**
- Create: `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/subsystems/Indicator.java`

- [ ] **Step 1: Create `Indicator.java`.**

```java
package org.firstinspires.ftc.teamcode.pedro.subsystems;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.hardware.rev.RevBlinkinLedDriver;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.pedro.AllianceColor;

/**
 * LED status indicator. Wraps the RevBlinkinLedDriver so commands never
 * poke the hardware directly.
 *
 * Default pattern shows the selected alliance color. Momentary overrides
 * (e.g. ShootClearCommand flashing BLUE) are handled by the commands
 * themselves interrupting IndicatorDefaultCommand.
 *
 * TEACHING NOTE: This is a subsystem by our own convention — Ivy
 * requirements() accepts any Object. We pass an Indicator instance as
 * a requirement to mark "this command owns the LED for as long as it
 * runs." The scheduler does the mutual-exclusion work for us.
 */
@Config
public class Indicator {

    public static RevBlinkinLedDriver.BlinkinPattern RED_PATTERN =
            RevBlinkinLedDriver.BlinkinPattern.RED;
    public static RevBlinkinLedDriver.BlinkinPattern BLUE_PATTERN =
            RevBlinkinLedDriver.BlinkinPattern.BLUE;

    private final RevBlinkinLedDriver blinkin;

    public Indicator(HardwareMap hw) {
        this.blinkin = hw.get(RevBlinkinLedDriver.class, "blinkin");
    }

    public void setPattern(RevBlinkinLedDriver.BlinkinPattern pattern) {
        blinkin.setPattern(pattern);
    }

    public void paintAlliance(AllianceColor alliance) {
        setPattern(alliance == AllianceColor.RED ? RED_PATTERN : BLUE_PATTERN);
    }
}
```

- [ ] **Step 2: Compile.**

Run: `./gradlew :TeamCode:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit.**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/subsystems/Indicator.java
git commit -m "pedro: add Indicator subsystem (blinkin LED)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 5: `Pusher` subsystem (pusher CRServo)

**Files:**
- Create: `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/subsystems/Pusher.java`

- [ ] **Step 1: Create `Pusher.java`.**

```java
package org.firstinspires.ftc.teamcode.pedro.subsystems;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * The pusher wheel that feeds balls into the launcher.
 *
 * Hardware name is "pusherwheel" (all lowercase) per the existing
 * MecanumDrive config — see spec §13 quirks.
 *
 * TEACHING NOTE: setPower takes positive and negative values. In today's
 * code, negative is "push toward launcher" and positive is the reverse
 * direction used by the ClearJamCommand. We keep that convention.
 */
public class Pusher {

    private final CRServo wheel;

    public Pusher(HardwareMap hw) {
        this.wheel = hw.get(CRServo.class, "pusherwheel");
    }

    public void setPower(double power) { wheel.setPower(power); }
    public void stop() { wheel.setPower(0.0); }
}
```

- [ ] **Step 2: Compile.**

Run: `./gradlew :TeamCode:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit.**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/subsystems/Pusher.java
git commit -m "pedro: add Pusher subsystem

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 6: `Intake` subsystem (dual CRServos)

**Files:**
- Create: `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/subsystems/Intake.java`

- [ ] **Step 1: Create `Intake.java`.**

```java
package org.firstinspires.ftc.teamcode.pedro.subsystems;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Two intake CRServos. intakeOne is the outer roller; intakeTwo is the
 * inner. Each is controlled by its own bumper today (gp2.RB / gp2.LB),
 * and both are reversed together as part of the clear-jam macro (gp2.X).
 *
 * TEACHING NOTE: We expose two independent setters plus a "stop" helper
 * rather than a combined "run" method, because today's driver controls
 * treat them as independent subsystems that just happen to share a
 * class. If that changes, it changes here.
 */
public class Intake {

    private final CRServo one, two;

    public Intake(HardwareMap hw) {
        this.one = hw.get(CRServo.class, "intakeOne");
        this.two = hw.get(CRServo.class, "intakeTwo");
    }

    public void setOnePower(double power) { one.setPower(power); }
    public void setTwoPower(double power) { two.setPower(power); }

    public void stopOne() { one.setPower(0.0); }
    public void stopTwo() { two.setPower(0.0); }
    public void stopAll() { stopOne(); stopTwo(); }
}
```

- [ ] **Step 2: Compile.**

Run: `./gradlew :TeamCode:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit.**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/subsystems/Intake.java
git commit -m "pedro: add Intake subsystem (dual CRServos)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 7: `Shooter` subsystem (dual launcher + live PIDF)

The "smart" shooter (spec §6.3). Exposes `shootNear()`, `shootFar()`, `shootClear()`, `reverse()`, `stop()`. PIDF coefficients live in an `@Config` nested class and are re-applied every tick via `applyPidfIfChanged()` (called from the commands or from a default command — we'll wire the application later).

Preset velocities mirror `DriveCodeCommon.shooter()` exactly (see `DriveCodeCommon.java:92–103, 116–117`).

**Files:**
- Create: `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/subsystems/Shooter.java`

- [ ] **Step 1: Create `Shooter.java`.**

```java
package org.firstinspires.ftc.teamcode.pedro.subsystems;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

/**
 * Two-wheel launcher ("bottom" = launcherOne, "top" = launcherTwo)
 * running under velocity control with live PIDF tuning from FTC
 * Dashboard. Mirrors the behavior currently in
 * DriveCodeCommon.shooter() and the PID_Tune / PID_Tune2 dashboard
 * classes.
 *
 * Public API intentionally hides the specific RPM numbers behind
 * shootNear / shootFar / shootClear so commands read like button labels.
 *
 * TEACHING NOTE (preset speeds): These are the three fire modes today:
 *   near:  bottom 1500, top 750   (close pillar shot)
 *   far:   bottom 950,  top 1450  (long pillar shot)
 *   clear: bottom 10000, top 10000 (desperation shot / clear-out)
 * They can be live-tuned from the dashboard via the SHOOTER_PARAMS
 * nested @Config class without redeploying.
 *
 * TEACHING NOTE (live PIDF): applyPidfIfChanged() writes the dashboard
 * coefficients to the motors. Today's DriveCodeCommon.shooter() does
 * this every loop unconditionally. We only write when coefficients
 * change, which is cheaper and doesn't cost anything in behavior.
 */
@Config
public class Shooter {

    /** Tunable fire-mode speeds. Use these with ShootNear/Far/Clear commands. */
    public static class ShooterParams {
        public double nearBottom = 1500, nearTop = 750;
        public double farBottom  = 950,  farTop  = 1450;
        public double clearBottom = 10000, clearTop = 10000;
        public double reverseBottom = -500, reverseTop = -500;
    }
    public static ShooterParams SHOOTER_PARAMS = new ShooterParams();

    /** Tunable PIDF coefficients for each motor (live-editable). */
    public static class PidfParams {
        public double oneP = 0, oneI = 0, oneD = 0, oneF = 0;
        public double twoP = 0, twoI = 0, twoD = 0, twoF = 0;
    }
    public static PidfParams PIDF = new PidfParams();

    private final DcMotorEx launcherOne, launcherTwo;
    private PIDFCoefficients lastOne, lastTwo;

    public Shooter(HardwareMap hw) {
        this.launcherOne = hw.get(DcMotorEx.class, "launcherOne");
        this.launcherTwo = hw.get(DcMotorEx.class, "launcherTwo");
        // Current MecanumDrive reverses launcherTwo; keep parity.
        launcherTwo.setDirection(DcMotorEx.Direction.REVERSE);
        applyPidfIfChanged();
    }

    /** Call every tick (e.g. from a shooter-related command's execute()). */
    public void applyPidfIfChanged() {
        PIDFCoefficients one = new PIDFCoefficients(PIDF.oneP, PIDF.oneI, PIDF.oneD, PIDF.oneF);
        PIDFCoefficients two = new PIDFCoefficients(PIDF.twoP, PIDF.twoI, PIDF.twoD, PIDF.twoF);
        if (!equals(one, lastOne)) {
            launcherOne.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, one);
            lastOne = one;
        }
        if (!equals(two, lastTwo)) {
            launcherTwo.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, two);
            lastTwo = two;
        }
    }

    public void shootNear()  { setVelocities(SHOOTER_PARAMS.nearBottom,  SHOOTER_PARAMS.nearTop); }
    public void shootFar()   { setVelocities(SHOOTER_PARAMS.farBottom,   SHOOTER_PARAMS.farTop); }
    public void shootClear() { setVelocities(SHOOTER_PARAMS.clearBottom, SHOOTER_PARAMS.clearTop); }
    public void reverse()    { setVelocities(SHOOTER_PARAMS.reverseBottom, SHOOTER_PARAMS.reverseTop); }
    public void stop()       { setVelocities(0.0, 0.0); }

    private void setVelocities(double bottom, double top) {
        launcherOne.setVelocity(bottom);
        launcherTwo.setVelocity(top);
    }

    private static boolean equals(PIDFCoefficients a, PIDFCoefficients b) {
        if (a == null || b == null) return false;
        return a.p == b.p && a.i == b.i && a.d == b.d && a.f == b.f;
    }
}
```

- [ ] **Step 2: Compile.**

Run: `./gradlew :TeamCode:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit.**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/subsystems/Shooter.java
git commit -m "pedro: add Shooter subsystem with three fire modes and live PIDF

Mirrors DriveCodeCommon.shooter(): near=1500/750, far=950/1450,
clear=10000/10000, reverse=-500/-500. PIDF coefficients tunable
via FTC Dashboard @Config; applied when they change.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Chunk 3: Drivetrain + Localizer Abstraction

This is the plan's biggest class. Split into two tasks: a compile-first scaffold (Task 8) that wires the four drive motors manually without any Pedro integration, and a Pedro-integration pass (Task 9) that adds the `Follower` and the `LocalizerType` switch.

### Task 8: `Drivetrain` scaffold (manual drive, no Pedro yet)

Drive-motor hardwareMap mapping mirrors the existing `MecanumDrive.java:238–241` exactly — Java field `leftFront` loads from hardware name `"rightFront"`, and the other three are swapped analogously (spec §13). Do not "fix" the apparent swap; the physical wiring depends on it.

**Files:**
- Create: `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/subsystems/Drivetrain.java`

- [ ] **Step 1: Create the scaffold `Drivetrain.java` (no Follower yet).**

```java
package org.firstinspires.ftc.teamcode.pedro.subsystems;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * The mecanum drivetrain. Owns the four drive motors AND (after Task 9)
 * Pedro's Follower + localizer. Only class in the Pedro package that
 * calls setPower(...) on a drive motor.
 *
 * Hardware-name crossover (spec §13): the existing Robot Controller
 * config is wired so the Java field "leftFront" maps to the hardware
 * name "rightFront" (and analogously for the other three). We keep
 * that exact mapping so the config file doesn't need to change. See
 * MecanumDrive.java:238-241 for the reference.
 *
 * TEACHING NOTE: drive(x, y, rot, slow) expects pre-signed values in
 * FTC convention (+x forward, +y left, +rot counterclockwise).
 * TeleOpDriveCommand owns the stick-to-axis sign flipping so the
 * intent lives next to the gamepad read.
 */
@Config
public class Drivetrain {

    public enum LocalizerType { THREE_DEAD_WHEEL, PINPOINT }
    public static LocalizerType LOCALIZER = LocalizerType.THREE_DEAD_WHEEL;

    /** Tunable params for the three-dead-wheel localizer. */
    public static class ThreeWheelParams {
        public double parTicksPerInch = 0.0;
        public double perpTicksPerInch = 0.0;
        public double par0_y = 0.0;
        public double par1_y = 0.0;
        public double perp_x = 0.0;
        public DcMotorSimple.Direction par0Dir  = DcMotorSimple.Direction.FORWARD;
        public DcMotorSimple.Direction par1Dir  = DcMotorSimple.Direction.FORWARD;
        public DcMotorSimple.Direction perpDir  = DcMotorSimple.Direction.FORWARD;
    }
    public static ThreeWheelParams THREE_WHEEL = new ThreeWheelParams();

    /** Tunable params for the goBILDA Pinpoint localizer. */
    public static class PinpointParams {
        public double podXOffsetMM = 0.0;
        public double podYOffsetMM = 0.0;
        public double ticksPerMM = 0.0;
        public GoBildaPinpointDriver.EncoderDirection parDir =
                GoBildaPinpointDriver.EncoderDirection.FORWARD;
        public GoBildaPinpointDriver.EncoderDirection perpDir =
                GoBildaPinpointDriver.EncoderDirection.FORWARD;
        public double yawScalar = 1.0;
    }
    public static PinpointParams PINPOINT = new PinpointParams();

    private final DcMotorEx leftFront, leftBack, rightFront, rightBack;

    public Drivetrain(HardwareMap hw) {
        // Preserve existing crossover. DO NOT normalize these names — it will
        // flip the drive direction in surprising ways.
        this.rightFront = hw.get(DcMotorEx.class, "leftFront");
        this.rightBack  = hw.get(DcMotorEx.class, "leftBack");
        this.leftBack   = hw.get(DcMotorEx.class, "rightBack");
        this.leftFront  = hw.get(DcMotorEx.class, "rightFront");

        for (DcMotorEx m : new DcMotorEx[] { leftFront, leftBack, rightFront, rightBack }) {
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }
        // Mirrors MecanumDrive.java:249
        rightFront.setDirection(DcMotorSimple.Direction.REVERSE);
    }

    /**
     * Robot-relative teleop drive. +x forward, +y left, +rot CCW.
     * {@code slow=true} halves all three axes.
     */
    public void drive(double x, double y, double rot, boolean slow) {
        double s = slow ? 0.5 : 1.0;
        x *= s; y *= s; rot *= s;

        // Standard mecanum mixing.
        double lf = x + y + rot;
        double lb = x - y + rot;
        double rf = x - y - rot;
        double rb = x + y - rot;

        double norm = Math.max(1.0,
                Math.max(Math.abs(lf),
                        Math.max(Math.abs(lb),
                                Math.max(Math.abs(rf), Math.abs(rb)))));
        leftFront.setPower(lf / norm);
        leftBack.setPower(lb / norm);
        rightFront.setPower(rf / norm);
        rightBack.setPower(rb / norm);
    }

    public void stop() { drive(0, 0, 0, false); }
}
```

- [ ] **Step 2: Compile.**

Run: `./gradlew :TeamCode:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit.**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/subsystems/Drivetrain.java
git commit -m "pedro: add Drivetrain scaffold (manual mecanum mix, no Pedro yet)

Mirrors the crossed hardwareMap from MecanumDrive:238-241 exactly
(see spec §13). Follower and localizer come in Task 9.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 9: Add Pedro `Follower` + localizer abstraction

The implementer MUST verify the Pedro 2.1 Follower/Pose/PathChain API before writing code in this task. The exact class names below are best-effort from the spec and partial doc reading — they could differ.

**Pre-task research (~15 min):**

1. Open https://pedropathing.com/docs/pathing/installation and the latest quickstart. Identify:
   - The `Follower` class's full package (likely `com.pedropathing.follower.Follower` or `com.pedropathing.ftc.Follower`).
   - How the Follower is constructed — look for a builder (`FollowerBuilder`, `FollowerConstants`) or a direct constructor that takes `HardwareMap` and a localizer.
   - How `Pose` is constructed (likely `com.pedropathing.geometry.Pose(x, y, heading)` in radians).
   - The `PathChain` / `PathBuilder` API — specifically how to build a straight line from pose A to pose B.
2. Open https://pedropathing.com/docs/pathing/custom/localizer and identify:
   - The class name for the three-wheel localizer (likely `ThreeWheelLocalizer`).
   - The class name for the Pinpoint localizer.
   - How either attaches to the Follower (constructor arg vs. setter vs. config file).
3. If a `pedroPathing/` quickstart directory is expected on the path (Pedro sometimes requires a companion config directory), set that up per the Pedro docs.

**Files:**
- Modify: `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/subsystems/Drivetrain.java`

- [ ] **Step 1: Replace `Drivetrain.java` with the Pedro-integrated version.**

Use the scaffold as the starting point and add: `Follower` field, localizer build in the constructor, `getPose` / `setPose` / `followPath` / `isFollowing` / `cancel` / `update` methods. Fields marked `TODO(API)` must be filled in with real Pedro 2.1 class/method names discovered in the pre-task research.

```java
package org.firstinspires.ftc.teamcode.pedro.subsystems;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

// TODO(API): confirm these imports against Pedro 2.1.1 javadoc.
import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.pedropathing.paths.PathBuilder;

@Config
public class Drivetrain {

    public enum LocalizerType { THREE_DEAD_WHEEL, PINPOINT }
    public static LocalizerType LOCALIZER = LocalizerType.THREE_DEAD_WHEEL;

    public static class ThreeWheelParams {
        public double parTicksPerInch = 0.0;
        public double perpTicksPerInch = 0.0;
        public double par0_y = 0.0;
        public double par1_y = 0.0;
        public double perp_x = 0.0;
        public DcMotorSimple.Direction par0Dir = DcMotorSimple.Direction.FORWARD;
        public DcMotorSimple.Direction par1Dir = DcMotorSimple.Direction.FORWARD;
        public DcMotorSimple.Direction perpDir = DcMotorSimple.Direction.FORWARD;
    }
    public static ThreeWheelParams THREE_WHEEL = new ThreeWheelParams();

    public static class PinpointParams {
        public double podXOffsetMM = 0.0;
        public double podYOffsetMM = 0.0;
        public double ticksPerMM = 0.0;
        public GoBildaPinpointDriver.EncoderDirection parDir =
                GoBildaPinpointDriver.EncoderDirection.FORWARD;
        public GoBildaPinpointDriver.EncoderDirection perpDir =
                GoBildaPinpointDriver.EncoderDirection.FORWARD;
        public double yawScalar = 1.0;
    }
    public static PinpointParams PINPOINT = new PinpointParams();

    private final DcMotorEx leftFront, leftBack, rightFront, rightBack;
    private final Follower follower;

    public Drivetrain(HardwareMap hw) {
        this.rightFront = hw.get(DcMotorEx.class, "leftFront");
        this.rightBack  = hw.get(DcMotorEx.class, "leftBack");
        this.leftBack   = hw.get(DcMotorEx.class, "rightBack");
        this.leftFront  = hw.get(DcMotorEx.class, "rightFront");

        for (DcMotorEx m : new DcMotorEx[] { leftFront, leftBack, rightFront, rightBack }) {
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }
        rightFront.setDirection(DcMotorSimple.Direction.REVERSE);

        this.follower = buildFollower(hw);
    }

    /**
     * Construct the Pedro Follower with whichever localizer is selected.
     *
     * TODO(API): Pedro 2.1.1's Follower construction pattern — replace
     * the pseudocode below with the real builder calls from Pedro's
     * installation/quickstart docs. Key things to wire up:
     *   - the four drive motors (already configured above)
     *   - the selected localizer (see buildLocalizer)
     *   - FTC Dashboard integration (so live pose shows up there)
     *
     * Typical shape (subject to change):
     *   return new FollowerBuilder(hw)
     *       .withDriveMotors(leftFront, leftBack, rightFront, rightBack)
     *       .withLocalizer(buildLocalizer(hw))
     *       .build();
     */
    private Follower buildFollower(HardwareMap hw) {
        // TODO(API): implement per Pedro 2.1.1 docs.
        throw new UnsupportedOperationException(
                "TODO(API): implement Follower construction with the selected localizer");
    }

    /**
     * Build the Pedro localizer that matches LOCALIZER.
     *
     * TODO(API): replace with real Pedro 2.1.1 localizer classes and
     * configuration calls, and verify the exact tick/offset constant
     * names used by each.
     */
    private Object buildLocalizer(HardwareMap hw) {
        // TODO(API): implement per Pedro 2.1.1 docs.
        throw new UnsupportedOperationException(
                "TODO(API): implement localizer construction");
    }

    // ---------- Manual teleop drive (Task 8) ----------

    public void drive(double x, double y, double rot, boolean slow) {
        double s = slow ? 0.5 : 1.0;
        x *= s; y *= s; rot *= s;
        double lf = x + y + rot;
        double lb = x - y + rot;
        double rf = x - y - rot;
        double rb = x + y - rot;
        double norm = Math.max(1.0,
                Math.max(Math.abs(lf),
                        Math.max(Math.abs(lb),
                                Math.max(Math.abs(rf), Math.abs(rb)))));
        leftFront.setPower(lf / norm);
        leftBack.setPower(lb / norm);
        rightFront.setPower(rf / norm);
        rightBack.setPower(rb / norm);
    }

    public void stop() { drive(0, 0, 0, false); }

    // ---------- Pedro path following ----------

    /** Call every loop when following a path. */
    public void update() {
        // TODO(API): follower.update(); — verify exact method name.
        follower.update();
    }

    /** Current pose, as reported by Pedro's localizer. */
    public Pose getPose() {
        // TODO(API): follower.getPose(); — verify.
        return follower.getPose();
    }

    /** Re-seed the localizer. Called by ResetPoseCommand. */
    public void setPose(Pose pose) {
        // TODO(API): follower.setStartingPose(pose) or setPose(pose) — verify.
        follower.setStartingPose(pose);
    }

    /**
     * Begin following the given path. cancel() clears it.
     *
     * TEACHING NOTE: we wrap follower.followPath so commands don't import
     * Pedro directly. If Pedro's API changes, only this class changes.
     */
    public void followPath(PathChain path) {
        // TODO(API): follower.followPath(path); or follower.startFollowing(path);
        follower.followPath(path);
    }

    public boolean isFollowing() {
        // TODO(API): follower.isBusy(); — verify.
        return follower.isBusy();
    }

    public void cancel() {
        // TODO(API): follower.breakFollowing(); or .cancel(); — verify.
        follower.breakFollowing();
        stop();
    }

    /** Expose the Follower for PedroCommands.follow(follower, ...). */
    public Follower getFollower() { return follower; }

    // ---------- Path helpers ----------

    /**
     * Build a straight-line PathChain from current pose to target pose.
     * Used by PathToPoseCommand, SnapToAprilTagCommand, ApproachShotCommand.
     *
     * TODO(API): PathBuilder.lineTo(...) or similar — verify Pedro 2.1.1 API.
     */
    public PathChain straightLineTo(Pose target) {
        // TODO(API): implement per Pedro's PathBuilder docs.
        throw new UnsupportedOperationException(
                "TODO(API): implement PathBuilder-based straight-line construction");
    }
}
```

- [ ] **Step 2: Resolve every `TODO(API)` against the Pedro docs and javadoc.**

Walk the file top to bottom, replace every `TODO(API)` with the real Pedro 2.1.1 call. This task is **not complete** until:

- `buildFollower(hw)` returns a working `Follower`, not a stub.
- `buildLocalizer(hw)` returns the correct localizer instance for each `LocalizerType`.
- `straightLineTo(target)` returns a real `PathChain` from the current pose to the target. Tasks 12, 13, and 14 depend on this; shipping a stub here means every driver-assist throws at runtime.
- Pose field accessors (`getX()/getY()/getHeading()` vs. `.x/.y/.heading`) are settled. Record the answer as a one-line comment at the top of `Drivetrain.java`. Tasks 13, 14, and 15 reuse this answer.

**HARD GATE:** If the Pedro 2.1.1 API for `Follower` construction, `PathBuilder`, or localizer wiring cannot be resolved from docs / javadoc / Pedro example repos after ~30 minutes of research, STOP. Do not proceed to Task 10. Ask for help — implementing the rest of the plan against a stubbed `Drivetrain` will produce compilation successes that fail immediately on-robot.

- [ ] **Step 3: Verify no `TODO(API)` remains.**

Run: `! grep -n 'TODO(API)' TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/subsystems/Drivetrain.java`
Expected: no output (exit 0).

- [ ] **Step 4: Compile.**

Run: `./gradlew :TeamCode:assembleDebug`
Expected: `BUILD SUCCESSFUL`. Any "cannot find symbol" on a Pedro class means an import or class name is wrong — re-check against the javadoc.

- [ ] **Step 5: Commit.**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/subsystems/Drivetrain.java
git commit -m "pedro: integrate Pedro Follower + localizer abstraction in Drivetrain

LOCALIZER flag on the @Config class selects between Pedro's
ThreeWheelLocalizer and PinpointLocalizer. All tuning constants
start as placeholders to be filled in during on-robot tuning.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Chunk 4: Commands

### Task 10: Default commands (`TeleOpDriveCommand`, `IndicatorDefaultCommand`)

Ivy doesn't ship an automatic "default command" concept. The pattern we use: `RobotContainer.resumeDefaults()` schedules the default commands, and each default uses `InterruptedBehavior.END` so preempting commands cleanly end them; when the preempting command finishes, `RobotContainer` re-schedules. `GamepadTriggers.poll()` in the OpMode loop handles the edge cases.

Both default commands are written using the `Command` interface directly (not the builder) because they live for the entire OpMode.

**Files:**
- Create: `.../pedro/commands/TeleOpDriveCommand.java`
- Create: `.../pedro/commands/IndicatorDefaultCommand.java`

- [ ] **Step 1: Create `TeleOpDriveCommand.java`.**

```java
package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.ivy.behaviors.BlockedBehavior;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.behaviors.ConflictBehavior;
import com.pedropathing.ivy.behaviors.EndCondition;
import com.pedropathing.ivy.behaviors.InterruptedBehavior;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.teamcode.pedro.subsystems.Drivetrain;

import java.util.Collections;
import java.util.Set;

/**
 * Default Drivetrain command. Reads gamepad1 and calls drive(x, y, rot, slow)
 * every tick. Signs match DriveCodeCommon.drives():
 *   x = -left_stick_y, y = -left_stick_x, rot = -right_stick_x
 * slow = gamepad1.right_bumper.
 *
 * Uses InterruptedBehavior.END so when a driver-assist command claims
 * Drivetrain, this one ends cleanly and RobotContainer can re-schedule
 * it later.
 */
public final class TeleOpDriveCommand implements Command {

    private final Drivetrain drivetrain;
    private final Gamepad gp1;

    public TeleOpDriveCommand(Drivetrain drivetrain, Gamepad gp1) {
        this.drivetrain = drivetrain;
        this.gp1 = gp1;
    }

    @Override public void start() { }

    @Override public void execute() {
        drivetrain.drive(-gp1.left_stick_y, -gp1.left_stick_x, -gp1.right_stick_x,
                gp1.right_bumper);
    }

    @Override public boolean done() { return false; }

    @Override public void end(EndCondition endCondition) { drivetrain.stop(); }

    @Override public Set<Object> requirements() {
        return Collections.singleton((Object) drivetrain);
    }

    @Override public int priority() { return 0; }
    @Override public InterruptedBehavior interruptedBehavior() { return InterruptedBehavior.END; }
    @Override public BlockedBehavior blockedBehavior() { return BlockedBehavior.CANCEL; }
    @Override public ConflictBehavior conflictBehavior() { return ConflictBehavior.CANCEL; }
}
```

- [ ] **Step 2: Create `IndicatorDefaultCommand.java`.**

```java
package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.ivy.behaviors.BlockedBehavior;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.behaviors.ConflictBehavior;
import com.pedropathing.ivy.behaviors.EndCondition;
import com.pedropathing.ivy.behaviors.InterruptedBehavior;

import org.firstinspires.ftc.teamcode.pedro.AllianceColor;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Indicator;

import java.util.Collections;
import java.util.Set;

/**
 * Default Indicator command. Paints the alliance color every tick.
 * Preempting commands (ShootClearCommand) temporarily override; when
 * they end, this resumes.
 */
public final class IndicatorDefaultCommand implements Command {

    private final Indicator indicator;
    private final AllianceColor alliance;

    public IndicatorDefaultCommand(Indicator indicator, AllianceColor alliance) {
        this.indicator = indicator;
        this.alliance = alliance;
    }

    @Override public void start() { }
    @Override public void execute() { indicator.paintAlliance(alliance); }
    @Override public boolean done() { return false; }
    @Override public void end(EndCondition endCondition) { }
    @Override public Set<Object> requirements() { return Collections.singleton((Object) indicator); }
    @Override public int priority() { return 0; }
    @Override public InterruptedBehavior interruptedBehavior() { return InterruptedBehavior.END; }
    @Override public BlockedBehavior blockedBehavior() { return BlockedBehavior.CANCEL; }
    @Override public ConflictBehavior conflictBehavior() { return ConflictBehavior.CANCEL; }
}
```

- [ ] **Step 3: Compile.**

Run: `./gradlew :TeamCode:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit.**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/commands/TeleOpDriveCommand.java TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/commands/IndicatorDefaultCommand.java
git commit -m "pedro: add default commands (TeleOpDrive, IndicatorDefault)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 11: Gamepad 2 triggered commands

Seven short commands that share the same shape: while-held, run a Runnable; on release (or interrupt), run a different Runnable to stop hardware. Ivy's `Command.build()` builder *does not expose an end/onRelease hook* in the confirmed API (re-verify during pre-chunk-1 API verification — if it does, rewrite using the builder). To guarantee hardware stops on release, we extract a single `HeldActionCommand` helper that implements the full `Command` interface with an explicit `end()`.

**Files:**
- Create: `.../pedro/commands/HeldActionCommand.java`  *(shared helper)*
- Create: `.../pedro/commands/IntakeOneRunCommand.java`
- Create: `.../pedro/commands/IntakeTwoRunCommand.java`
- Create: `.../pedro/commands/PushCommand.java`
- Create: `.../pedro/commands/ShootNearCommand.java`
- Create: `.../pedro/commands/ShootFarCommand.java`
- Create: `.../pedro/commands/ShootClearCommand.java`
- Create: `.../pedro/commands/ClearJamCommand.java`

- [ ] **Step 1: Create `HeldActionCommand.java` (the helper).**

```java
package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.ivy.behaviors.BlockedBehavior;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.behaviors.ConflictBehavior;
import com.pedropathing.ivy.behaviors.EndCondition;
import com.pedropathing.ivy.behaviors.InterruptedBehavior;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Runs a Runnable every tick, stops hardware via another Runnable on
 * release/interrupt. Used by every "hold a button" command on gamepad2.
 *
 * TEACHING NOTE: we use this instead of Ivy's Command.build() because
 * the builder (as documented) has no onEnd/setEnd hook — so a builder-
 * based command would leave hardware running when the button is
 * released. Explicit end() is the safe path.
 */
public final class HeldActionCommand implements Command {

    private final Runnable onRun;
    private final Runnable onStop;
    private final Set<Object> requirements;

    public HeldActionCommand(Runnable onRun, Runnable onStop, Object... reqs) {
        this.onRun = onRun;
        this.onStop = onStop;
        this.requirements = Collections.unmodifiableSet(
                new HashSet<>(Arrays.asList(reqs)));
    }

    @Override public void start() { }
    @Override public void execute() { onRun.run(); }
    @Override public boolean done() { return false; }
    @Override public void end(EndCondition endCondition) { onStop.run(); }
    @Override public Set<Object> requirements() { return requirements; }
    @Override public int priority() { return 0; }
    @Override public InterruptedBehavior interruptedBehavior() { return InterruptedBehavior.END; }
    @Override public BlockedBehavior blockedBehavior() { return BlockedBehavior.CANCEL; }
    @Override public ConflictBehavior conflictBehavior() { return ConflictBehavior.CANCEL; }
}
```

- [ ] **Step 2: Create `IntakeOneRunCommand.java`.**

```java
package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.ivy.Command;

import org.firstinspires.ftc.teamcode.pedro.subsystems.Intake;

/** gamepad2 right_bumper — runs intakeOne at -1.0, stops on release. */
public final class IntakeOneRunCommand {
    private IntakeOneRunCommand() {}
    public static Command create(Intake intake) {
        return new HeldActionCommand(
                () -> intake.setOnePower(-1.0),
                intake::stopOne,
                intake);
    }
}
```

- [ ] **Step 3: Create `IntakeTwoRunCommand.java`.**

```java
package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.ivy.Command;

import org.firstinspires.ftc.teamcode.pedro.subsystems.Intake;

/** gamepad2 left_bumper — runs intakeTwo at -1.0, stops on release. */
public final class IntakeTwoRunCommand {
    private IntakeTwoRunCommand() {}
    public static Command create(Intake intake) {
        return new HeldActionCommand(
                () -> intake.setTwoPower(-1.0),
                intake::stopTwo,
                intake);
    }
}
```

- [ ] **Step 4: Create `PushCommand.java`.**

```java
package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.ivy.Command;

import org.firstinspires.ftc.teamcode.pedro.subsystems.Pusher;

/** gamepad2 y — pushes at -1.0 while held, stops on release. */
public final class PushCommand {
    private PushCommand() {}
    public static Command create(Pusher pusher) {
        return new HeldActionCommand(
                () -> pusher.setPower(-1.0),
                pusher::stop,
                pusher);
    }
}
```

- [ ] **Step 5: Create `ShootNearCommand.java`, `ShootFarCommand.java`, `ShootClearCommand.java`.**

`ShootNearCommand.java`:

```java
package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.ivy.Command;

import org.firstinspires.ftc.teamcode.pedro.subsystems.Shooter;

/** gamepad2 right_trigger > 0.5 — launcher at 1500/750 while held. */
public final class ShootNearCommand {
    private ShootNearCommand() {}
    public static Command create(Shooter shooter) {
        return new HeldActionCommand(
                () -> { shooter.applyPidfIfChanged(); shooter.shootNear(); },
                shooter::stop,
                shooter);
    }
}
```

`ShootFarCommand.java`:

```java
package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.ivy.Command;

import org.firstinspires.ftc.teamcode.pedro.subsystems.Shooter;

/** gamepad2 left_trigger > 0.5 — launcher at 950/1450 while held. */
public final class ShootFarCommand {
    private ShootFarCommand() {}
    public static Command create(Shooter shooter) {
        return new HeldActionCommand(
                () -> { shooter.applyPidfIfChanged(); shooter.shootFar(); },
                shooter::stop,
                shooter);
    }
}
```

`ShootClearCommand.java`:

```java
package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.ivy.Command;
import com.qualcomm.hardware.rev.RevBlinkinLedDriver;

import org.firstinspires.ftc.teamcode.pedro.subsystems.Indicator;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Shooter;

/**
 * gamepad2 right_stick_button — launcher at 10000/10000 + blinkin BLUE
 * while held. Releases return the shooter to 0; IndicatorDefaultCommand
 * reclaims the LED.
 *
 * TEACHING NOTE: the LED is forced BLUE regardless of alliance — that's
 * today's behavior (spec §13) and we preserve it.
 */
public final class ShootClearCommand {
    private ShootClearCommand() {}
    public static Command create(Shooter shooter, Indicator indicator) {
        return new HeldActionCommand(
                () -> {
                    shooter.applyPidfIfChanged();
                    shooter.shootClear();
                    indicator.setPattern(RevBlinkinLedDriver.BlinkinPattern.BLUE);
                },
                shooter::stop,   // indicator default command repaints alliance on release
                shooter, indicator);
    }
}
```

- [ ] **Step 6: Create `ClearJamCommand.java`.**

```java
package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.ivy.Command;

import org.firstinspires.ftc.teamcode.pedro.subsystems.Intake;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Pusher;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Shooter;

/**
 * gamepad2 x — multi-subsystem "unjam everything" macro. While held:
 * intakes forward, launcher reversed, pusher forward. On release: all
 * three stopped. Ports DriveCodeCommon.java:77-80 + :115-118 into one
 * command that requires all three subsystems so any conflicting
 * command preempts cleanly.
 */
public final class ClearJamCommand {
    private ClearJamCommand() {}
    public static Command create(Intake intake, Shooter shooter, Pusher pusher) {
        return new HeldActionCommand(
                () -> {
                    intake.setOnePower(1.0);
                    intake.setTwoPower(1.0);
                    shooter.reverse();
                    pusher.setPower(1.0);
                },
                () -> {
                    intake.stopAll();
                    shooter.stop();
                    pusher.stop();
                },
                intake, shooter, pusher);
    }
}
```

- [ ] **Step 7: Compile.**

Run: `./gradlew :TeamCode:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit.**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/commands/
git commit -m "pedro: add gamepad2 triggered commands with explicit stop on release

HeldActionCommand helper wraps Command interface with run/stop
Runnables so hardware always stops when the button releases
(Ivy's builder has no documented onEnd hook).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 12: `ResetPoseCommand`, `PathToPoseCommand`, driver-override helper

`PathToPoseCommand` is the first command that drives the robot via Pedro. It uses `PedroCommands.follow(follower, pathChain)` from `com.pedropathing.ivy.pedro.PedroCommands` — we just need to wrap that with cancel-on-stick-override logic.

Pattern: `PathToPoseCommand` is itself a `Command` (full interface implementation). In `start()` it builds the path, stores a reference to the inner Pedro follow-command, and calls `Scheduler.schedule(...)` on it. In `done()` it returns true if the inner command is finished OR if `driverOverride` is true. In `end(...)` it cancels the inner command and calls `drivetrain.cancel()`.

**Files:**
- Create: `.../pedro/commands/DriverOverride.java`
- Create: `.../pedro/commands/ResetPoseCommand.java`
- Create: `.../pedro/commands/PathToPoseCommand.java`

- [ ] **Step 1: Create `DriverOverride.java` (tiny helper).**

```java
package org.firstinspires.ftc.teamcode.pedro.commands;

import com.qualcomm.robotcore.hardware.Gamepad;

/**
 * Returns true when any drive stick is past a small deadband — used by
 * driver-assist commands to cancel themselves so the driver can regain
 * control by bumping a stick. See spec §8.4.
 *
 * TEACHING NOTE: the default TeleOpDriveCommand can't cancel driver-
 * assist commands on its own because it doesn't run while they own the
 * Drivetrain. This helper gives every driver-assist a simple "driver
 * moved the stick -> I'm done" rule.
 */
public final class DriverOverride {
    public static final double DEADBAND = 0.1;
    private DriverOverride() {}
    public static boolean isActive(Gamepad gp1) {
        return Math.abs(gp1.left_stick_x)  > DEADBAND
            || Math.abs(gp1.left_stick_y)  > DEADBAND
            || Math.abs(gp1.right_stick_x) > DEADBAND
            || Math.abs(gp1.right_stick_y) > DEADBAND;
    }
}
```

- [ ] **Step 2: Create `ResetPoseCommand.java`.**

```java
package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.geometry.Pose; // TODO(API): verify package
import com.pedropathing.ivy.behaviors.BlockedBehavior;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.behaviors.ConflictBehavior;
import com.pedropathing.ivy.behaviors.EndCondition;
import com.pedropathing.ivy.behaviors.InterruptedBehavior;

import org.firstinspires.ftc.teamcode.pedro.subsystems.Drivetrain;

import java.util.Collections;
import java.util.Set;

/** gamepad1 back — one-shot. Re-seeds Pedro's localizer to target pose. */
public final class ResetPoseCommand implements Command {
    private final Drivetrain drivetrain;
    private final Pose target;

    public ResetPoseCommand(Drivetrain drivetrain, Pose target) {
        this.drivetrain = drivetrain;
        this.target = target;
    }

    @Override public void start() { drivetrain.setPose(target); }
    @Override public void execute() { }
    @Override public boolean done() { return true; }
    @Override public void end(EndCondition endCondition) { }
    @Override public Set<Object> requirements() { return Collections.singleton((Object) drivetrain); }
    @Override public int priority() { return 0; }
    @Override public InterruptedBehavior interruptedBehavior() { return InterruptedBehavior.END; }
    @Override public BlockedBehavior blockedBehavior() { return BlockedBehavior.CANCEL; }
    @Override public ConflictBehavior conflictBehavior() { return ConflictBehavior.CANCEL; }
}
```

- [ ] **Step 3: Create `PathToPoseCommand.java`.**

```java
package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.geometry.Pose; // TODO(API): verify
import com.pedropathing.ivy.behaviors.BlockedBehavior;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.behaviors.ConflictBehavior;
import com.pedropathing.ivy.behaviors.EndCondition;
import com.pedropathing.ivy.behaviors.InterruptedBehavior;
import com.pedropathing.ivy.Scheduler;
import com.pedropathing.ivy.pedro.PedroCommands;
import com.pedropathing.paths.PathChain; // TODO(API): verify
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.teamcode.pedro.subsystems.Drivetrain;

import java.util.Collections;
import java.util.Set;

/**
 * gamepad1 y — straight-line path from current pose to a target pose.
 * Cancels when either the path completes OR the driver nudges a stick.
 *
 * TEACHING NOTE: why wrap Pedro's follow() in our own Command instead of
 * scheduling PedroCommands.follow(...) directly? Two reasons:
 *   1. We need the stick-override check in done(), which PedroCommands'
 *      built-in follow doesn't have.
 *   2. We need to own the Drivetrain requirement. PedroCommands.follow
 *      internally requires the Follower object; we want to require the
 *      Drivetrain *subsystem* so our TeleOpDriveCommand (which requires
 *      Drivetrain) is properly preempted.
 */
public final class PathToPoseCommand implements Command {

    private final Drivetrain drivetrain;
    private final Pose target;
    private final Gamepad gp1;
    private Command innerFollow;

    public PathToPoseCommand(Drivetrain drivetrain, Pose target, Gamepad gp1) {
        this.drivetrain = drivetrain;
        this.target = target;
        this.gp1 = gp1;
    }

    @Override public void start() {
        PathChain path = drivetrain.straightLineTo(target);
        innerFollow = PedroCommands.follow(drivetrain.getFollower(), path);
        Scheduler.schedule(innerFollow);
    }

    @Override public void execute() { /* Drivetrain.update() is called by whoever owns the periodic hook */ }

    @Override public boolean done() {
        return DriverOverride.isActive(gp1) || !Scheduler.isRunning(innerFollow);
    }

    @Override public void end(EndCondition endCondition) {
        if (innerFollow != null) Scheduler.cancel(innerFollow);
        drivetrain.cancel();
    }

    @Override public Set<Object> requirements() { return Collections.singleton((Object) drivetrain); }
    @Override public int priority() { return 0; }
    @Override public InterruptedBehavior interruptedBehavior() { return InterruptedBehavior.END; }
    @Override public BlockedBehavior blockedBehavior() { return BlockedBehavior.CANCEL; }
    @Override public ConflictBehavior conflictBehavior() { return ConflictBehavior.OVERRIDE; }
}
```

**Important:** Review Pedro's `PedroCommands.follow` signature — it may need just `(follower, path)` or may take additional args. Fix the call in `start()` accordingly.

- [ ] **Step 4: Compile.**

Run: `./gradlew :TeamCode:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit.**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/commands/
git commit -m "pedro: add ResetPose, PathToPose commands and DriverOverride helper

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 13: `SnapToAprilTagCommand`

Pillar-only in v1 (spec §8.2). Uses existing `LimelightVision.getPillarTarget(isRedAlliance)`. Computes a standoff pose per spec §8.5 formula:

```
targetHeading = h0 + a
targetX = x0 + (d - s) * cos(targetHeading)
targetY = y0 + (d - s) * sin(targetHeading)
```

Where `a` is `pillarTag.getAngleToTarget()` (radians), `d` is `pillarTag.getDistance()` (inches), `s` is `APRILTAG_STANDOFF_INCHES`.

**Files:**
- Create: `.../pedro/commands/SnapToAprilTagCommand.java`

- [ ] **Step 1: Create `SnapToAprilTagCommand.java`.**

```java
package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.geometry.Pose; // TODO(API): verify
import com.pedropathing.ivy.behaviors.BlockedBehavior;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.behaviors.ConflictBehavior;
import com.pedropathing.ivy.behaviors.EndCondition;
import com.pedropathing.ivy.behaviors.InterruptedBehavior;
import com.pedropathing.ivy.Scheduler;
import com.pedropathing.ivy.pedro.PedroCommands;
import com.pedropathing.paths.PathChain; // TODO(API): verify
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.teamcode.pedro.AllianceColor;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Drivetrain;
import org.firstinspires.ftc.teamcode.vision.LimelightVision;
import org.firstinspires.ftc.teamcode.vision.VisionTarget;

import java.util.Collections;
import java.util.Set;

/**
 * gamepad1 a — path to a standoff pose derived from the nearest visible
 * pillar AprilTag. Replaces today's autoAlign toggle with a proper
 * Pedro-driven path. Aborts if the tag isn't visible at start.
 *
 * Pillar-only in v1; center-tag snap is deferred (spec §14) because
 * LimelightVision.readCenterAprilTag() currently only returns ball
 * colors, not a positional target.
 */
public final class SnapToAprilTagCommand implements Command {

    private final Drivetrain drivetrain;
    private final LimelightVision vision;
    private final AllianceColor alliance;
    private final double standoffInches;
    private final Gamepad gp1;

    private Command innerFollow;
    private boolean abort = false;

    public SnapToAprilTagCommand(Drivetrain drivetrain, LimelightVision vision,
                                 AllianceColor alliance, double standoffInches, Gamepad gp1) {
        this.drivetrain = drivetrain;
        this.vision = vision;
        this.alliance = alliance;
        this.standoffInches = standoffInches;
        this.gp1 = gp1;
    }

    @Override public void start() {
        VisionTarget tag = vision.getPillarTarget(alliance == AllianceColor.RED);
        if (!tag.isTargetFound()) {
            abort = true;
            return;
        }
        Pose current = drivetrain.getPose();
        double a = tag.getAngleToTarget();       // radians
        double d = tag.getDistance();            // inches
        // TODO(API): Pose field accessors may be getX/getY/getHeading or .x/.y/.heading.
        double x0 = current.getX();
        double y0 = current.getY();
        double h0 = current.getHeading();
        double targetHeading = h0 + a;
        double targetX = x0 + (d - standoffInches) * Math.cos(targetHeading);
        double targetY = y0 + (d - standoffInches) * Math.sin(targetHeading);
        Pose target = new Pose(targetX, targetY, targetHeading);

        PathChain path = drivetrain.straightLineTo(target);
        innerFollow = PedroCommands.follow(drivetrain.getFollower(), path);
        Scheduler.schedule(innerFollow);
    }

    @Override public void execute() { }

    @Override public boolean done() {
        if (abort) return true;
        return DriverOverride.isActive(gp1) || !Scheduler.isRunning(innerFollow);
    }

    @Override public void end(EndCondition endCondition) {
        if (innerFollow != null) Scheduler.cancel(innerFollow);
        drivetrain.cancel();
    }

    @Override public Set<Object> requirements() { return Collections.singleton((Object) drivetrain); }
    @Override public int priority() { return 0; }
    @Override public InterruptedBehavior interruptedBehavior() { return InterruptedBehavior.END; }
    @Override public BlockedBehavior blockedBehavior() { return BlockedBehavior.CANCEL; }
    @Override public ConflictBehavior conflictBehavior() { return ConflictBehavior.OVERRIDE; }
}
```

- [ ] **Step 2: Compile.**

Run: `./gradlew :TeamCode:assembleDebug`
Expected: `BUILD SUCCESSFUL`. If `Pose.getX()/getY()/getHeading()` fails, try field access (`.x`, `.y`, `.heading`) or check Pedro's javadoc for the real accessors.

- [ ] **Step 3: Commit.**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/commands/SnapToAprilTagCommand.java
git commit -m "pedro: add SnapToAprilTagCommand (pillar-only v1)

Computes standoff pose from Limelight's relative tag vector plus
current Pedro pose (spec §8.5 formula). Pedro stays the source of
truth for bot pose; Limelight provides only the relative vector.
Aborts cleanly if the tag isn't visible at start.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 14: `ApproachShotCommand`

Similar shape to `SnapToAprilTagCommand`, but the target pose is "just far enough from the pillar to be at `IDEAL_SHOOT_DISTANCE`" — effectively the same formula with `s = IDEAL_SHOOT_DISTANCE` instead of `APRILTAG_STANDOFF_INCHES`. Replaces today's `gamepad1.b` creep-forward behavior.

**Files:**
- Create: `.../pedro/commands/ApproachShotCommand.java`

- [ ] **Step 1: Create `ApproachShotCommand.java`.**

```java
package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.geometry.Pose; // TODO(API): verify
import com.pedropathing.ivy.behaviors.BlockedBehavior;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.behaviors.ConflictBehavior;
import com.pedropathing.ivy.behaviors.EndCondition;
import com.pedropathing.ivy.behaviors.InterruptedBehavior;
import com.pedropathing.ivy.Scheduler;
import com.pedropathing.ivy.pedro.PedroCommands;
import com.pedropathing.paths.PathChain; // TODO(API): verify
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.teamcode.pedro.AllianceColor;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Drivetrain;
import org.firstinspires.ftc.teamcode.vision.LimelightVision;
import org.firstinspires.ftc.teamcode.vision.VisionTarget;

import java.util.Collections;
import java.util.Set;

/**
 * gamepad1 b — path to a standoff pose at IDEAL_SHOOT_DISTANCE from the
 * nearest pillar. Replaces today's proportional creep-forward with a
 * proper Pedro path.
 */
public final class ApproachShotCommand implements Command {

    private final Drivetrain drivetrain;
    private final LimelightVision vision;
    private final AllianceColor alliance;
    private final double idealShootDistance;
    private final Gamepad gp1;

    private Command innerFollow;
    private boolean abort = false;

    public ApproachShotCommand(Drivetrain drivetrain, LimelightVision vision,
                               AllianceColor alliance, double idealShootDistance,
                               Gamepad gp1) {
        this.drivetrain = drivetrain;
        this.vision = vision;
        this.alliance = alliance;
        this.idealShootDistance = idealShootDistance;
        this.gp1 = gp1;
    }

    @Override public void start() {
        VisionTarget tag = vision.getPillarTarget(alliance == AllianceColor.RED);
        if (!tag.isTargetFound()) {
            abort = true;
            return;
        }
        Pose current = drivetrain.getPose();
        double a = tag.getAngleToTarget();
        double d = tag.getDistance();
        double x0 = current.getX();
        double y0 = current.getY();
        double h0 = current.getHeading();
        double targetHeading = h0 + a;
        double targetX = x0 + (d - idealShootDistance) * Math.cos(targetHeading);
        double targetY = y0 + (d - idealShootDistance) * Math.sin(targetHeading);
        Pose target = new Pose(targetX, targetY, targetHeading);

        PathChain path = drivetrain.straightLineTo(target);
        innerFollow = PedroCommands.follow(drivetrain.getFollower(), path);
        Scheduler.schedule(innerFollow);
    }

    @Override public void execute() { }
    @Override public boolean done() {
        if (abort) return true;
        return DriverOverride.isActive(gp1) || !Scheduler.isRunning(innerFollow);
    }
    @Override public void end(EndCondition endCondition) {
        if (innerFollow != null) Scheduler.cancel(innerFollow);
        drivetrain.cancel();
    }
    @Override public Set<Object> requirements() { return Collections.singleton((Object) drivetrain); }
    @Override public int priority() { return 0; }
    @Override public InterruptedBehavior interruptedBehavior() { return InterruptedBehavior.END; }
    @Override public BlockedBehavior blockedBehavior() { return BlockedBehavior.CANCEL; }
    @Override public ConflictBehavior conflictBehavior() { return ConflictBehavior.OVERRIDE; }
}
```

- [ ] **Step 2: Compile.**

Run: `./gradlew :TeamCode:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit.**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/commands/ApproachShotCommand.java
git commit -m "pedro: add ApproachShotCommand (path to IDEAL_SHOOT_DISTANCE)

Replaces the gamepad1.b proportional creep-forward from
DriveCodeCommon.java:42-46 with a Pedro path to the same
standoff distance from the nearest pillar tag.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Chunk 5: OpMode Assembly and Verification

### Task 15: `RobotContainer`

The "one place" that wires everything together. Holds subsystem instances, preset constants, and the gamepad binding table. Exposes `scheduleDefaults()` and `tick()`, plus an opt-in `bindTriggers()` that the full OpMode calls (and the bring-up OpMode skips).

**Pre-task verification:**

- [ ] Confirm `LimelightVision`'s public constructor signature in `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/vision/LimelightVision.java`. At the time this plan was written it was `public LimelightVision(HardwareMap hardwareMap, String name, Telemetry telemetry)` (line 143). If it has changed, adjust the instantiation below.

**Files:**
- Create: `.../pedro/RobotContainer.java`

- [ ] **Step 1: Create `RobotContainer.java` (trigger binding is opt-in; constructor does NOT auto-bind).**

```java
package org.firstinspires.ftc.teamcode.pedro;

import com.acmerobotics.dashboard.config.Config;
import com.pedropathing.geometry.Pose; // TODO(API): verify
import com.pedropathing.ivy.Scheduler;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.pedro.commands.ApproachShotCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.ClearJamCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.IndicatorDefaultCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.IntakeOneRunCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.IntakeTwoRunCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.PathToPoseCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.PushCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.ResetPoseCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.ShootClearCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.ShootFarCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.ShootNearCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.SnapToAprilTagCommand;
import org.firstinspires.ftc.teamcode.pedro.commands.TeleOpDriveCommand;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Drivetrain;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Indicator;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Intake;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Pusher;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Shooter;
import org.firstinspires.ftc.teamcode.pedro.util.GamepadTriggers;
import org.firstinspires.ftc.teamcode.vision.LimelightVision;

/**
 * Single place where all subsystems, commands, and bindings live. If
 * you want to change what a button does, edit this file and nothing
 * else.
 *
 * TEACHING NOTE: the pattern is "construct everything, register
 * defaults, wire triggers." Nowhere else in the pedro package does
 * anything read a gamepad directly or call Scheduler.schedule.
 */
@Config
public class RobotContainer {

    // Presets. Field-tuned TODOs.
    public static Pose STARTING_POSE = new Pose(0, 0, 0);
    public static Pose SCORING_POSE  = new Pose(24, 0, 0);
    public static double APRILTAG_STANDOFF_INCHES = 24.0;
    public static double IDEAL_SHOOT_DISTANCE     = 97.0;

    public final Drivetrain drivetrain;
    public final Intake intake;
    public final Shooter shooter;
    public final Pusher pusher;
    public final Indicator indicator;
    public final LimelightVision vision;

    private final Gamepad gp1, gp2;
    private final AllianceColor alliance;
    private final Telemetry telemetry;
    private final GamepadTriggers triggers = new GamepadTriggers();

    private final com.pedropathing.ivy.Command teleOpDrive;
    private final com.pedropathing.ivy.Command indicatorDefault;

    public RobotContainer(HardwareMap hw, Gamepad gp1, Gamepad gp2,
                          AllianceColor alliance, Telemetry telemetry) {
        this.gp1 = gp1;
        this.gp2 = gp2;
        this.alliance = alliance;
        this.telemetry = telemetry;

        this.drivetrain = new Drivetrain(hw);
        this.intake     = new Intake(hw);
        this.shooter    = new Shooter(hw);
        this.pusher     = new Pusher(hw);
        this.indicator  = new Indicator(hw);
        this.vision     = new LimelightVision(hw, "limelight", telemetry);

        this.teleOpDrive       = new TeleOpDriveCommand(drivetrain, gp1);
        this.indicatorDefault  = new IndicatorDefaultCommand(indicator, alliance);

        // Trigger binding is opt-in — PedroBringUpTeleOp skips this call.
    }

    /**
     * Wire all gamepad triggers to commands. PedroBringUpTeleOp
     * (defaults-only, spec §10 step 2) does NOT call this. PedroTeleOp
     * calls it once after construction.
     *
     * TEACHING NOTE (spec §9.3): gamepad1.a changes from the toggle
     * behavior in DriveCode to hold-to-run here. This is intentional
     * and matches the spec — hold-to-run is the idiomatic command-based
     * pattern and removes the autoAlignActive state machine.
     */
    public void bindTriggers() {
        // ---- gamepad 2 (operator) ----
        triggers.whileTrue(() -> gp2.right_bumper,        () -> IntakeOneRunCommand.create(intake));
        triggers.whileTrue(() -> gp2.left_bumper,         () -> IntakeTwoRunCommand.create(intake));
        triggers.whileTrue(() -> gp2.x,                   () -> ClearJamCommand.create(intake, shooter, pusher));
        triggers.whileTrue(() -> gp2.y,                   () -> PushCommand.create(pusher));
        triggers.whileTrue(() -> gp2.right_trigger > 0.5, () -> ShootNearCommand.create(shooter));
        triggers.whileTrue(() -> gp2.left_trigger  > 0.5, () -> ShootFarCommand.create(shooter));
        triggers.whileTrue(() -> gp2.right_stick_button,  () -> ShootClearCommand.create(shooter, indicator));

        // ---- gamepad 1 (driver) ----
        triggers.onTrue   (() -> gp1.back,                () -> new ResetPoseCommand(drivetrain, STARTING_POSE));
        triggers.whileTrue(() -> gp1.y,                   () -> new PathToPoseCommand(drivetrain, SCORING_POSE, gp1));
        triggers.whileTrue(() -> gp1.a,                   () -> new SnapToAprilTagCommand(drivetrain, vision, alliance, APRILTAG_STANDOFF_INCHES, gp1));
        triggers.whileTrue(() -> gp1.b,                   () -> new ApproachShotCommand(drivetrain, vision, alliance, IDEAL_SHOOT_DISTANCE, gp1));
    }

    /** Called from PedroTeleOp once after Scheduler.reset(). */
    public void scheduleDefaults() {
        Scheduler.schedule(teleOpDrive);
        Scheduler.schedule(indicatorDefault);
    }

    /** Call every loop, before Scheduler.execute(). Re-schedules defaults
     *  when nothing else owns their subsystem. Also ticks Pedro's
     *  follower so the localizer stays current even when no path is
     *  active — the driver-assists need an up-to-date pose the moment
     *  they fire. */
    public void tick() {
        // NOTE: the spec §6.1 originally said "only update when following."
        // That would stop pose tracking during manual driving and break
        // SnapToAprilTagCommand / PathToPoseCommand at start (they read
        // a stale pose). Pedro's follower.update() ticks the localizer
        // whether or not a path is active, so we always call it.
        drivetrain.update();

        // Re-schedule defaults if they ended because something preempted.
        if (!Scheduler.isScheduled(teleOpDrive))      Scheduler.schedule(teleOpDrive);
        if (!Scheduler.isScheduled(indicatorDefault)) Scheduler.schedule(indicatorDefault);

        triggers.poll();
    }

    public void pollTelemetry(Telemetry t) {
        Pose pose = drivetrain.getPose();
        t.addData("Alliance", alliance);
        t.addData("Localizer", Drivetrain.LOCALIZER);
        t.addData("Pose", "x=%.1f y=%.1f h=%.1f°",
                pose.getX(), pose.getY(), Math.toDegrees(pose.getHeading()));
        t.addData("Following", drivetrain.isFollowing());
    }
}
```

- [ ] **Step 2: Compile.**

Run: `./gradlew :TeamCode:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit.**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/RobotContainer.java
git commit -m "pedro: add RobotContainer (subsystems + defaults + bindings)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 16: `PedroBringUpTeleOp` (debug OpMode)

Defaults-only TeleOp for spec §10 step 2. If this feels like today's `DriveCode` drive portion (and the pose streams to Dashboard), the subsystem + Drivetrain layer is correct.

**Files:**
- Create: `.../pedro/PedroBringUpTeleOp.java`

- [ ] **Step 1: Create `PedroBringUpTeleOp.java`.**

```java
package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.ivy.Scheduler;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * Debug-only TeleOp. Binds default commands ONLY — no driver assists,
 * no intake/shoot/push buttons. Used for the spec §10 step 2 gate.
 *
 * If manual drive feels identical to today's DriveCode (mecanum +
 * slow mode), the hardware layer and Drivetrain subsystem are correct.
 */
@TeleOp(name = "Pedro BringUp", group = "pedro-debug")
public class PedroBringUpTeleOp extends LinearOpMode {
    @Override public void runOpMode() {
        AllianceColor alliance = AllianceColor.BLUE;
        telemetry.addLine("Pedro BringUp — defaults only");
        telemetry.update();
        waitForStart();
        if (isStopRequested()) return;

        Scheduler.reset();
        RobotContainer robot = new RobotContainer(hardwareMap, gamepad1, gamepad2, alliance, telemetry);
        // Intentionally NOT calling robot.bindTriggers() — this OpMode
        // exercises only the default commands (spec §10 step 2 gate).
        robot.scheduleDefaults();

        while (opModeIsActive()) {
            robot.tick();
            Scheduler.execute();
            robot.pollTelemetry(telemetry);
            telemetry.update();
        }
    }
}
```

- [ ] **Step 2: Compile.**

Run: `./gradlew :TeamCode:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit.**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/PedroBringUpTeleOp.java
git commit -m "pedro: add PedroBringUpTeleOp (defaults-only debug OpMode)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 17: `PedroTeleOp` (full TeleOp with alliance select)

**Files:**
- Create: `.../pedro/PedroTeleOp.java`

- [ ] **Step 1: Create `PedroTeleOp.java`.**

```java
package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.ivy.Scheduler;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * The full command-based TeleOp. During INIT, gamepad1 dpad_left/right
 * selects the alliance; press START to begin. Then every behavior in
 * today's DriveCode is wired through Ivy commands, plus the three new
 * driver-assist commands.
 */
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

        Scheduler.reset();
        RobotContainer robot = new RobotContainer(hardwareMap, gamepad1, gamepad2, alliance, telemetry);
        robot.bindTriggers();
        robot.scheduleDefaults();

        while (opModeIsActive()) {
            robot.tick();
            Scheduler.execute();
            robot.pollTelemetry(telemetry);
            telemetry.update();
        }
    }
}
```

- [ ] **Step 2: Compile.**

Run: `./gradlew :TeamCode:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit.**

```bash
git add TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/PedroTeleOp.java
git commit -m "pedro: add PedroTeleOp (full command-based TeleOp with alliance select)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 18: On-robot verification walkthrough

This isn't a code task; it's the handoff checklist the team runs on the robot after deploying the new `@TeleOp`. Put it in the repo as a markdown file for future reference.

**Files:**
- Create: `docs/superpowers/plans/pedro-ivy-bringup-checklist.md`

- [ ] **Step 1: Write the checklist.**

```markdown
# Pedro + Ivy TeleOp bring-up checklist

Do these in order. Each step is a Go / No-Go gate — do not skip ahead
on a No-Go.

## 1. Compile
- [ ] `./gradlew :TeamCode:assembleDebug` succeeds.
- [ ] `./gradlew :TeamCode:installDebug` deploys to the Control Hub.

## 2. PedroBringUpTeleOp (defaults only)
- [ ] Init "Pedro BringUp" from the DS.
- [ ] Dashboard connects; `Pose` shows `x=0 y=0 h=0°`.
- [ ] Press START. Mecanum driving feels the same as today's DriveCode
      (including `gp1.right_bumper` slow-mode).
- [ ] **Hardware-crossover sanity:** push `gp1.left_stick_y` forward —
      robot moves physically forward (not backward). Push the stick
      right — robot strafes right. Right-stick-x right — robot rotates
      clockwise. If any are wrong, the crossed hardwareMap (spec §13)
      didn't round-trip correctly; fix before moving on.
- [ ] Blinkin LED shows BLUE (default alliance).

## 3. Localizer check
- [ ] With bring-up running, drive forward 24", then strafe 24", then
      turn 360°, then return. Pose returns to roughly `(0, 0, 0)`.
- [ ] If pose drifts badly, stop. Run Pedro's three-wheel tuning
      routines (`parTicksPerInch`, `perpTicksPerInch`, `par0_y`,
      `par1_y`, `perp_x`, and direction flags) until the 24/24/360 test
      returns to origin ± 2 inches / 5°.
- [ ] Optionally: set `Drivetrain.LOCALIZER = PINPOINT` in Dashboard,
      reconfigure the Robot Controller for a `pinpoint` I²C device,
      re-run this test.

## 4. PedroTeleOp — subsystem buttons
- [ ] Init "Pedro TeleOp". Dpad-left / dpad-right toggles alliance
      color. Press START.
- [ ] `gp2.RB` runs intakeOne forward (like today).
- [ ] `gp2.LB` runs intakeTwo forward (like today).
- [ ] `gp2.x` (held) runs the clear-jam macro: both intakes forward,
      launcher reversed, pusher forward. Releasing stops all three.
- [ ] `gp2.y` (held) runs pusher at -1.0.
- [ ] `gp2.RT > 0.5` spins launcher at 1500/750. `gp2.LT > 0.5` spins
      at 950/1450. `gp2.right_stick_button` spins at 10000/10000 and
      flashes blinkin BLUE.
- [ ] FTC Dashboard `Shooter.PIDF` coefficient changes take effect
      live (no redeploy).

## 5. PedroTeleOp — driver-assists
- [ ] `gp1.back` resets pose to `STARTING_POSE`. Verify on Dashboard.
- [ ] `gp1.y` (held): temporarily set `SCORING_POSE = (24, 0, 0)` in
      Dashboard so the robot only moves 24" forward. Hold `y`. Robot
      paths to the target and stops. Bumping any drive stick cancels
      cleanly.
- [ ] `gp1.a` (held): place a pillar AprilTag in view. Override
      `APRILTAG_STANDOFF_INCHES` to 36 in Dashboard. Hold `a`. Robot
      paths to a standoff from the tag. Release → cancels. Restore
      standoff to 24 once stable.
- [ ] `gp1.b` (held): with pillar visible, hold `b`. Robot paths to
      `IDEAL_SHOOT_DISTANCE = 97`. Tape-measure to confirm.

## 6. Regression
- [ ] Init and run the old "DriveCode" from the RR side. Confirm it
      still works end-to-end (drive, intakes, shoots, alliance select,
      old auto-align on `gp1.a`).
- [ ] Init and run one of the Road Runner autos (e.g. BlueAuto).
      Confirm it runs unchanged.

## 7. Coexistence check
- [ ] If any gate in §2–§6 fails, DO NOT remove or disable the old
      "DriveCode" TeleOp — the new Pedro TeleOp and old RR TeleOp
      coexist in the project. You can always fall back to DriveCode
      on the field while debugging Pedro.

## 8. Tuning follow-ups (after bring-up passes)
- [ ] Field-tune `STARTING_POSE` and `SCORING_POSE` to real field
      coordinates.
- [ ] Field-tune `APRILTAG_STANDOFF_INCHES` to the desired shooter
      stand-off distance.
- [ ] Tune launcher PIDF on-field (Dashboard).
- [ ] If switching to Pinpoint, tune `PINPOINT.*` params using Pedro's
      tuning routines.
```

- [ ] **Step 2: Commit.**

```bash
git add docs/superpowers/plans/pedro-ivy-bringup-checklist.md
git commit -m "docs: add Pedro + Ivy on-robot bring-up checklist

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Post-implementation notes

Known deferred items (see spec §14):
- Center-tag variant of `SnapToAprilTagCommand` — requires a positional center-tag API on `LimelightVision`.
- Spline-based `PathToPoseCommand`.
- Toggle-style auto-align.
- Search-spin fallback if the tag isn't visible.
- Migrating autonomous off Road Runner.

If the `Pose`/`PathChain`/`PathBuilder` API surface in Pedro 2.1.1 differs from the imports used in this plan, the only places that need changing are `Drivetrain.java`, `ResetPoseCommand.java`, `PathToPoseCommand.java`, `SnapToAprilTagCommand.java`, `ApproachShotCommand.java`, and `RobotContainer.java`. The subsystem classes (except `Drivetrain`) import nothing from Pedro, by design — see spec §1 Motivation.
