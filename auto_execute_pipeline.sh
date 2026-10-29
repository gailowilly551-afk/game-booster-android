#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# auto_execute_pipeline.sh — Automated Project Build & Verification Pipeline
# Author: Game Launcher Team (for LO)
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_DIR="${ROOT_DIR}/android"

log_step() {
    echo ""
    echo "================================================================================"
    echo "▶ STEP $1: $2"
    echo "================================================================================"
}

log_success() {
    echo "✅ [SUCCESS] $1"
}

# Stage 1: Java & AIDL Compile Check
log_step "1/4" "Verifying Java, AIDL, and Resource Compilation..."
cd "${ANDROID_DIR}"
./gradlew compileDebugJavaWithJavac --quiet
log_success "Javac compilation passed with 0 errors."

# Stage 2: Native CMake ABIs (arm64-v8a, armeabi-v7a, x86_64)
log_step "2/4" "Configuring & Building Native C++ Libraries..."
./gradlew configureCMakeDebug[arm64-v8a] buildCMakeDebug[arm64-v8a] --quiet
log_success "Native arm64-v8a shared libraries built successfully."

# Stage 3: Full Debug APK Assembly & DEX Packaging
log_step "3/4" "Assembling Debug APK (Game_Space_Debug.apk)..."
./gradlew assembleDebug --quiet
log_success "DEX optimization and APK packaging complete."

# Stage 4: Output Verification
log_step "4/4" "Verifying Output Artifacts..."
APK_PATH="${ANDROID_DIR}/app/build/outputs/apk/debug/Game_Space_Debug.apk"

if [[ -f "${APK_PATH}" ]]; then
    APK_SIZE=$(du -h "${APK_PATH}" | cut -f1)
    echo "Target APK: ${APK_PATH}"
    echo "Size:       ${APK_SIZE}"
    log_success "Pipeline executed successfully. Target APK is ready for deployment."
else
    echo "❌ [ERROR] Expected APK not found at ${APK_PATH}"
    exit 1
fi
