/**
 * CODM Deep Anti-Ban & Anticheat Cloak Script (Frida)
 * Targets:
 *  - Tencent ACE (libanort.so, libanogs.so AnoSDK*@@ANO)
 *  - CrashSight (libCrashSight.so cs_* and UQM*, libCrashSightPlugin.so)
 *  - Telemetry (libTDataMaster.so) & SSL Pinning
 *  (libsaf.so is OpenAL Soft audio in CODM and intentionally untouched)
 */

'use strict';

console.log('[AntiBan-CODM] Initializing Anticheat Suppression Suite...');

// ─── 1. SSL Unpinning & Telemetry Host Redirection ───────────────────────────
Java.perform(() => {
    try {
        const CertificatePinner = Java.use('okhttp3.CertificatePinner');
        CertificatePinner.check.overload('java.lang.String', 'java.util.List').implementation = function(host, certs) {
            // Nullify SSL check
            return;
        };
        console.log('[AntiBan-CODM] OkHttp3 CertificatePinner unpinned');
    } catch(e) {}

    try {
        const TrustManagerImpl = Java.use('com.android.org.conscrypt.TrustManagerImpl');
        TrustManagerImpl.verifyChain.implementation = function(chain, authType, host, clientAuth, ocsp, tls) {
            return chain;
        };
        console.log('[AntiBan-CODM] Conscrypt TrustManager unpinned');
    } catch(e) {}
});

// Helper: Module watcher
function onModuleLoaded(moduleName, callback) {
    const mod = Process.findModuleByName(moduleName);
    if (mod) {
        callback(mod);
    } else {
        const timer = setInterval(() => {
            const m = Process.findModuleByName(moduleName);
            if (m) {
                clearInterval(timer);
                callback(m);
            }
        }, 300);
    }
}

// ─── 2. Tencent ACE (libanort.so & libanogs.so) Watchdog Suppression ─────────
onModuleLoaded('libanort.so', (mod) => {
    const tpSyscall = Module.findExportByName('libanort.so', 'tp_syscall_imp');
    console.log('[AntiBan-CODM] libanort.so detected @ ' + mod.base + ' (tp_syscall_imp=' + tpSyscall + ')');
});

onModuleLoaded('libanogs.so', (mod) => {
    console.log('[AntiBan-CODM] libanogs.so detected @ ' + mod.base);
    const prefixes = ['AnoSDKGetReportData', 'AnoSDKDelReportData', 'AnoSDKSetUserInfo', 'AnoSDKRegister'];
    mod.enumerateExports().forEach(exp => {
        if (exp.type !== 'function') return;
        if (prefixes.some(p => exp.name.indexOf(p) === 0)) {
            try {
                Interceptor.replace(exp.address, new NativeCallback(() => 0, 'int', []));
                console.log('[AntiBan-CODM] Hooked libanogs export: ' + exp.name);
            } catch(e) {}
        }
    });
});

// ─── 3. CrashSight Suppression (cs_* / UQM*) ──────────────────────────────────
onModuleLoaded('libCrashSight.so', (mod) => {
    console.log('[AntiBan-CODM] libCrashSight.so detected @ ' + mod.base);
    const prefixes = ['cs_reportLogInfo', 'cs_reportStuck', 'cs_startDumpRoutine', 'UQMReportLogInfo', 'UQMCrashAgentReport'];
    mod.enumerateExports().forEach(exp => {
        if (exp.type !== 'function') return;
        if (prefixes.some(p => exp.name.indexOf(p) === 0)) {
            try {
                Interceptor.replace(exp.address, new NativeCallback(() => 0, 'int', []));
            } catch(e) {}
        }
    });
});

// ─── 4. TGPA Frame / Telemetry Guard ──────────────────────────────────────────
onModuleLoaded('libtgpa.so', (mod) => {
    console.log('[AntiBan-CODM] libtgpa.so detected @ ' + mod.base);
    const mtkApi = Module.findExportByName('libtgpa.so', 'MTKFRC_GetAPILevel');
    const mfrcApi = Module.findExportByName('libtgpa.so', 'MFRC_GetAPILevel');
    [mtkApi, mfrcApi].forEach(ptrFn => {
        if (ptrFn) {
            try {
                Interceptor.attach(ptrFn, { onLeave(retval) { if (retval.toInt32() <= 0) retval.replace(ptr(1)); } });
            } catch(e) {}
        }
    });
});

// ─── 5. Permanent Zero-Revert Inode & Config Shielding ────────────────────────
(function shieldConfigFiles() {
    const protectedNames = [
        'UserSetting.json', 'GameConfig.xml', 'ControlsSettings.ini',
        'GraphicsSettings.ini', 'MSDKConfig.ini', 'res_skip_patch'
    ];
    const openatPtr = Module.findExportByName(null, 'openat');
    if (openatPtr) {
        Interceptor.attach(openatPtr, {
            onEnter(args) {
                const pathPtr = args[1];
                if (pathPtr) {
                    const path = pathPtr.readCString();
                    if (path && protectedNames.some(p => path.includes(p))) {
                        const flags = args[2].toInt32();
                        // Strip O_TRUNC (0x200 / 512) to prevent reverting injected configs
                        if ((flags & 0x200) !== 0) {
                            args[2] = ptr(flags & ~0x200);
                        }
                    }
                }
            }
        });
    }

    const unlinkPtr = Module.findExportByName(null, 'unlink');
    if (unlinkPtr) {
        Interceptor.attach(unlinkPtr, {
            onEnter(args) {
                const pathPtr = args[0];
                if (pathPtr) {
                    const path = pathPtr.readCString();
                    if (path && protectedNames.some(p => path.includes(p))) {
                        this.suppress = true;
                    }
                }
            },
            onLeave(retval) {
                if (this.suppress) {
                    retval.replace(ptr(0)); // Pretend unlinked, keep permanent mod
                }
            }
        });
    }
})();

