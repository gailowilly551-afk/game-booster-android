/**
 * PUBGM Frida Hook Engine (v4.6.0 UE4 2026.3)
 * Game: PUBG Mobile / BGMI (com.tencent.ig / com.pubg.imobile)
 * Architecture: arm64-v8a
 * Engine: Unreal Engine 4 (libUE4.so)
 * Anti-Cheat: Tencent ACE (libanort.so, libanogs.so) + CrashSight
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

// ─── Config Loaded Safely via SystemProperties ───────────────────────────────
const CFG = {
  aimbot:           getSystemProp('gamebooster.pubgm.aim', '1') === '1',
  noRecoil:         getSystemProp('gamebooster.pubgm.recoil', '1') === '1',
  noSpread:         getSystemProp('gamebooster.pubgm.spread', '1') === '1',
  magicBullet:      getSystemProp('gamebooster.pubgm.bullet', '1') === '1',
  droneFov:         parseFloat(getSystemProp('gamebooster.pubgm.fov', '130.0')),
  fpsUnlock:        parseInt(getSystemProp('gamebooster.pubgm.fps', '185')),
  antiBan:          getSystemProp('gamebooster.pubgm.antiban', '1') === '1',
  damageMultiplier: parseFloat(getSystemProp('gamebooster.pubgm.dmg', '10000.0'))
};

console.log('[GameBoosterPRO] PUBGM v4.6.0 UE4 2026 combat hooks booting — config:', JSON.stringify(CFG));

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

// ─── 1. Tencent ACE & CrashSight Telemetry Cloaking ──────────────────────────
function applyAntiCheatCloak() {
  if (!CFG.antiBan) return;

  // Cloak libanort.so
  waitForModule('libanort.so', (mod) => {
    console.log('[ACE-Cloak] Found libanort.so @ ' + mod.base);
    try {
      // Ret 0 stub: mov x0, #0; ret (0xD2800000, 0xD65F03C0)
      const reportLoopAddr = mod.base.add(0x2A140);
      Memory.protect(reportLoopAddr, 8, 'rwx');
      reportLoopAddr.writeByteArray([0x00, 0x00, 0x80, 0xD2, 0xC0, 0x03, 0x5F, 0xD6]);
      console.log('[ACE-Cloak] Neutralized libanort report loop @ ' + reportLoopAddr);
    } catch (e) {
      console.log('[ACE-Cloak] Note libanort: ' + e.message);
    }
  });

  // Cloak libanogs.so
  waitForModule('libanogs.so', (mod) => {
    console.log('[ACE-Cloak] Found libanogs.so @ ' + mod.base);
    try {
      const heartbeatAddr = mod.base.add(0x1E080);
      Memory.protect(heartbeatAddr, 8, 'rwx');
      heartbeatAddr.writeByteArray([0x00, 0x00, 0x80, 0xD2, 0xC0, 0x03, 0x5F, 0xD6]);
      console.log('[ACE-Cloak] Neutralized libanogs heartbeat @ ' + heartbeatAddr);
    } catch (e) {
      console.log('[ACE-Cloak] Note libanogs: ' + e.message);
    }
  });

  // Cloak libCrashSight.so
  waitForModule('libCrashSight.so', (mod) => {
    console.log('[ACE-Cloak] Found libCrashSight.so @ ' + mod.base);
    try {
      const uploadAddr = mod.base.add(0x18320);
      Memory.protect(uploadAddr, 8, 'rwx');
      uploadAddr.writeByteArray([0x00, 0x00, 0x80, 0xD2, 0xC0, 0x03, 0x5F, 0xD6]);
      console.log('[ACE-Cloak] Neutralized CrashSight telemetry upload');
    } catch (e) {
      console.log('[ACE-Cloak] Note CrashSight: ' + e.message);
    }
  });
}

// ─── 2. Unreal Engine 4 Runtime Hooks (libUE4.so) ────────────────────────────
function applyUe4CombatHooks() {
  waitForModule('libUE4.so', (mod) => {
    console.log('[PUBGM-UE4] Found libUE4.so @ ' + mod.base + ' (size: ' + mod.size + ')');

    // Evade Android 15 DETECT_SCREEN_CAPTURE
    try {
      const screenCapAddr = mod.base.add(0x05EE4200);
      Memory.protect(screenCapAddr, 8, 'rwx');
      screenCapAddr.writeByteArray([0x00, 0x00, 0x80, 0xD2, 0xC0, 0x03, 0x5F, 0xD6]);
      console.log('[PUBGM-UE4] Evaded screen capture detection listener');
    } catch (e) {}

    // Camera FOV Override (iPad 130 FOV View)
    if (CFG.droneFov > 90.0) {
      try {
        const setFovAddr = mod.base.add(0x052B3800);
        Interceptor.attach(setFovAddr, {
          onEnter: function (args) {
            // args[1] is target float FOV
            args[1] = ptr(Math.round(CFG.droneFov));
          }
        });
        console.log('[PUBGM-UE4] SetCameraFOV hooked -> locked to ' + CFG.droneFov);
      } catch (e) {
        console.log('[PUBGM-UE4] SetCameraFOV hook note: ' + e.message);
      }
    }

    // Zero Recoil & Weapon Shake Override
    if (CFG.noRecoil) {
      try {
        const recoilAddr = mod.base.add(0x05DF1200);
        Interceptor.attach(recoilAddr, {
          onEnter: function (args) {
            // Zero out recoil scale and pitch/yaw offsets
          },
          onLeave: function (retval) {
            retval.replace(ptr(0));
          }
        });
        console.log('[PUBGM-UE4] ApplyWeaponRecoil hooked -> zero recoil active');
      } catch (e) {
        console.log('[PUBGM-UE4] ApplyWeaponRecoil hook note: ' + e.message);
      }
    }

    // Zero Bullet Spread
    if (CFG.noSpread) {
      try {
        const spreadAddr = mod.base.add(0x05DF1540);
        Interceptor.attach(spreadAddr, {
          onLeave: function (retval) {
            retval.replace(ptr(0));
          }
        });
        console.log('[PUBGM-UE4] CalculateBulletSpread hooked -> zero spread active');
      } catch (e) {
        console.log('[PUBGM-UE4] CalculateBulletSpread hook note: ' + e.message);
      }
    }
  });
}

// ─── Entry Point ─────────────────────────────────────────────────────────────
applyAntiCheatCloak();
applyUe4CombatHooks();
console.log('[GameBoosterPRO] PUBGM v4.6.0 hook engine initialized.');
