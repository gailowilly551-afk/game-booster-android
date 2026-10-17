package com.gamebooster.app.mods.pubgm;

import com.gamebooster.app.config.GameConfigPathResolver;
import com.gamebooster.app.config.PubgConfigPatcher;
import com.gamebooster.app.mods.GameModProfile;
import com.gamebooster.app.saf.SafStorageManager;

import org.junit.Test;
import java.util.List;

import static org.junit.Assert.*;

/**
 * PubgmPatchVerificationTest — Validates contract invariants for PUBG Mobile 4.6.0:
 * 1. Package: com.tencent.ig, Target Version: 4.6.0, Target VersionCode: 21525
 * 2. Dual Directory roots: /data/user/0/com.tencent.ig & /storage/emulated/0/Android/data/com.tencent.ig
 * 3. SAF Storage Manager recognizes PUBGM family
 * 4. GameModProfile PUBGM defaults and options
 */
public class PubgmPatchVerificationTest {

    @Test
    public void testPubgmConstantsAndContract() {
        assertEquals("PUBGM package must match com.tencent.ig", "com.tencent.ig", PubgmModManager.PKG_PUBGM);
        assertEquals("PUBGM versionCode must match 21525", 21525, PubgmModManager.TARGET_VERSION_CODE);
        assertEquals("PUBGM version name must match 4.6.0", "4.6.0", PubgmModManager.TARGET_VERSION_NAME);

        assertEquals("PubgConfigPatcher package must match", "com.tencent.ig", PubgConfigPatcher.PKG_PUBGM);
        assertEquals("PubgConfigPatcher versionCode must match", 21525, PubgConfigPatcher.TARGET_VERSION_CODE);
        assertEquals("PubgConfigPatcher version name must match", "4.6.0", PubgConfigPatcher.TARGET_VERSION_NAME);

        assertEquals("PUBGM Directory 1 must be /data/user/0/com.tencent.ig",
                "/data/user/0/com.tencent.ig", PubgConfigPatcher.DIR_USER_0);
        assertEquals("PUBGM Directory 2 must be /storage/emulated/0/Android/data/com.tencent.ig",
                "/storage/emulated/0/Android/data/com.tencent.ig", PubgConfigPatcher.DIR_ANDROID_DATA);
    }

    @Test
    public void testDualRootsAndPaths() {
        List<String> roots = PubgConfigPatcher.resolveRootDirs(PubgmModManager.PKG_PUBGM);
        assertTrue("Roots must include Directory 1 (/data/user/0/)",
                roots.contains("/data/user/0/com.tencent.ig"));
        assertTrue("Roots must include Directory 2 (/storage/emulated/0/Android/data/)",
                roots.contains("/storage/emulated/0/Android/data/com.tencent.ig"));

        List<String> basePaths = GameConfigPathResolver.generateBasePaths("com.tencent.ig");
        assertTrue(basePaths.contains("/data/user/0/com.tencent.ig"));
        assertTrue(basePaths.contains("/storage/emulated/0/Android/data/com.tencent.ig"));
    }

    @Test
    public void testSafStorageFamilyPubgm() {
        String[] family = SafStorageManager.getFamilyForPackage("com.tencent.ig");
        assertNotNull("Family must not be null for com.tencent.ig", family);
        assertArrayEquals("Family must be FAMILY_PUBGM", SafStorageManager.FAMILY_PUBGM, family);

        String displayName = SafStorageManager.getGameDisplayName("com.tencent.ig");
        assertTrue("Display name must identify PUBG Mobile", displayName.contains("PUBG Mobile"));
    }

    @Test
    public void testPubgmModProfileDefaults() {
        GameModProfile profile = new GameModProfile();
        assertFalse("FPS unlock default should be false until user toggles", profile.pubgmFpsUnlock);
        assertEquals("Default target FPS should be 185", 185, profile.pubgmTargetFps);
        assertTrue("Anti-ban default should be true", profile.pubgmAntiBan);
        assertFalse("Aimbot default should be false until enabled", profile.pubgmAimbot);
        assertFalse("No recoil default should be false until enabled", profile.pubgmNoRecoil);
        assertFalse("No spread default should be false until enabled", profile.pubgmNoSpread);
        assertFalse("Magic bullet default should be false until enabled", profile.pubgmMagicBullet);
        assertFalse("Damage lock max default should be false until enabled", profile.pubgmDamageLockMax);
    }
}
