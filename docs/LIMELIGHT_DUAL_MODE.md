# Limelight Test UI - Dual Connection Mode

The Limelight Test UI now supports **two connection modes**, allowing you to connect the Limelight to either the Robot Controller Hub or access it via network.

## Connection Modes

### Mode 1: USB/HardwareMap (Recommended for Competition)
```
[Limelight] --USB--> [Robot Controller Hub] --WiFi--> [Drive Hub]
                            ↑
                     Direct USB access
                     Fastest, most reliable
```

**When to use:**
- During competition matches
- Final testing with full robot
- When maximum reliability is needed

**Setup:**
1. Connect Limelight to USB port on Robot Controller Hub
2. Configure hardware: Settings → Configure Robot → Add "Limelight 3A" device
3. Name it **"limelight"** (lowercase, exactly as shown)
4. Run OpMode normally

### Mode 2: HTTP Network (Great for Development)
```
[Limelight] --USB--> [Laptop] --Network--> [Robot Controller] --WiFi--> [Drive Hub]
                        ↑                          ↑
                  limelight.local:5807      OpMode runs here
                  HTTP JSON API             Sends HTTP requests
```

**When to use:**
- Development and testing without robot
- When Robot Controller USB ports are full
- Debugging vision pipelines on laptop first

**Setup:**
1. Connect Limelight to laptop via USB
2. Verify laptop can reach `http://limelight.local:5807/results`
3. Connect Robot Controller to same network as laptop
4. Run OpMode - it will auto-detect HTTP mode

## How Auto-Detection Works

The OpMode tries both modes automatically:

```java
1. Try USB/HardwareMap first
   ├─ Success → Use USB mode ✅
   └─ Fail → Try HTTP...

2. Try HTTP at limelight.local:5807
   ├─ Success → Use HTTP mode ✅
   └─ Fail → Show error screen ❌
```

## Visual Indicators

**Main Menu Display:**
```
=== LIMELIGHT VISION TESTS ===

  1. Distance Calculation
> 2. Detection Reliability
  3. Pipeline Switching
  4. Center Tag Sequences
  5. Calibration Tuner
  6. Run All Tests

Pipeline 2 | USB Mode          ← Connection mode shown here
                 ^^^

DPAD↕=Navigate | A=Select | B=Exit
```

**Initialization Messages:**

USB Mode:
```
Status: ✓ Limelight connected
Mode: USB (Robot Controller)
```

HTTP Mode:
```
Status: ✓ Limelight connected
Mode: HTTP (limelight.local:5807)
```

## Network Setup for HTTP Mode

### Option A: Laptop as WiFi Host

1. **On Laptop:**
   - Create WiFi hotspot
   - Name: "FTC-Network" (or any name)
   - Connect Limelight via USB

2. **On Robot Controller:**
   - Connect to laptop's WiFi hotspot
   - Should auto-detect Limelight at limelight.local

3. **On Drive Hub:**
   - Connect to Robot Controller via WiFi Direct (normal FTC setup)
   - Run OpMode

### Option B: Shared Network

1. **All devices on same network:**
   - Laptop with Limelight → WiFi Network
   - Robot Controller → Same WiFi Network
   - Drive Hub → WiFi Direct to Robot Controller

2. **Verify connectivity:**
   ```bash
   # From laptop, test Limelight:
   curl http://limelight.local:5807/results

   # Should return JSON data
   ```

## Testing Connection Modes

### Test USB Mode
```
Setup:
1. Connect Limelight to Robot Controller USB
2. Configure hardware device "limelight"
3. Run OpMode

Expected:
✓ "USB (Robot Controller)" shown on init
✓ Main menu shows "USB Mode"
✓ All 6 tests work normally
```

### Test HTTP Mode
```
Setup:
1. Connect Limelight to laptop USB
2. Robot Controller on network with laptop
3. NO hardware config needed
4. Run OpMode

Expected:
✓ "HardwareMap - FAILED" message briefly
✓ "HTTP (limelight.local:5807)" shown on init
✓ Main menu shows "HTTP Mode"
✓ All 6 tests work via HTTP API
```

## Feature Comparison

| Feature | USB Mode | HTTP Mode |
|---------|----------|-----------|
| **Latency** | ~5-10ms | ~20-50ms |
| **Reliability** | Highest | Network-dependent |
| **Setup Complexity** | Medium | Higher |
| **Competition Legal** | ✅ Yes | ❌ No (network not allowed) |
| **Development** | ✅ Good | ✅ Excellent |
| **Pipeline Switching** | ✅ Instant | ✅ ~200ms |
| **Distance Calculation** | ✅ Identical | ✅ Identical (same formulas) |
| **Requires Robot** | Yes | No |

## Troubleshooting

### Both Modes Fail

**Error Screen Shows:**
```
Connection Error

Limelight not accessible

Tried:
  1. USB/HardwareMap - FAILED
  2. HTTP Network - FAILED
```

**Solutions:**

For USB Mode:
- ✅ Check USB cable connection
- ✅ Verify hardware configuration exists
- ✅ Device named exactly "limelight" (lowercase)
- ✅ Restart Robot Controller

For HTTP Mode:
- ✅ Verify Limelight connected to laptop
- ✅ Test: `curl http://limelight.local:5807/results`
- ✅ Robot Controller on same network as laptop
- ✅ Check firewall settings (allow port 5807)
- ✅ Try IP address instead: `http://192.168.x.x:5807`

### HTTP Mode Not Detecting

If stuck on "Trying USB..." and then fails:

1. **Test Limelight accessibility from Robot Controller:**
   - SSH into Robot Controller (if possible)
   - Or check network routing

2. **Alternative: Use IP address**
   - Find Limelight IP (e.g., 192.168.1.50)
   - Modify LimelightHttpClient to use IP instead of hostname

3. **mDNS Issues:**
   - Some networks block `.local` mDNS resolution
   - Use static IP assignment for Limelight

## Files Added

```
TeamCode/src/main/java/org/firstinspires/ftc/teamcode/vision/
├── LimelightHttpClient.java        - HTTP communication layer
└── LimelightVisionAdapter.java     - Dual-mode adapter

TeamCode/src/main/java/org/firstinspires/ftc/teamcode/
└── LimelightTestUI.java            - Updated to use adapter
```

## Performance Notes

**HTTP Mode Overhead:**
- Each vision call: ~1-2 HTTP requests
- Typical latency: 20-50ms per request
- Test 2 (100 frames): ~5-10 seconds total

**USB Mode (Direct):**
- No network overhead
- Typical latency: 5-10ms per request
- Test 2 (100 frames): ~5 seconds total

**Recommendation:** Develop with HTTP mode for convenience, but always final-test with USB mode before competition.

## Advanced: Custom HTTP Configuration

To use a custom Limelight address, modify `LimelightVisionAdapter.java`:

```java
// Line 48 - Change default address
this.httpClient = new LimelightHttpClient("192.168.1.50", 5807);
```

Or create multiple adapters for multi-camera setups.

## Summary

✅ **Automatic mode detection** - No code changes needed
✅ **Same interface** - All tests work in both modes
✅ **Same formulas** - Distance calculations identical
✅ **Visual feedback** - Always shows active mode
✅ **Graceful fallback** - Tries USB first, then HTTP
✅ **Development friendly** - Test without robot assembly

The dual-mode system makes development faster while maintaining competition-ready USB support!
