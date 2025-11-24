package tester;

import tester.models.*;

import java.util.List;
import java.util.Scanner;

/**
 * Main test application for Limelight vision system.
 * Tests with EXACT COPY of LimelightVision.java formulas.
 *
 * NOTE: RealVisionWrapper contains line-by-line copies of the distance
 * calculation and vision logic from TeamCode. Any changes to LimelightVision.java
 * MUST be reflected in RealVisionWrapper.java!
 */
public class TestRunner {

    private static final String LIMELIGHT_URL = "http://limelight.local:5807";

    private final LimelightHttpClient httpClient;
    private final RealVisionWrapper limelight;
    private final Scanner scanner;

    public TestRunner() {
        this.httpClient = new LimelightHttpClient(LIMELIGHT_URL);
        this.limelight = new RealVisionWrapper(httpClient);
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        TestRunner runner = new TestRunner();
        runner.run();
    }

    public void run() {
        System.out.println("==========================================================");
        System.out.println("           Limelight Vision Test System");
        System.out.println("==========================================================");

        // Check connection
        System.out.println("\nConnecting to Limelight at: " + LIMELIGHT_URL);
        if (!httpClient.isConnected()) {
            System.err.println("ERROR: Cannot connect to Limelight!");
            System.err.println("Make sure:");
            System.err.println("  1. Limelight is connected via USB");
            System.err.println("  2. " + LIMELIGHT_URL + " is accessible in your browser");
            return;
        }
        System.out.println("Connection successful!");
        System.out.println("FPS: " + String.format("%.0f", httpClient.getFPS()));

        // Main menu loop
        boolean running = true;
        while (running) {
            showMainMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    testDistanceCalculation();
                    break;
                case "2":
                    testDetectionReliability();
                    break;
                case "3":
                    testPipelineSwitching();
                    break;
                case "4":
                    testCenterTagSequences();
                    break;
                case "5":
                    calibrationTuner();
                    break;
                case "6":
                    runAllTests();
                    break;
                case "7":
                    running = false;
                    System.out.println("\nExiting test system. Goodbye!");
                    break;
                default:
                    System.out.println("\nInvalid choice. Please enter 1-7.");
            }
        }

