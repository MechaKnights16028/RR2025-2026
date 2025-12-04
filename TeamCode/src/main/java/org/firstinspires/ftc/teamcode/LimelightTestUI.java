package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.vision.BallColor;
import org.firstinspires.ftc.teamcode.vision.LimelightVision;
import org.firstinspires.ftc.teamcode.vision.LimelightVisionAdapter;
import org.firstinspires.ftc.teamcode.vision.VisionTarget;

import java.util.ArrayList;
import java.util.List;

/**
 * Button-based UI OpMode for testing Limelight vision system on the FTC Drive Hub.
 *
 * This OpMode provides an interactive menu system to run all 6 vision tests without
 * requiring command-line interface or text input. All navigation is done via gamepad buttons.
 *
 * Features:
 * - Main menu with DPAD navigation
 * - Red/Blue alliance selection with bumpers
 * - Distance input with increment/decrement buttons
 * - Real-time test execution with progress display
 * - Test results with PASS/FAIL verdicts
 *
 * Tests Included:
 * 1. Distance Calculation - Verify AprilTag distance accuracy
 * 2. Detection Reliability - 100-frame detection rate test
 * 3. Pipeline Switching - Test all 4 pipeline switches
 * 4. Center Tag Sequences - Interactive tag reading with ball sequence validation
 * 5. Calibration Tuner - Display calibration constants and test reading
 * 6. Run All Tests - Automated execution of Tests 2 and 3
 */
@TeleOp(name = "Limelight Test UI", group = "Testing")
public class LimelightTestUI extends LinearOpMode {

    // ===== STATE MACHINE =====

    /**
     * UI state machine states
     */
    private enum UIState {
        MAIN_MENU,           // Test selection with DPAD navigation
        ALLIANCE_SELECT,     // Red/Blue alliance selection with bumpers
        DISTANCE_INPUT,      // Numeric distance input with DPAD/bumpers
        TEST_RUNNING,        // Test execution with progress display
        TEST_RESULTS,        // Results display with scrolling
        ERROR_SCREEN         // Hardware error handling
    }

    /** Current UI state */
    private UIState currentState;

    // ===== TEST PARAMETERS =====

    /** Currently selected test index (0-5 for tests 1-6) */
    private int selectedTestIndex = 0;

    /** Red alliance (true) or Blue alliance (false) */
    private boolean isRedAlliance = false;

    /** Target distance for Test 1 comparison (inches) */
    private double targetDistance = 48.0;

    /** Test names for menu display */
    private static final String[] TEST_NAMES = {
        "1. Distance Calculation",
        "2. Detection Reliability",
        "3. Pipeline Switching",
        "4. Center Tag Sequences",
        "5. Calibration Tuner",
        "6. Run All Tests"
    };

    // ===== HARDWARE =====

    /** Limelight vision system (supports USB or HTTP connection) */
    private LimelightVisionAdapter limelight;

    // ===== TEST RESULTS =====

    /** Last test result for display */
    private TestResult lastTestResult;

    // ===== BUTTON EDGE DETECTION =====

    private boolean lastA = false;
    private boolean lastB = false;
    private boolean lastX = false;
    private boolean lastY = false;
    private boolean lastDpadUp = false;
    private boolean lastDpadDown = false;
    private boolean lastDpadLeft = false;
    private boolean lastDpadRight = false;
    private boolean lastLeftBumper = false;
    private boolean lastRightBumper = false;

    // ===== TEST EXECUTION STATE =====

    /** Flag indicating test is currently running */
    private boolean testRunning = false;

    /** Flag indicating test was cancelled by user */
    private boolean testCancelled = false;

    /** Current progress value for long-running tests */
    private int testProgressCurrent = 0;

    /** Total progress value for long-running tests */
    private int testProgressTotal = 0;

    // ===== HELPER CLASS: TEST RESULT =====

    /**
     * Container for test results
     */
    private static class TestResult {
        String testName;
        boolean passed;
        List<String> outputLines;

        TestResult(String testName) {
            this.testName = testName;
            this.passed = false;
            this.outputLines = new ArrayList<>();
        }

        void addLine(String line) {
            outputLines.add(line);
        }

        void setPass(boolean pass) {
            this.passed = pass;
        }
    }

    // ===== MAIN OPMODE METHODS =====

