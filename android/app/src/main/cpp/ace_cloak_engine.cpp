//
// ace_cloak_engine.cpp — Native Tencent ACE, CrashSight, Sentry & ScreenCapture Cloak (PUBGM 4.7.0 / CODM / HoK)
// Symbol-driven resolver targeting libanogs.so (AnoSDK*), libCrashSight.so (cs_*/UQM*),
// libsentry-android.so, libcubehawk.so, libUE4.so screen-capture JNI, and libanort.so
// via ELF64 .dynsym + dlsym fallback and stealth /proc/self/mem writes.
//

#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <elf.h>
#include <fcntl.h>
#include <link.h>
#include <sys/stat.h>
#include <unistd.h>
#include <cinttypes>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

#define TAG "AceCloakEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace {

struct MappedModule {
    uintptr_t base = 0;
    std::string path;
    struct Seg {
        uintptr_t start;
        uintptr_t end;
        bool readable;
        bool executable;
    };
    std::vector<Seg> segments;
};

bool is_addr_mapped_executable(const MappedModule& mod, uintptr_t addr, size_t len) {
    for (const auto& s : mod.segments) {
        if (s.readable && addr >= s.start && (addr + len) <= s.end) {
            return true;
        }
    }
    return false;
}

// Stealth memory writer via /proc/self/mem to avoid mprotect VM_WRITE flags
bool patch_code_arm64(const MappedModule& mod, uintptr_t addr, const uint32_t* insns, size_t count) {
    size_t byte_len = count * sizeof(uint32_t);
    if (!is_addr_mapped_executable(mod, addr, byte_len)) {
        return false;
    }

    int fd = open("/proc/self/mem", O_RDWR | O_CLOEXEC);
    if (fd < 0) return false;

    off_t target = static_cast<off_t>(addr);
    ssize_t bytes = static_cast<ssize_t>(byte_len);
    ssize_t written = pwrite(fd, insns, bytes, target);
    close(fd);

    if (written == bytes) {
        __builtin___clear_cache(reinterpret_cast<char*>(addr),
                                reinterpret_cast<char*>(addr + bytes));
        return true;
    }
    return false;
}

// ARM64 MOV X0, #0 ; RET -> 0xD2800000, 0xD65F03C0
static const uint32_t RET_ZERO_STUB[] = {
    0xD2800000, // mov x0, #0
    0xD65F03C0  // ret
};

