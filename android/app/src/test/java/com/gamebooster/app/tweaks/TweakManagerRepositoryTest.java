package com.gamebooster.app.tweaks;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class TweakManagerRepositoryTest {

    @Test
    public void testAllTweaksHaveValidCommandsAndRollback() {
        List<TweakItem> all = TweakManagerRepository.getAllTweaks();
        assertNotNull(all);
        assertFalse("Tweak repository must not be empty", all.isEmpty());

        for (TweakItem item : all) {
            assertNotNull("Tweak ID must not be null", item.getId());
            assertFalse("Tweak ID must not be empty", item.getId().trim().isEmpty());
            assertNotNull("Tweak title must not be null", item.getTitle());
            assertNotNull("Tweak description must not be null", item.getDescription());
            assertNotNull("Apply command must not be null: " + item.getId(), item.getApplyCommand());
            assertFalse("Apply command must not be empty: " + item.getId(), item.getApplyCommand().trim().isEmpty());
            assertNotNull("Revert command must not be null: " + item.getId(), item.getRevertCommand());
            assertNotNull("Category must not be null: " + item.getId(), item.getCategory());
        }
    }

    @Test
    public void testCategoryMappings() {
        for (TweakCategory cat : TweakCategory.values()) {
            List<TweakItem> items = TweakManagerRepository.getTweaksByCategory(cat);
            assertNotNull(items);
            if (cat != TweakCategory.ALL) {
                for (TweakItem item : items) {
                    assertEquals(cat, item.getCategory());
                }
            }
        }
    }

    @Test
    public void testVerificationProbeExtraction() {
        TweakItem item = TweakManagerRepository.getTweakById("hw_overlays");
        assertNotNull("hw_overlays tweak must exist", item);
        String probe = item.getVerificationProbe();
        assertNotNull("Verification probe must not be null", probe);
        assertTrue("Probe for hw_overlays should query debug.sf.hw: " + probe, probe.contains("getprop debug.sf.hw"));
    }
}