    @Override
    public void runOpMode() {
        // Initialize state
        currentState = UIState.MAIN_MENU;
        selectedTestIndex = 0;
        targetDistance = 48.0;

        // Initialize Limelight hardware - tries USB first, then HTTP
        telemetry.addData("Status", "Connecting to Limelight...");
        telemetry.addLine("Trying USB (HardwareMap)...");
        telemetry.update();

        try {
            limelight = new LimelightVisionAdapter(hardwareMap, "limelight", telemetry);

            // Show connection mode
            String connectionMode = "";
            switch (limelight.getConnectionMode()) {
                case HARDWARE_MAP:
                    connectionMode = "USB (Robot Controller)";
                    break;
                case HTTP_NETWORK:
                    connectionMode = "HTTP (limelight.local:5807)";
                    break;
                case DISCONNECTED:
                    connectionMode = "DISCONNECTED";
                    break;
            }

            telemetry.clear();
            telemetry.addData("Status", "\u2713 Limelight connected");
            telemetry.addData("Mode", connectionMode);
            telemetry.update();

        } catch (IllegalArgumentException e) {
            // Neither USB nor HTTP connection available
            currentState = UIState.ERROR_SCREEN;
            lastTestResult = new TestResult("Connection Error");
            lastTestResult.addLine("Limelight not accessible");
            lastTestResult.addLine("");
            lastTestResult.addLine("Tried:");
            lastTestResult.addLine("  1. USB/HardwareMap - FAILED");
            lastTestResult.addLine("  2. HTTP Network - FAILED");
            lastTestResult.addLine("");
            lastTestResult.addLine("For USB mode:");
            lastTestResult.addLine("- Check Robot Configuration");
            lastTestResult.addLine("- Device named 'limelight'");
            lastTestResult.addLine("- USB connected to Robot Controller");
            lastTestResult.addLine("");
            lastTestResult.addLine("For HTTP mode:");
            lastTestResult.addLine("- Limelight at limelight.local:5807");
            lastTestResult.addLine("- Network accessible from Robot Controller");

            telemetry.clear();
            telemetry.addData("Error", "Limelight not found");
            telemetry.addLine("Tried USB and HTTP - both failed");
            telemetry.addLine("See error screen after START");
            telemetry.update();
        } catch (Exception e) {
            // Other initialization errors
            currentState = UIState.ERROR_SCREEN;
            lastTestResult = new TestResult("Initialization Error");
            lastTestResult.addLine("Failed to initialize Limelight");
            lastTestResult.addLine("");
            lastTestResult.addLine("Error: " + e.getMessage());
            lastTestResult.addLine("");
            lastTestResult.addLine("Check hardware connections");
            lastTestResult.addLine("and Robot Controller logs");

            telemetry.clear();
            telemetry.addData("Error", "Init failed: " + e.getMessage());
            telemetry.update();
        }

        waitForStart();

        // Main loop
        while (opModeIsActive()) {
            updateButtonStates();

            switch (currentState) {
                case MAIN_MENU:
                    handleMainMenu();
                    displayMainMenu();
                    break;

                case ALLIANCE_SELECT:
                    handleAllianceSelect();
                    displayAllianceSelect();
                    break;

                case DISTANCE_INPUT:
                    handleDistanceInput();
                    displayDistanceInput();
                    break;

                case TEST_RUNNING:
                    handleTestRunning();
                    // Display handled by individual tests
                    break;

                case TEST_RESULTS:
                    handleTestResults();
                    displayTestResults();
                    break;

                case ERROR_SCREEN:
                    handleErrorScreen();
                    displayErrorScreen();
                    break;
            }

            sleep(50);  // 20 Hz update rate
        }

        // Cleanup
        if (limelight != null) {
            limelight.stop();
        }
    }

    // ===== BUTTON STATE MANAGEMENT =====

    /**
     * Updates button state tracking for edge detection
     */
    private void updateButtonStates() {
        lastA = gamepad1.a;
        lastB = gamepad1.b;
        lastX = gamepad1.x;
        lastY = gamepad1.y;
        lastDpadUp = gamepad1.dpad_up;
        lastDpadDown = gamepad1.dpad_down;
        lastDpadLeft = gamepad1.dpad_left;
        lastDpadRight = gamepad1.dpad_right;
        lastLeftBumper = gamepad1.left_bumper;
        lastRightBumper = gamepad1.right_bumper;
    }

    /**
     * Detects rising edge (button just pressed, not held)
     */
    private boolean buttonPressed(boolean current, boolean last) {
        return current && !last;
    }

    // ===== STATE HANDLERS =====

    /**
     * Handle input in main menu state
     */
    private void handleMainMenu() {
        // DPAD UP: Navigate up in menu
        if (buttonPressed(gamepad1.dpad_up, lastDpadUp)) {
            selectedTestIndex--;
            if (selectedTestIndex < 0) {
                selectedTestIndex = TEST_NAMES.length - 1;  // Wrap to bottom
            }
        }

        // DPAD DOWN: Navigate down in menu
        if (buttonPressed(gamepad1.dpad_down, lastDpadDown)) {
            selectedTestIndex++;
            if (selectedTestIndex >= TEST_NAMES.length) {
                selectedTestIndex = 0;  // Wrap to top
            }
        }

        // A button: Select test
        if (buttonPressed(gamepad1.a, lastA)) {
            // Determine next state based on selected test
            switch (selectedTestIndex) {
                case 0:  // Test 1: Distance Calculation
                case 1:  // Test 2: Detection Reliability
                case 5:  // Test 6: Run All Tests
                    // These tests need alliance selection
                    currentState = UIState.ALLIANCE_SELECT;
                    break;

                case 2:  // Test 3: Pipeline Switching
                case 3:  // Test 4: Center Tag Sequences
                    // These tests run immediately
                    currentState = UIState.TEST_RUNNING;
                    startTest(selectedTestIndex);
                    break;

                case 4:  // Test 5: Calibration Tuner
                    // Needs alliance for test reading
                    currentState = UIState.ALLIANCE_SELECT;
                    break;
            }
        }

        // B button: Exit OpMode (with confirmation via holding)
        if (gamepad1.b) {
            requestOpModeStop();
        }

        // Y button: Show status info
        if (buttonPressed(gamepad1.y, lastY)) {
            // Future enhancement: show connection status and calibration
        }
    }

