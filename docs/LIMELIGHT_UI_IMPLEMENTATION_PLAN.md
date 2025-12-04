# Limelight Vision Test UI - Button-Based OpMode for FTC Drive Hub

> **Note**: This plan should be saved to the code repository for version control and future reference.
> Recommended location: `/Users/mechaknights/StudioProjects/RR2025-2026/docs/LIMELIGHT_UI_IMPLEMENTATION_PLAN.md`

## Quick Start

**To resume this implementation:**
1. Read this plan file
2. Review the Implementation Steps checklist below
3. Continue from the last unchecked item
4. Test thoroughly after each phase

**Overall Progress:**
- [ ] Phase 1: Core Infrastructure (Steps 1-8)
- [ ] Phase 2: Navigation Framework (Steps 9-15)
- [ ] Phase 3: Distance Input Screen (Steps 16-20)
- [ ] Phase 4: Limelight Integration (Steps 21-25)
- [ ] Phase 5: Test 1 Implementation (Steps 26-29)
- [ ] Phase 6: Test 3 Implementation (Steps 30-33)
- [ ] Phase 7: Test 2 Implementation (Steps 34-38)
- [ ] Phase 8: Test 5 Implementation (Steps 39-42)
- [ ] Phase 9: Test 4 Implementation (Steps 43-47)
- [ ] Phase 10: Test 6 Implementation (Steps 48-51)
- [ ] Phase 11: Polish (Steps 52-56)
- [ ] Phase 12: Final Testing (Steps 57-63)
- [ ] Save plan to docs/ directory in repo
- [ ] Commit and push implementation

## Summary

Create a button-based UI OpMode that runs all 6 Limelight vision tests directly on the FTC Drive Hub. This allows testing the vision system without using the CLI, with full gamepad navigation and real-time results displayed on the Driver Station.

**Key Features:**
- Button-based navigation (no text input required)
- All 6 tests from LimelightTester ported to OpMode
- Real LimelightVision.java integration
- Interactive UI with multiple screens
- Distance input via increment/decrement buttons
- Start/Stop control for long-running tests

## Requirements

- **New file only**: `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/LimelightTestUI.java`
- **No modifications** to existing files
- **Real hardware**: Uses actual LimelightVision.java class
- **All 6 tests** included with button adaptations

## Implementation Overview

### Single File to Create
**`TeamCode/src/main/java/org/firstinspires/ftc/teamcode/LimelightTestUI.java`**
- Estimated ~900 lines of code
- @TeleOp annotation for Drive Hub visibility
- Extends LinearOpMode
- Contains all UI, navigation, and test execution logic

### State Machine Design

**States:**
```java
enum UIState {
    MAIN_MENU,           // Test selection with DPAD navigation
    ALLIANCE_SELECT,     // Red/Blue selection with bumpers
    DISTANCE_INPUT,      // Numeric input with DPAD/bumpers
    TEST_RUNNING,        // Test execution with progress display
    TEST_RESULTS,        // Results display with scrolling
    ERROR_SCREEN         // Hardware error handling
}
```

**State Flow:**
```
MAIN_MENU
  ├─ Test 1 → ALLIANCE_SELECT → DISTANCE_INPUT → TEST_RUNNING → TEST_RESULTS
  ├─ Test 2 → ALLIANCE_SELECT → TEST_RUNNING → TEST_RESULTS
  ├─ Test 3 → TEST_RUNNING → TEST_RESULTS
  ├─ Test 4 → TEST_RUNNING (interactive) → TEST_RESULTS
  ├─ Test 5 → ALLIANCE_SELECT → TEST_RESULTS
  └─ Test 6 → ALLIANCE_SELECT → TEST_RUNNING → TEST_RESULTS
```

### Button Mapping

**Global Controls:**
- B button: Always goes back/cancels
- Consistent patterns across all screens