// Parse ELF64 .dynsym from disk backing file so versioned symbols (e.g. @@ANO) match cleanly
std::vector<uintptr_t> resolve_elf_symbol_prefixes(
        const MappedModule& mod,
        const char* const* prefixes,
        size_t prefix_count) {
    std::vector<uintptr_t> out;
    if (mod.base == 0 || mod.path.empty()) return out;

    int fd = open(mod.path.c_str(), O_RDONLY | O_CLOEXEC);
    if (fd < 0) return out;

    struct stat st{};
    if (fstat(fd, &st) != 0 || st.st_size < static_cast<off_t>(sizeof(Elf64_Ehdr))) {
        close(fd);
        return out;
    }

    Elf64_Ehdr ehdr{};
    if (pread(fd, &ehdr, sizeof(ehdr), 0) != sizeof(ehdr)) {
        close(fd);
        return out;
    }
    if (memcmp(ehdr.e_ident, ELFMAG, SELFMAG) != 0 ||
        ehdr.e_ident[EI_CLASS] != ELFCLASS64 ||
        ehdr.e_shoff == 0 || ehdr.e_shnum == 0 ||
        ehdr.e_shentsize != sizeof(Elf64_Shdr)) {
        close(fd);
        return out;
    }

    std::vector<Elf64_Shdr> shdrs(ehdr.e_shnum);
    ssize_t sh_bytes = static_cast<ssize_t>(ehdr.e_shnum * sizeof(Elf64_Shdr));
    if (pread(fd, shdrs.data(), sh_bytes, static_cast<off_t>(ehdr.e_shoff)) != sh_bytes) {
        close(fd);
        return out;
    }

    uintptr_t min_vaddr = UINTPTR_MAX;
    for (const auto& sh : shdrs) {
        if ((sh.sh_flags & SHF_ALLOC) && sh.sh_addr < min_vaddr) {
            min_vaddr = static_cast<uintptr_t>(sh.sh_addr);
        }
    }
    if (min_vaddr == UINTPTR_MAX) min_vaddr = 0;

    for (const auto& sh : shdrs) {
        if (sh.sh_type != SHT_DYNSYM || sh.sh_link >= ehdr.e_shnum || sh.sh_size == 0) {
            continue;
        }
        const Elf64_Shdr& str_sh = shdrs[sh.sh_link];
        if (str_sh.sh_size == 0 || str_sh.sh_size > (8u * 1024u * 1024u)) continue;

        std::string strtab(static_cast<size_t>(str_sh.sh_size), '\0');
        if (pread(fd, strtab.data(), str_sh.sh_size, static_cast<off_t>(str_sh.sh_offset)) !=
            static_cast<ssize_t>(str_sh.sh_size)) {
            continue;
        }

        size_t sym_count = static_cast<size_t>(sh.sh_size / sizeof(Elf64_Sym));
        std::vector<Elf64_Sym> syms(sym_count);
        if (pread(fd, syms.data(), sh.sh_size, static_cast<off_t>(sh.sh_offset)) !=
            static_cast<ssize_t>(sh.sh_size)) {
            continue;
        }

        for (const auto& sym : syms) {
            if (sym.st_shndx == SHN_UNDEF || sym.st_value == 0 ||
                ELF64_ST_TYPE(sym.st_info) != STT_FUNC ||
                sym.st_name >= strtab.size()) {
                continue;
            }
            const char* name = strtab.c_str() + sym.st_name;
            for (size_t i = 0; i < prefix_count; ++i) {
                size_t plen = strlen(prefixes[i]);
                if (strncmp(name, prefixes[i], plen) == 0) {
                    uintptr_t addr = mod.base + (static_cast<uintptr_t>(sym.st_value) - min_vaddr);
                    out.push_back(addr);
                    break;
                }
            }
        }
    }

    close(fd);
    return out;
}

int cloak_by_dlsym(const MappedModule& mod, const char* lib_name, const char* const* symbols, size_t count) {
    void* handle = dlopen(lib_name, RTLD_NOLOAD);
    if (!handle) return 0;

    int patched = 0;
    for (size_t i = 0; i < count; ++i) {
        void* sym_addr = dlsym(handle, symbols[i]);
        if (sym_addr && patch_code_arm64(mod, reinterpret_cast<uintptr_t>(sym_addr), RET_ZERO_STUB, 2)) {
            patched++;
        }
    }
    dlclose(handle);
    return patched;
}

void collect_module_map(const char* name, MappedModule& out) {
    FILE* fp = fopen("/proc/self/maps", "r");
    if (!fp) return;

    char line[768];
    while (fgets(line, sizeof(line), fp)) {
        if (!strstr(line, name)) continue;
        uintptr_t start = 0, end = 0;
        char perms[8] = {0};
        char path[512] = {0};
        int n = sscanf(line, "%" SCNxPTR "-%" SCNxPTR " %7s %*x %*s %*u %511[^\n]",
                       &start, &end, perms, path);
        if (n >= 3) {
            if (out.base == 0) out.base = start;
            if (out.path.empty() && n == 4) {
                const char* p = path;
                while (*p == ' ' || *p == '\t') p++;
                if (*p == '/') out.path = p;
            }
            out.segments.push_back({start, end, perms[0] == 'r', perms[2] == 'x'});
        }
    }
    fclose(fp);
}