    /**
     * Handle input in alliance selection state
     */
    private void handleAllianceSelect() {
        // LEFT BUMPER: Select Blue Alliance
        if (buttonPressed(gamepad1.left_bumper, lastLeftBumper)) {
            isRedAlliance = false;
        }

        // RIGHT BUMPER: Select Red Alliance
        if (buttonPressed(gamepad1.right_bumper, lastRightBumper)) {
            isRedAlliance = true;
        }

        // A button: Confirm selection
        if (buttonPressed(gamepad1.a, lastA)) {
            // Determine next state based on selected test
            if (selectedTestIndex == 0) {
                // Test 1: Go to distance input
                currentState = UIState.DISTANCE_INPUT;
            } else {
                // All other tests: Start immediately
                currentState = UIState.TEST_RUNNING;
                startTest(selectedTestIndex);
            }
        }

        // B button: Cancel and return to menu
        if (buttonPressed(gamepad1.b, lastB)) {
            currentState = UIState.MAIN_MENU;
        }
    }

    /**
     * Handle input in distance input state
     */
    private void handleDistanceInput() {
        // DPAD UP: Increment by 1 inch
        if (buttonPressed(gamepad1.dpad_up, lastDpadUp)) {
            targetDistance += 1.0;
        }

        // DPAD DOWN: Decrement by 1 inch
        if (buttonPressed(gamepad1.dpad_down, lastDpadDown)) {
            targetDistance -= 1.0;
        }

        // LEFT BUMPER: Decrement by 10 inches
        if (buttonPressed(gamepad1.left_bumper, lastLeftBumper)) {
            targetDistance -= 10.0;
        }

        // RIGHT BUMPER: Increment by 10 inches
        if (buttonPressed(gamepad1.right_bumper, lastRightBumper)) {
            targetDistance += 10.0;
        }

        // Clamp to valid range (12.0 - 120.0 inches)
        if (targetDistance < 12.0) {
            targetDistance = 12.0;
        }
        if (targetDistance > 120.0) {
            targetDistance = 120.0;
        }

        // A button: Confirm distance and run test
        if (buttonPressed(gamepad1.a, lastA)) {
            currentState = UIState.TEST_RUNNING;
            startTest(selectedTestIndex);
        }

        // B button: Cancel and return to menu
        if (buttonPressed(gamepad1.b, lastB)) {
            currentState = UIState.MAIN_MENU;
            targetDistance = 48.0;  // Reset to default
        }
    }

    private void handleTestRunning() {
        // Usually empty - tests manage themselves
    }

    /**
     * Handle input in test results state
     */
    private void handleTestResults() {
        // A or B button: Return to main menu
        if (buttonPressed(gamepad1.a, lastA) || buttonPressed(gamepad1.b, lastB)) {
            currentState = UIState.MAIN_MENU;
        }

        // Future enhancement: DPAD up/down for scrolling long results
    }

    /**
     * Handle input in error screen state
     */
    private void handleErrorScreen() {
        // B button: Return to main menu (even though tests won't work)
        if (buttonPressed(gamepad1.b, lastB)) {
            currentState = UIState.MAIN_MENU;
        }

        // Y button: Future enhancement - show detailed error info
        if (buttonPressed(gamepad1.y, lastY)) {
            // Could show stack trace or additional debugging info
        }
    }

    // ===== DISPLAY METHODS =====

    /**
     * Display main menu with test list and Limelight status
     */
    private void displayMainMenu() {
        telemetry.clear();
        telemetry.addLine("=== LIMELIGHT VISION TESTS ===");
        telemetry.addLine();

        // Display each test with cursor
        for (int i = 0; i < TEST_NAMES.length; i++) {
            String cursor = (i == selectedTestIndex) ? "> " : "  ";
            telemetry.addLine(cursor + TEST_NAMES[i]);
        }

        telemetry.addLine();

        // Display Limelight status and connection mode
        if (limelight != null) {
            try {
                int currentPipeline = limelight.getCurrentPipeline();
                String mode = "";
                switch (limelight.getConnectionMode()) {
                    case HARDWARE_MAP:
                        mode = "USB";
                        break;
                    case HTTP_NETWORK:
                        mode = "HTTP";
                        break;
                    case DISCONNECTED:
                        mode = "ERROR";
                        break;
                }
                telemetry.addLine("Pipeline " + currentPipeline + " | " + mode + " Mode");
            } catch (Exception e) {
                telemetry.addLine("Limelight connected");
            }
        } else {
            telemetry.addLine("Status: ERROR - No Limelight");
        }

        telemetry.addLine();
        telemetry.addLine("DPAD\u2195=Navigate | A=Select | B=Exit");
        telemetry.update();
    }

    /**
     * Display alliance selection screen
     */
    private void displayAllianceSelect() {
        telemetry.clear();
        telemetry.addLine("=== ALLIANCE SELECTION ===");
        telemetry.addLine();
        telemetry.addLine("Select alliance color for test:");
        telemetry.addLine();

        // Show selection with highlighting
        String blueMarker = isRedAlliance ? "  " : "> ";
        String redMarker = isRedAlliance ? "> " : "  ";

        telemetry.addLine(blueMarker + "[BLUE] Alliance (Tag 20)");
        telemetry.addLine(redMarker + "[RED]  Alliance (Tag 24)");

        telemetry.addLine();
        telemetry.addLine("L1=Blue | R1=Red | A=Confirm | B=Back");
        telemetry.update();
    }

