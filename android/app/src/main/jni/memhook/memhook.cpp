#include "memhook.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <dirent.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/uio.h>
#include <android/log.h>

#define TAG "memhook_daemon"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#define BUFFER_CHUNK_SIZE (64 * 1024)

pid_t FindProcessPid(const char* processName) {
    if (!processName || strlen(processName) == 0) return -1;

    DIR* dir = opendir("/proc");
    if (!dir) return -1;

    struct dirent* entry;
    pid_t targetPid = -1;

    while ((entry = readdir(dir)) != NULL) {
        if (entry->d_type != DT_DIR) continue;

        char* endptr;
        long pid = strtol(entry->d_name, &endptr, 10);
        if (*endptr != '\0') continue; // Not a numeric directory

        char cmdlinePath[256];
        snprintf(cmdlinePath, sizeof(cmdlinePath), "/proc/%ld/cmdline", pid);

        int fd = open(cmdlinePath, O_RDONLY);
        if (fd >= 0) {
            char cmdline[256];
            ssize_t bytes = read(fd, cmdline, sizeof(cmdline) - 1);
            close(fd);
            if (bytes > 0) {
                cmdline[bytes] = '\0';
                if (strstr(cmdline, processName) != NULL) {
                    targetPid = (pid_t)pid;
                    break;
                }
            }
        }
    }

    closedir(dir);
    return targetPid;
}

int ScanAndReplaceString(pid_t pid, const char* target, const char* replacement) {
    if (!target || !replacement || strlen(target) == 0) return 0;
    size_t targetLen = strlen(target);
    size_t replLen = strlen(replacement);

    char mapsPath[64];
    snprintf(mapsPath, sizeof(mapsPath), "/proc/%d/maps", (int)pid);

    FILE* fp = fopen(mapsPath, "re");
    if (!fp) {
        LOGE("Failed to open %s", mapsPath);
        return 0;
    }

    char memPath[64];
    snprintf(memPath, sizeof(memPath), "/proc/%d/mem", (int)pid);
    int memFd = open(memPath, O_RDWR);
    if (memFd < 0) {
        LOGE("Failed to open %s", memPath);
        fclose(fp);
        return 0;
    }

    char line[512];
    int replacedCount = 0;
    uint8_t buffer[BUFFER_CHUNK_SIZE];

    while (fgets(line, sizeof(line), fp)) {
        uintptr_t start, end;
        char perms[16];
        if (sscanf(line, "%lx-%lx %15s", &start, &end, perms) != 3) continue;

        // Scan writable regions: heap, anon, .data, .bss, [anon:dalvik...]
        // Avoid read-only code segments to prevent crashes
        if (perms[0] != 'r' || perms[1] != 'w') continue;

        size_t regionSize = end - start;
        uintptr_t curAddr = start;

        while (curAddr < end) {
            size_t toRead = (end - curAddr > BUFFER_CHUNK_SIZE) ? BUFFER_CHUNK_SIZE : (end - curAddr);
            if (pread64(memFd, buffer, toRead, (off64_t)curAddr) != (ssize_t)toRead) {
                break;
            }

            for (size_t i = 0; i + targetLen <= toRead; ++i) {
                if (memcmp(&buffer[i], target, targetLen) == 0) {
                    uintptr_t hitAddr = curAddr + i;
                    // Prepare padded replacement buffer to keep memory aligned
                    size_t writeLen = (replLen < targetLen) ? replLen : targetLen;
                    pwrite64(memFd, replacement, writeLen, (off64_t)hitAddr);
                    
                    // If replacement is shorter, null-pad to prevent trailing garbage
                    if (replLen < targetLen) {
                        char zeros[64] = {0};
                        size_t padLen = targetLen - replLen;
                        pwrite64(memFd, zeros, padLen, (off64_t)(hitAddr + replLen));
                    }

                    replacedCount++;
                    LOGI("Live RAM Mask: 0x%lx '%s' -> '%s'", (unsigned long)hitAddr, target, replacement);
                }
            }

            // Advance window with overlap for strings that cross boundaries
            if (toRead > targetLen) {
                curAddr += (toRead - targetLen);
            } else {
                curAddr += toRead;
            }
        }
    }

    close(memFd);
    fclose(fp);
    return replacedCount;
}

