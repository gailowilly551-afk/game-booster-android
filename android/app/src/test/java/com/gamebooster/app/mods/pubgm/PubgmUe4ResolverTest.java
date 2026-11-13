package com.gamebooster.app.mods.pubgm;

import com.gamebooster.app.config.PubgConfigPatcher;
import com.gamebooster.app.games.GameFamily;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class PubgmUe4ResolverTest {

    private static String loadOffsetsJson() throws Exception {
        File direct = new File("src/main/assets/frida/pubgm_offsets.json");
        File candidate = direct.exists()
                ? direct
                : new File("app/src/main/assets/frida/pubgm_offsets.json");
        assertTrue("pubgm_offsets.json must exist", candidate.exists());
        return new String(Files.readAllBytes(candidate.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void parseJson_loads470GlobalAndFlashOffsets() throws Exception {
        PubgmUe4Resolver.parseJson(loadOffsetsJson());

        assertEquals("4.7.0", PubgmUe4Resolver.getTargetVersionName());
        assertEquals(21740, PubgmUe4Resolver.getTargetVersionCode("com.tencent.ig"));
        assertEquals(21745, PubgmUe4Resolver.getTargetVersionCode("com.tencent.igfit"));

        assertEquals(0L, PubgmUe4Resolver.getUe4Offset("GWorld"));
        assertEquals(0x001d73fcL, PubgmUe4Resolver.getAnticheatOffset("libanogs_AnoSDKInit"));
        assertEquals(0x000029a8L, PubgmUe4Resolver.getAnticheatOffset("libsentry_android_nativeLoadModuleList"));
        assertEquals(0x0005f19cL, PubgmUe4Resolver.getAnticheatOffset("libcubehawk_Hawk_checkEmulator"));

        assertEquals(0x05e4d9c8L, PubgmUe4Resolver.getJniOffset(
                "com.tencent.ig", "Java_com_epicgames_ue4_GameActivity_nativeIsEnableScreenCaptureNotify"));
        assertEquals(0x0d829584L, PubgmUe4Resolver.getJniOffset(
                "com.tencent.ig", "Java_com_blue_studio_screenshot_GameScreenshot_onScreenshotTaken"));
        assertEquals(0x0d802814L, PubgmUe4Resolver.getJniOffset(
                "com.tencent.igfit", "Java_com_blue_studio_screenshot_GameScreenshot_onScreenshotTaken"));
        assertEquals(0x0d83e220L, PubgmUe4Resolver.getJniOffset(
                "com.tencent.ig", "Java_com_tencent_hawk_bridge_HawkNative_notifySystemInfo"));
        assertEquals(0x0d8174b0L, PubgmUe4Resolver.getJniOffset(
                "com.tencent.igfit", "Java_com_tencent_hawk_bridge_HawkNative_notifySystemInfo"));
    }

    @Test
    public void variantRouting_resolvesGlobalAndFlash() {
        assertEquals(GameFamily.PUBGM, GameFamily.fromPackage("com.tencent.ig"));
        assertEquals(GameFamily.PUBGM, GameFamily.fromPackage("com.tencent.igfit"));
        assertEquals(GameFamily.PUBGM, GameFamily.fromPackage("com.tencent.igce"));
        assertEquals(21740, PubgConfigPatcher.resolveTargetVersionCode("com.tencent.ig"));
        assertEquals(21745, PubgConfigPatcher.resolveTargetVersionCode("com.tencent.igfit"));
    }
}