    /**
     * Display distance input screen
     */
    private void displayDistanceInput() {
        telemetry.clear();
        telemetry.addLine("=== DISTANCE INPUT ===");
        telemetry.addLine();
        telemetry.addLine("Enter actual measured distance:");
        telemetry.addLine();

        // Display current value with formatting
        telemetry.addLine("    >>> " + formatDouble(targetDistance, 1) + " inches <<<");

        telemetry.addLine();
        telemetry.addLine("Range: 12.0\" - 120.0\"");
        telemetry.addLine();
        telemetry.addLine("DPAD\u2195=\u00b11\" | L1/R1=\u00b110\" | A=OK | B=Cancel");
        telemetry.update();
    }

    /**
     * Display test results with PASS/FAIL verdict
     */
    private void displayTestResults() {
        telemetry.clear();
        telemetry.addLine("=== TEST RESULTS ===");
        telemetry.addLine();

        if (lastTestResult != null) {
            // Show test name and verdict
            String verdict = lastTestResult.passed ? "[PASS]" : "[FAIL]";
            telemetry.addLine(lastTestResult.testName + " " + verdict);
            telemetry.addLine();

            // Show result details
            if (lastTestResult.outputLines != null) {
                for (String line : lastTestResult.outputLines) {
                    telemetry.addLine(line);
                }
            }
        } else {
            telemetry.addLine("No test results available");
        }

        telemetry.addLine();
        telemetry.addLine("A or B=Return to Menu");
        telemetry.update();
    }

    /**
     * Display error screen with troubleshooting information
     */
    private void displayErrorScreen() {
        telemetry.clear();
        telemetry.addLine("=== ERROR ===");
        telemetry.addLine();

        // Display error details from lastTestResult
        if (lastTestResult != null && lastTestResult.outputLines != null) {
            for (String line : lastTestResult.outputLines) {
                telemetry.addLine(line);
            }
        } else {
            telemetry.addLine("An unknown error occurred");
            telemetry.addLine("Check Robot Controller logs");
        }

        telemetry.addLine();
        telemetry.addLine("B=Return to Menu");
        telemetry.update();
    }

    private void displayTestProgress(String message) {
        telemetry.clear();
        telemetry.addLine("=== TEST RUNNING ===");
        telemetry.addLine();
        telemetry.addLine(message);
        telemetry.update();
    }

    // ===== TEST EXECUTION METHODS (STUBS) =====

    /**
     * Start test execution based on test index
     */
    private void startTest(int testIndex) {
        switch (testIndex) {
            case 0:
                runTest1_DistanceCalculation();
                break;
            case 1:
                runTest2_DetectionReliability();
                break;
            case 2:
                runTest3_PipelineSwitching();
                break;
            case 3:
                runTest4_CenterTagSequences();
                break;
            case 4:
                runTest5_CalibrationTuner();
                break;
            case 5:
                runTest6_RunAllTests();
                break;
        }
    }

    /**
     * Test 1: Distance Calculation
     * Validates distance formula accuracy by comparing calculated vs actual distance
     */
    private void runTest1_DistanceCalculation() {
        lastTestResult = new TestResult("Test 1: Distance Calculation");

        displayTestProgress("Capturing AprilTag frame...");

        try {
            // Get pillar target based on alliance
            VisionTarget target = limelight.getPillarTarget(isRedAlliance);

            if (!target.isTargetFound()) {
                lastTestResult.addLine("ERROR: No AprilTag detected");
                lastTestResult.addLine("");
                lastTestResult.addLine("Troubleshooting:");
                lastTestResult.addLine("- Ensure tag " + (isRedAlliance ? "24" : "20") + " is visible");
                lastTestResult.addLine("- Check Limelight pipeline 2");
                lastTestResult.addLine("- Verify camera focus and lighting");
                lastTestResult.setPass(false);
                currentState = UIState.TEST_RESULTS;
                return;
            }

            // Extract target data
            double tx = target.getTx();
            double ty = target.getTy();
            double ta = target.getTa();
            double calculatedDistance = target.getDistance();
            double angleToTarget = target.getAngleToTarget();
            int tagId = target.getAprilTagId();

            // Calculate error
            double error = Math.abs(calculatedDistance - targetDistance);
            double errorPercent = (error / targetDistance) * 100.0;

            // Build result output
            lastTestResult.addLine("AprilTag Detected: ID " + tagId);
            lastTestResult.addLine("");
            lastTestResult.addLine("Raw Values:");
            lastTestResult.addLine("  tx: " + formatDouble(tx, 2) + "\u00b0");
            lastTestResult.addLine("  ty: " + formatDouble(ty, 2) + "\u00b0");
            lastTestResult.addLine("  ta: " + formatDouble(ta, 2) + "%");
            lastTestResult.addLine("");
            lastTestResult.addLine("Calculated Distance: " + formatDouble(calculatedDistance, 2) + "\"");
            lastTestResult.addLine("Actual Distance:     " + formatDouble(targetDistance, 2) + "\"");
            lastTestResult.addLine("Error:               " + formatDouble(error, 2) + "\" (" + formatDouble(errorPercent, 1) + "%)");
            lastTestResult.addLine("");
            lastTestResult.addLine("Angle to Target: " + formatDouble(Math.toDegrees(angleToTarget), 2) + "\u00b0");

            // Determine pass/fail (pass if error < 10%)
            boolean passed = errorPercent < 10.0;
            lastTestResult.setPass(passed);

            if (passed) {
                lastTestResult.addLine("");
                lastTestResult.addLine("Distance calculation is accurate!");
            } else {
                lastTestResult.addLine("");
                lastTestResult.addLine("Distance error too high. Check calibration.");
            }

        } catch (Exception e) {
            lastTestResult.addLine("ERROR: " + e.getMessage());
            lastTestResult.setPass(false);
        }

        currentState = UIState.TEST_RESULTS;
    }

