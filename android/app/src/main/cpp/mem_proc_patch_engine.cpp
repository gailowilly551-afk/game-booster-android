//
// mem_proc_patch_engine.cpp — /proc/self/mem ARM64 patch engine
//
// WHY: ACE 2026 monitors mprotect() via seccomp BPF.
//      Writing via /proc/self/mem fd avoids the mprotect syscall entirely.
//      ARM64 still requires icache flush — __builtin___clear_cache() handles this.
//
// JNI exports:
//   patchBytesViaProcMem(jlong addr, jbyteArray patch)  → jboolean
//   readBytesViaProcMem(jlong addr, jint length)        → jbyteArray
//   getLibraryBaseAddress(jstring libName)              → jlong
//
// Updated: Sep 30 2026
//

#include <jni.h>
#include <android/log.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <cstdint>
#include <cstring>
#include <cstdio>
#include <string>

#define TAG "MemProcPatchNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// ─────────────────────────────────────────────────────────────────────────────
// Core patch via /proc/self/mem
// No mprotect() syscall → ACE seccomp monitor stays blind.
// ─────────────────────────────────────────────────────────────────────────────
static bool patchBytesInternal(uintptr_t addr, const uint8_t* patch, size_t size) {
    if (!addr || !patch || size == 0) return false;

    int fd = open("/proc/self/mem", O_RDWR | O_CLOEXEC);
    if (fd < 0) {
        LOGE("open /proc/self/mem failed: %d", fd);
        return false;
    }

    // Seek to target address
    off64_t seeked = lseek64(fd, (off64_t)addr, SEEK_SET);
    if (seeked == (off64_t)-1) {
        LOGE("lseek64 to 0x%lx failed", (unsigned long)addr);
        close(fd);
        return false;
    }

    // Write patch bytes
    ssize_t written = write(fd, patch, size);
    close(fd);

    if (written != (ssize_t)size) {
        LOGE("write failed: wrote %zd of %zu bytes @ 0x%lx", written, size, (unsigned long)addr);
        return false;
    }

    // ARM64 instruction cache flush (mandatory after code patching)
    __builtin___clear_cache(
        reinterpret_cast<char*>(addr),
        reinterpret_cast<char*>(addr + size)
    );

    LOGI("Patched %zu bytes @ 0x%lx OK", size, (unsigned long)addr);
    return true;
}

// ─────────────────────────────────────────────────────────────────────────────
// Read bytes via /proc/self/mem
// ─────────────────────────────────────────────────────────────────────────────
static bool readBytesInternal(uintptr_t addr, uint8_t* out, size_t size) {
    if (!addr || !out || size == 0) return false;

    int fd = open("/proc/self/mem", O_RDONLY | O_CLOEXEC);
    if (fd < 0) return false;

    lseek64(fd, (off64_t)addr, SEEK_SET);
    ssize_t got = read(fd, out, size);
    close(fd);

    return got == (ssize_t)size;
}

// ─────────────────────────────────────────────────────────────────────────────
// Library base address from /proc/self/maps
// ─────────────────────────────────────────────────────────────────────────────
static uintptr_t getLibBase(const char* libName) {
    FILE* maps = fopen("/proc/self/maps", "r");
    if (!maps) return 0;

    char line[512];
    uintptr_t base = 0;
    while (fgets(line, sizeof(line), maps)) {
        if (strstr(line, libName)) {
            // Format: start-end perms offset dev inode path
            unsigned long start = 0;
            sscanf(line, "%lx-", &start);
            base = (uintptr_t)start;
            break;
        }
    }
    fclose(maps);
    return base;
}