**MAIN_MENU:**
- DPAD UP/DOWN: Navigate test list (wrap around)
- A: Select test and proceed
- Y: Show connection status and calibration info
- B: Exit OpMode (with confirmation)

**ALLIANCE_SELECT:**
- LEFT BUMPER: Select Blue Alliance (tag 20)
- RIGHT BUMPER: Select Red Alliance (tag 24)
- A: Confirm selection
- B: Cancel, return to menu

**DISTANCE_INPUT:** (Test 1 only)
- DPAD UP/DOWN: Adjust by ±1 inch
- LEFT/RIGHT BUMPER: Adjust by ±10 inches
- A: Confirm distance
- B: Cancel
- Range: 12.0" - 120.0"

**TEST_RUNNING:**
- A: Capture reading (Test 4 only)
- B: Stop/cancel test
- Most tests: Auto-transition when complete

**TEST_RESULTS:**
- A or B: Return to MAIN_MENU
- DPAD UP/DOWN: Scroll results (future enhancement)

## Test Implementations

### Test 1: Distance Calculation
**Flow:** MAIN_MENU → ALLIANCE_SELECT → DISTANCE_INPUT → TEST_RESULTS

**Logic:**
- Capture single frame of pillar AprilTag (20 or 24)
- Display: tx, ty, ta, calculated distance, angle
- Compare calculated vs. user-entered actual distance
- PASS if error < 10%

**Button Adaptation:**
- Distance input: DPAD up/down (±1"), bumpers (±10")
- Default: 48.0"
- Range: 12.0" - 120.0"

### Test 2: Detection Reliability
**Flow:** MAIN_MENU → ALLIANCE_SELECT → TEST_RUNNING → TEST_RESULTS

**Logic:**
- Capture 100 frames at 50ms intervals (5 seconds total)
- Live progress display every 10 frames
- Calculate detection rate, avg tx/ty/distance, min/max
- PASS if detection rate ≥ 90%

**Button Adaptation:**
- B button: Cancel test early
- Progress: Shows current frame count and detection %

### Test 3: Pipeline Switching
**Flow:** MAIN_MENU → TEST_RUNNING → TEST_RESULTS

**Logic:**
- Test all 4 pipelines sequentially:
  - Pipeline 0: Purple balls
  - Pipeline 1: Green balls
  - Pipeline 2: Pillar AprilTags (20, 24)
  - Pipeline 3: Center AprilTags (21, 22, 23)
- 500ms delay for stabilization after each switch
- PASS if all 4 pipelines switch correctly

**Button Adaptation:**
- Fully automated (no user input)
- Returns to Pipeline 2 at end

### Test 4: Center Tag Sequences
**Flow:** MAIN_MENU → TEST_RUNNING → TEST_RESULTS

**Logic:**
- Interactive capture mode
- User shows tags 21, 22, or 23 to camera
- Each capture identifies tag and ball sequence
- Validates sequence mapping:
  - Tag 21 → [GREEN, PURPLE, PURPLE]
  - Tag 22 → [PURPLE, GREEN, PURPLE]
  - Tag 23 → [PURPLE, PURPLE, GREEN]

**Button Adaptation:**
- A button: Capture current reading
- B button: Finish test and show results
- Allows multiple captures

**CRITICAL WORKAROUND:**
```java
// LimelightVision.java line 244 bug workaround
limelight.switchToCenterTagPipeline();  // Call manually first
sleep(500);  // Stabilize
List<BallColor> sequence = limelight.readCenterAprilTag();
```

### Test 5: Calibration Tuner
**Flow:** MAIN_MENU → ALLIANCE_SELECT → TEST_RESULTS

**Logic:**
- Display current calibration constants:
  - LIMELIGHT_HEIGHT_INCHES
  - LIMELIGHT_ANGLE_DEGREES
  - APRILTAG_HEIGHT_INCHES
- Take single test reading for verification
- Show calculated distance with current calibration

**Button Adaptation:**
- Alliance selection for test reading
- Results screen shows constants and test distance

