/**
 * Universal Frida Hook Script for Android Games
 * Target: Any Android game running on Unity, Unreal Engine 4/5, or proprietary native engines
 * Non-rooted execution via Shizuku shell (UID 2000)
 */

'use strict';

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
  return defaultVal;
}

const CFG = {
  fpsTarget: parseInt(getSystemProp('gamebooster.universal.fps', '185')),
  antiBan:   getSystemProp('gamebooster.universal.antiban', '1') === '1',
  touchRate: parseInt(getSystemProp('gamebooster.universal.touch', '1000')),
};

console.log('[GameBoosterPRO] Universal Frida Hook loaded — config:', JSON.stringify(CFG));

// 1. SSL Unpinning for all HTTPS connections
if (CFG.antiBan && typeof Java !== 'undefined') {
  Java.perform(() => {
    try {
      const CertificatePinner = Java.use('okhttp3.CertificatePinner');
      CertificatePinner.check.overload('java.lang.String', 'java.util.List').implementation = function(hostname, certs) {
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
}

// 2. Unity FPS Unlock (libunity.so)
if (CFG.fpsTarget > 60) {
  const unityFpsExports = [
    '_ZN5Unity12Application16SetTargetFrameRateEi',
    'Application_set_targetFrameRate',
    '_ZN12UnityPlayer18setTargetFrameRateEi',
    'UnityEngine_Application_set_targetFrameRate'
  ];

  const hookUnityFps = (mod) => {
    for (const exp of unityFpsExports) {
      const addr = Module.findExportByName(mod.name, exp);
      if (addr) {
        Interceptor.attach(addr, {
          onEnter(args) { args[0] = ptr(CFG.fpsTarget); }
        });
        console.log('[GameBoosterPRO] Universal FPS unlocked to ' + CFG.fpsTarget + ' via ' + exp);
        break;
      }
    }
  };

  const uMod = Process.findModuleByName('libunity.so');
  if (uMod) hookUnityFps(uMod);
  else {
    const p = Module.findExportByName(null, 'dlopen');
    if (p) {
      Interceptor.attach(p, {
        onEnter(args) { this.path = args[0] ? args[0].readCString() : ''; },
        onLeave(retval) {
          if (this.path && this.path.indexOf('libunity.so') !== -1) {
            const m = Process.findModuleByName('libunity.so');
            if (m) hookUnityFps(m);
          }
        }
      });
    }
  }
}
