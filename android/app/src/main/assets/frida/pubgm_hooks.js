/**
 * PUBGM Frida Hook Engine (v4.7.0 Global 21740 & Flash 21745)
 * Game: PUBG Mobile / PUBGM Flash / BGMI (com.tencent.ig / com.tencent.igfit / com.pubg.imobile)
 * Architecture: arm64-v8a
 * Engine: Unreal Engine 4.18 (libUE4.so)
 * Anti-Cheat & Telemetry: Tencent ACE (libanort.so, libanogs.so), CrashSight,
 *   TGPA/MGPA (libtgpa.so), CubeHawk (libcubehawk.so), Sentry NDK (libsentry-android.so),
 *   Play Games Deku Overlay (libgpdeku.so), SpecialGem (libspecialgem.so)
 */

'use strict';

// ─── Native System Property Resolver (Safe, Zero-Crash) ───────────────────────
function getSystemProp(key, defaultVal) {
  try {
    const propGet = Module.findExportByName('libc.so', '__system_property_get');
    if (propGet) {
      const fn = new NativeFunction(propGet, 'int', ['pointer', 'pointer']);
      const k = Memory.allocUtf8String(key);
      const v = Memory.alloc(128);
      const len = fn(k, v);
      if (len > 0) {
        const val = v.readUtf8String();
        if (val && val.length > 0) return val;
      }
    }
  } catch (e) {}

  if (typeof Java !== 'undefined' && Java.available) {
    try {
      let res = null;
      Java.perform(() => {
        try {
          const sysProp = Java.use('android.os.SystemProperties');
          res = sysProp.get(key, defaultVal);
        } catch (je) {}
      });
      if (res !== null && res !== undefined) return res;
    } catch (e) {}
  }
  return defaultVal;
}

function parseOptionalHexProp(key) {
  const raw = getSystemProp(key, '');
  if (!raw || raw === '0' || raw === '0x0') return 0;
  const parsed = parseInt(raw, 16);
  return (!isNaN(parsed) && parsed > 0) ? parsed : 0;
}

// ─── Config Loaded Safely via SystemProperties ───────────────────────────────
const CFG = {
  variant:          getSystemProp('gamebooster.pubgm.variant', 'global'),
  version:          getSystemProp('gamebooster.pubgm.version', '4.7.0'),
  aimbot:           getSystemProp('gamebooster.pubgm.aim', '1') === '1',
  noRecoil:         getSystemProp('gamebooster.pubgm.recoil', '1') === '1',
  noSpread:         getSystemProp('gamebooster.pubgm.spread', '1') === '1',
  magicBullet:      getSystemProp('gamebooster.pubgm.bullet', '1') === '1',
  droneFov:         parseFloat(getSystemProp('gamebooster.pubgm.fov', '130.0')),
  fpsUnlock:        parseInt(getSystemProp('gamebooster.pubgm.fps', '185')),
  antiBan:          getSystemProp('gamebooster.pubgm.antiban', '1') === '1',
  damageMultiplier: parseFloat(getSystemProp('gamebooster.pubgm.dmg', '10000.0'))
};

console.log('[GameBoosterPRO] PUBGM v4.7.0 UE4 hook engine booting — config:', JSON.stringify(CFG));

// ─── Module Watcher ──────────────────────────────────────────────────────────
function waitForModule(names, callback) {
  const nameList = Array.isArray(names) ? names : [names];

  const check = () => {
    for (const name of nameList) {
      const m = Process.findModuleByName(name);
      if (m && m.size > 0) return m;
    }
    return null;
  };

  const initial = check();
  if (initial) {
    callback(initial);
    return;
  }

  const interval = setInterval(() => {
    const found = check();
    if (found) {
      clearInterval(interval);
      callback(found);
    }
  }, 100);
}

// ─── Shared Symbol Cloaking Helpers ──────────────────────────────────────────
function hookExportsByPrefix(mod, prefixes, label, retVal) {
  const code = (retVal === undefined) ? 0 : retVal;
  let hooked = 0;
  try {
    mod.enumerateExports().forEach((exp) => {
      if (exp.type !== 'function') return;
      if (prefixes.some((p) => exp.name.indexOf(p) === 0)) {
        try {
          Interceptor.replace(exp.address, new NativeCallback(() => code, 'int', []));
          hooked++;
        } catch (e) {}
      }
    });
  } catch (e) {}
  console.log(`[${label}] Cloaked ${hooked} exported symbols in ${mod.name} @ ${mod.base}`);
}

function silenceExportVoid(modName, symName, label) {
  const addr = Module.findExportByName(modName, symName);
  if (!addr) return false;
  try {
    Interceptor.replace(addr, new NativeCallback(() => {}, 'void', []));
    console.log(`[${label}] Silenced ${symName} @ ${addr}`);
    return true;
  } catch (e) {
    return false;
  }
}

