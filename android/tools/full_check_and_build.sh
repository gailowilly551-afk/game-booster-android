#!/bin/bash
# ==============================================================================
# Full Pre-Build Verification & Automated APK Build Pipeline
# Game Space PRO (Android 13 - 16 / API 33 - 36)
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ANDROID_DIR="$PROJECT_ROOT/android"

cd "$ANDROID_DIR"

echo "=================================================================="
echo "🚀 [1/4] Running Java & Kotlin Compile Check..."
echo "=================================================================="
./gradlew compileDebugJavaWithJavac

echo "=================================================================="
echo "🛡️ [2/4] Running Android Lint Check (Bugs, Warnings, Compatibility)..."
echo "=================================================================="
./gradlew lintDebug

echo "=================================================================="
echo "🧪 [3/4] Running Full Unit Test Suite..."
echo "=================================================================="
./gradlew testDebugUnitTest

echo "=================================================================="
echo "📦 [4/4] Assembling APK & Building Native Engines..."
echo "=================================================================="
./gradlew assembleDebug

# Copy APK to project root for easy user access
APK_SOURCE="$ANDROID_DIR/app/build/outputs/apk/debug/Game_Space_Debug.apk"
APK_TARGET="$PROJECT_ROOT/Game_Space_v20.1.0-PRO_debug.apk"

if [ -f "$APK_SOURCE" ]; then
    cp "$APK_SOURCE" "$APK_TARGET"
    echo "=================================================================="
    echo "✅ 100% BUILD & VERIFICATION SUCCESSFUL!"
    echo "📱 APK Ready at: $APK_TARGET"
    echo "=================================================================="
else
    echo "❌ APK build output not found!"
    exit 1
fi
