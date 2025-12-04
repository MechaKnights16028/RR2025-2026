# Limelight Testing System

A standalone testing application for validating LimelightVision functionality without deploying to the FTC Control Hub.

## Requirements

- **Java 8 or higher** installed on your dev machine
- **Limelight camera** connected via USB
- **Network access** to Limelight at `http://limelight.local:5807` (JSON API)

### Installing Java

If you don't have Java installed:

**macOS:**
```bash
brew install openjdk@11
```

**Linux:**
```bash
sudo apt-get install openjdk-11-jdk
```

**Windows:**
Download from https://adoptium.net/

## Usage

### 1. Connect Limelight

Connect your Limelight3A camera to your laptop via USB. Verify it's accessible:

```bash
# Test in browser - should show Limelight web interface (config on 5801)
open http://limelight.local:5801

# Test JSON API - should return JSON data (used by test system on 5807)
curl http://limelight.local:5807/results
```

### 2. Run Tests

**Easy Method (uses convenience script):**
```bash
./run-limelight-tests.sh
```

**Manual Method:**
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :LimelightTester:run
```

### 3. Test Menu

The system provides an interactive menu:

```
Main Menu:
  1. Test Distance Calculation (AprilTag)
  2. Test Detection Reliability
  3. Test Pipeline Switching
  4. Test Center Tag Sequences
  5. Calibration Tuner
  6. Run All Tests
  7. Exit
```

## Test Descriptions

### Test 1: Distance Calculation
- Validates the trigonometric distance formula
- Compare calculated distance vs. actual measured distance
- Helps verify camera height and angle calibration

**How to use:**
1. Place AprilTag at known distance (e.g., 36 inches)
2. Point Limelight at tag
3. System calculates distance
4. Enter actual distance to see error percentage

### Test 2: Detection Reliability
- Runs 100-frame capture test
- Measures detection consistency
- Tracks average tx, ty, and distance stability

**Pass criteria:** ≥90% detection rate

### Test 3: Pipeline Switching
- Cycles through all 4 pipelines:
  - Pipeline 0: Purple ball detection
  - Pipeline 1: Green ball detection
  - Pipeline 2: Pillar AprilTag detection (tags 20, 24)
  - Pipeline 3: Center AprilTag detection (tags 21, 22, 23)
- Verifies each switch is successful

### Test 4: Center Tag Sequences
- Tests center AprilTag (21-23) reading
- Verifies ball sequence mapping:
  - Tag 21 → [GREEN, PURPLE, PURPLE]
  - Tag 22 → [PURPLE, GREEN, PURPLE]
  - Tag 23 → [PURPLE, PURPLE, GREEN]

### Test 5: Calibration Tuner
- Interactively adjust calibration parameters:
  - Limelight height (inches from floor)
  - Limelight angle (degrees, positive = angled up)
  - AprilTag height (inches from floor)
- See real-time distance calculation updates
- Find optimal values for your robot

### Test 6: Run All Tests
- Automated test suite
- Runs tests 2 and 3 automatically
- Tests 1, 4, and 5 require manual verification

## Calibration Constants

Current values (from LimelightVision.java):
- **Limelight Height:** 40.0 inches (measured from floor to camera lens center)
- **Limelight Angle:** 15.0 degrees (positive = angled up)
- **AprilTag Height:** 36.0 inches (measured from floor to tag center)

**Important:** These values must be accurately measured on your robot for correct distance calculations. Use the Calibration Tuner (Test 5) to validate and adjust values. Once you confirm good values, update them in `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/vision/LimelightVision.java` (lines 56-63).

## Architecture

The test system uses **EXACT COPIES** of formulas from `LimelightVision.java`:

```
LimelightTester/
├── LimelightHttpClient.java     # HTTP communication with Limelight
├── RealVisionWrapper.java       # EXACT COPY of LimelightVision formulas
├── TestRunner.java               # Main test application
├── SYNC.md                       # How to keep code synchronized
└── models/
    ├── BallColor.java
    ├── TargetType.java
    ├── VisionTarget.java
    └── LimelightResult.java      # HTTP response parsing
```

**Key principle:** If distance calculation works in the tester, it will work on the robot.

**IMPORTANT:** `RealVisionWrapper.java` contains line-by-line copies of the vision code from `TeamCode/vision/LimelightVision.java`. When you change LimelightVision.java, you **MUST** update RealVisionWrapper.java to match. See [SYNC.md](SYNC.md) for details.

## Important Notes

### Manual Pipeline Switching

If automatic pipeline switching doesn't work (you'll see a message), you can manually switch pipelines:

1. Open `http://limelight.local:5801` in your browser
2. Go to the "Pipeline" tab
3. Select the desired pipeline:
   - **Pipeline 0**: Purple ball detection
   - **Pipeline 1**: Green ball detection
   - **Pipeline 2**: Pillar AprilTag detection (tags 20, 24)
   - **Pipeline 3**: Center AprilTag detection (tags 21, 22, 23)

The test system will detect which pipeline is active and work accordingly.

## Troubleshooting

### "Cannot connect to Limelight"
- Verify USB connection
- Open `http://limelight.local:5801` in browser
- Test JSON API: `curl http://limelight.local:5807/results`
- Check that Limelight is powered on

### "Unable to locate a Java Runtime"
- Install Java 8 or higher (see Requirements above)
- Verify: `java -version`

### Distance calculations seem wrong
- Use Calibration Tuner (Test 5) to adjust parameters
- Measure actual camera height and angle
- Ensure AprilTag height is correct (center of tag to floor)

### Pipeline switching doesn't work
- Verify all 4 pipelines are configured in Limelight web interface:
  - Pipeline 0: Purple ball color detector
  - Pipeline 1: Green ball color detector
  - Pipeline 2: AprilTag detector (for tags 20, 24)
  - Pipeline 3: AprilTag detector (for tags 21, 22, 23)
- You can manually switch pipelines via `http://limelight.local:5801`

## Development

To modify the test system:

1. Edit files in `LimelightTester/src/main/java/tester/`
2. Rebuild: `./gradlew :LimelightTester:build`
3. Run: `./gradlew :LimelightTester:run`

To add new tests, modify `TestRunner.java` and add new menu options.

## Support

- FTC SDK documentation: https://ftc-docs.firstinspires.org/
- Road Runner docs: https://rr.brott.dev/
- Limelight docs: https://docs.limelightvision.io/
