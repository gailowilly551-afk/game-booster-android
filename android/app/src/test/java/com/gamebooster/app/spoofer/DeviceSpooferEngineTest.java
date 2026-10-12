package com.gamebooster.app.spoofer;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class DeviceSpooferEngineTest {

    @Test
    public void testResetpropCommandGeneration() {
        SpoofProfile profile = SpoofProfileRegistry.getById("samsung_s25_ultra");
        assertNotNull("samsung_s25_ultra profile must exist", profile);

        List<String> commands = HardwareMaskEngine.buildResetpropCommands(profile);
        assertNotNull("Commands must not be null", commands);
        assertFalse("Commands list must not be empty", commands.isEmpty());

        boolean hasModel = false;
        boolean hasBrand = false;
        boolean hasFingerprint = false;
        boolean hasSoc = false;

        for (String cmd : commands) {
            if (cmd.contains("resetprop ro.product.model \"" + profile.model + "\"")) hasModel = true;
            if (cmd.contains("resetprop ro.product.brand \"" + profile.brand + "\"")) hasBrand = true;
            if (cmd.contains("resetprop ro.build.fingerprint \"" + profile.fingerprint + "\"")) hasFingerprint = true;
            if (cmd.contains("resetprop ro.soc.model \"" + profile.socModel + "\"")) hasSoc = true;
        }

        assertTrue("Must include ro.product.model resetprop", hasModel);
        assertTrue("Must include ro.product.brand resetprop", hasBrand);
        assertTrue("Must include ro.build.fingerprint resetprop", hasFingerprint);
        assertTrue("Must include ro.soc.model resetprop", hasSoc);
    }

    @Test
    public void testResetpropRestoreCommands() {
        DeviceSpooferEngine.DeviceIdentitySnapshot snap = new DeviceSpooferEngine.DeviceIdentitySnapshot();
        List<String> restoreCmds = HardwareMaskEngine.buildResetpropRestoreCommands(snap);
        assertNotNull("Restore commands must not be null", restoreCmds);
        assertFalse("Restore commands must not be empty", restoreCmds.isEmpty());

        boolean hasModel = false;
        for (String cmd : restoreCmds) {
            if (cmd.contains("resetprop ro.product.model \"" + snap.model + "\"")) hasModel = true;
        }
        assertTrue("Must restore ro.product.model", hasModel);
    }
}