    /**
     * Test 2: Detection Reliability
     * Runs 100-frame capture test and calculates detection statistics
     */
    private void runTest2_DetectionReliability() {
        lastTestResult = new TestResult("Test 2: Detection Reliability");

        final int TOTAL_FRAMES = 100;
        final int FRAME_INTERVAL_MS = 50;

        int detectedCount = 0;
        double sumTx = 0.0;
        double sumTy = 0.0;
        double sumDistance = 0.0;
        double minDistance = Double.MAX_VALUE;
        double maxDistance = Double.MIN_VALUE;

        try {
            for (int frame = 1; frame <= TOTAL_FRAMES; frame++) {
                // Check for cancellation
                if (buttonPressed(gamepad1.b, lastB)) {
                    lastTestResult.addLine("Test cancelled by user");
                    lastTestResult.addLine("");
                    lastTestResult.addLine("Frames captured: " + (frame - 1) + "/" + TOTAL_FRAMES);
                    lastTestResult.setPass(false);
                    currentState = UIState.TEST_RESULTS;
                    return;
                }

                // Update progress display every 10 frames
                if (frame % 10 == 0 || frame == 1) {
                    double detectionRate = (detectedCount / (double) (frame - 1)) * 100.0;
                    displayTestProgress("Frame " + frame + "/" + TOTAL_FRAMES + "\n" +
                                      "Detection: " + detectedCount + " (" +
                                      formatDouble(detectionRate, 1) + "%)\n\n" +
                                      "Press B to cancel");
                }

                // Capture frame
                VisionTarget target = limelight.getPillarTarget(isRedAlliance);

                if (target.isTargetFound()) {
                    detectedCount++;
                    double tx = target.getTx();
                    double ty = target.getTy();
                    double distance = target.getDistance();

                    sumTx += tx;
                    sumTy += ty;
                    sumDistance += distance;

                    if (distance < minDistance) minDistance = distance;
                    if (distance > maxDistance) maxDistance = distance;
                }

                // Wait for next frame
                sleep(FRAME_INTERVAL_MS);
                updateButtonStates();  // Update button states for cancellation check
            }

            // Calculate statistics
            double detectionRate = (detectedCount / (double) TOTAL_FRAMES) * 100.0;
            double avgTx = detectedCount > 0 ? sumTx / detectedCount : 0.0;
            double avgTy = detectedCount > 0 ? sumTy / detectedCount : 0.0;
            double avgDistance = detectedCount > 0 ? sumDistance / detectedCount : 0.0;

            // Build results
            lastTestResult.addLine("Frames Captured: " + TOTAL_FRAMES);
            lastTestResult.addLine("Frames Detected: " + detectedCount);
            lastTestResult.addLine("Detection Rate:  " + formatDouble(detectionRate, 1) + "%");
            lastTestResult.addLine("");

            if (detectedCount > 0) {
                lastTestResult.addLine("Average Values:");
                lastTestResult.addLine("  tx: " + formatDouble(avgTx, 2) + "\u00b0");
                lastTestResult.addLine("  ty: " + formatDouble(avgTy, 2) + "\u00b0");
                lastTestResult.addLine("  Distance: " + formatDouble(avgDistance, 2) + "\"");
                lastTestResult.addLine("");
                lastTestResult.addLine("Distance Range:");
                lastTestResult.addLine("  Min: " + formatDouble(minDistance, 2) + "\"");
                lastTestResult.addLine("  Max: " + formatDouble(maxDistance, 2) + "\"");
                lastTestResult.addLine("  Spread: " + formatDouble(maxDistance - minDistance, 2) + "\"");
            } else {
                lastTestResult.addLine("No targets detected during test");
            }

            // Determine pass/fail (≥90% detection rate)
            boolean passed = detectionRate >= 90.0;
            lastTestResult.setPass(passed);

            lastTestResult.addLine("");
            if (passed) {
                lastTestResult.addLine("Detection reliability is excellent!");
            } else {
                lastTestResult.addLine("Detection rate below 90% threshold.");
                lastTestResult.addLine("Check tag visibility and lighting.");
            }

        } catch (Exception e) {
            lastTestResult.addLine("ERROR: " + e.getMessage());
            lastTestResult.setPass(false);
        }

        currentState = UIState.TEST_RESULTS;
    }