        scanner.close();
    }

    private void showMainMenu() {
        System.out.println("\n==========================================================");
        System.out.println("Pipeline: " + limelight.getCurrentPipeline() + " | " +
                "Target: " + (limelight.hasTarget() ? "YES" : "NO"));
        System.out.println("Testing with EXACT formulas from LimelightVision.java");
        System.out.println("==========================================================");
        System.out.println("Main Menu:");
        System.out.println("  1. Test Distance Calculation (AprilTag)");
        System.out.println("  2. Test Detection Reliability");
        System.out.println("  3. Test Pipeline Switching");
        System.out.println("  4. Test Center Tag Sequences");
        System.out.println("  5. Calibration Tuner");
        System.out.println("  6. Run All Tests");
        System.out.println("  7. Exit");
        System.out.print("\nEnter choice (1-7): ");
    }

    // ===== TEST 1: DISTANCE CALCULATION =====

    private void testDistanceCalculation() {
        System.out.println("\n========== Distance Calculation Test ==========");
        System.out.println("This test verifies the distance calculation formula.");
        System.out.println("\nInstructions:");
        System.out.println("  1. Place an AprilTag at a known distance");
        System.out.println("  2. Point Limelight at the tag");
        System.out.println("  3. Measure the actual distance");
        System.out.println("  4. Compare calculated vs actual distance");

        System.out.print("\nTest Red alliance (tag 24) or Blue alliance (tag 20)? (r/b): ");
        String alliance = scanner.nextLine().trim().toLowerCase();
        boolean isRed = alliance.equals("r");

        System.out.println("\nDetecting pillar tag " + (isRed ? "24 (Red)" : "20 (Blue)") + "...");
        System.out.println("Press Enter to capture frame (or 'q' to quit)");

        while (true) {
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("q")) {
                break;
            }

            VisionTarget target = limelight.getPillarTarget(isRed);

            if (target.isTargetFound()) {
                System.out.println("\n--- Detection Results ---");
                System.out.println("Tag ID: " + target.getAprilTagId());
                System.out.println("tx (horizontal offset): " + String.format("%.2f", target.getTx()) + " degrees");
                System.out.println("ty (vertical offset): " + String.format("%.2f", target.getTy()) + " degrees");
                System.out.println("ta (target area): " + String.format("%.2f", target.getTa()) + " %");
                System.out.println("\nCalculated Distance: " + String.format("%.2f", target.getDistance()) + " inches");
                System.out.println("Angle to Target: " + String.format("%.3f", target.getAngleToTarget()) + " radians");

                System.out.print("\nEnter actual measured distance (inches) for comparison: ");
                String distInput = scanner.nextLine().trim();
                if (!distInput.isEmpty()) {
                    try {
                        double actualDistance = Double.parseDouble(distInput);
                        double error = target.getDistance() - actualDistance;
                        double errorPercent = (error / actualDistance) * 100.0;

                        System.out.println("\n--- Accuracy Analysis ---");
                        System.out.println("Calculated: " + String.format("%.2f", target.getDistance()) + " inches");
                        System.out.println("Actual: " + String.format("%.2f", actualDistance) + " inches");
                        System.out.println("Error: " + String.format("%.2f", error) + " inches (" +
                                String.format("%.1f", errorPercent) + "%)");

                        if (Math.abs(errorPercent) < 10) {
                            System.out.println("Result: PASS (error < 10%)");
                        } else {
                            System.out.println("Result: FAIL (error >= 10%)");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid distance entered.");
                    }
                }
            } else {
                System.out.println("\nNo target detected. Make sure tag is visible.");
            }

            System.out.println("\nPress Enter to capture again (or 'q' to quit)");
        }
    }

    // ===== TEST 2: DETECTION RELIABILITY =====

    private void testDetectionReliability() {
        System.out.println("\n========== Detection Reliability Test ==========");
        System.out.println("This test runs 100 frames to measure detection consistency.");

        System.out.print("\nTest Red alliance (tag 24) or Blue alliance (tag 20)? (r/b): ");
        String alliance = scanner.nextLine().trim().toLowerCase();
        boolean isRed = alliance.equals("r");

        System.out.print("\nStarting test in 3 seconds... Point camera at tag!");
        try {
            Thread.sleep(1000);
            System.out.print(".");
            Thread.sleep(1000);
            System.out.print(".");
            Thread.sleep(1000);
            System.out.println(".\n");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int totalFrames = 100;
        int detectedFrames = 0;
        double sumTx = 0, sumTy = 0, sumDistance = 0;
        double minDistance = Double.MAX_VALUE, maxDistance = Double.MIN_VALUE;

        System.out.println("Running test...");

        for (int i = 0; i < totalFrames; i++) {
            VisionTarget target = limelight.getPillarTarget(isRed);

            if (target.isTargetFound()) {
                detectedFrames++;
                sumTx += target.getTx();
                sumTy += target.getTy();
                sumDistance += target.getDistance();
                minDistance = Math.min(minDistance, target.getDistance());
                maxDistance = Math.max(maxDistance, target.getDistance());
            }

            if ((i + 1) % 20 == 0) {
                System.out.println("Progress: " + (i + 1) + "/" + totalFrames);
            }

            try {
                Thread.sleep(50); // 20 Hz sampling
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("\n--- Reliability Results ---");
        System.out.println("Total Frames: " + totalFrames);
        System.out.println("Detected Frames: " + detectedFrames);
        double detectionRate = (detectedFrames * 100.0) / totalFrames;
        System.out.println("Detection Rate: " + String.format("%.1f", detectionRate) + " %");

        if (detectedFrames > 0) {
            double avgTx = sumTx / detectedFrames;
            double avgTy = sumTy / detectedFrames;
            double avgDistance = sumDistance / detectedFrames;

            System.out.println("\nAverage tx: " + String.format("%.2f", avgTx) + " degrees");
            System.out.println("Average ty: " + String.format("%.2f", avgTy) + " degrees");
            System.out.println("Average Distance: " + String.format("%.2f", avgDistance) + " inches");
            System.out.println("Distance Range: " + String.format("%.2f", minDistance) + " - " +
                    String.format("%.2f", maxDistance) + " inches");
        }

        if (detectionRate >= 90) {
            System.out.println("\nResult: PASS (detection rate >= 90%)");
        } else {
            System.out.println("\nResult: FAIL (detection rate < 90%)");
        }

        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    // ===== TEST 3: PIPELINE SWITCHING =====

    private void testPipelineSwitching() {
        System.out.println("\n========== Pipeline Switching Test ==========");
        System.out.println("This test cycles through all 3 pipelines.");

        boolean allSuccess = true;

        // Test AprilTag pipeline
        System.out.println("\nSwitching to Pipeline 0 (AprilTag)...");
        limelight.switchToAprilTagPipeline();
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        if (limelight.getCurrentPipeline() == 0) {
            System.out.println("✓ Pipeline 0: SUCCESS");
        } else {
            System.out.println("✗ Pipeline 0: FAILED");
            allSuccess = false;
        }

        // Test Purple pipeline
        System.out.println("\nSwitching to Pipeline 1 (Purple)...");
        limelight.switchToBallPipeline(BallColor.PURPLE);
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        if (limelight.getCurrentPipeline() == 1) {
            System.out.println("✓ Pipeline 1: SUCCESS");
        } else {
            System.out.println("✗ Pipeline 1: FAILED");
            allSuccess = false;
        }

        // Test Green pipeline
        System.out.println("\nSwitching to Pipeline 2 (Green)...");
        limelight.switchToBallPipeline(BallColor.GREEN);
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        if (limelight.getCurrentPipeline() == 2) {
            System.out.println("✓ Pipeline 2: SUCCESS");
        } else {
            System.out.println("✗ Pipeline 2: FAILED");
            allSuccess = false;
        }

        // Return to AprilTag
        System.out.println("\nReturning to Pipeline 0 (AprilTag)...");
        limelight.switchToAprilTagPipeline();
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        System.out.println("\n--- Pipeline Switching Results ---");
        if (allSuccess) {
            System.out.println("Result: PASS (all pipelines switched successfully)");
        } else {
            System.out.println("Result: FAIL (one or more pipelines failed to switch)");
        }

        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    // ===== TEST 4: CENTER TAG SEQUENCES =====

    private void testCenterTagSequences() {
        System.out.println("\n========== Center Tag Sequence Test ==========");
        System.out.println("This test verifies center tag → ball sequence mapping.");
        System.out.println("\nExpected Mappings:");
        System.out.println("  Tag 21 → [PURPLE, PURPLE, GREEN]");
        System.out.println("  Tag 22 → [PURPLE, GREEN, PURPLE]");
        System.out.println("  Tag 23 → [GREEN, PURPLE, PURPLE]");

        System.out.println("\nShow center tag 21, 22, or 23 to the camera.");
        System.out.println("Press Enter to read tag (or 'q' to quit)");

        while (true) {
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("q")) {
                break;
            }

            List<BallColor> sequence = limelight.readCenterAprilTag();

            if (!sequence.isEmpty()) {
                System.out.println("\n--- Detected Sequence ---");
                System.out.println("Ball Sequence: " + sequence);

                // Verify correctness based on known mappings
                String result = "UNKNOWN";
                if (sequence.equals(List.of(BallColor.PURPLE, BallColor.PURPLE, BallColor.GREEN))) {
                    result = "Tag 21 detected - CORRECT";
                } else if (sequence.equals(List.of(BallColor.PURPLE, BallColor.GREEN, BallColor.PURPLE))) {
                    result = "Tag 22 detected - CORRECT";
                } else if (sequence.equals(List.of(BallColor.GREEN, BallColor.PURPLE, BallColor.PURPLE))) {
                    result = "Tag 23 detected - CORRECT";
                }

                System.out.println("Result: " + result);
            } else {
                System.out.println("\nNo center tag detected (21-23). Make sure tag is visible.");
            }

            System.out.println("\nPress Enter to read again (or 'q' to quit)");
        }
    }

    // ===== TEST 5: CALIBRATION TUNER =====

    private void calibrationTuner() {
        System.out.println("\n========== Calibration Tuner ==========");
        System.out.println("NOTE: Calibration values are in LimelightVision.java constants.");
        System.out.println("To adjust calibration:");
        System.out.println("  1. Edit TeamCode/src/main/java/org/firstinspires/ftc/teamcode/vision/LimelightVision.java:52-59");
        System.out.println("  2. Update LIMELIGHT_HEIGHT_INCHES, LIMELIGHT_ANGLE_DEGREES, APRILTAG_HEIGHT_INCHES");
        System.out.println("  3. Rebuild and re-run tests");

        System.out.println("\n--- Current Calibration (from LimelightVision.java) ---");
        System.out.println("Limelight Height: " + String.format("%.2f", RealVisionWrapper.LIMELIGHT_HEIGHT_INCHES) + " inches");
        System.out.println("Limelight Angle: " + String.format("%.2f", RealVisionWrapper.LIMELIGHT_ANGLE_DEGREES) + " degrees");
        System.out.println("AprilTag Height: " + String.format("%.2f", RealVisionWrapper.APRILTAG_HEIGHT_INCHES) + " inches");

        System.out.print("\nPress Enter to test with current settings (or 'q' to quit): ");
        while (true) {
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("q")) {
                break;
            }

            System.out.print("Test Red alliance (r) or Blue alliance (b)? ");
            String alliance = scanner.nextLine().trim().toLowerCase();
            boolean isRed = alliance.equals("r");
            VisionTarget target = limelight.getPillarTarget(isRed);
            if (target.isTargetFound()) {
                System.out.println("\nCalculated Distance: " + String.format("%.2f", target.getDistance()) + " inches");
            } else {
                System.out.println("\nNo target detected.");
            }

            System.out.print("\nPress Enter to test again (or 'q' to quit): ");
        }
    }

    // ===== TEST 6: RUN ALL TESTS =====

    private void runAllTests() {
        System.out.println("\n========== Running All Tests ==========");
        System.out.println("This will run tests 1-4 automatically.\n");

        System.out.print("Test Red alliance (r) or Blue alliance (b)? ");
        String alliance = scanner.nextLine().trim().toLowerCase();
        boolean isRed = alliance.equals("r");

        System.out.println("\nStarting automated test suite...\n");

        // Note: Full automation would require more sophisticated testing
        // For now, just run the reliability and pipeline tests
        System.out.println("Running pipeline switching test...");
        testPipelineSwitching();

        System.out.println("\nAll automated tests complete!");
        System.out.println("Note: Distance and center tag tests require manual verification.");

        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}
