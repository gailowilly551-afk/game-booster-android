// =============================================================================
// GameBooster Native — Signal Trap Guard (Safe No-Op / ART Safe)
// Protects ART JVM runtime integrity without hijacking SIGSEGV or calling ptrace.
// =============================================================================

#include <jni.h>
#include <android/log.h>
#include <atomic>

#define TAG "SignalTrapGuard"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

static std::atomic<bool> sIsArmed(false);

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_gamebooster_app_config_NativeConfigInjector_nativeArmSignalTrapGuard(
        JNIEnv *env, jclass clazz) {
    sIsArmed.store(true);
    LOGI("SignalTrapGuard armed in ART-safe mode (JVM signal handlers preserved)");
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_gamebooster_app_config_NativeConfigInjector_nativeDisarmSignalTrapGuard(
        JNIEnv *env, jclass clazz) {
    sIsArmed.store(false);
    LOGI("SignalTrapGuard disarmed");
    return JNI_TRUE;
}

} // extern "C"
