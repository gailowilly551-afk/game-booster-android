package com.gamebooster.app.booster;

import org.junit.Test;
import static org.junit.Assert.*;

public class MaxHzForceChannelTest {

    @Test
    public void testForceResultNoPrivilege() {
        MaxHzForceChannel.ForceResult result = MaxHzForceChannel.ForceResult.noPrivilege(185);
        assertNotNull(result);
        assertFalse(result.success);
        assertEquals(185, result.appliedHz);
        assertEquals(0, result.successCount);
        assertTrue(result.message.contains("Neither Root nor Shizuku"));
    }

    @Test
    public void testForceResultComplete() {
        MaxHzForceChannel.ForceResult allSuccess =
                MaxHzForceChannel.ForceResult.complete(185, 30, 0, 30);
        assertNotNull(allSuccess);
        assertTrue(allSuccess.success);
        assertEquals(185, allSuccess.appliedHz);
        assertEquals(30, allSuccess.successCount);
        assertEquals(0, allSuccess.failCount);
        assertTrue(allSuccess.message.contains("185Hz"));

        MaxHzForceChannel.ForceResult partial =
                MaxHzForceChannel.ForceResult.complete(144, 25, 5, 30);
        assertTrue(partial.success);
        assertEquals(25, partial.successCount);
        assertEquals(5, partial.failCount);
        assertTrue(partial.message.contains("5 failed"));
    }

    @Test
    public void testHzParsingInHzFpsChannel() {
        assertEquals(185, HzFpsChannel.parseHzSetting("185"));
        assertEquals(185, HzFpsChannel.parseHzSetting("185.0"));
        assertEquals(120, HzFpsChannel.parseHzSetting("  120.0  "));
        assertEquals(60, HzFpsChannel.parseHzSetting("60"));
        assertEquals(-1, HzFpsChannel.parseHzSetting(null));
        assertEquals(-1, HzFpsChannel.parseHzSetting(""));
        assertEquals(-1, HzFpsChannel.parseHzSetting("null"));
        assertEquals(-1, HzFpsChannel.parseHzSetting("undefined"));
        assertEquals(-1, HzFpsChannel.parseHzSetting("-1"));
        assertEquals(-1, HzFpsChannel.parseHzSetting("abc"));
    }
}