int cloak_module_targets(
        const char* lib_name,
        const char* const* prefixes,
        size_t prefix_count,
        const char* const* fallback_exports,
        size_t fallback_count) {
    MappedModule mod;
    collect_module_map(lib_name, mod);
    if (!mod.base) return 0;

    int count = 0;
    auto addrs = resolve_elf_symbol_prefixes(mod, prefixes, prefix_count);
    if (!addrs.empty()) {
        for (uintptr_t addr : addrs) {
            if (patch_code_arm64(mod, addr, RET_ZERO_STUB, 2)) count++;
        }
    } else if (fallback_exports && fallback_count > 0) {
        count += cloak_by_dlsym(mod, lib_name, fallback_exports, fallback_count);
    }
    LOGI("%s base @ 0x%" PRIxPTR " (cloaked %d targets)", lib_name, mod.base, count);
    return count;
}

} // namespace

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_gamebooster_app_engine_AceCloakEngine_cloakAceModules(
        JNIEnv* /*env*/, jclass /*clazz*/) {
    LOGI("⚡ Initializing Symbol-Driven Native 4.7.0 ACE, CrashSight & Telemetry Cloak...");

    int patchedCount = 0;

    // 1. libanogs.so: neutralize report dispatchers (1..4) & metadata setters (AnoSDK*)
    static const char* const ANOGS_PREFIXES[] = {
        "AnoSDKGetReportData",
        "AnoSDKDelReportData",
        "AnoSDKSetUserInfo",
        "AnoSDKRegister",
        "AnoSDKRegistInfoListener",
        "AnoSDKOnRecvSignature"
    };
    static const char* const ANOGS_EXPORTS[] = {
        "AnoSDKGetReportData", "AnoSDKGetReportData2", "AnoSDKGetReportData3",
        "AnoSDKGetReportData4", "AnoSDKSetUserInfo", "AnoSDKSetUserInfoWithLicense"
    };
    patchedCount += cloak_module_targets("libanogs.so", ANOGS_PREFIXES, 6, ANOGS_EXPORTS, 6);

    // 2. libCrashSight.so: neutralize crash/stuck log uploads while avoiding crash traps
    static const char* const CS_PREFIXES[] = {
        "cs_reportLogInfo",
        "cs_reportStuck",
        "cs_startDumpRoutine",
        "UQMReportLogInfo",
        "UQMCrashAgentReport"
    };
    static const char* const CS_EXPORTS[] = {
        "cs_reportLogInfo", "cs_reportStuck", "cs_startDumpRoutine", "UQMReportLogInfoNative"
    };
    patchedCount += cloak_module_targets("libCrashSight.so", CS_PREFIXES, 5, CS_EXPORTS, 4);

    // 3. libsentry-android.so (PUBGM 4.7.0): neutralize native module map enumeration
    static const char* const SENTRY_PREFIXES[] = {
        "Java_io_sentry_android_ndk_NativeModuleListLoader_nativeLoadModuleList",
        "Java_io_sentry_android_ndk_NativeScope_nativeAddBreadcrumb"
    };
    patchedCount += cloak_module_targets("libsentry-android.so", SENTRY_PREFIXES, 2, SENTRY_PREFIXES, 2);

    // 4. libcubehawk.so (PUBGM 4.7.0): neutralize Hawk emulator check
    static const char* const HAWK_PREFIXES[] = {
        "_ZN4Hawk13checkEmulatorEPKcS1_"
    };
    patchedCount += cloak_module_targets("libcubehawk.so", HAWK_PREFIXES, 1, HAWK_PREFIXES, 1);

    // 5. libUE4.so (PUBGM 4.7.0): silence Android 14/15/16 screen capture callbacks
    static const char* const UE4_CAPTURE_PREFIXES[] = {
        "Java_com_epicgames_ue4_GameActivity_nativeIsEnableScreenCaptureNotify",
        "Java_com_epicgames_ue4_GameActivity_nativeNotifyCaptureScreen",
        "Java_com_blue_studio_screenshot_GameScreenshot_onScreenshotTaken"
    };
    patchedCount += cloak_module_targets("libUE4.so", UE4_CAPTURE_PREFIXES, 3, UE4_CAPTURE_PREFIXES, 3);

    // 6. libanort.so: log presence & guard stubs; do not patch stale offsets or tp_syscall_imp
    MappedModule anort_mod;
    collect_module_map("libanort.so", anort_mod);
    if (anort_mod.base) {
        LOGI("libanort.so mapped @ 0x%" PRIxPTR " (%zu segments) — syscall guard active",
             anort_mod.base, anort_mod.segments.size());
    }

    LOGI("ACE Cloaking applied: %d symbol targets patched.", patchedCount);
    return (patchedCount > 0) ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
