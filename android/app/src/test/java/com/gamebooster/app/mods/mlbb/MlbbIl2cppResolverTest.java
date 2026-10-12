package com.gamebooster.app.mods.mlbb;

import org.junit.Test;
import static org.junit.Assert.*;

public class MlbbIl2cppResolverTest {

    @Test
    public void testDefaultMethodsIncludeVerifiedSymbols() {
        long ishow = MlbbIl2cppResolver.getMethodOffset("ISHOW_Init_SkillCoolDownCalc");
        assertTrue("ISHOW_Init_SkillCoolDownCalc should have positive RVA", ishow > 0L);

        long fowForce = MlbbIl2cppResolver.getMethodOffset("FogOfWar_ShouldForceVisible");
        assertTrue("FogOfWar_ShouldForceVisible should have positive RVA", fowForce > 0L);

        long fowHidden = MlbbIl2cppResolver.getMethodOffset("FogOfWar_IsEntityHiddenByNewFOW");
        assertTrue("FogOfWar_IsEntityHiddenByNewFOW should have positive RVA", fowHidden > 0L);
    }

    @Test
    public void testTokensIncludeVerifiedClasses() {
        long antiCheat = MlbbIl2cppResolver.getToken("AntiCheatReporter");
        assertTrue("AntiCheatReporter token should be non-zero", antiCheat > 0L);

        long resCheck = MlbbIl2cppResolver.getToken("ResCheck");
        assertTrue("ResCheck token should be non-zero", resCheck > 0L);

        long fov = MlbbIl2cppResolver.getToken("FieldOfView");
        assertTrue("FieldOfView token should be non-zero", fov > 0L);
    }

    @Test
    public void testStringPatternLookups() {
        long refFow = MlbbIl2cppResolver.getStringOffset("RefFogOfWarMesh");
        assertTrue("RefFogOfWarMesh string offset should be resolved", refFow > 0L);

        String catFow = MlbbIl2cppResolver.getStringCategory("RefFogOfWarMesh");
        assertEquals("FogOfWar", catFow);

        String catAc = MlbbIl2cppResolver.getStringCategory("Freeze142_AntiCheatReport");
        assertEquals("AntiCheat", catAc);
    }
}
