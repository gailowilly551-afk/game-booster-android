package com.gamebooster.app.config;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

/**
 * MlbbCodmPatchVerificationTest — Validates contract invariants from Everything.md:
 * 1. MLBB: versionCode 211812322, UID 10677, Directory 1 & Directory 2
 * 2. CODM: versionCode 28028, UID 10656, Directory 1 & Directory 2
 * 3. Base path resolver generates dual roots for both packages
 * 4. Permanent anti-revert targets are properly registered
 */
public class MlbbCodmPatchVerificationTest {

    @Test
    public void testMlbbConstantsAndContract() {
        assertEquals("MLBB package must match com.mobile.legends", "com.mobile.legends", MlbbConfigPatcher.PKG_MLBB);
        assertEquals("MLBB versionCode must be 211812322", 211812322, MlbbConfigPatcher.TARGET_VERSION_CODE);
        assertEquals("MLBB UID must be 10677", 10677, MlbbConfigPatcher.TARGET_UID);
        assertEquals("MLBB Directory 1 must be /data/user/0/com.mobile.legends", "/data/user/0/com.mobile.legends", MlbbConfigPatcher.DIR_USER_0);
        assertEquals("MLBB Directory 2 must be /storage/emulated/0/Android/data/com.mobile.legends", "/storage/emulated/0/Android/data/com.mobile.legends", MlbbConfigPatcher.DIR_ANDROID_DATA);

        List<String> roots = MlbbDroneViewPatcher.resolveMlbbRootDirs(MlbbConfigPatcher.PKG_MLBB);
        assertTrue("Roots must include Directory 1 (/data/user/0/)", roots.contains("/data/user/0/com.mobile.legends"));
        assertTrue("Roots must include Directory 2 (/storage/emulated/0/Android/data/)", roots.contains("/storage/emulated/0/Android/data/com.mobile.legends"));
    }

    @Test
    public void testCodmConstantsAndContract() {
        assertEquals("CODM package must match com.garena.game.codm", "com.garena.game.codm", CodmConfigPatcher.PKG_CODM);
        assertEquals("CODM target package must match com.garena.game.codm", "com.garena.game.codm", CodmConfigPatcher.TARGET_PACKAGE);
        assertEquals("CODM versionCode must be 28028", 28028, CodmConfigPatcher.TARGET_VERSION_CODE);
        assertEquals("CODM UID must be 10656", 10656, CodmConfigPatcher.TARGET_UID);
        assertEquals("CODM Directory 1 must be /data/user/0/com.garena.game.codm", "/data/user/0/com.garena.game.codm", CodmConfigPatcher.DIR_USER_0);
        assertEquals("CODM Directory 2 must be /storage/emulated/0/Android/data/com.garena.game.codm", "/storage/emulated/0/Android/data/com.garena.game.codm", CodmConfigPatcher.DIR_ANDROID_DATA);

        List<String> roots = CodmConfigPatcher.resolveRootDirs(CodmConfigPatcher.PKG_CODM);
        assertTrue("Roots must include Directory 1 (/data/user/0/)", roots.contains("/data/user/0/com.garena.game.codm"));
        assertTrue("Roots must include Directory 2 (/storage/emulated/0/Android/data/)", roots.contains("/storage/emulated/0/Android/data/com.garena.game.codm"));
    }

    @Test
    public void testGameConfigPathResolverDualRoots() {
        List<String> mlbbBase = GameConfigPathResolver.generateBasePaths("com.mobile.legends");
        assertTrue(mlbbBase.contains("/data/user/0/com.mobile.legends"));
        assertTrue(mlbbBase.contains("/storage/emulated/0/Android/data/com.mobile.legends"));

        List<String> codmBase = GameConfigPathResolver.generateBasePaths("com.garena.game.codm");
        assertTrue(codmBase.contains("/data/user/0/com.garena.game.codm"));
        assertTrue(codmBase.contains("/storage/emulated/0/Android/data/com.garena.game.codm"));
    }

    @Test
    public void testGameModAutoSyncDestinations() {
        List<String> mlbbDest = GameModAutoSyncEngine.getTargetDestinations("com.mobile.legends");
        assertTrue(mlbbDest.contains("/data/user/0/com.mobile.legends/files"));
        assertTrue(mlbbDest.contains("/storage/emulated/0/Android/data/com.mobile.legends/files"));

        List<String> codmDest = GameModAutoSyncEngine.getTargetDestinations("com.garena.game.codm");
        assertTrue(codmDest.contains("/data/user/0/com.garena.game.codm/files"));
        assertTrue(codmDest.contains("/storage/emulated/0/Android/data/com.garena.game.codm/files"));
    }
}
