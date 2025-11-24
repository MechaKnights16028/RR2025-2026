#!/bin/bash
# Convenience script to run Limelight tests using Android Studio's bundled JDK

export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

echo "==================================================="
echo "       Limelight Vision Test System"
echo "==================================================="
echo ""
echo "Using Java: $JAVA_HOME"
echo ""
echo "Make sure your Limelight is connected via USB and"
echo "accessible at: http://limelight.local:5807 (JSON API)"
echo ""
echo "Starting test system..."
echo ""

./gradlew :LimelightTester:run
