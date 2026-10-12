package com.gamebooster.app.ui;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

import com.gamebooster.app.config.ManualSettingsPreferences;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class SettingsNetworkWiringTest {

    private Context mockContext;
    private final Map<String, Object> preferenceStorage = new HashMap<>();

    @Before
    public void setUp() {
        preferenceStorage.clear();

        SharedPreferences fakePrefs = new FakeSharedPreferences(preferenceStorage);
        mockContext = new ContextWrapper(null) {
            @Override
            public Context getApplicationContext() {
                return this;
            }

            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                return fakePrefs;
            }
        };
    }

    @Test
    public void testNetworkPreferencesDefaultValuesWithNullContext() {
        // Safe null fallbacks
        assertTrue(ManualSettingsPreferences.isDisableDataSaverEnabled(null));
        assertTrue(ManualSettingsPreferences.isDisableBatterySaverEnabled(null));
        assertTrue(ManualSettingsPreferences.isDisableWifiSaverEnabled(null));
        assertTrue(ManualSettingsPreferences.isTcpBbrBuffersEnabled(null));
        assertTrue(ManualSettingsPreferences.is5g6gDataEnabled(null));
        assertTrue(ManualSettingsPreferences.isWifiLowLatencyEnabled(null));
        assertTrue(ManualSettingsPreferences.isWifi5g6g7gTurboEnabled(null));
        assertFalse(ManualSettingsPreferences.isDualDataWifiEnabled(null));
        assertFalse(ManualSettingsPreferences.isTntSmartSuperchargerEnabled(null));
        assertFalse(ManualSettingsPreferences.isTmGlobeSuperchargerEnabled(null));
        assertFalse(ManualSettingsPreferences.isPhTelcoSuperchargerEnabled(null));
        assertTrue(ManualSettingsPreferences.isAutoDnsFlushEnabled(null));
        assertTrue(ManualSettingsPreferences.isTetherHwEnabled(null));
        assertFalse(ManualSettingsPreferences.isForceGnssEnabled(null));
        assertEquals("CLOUDFLARE_1_1_1_1", ManualSettingsPreferences.getGamingDns(null));
        assertEquals("dual", ManualSettingsPreferences.getNetworkMode(null));
    }

    @Test
    public void testNetworkPreferencesRoundTrip() {
        // Data Saver
        ManualSettingsPreferences.setDisableDataSaverEnabled(mockContext, false);
        assertFalse(ManualSettingsPreferences.isDisableDataSaverEnabled(mockContext));
        ManualSettingsPreferences.setDisableDataSaverEnabled(mockContext, true);
        assertTrue(ManualSettingsPreferences.isDisableDataSaverEnabled(mockContext));

        // Battery Saver
        ManualSettingsPreferences.setDisableBatterySaverEnabled(mockContext, false);
        assertFalse(ManualSettingsPreferences.isDisableBatterySaverEnabled(mockContext));
        ManualSettingsPreferences.setDisableBatterySaverEnabled(mockContext, true);
        assertTrue(ManualSettingsPreferences.isDisableBatterySaverEnabled(mockContext));

        // Wi-Fi Saver
        ManualSettingsPreferences.setDisableWifiSaverEnabled(mockContext, false);
        assertFalse(ManualSettingsPreferences.isDisableWifiSaverEnabled(mockContext));
        ManualSettingsPreferences.setDisableWifiSaverEnabled(mockContext, true);
        assertTrue(ManualSettingsPreferences.isDisableWifiSaverEnabled(mockContext));

        // TCP BBR
        ManualSettingsPreferences.setTcpBbrBuffersEnabled(mockContext, false);
        assertFalse(ManualSettingsPreferences.isTcpBbrBuffersEnabled(mockContext));
        ManualSettingsPreferences.setTcpBbrBuffersEnabled(mockContext, true);
        assertTrue(ManualSettingsPreferences.isTcpBbrBuffersEnabled(mockContext));

        // 5G/6G Data
        ManualSettingsPreferences.set5g6gDataEnabled(mockContext, false);
        assertFalse(ManualSettingsPreferences.is5g6gDataEnabled(mockContext));
        ManualSettingsPreferences.set5g6gDataEnabled(mockContext, true);
        assertTrue(ManualSettingsPreferences.is5g6gDataEnabled(mockContext));

        // Wi-Fi Low Latency
        ManualSettingsPreferences.setWifiLowLatencyEnabled(mockContext, false);
        assertFalse(ManualSettingsPreferences.isWifiLowLatencyEnabled(mockContext));
        ManualSettingsPreferences.setWifiLowLatencyEnabled(mockContext, true);
        assertTrue(ManualSettingsPreferences.isWifiLowLatencyEnabled(mockContext));

        // Wi-Fi 5G/6G/7G Turbo
        ManualSettingsPreferences.setWifi5g6g7gTurboEnabled(mockContext, false);
        assertFalse(ManualSettingsPreferences.isWifi5g6g7gTurboEnabled(mockContext));
        ManualSettingsPreferences.setWifi5g6g7gTurboEnabled(mockContext, true);
        assertTrue(ManualSettingsPreferences.isWifi5g6g7gTurboEnabled(mockContext));

        // Dual Data + Wi-Fi
        ManualSettingsPreferences.setDualDataWifiEnabled(mockContext, true);
        assertTrue(ManualSettingsPreferences.isDualDataWifiEnabled(mockContext));
        ManualSettingsPreferences.setDualDataWifiEnabled(mockContext, false);
        assertFalse(ManualSettingsPreferences.isDualDataWifiEnabled(mockContext));

        // TNT / Smart
        ManualSettingsPreferences.setTntSmartSuperchargerEnabled(mockContext, true);
        assertTrue(ManualSettingsPreferences.isTntSmartSuperchargerEnabled(mockContext));
        ManualSettingsPreferences.setTntSmartSuperchargerEnabled(mockContext, false);
        assertFalse(ManualSettingsPreferences.isTntSmartSuperchargerEnabled(mockContext));

        // TM / Globe
        ManualSettingsPreferences.setTmGlobeSuperchargerEnabled(mockContext, true);
        assertTrue(ManualSettingsPreferences.isTmGlobeSuperchargerEnabled(mockContext));
        ManualSettingsPreferences.setTmGlobeSuperchargerEnabled(mockContext, false);
        assertFalse(ManualSettingsPreferences.isTmGlobeSuperchargerEnabled(mockContext));

        // PH Telco Supercharger
        ManualSettingsPreferences.setPhTelcoSuperchargerEnabled(mockContext, true);
        assertTrue(ManualSettingsPreferences.isPhTelcoSuperchargerEnabled(mockContext));
        ManualSettingsPreferences.setPhTelcoSuperchargerEnabled(mockContext, false);
        assertFalse(ManualSettingsPreferences.isPhTelcoSuperchargerEnabled(mockContext));

        // Auto DNS Flush
        ManualSettingsPreferences.setAutoDnsFlushEnabled(mockContext, false);
        assertFalse(ManualSettingsPreferences.isAutoDnsFlushEnabled(mockContext));
        ManualSettingsPreferences.setAutoDnsFlushEnabled(mockContext, true);
        assertTrue(ManualSettingsPreferences.isAutoDnsFlushEnabled(mockContext));

        // Tether HW
        ManualSettingsPreferences.setTetherHwEnabled(mockContext, false);
        assertFalse(ManualSettingsPreferences.isTetherHwEnabled(mockContext));
        ManualSettingsPreferences.setTetherHwEnabled(mockContext, true);
        assertTrue(ManualSettingsPreferences.isTetherHwEnabled(mockContext));

        // Force GNSS
        ManualSettingsPreferences.setForceGnssEnabled(mockContext, true);
        assertTrue(ManualSettingsPreferences.isForceGnssEnabled(mockContext));
        ManualSettingsPreferences.setForceGnssEnabled(mockContext, false);
        assertFalse(ManualSettingsPreferences.isForceGnssEnabled(mockContext));

        // Gaming DNS
        ManualSettingsPreferences.setGamingDns(mockContext, "GOOGLE_8_8_8_8");
        assertEquals("GOOGLE_8_8_8_8", ManualSettingsPreferences.getGamingDns(mockContext));

        // Network Mode
        ManualSettingsPreferences.setNetworkMode(mockContext, "wifi_only");
        assertEquals("wifi_only", ManualSettingsPreferences.getNetworkMode(mockContext));
    }

    private static class FakeSharedPreferences implements SharedPreferences {
        private final Map<String, Object> storage;

        FakeSharedPreferences(Map<String, Object> storage) {
            this.storage = storage;
        }

        @Override public Map<String, ?> getAll() { return new HashMap<>(storage); }
        @Override public String getString(String key, String defValue) {
            Object v = storage.get(key);
            return v instanceof String ? (String) v : defValue;
        }
        @Override public Set<String> getStringSet(String key, Set<String> defValues) { return defValues; }
        @Override public int getInt(String key, int defValue) {
            Object v = storage.get(key);
            return v instanceof Integer ? (Integer) v : defValue;
        }
        @Override public long getLong(String key, long defValue) {
            Object v = storage.get(key);
            return v instanceof Long ? (Long) v : defValue;
        }
        @Override public float getFloat(String key, float defValue) {
            Object v = storage.get(key);
            return v instanceof Float ? (Float) v : defValue;
        }
        @Override public boolean getBoolean(String key, boolean defValue) {
            Object v = storage.get(key);
            return v instanceof Boolean ? (Boolean) v : defValue;
        }
        @Override public boolean contains(String key) { return storage.containsKey(key); }
        @Override public Editor edit() { return new FakeEditor(storage); }
        @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}
        @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}
    }

    private static class FakeEditor implements SharedPreferences.Editor {
        private final Map<String, Object> storage;
        private final Map<String, Object> pending = new HashMap<>();

        FakeEditor(Map<String, Object> storage) {
            this.storage = storage;
        }

        @Override public SharedPreferences.Editor putString(String key, String value) { pending.put(key, value); return this; }
        @Override public SharedPreferences.Editor putStringSet(String key, Set<String> values) { pending.put(key, values); return this; }
        @Override public SharedPreferences.Editor putInt(String key, int value) { pending.put(key, value); return this; }
        @Override public SharedPreferences.Editor putLong(String key, long value) { pending.put(key, value); return this; }
        @Override public SharedPreferences.Editor putFloat(String key, float value) { pending.put(key, value); return this; }
        @Override public SharedPreferences.Editor putBoolean(String key, boolean value) { pending.put(key, value); return this; }
        @Override public SharedPreferences.Editor remove(String key) { pending.put(key, null); return this; }
        @Override public SharedPreferences.Editor clear() { storage.clear(); return this; }
        @Override public boolean commit() { apply(); return true; }
        @Override public void apply() {
            for (Map.Entry<String, Object> entry : pending.entrySet()) {
                if (entry.getValue() == null) storage.remove(entry.getKey());
                else storage.put(entry.getKey(), entry.getValue());
            }
        }
    }
}