bool SpoofDeviceIdentity(const char* realModel, const char* spoofModel, const char* spoofBrand, int targetFps) {
    LOGI("⚡ [memhook] Initiating Live RAM Identity Spoofing for MLBB...");
    pid_t pid = FindProcessPid("com.mobile.legends");
    if (pid <= 0) {
        pid = FindProcessPid("mobilelegends");
    }

    if (pid <= 0) {
        LOGE("Target MLBB process not currently running. Will hook on process spawn.");
        return false;
    }

    LOGI("Attached to MLBB process PID: %d", pid);

    int replaced = 0;

    // 1. Replace host device model with ASUS ROG Phone 10 Pro+ model string
    if (realModel && strlen(realModel) > 0) {
        replaced += ScanAndReplaceString(pid, realModel, spoofModel);
    }

    // 2. Scan and mask common fallback device model queries inside Unity Engine
    replaced += ScanAndReplaceString(pid, "SM-G998B", spoofModel);
    replaced += ScanAndReplaceString(pid, "M2012K11AC", spoofModel);
    replaced += ScanAndReplaceString(pid, "24122RKC7C", spoofModel);
    replaced += ScanAndReplaceString(pid, "Generic Android", spoofModel);

    // 3. Overwrite GPU/Vendor identity strings to Snapdragon 8 Elite Gen 2 / Adreno 840
    ScanAndReplaceString(pid, "Mali-G77", "Adreno (TM) 840");
    ScanAndReplaceString(pid, "Mali-G710", "Adreno (TM) 840");
    ScanAndReplaceString(pid, "Mali-G720", "Adreno (TM) 840");
    ScanAndReplaceString(pid, "Adreno (TM) 640", "Adreno (TM) 840");
    ScanAndReplaceString(pid, "Adreno (TM) 730", "Adreno (TM) 840");

    LOGI("✅ [memhook] Live RAM Masking Completed: %d patches applied. Target FPS: %d", replaced, targetFps);
    return true;
}

#include <jni.h>

extern "C" JNIEXPORT jboolean JNICALL
Java_com_android_support_shizuku_MlbbModInjector_nativeSpoofDeviceIdentity(
        JNIEnv* env, jclass clazz, jstring jRealModel, jstring jSpoofModel, jstring jSpoofBrand, jint targetFps) {
    const char* realModel = jRealModel ? env->GetStringUTFChars(jRealModel, nullptr) : "Unknown";
    const char* spoofModel = jSpoofModel ? env->GetStringUTFChars(jSpoofModel, nullptr) : "ASUS_AI2601_A";
    const char* spoofBrand = jSpoofBrand ? env->GetStringUTFChars(jSpoofBrand, nullptr) : "ASUS ROG Phone 10 Pro+";

    bool result = SpoofDeviceIdentity(realModel, spoofModel, spoofBrand, targetFps);

    if (jRealModel) env->ReleaseStringUTFChars(jRealModel, realModel);
    if (jSpoofModel) env->ReleaseStringUTFChars(jSpoofModel, spoofModel);
    if (jSpoofBrand) env->ReleaseStringUTFChars(jSpoofBrand, spoofBrand);

    return result ? JNI_TRUE : JNI_FALSE;
}

#ifndef MEMHOOK_AS_LIBRARY
int main(int argc, char* argv[]) {
    const char* realModel = "Unknown";
    const char* spoofModel = "ASUS_AI2601_A";
    const char* spoofBrand = "ASUS ROG Phone 10 Pro+";
    int targetFps = 165;

    for (int i = 1; i < argc; ++i) {
        if (strcmp(argv[i], "--spoof") == 0) {
            if (i + 1 < argc) realModel = argv[++i];
        } else if (strcmp(argv[i], "--model") == 0 && i + 1 < argc) {
            spoofModel = argv[++i];
        } else if (strcmp(argv[i], "--brand") == 0 && i + 1 < argc) {
            spoofBrand = argv[++i];
        } else if (strcmp(argv[i], "--fps") == 0 && i + 1 < argc) {
            targetFps = atoi(argv[++i]);
        }
    }

    SpoofDeviceIdentity(realModel, spoofModel, spoofBrand, targetFps);
    return 0;
}
#endif