    /**
     * Test 3: Pipeline Switching
     * Verifies all 4 pipelines can be switched successfully
     */
    private void runTest3_PipelineSwitching() {
        lastTestResult = new TestResult("Test 3: Pipeline Switching");

        boolean allPassed = true;
        int pipelinesTestedCount = 0;

        try {
            // Test Pipeline 0: Purple ball detection
            displayTestProgress("Testing Pipeline 0 (Purple balls)...");
            limelight.switchToBallPipeline(BallColor.PURPLE);
            sleep(500);  // Stabilization delay

            int currentPipeline = limelight.getCurrentPipeline();
            boolean pipeline0Pass = (currentPipeline == LimelightVision.PIPELINE_PURPLE);
            pipelinesTestedCount++;

            lastTestResult.addLine("Pipeline 0 (Purple): " + (pipeline0Pass ? "PASS" : "FAIL"));
            if (!pipeline0Pass) {
                lastTestResult.addLine("  Expected: 0, Got: " + currentPipeline);
                allPassed = false;
            }

            // Test Pipeline 1: Green ball detection
            displayTestProgress("Testing Pipeline 1 (Green balls)...");
            limelight.switchToBallPipeline(BallColor.GREEN);
            sleep(500);

            currentPipeline = limelight.getCurrentPipeline();
            boolean pipeline1Pass = (currentPipeline == LimelightVision.PIPELINE_GREEN);
            pipelinesTestedCount++;

            lastTestResult.addLine("Pipeline 1 (Green):  " + (pipeline1Pass ? "PASS" : "FAIL"));
            if (!pipeline1Pass) {
                lastTestResult.addLine("  Expected: 1, Got: " + currentPipeline);
                allPassed = false;
            }

            // Test Pipeline 2: Pillar AprilTag detection
            displayTestProgress("Testing Pipeline 2 (Pillar tags)...");
            limelight.switchToPillarTagPipeline();
            sleep(500);

            currentPipeline = limelight.getCurrentPipeline();
            boolean pipeline2Pass = (currentPipeline == LimelightVision.PIPELINE_PILLAR_TAGS);
            pipelinesTestedCount++;

            lastTestResult.addLine("Pipeline 2 (Pillar): " + (pipeline2Pass ? "PASS" : "FAIL"));
            if (!pipeline2Pass) {
                lastTestResult.addLine("  Expected: 2, Got: " + currentPipeline);
                allPassed = false;
            }

            // Test Pipeline 3: Center AprilTag detection
            displayTestProgress("Testing Pipeline 3 (Center tags)...");
            limelight.switchToCenterTagPipeline();
            sleep(500);

            currentPipeline = limelight.getCurrentPipeline();
            boolean pipeline3Pass = (currentPipeline == LimelightVision.PIPELINE_CENTER_TAGS);
            pipelinesTestedCount++;

            lastTestResult.addLine("Pipeline 3 (Center): " + (pipeline3Pass ? "PASS" : "FAIL"));
            if (!pipeline3Pass) {
                lastTestResult.addLine("  Expected: 3, Got: " + currentPipeline);
                allPassed = false;
            }

            // Return to default pipeline (Pillar tags)
            displayTestProgress("Returning to default pipeline...");
            limelight.switchToPillarTagPipeline();
            sleep(500);

            // Summary
            lastTestResult.addLine("");
            lastTestResult.addLine("Pipelines Tested: " + pipelinesTestedCount + "/4");

            if (allPassed) {
                lastTestResult.addLine("All pipeline switches successful!");
            } else {
                lastTestResult.addLine("Some pipeline switches failed.");
                lastTestResult.addLine("Check Limelight web interface configuration.");
            }

            lastTestResult.setPass(allPassed);

        } catch (Exception e) {
            lastTestResult.addLine("");
            lastTestResult.addLine("ERROR: " + e.getMessage());
            lastTestResult.setPass(false);
        }

        currentState = UIState.TEST_RESULTS;
    }

