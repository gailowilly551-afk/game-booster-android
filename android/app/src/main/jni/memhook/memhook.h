#pragma once

#include <stdint.h>
#include <stddef.h>
#include <stdbool.h>
#include <sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Scan and replace an ASCII/UTF-8 string across all writable memory segments
 * (/proc/pid/mem) of a target process without invoking mprotect syscall.
 *
 * @param pid Target process ID
 * @param target The string to search for (e.g. real device model)
 * @param replacement The string to write in place (e.g. ASUS_AI2601_A)
 * @return Number of replacements made
 */
int ScanAndReplaceString(pid_t pid, const char* target, const char* replacement);

/**
 * Spoofs device identity directly inside running process RAM.
 * Replaces real device model, brand strings, and locks native Unity/MLBB FPS in memory.
 *
 * @param realModel The host device's actual Build.MODEL
 * @param spoofModel The spoofed model string ("ASUS_AI2601_A")
 * @param spoofBrand The spoofed marketing name ("ASUS ROG Phone 10 Pro+")
 * @param targetFps Native FPS ceiling to enforce (165)
 * @return true if identity was successfully spoofed in memory
 */
bool SpoofDeviceIdentity(const char* realModel, const char* spoofModel, const char* spoofBrand, int targetFps);

/**
 * Finds PID of a running game by its package or process name.
 */
pid_t FindProcessPid(const char* processName);

#ifdef __cplusplus
}
#endif
