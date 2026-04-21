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
      routines (`forwardTicksToInches`, `strafeTicksToInches`,
      `turnTicksToInches`, `leftPodY`, `rightPodY`, `strafePodX`, and
      encoder direction flags) until the 24/24/360 test returns to
      origin ± 2 inches / 5°.
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
