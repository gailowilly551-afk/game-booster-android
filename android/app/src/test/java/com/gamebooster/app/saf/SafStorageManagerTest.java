package com.gamebooster.app.saf;

import org.junit.Test;

import static org.junit.Assert.*;

public class SafStorageManagerTest {

    @Test
    public void testNormalizeDocumentIdToPath() {
        assertEquals("Android/data/com.mobile.legends/files",
                SafStorageManager.normalizeDocumentIdToPath("primary:Android/data/com.mobile.legends/files"));

        assertEquals("Android/data/com.mobile.legends",
                SafStorageManager.normalizeDocumentIdToPath("primary:Android/data/com.mobile.legends"));

        assertEquals("Android/data",
                SafStorageManager.normalizeDocumentIdToPath("primary:Android/data"));

        assertEquals("",
                SafStorageManager.normalizeDocumentIdToPath("primary:"));

        assertEquals("Android/data/com.activision.callofduty.shooter/files",
                SafStorageManager.normalizeDocumentIdToPath("XXXX-XXXX:Android/data/com.activision.callofduty.shooter/files"));

        assertEquals("",
                SafStorageManager.normalizeDocumentIdToPath(null));
    }

    @Test
    public void testNormalizeToCanonicalStoragePath() {
        // Full storage prefix
        assertEquals("Android/data/com.mobile.legends/files/dragon2017/assets/prefs_int",
                SafStorageManager.normalizeToCanonicalStoragePath("com.mobile.legends",
                        "/storage/emulated/0/Android/data/com.mobile.legends/files/dragon2017/assets/prefs_int"));

        // Sdcard prefix
        assertEquals("Android/data/com.mobile.legends/files/dragon2017/assets/prefs_int",
                SafStorageManager.normalizeToCanonicalStoragePath("com.mobile.legends",
                        "/sdcard/Android/data/com.mobile.legends/files/dragon2017/assets/prefs_int"));

        // Relative to package
        assertEquals("Android/data/com.mobile.legends/files/dragon2017/assets/prefs_int",
                SafStorageManager.normalizeToCanonicalStoragePath("com.mobile.legends",
                        "files/dragon2017/assets/prefs_int"));

        // Data/data prefix
        assertEquals("Android/data/com.mobile.legends/files/prefs_int",
                SafStorageManager.normalizeToCanonicalStoragePath("com.mobile.legends",
                        "/data/data/com.mobile.legends/files/prefs_int"));
    }

    @Test
    public void testResolveRelativeNavPath() {
        // Case 1: Tree granted at files/ level
        String treeFiles = "Android/data/com.mobile.legends/files";
        String target = "Android/data/com.mobile.legends/files/dragon2017/assets/prefs_int";
        assertEquals("dragon2017/assets/prefs_int",
                SafStorageManager.resolveRelativeNavPath(treeFiles, target, "files", "com.mobile.legends"));

        // Case 2: Tree granted at package level
        String treePkg = "Android/data/com.mobile.legends";
        assertEquals("files/dragon2017/assets/prefs_int",
                SafStorageManager.resolveRelativeNavPath(treePkg, target, "com.mobile.legends", "com.mobile.legends"));

        // Case 3: Tree granted at Android/data level
        String treeData = "Android/data";
        assertEquals("com.mobile.legends/files/dragon2017/assets/prefs_int",
                SafStorageManager.resolveRelativeNavPath(treeData, target, "data", "com.mobile.legends"));

        // Case 4: Tree granted at storage root
        String treeRoot = "";
        assertEquals("Android/data/com.mobile.legends/files/dragon2017/assets/prefs_int",
                SafStorageManager.resolveRelativeNavPath(treeRoot, target, "primary", "com.mobile.legends"));
    }

    @Test
    public void testMimeTypes() {
        assertEquals("application/json", SafStorageManager.getMimeTypeForName("BattleConfig.json"));
        assertEquals("text/xml", SafStorageManager.getMimeTypeForName("version.xml"));
        assertEquals("text/plain", SafStorageManager.getMimeTypeForName("GraphicsSettings.ini"));
        assertEquals("application/octet-stream", SafStorageManager.getMimeTypeForName("High_M_Map_001.unity3d"));
        assertEquals("application/octet-stream", SafStorageManager.getMimeTypeForName("libil2cpp.so"));
    }

    @Test
    public void testGameFamiliesAndDisplayNames() {
        assertNotNull(SafStorageManager.getFamilyForPackage("com.mobile.legends"));
        assertNotNull(SafStorageManager.getFamilyForPackage("com.activision.callofduty.shooter"));
        assertNotNull(SafStorageManager.getFamilyForPackage("com.tencent.ig"));
        assertNotNull(SafStorageManager.getFamilyForPackage("com.dts.freefireth"));
        assertNotNull(SafStorageManager.getFamilyForPackage("com.levelinfinite.sgameGlobal"));

        assertEquals("Mobile Legends (MLBB)", SafStorageManager.getGameDisplayName("com.mobile.legends"));
        assertEquals("Call of Duty: Mobile (CODM)", SafStorageManager.getGameDisplayName("com.activision.callofduty.shooter"));
        assertEquals("Free Fire / MAX", SafStorageManager.getGameDisplayName("com.dts.freefireth"));
        assertEquals("All Games (Global Android/data)", SafStorageManager.getGameDisplayName("all"));
    }
}
