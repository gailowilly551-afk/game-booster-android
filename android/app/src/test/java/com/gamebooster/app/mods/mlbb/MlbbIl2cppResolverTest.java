package com.gamebooster.app.mods.mlbb;

import org.junit.Test;
import static org.junit.Assert.*;

public class MlbbIl2cppResolverTest {

    @Test
    public void testDefaultMethodsIncludeVerifiedSymbols() {
        long ishow = MlbbIl2cppResolver.getMethodOffset("ISHOW_Init_SkillCoolDownCalc");
        assertTrue("ISHOW_Init_SkillCoolDownCalc should have positive RVA", ishow > 0L);

        long ishowAdd = MlbbIl2cppResolver.getMethodOffset("ISHOW_AddSkillCoolDownCalc");
        assertTrue("ISHOW_AddSkillCoolDownCalc should have positive RVA", ishowAdd > 0L);

        long syncInit = MlbbIl2cppResolver.getMethodOffset("Sync_Init_SkillCoolDownCalc");
        assertTrue("Sync_Init_SkillCoolDownCalc should have positive RVA", syncInit > 0L);

        long fowForce = MlbbIl2cppResolver.getMethodOffset("FogOfWar_ShouldForceVisible");
        assertTrue("FogOfWar_ShouldForceVisible should have positive RVA", fowForce > 0L);

        long fowHidden = MlbbIl2cppResolver.getMethodOffset("FogOfWar_IsEntityHiddenByNewFOW");
        assertTrue("FogOfWar_IsEntityHiddenByNewFOW should have positive RVA", fowHidden > 0L);

        long fowBlock = MlbbIl2cppResolver.getMethodOffset("FogOfWar_IsBlockedByTerrain");
        assertTrue("FogOfWar_IsBlockedByTerrain should have positive RVA", fowBlock > 0L);

        long fowExposed = MlbbIl2cppResolver.getMethodOffset("FogOfWar_IsAttackExposed");
        assertTrue("FogOfWar_IsAttackExposed should have positive RVA", fowExposed > 0L);

        long fov = MlbbIl2cppResolver.getMethodOffset("m_iFieldOfView");
        assertTrue("m_iFieldOfView should have positive RVA", fov > 0L);

        long antiCheat = MlbbIl2cppResolver.getMethodOffset("Freeze142_AntiCheatReport");
        assertTrue("Freeze142_AntiCheatReport should have positive RVA", antiCheat > 0L);

        long resCheck = MlbbIl2cppResolver.getMethodOffset("ResCheckStartHandler");
        assertTrue("ResCheckStartHandler should have positive RVA", resCheck > 0L);
    }

    @Test
    public void testTokensIncludeVerifiedClasses() {
        long antiCheat = MlbbIl2cppResolver.getToken("AntiCheatReporter");
        assertTrue("AntiCheatReporter token should be non-zero", antiCheat > 0L);
        assertEquals("AntiCheatReporter token should match TypeDef token range (0x02...)", 0x02000000L, antiCheat & 0xFF000000L);

        long resCheck = MlbbIl2cppResolver.getToken("ResCheck");
        assertTrue("ResCheck token should be non-zero", resCheck > 0L);
        assertEquals("ResCheck token should match TypeDef token range", 0x02000000L, resCheck & 0xFF000000L);

        long fov = MlbbIl2cppResolver.getToken("FieldOfView");
        assertTrue("FieldOfView token should be non-zero", fov > 0L);
        assertEquals("FieldOfView token should match TypeDef token range", 0x02000000L, fov & 0xFF000000L);

        long mola = MlbbIl2cppResolver.getToken("MolaDownloadProvider");
        assertTrue("MolaDownloadProvider token should be non-zero", mola > 0L);
        assertEquals("MolaDownloadProvider token should match TypeDef token range", 0x02000000L, mola & 0xFF000000L);

        long fow = MlbbIl2cppResolver.getToken("FogOfWar");
        assertTrue("FogOfWar token should be non-zero", fow > 0L);
    }

    @Test
    public void testStringPatternLookups() {
        long refFow = MlbbIl2cppResolver.getStringOffset("RefFogOfWarMesh");
        assertTrue("RefFogOfWarMesh string offset should be resolved", refFow > 0L);

        String catFow = MlbbIl2cppResolver.getStringCategory("RefFogOfWarMesh");
        assertEquals("FogOfWar", catFow);

        long refCmd = MlbbIl2cppResolver.getStringOffset("RefFogOfWarCommandBuffer");
        assertTrue("RefFogOfWarCommandBuffer string offset should be resolved", refCmd > 0L);

        String catCmd = MlbbIl2cppResolver.getStringCategory("RefFogOfWarCommandBuffer");
        assertEquals("FogOfWar", catCmd);

        String catAc = MlbbIl2cppResolver.getStringCategory("Freeze142_AntiCheatReport");
        assertEquals("AntiCheat", catAc);

        String catRc = MlbbIl2cppResolver.getStringCategory("ResCheckStartHandler");
        assertEquals("ResCheck", catRc);
    }

    @Test
    public void testFieldOffsets() {
        long hp = MlbbIl2cppResolver.getFieldOffset("Hero_CurrentHp");
        assertEquals(0x48L, hp);

        long maxHp = MlbbIl2cppResolver.getFieldOffset("Hero_MaxHp");
        assertEquals(0x4CL, maxHp);

        long atk = MlbbIl2cppResolver.getFieldOffset("Hero_AttackPower");
        assertEquals(0x54L, atk);
    }
}
