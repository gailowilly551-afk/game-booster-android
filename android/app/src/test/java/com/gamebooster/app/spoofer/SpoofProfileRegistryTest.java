package com.gamebooster.app.spoofer;

import org.junit.Test;
import static org.junit.Assert.*;

public class SpoofProfileRegistryTest {

    @Test
    public void getDefaultProfile_returnsValidRogFlagship() {
        SpoofProfile profile = SpoofProfileRegistry.getDefaultProfile();
        assertNotNull(profile);
        assertEquals(SpoofProfileRegistry.ROG9_PRO_ID, profile.id);
        assertEquals("ASUS_AI2601_A", profile.model);
        assertTrue(profile.maxRefreshRateHz >= 144);
    }

    @Test
    public void getById_fallsBackToDefaultWhenUnknown() {
        SpoofProfile fallback = SpoofProfileRegistry.getById("non_existent_profile_id");
        assertNotNull(fallback);
        assertEquals(SpoofProfileRegistry.ROG9_PRO_ID, fallback.id);
    }
}

