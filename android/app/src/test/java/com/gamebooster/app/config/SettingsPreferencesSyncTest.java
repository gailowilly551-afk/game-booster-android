package com.gamebooster.app.config;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

import com.gamebooster.app.tweaks.TweakItem;
import com.gamebooster.app.tweaks.TweakManagerRepository;
import com.gamebooster.app.ui.adapters.TweaksAdapter;

import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SettingsPreferencesSyncTest {

    private static Context createInMemoryPrefsContext() {
        final Map<String, Map<String, Object>> stores = new HashMap<>();
        return new ContextWrapper(null) {
            @Override
            public Context getApplicationContext() {
                return this;
            }

            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                Map<String, Object> backing = stores.computeIfAbsent(name, k -> new HashMap<>());
                return createSharedPreferencesProxy(backing);
            }
        };
    }

    private static SharedPreferences createSharedPreferencesProxy(Map<String, Object> backing) {
        return (SharedPreferences) Proxy.newProxyInstance(
                SharedPreferences.class.getClassLoader(),
                new Class<?>[]{SharedPreferences.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    switch (name) {
                        case "edit":
                            return createEditorProxy(backing);
                        case "getBoolean":
                            Object bv = backing.get(args[0]);
                            return bv instanceof Boolean ? bv : args[1];
                        case "getInt":
                            Object iv = backing.get(args[0]);
                            return iv instanceof Integer ? iv : args[1];
                        case "getString":
                            Object sv = backing.get(args[0]);
                            return sv instanceof String ? sv : args[1];
                        case "getStringSet":
                            Object setv = backing.get(args[0]);
                            return setv instanceof Set ? new HashSet<>((Set<String>) setv) : args[1];
                        case "getAll":
                            return new HashMap<>(backing);
                        case "contains":
                            return backing.containsKey(args[0]);
                        default:
                            return null;
                    }
                }
        );
    }

    private static SharedPreferences.Editor createEditorProxy(Map<String, Object> backing) {
        final Map<String, Object> pending = new HashMap<>();
        final boolean[] clearFlag = {false};
        return (SharedPreferences.Editor) Proxy.newProxyInstance(
                SharedPreferences.Editor.class.getClassLoader(),
                new Class<?>[]{SharedPreferences.Editor.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    switch (name) {
                        case "putBoolean":
                        case "putInt":
                        case "putString":
                            pending.put(String.valueOf(args[0]), args[1]);
                            return proxy;
                        case "putStringSet":
                            Set<String> copy = args[1] != null ? new HashSet<>((Set<String>) args[1]) : null;
                            pending.put(String.valueOf(args[0]), copy);
                            return proxy;
                        case "remove":
                            pending.put(String.valueOf(args[0]), null);
                            return proxy;
                        case "clear":
                            clearFlag[0] = true;
                            pending.clear();
                            return proxy;
                        case "apply":
                        case "commit":
                            if (clearFlag[0]) {
                                backing.clear();
                            }
                            for (Map.Entry<String, Object> e : pending.entrySet()) {
                                if (e.getValue() == null) {
                                    backing.remove(e.getKey());
                                } else {
                                    backing.put(e.getKey(), e.getValue());
                                }
                            }
                            return "commit".equals(name) ? Boolean.TRUE : null;
                        default:
                            return proxy;
                    }
                }
        );
    }

    @Test
    public void resetAllManualSettings_writesStockDefaultsWithoutResurrectingTrueGetters() {
        Context ctx = createInMemoryPrefsContext();
        ManualSettingsPreferences.setTouch1000HzLockEnabled(ctx, true);
        ManualSettingsPreferences.setGpuMode(ctx, "vulkan");
        ManualSettingsPreferences.setCpuMode(ctx, "performance");
        ManualSettingsPreferences.setGamingDns(ctx, "CLOUDFLARE_1_1_1_1");
        ManualSettingsPreferences.setSelectedPhCarrier(ctx, "DITO");
        ManualSettingsPreferences.setActiveFpsProfile(ctx, 185);

        ManualSettingsPreferences.resetAllManualSettings(ctx);

        assertFalse(ManualSettingsPreferences.isAntiLogEnabled(ctx));
        assertFalse(ManualSettingsPreferences.isAutoPurgeLogsEnabled(ctx));
        assertFalse(ManualSettingsPreferences.isAotSpeedEnabled(ctx));
        assertFalse(ManualSettingsPreferences.isGameDriverEnabled(ctx));
        assertFalse(ManualSettingsPreferences.isThermalBypassEnabled(ctx));
        assertFalse(ManualSettingsPreferences.isAdpfEngineEnabled(ctx));
        assertFalse(ManualSettingsPreferences.isTouch1000HzLockEnabled(ctx));
        assertFalse(ManualSettingsPreferences.isPhantomFreezerKillEnabled(ctx));
        assertFalse(ManualSettingsPreferences.isMemory16kbShieldEnabled(ctx));
        assertFalse(ManualSettingsPreferences.isWebViewBoostEnabled(ctx));
        assertFalse(ManualSettingsPreferences.isPhTelcoSuperchargerEnabled(ctx));
        assertEquals("skia", ManualSettingsPreferences.getGpuMode(ctx));
        assertEquals("schedutil", ManualSettingsPreferences.getCpuMode(ctx));
        assertEquals("SYSTEM_DEFAULT", ManualSettingsPreferences.getGamingDns(ctx));
        assertNull(ManualSettingsPreferences.getSelectedPhCarrier(ctx));
        assertEquals(60, ManualSettingsPreferences.getActiveFpsProfile(ctx));
    }

    @Test
    public void tweakSync_mapsCanonicalRepositoryIdsBidirectionally() {
        Context ctx = createInMemoryPrefsContext();

        TweakPreferences.syncManualSettingToTweak(ctx, "touch_1000hz", true);
        assertTrue(TweakPreferences.isTweakApplied(ctx, "vendor_touch_digitizer_1000hz_universal"));

        TweakPreferences.syncManualSettingToTweak(ctx, "phantom_freezer", true);
        assertTrue(TweakPreferences.isTweakApplied(ctx, "android_13_16_phantom_freezer_kill"));

        TweakPreferences.syncTweakToManualSetting(ctx, "vulkan_skiavk_render_thread_rt", true);
        assertTrue(ManualSettingsPreferences.isVulkanSkiaVkEnabled(ctx));
        assertEquals("vulkan", ManualSettingsPreferences.getGpuMode(ctx));

        TweakPreferences.syncTweakToManualSetting(ctx, "uclamp_cpuset_top_app_boost", true);
        assertTrue(ManualSettingsPreferences.isUclampBoostEnabled(ctx));
        assertEquals("performance", ManualSettingsPreferences.getCpuMode(ctx));
    }

    @Test
    public void tweaksAdapter_excludesTopSwitchTweaksFromVisibleCount() {
        Context ctx = createInMemoryPrefsContext();
        List<TweakItem> all = TweakManagerRepository.getAllTweaks();
        TweaksAdapter adapter = new TweaksAdapter(ctx, all);

        assertTrue(adapter.getVisibleMasterCount() < all.size());
        assertEquals(adapter.getItemCount(), adapter.getVisibleMasterCount());
    }
}
