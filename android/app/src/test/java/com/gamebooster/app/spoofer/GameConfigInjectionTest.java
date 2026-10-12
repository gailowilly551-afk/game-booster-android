package com.gamebooster.app.spoofer;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class GameConfigInjectionTest {

    @Test
    public void testMlbbAppOpsPreservesPhoneState() {
        List<String> mlbbCmds = new ArrayList<>();
        HardwareMaskEngine.applyAppOpsShieldForPackage(mlbbCmds, "com.mobile.legends");

        for (String cmd : mlbbCmds) {
            assertFalse("Must NOT ignore READ_PHONE_STATE on MLBB", cmd.contains("READ_PHONE_STATE ignore"));
            assertFalse("Must NOT ignore READ_DEVICE_IDENTIFIERS on MLBB", cmd.contains("READ_DEVICE_IDENTIFIERS ignore"));
        }

        List<String> codmCmds = new ArrayList<>();
        HardwareMaskEngine.applyAppOpsShieldForPackage(codmCmds, "com.activision.callofduty.shooter");
        boolean hasReadDevId = false;
        for (String cmd : codmCmds) {
            if (cmd.contains("READ_DEVICE_IDENTIFIERS ignore")) hasReadDevId = true;
        }
        assertTrue("Non-MLBB game must shield device identifiers", hasReadDevId);
    }

    @Test
    public void testUe4ProfileKeysFormat() {
        SpoofProfile profile = SpoofProfileRegistry.getById("asus_rog9_pro");
        assertNotNull(profile);
        String[] keys = profile.generateUe4DeviceProfileKeys(185);
        assertNotNull(keys);
        assertTrue(keys.length > 0);

        boolean hasFps = false;
        for (String k : keys) {
            if (k.contains("FrameRateLimit=185")) hasFps = true;
        }
        assertTrue("Must contain FrameRateLimit key", hasFps);
    }

    @Test
    public void testJsonHardwareProfileFormat() {
        SpoofProfile profile = SpoofProfileRegistry.getById("samsung_s25_ultra");
        assertNotNull(profile);
        String json = profile.generateJsonHardwareProfile(120);
        assertNotNull(json);
        assertTrue(json.contains("\"GPURenderer\": \"" + profile.glRenderer + "\""));
        assertTrue(json.contains("\"SoCModel\": \"" + profile.socModel + "\""));
        assertTrue(json.contains("\"TargetFPS\": 120"));
    }
}
