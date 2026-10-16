package com.gamebooster.app.booster;

import org.junit.Test;
import static org.junit.Assert.*;

public class SafetyGuardTest {

    @Test
    public void testEvaluateStage() {
        assertEquals(SafetyGuard.STAGE_OK, SafetyGuard.evaluateStage(45_000));
        assertEquals(SafetyGuard.STAGE_OK, SafetyGuard.evaluateStage(49_999));
        assertEquals(SafetyGuard.STAGE_WARN, SafetyGuard.evaluateStage(50_000));
        assertEquals(SafetyGuard.STAGE_WARN, SafetyGuard.evaluateStage(54_999));
        assertEquals(SafetyGuard.STAGE_CRITICAL, SafetyGuard.evaluateStage(55_000));
        assertEquals(SafetyGuard.STAGE_CRITICAL, SafetyGuard.evaluateStage(65_000));
    }

    @Test
    public void testParseMaxMilliC() {
        assertEquals(-1, SafetyGuard.parseMaxMilliC(null));
        assertEquals(-1, SafetyGuard.parseMaxMilliC(""));
        assertEquals(-1, SafetyGuard.parseMaxMilliC("not_a_number"));

        String output = "38000\n45000\n51200\n42000\n";
        assertEquals(51200, SafetyGuard.parseMaxMilliC(output));

        String dirtyOutput = "38000\nerror reading node\n49000\n\n";
        assertEquals(49000, SafetyGuard.parseMaxMilliC(dirtyOutput));
    }

    @Test
    public void testFindBlockDeviceDetection() {
        assertNull(SafetyGuard.findBlock(null, null));
        assertNull(SafetyGuard.findBlock("samsung", "galaxy s24"));

        SafetyGuard.DeviceBlock pixelBlock = SafetyGuard.findBlock("google", "pixel 7 pro");
        assertNotNull(pixelBlock);
        assertFalse(pixelBlock.blocked); // WARN only

        SafetyGuard.DeviceBlock pixel8Block = SafetyGuard.findBlock("Google", "Pixel 8");
        assertNotNull(pixel8Block);
        assertFalse(pixel8Block.blocked);
    }
}