    /**
     * Test 4: Center Tag Sequences
     * Interactive test for reading center AprilTags and validating ball sequences
     */
    private void runTest4_CenterTagSequences() {
        lastTestResult = new TestResult("Test 4: Center Tag Sequences");

        // Switch to center tag pipeline (workaround for bug on line 244)
        displayTestProgress("Switching to center tag pipeline...");
        limelight.switchToCenterTagPipeline();
        sleep(500);  // Stabilization delay

        List<String> captures = new ArrayList<>();
        int captureCount = 0;
        boolean finished = false;

        try {
            // Interactive capture loop
            while (!finished && opModeIsActive()) {
                // Display instructions
                telemetry.clear();
                telemetry.addLine("=== TEST 4: CENTER TAGS ===");
                telemetry.addLine();
                telemetry.addLine("Show tags 21, 22, or 23 to camera");
                telemetry.addLine();
                telemetry.addLine("Captures: " + captureCount);
                telemetry.addLine();
                telemetry.addLine("A=Capture Reading | B=Finish Test");
                telemetry.update();

                // Check for button presses
                if (buttonPressed(gamepad1.a, lastA)) {
                    // Capture reading
                    displayTestProgress("Capturing tag...");

                    List<BallColor> sequence = limelight.readCenterAprilTag();

                    if (sequence != null && !sequence.isEmpty()) {
                        captureCount++;
                        String tagId = determineTagId(sequence);
                        String seqStr = formatSequence(sequence);

                        String captureResult = "Capture " + captureCount + ": Tag " + tagId + " → " + seqStr;
                        captures.add(captureResult);

                        // Validate sequence
                        boolean validSequence = !tagId.equals("UNKNOWN");
                        if (!validSequence) {
                            captures.add("  WARNING: Unexpected sequence!");
                        }

                    } else {
                        captures.add("Capture " + captureCount + ": No tag detected");
                    }

                } else if (buttonPressed(gamepad1.b, lastB)) {
                    // Finish test
                    finished = true;
                }

                updateButtonStates();
                sleep(50);
            }

            // Build results
            if (captureCount == 0) {
                lastTestResult.addLine("No captures taken");
                lastTestResult.addLine("");
                lastTestResult.addLine("Use A button to capture center tags");
                lastTestResult.setPass(false);
            } else {
                lastTestResult.addLine("Total Captures: " + captureCount);
                lastTestResult.addLine("");

                for (String capture : captures) {
                    lastTestResult.addLine(capture);
                }

                lastTestResult.addLine("");
                lastTestResult.addLine("Expected Sequences:");
                lastTestResult.addLine("  Tag 21: [G, P, P]");
                lastTestResult.addLine("  Tag 22: [P, G, P]");
                lastTestResult.addLine("  Tag 23: [P, P, G]");

                // All captures with valid tags = PASS
                boolean allValid = true;
                for (String capture : captures) {
                    if (capture.contains("UNKNOWN") || capture.contains("No tag")) {
                        allValid = false;
                        break;
                    }
                }

                lastTestResult.setPass(allValid && captureCount > 0);
            }

        } catch (Exception e) {
            lastTestResult.addLine("ERROR: " + e.getMessage());
            lastTestResult.setPass(false);
        } finally {
            // Return to pillar pipeline
            limelight.switchToPillarTagPipeline();
        }

        currentState = UIState.TEST_RESULTS;
    }

    /**
     * Test 5: Calibration Tuner
     * Displays current calibration constants and takes a test reading
     */
    private void runTest5_CalibrationTuner() {
        lastTestResult = new TestResult("Test 5: Calibration Tuner");

        try {
            // Display current calibration constants
            lastTestResult.addLine("Current Calibration Constants:");
            lastTestResult.addLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            lastTestResult.addLine("Limelight Height: " + formatDouble(LimelightVision.LIMELIGHT_HEIGHT_INCHES, 1) + "\"");
            lastTestResult.addLine("Limelight Angle:  " + formatDouble(LimelightVision.LIMELIGHT_ANGLE_DEGREES, 1) + "\u00b0");
            lastTestResult.addLine("AprilTag Height:  " + formatDouble(LimelightVision.APRILTAG_HEIGHT_INCHES, 1) + "\"");
            lastTestResult.addLine("");

            // Take a test reading
            displayTestProgress("Taking test reading...");

            VisionTarget target = limelight.getPillarTarget(isRedAlliance);

            if (target.isTargetFound()) {
                double tx = target.getTx();
                double ty = target.getTy();
                double ta = target.getTa();
                double distance = target.getDistance();
                int tagId = target.getAprilTagId();

                lastTestResult.addLine("Test Reading (Tag " + tagId + "):");
                lastTestResult.addLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                lastTestResult.addLine("tx:       " + formatDouble(tx, 2) + "\u00b0");
                lastTestResult.addLine("ty:       " + formatDouble(ty, 2) + "\u00b0");
                lastTestResult.addLine("ta:       " + formatDouble(ta, 2) + "%");
                lastTestResult.addLine("Distance: " + formatDouble(distance, 2) + "\"");
                lastTestResult.addLine("");
                lastTestResult.addLine("Measure the actual distance and");
                lastTestResult.addLine("compare with calculated value.");
                lastTestResult.addLine("");
                lastTestResult.addLine("If error > 10%, update calibration");
                lastTestResult.addLine("constants in LimelightVision.java");
                lastTestResult.addLine("(lines 56-63)");

                lastTestResult.setPass(true);  // Informational test, always "pass"

            } else {
                lastTestResult.addLine("Test Reading:");
                lastTestResult.addLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                lastTestResult.addLine("No AprilTag detected");
                lastTestResult.addLine("");
                lastTestResult.addLine("Point camera at tag " + (isRedAlliance ? "24" : "20"));
                lastTestResult.addLine("and ensure good lighting.");

                lastTestResult.setPass(true);  // Informational test
            }

        } catch (Exception e) {
            lastTestResult.addLine("ERROR: " + e.getMessage());
            lastTestResult.setPass(false);
        }

        currentState = UIState.TEST_RESULTS;
    }

