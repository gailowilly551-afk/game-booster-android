package com.gamebooster.app.mods.pubgm;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

import com.gamebooster.app.mods.GameModEngine;
import com.gamebooster.app.mods.GameModProfile;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class PubgmGameModUiProfileTest {

    private Context mockContext;
    private final Map<String, Object> storage = new HashMap<>();

    @Before
    public void setUp() {
        storage.clear();
        mockContext = new ContextWrapper(null) {
            @Override
            public Context getApplicationContext() {
                return this;
            }

            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                return new FakeSharedPreferences(storage);
            }
        };
    }

    @Test
    public void testPubgmProfileDefaultValues() {
        GameModProfile profile = new GameModProfile();
        assertFalse("Aimbot should default to false", profile.pubgmAimbot);
        assertTrue("AllScopeLock should default to true", profile.pubgmAllScopeLock);
        assertFalse("NoRecoil should default to false", profile.pubgmNoRecoil);
        assertFalse("MagicBullet should default to false", profile.pubgmMagicBullet);
        assertFalse("UltraDroneView should default to false", profile.pubgmUltraDroneView);
        assertEquals("Default Drone FOV should be 130", 130, profile.pubgmDroneFov);
        assertEquals("Default Target FPS should be 185", 185, profile.pubgmTargetFps);
        assertTrue("AntiBan should default to true", profile.pubgmAntiBan);
        assertTrue("MidnightHuntersFix should default to true", profile.pubgmMidnightHuntersFix);
    }

    @Test
    public void testPubgmProfileSaveAndLoad() {
        GameModProfile p1 = new GameModProfile();
        p1.pubgmAimbot = true;
        p1.pubgmNoRecoil = true;
        p1.pubgmNoSpread = true;
        p1.pubgmMagicBullet = true;
        p1.pubgmUltraDroneView = true;
        p1.pubgmDroneFov = 150;
        p1.pubgmDamageLockMax = true;
        p1.pubgmCombatOverdrive = true;
        p1.pubgmFpsUnlock = true;
        p1.pubgmTargetFps = 185;
        p1.pubgmAntiBan = true;
        p1.pubgmSovereignOverdriveBypass = true;

        p1.save(mockContext);

        GameModProfile p2 = GameModProfile.load(mockContext);
        assertTrue("Aimbot state must persist", p2.pubgmAimbot);
        assertTrue("NoRecoil state must persist", p2.pubgmNoRecoil);
        assertTrue("NoSpread state must persist", p2.pubgmNoSpread);
        assertTrue("MagicBullet state must persist", p2.pubgmMagicBullet);
        assertTrue("UltraDroneView state must persist", p2.pubgmUltraDroneView);
        assertEquals("Drone FOV must persist", 150, p2.pubgmDroneFov);
        assertTrue("DamageLockMax state must persist", p2.pubgmDamageLockMax);
        assertTrue("CombatOverdrive state must persist", p2.pubgmCombatOverdrive);
        assertTrue("FpsUnlock state must persist", p2.pubgmFpsUnlock);
        assertEquals("Target FPS must persist", 185, p2.pubgmTargetFps);
        assertTrue("AntiBan state must persist", p2.pubgmAntiBan);
        assertTrue("SovereignOverdriveBypass state must persist", p2.pubgmSovereignOverdriveBypass);
    }

    @Test
    public void testPubgmPackageConstant() {
        assertEquals("PUBGM package constant must match com.tencent.ig", "com.tencent.ig", GameModEngine.PKG_PUBGM);
    }

    private static class FakeSharedPreferences implements SharedPreferences {
        private final Map<String, Object> storage;

        FakeSharedPreferences(Map<String, Object> storage) {
            this.storage = storage;
        }

        @Override public Map<String, ?> getAll() { return new HashMap<>(storage); }
        @Override public String getString(String key, String def) { Object v = storage.get(key); return v instanceof String ? (String) v : def; }
        @Override public Set<String> getStringSet(String key, Set<String> def) { return def; }
        @Override public int getInt(String key, int def) { Object v = storage.get(key); return v instanceof Integer ? (Integer) v : def; }
        @Override public long getLong(String key, long def) { Object v = storage.get(key); return v instanceof Long ? (Long) v : def; }
        @Override public float getFloat(String key, float def) { Object v = storage.get(key); return v instanceof Float ? (Float) v : def; }
        @Override public boolean getBoolean(String key, boolean def) { Object v = storage.get(key); return v instanceof Boolean ? (Boolean) v : def; }
        @Override public boolean contains(String key) { return storage.containsKey(key); }
        @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}
        @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        @Override
        public Editor edit() {
            return new Editor() {
                private final Map<String, Object> staged = new HashMap<>();

                @Override public Editor putString(String key, String value) { staged.put(key, value); return this; }
                @Override public Editor putStringSet(String key, Set<String> values) { staged.put(key, values); return this; }
                @Override public Editor putInt(String key, int value) { staged.put(key, value); return this; }
                @Override public Editor putLong(String key, long value) { staged.put(key, value); return this; }
                @Override public Editor putFloat(String key, float value) { staged.put(key, value); return this; }
                @Override public Editor putBoolean(String key, boolean value) { staged.put(key, value); return this; }
                @Override public Editor remove(String key) { staged.put(key, this); return this; }
                @Override public Editor clear() { storage.clear(); return this; }
                @Override public boolean commit() { apply(); return true; }

                @Override
                public void apply() {
                    for (Map.Entry<String, Object> e : staged.entrySet()) {
                        if (e.getValue() == this) storage.remove(e.getKey());
                        else storage.put(e.getKey(), e.getValue());
                    }
                }
            };
        }
    }
}
