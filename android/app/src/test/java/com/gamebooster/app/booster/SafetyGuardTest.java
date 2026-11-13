package com.gamebooster.app.booster;

import org.junit.Test;
import static org.junit.Assert.*;

public class SafetyGuardTest {

    @Test
    public void evaluateStage_mapsThermalThresholds() {
        assertEquals(SafetyGuard.STAGE_OK, SafetyGuard.evaluateStage(42_000));
        assertEquals(SafetyGuard.STAGE_WARN, SafetyGuard.evaluateStage(50_000));
        assertEquals(SafetyGuard.STAGE_WARN, SafetyGuard.evaluateStage(54_999));
        assertEquals(SafetyGuard.STAGE_CRITICAL, SafetyGuard.evaluateStage(55_000));
        assertEquals(SafetyGuard.STAGE_CRITICAL, SafetyGuard.evaluateStage(62_500));
    }

    @Test
    public void parseMaxMilliC_ignoresGarbageAndPicksMax() {
        assertEquals(-1, SafetyGuard.parseMaxMilliC(null));
        assertEquals(-1, SafetyGuard.parseMaxMilliC(""));
        assertEquals(53_400, SafetyGuard.parseMaxMilliC("41000\nN/A\n53400\n48200\n"));
    }

    @Test
    public void findBlock_identifiesKnownWarningDevices() {
        SafetyGuard.DeviceBlock pixel7 = SafetyGuard.findBlock("Google", "Pixel 7 Pro");
        assertNotNull(pixel7);
        assertFalse(pixel7.blocked);

        SafetyGuard.DeviceBlock s22 = SafetyGuard.findBlock("samsung", "SM-S908B");
        assertNotNull(s22);
        assertFalse(s22.blocked);
    }
}