    /**
     * Test 6: Run All Tests
     * Runs Test 3 (Pipeline Switching) and Test 2 (Detection Reliability) sequentially
     */
    private void runTest6_RunAllTests() {
        TestResult combinedResult = new TestResult("Test 6: Run All Tests");

        boolean test3Passed = false;
        boolean test2Passed = false;

        try {
            // ===== RUN TEST 3: PIPELINE SWITCHING =====
            displayTestProgress("Running Test 3: Pipeline Switching...");

            TestResult test3Result = new TestResult("Test 3: Pipeline Switching");
            boolean allPassed = true;
            int pipelinesTestedCount = 0;

            // Test all 4 pipelines (copied from Test 3)
            limelight.switchToBallPipeline(BallColor.PURPLE);
            sleep(500);
            boolean pipeline0Pass = (limelight.getCurrentPipeline() == LimelightVision.PIPELINE_PURPLE);
            pipelinesTestedCount++;
            if (!pipeline0Pass) allPassed = false;

            limelight.switchToBallPipeline(BallColor.GREEN);
            sleep(500);
            boolean pipeline1Pass = (limelight.getCurrentPipeline() == LimelightVision.PIPELINE_GREEN);
            pipelinesTestedCount++;
            if (!pipeline1Pass) allPassed = false;

            limelight.switchToPillarTagPipeline();
            sleep(500);
            boolean pipeline2Pass = (limelight.getCurrentPipeline() == LimelightVision.PIPELINE_PILLAR_TAGS);
            pipelinesTestedCount++;
            if (!pipeline2Pass) allPassed = false;

            limelight.switchToCenterTagPipeline();
            sleep(500);
            boolean pipeline3Pass = (limelight.getCurrentPipeline() == LimelightVision.PIPELINE_CENTER_TAGS);
            pipelinesTestedCount++;
            if (!pipeline3Pass) allPassed = false;

            limelight.switchToPillarTagPipeline();  // Return to default
            sleep(500);

            test3Passed = allPassed;

            combinedResult.addLine("━━━ TEST 3: PIPELINE SWITCHING ━━━");
            combinedResult.addLine("Result: " + (test3Passed ? "PASS" : "FAIL"));
            combinedResult.addLine("Pipelines tested: " + pipelinesTestedCount + "/4");
            combinedResult.addLine("");

            // ===== RUN TEST 2: DETECTION RELIABILITY =====
            displayTestProgress("Running Test 2: Detection Reliability...");

            final int TOTAL_FRAMES = 100;
            final int FRAME_INTERVAL_MS = 50;

            int detectedCount = 0;
            double sumDistance = 0.0;

            for (int frame = 1; frame <= TOTAL_FRAMES; frame++) {
                if (frame % 20 == 0) {
                    displayTestProgress("Test 2: Frame " + frame + "/" + TOTAL_FRAMES);
                }

                VisionTarget target = limelight.getPillarTarget(isRedAlliance);
                if (target.isTargetFound()) {
                    detectedCount++;
                    sumDistance += target.getDistance();
                }

                sleep(FRAME_INTERVAL_MS);
            }

            double detectionRate = (detectedCount / (double) TOTAL_FRAMES) * 100.0;
            double avgDistance = detectedCount > 0 ? sumDistance / detectedCount : 0.0;

            test2Passed = (detectionRate >= 90.0);

            combinedResult.addLine("━━━ TEST 2: DETECTION RELIABILITY ━━━");
            combinedResult.addLine("Result: " + (test2Passed ? "PASS" : "FAIL"));
            combinedResult.addLine("Detection Rate: " + formatDouble(detectionRate, 1) + "%");
            combinedResult.addLine("Frames Detected: " + detectedCount + "/" + TOTAL_FRAMES);
            if (detectedCount > 0) {
                combinedResult.addLine("Avg Distance: " + formatDouble(avgDistance, 2) + "\"");
            }
            combinedResult.addLine("");

            // ===== OVERALL RESULT =====
            combinedResult.addLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            combinedResult.addLine("Overall: " + (test3Passed && test2Passed ? "PASS" : "FAIL"));
            combinedResult.addLine("Test 3: " + (test3Passed ? "PASS" : "FAIL"));
            combinedResult.addLine("Test 2: " + (test2Passed ? "PASS" : "FAIL"));

            combinedResult.setPass(test3Passed && test2Passed);

        } catch (Exception e) {
            combinedResult.addLine("ERROR: " + e.getMessage());
            combinedResult.setPass(false);
        }

        lastTestResult = combinedResult;
        currentState = UIState.TEST_RESULTS;
    }

    // ===== UTILITY METHODS (STUBS) =====

    /**
     * Formats a double value with specified decimal places
     */
    private String formatDouble(double value, int decimals) {
        String format = "%." + decimals + "f";
        return String.format(format, value);
    }

    /**
     * Determines AprilTag ID from ball color sequence
     */
    private String determineTagId(List<BallColor> sequence) {
        if (sequence.equals(List.of(BallColor.GREEN, BallColor.PURPLE, BallColor.PURPLE))) {
            return "21";
        } else if (sequence.equals(List.of(BallColor.PURPLE, BallColor.GREEN, BallColor.PURPLE))) {
            return "22";
        } else if (sequence.equals(List.of(BallColor.PURPLE, BallColor.PURPLE, BallColor.GREEN))) {
            return "23";
        }
        return "UNKNOWN";
    }

    /**
     * Formats ball color sequence for display
     */
    private String formatSequence(List<BallColor> sequence) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < sequence.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(sequence.get(i).toString().charAt(0));  // "P" or "G"
        }
        sb.append("]");
        return sb.toString();
    }
}