// ─────────────────────────────────────────────────────────────────────────────
// JNI Exports
// ─────────────────────────────────────────────────────────────────────────────
extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_gamebooster_app_engine_MemProcPatchEngine_patchBytesViaProcMem(
        JNIEnv* env, jclass /*clazz*/, jlong address, jbyteArray patchArray) {

    if (!patchArray) return JNI_FALSE;
    jsize len = env->GetArrayLength(patchArray);
    if (len <= 0) return JNI_FALSE;

    jbyte* bytes = env->GetByteArrayElements(patchArray, nullptr);
    if (!bytes) return JNI_FALSE;

    bool ok = patchBytesInternal(
        static_cast<uintptr_t>(address),
        reinterpret_cast<const uint8_t*>(bytes),
        static_cast<size_t>(len)
    );

    env->ReleaseByteArrayElements(patchArray, bytes, JNI_ABORT);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jbyteArray JNICALL
Java_com_gamebooster_app_engine_MemProcPatchEngine_readBytesViaProcMem(
        JNIEnv* env, jclass /*clazz*/, jlong address, jint length) {

    if (length <= 0 || length > 4096) return nullptr;
    jbyteArray result = env->NewByteArray(length);
    if (!result) return nullptr;

    jbyte* buf = env->GetByteArrayElements(result, nullptr);
    bool ok = readBytesInternal(
        static_cast<uintptr_t>(address),
        reinterpret_cast<uint8_t*>(buf),
        static_cast<size_t>(length)
    );
    env->ReleaseByteArrayElements(result, buf, ok ? 0 : JNI_ABORT);
    return ok ? result : nullptr;
}

JNIEXPORT jlong JNICALL
Java_com_gamebooster_app_engine_MemProcPatchEngine_getLibraryBaseAddress(
        JNIEnv* env, jclass /*clazz*/, jstring libNameJStr) {

    if (!libNameJStr) return 0L;
    const char* libName = env->GetStringUTFChars(libNameJStr, nullptr);
    uintptr_t base = getLibBase(libName);
    env->ReleaseStringUTFChars(libNameJStr, libName);
    return static_cast<jlong>(base);
}

// ─────────────────────────────────────────────────────────────────────────────
// Remote Process Memory Patcher (process_vm_writev + /proc/<pid>/mem fallback)
// ─────────────────────────────────────────────────────────────────────────────

static uintptr_t getRemoteLibBase(pid_t pid, const char* libName) {
    if (pid <= 0 || !libName) return 0;
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/maps", pid);
    FILE* maps = fopen(path, "r");
    if (!maps) return 0;

    char line[512];
    uintptr_t base = 0;
    while (fgets(line, sizeof(line), maps)) {
        if (strstr(line, libName)) {
            unsigned long start = 0;
            sscanf(line, "%lx-", &start);
            base = (uintptr_t)start;
            break;
        }
    }
    fclose(maps);
    return base;
}

static bool patchRemoteBytesInternal(pid_t pid, uintptr_t addr, const uint8_t* patch, size_t size) {
    if (pid <= 0 || !addr || !patch || size == 0) return false;

    // Fast path: process_vm_writev
    struct iovec local_iov;
    local_iov.iov_base = const_cast<uint8_t*>(patch);
    local_iov.iov_len = size;

    struct iovec remote_iov;
    remote_iov.iov_base = reinterpret_cast<void*>(addr);
    remote_iov.iov_len = size;

    ssize_t nwritten = process_vm_writev(pid, &local_iov, 1, &remote_iov, 1, 0);
    if (nwritten == (ssize_t)size) {
        LOGI("process_vm_writev patched %zu bytes in PID %d @ 0x%lx", size, pid, (unsigned long)addr);
        return true;
    }

    // Fallback: /proc/<pid>/mem
    char memPath[64];
    snprintf(memPath, sizeof(memPath), "/proc/%d/mem", pid);
    int fd = open(memPath, O_RDWR | O_CLOEXEC);
    if (fd >= 0) {
        off64_t seeked = lseek64(fd, (off64_t)addr, SEEK_SET);
        if (seeked != (off64_t)-1) {
            ssize_t written = write(fd, patch, size);
            close(fd);
            if (written == (ssize_t)size) {
                LOGI("/proc/<pid>/mem patched %zu bytes in PID %d @ 0x%lx", size, pid, (unsigned long)addr);
                return true;
            }
        } else {
            close(fd);
        }
    }

    LOGE("Failed to patch remote memory in PID %d @ 0x%lx", pid, (unsigned long)addr);
    return false;
}

static bool readRemoteBytesInternal(pid_t pid, uintptr_t addr, uint8_t* out, size_t size) {
    if (pid <= 0 || !addr || !out || size == 0) return false;

    struct iovec local_iov;
    local_iov.iov_base = out;
    local_iov.iov_len = size;

    struct iovec remote_iov;
    remote_iov.iov_base = reinterpret_cast<void*>(addr);
    remote_iov.iov_len = size;

    ssize_t nread = process_vm_readv(pid, &local_iov, 1, &remote_iov, 1, 0);
    if (nread == (ssize_t)size) return true;

    char memPath[64];
    snprintf(memPath, sizeof(memPath), "/proc/%d/mem", pid);
    int fd = open(memPath, O_RDONLY | O_CLOEXEC);
    if (fd < 0) return false;

    lseek64(fd, (off64_t)addr, SEEK_SET);
    ssize_t got = read(fd, out, size);
    close(fd);
    return got == (ssize_t)size;
}

JNIEXPORT jboolean JNICALL
Java_com_gamebooster_app_engine_MemProcPatchEngine_patchRemoteProcessMemory(
        JNIEnv* env, jclass /*clazz*/, jint pid, jlong address, jbyteArray patchArray) {

    if (!patchArray || pid <= 0) return JNI_FALSE;
    jsize len = env->GetArrayLength(patchArray);
    if (len <= 0) return JNI_FALSE;

    jbyte* bytes = env->GetByteArrayElements(patchArray, nullptr);
    if (!bytes) return JNI_FALSE;

    bool ok = patchRemoteBytesInternal(
        static_cast<pid_t>(pid),
        static_cast<uintptr_t>(address),
        reinterpret_cast<const uint8_t*>(bytes),
        static_cast<size_t>(len)
    );

    env->ReleaseByteArrayElements(patchArray, bytes, JNI_ABORT);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jbyteArray JNICALL
Java_com_gamebooster_app_engine_MemProcPatchEngine_readRemoteProcessMemory(
        JNIEnv* env, jclass /*clazz*/, jint pid, jlong address, jint length) {

    if (pid <= 0 || length <= 0 || length > 4096) return nullptr;
    jbyteArray result = env->NewByteArray(length);
    if (!result) return nullptr;

    jbyte* buf = env->GetByteArrayElements(result, nullptr);
    bool ok = readRemoteBytesInternal(
        static_cast<pid_t>(pid),
        static_cast<uintptr_t>(address),
        reinterpret_cast<uint8_t*>(buf),
        static_cast<size_t>(length)
    );
    env->ReleaseByteArrayElements(result, buf, ok ? 0 : JNI_ABORT);
    return ok ? result : nullptr;
}

JNIEXPORT jlong JNICALL
Java_com_gamebooster_app_engine_MemProcPatchEngine_getRemoteLibraryBaseAddress(
        JNIEnv* env, jclass /*clazz*/, jint pid, jstring libNameJStr) {

    if (pid <= 0 || !libNameJStr) return 0L;
    const char* libName = env->GetStringUTFChars(libNameJStr, nullptr);
    uintptr_t base = getRemoteLibBase(static_cast<pid_t>(pid), libName);
    env->ReleaseStringUTFChars(libNameJStr, libName);
    return static_cast<jlong>(base);
}

} // extern "C"
