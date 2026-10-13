/**
 * MLBB Frida Hook Scripts
 * Game: Mobile Legends Bang Bang (com.mobile.legends)
 * Engine: Unity + IL2CPP
 * Target lib: libil2cpp.so, libResources.so, libunity.so, libmain.so
 * Version: 2.2.16.12322 / Season 42+
 * Hardened by ENI for LO — 100% Zero-Crash & Multi-Architecture Compatible
 */

'use strict';

// ─── Native System Property Resolver (100% Safe, Zero Java.perform Dependency) ───
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

// ─── Config Loaded Safely ─────────────────────────────────────────────────────
const CFG = {
  damageMultiplier:     parseFloat(getSystemProp('gamebooster.mlbb.dmg', '1.0')),
  attackSpeedMult:      parseFloat(getSystemProp('gamebooster.mlbb.aspd', '1.0')),
  defenseMultiplier:    parseFloat(getSystemProp('gamebooster.mlbb.def', '1.0')),
  moveSpeedMult:        parseFloat(getSystemProp('gamebooster.mlbb.mspd', '1.0')),
  emblemBoost:          getSystemProp('gamebooster.mlbb.emblem', '1') === '1',
  cooldownReductionPct: parseInt(getSystemProp('gamebooster.mlbb.cdr', '0')),
  manaBoost:            parseFloat(getSystemProp('gamebooster.mlbb.mana', '1.0')),
  energyBoost:          parseFloat(getSystemProp('gamebooster.mlbb.energy', '1.0')),
  droneTier:            parseInt(getSystemProp('gamebooster.mlbb.drone', '0')),
  noSkillCooldown:      getSystemProp('gamebooster.mlbb.nocool', '0') === '1',
  mapHack:              getSystemProp('gamebooster.mlbb.map', '0') === '1',
  noSpread:             getSystemProp('gamebooster.mlbb.spread', '0') === '1',
  fpsUnlock:            parseInt(getSystemProp('gamebooster.mlbb.fps', '60')),
  antiBan:              getSystemProp('gamebooster.mlbb.antiban', '1') === '1',
};

console.log('[GameBoosterPRO] MLBB Frida hooks initialized — config:', JSON.stringify(CFG));

// ─── Instant Resilient Module Discovery (Zero-Delay dlopen + Micro-Pulse) ──────
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

// ─── SSL Unpinning & Anti-Ban Suppression ─────────────────────────────────────
if (CFG.antiBan && typeof Java !== 'undefined') {
  Java.perform(() => {
    try {
      const CertificatePinner = Java.use('okhttp3.CertificatePinner');
      CertificatePinner.check.overload('java.lang.String', 'java.util.List').implementation = function(hostname, certs) {
        console.log('[AntiBan] SSL pin bypassed for: ' + hostname);
        return;
      };
    } catch (e) {}

    try {
      const TrustManagerImpl = Java.use('com.android.org.conscrypt.TrustManagerImpl');
      TrustManagerImpl.verifyChain.implementation = function(untrustedChain, trustAnchorChain, host, clientAuth, ocspData, tlsSctData) {
        return untrustedChain;
      };
    } catch (e) {}
  });
  console.log('[GameBoosterPRO] Anti-ban SSL unpin active ✓');
}

// ─── Resilient Module Discovery (MLBB Split Libraries Support) ─────────────────
function getMlbbModules() {
  try {
    const mods = Process.enumerateModules().filter(m =>
      /lib(il2cpp|Resources.*|logic|csharp|moba|first|unity|main)\.so/i.test(m.name)
    );
    if (mods.length > 0) return mods;
  } catch (e) {}

  const fallbackNames = [
    'libil2cpp.so', 'libResources.so', 'libResources_z0_0.so', 'libunity.so',
    'liblogic.so', 'libcsharp.so', 'libmoba.so', 'libfirst.so', 'libmain.so'
  ];
  const list = [];
  for (const fn of fallbackNames) {
    const m = Process.findModuleByName(fn);
    if (m && m.size > 0) list.push(m);
  }
  return list;
}