### Test 6: Run All Tests
**Flow:** MAIN_MENU → ALLIANCE_SELECT → TEST_RUNNING → TEST_RESULTS

**Logic:**
- Run Test 3 (Pipeline Switching) first
- Run Test 2 (Detection Reliability) second
- Combine results into single display
- PASS if both sub-tests pass

**Button Adaptation:**
- Alliance selection used for Test 2
- Sequential execution with progress updates

## Class Structure

### Core Components
```java
@TeleOp(name = "Limelight Test UI", group = "Testing")
public class LimelightTestUI extends LinearOpMode {

    // === STATE ===
    private UIState currentState;
    private int selectedTestIndex = 0;  // 0-5 for tests 1-6
    private boolean isRedAlliance = false;
    private double targetDistance = 48.0;

    // === HARDWARE ===
    private LimelightVision limelight;

    // === RESULTS ===
    private TestResult lastTestResult;

    // === BUTTON EDGE DETECTION ===
    private boolean lastA, lastB, lastX, lastY;
    private boolean lastDpadUp, lastDpadDown;
    private boolean lastLeftBumper, lastRightBumper;

    // === HELPER CLASS ===
    private static class TestResult {
        String testName;
        boolean passed;
        List<String> outputLines;
        // Constructor and methods...
    }
}
```

### Key Methods
- `runOpMode()`: Main entry point, initialization, main loop, cleanup
- `updateButtonStates()`: Copy current button states to "last" states
- `buttonPressed(current, last)`: Rising edge detection
- `handleMainMenu()`: DPAD navigation and test selection
- `handleAllianceSelect()`: Bumper selection for Red/Blue
- `handleDistanceInput()`: DPAD/bumper numeric adjustment
- `handleTestResults()`: Return to menu
- `displayMainMenu()`: Test list with cursor
- `displayAllianceSelect()`: Red/Blue choice display
- `displayDistanceInput()`: Numeric value with controls
- `displayTestProgress(msg)`: Progress messages during test
- `displayTestResults()`: Final results display
- `displayErrorScreen(error)`: Hardware error messages
- `runTest1_DistanceCalculation()`: Test 1 implementation
- `runTest2_DetectionReliability()`: Test 2 implementation
- `runTest3_PipelineSwitching()`: Test 3 implementation
- `runTest4_CenterTagSequences()`: Test 4 implementation
- `runTest5_CalibrationTuner()`: Test 5 implementation
- `runTest6_RunAllTests()`: Test 6 implementation
- `formatDouble(value, decimals)`: String formatting helper
- `determineTagId(sequence)`: Map ball sequence to tag ID

## LimelightVision Integration

### Initialization
```java
try {
    limelight = new LimelightVision(hardwareMap, "limelight", telemetry);
    telemetry.addData("✓", "Limelight initialized");
} catch (IllegalArgumentException e) {
    // Device not in hardware map
    currentState = UIState.ERROR_SCREEN;
    // Show error instructions
} catch (Exception e) {
    // Other errors
    currentState = UIState.ERROR_SCREEN;
}
```

### Key Method Calls
```java
// Get pillar target
VisionTarget target = limelight.getPillarTarget(isRedAlliance);
if (target.isTargetFound()) {
    double tx = target.getTx();
    double ty = target.getTy();
    double distance = target.getDistance();
    int tagId = target.getAprilTagId();
}

// Switch pipelines
limelight.switchToBallPipeline(BallColor.PURPLE);  // Pipeline 0
limelight.switchToBallPipeline(BallColor.GREEN);   // Pipeline 1
limelight.switchToPillarTagPipeline();             // Pipeline 2
limelight.switchToCenterTagPipeline();             // Pipeline 3

// Read center tag
limelight.switchToCenterTagPipeline();  // Workaround for bug
sleep(500);
List<BallColor> sequence = limelight.readCenterAprilTag();

// Check target
boolean hasTarget = limelight.hasTarget();
```

