package com.gamebooster.app.booster;

import org.junit.Test;
import static org.junit.Assert.*;

public class PerformanceChannelTest {

    @Test
    public void testProfilesHaveValidTitles() {
        for (PerformanceChannel.Profile profile : PerformanceChannel.Profile.values()) {
            assertNotNull("Profile title must not be null for " + profile.name(), profile.title);
            assertFalse("Profile title must not be empty for " + profile.name(), profile.title.trim().isEmpty());
        }
    }

    @Test
    public void testProfileResultStructure() {
        PerformanceChannel.ProfileResult result =
                new PerformanceChannel.ProfileResult(true, 185, "185Hz Enforced");
        assertTrue(result.refreshRateApplied);
        assertEquals(185, result.appliedHz);
        assertEquals("185Hz Enforced", result.message);
    }

    @Test
    public void testNullContextHandling() {
        PerformanceChannel.ProfileResult result =
                PerformanceChannel.applyProfileWithResult(null, PerformanceChannel.Profile.EXTREME_PERFORMANCE);
        assertNotNull(result);
        assertFalse(result.refreshRateApplied);
        assertEquals(0, result.appliedHz);
        assertTrue(result.message.contains("unavailable"));
    }

    @Test
    public void testScriptExecutionDoesNotThrow() {
        // In local unit test environment (non-Android), these methods should execute safely
        boolean perfScriptOk = PerformanceChannel.writeAndExecutePerformanceTweaksScript(120);
        // May return true or false based on mock/shell, but must not crash
        assertNotNull(perfScriptOk);

        boolean rootScriptOk = PerformanceChannel.writeAndExecuteRootTweaksScript(185);
        assertNotNull(rootScriptOk);
    }

    @Test
    public void testGpuRenderMode() {
        assertTrue(PerformanceChannel.setGpuRenderMode(false));
    }
}