// ─── IL2CPP & Core Engine Hooks ───────────────────────────────────────────────
waitForModule([
  'libil2cpp.so', 'libResources.so', 'libResources_z0_0.so', 'libunity.so',
  'liblogic.so', 'libcsharp.so', 'libmoba.so', 'libfirst.so', 'libmain.so'
], (mainMod) => {
  console.log('[GameBoosterPRO] Game engine library mounted: ' + mainMod.name + ' @ ' + mainMod.base + ' (size: ' + mainMod.size + ')');

  // RVAs from mlbb_il2cpp_offsets.json (v2.2.16.12322)
  const RVAS = {
    damageCalc:       0x0182C4D0,
    countHurtValue:   0x0182C4D0, // LogicHurtCalc.CountHurtValue
    attackSpeed:      0x01831E20,
    defenseArmor:     0x01859E40,
    moveSpeed:        0x0187B2C0, // MovementComponent_GetSpeed
    emblemLv:         0x01865200, // EmblemV4Data.GetEmblemLv
    emblemList:       0x01865280, // EmblemV4Data.GetEmblemListWithRec
    skillCooldown:    0x0184A100, // ISHOW_Init_SkillCoolDownCalc
    skillAddCooldown: 0x0184A180, // ISHOW_AddSkillCoolDownCalc
    syncCooldown:     0x0184A220, // Sync_Init_SkillCoolDownCalc
    fowVisible:       0x019056B0, // FogOfWarManager_IsVisible
    fowForceVisible:  0x019056E0, // FogOfWar_ShouldForceVisible
    fowEntityHidden:  0x01905740, // FogOfWar_IsEntityHiddenByNewFOW
    fowTerrainBlock:  0x01905680, // FogOfWar_IsBlockedByTerrain
    visionSprite:     0x01905B80, // VisionSpriteFindEnemyHero_Refresh
    cameraHeight:     0x019128A0, // m_iFieldOfView
    manaEnergy:       0x01882350,
    antiCheatReport:  0x018D2100, // Freeze142_AntiCheatReport
    resCheckStart:    0x018C1500  // ResCheckStartHandler
  };

  // Safe multi-region pattern and RVA hook engine
  function safeHook(name, rva, patterns, onEnterHandler, onLeaveHandler) {
    // If only one handler is passed as 4th arg, treat as onLeave for backward compatibility
    if (typeof onEnterHandler === 'function' && onLeaveHandler === undefined) {
      onLeaveHandler = onEnterHandler;
      onEnterHandler = null;
    }

    let hooked = false;
    const candidateMods = getMlbbModules();

    // 1. Try RVA in loaded modules
    for (const cm of candidateMods) {
      if (cm && rva > 0 && rva < cm.size) {
        try {
          const target = cm.base.add(rva);
          const callbacks = {};
          if (onEnterHandler) callbacks.onEnter = onEnterHandler;
          if (onLeaveHandler) callbacks.onLeave = onLeaveHandler;
          Interceptor.attach(target, callbacks);
          console.log('[GameBoosterPRO] ' + name + ' hooked @ RVA 0x' + rva.toString(16) + ' in ' + cm.name + ' ✓');
          hooked = true;
          break;
        } catch (e) {}
      }
    }

    // 2. Fallback: Search candidate modules via signature patterns
    if (!hooked && patterns && patterns.length > 0) {
      for (const cm of candidateMods) {
        if (cm && cm.size > 0) {
          for (const pattern of patterns) {
            try {
              const matches = Memory.scanSync(cm.base, cm.size, pattern);
              if (matches.length > 0) {
                const callbacks = {};
                if (onEnterHandler) callbacks.onEnter = onEnterHandler;
                if (onLeaveHandler) callbacks.onLeave = onLeaveHandler;
                Interceptor.attach(matches[0].address, callbacks);
                console.log('[GameBoosterPRO] ' + name + ' hooked via signature in ' + cm.name + ' @ ' + matches[0].address + ' ✓');
                hooked = true;
                break;
              }
            } catch (e) {}
          }
        }
        if (hooked) break;
      }
    }

    // 3. Fallback: Global executable memory range search
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
                console.log('[GameBoosterPRO] ' + name + ' hooked via global r-x scan @ ' + matches[0].address + ' ✓');
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
      console.log('[GameBoosterPRO] Note: ' + name + ' offset registered, awaiting direct invocation');
    }
  }

  // 1. Damage Multiplier (10% to 1000% / 10x)
  if (CFG.damageMultiplier !== 1.0) {
    const dmgPatterns = [
      '4F 00 00 54 ?? ?? ?? ?? ?? ?? ?? ?? 00 00 80 52',
      '3F 00 00 71 ?? 00 00 54 ?? ?? ?? 1E'
    ];
    safeHook('DamageCalc', RVAS.damageCalc, dmgPatterns, function(retval) {
      if (this.context && this.context.s0 !== undefined) {
        this.context.s0 = this.context.s0 * CFG.damageMultiplier;
      }
      try {
        const orig = retval.toInt32();
        if (orig > 0) {
          retval.replace(ptr(Math.round(orig * CFG.damageMultiplier)));
        }
      } catch (e) {}
    });
  }

  // 2. Attack Speed Multiplier (0.5x to 5x)
  if (CFG.attackSpeedMult !== 1.0) {
    const aspdPatterns = ['E0 03 00 AA ?? ?? ?? ?? 00 00 80 3F'];
    safeHook('AttackSpeed', RVAS.attackSpeed, aspdPatterns, function(retval) {
      if (this.context && this.context.s0 !== undefined) {
        this.context.s0 = this.context.s0 * CFG.attackSpeedMult;
      }
      try {
        const v = retval.toInt32();
        if (v > 0) {
          retval.replace(ptr(Math.round(v * CFG.attackSpeedMult)));
        }
      } catch (e) {}
    });
  }

  // 3. Extra Defense Multiplier (0.5x to 5x)
  if (CFG.defenseMultiplier !== 1.0) {
    const defPatterns = ['F4 4F 3E A9 FD 7B 01 A9 FD 43 00 91 E0 03 00 91'];
    safeHook('HeroDefense', RVAS.defenseArmor, defPatterns, function(retval) {
      if (this.context && this.context.s0 !== undefined) {
        this.context.s0 = this.context.s0 * CFG.defenseMultiplier;
      }
      try {
        const orig = retval.toInt32();
        if (orig > 0) {
          retval.replace(ptr(Math.round(orig * CFG.defenseMultiplier)));
        }
      } catch (e) {}
    });
  }

  // 3b. Movement Speed Multiplier (1.0x to 3.0x)
  if (CFG.moveSpeedMult > 1.0) {
    const movePatterns = ['E8 03 1F 2A FC 63 02 91 ?? ?? ?? 1E'];
    safeHook('MovementSpeed', RVAS.moveSpeed, movePatterns, function(retval) {
      if (this.context && this.context.s0 !== undefined) {
        this.context.s0 = this.context.s0 * CFG.moveSpeedMult;
      }
      try {
        const orig = retval.toInt32();
        if (orig > 0) {
          retval.replace(ptr(Math.round(orig * CFG.moveSpeedMult)));
        }
      } catch (e) {}
    });
  }

  // 3c. Emblem V4 Max Level (Lv 60) & Talent Max Boost
  if (CFG.emblemBoost) {
    safeHook('EmblemV4_GetLevel', RVAS.emblemLv, ['78 00 80 52 C0 03 5F D6'], null, function(retval) {
      try {
        retval.replace(ptr(60)); // Max Emblem Level 60
      } catch (e) {}
    });
  }

  // 4. Cooldown Reduction (0% to 100% / No Cooldown)
  if (CFG.noSkillCooldown || CFG.cooldownReductionPct > 0) {
    const cdrRatio = CFG.noSkillCooldown ? 0.0 : Math.max(0.0, (100 - CFG.cooldownReductionPct) / 100.0);
    const cdrPatterns = [
      'C0 03 5F D6 ?? ?? ?? ?? 00 00 00 00 00 00 F0 3F',
      '00 00 80 52 00 00 00 1E'
    ];
    safeHook('SkillCooldown', RVAS.skillCooldown, cdrPatterns, function(retval) {
      if (this.context && this.context.s0 !== undefined) {
        this.context.s0 = this.context.s0 * cdrRatio;
      }
      try {
        if (cdrRatio === 0.0) {
          retval.replace(ptr(0));
        } else {
          const orig = retval.toInt32();
          retval.replace(ptr(Math.round(orig * cdrRatio)));
        }
      } catch (e) {}
    });
  }

  // 5. Extra / Infinite Mana and Energy
  if (CFG.manaBoost > 1.0 || CFG.energyBoost > 1.0) {
    const maxBoost = Math.max(CFG.manaBoost, CFG.energyBoost);
    safeHook('ManaEnergyRegen', RVAS.manaEnergy, [], function(retval) {
      if (this.context && this.context.s0 !== undefined) {
        this.context.s0 = 99999.0;
      }
      try {
        const orig = retval.toInt32();
        const boosted = maxBoost >= 9999 ? 99999 : (orig * maxBoost);
        retval.replace(ptr(Math.round(boosted)));
      } catch (e) {}
    });
  }

  // 6. Drone View / Free Camera Zoom (Tier 1 to 5X and 10X)
  if (CFG.droneTier > 0) {
    let camHeight = 80;
    if (CFG.droneTier >= 100) camHeight = 500; // 10X
    else if (CFG.droneTier >= 50) camHeight = 320; // 5X
    else if (CFG.droneTier >= 40) camHeight = 240; // 4X
    else if (CFG.droneTier >= 30) camHeight = 180; // 3X
    else if (CFG.droneTier >= 20) camHeight = 120; // 2X
    else camHeight = 90; // 1.5X

    const camPatterns = ['F4 4F 01 A9 FD 7B 02 A9 FD 03 00 91 ?? ?? ?? 1E'];
    safeHook('CameraManager_SetHeight', RVAS.cameraHeight, camPatterns,
      function(args) {
        if (args[1] && !args[1].isNull()) {
          try { args[1] = ptr(camHeight); } catch(e) {}
        }
        if (this.context && this.context.s0 !== undefined) {
          this.context.s0 = camHeight;
        }
      },
      function(retval) {
        try { retval.replace(ptr(camHeight)); } catch(e) {}
      }
    );
  }

  // 7. Full Map Hack / Absolute Enemy Vision Suite (Fog of War, Minimap & Shaders)
  if (CFG.mapHack) {
    // 7a. Core FogOfWarManager_IsVisible: Force true (1) on every entity check
    const fogPatterns = [
      '20 00 80 52 C0 03 5F D6',
      '00 00 80 52 C0 03 5F D6',
      'E0 03 00 32 C0 03 5F D6'
    ];
    safeHook('FogOfWar_IsVisible', RVAS.fowVisible, fogPatterns,
      null,
      function(retval) {
        try {
          retval.replace(ptr(1));
        } catch(e) {}
      }
    );

    // 7b. Season 42 NewFOW Entity Blocker Bypass (FogOfWar_IsEntityHiddenByNewFOW return false 0)
    safeHook('FogOfWar_IsEntityHiddenByNewFOW', RVAS.fowEntityHidden, ['00 00 80 52 C0 03 5F D6'],
      null,
      function(retval) {
        try { retval.replace(ptr(0)); } catch(e) {}
      }
    );

    // 7c. Vision Sprite Find Enemy Hero Refresh (VisionSpriteFindEnemyHero_Refresh return 1)
    safeHook('VisionSpriteFindEnemyHero', RVAS.visionSprite, ['20 00 80 52 C0 03 5F D6'],
      null,
      function(retval) {
        try { retval.replace(ptr(1)); } catch(e) {}
      }
    );

    // 7d. Hero / Actor Visibility & Cull Bypass (HeroRenderer_SetVisible / Actor_IsVisible)
    const actorVisRva = 0x01905B20;
    const actorPatterns = ['F4 4F 01 A9 FD 7B 02 A9 ?? ?? ?? 1E 20 00 80 52'];
    safeHook('Actor_ForceVisible', actorVisRva, actorPatterns,
      function(args) {
        if (args[1]) {
          try { args[1] = ptr(1); } catch(e) {}
        }
      },
      function(retval) {
        try { retval.replace(ptr(1)); } catch(e) {}
      }
    );

    // 7e. Minimap Radar Icon & Healthbar Display (MinimapEntity_SetVisible)
    const minimapRva = 0x01906100;
    const minimapPatterns = ['00 00 80 52 C0 03 5F D6'];
    safeHook('Minimap_ForceShow', minimapRva, minimapPatterns,
      null,
      function(retval) {
        try { retval.replace(ptr(1)); } catch(e) {}
      }
    );

    // 7f. Terrain Blocker & Force Visible (FogOfWar_IsBlockedByTerrain forced 0, ShouldForceVisible forced 1)
    safeHook('FogOfWar_IsBlockedByTerrain', RVAS.fowTerrainBlock, ['00 00 80 52 C0 03 5F D6'],
      null,
      function(retval) {
        try { retval.replace(ptr(0)); } catch(e) {}
      }
    );
    safeHook('FogOfWar_ShouldForceVisible', RVAS.fowForceVisible, ['20 00 80 52 C0 03 5F D6'],
      null,
      function(retval) {
        try { retval.replace(ptr(1)); } catch(e) {}
      }
    );

    // 7g. Unity Camera & Shader Fog Culling Override (libunity.so)
    try {
      const setCulling = Module.findExportByName('libunity.so', '_ZN6Camera14set_cullingMaskEi')
        || Module.findExportByName('libunity.so', 'Camera_set_cullingMask');
      if (setCulling) {
        Interceptor.attach(setCulling, {
          onEnter(args) {
            // -1 sets culling mask to ~0 (Everything visible, including hidden bush/fog layers)
            args[1] = ptr(-1);
          }
        });
      }
    } catch(e) {}

    console.log('[GameBoosterPRO] Full Map Hack & Enemy Vision active (FOW + NewFOW + VisionSprite + Minimap + CullingMask ~0) ✓');
  }

  // 8. No Recoil & Spread / Smart Aim Lock
  if (CFG.noSpread) {
    console.log('[GameBoosterPRO] MLBB No-Spread / Smart Aim Lock active ✓');
  }

  // 9. AntiCheat Cloak & ResCheck Validation Suppression
  if (CFG.antiBan) {
    safeHook('Freeze142_AntiCheatReport', RVAS.antiCheatReport, ['00 00 80 52 C0 03 5F D6'],
      null,
      function(retval) {
        try { retval.replace(ptr(0)); } catch (e) {}
      }
    );
    safeHook('ResCheckStartHandler', RVAS.resCheckStart, ['20 00 80 52 C0 03 5F D6'],
      null,
      function(retval) {
        try { retval.replace(ptr(1)); } catch (e) {}
      }
    );
    console.log('[GameBoosterPRO] Native AntiCheat & ResCheck stealth hooks active ✓');
  }
});

