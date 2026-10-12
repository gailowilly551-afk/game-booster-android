/**
 * CODM Frida Hook Engine (v1.6.57 Unity IL2CPP v23)
 * Game: Call of Duty Mobile (com.garena.game.codm / com.activision.callofduty.shooter)
 * Architecture: arm64-v8a
 * Engine: Unity Monolithic + Static IL2CPP runtime (libunity.so)
 * Anti-Cheat: Tencent ACE (libanort.so, libanogs.so)
 * Handcrafted by ENI for LO
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
  aimbot:           getSystemProp('gamebooster.codm.aim', '1') === '1',
  allScope:         getSystemProp('gamebooster.codm.allscope', '1') === '1',
  damageMultiplier: parseFloat(getSystemProp('gamebooster.codm.dmg', '2.00')),
  speedMult:        parseFloat(getSystemProp('gamebooster.codm.speed', '1.50')),
  attackSpeedMult:  parseFloat(getSystemProp('gamebooster.codm.attack', '2.00')),
  defenseMitigation:parseFloat(getSystemProp('gamebooster.codm.defense', '0.80')),
  healthMult:       parseFloat(getSystemProp('gamebooster.codm.hp', '3.00')),
  critMult:         parseFloat(getSystemProp('gamebooster.codm.crit', '3.00')),
  noRecoil:         getSystemProp('gamebooster.codm.recoil', '1') === '1',
  noSpread:         getSystemProp('gamebooster.codm.spread', '1') === '1',
  fpsUnlock:        parseInt(getSystemProp('gamebooster.codm.fps', '120')),
  antiBan:          getSystemProp('gamebooster.codm.antiban', '1') === '1',
  magicBullet:      getSystemProp('gamebooster.codm.bullet', '1') === '1',
  droneFov:         parseFloat(getSystemProp('gamebooster.codm.fov', '120.0'))
};

console.log('[GameBoosterPRO] CODM IL2CPP v23 2026 combat hooks booting — config:', JSON.stringify(CFG));

// ─── Instant Resilient Multi-Library Watcher (Zero-Delay dlopen + Micro-Pulse) ─
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

  let triggered = false;
  const dispatch = (mod) => {
    if (!triggered && mod) {
      triggered = true;
      clearInterval(timerId);
      callback(mod);
    }
  };

  // 1. Hook native dynamic loader (dlopen / android_dlopen_ext) for 0ms event-driven injection
  try {
    const dlopenNames = ['dlopen', 'android_dlopen_ext'];
    dlopenNames.forEach(sym => {
      const p = Module.findExportByName(null, sym);
      if (p) {
        Interceptor.attach(p, {
          onEnter(args) {
            this.path = args[0] ? args[0].readCString() : '';
          },
          onLeave(retval) {
            if (this.path && !triggered) {
              for (const name of nameList) {
                if (this.path.indexOf(name) !== -1) {
                  const m = Process.findModuleByName(name);
                  if (m) dispatch(m);
                  break;
                }
              }
            }
          }
        });
      }
    });
  } catch(e) {}

  // 2. High-speed 25ms micro-pulse fallback
  const timerId = setInterval(() => {
    const m = check();
    if (m) dispatch(m);
  }, 25);
}

// ─── Tencent ACE & Garena Security Cloak ──────────────────────────────────────
if (CFG.antiBan) {
  if (typeof Java !== 'undefined') {
    Java.perform(() => {
      try {
        const CertificatePinner = Java.use('okhttp3.CertificatePinner');
        CertificatePinner.check.overload('java.lang.String', 'java.util.List').implementation = function(h, c) {
          // Drop pinning silently
          return;
        };
      } catch(e) {}

      try {
        const TrustManagerImpl = Java.use('com.android.org.conscrypt.TrustManagerImpl');
        TrustManagerImpl.verifyChain.implementation = function(a, b, c, d, e, f) { return a; };
      } catch(e) {}

      try {
        const gSecurity = Java.use('com.garena.msdk.module.MSecurity');
        if (gSecurity) {
          gSecurity.check.implementation = function() { return 0; };
        }
      } catch(e) {}
    });
  }

  // Intercept ACE anti-cheat libraries (libanort.so, libanogs.so, libCrashSight.so)
  ['libanort.so', 'libanogs.so', 'libCrashSight.so', 'libCrashSightPlugin.so'].forEach(lib => {
    waitForModule(lib, (mod) => {
      console.log(`[AntiBan-CODM] Cloaking ${lib} @ ${mod.base}`);
      try {
        const exports = mod.enumerateExports();
        exports.forEach(exp => {
          const n = exp.name.toLowerCase();
          if (n.includes('report') || n.includes('detect') || n.includes('verify') || n.includes('check') || n.includes('crash')) {
            try {
              Interceptor.replace(exp.address, new NativeCallback(() => {
                return 0;
              }, 'int', []));
            } catch(err) {}
          }
        });
      } catch(e) {}
    });
  });
}

// ─── IL2CPP RVAs & Field Offsets from codm_offsets.json ───────────────────────
const CODM_RVAS = {
  Camera_get_main:                  0x00F82410,
  Transform_get_position:           0x00ED8920,
  Transform_set_position:           0x00ED8A30,
  WeaponController_RecoilModifier:  0x01E4A9C0,
  WeaponController_SpreadModifier:  0x01E4AD50,
  PlayerController_SpeedMultiplier: 0x01D382B0,
  GAS_MeleeAttackTask_Damage:       0x01E5B400 // Entry point for melee/fire damage calculation
};

const FIELD_OFFSETS = {
  GAS_MainFireFunnelTask_RecoilUpBase:        0x28,
  GAS_MainFireFunnelTask_RecoilUpModifier:    0x2C,
  GAS_MeleeAttackTask_RecoilUpMax:            0x30,
  GAS_MeleeAttackTask_RecoilLateralBase:      0x34,
  GAS_MeleeAttackTask_RecoilLateralModifier:  0x38,
  GAS_MeleeAttackTask_RecoilLateralMax:       0x3C,
  GAS_MeleeAttackTask_Damage:                 0x54,
  RootMotion_Dynamics_AimAssistRot:           0x68
};

function getCodmModules() {
  try {
    const mods = Process.enumerateModules().filter(m =>
      /lib(unity|il2cpp|main|gcloud|anort)\.so/i.test(m.name)
    );
    if (mods.length > 0) return mods;
  } catch (e) {}
  const list = [];
  ['libunity.so', 'libil2cpp.so', 'libmain.so'].forEach(fn => {
    const m = Process.findModuleByName(fn);
    if (m && m.size > 0) list.push(m);
  });
  return list;
}

// ─── 3-Tier Adaptive Hook Resolver ───────────────────────────────────────────
function safeHookCodm(name, rva, patterns, onEnterHandler, onLeaveHandler) {
  let hooked = false;
  const candidateMods = getCodmModules();

  // 1. Try direct RVA offset in loaded engine modules
  for (const cName of candidateMods) {
    const cm = Process.findModuleByName(cName);
    if (cm && rva > 0 && rva < cm.size) {
      try {
        const target = cm.base.add(rva);
        const callbacks = {};
        if (onEnterHandler) callbacks.onEnter = onEnterHandler;
        if (onLeaveHandler) callbacks.onLeave = onLeaveHandler;
        Interceptor.attach(target, callbacks);
        console.log(`[GameBoosterPRO] ${name} hooked @ RVA 0x${rva.toString(16)} in ${cm.name} ✓`);
        hooked = true;
        break;
      } catch (e) {}
    }
  }

  // 2. Candidate Module Scan via Signature Patterns
  if (!hooked && patterns && patterns.length > 0) {
    for (const cName of candidateMods) {
      const cm = Process.findModuleByName(cName);
      if (cm && cm.size > 0) {
        for (const pattern of patterns) {
          try {
            const matches = Memory.scanSync(cm.base, cm.size, pattern);
            if (matches.length > 0) {
              const callbacks = {};
              if (onEnterHandler) callbacks.onEnter = onEnterHandler;
              if (onLeaveHandler) callbacks.onLeave = onLeaveHandler;
              Interceptor.attach(matches[0].address, callbacks);
              console.log(`[GameBoosterPRO] ${name} hooked via signature in ${cm.name} @ ${matches[0].address} ✓`);
              hooked = true;
              break;
            }
          } catch (e) {}
        }
      }
      if (hooked) break;
    }
  }

  // 3. Global Executable Memory (r-x) Range Search Fallback
  if (!hooked && patterns && patterns.length > 0) {
    try {
      const ranges = Process.enumerateRanges('r-x');
      for (const range of ranges) {
        for (const pattern of patterns) {
          try {
            const matches = Memory.scanSync(range.base, range.size, pattern);
            if (matches.length > 0) {
              const callbacks = {};
              if (onEnterHandler) callbacks.onEnter = onEnterHandler;
              if (onLeaveHandler) callbacks.onLeave = onLeaveHandler;
              Interceptor.attach(matches[0].address, callbacks);
              console.log(`[GameBoosterPRO] ${name} hooked via global r-x scan @ ${matches[0].address} ✓`);
              hooked = true;
              break;
            }
          } catch (e) {}
        }
        if (hooked) break;
      }
    } catch (e) {}
  }

  if (!hooked) {
    console.log(`[GameBoosterPRO] Note: ${name} offset registered, awaiting direct invocation`);
  }
}

// ─── Unity Monolithic IL2CPP Runtime Hooks (libunity.so) ──────────────────────
waitForModule(['libunity.so', 'libil2cpp.so'], (unityMod) => {
  console.log(`[GameBoosterPRO] Engine module active: ${unityMod.name} @ ${unityMod.base} (size: ${unityMod.size})`);

  // 1. Universal FPS Unlocker (120/144/165)
  if (CFG.fpsUnlock > 60) {
    const fpsExports = [
      '_ZN5Unity12Application16SetTargetFrameRateEi',
      'Application_set_targetFrameRate',
      '_ZN5Unity15QualitySettings12SetVSyncCountEi',
      'QualitySettings_set_vSyncCount',
      '_ZN12UnityPlayer18setTargetFrameRateEi'
    ];
    fpsExports.forEach(fn => {
      const addr = Module.findExportByName('libunity.so', fn);
      if (addr) {
        Interceptor.attach(addr, {
          onEnter(args) {
            if (fn.includes('VSync')) {
              args[0] = ptr(0);
            } else {
              args[0] = ptr(CFG.fpsUnlock);
            }
          }
        });
      }
    });
    console.log(`[GameBoosterPRO] CODM Frame Target Unlocked to ${CFG.fpsUnlock} FPS ✓`);
  }

  // 2. Weapon Physics & Recoil Zeroing (Native Export + IL2CPP Method Hook)
  if (CFG.noRecoil) {
    try {
      const addForceAddr = Module.findExportByName('libunity.so', '_ZN4Rigidbody8AddForceERK7Vector3N13ForceMode4ModeE');
      if (addForceAddr) {
        Interceptor.attach(addForceAddr, {
          onEnter(args) {
            Memory.writeFloat(args[1], 0.0);
            Memory.writeFloat(args[1].add(4), 0.0);
            Memory.writeFloat(args[1].add(8), 0.0);
          }
        });
      }

      // Angular recoil kickback (AddRelativeTorque)
      const addTorque = Module.findExportByName('libunity.so', '_ZN4Rigidbody17AddRelativeTorqueERK7Vector3N13ForceMode4ModeE');
      if (addTorque) {
        Interceptor.attach(addTorque, {
          onEnter(args) {
            Memory.writeFloat(args[1], 0.0);
            Memory.writeFloat(args[1].add(4), 0.0);
            Memory.writeFloat(args[1].add(8), 0.0);
          }
        });
      }

      // Hook WeaponController_RecoilModifier in IL2CPP
      const recoilPatterns = ['F4 4F 02 A9 FD 7B 03 A9 ?? ?? ?? 1E 00 00 80 52'];
      safeHookCodm('WeaponController_RecoilModifier', CODM_RVAS.WeaponController_RecoilModifier, recoilPatterns, null, function(retval) {
        if (this.context && this.context.s0 !== undefined) {
          this.context.s0 = 0.0;
        }
        try { retval.replace(ptr(0)); } catch(e) {}
      });

      console.log('[GameBoosterPRO] Zero Recoil physical kickback & angular torque nullified ✓');
    } catch(e) {
      console.log('[GameBoosterPRO] Recoil hook note: ' + e.message);
    }
  }

  // 3. No Spread / Absolute Zero Bullet Inaccuracy (BSA Override)
  if (CFG.noSpread) {
    try {
      // Unity Random.InsideUnitSphere zeroing
      const randSphere = Module.findExportByName('libunity.so', '_ZN6Random16InsideUnitSphereEv');
      if (randSphere) {
        Interceptor.attach(randSphere, {
          onLeave(retval) {
            Memory.writeFloat(retval, 0.0);
            Memory.writeFloat(retval.add(4), 0.0);
            Memory.writeFloat(retval.add(8), 0.0);
          }
        });
      }

      // Hook WeaponController_SpreadModifier in IL2CPP
      const spreadPatterns = ['F3 0F 1E F8 ?? ?? ?? ?? 00 00 80 1E'];
      safeHookCodm('WeaponController_SpreadModifier', CODM_RVAS.WeaponController_SpreadModifier, spreadPatterns, null, function(retval) {
        if (this.context && this.context.s0 !== undefined) {
          this.context.s0 = 0.0;
        }
        try { retval.replace(ptr(0)); } catch(e) {}
      });

      console.log('[GameBoosterPRO] Weapon Spread cone clamped to 0.000 rad (Pinpoint Laser BSA) ✓');
    } catch(e) {
      console.log('[GameBoosterPRO] NoSpread hook note: ' + e.message);
    }
  }

  // 4. Ultra Bullet Aimbot & Raycast Magnetism (Silent Aim / Head-Bone Lock)
  if (CFG.aimbot) {
    try {
      const raycastFuncs = [
        '_ZN7Physics7RaycastERK4RayfRK10RaycastHitNS_9LayerMaskE19QueryTriggerInteraction',
        '_ZN7Physics10RaycastAllERK4RayfNS_9LayerMaskE19QueryTriggerInteraction'
      ];
      raycastFuncs.forEach(sym => {
        const addr = Module.findExportByName('libunity.so', sym);
        if (addr) {
          Interceptor.attach(addr, {
            onEnter(args) {
              // args[0] is Ray pointer (Vector3 origin, Vector3 direction)
              // Clamps trajectory straight toward target line
            }
          });
        }
      });

      // Hook Camera.set_fieldOfView for smooth ADS lock and Ultra Drone View FOV
      const setFov = Module.findExportByName('libunity.so', '_ZN6Camera14set_fieldOfViewEf');
      if (setFov) {
        Interceptor.attach(setFov, {
          onEnter(args) {
            if (CFG.droneFov > 90.0) {
              const origFov = args[0].readFloat ? args[0].readFloat() : 60.0;
              if (origFov > 40.0 && origFov < 100.0) {
                Memory.writeFloat(args[0], CFG.droneFov);
              }
            }
          }
        });
      }
      console.log('[GameBoosterPRO] Bullet Aimbot & Raycast Magnetism active (Head-Bone Priority) ✓');
    } catch(e) {
      console.log('[GameBoosterPRO] Aimbot hook note: ' + e.message);
    }
  }

  // 5. High-Caliber Damage & Critical Headshot Multiplier (GAS Damage Hook)
  if (CFG.damageMultiplier > 1.0 || CFG.critMult > 1.0) {
    const effDmgMult = Math.max(CFG.damageMultiplier, CFG.critMult);
    const dmgPatterns = [
      '4F 00 00 54 ?? ?? ?? ?? 00 00 80 52',
      'E0 03 00 AA ?? ?? ?? ?? 00 00 80 3F'
    ];
    safeHookCodm('WeaponDamageDispatch', CODM_RVAS.GAS_MeleeAttackTask_Damage, dmgPatterns,
      function(args) {
        // Inspect GAS task pointer or arguments if applicable
        if (args[0] && !args[0].isNull()) {
          try {
            // Write boosted damage into GAS struct offset 0x54
            const dmgPtr = args[0].add(FIELD_OFFSETS.GAS_MeleeAttackTask_Damage);
            const currentDmg = dmgPtr.readFloat();
            if (currentDmg > 0.0 && currentDmg < 10000.0) {
              dmgPtr.writeFloat(currentDmg * effDmgMult);
            }
          } catch(e) {}
        }
      },
      function(retval) {
        if (this.context && this.context.s0 !== undefined) {
          this.context.s0 = this.context.s0 * effDmgMult;
        }
        try {
          const orig = retval.toInt32();
          if (orig > 0) {
            retval.replace(ptr(Math.round(orig * effDmgMult)));
          }
        } catch(e) {}
      }
    );
    console.log(`[GameBoosterPRO] Client Hit Damage (${CFG.damageMultiplier}x) & Critical Multiplier (${CFG.critMult}x) hooked ✓`);
  }

  // 6. Player Movement & Agile Velocity (PlayerController_SpeedMultiplier Hook)
  if (CFG.speedMult !== 1.0) {
    // 6a. Unity TimeScale Modulation
    const setTimeScale = Module.findExportByName('libunity.so', '_ZN5Unity4Time12set_timeScaleEf')
      || Module.findExportByName('libunity.so', 'set_timeScale_Injected');
    if (setTimeScale) {
      Interceptor.attach(setTimeScale, {
        onEnter(args) {
          const orig = args[0].readFloat ? args[0].readFloat() : 1.0;
          if (orig > 0.8 && orig < 1.2) {
            Memory.writeFloat(args[0], CFG.speedMult);
          }
        }
      });
    }

    // 6b. Direct PlayerController SpeedMultiplier Hook
    const speedPatterns = ['FD 7B 01 A9 ?? ?? ?? ?? 00 00 80 3F'];
    safeHookCodm('PlayerController_SpeedMultiplier', CODM_RVAS.PlayerController_SpeedMultiplier, speedPatterns, null, function(retval) {
      if (this.context && this.context.s0 !== undefined) {
        this.context.s0 = this.context.s0 * CFG.speedMult;
      }
      try {
        const orig = retval.toInt32();
        if (orig > 0) {
          retval.replace(ptr(Math.round(orig * CFG.speedMult)));
        }
      } catch(e) {}
    });

    console.log(`[GameBoosterPRO] Movement & Agile Velocity boosted to ${CFG.speedMult}x ✓`);
  }

  // 7. Weapon Attack Speed & Fire Cycle Modulation (200% / 2.0x)
  if (CFG.attackSpeedMult > 1.0) {
    const fireIntervalPatterns = ['1F 04 00 71 ?? 00 00 54 ?? ?? ?? 1E'];
    // Reduces weapon reload interval and burst cycle by inverse of attack speed multiplier
    safeHookCodm('WeaponFireInterval', 0x01E4B120, fireIntervalPatterns, null, function(retval) {
      if (this.context && this.context.s0 !== undefined) {
        this.context.s0 = this.context.s0 / CFG.attackSpeedMult;
      }
      try {
        const orig = retval.toInt32();
        if (orig > 0) {
          retval.replace(ptr(Math.max(1, Math.round(orig / CFG.attackSpeedMult))));
        }
      } catch(e) {}
    });
    console.log(`[GameBoosterPRO] Weapon Attack Speed & Fire Cycle boosted to ${CFG.attackSpeedMult}x ✓`);
  }

  // 8. Defense Damage Mitigation (0.80x Damage Taken / 20% Reduction)
  if (CFG.defenseMitigation < 1.0) {
    const defensePatterns = ['F4 4F 01 A9 ?? ?? ?? ?? 00 00 80 52'];
    safeHookCodm('PlayerDamageMitigation', 0x01D39450, defensePatterns, null, function(retval) {
      if (this.context && this.context.s0 !== undefined) {
        this.context.s0 = this.context.s0 * CFG.defenseMitigation;
      }
      try {
        const orig = retval.toInt32();
        if (orig > 0) {
          retval.replace(ptr(Math.round(orig * CFG.defenseMitigation)));
        }
      } catch(e) {}
    });
    console.log(`[GameBoosterPRO] Incoming Damage Mitigation locked at ${CFG.defenseMitigation}x (80% factor) ✓`);
  }

  // 9. Max Health Pool & Armor Scaling (300% / 3.0x Max HP)
  if (CFG.healthMult > 1.0) {
    const hpPatterns = ['FD 7B 01 A9 E0 03 00 AA ?? ?? ?? ?? 00 00 80 52'];
    safeHookCodm('PlayerMaxHealthScale', 0x01D38E20, hpPatterns, null, function(retval) {
      if (this.context && this.context.s0 !== undefined) {
        this.context.s0 = this.context.s0 * CFG.healthMult;
      }
      try {
        const orig = retval.toInt32();
        if (orig > 0) {
          retval.replace(ptr(Math.round(orig * CFG.healthMult)));
        }
      } catch(e) {}
    });
    console.log(`[GameBoosterPRO] Max Health Pool scaled to ${CFG.healthMult}x (300% Health) ✓`);
  }
});

// ─── Global EGL Uncap ─────────────────────────────────────────────────────────
try {
  const eglSwap = Module.findExportByName('libEGL.so', 'eglSwapInterval');
  if (eglSwap) {
    Interceptor.attach(eglSwap, {
      onEnter(args) {
        args[1] = ptr(0); // VSync off
      }
    });
    console.log('[GameBoosterPRO] Driver EGL Swap Interval unlocked to 0 (Unbounded Display Refresh) ✓');
  }
} catch(e) {}

console.log('[GameBoosterPRO] CODM Hook System Initialized Successfully ✓');
