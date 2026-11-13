//
// il2cpp_direct_scanner.cpp — Standalone Memory Pattern & Symbol Scanner for libil2cpp.so
// Segment-safe /proc/self/maps parsing (prevents SIGSEGV across unmapped guard holes) +
// dynamic il2cpp_* C-API export resolution.
//

#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <unistd.h>
#include <fcntl.h>
#include <cstring>
#include <cstdio>
#include <cstdint>
#include <cinttypes>
#include <vector>
#include <string>

#define TAG "Il2cppDirectScanner"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace {

struct ReadableSegment {
    uintptr_t start;
    uintptr_t end;
};

uintptr_t collect_readable_segments(const char* module_name, std::vector<ReadableSegment>& segments) {
    segments.clear();
    FILE* fp = fopen("/proc/self/maps", "r");
    if (!fp) return 0;

    char line[512];
    uintptr_t base = 0;

    while (fgets(line, sizeof(line), fp)) {
        if (!strstr(line, module_name)) continue;
        uintptr_t seg_start = 0, seg_end = 0;
        char perms[8] = {0};
        if (sscanf(line, "%" SCNxPTR "-%" SCNxPTR " %7s", &seg_start, &seg_end, perms) == 3) {
            if (base == 0) base = seg_start;
            // Scan only readable mappings so PROT_NONE / ---p guard pages never SIGSEGV
            if (perms[0] == 'r' && seg_end > seg_start) {
                segments.push_back({seg_start, seg_end});
            }
        }
    }
    fclose(fp);
    return base;
}

uintptr_t find_module_base(const char* module_name) {
    std::vector<ReadableSegment> segments;
    return collect_readable_segments(module_name, segments);
}

uintptr_t scan_pattern_segments(const std::vector<ReadableSegment>& segments,
                                const uint8_t* pattern,
                                const char* mask) {
    size_t pattern_len = strlen(mask);
    if (pattern_len == 0) return 0;

    for (const auto& seg : segments) {
        size_t length = static_cast<size_t>(seg.end - seg.start);
        if (length < pattern_len) continue;

        const uint8_t* current = reinterpret_cast<const uint8_t*>(seg.start);
        for (size_t i = 0; i <= length - pattern_len; ++i) {
            bool match = true;
            for (size_t j = 0; j < pattern_len; ++j) {
                if (mask[j] != '?' && pattern[j] != current[i + j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return seg.start + i;
            }
        }
    }
    return 0;
}

const char* const IL2CPP_CANDIDATES[] = {
    "libil2cpp.so",
    "libil2cpp_cp.so",
    "libcsharp.so",
    "liblogic.so",
    "libmoba.so",
    "libfirst.so",
    "libunity.so"
};

uintptr_t collect_primary_il2cpp_segments(std::vector<ReadableSegment>& segments) {
    for (const char* name : IL2CPP_CANDIDATES) {
        uintptr_t base = collect_readable_segments(name, segments);
        if (base != 0 && !segments.empty()) {
            return base;
        }
    }
    return 0;
}

} // namespace

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_gamebooster_app_engine_Il2cppDirectScanner_getIl2cppBase(
        JNIEnv* /*env*/, jclass /*clazz*/) {
    for (const char* name : IL2CPP_CANDIDATES) {
        uintptr_t base = find_module_base(name);
        if (base != 0) return static_cast<jlong>(base);
    }
    return 0;
}

JNIEXPORT jlong JNICALL
Java_com_gamebooster_app_engine_Il2cppDirectScanner_scanPattern(
        JNIEnv* env, jclass /*clazz*/, jbyteArray jPattern, jstring jMask) {
    if (!jPattern || !jMask) return 0;

    std::vector<ReadableSegment> segments;
    if (collect_primary_il2cpp_segments(segments) == 0) return 0;

    jsize pat_len = env->GetArrayLength(jPattern);
    if (pat_len <= 0) return 0;

    jbyte* pat_bytes = env->GetByteArrayElements(jPattern, nullptr);
    const char* mask_str = env->GetStringUTFChars(jMask, nullptr);
    if (!pat_bytes || !mask_str) {
        if (pat_bytes) env->ReleaseByteArrayElements(jPattern, pat_bytes, JNI_ABORT);
        if (mask_str) env->ReleaseStringUTFChars(jMask, mask_str);
        return 0;
    }

    uintptr_t found = 0;
    if (strlen(mask_str) <= static_cast<size_t>(pat_len)) {
        found = scan_pattern_segments(segments,
                                      reinterpret_cast<const uint8_t*>(pat_bytes),
                                      mask_str);
    }

    env->ReleaseByteArrayElements(jPattern, pat_bytes, JNI_ABORT);
    env->ReleaseStringUTFChars(jMask, mask_str);

    return static_cast<jlong>(found);
}

JNIEXPORT jlong JNICALL
Java_com_gamebooster_app_engine_Il2cppDirectScanner_getUnityBase(
        JNIEnv* /*env*/, jclass /*clazz*/) {
    return static_cast<jlong>(find_module_base("libunity.so"));
}

JNIEXPORT jlong JNICALL
Java_com_gamebooster_app_engine_Il2cppDirectScanner_scanPatternInModule(
        JNIEnv* env, jclass /*clazz*/, jstring jModuleName, jbyteArray jPattern, jstring jMask) {
    if (!jModuleName || !jPattern || !jMask) return 0;

    const char* mod_str = env->GetStringUTFChars(jModuleName, nullptr);
    if (!mod_str) return 0;
    std::vector<ReadableSegment> segments;
    uintptr_t base = collect_readable_segments(mod_str, segments);
    env->ReleaseStringUTFChars(jModuleName, mod_str);
    if (!base || segments.empty()) return 0;

    jsize pat_len = env->GetArrayLength(jPattern);
    if (pat_len <= 0) return 0;

    jbyte* pat_bytes = env->GetByteArrayElements(jPattern, nullptr);
    const char* mask_str = env->GetStringUTFChars(jMask, nullptr);
    if (!pat_bytes || !mask_str) {
        if (pat_bytes) env->ReleaseByteArrayElements(jPattern, pat_bytes, JNI_ABORT);
        if (mask_str) env->ReleaseStringUTFChars(jMask, mask_str);
        return 0;
    }

    uintptr_t found = 0;
    if (strlen(mask_str) <= static_cast<size_t>(pat_len)) {
        found = scan_pattern_segments(segments,
                                      reinterpret_cast<const uint8_t*>(pat_bytes),
                                      mask_str);
    }

    env->ReleaseByteArrayElements(jPattern, pat_bytes, JNI_ABORT);
    env->ReleaseStringUTFChars(jMask, mask_str);

    return static_cast<jlong>(found);
}

JNIEXPORT jlong JNICALL
Java_com_gamebooster_app_engine_Il2cppDirectScanner_resolveIl2cppExport(
        JNIEnv* env, jclass /*clazz*/, jstring jSymbolName) {
    if (!jSymbolName) return 0;
    const char* sym = env->GetStringUTFChars(jSymbolName, nullptr);
    if (!sym) return 0;

    void* addr = nullptr;
    for (const char* lib : IL2CPP_CANDIDATES) {
        void* handle = dlopen(lib, RTLD_NOLOAD);
        if (handle) {
            addr = dlsym(handle, sym);
            dlclose(handle);
            if (addr) break;
        }
    }

    env->ReleaseStringUTFChars(jSymbolName, sym);
    return static_cast<jlong>(reinterpret_cast<uintptr_t>(addr));
}

} // extern "C"