### Cleanup
```java
@Override
public void runOpMode() {
    // ... initialization and main loop ...

    // Cleanup at end
    if (limelight != null) {
        limelight.stop();
    }
}
```

## Error Handling

### Hardware Initialization Errors
- Try-catch around LimelightVision constructor
- IllegalArgumentException: Device not in hardware map
- Display helpful error message with troubleshooting steps
- Allow return to menu (but tests won't work)

### Runtime Errors During Tests
- Try-catch around all vision operations
- Display error message in test results
- Mark test as FAILED
- Allow graceful return to menu

### Null Safety
- Always check `target.isTargetFound()` before accessing data
- Handle empty sequence lists from `readCenterAprilTag()`
- Check `limelight != null` before calling methods

### OpMode Stop Safety
- All test loops check `opModeIsActive()`
- Cleanup in finally block (optional)
- Stop button always works

## Implementation Steps

### Phase 1: Core Infrastructure
- [x] 1. Create LimelightTestUI.java with @TeleOp annotation
- [x] 2. Define UIState enum
- [x] 3. Define TestResult helper class
- [x] 4. Add all instance fields
- [x] 5. Implement runOpMode() skeleton
- [x] 6. Implement updateButtonStates() and buttonPressed()
- [x] 7. Add state switch in main loop
- [x] **Test:** OpMode appears, initializes - BUILD SUCCESSFUL

### Phase 2: Navigation Framework
- [x] 9. Implement displayMainMenu() with formatting
- [x] 10. Implement handleMainMenu() with DPAD navigation
- [x] 11. Add menu cursor with wrap-around
- [x] 12. Implement displayAllianceSelect()
- [x] 13. Implement handleAllianceSelect() with bumpers
- [x] 14. Wire MAIN_MENU → ALLIANCE_SELECT
- [x] **Test:** Navigation and alliance selection work (ready to test on device)

### Phase 3: Distance Input Screen
- [x] 16. Implement displayDistanceInput()
- [x] 17. Implement handleDistanceInput() with DPAD/bumpers
- [x] 18. Add value clamping (12.0 - 120.0)
- [x] 19. Wire for Test 1
- [x] **Test:** Distance adjustment smooth - BUILD SUCCESSFUL

### Phase 4: Limelight Integration
- [ ] 21. Add Limelight initialization in runOpMode()
- [ ] 22. Wrap in try-catch with error handling
- [ ] 23. Implement displayErrorScreen()
- [ ] 24. Add cleanup (limelight.stop())
- [ ] **Test:** Handles missing Limelight gracefully

### Phase 5: Test 1 Implementation
- [ ] 26. Implement runTest1_DistanceCalculation()
- [ ] 27. Implement displayTestResults()
- [ ] 28. Wire full flow
- [ ] **Test:** Complete Test 1 end-to-end

### Phase 6: Test 3 Implementation
- [ ] 30. Implement runTest3_PipelineSwitching()
- [ ] 31. Implement displayTestProgress()
- [ ] 32. Wire flow
- [ ] **Test:** Pipeline test runs

### Phase 7: Test 2 Implementation
- [ ] 34. Implement runTest2_DetectionReliability()
- [ ] 35. Add progress counter display
- [ ] 36. Add B button cancel detection
- [ ] 37. Wire with alliance selection
- [ ] **Test:** 100-frame test runs, can cancel

### Phase 8: Test 5 Implementation
- [ ] 39. Implement runTest5_CalibrationTuner()
- [ ] 40. Access LimelightVision static constants
- [ ] 41. Wire with alliance selection
- [ ] **Test:** Shows calibration values

### Phase 9: Test 4 Implementation
- [ ] 43. Implement runTest4_CenterTagSequences()
- [ ] 44. Implement determineTagId() and formatSequence()
- [ ] 45. Add interactive capture loop
- [ ] 46. Add bug workaround
- [ ] **Test:** Can capture multiple readings

### Phase 10: Test 6 Implementation
- [ ] 48. Implement runTest6_RunAllTests()
- [ ] 49. Call tests 2 and 3 sequentially
- [ ] 50. Merge results
- [ ] **Test:** Both tests run, combined results

### Phase 11: Polish
- [ ] 52. Add telemetry.clear() and .update()
- [ ] 53. Add FPS and pipeline info to menu
- [ ] 54. Format numeric outputs consistently
- [ ] 55. Add button hints to all screens
- [ ] **Test:** All screens look clean

### Phase 12: Final Testing
- [ ] 57. Run each test individually
- [ ] 58. Test error scenarios
- [ ] 59. Test rapid button presses
- [ ] 60. Test cancellation
- [ ] 61. Test interactive capture
- [ ] 62. Verify cleanup on stop
- [ ] 63. Full integration test

**Total Estimated Time:** 9-12 hours

**Progress Tracking:** Check off items as you complete them. Each phase should be tested before moving to the next.

## Critical Files

### Files to Create
1. **`TeamCode/src/main/java/org/firstinspires/ftc/teamcode/LimelightTestUI.java`**
   - NEW FILE (~900 lines)
   - All UI logic, state machine, button handling, test execution

### Reference Files (NO CHANGES)
2. **`TeamCode/src/main/java/org/firstinspires/ftc/teamcode/vision/LimelightVision.java`**
   - Integration point for vision operations
   - Bug on line 244 requires workaround
   - Public static constants for calibration

3. **`TeamCode/src/main/java/org/firstinspires/ftc/teamcode/vision/VisionTarget.java`**
   - Return type from getPillarTarget()
   - Getter methods: getTx(), getTy(), getDistance(), etc.

4. **`LimelightTester/src/main/java/tester/TestRunner.java`**
   - Source of test logic to adapt from CLI
   - Reference for success criteria

5. **`TeamCode/src/main/java/org/firstinspires/ftc/teamcode/DriveCode.java`**
   - Example @TeleOp OpMode pattern
   - Reference for FTC conventions

## Known Issues and Workarounds

### LimelightVision.java Line 244 Bug
**Problem:** `readCenterAprilTag()` calls undefined `switchToAprilTagPipeline()`
**Solution:** Manually call `switchToCenterTagPipeline()` before `readCenterAprilTag()`

### Pipeline Switch Latency
**Solution:** Add 500ms sleep after all pipeline switches

### Button Bounce
**Solution:** Edge detection + 50ms main loop delay

### Telemetry Line Limit
**Solution:** Keep displays concise (~20 lines max)

## Success Criteria

- [ ] OpMode appears in Driver Station under "Testing" group
- [ ] Main menu displays all 6 tests with DPAD navigation
- [ ] Alliance selection works with bumpers
- [ ] Distance input adjusts smoothly with DPAD/bumpers
- [ ] Test 1 captures frame and compares distances
- [ ] Test 2 runs 100 frames with progress display
- [ ] Test 3 tests all 4 pipelines successfully
- [ ] Test 4 allows interactive captures
- [ ] Test 5 shows calibration constants
- [ ] Test 6 runs Tests 2 and 3 in sequence
- [ ] All tests display PASS/FAIL correctly
- [ ] B button always goes back/cancels
- [ ] Error screen appears if Limelight missing
- [ ] Cleanup (limelight.stop()) runs on OpMode stop

## Deployment

1. Build TeamCode module:
   ```bash
   ./gradlew :TeamCode:build
   ```

2. Deploy to Robot Controller:
   ```bash
   ./gradlew installDebug
   ```

3. On Driver Station:
   - Select "TeleOp" mode
   - Find "Limelight Test UI" under "Testing" group
   - Press INIT, then START

4. Configure hardware:
   - Ensure Limelight device named "limelight" in Robot Configuration
   - Verify Limelight connected via USB
   - Configure all 4 pipelines in Limelight web interface
