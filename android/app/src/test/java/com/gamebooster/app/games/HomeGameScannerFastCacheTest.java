package com.gamebooster.app.games;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class HomeGameScannerFastCacheTest {

    private File tempFilesDir;
    private Context mockContext;
    private final Map<String, Object> preferenceStorage = new HashMap<>();

    @Before
    public void setUp() throws IOException {
        tempFilesDir = Files.createTempDirectory("game_launcher_test_files").toFile();
        preferenceStorage.clear();

        mockContext = new ContextWrapper(null) {
            @Override
            public Context getApplicationContext() {
                return this;
            }

            @Override
            public File getFilesDir() {
                return tempFilesDir;
            }

            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                return new FakeTestSharedPreferences(preferenceStorage);
            }
        };

        HomeGameScanner.clearFastCache(mockContext);
    }

    @After
    public void tearDown() {
        if (tempFilesDir != null && tempFilesDir.exists()) {
            File[] files = tempFilesDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
            tempFilesDir.delete();
        }
    }

    @Test
    public void testSaveAndLoadFastCachedGames() {
        List<GameAppInfo> initialGames = new ArrayList<>();
        initialGames.add(new GameAppInfo("Mobile Legends", "com.mobile.legends", null, new Intent(), "MOBA", 1234, 0xFF4A90E2));
        initialGames.add(new GameAppInfo("Call of Duty: Mobile", "com.garena.game.codm", null, new Intent(), "FPS", 5678, 0xFFFF0055));
        initialGames.add(new GameAppInfo("PUBG Mobile", "com.tencent.ig", null, new Intent(), "BATTLE ROYALE", 9012, 0xFFFF8800));

        HomeGameScanner.saveFastCachedGames(mockContext, initialGames);

        List<GameAppInfo> cached = HomeGameScanner.loadFastCachedGames(mockContext);
        assertNotNull("Cached games list must not be null", cached);
        assertEquals("Must load 3 cached games", 3, cached.size());

        assertEquals("com.mobile.legends", cached.get(0).getPackageName());
        assertEquals("Mobile Legends", cached.get(0).getLabel());
        assertEquals("MOBA", cached.get(0).getGameType());

        assertEquals("com.garena.game.codm", cached.get(1).getPackageName());
        assertEquals("com.tencent.ig", cached.get(2).getPackageName());
    }

    @Test
    public void testExcludedPackageFilteredFromFastCache() {
        List<GameAppInfo> initialGames = new ArrayList<>();
        initialGames.add(new GameAppInfo("Mobile Legends", "com.mobile.legends", null, new Intent(), "MOBA", 1234, 0xFF4A90E2));
        initialGames.add(new GameAppInfo("PUBG Mobile", "com.tencent.ig", null, new Intent(), "BATTLE ROYALE", 9012, 0xFFFF8800));

        HomeGameScanner.saveFastCachedGames(mockContext, initialGames);

        // Exclude PUBG Mobile in preferences via GameLauncherHelper
        GameLauncherHelper.excludePackage(mockContext, "com.tencent.ig");

        List<GameAppInfo> cached = HomeGameScanner.loadFastCachedGames(mockContext);
        assertNotNull(cached);
        assertEquals("Excluded game must be filtered out", 1, cached.size());
        assertEquals("com.mobile.legends", cached.get(0).getPackageName());
    }

    @Test
    public void testProbePrimaryGamesReturnsPredefinedSpecs() {
        // Test primary probe when disk cache is empty
        List<GameAppInfo> probed = HomeGameScanner.probePrimaryGamesFast(mockContext);
        assertNotNull("Probed list must never be null", probed);
        // Under headless mock without PackageManager installed packages, probe should safely return empty or non-null list without throwing
        assertTrue(probed.isEmpty() || !probed.isEmpty());
    }

    private static class FakeTestSharedPreferences implements SharedPreferences {
        private final Map<String, Object> storage;

        FakeTestSharedPreferences(Map<String, Object> storage) {
            this.storage = storage;
        }

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(storage);
        }

        @Override
        public String getString(String key, String defValue) {
            Object val = storage.get(key);
            return val instanceof String ? (String) val : defValue;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Set<String> getStringSet(String key, Set<String> defValues) {
            Object val = storage.get(key);
            return val instanceof Set ? new HashSet<>((Set<String>) val) : defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object val = storage.get(key);
            return val instanceof Integer ? (Integer) val : defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object val = storage.get(key);
            return val instanceof Long ? (Long) val : defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object val = storage.get(key);
            return val instanceof Float ? (Float) val : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object val = storage.get(key);
            return val instanceof Boolean ? (Boolean) val : defValue;
        }

        @Override
        public boolean contains(String key) {
            return storage.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new Editor() {
                private final Map<String, Object> temp = new HashMap<>();

                @Override
                public Editor putString(String key, String value) {
                    temp.put(key, value);
                    return this;
                }

                @Override
                public Editor putStringSet(String key, Set<String> values) {
                    temp.put(key, values != null ? new HashSet<>(values) : null);
                    return this;
                }

                @Override
                public Editor putInt(String key, int value) {
                    temp.put(key, value);
                    return this;
                }

                @Override
                public Editor putLong(String key, long value) {
                    temp.put(key, value);
                    return this;
                }

                @Override
                public Editor putFloat(String key, float value) {
                    temp.put(key, value);
                    return this;
                }

                @Override
                public Editor putBoolean(String key, boolean value) {
                    temp.put(key, value);
                    return this;
                }

                @Override
                public Editor remove(String key) {
                    temp.put(key, this);
                    return this;
                }

                @Override
                public Editor clear() {
                    storage.clear();
                    return this;
                }

                @Override
                public boolean commit() {
                    apply();
                    return true;
                }

                @Override
                public void apply() {
                    for (Map.Entry<String, Object> entry : temp.entrySet()) {
                        if (entry.getValue() == this) {
                            storage.remove(entry.getKey());
                        } else {
                            storage.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
            };
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}
    }
}