function replaceExportInt(modName, symName, retVal, label) {
  const addr = Module.findExportByName(modName, symName);
  if (!addr) return false;
  try {
    Interceptor.replace(addr, new NativeCallback(() => retVal, 'int', []));
    console.log(`[${label}] Overrode ${symName} -> ${retVal} @ ${addr}`);
    return true;
  } catch (e) {
    return false;
  }
}

// ─── 1. Tencent ACE, CrashSight, TGPA, CubeHawk, Sentry & Deku Cloaking ──────
function applyAntiCheatCloak() {
  if (!CFG.antiBan) return;

  // Guard MGPA PrivacyDeviceInfo & Java screen capture listeners
  if (typeof Java !== 'undefined' && Java.available) {
    Java.perform(() => {
      try {
        const PrivacyDeviceInfo = Java.use('com.ihoc.mgpa.toolkit.privacy.PrivacyDeviceInfo');
        const spoofMfg = getSystemProp('debug.game.spoofed_manufacturer', '');
        const spoofModel = getSystemProp('debug.game.spoofed_model', '');
        if (spoofMfg) {
          PrivacyDeviceInfo.getManufacturer.implementation = function () { return spoofMfg; };
        }
        if (spoofModel) {
          PrivacyDeviceInfo.getModel.implementation = function () { return spoofModel; };
        }
      } catch (e) {}

      try {
        const GameScreenshot = Java.use('com.blue.studio.screenshot.GameScreenshot');
        GameScreenshot.onScreenCaptured.implementation = function (_code) {};
      } catch (e) {}
    });
  }

  // Cloak libanogs.so via 4.7.0 AnoSDK* exports (including AnoSDKGetReportData4 & AnoSDKOnRecvSignature)
  waitForModule('libanogs.so', (mod) => {
    hookExportsByPrefix(mod, [
      'AnoSDKGetReportData',
      'AnoSDKDelReportData',
      'AnoSDKSetUserInfo',
      'AnoSDKRegister',
      'AnoSDKRegistInfoListener',
      'AnoSDKOnRecvSignature'
    ], 'ACE-Cloak', 0);
  });

  // Cloak libCrashSight.so via 4.7.0 cs_* / UQM* exports
  waitForModule('libCrashSight.so', (mod) => {
    hookExportsByPrefix(mod, [
      'cs_reportLogInfo',
      'cs_reportStuck',
      'cs_startDumpRoutine',
      'UQMReportLogInfo',
      'UQMCrashAgentReport'
    ], 'CrashSight-Cloak', 0);
  });

  // Synchronize libtgpa.so via actual 4.7.0 MTKFRC_* / TGPA_MTKFRC_* exports
  waitForModule('libtgpa.so', (mod) => {
    const mtkApi = Module.findExportByName('libtgpa.so', 'MTKFRC_GetAPILevel');
    const tgpaSupported = Module.findExportByName('libtgpa.so', 'TGPA_MTKFRC_IsSupported');
    if (mtkApi) {
      try {
        Interceptor.attach(mtkApi, { onLeave(retval) { if (retval.toInt32() <= 0) retval.replace(ptr(1)); } });
      } catch (e) {}
    }
    if (tgpaSupported) {
      try {
        Interceptor.attach(tgpaSupported, { onLeave(retval) { if (retval.toInt32() <= 0) retval.replace(ptr(1)); } });
      } catch (e) {}
    }
    console.log('[TGPA] libtgpa.so 4.7.0 MTKFRC/TGPA exports synchronized @ ' + mod.base);
  });

  // Cloak libsentry-android.so native module enumeration & breadcrumbs (new in 4.7.0)
  waitForModule('libsentry-android.so', () => {
    replaceExportInt('libsentry-android.so',
      'Java_io_sentry_android_ndk_NativeModuleListLoader_nativeLoadModuleList', 0, 'Sentry-Cloak');
    silenceExportVoid('libsentry-android.so',
      'Java_io_sentry_android_ndk_NativeScope_nativeAddBreadcrumb', 'Sentry-Cloak');
  });

  // Neutralize libcubehawk.so emulator detection & monitor telemetry (new in 4.7.0)
  waitForModule('libcubehawk.so', (mod) => {
    replaceExportInt('libcubehawk.so', '_ZN4Hawk13checkEmulatorEPKcS1_', 0, 'Hawk-Cloak');
    console.log('[Hawk-Cloak] libcubehawk.so guarded @ ' + mod.base);
  });

  // Neutralize Play Games Deku overlay screen capture mirror surface (new in 4.7.0)
  waitForModule('libgpdeku.so', () => {
    replaceExportInt('libgpdeku.so',
      'Java_com_google_android_play_deku_screencapture_NativeBridge_nativeGetMirrorSurfaceControl', 0, 'Deku-Cloak');
  });

  // Log libanort.so without patching stale RVAs or breaking tp_syscall_imp
  waitForModule('libanort.so', (mod) => {
    const tpSyscall = Module.findExportByName('libanort.so', 'tp_syscall_imp');
    console.log('[ACE-Cloak] libanort.so mapped @ ' + mod.base + ' (tp_syscall_imp=' + tpSyscall + ')');
  });
}