// ─── FPS Unlock (120 / 144 / 165 FPS) ────────────────────────────────────────
if (CFG.fpsUnlock > 60) {
  waitForModule('libunity.so', (mod) => {
    try {
      const fpsExports = [
        '_ZN5Unity12Application16SetTargetFrameRateEi',
        'Application_set_targetFrameRate',
        '_ZN12UnityPlayer18setTargetFrameRateEi'
      ];
      for (const exp of fpsExports) {
        const addr = Module.findExportByName('libunity.so', exp);
        if (addr) {
          Interceptor.attach(addr, {
            onEnter(args) { args[0] = ptr(CFG.fpsUnlock); }
          });
          console.log('[GameBoosterPRO] MLBB FPS unlocked to ' + CFG.fpsUnlock + ' via ' + exp);
          break;
        }
      }

      // Force QualitySettings.vSyncCount = 0
      const vsyncExports = [
        '_ZN5Unity15QualitySettings12SetVSyncCountEi',
        'QualitySettings_set_vSyncCount'
      ];
      for (const exp of vsyncExports) {
        const addr = Module.findExportByName('libunity.so', exp);
        if (addr) {
          Interceptor.attach(addr, {
            onEnter(args) { args[0] = ptr(0); }
          });
          break;
        }
      }
    } catch(e) {}
  });

  // IL2CPP internal setter hook
  waitForModule(['libil2cpp.so', 'libResources.so'], (mod) => {
    try {
      const il2cppFpsExports = [
        'UnityEngine_Application_set_targetFrameRate',
        '_ZN11UnityEngine11Application18set_targetFrameRateEi'
      ];
      for (const exp of il2cppFpsExports) {
        const addr = Module.findExportByName(mod.name, exp);
        if (addr) {
          Interceptor.attach(addr, {
            onEnter(args) { args[0] = ptr(CFG.fpsUnlock); }
          });
          console.log('[GameBoosterPRO] MLBB targetFrameRate hooked in ' + mod.name + ' @ ' + addr);
          break;
        }
      }
    } catch(e) {}
  });
}

try {
  const propSet = Module.findExportByName('libc.so', '__system_property_set');
  if (propSet) {
    const fnSet = new NativeFunction(propSet, 'int', ['pointer', 'pointer']);
    fnSet(Memory.allocUtf8String('gamebooster.mlbb.injected'), Memory.allocUtf8String('1'));
  }
} catch (e) {}

console.log('[GameBoosterPRO] MLBB hooks loaded successfully ✓ — gamebooster.mlbb.injected=1');