// ─── 2. Unreal Engine 4.18 Runtime Hooks (libUE4.so — 4.7.0) ─────────────────
function applyUe4CombatHooks() {
  waitForModule('libUE4.so', (mod) => {
    console.log('[PUBGM-UE4] Found libUE4.so @ ' + mod.base + ' (size: ' + mod.size + ', variant: ' + CFG.variant + ')');

    // Evade 4.7.0 screen capture listeners via verified exported JNI symbols
    replaceExportInt('libUE4.so', 'Java_com_epicgames_ue4_GameActivity_nativeIsEnableScreenCaptureNotify', 0, 'PUBGM-UE4');
    silenceExportVoid('libUE4.so', 'Java_com_epicgames_ue4_GameActivity_nativeNotifyCaptureScreen', 'PUBGM-UE4');
    silenceExportVoid('libUE4.so', 'Java_com_blue_studio_screenshot_GameScreenshot_onScreenshotTaken', 'PUBGM-UE4');

    // Optional stripped internal RVAs — only hook if explicitly resolved for this build
    const setFovRva = parseOptionalHexProp('gamebooster.pubgm.rva.set_camera_fov');
    if (CFG.droneFov > 90.0 && setFovRva > 0) {
      try {
        Interceptor.attach(mod.base.add(setFovRva), {
          onEnter(args) { args[1] = ptr(Math.round(CFG.droneFov)); }
        });
        console.log('[PUBGM-UE4] SetCameraFOV hooked @ +0x' + setFovRva.toString(16));
      } catch (e) {}
    }

    const recoilRva = parseOptionalHexProp('gamebooster.pubgm.rva.apply_weapon_recoil');
    if (CFG.noRecoil && recoilRva > 0) {
      try {
        Interceptor.attach(mod.base.add(recoilRva), { onLeave(retval) { retval.replace(ptr(0)); } });
        console.log('[PUBGM-UE4] ApplyWeaponRecoil hooked @ +0x' + recoilRva.toString(16));
      } catch (e) {}
    }

    const spreadRva = parseOptionalHexProp('gamebooster.pubgm.rva.calc_bullet_spread');
    if (CFG.noSpread && spreadRva > 0) {
      try {
        Interceptor.attach(mod.base.add(spreadRva), { onLeave(retval) { retval.replace(ptr(0)); } });
        console.log('[PUBGM-UE4] CalculateBulletSpread hooked @ +0x' + spreadRva.toString(16));
      } catch (e) {}
    }

    const hitDmgRva = parseOptionalHexProp('gamebooster.pubgm.rva.calc_hit_damage');
    if (CFG.damageMultiplier > 1.0 && hitDmgRva > 0) {
      try {
        Interceptor.attach(mod.base.add(hitDmgRva), {
          onLeave(retval) {
            const origVal = retval.toInt32();
            if (origVal > 0) {
              retval.replace(ptr(Math.round(origVal * Math.min(CFG.damageMultiplier, 10000.0))));
            }
          }
        });
        console.log('[PUBGM-UE4] CalculateHitDamage hooked @ +0x' + hitDmgRva.toString(16));
      } catch (e) {}
    }

    // Exported UE4 GameActivity & telemetry JNI hooks verified in 4.7.0 Global & Flash dumps
    const verifiedExports = [
      'Java_com_epicgames_ue4_GameActivity_nativeSetChipSet',
      'Java_com_epicgames_ue4_GameActivity_nativeConsoleCommand',
      'Java_com_epicgames_ue4_GameActivity_nativeSetAndroidVersionInformation',
      'Java_com_ihoc_mgpa_MgpaManager_nativeNotifySystemInfo',
      'Java_com_tencent_hawk_bridge_HawkNative_notifySystemInfo',
      'Java_com_onesdk_special_gem_utils_JNIHelper_SendInfo'
    ];
    for (const sym of verifiedExports) {
      const addr = Module.findExportByName('libUE4.so', sym);
      if (addr) {
        console.log(`[PUBGM-UE4] Export located: ${sym} @ ${addr}`);
      }
    }
  });
}

// ─── Entry Point ─────────────────────────────────────────────────────────────
applyAntiCheatCloak();
applyUe4CombatHooks();
console.log('[GameBoosterPRO] PUBGM v4.7.0 hook engine initialized.');
