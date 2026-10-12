package com.gamebooster.app.mods.codm;

import org.junit.Test;
import static org.junit.Assert.*;

public class CodmIl2cppResolverTest {

    @Test
    public void testDefaultOffsetsAndTokensAvailableWithoutContext() {
        long camOffset = CodmIl2cppResolver.getOffset("Camera_get_main");
        assertTrue("Camera_get_main offset should be > 0", camOffset > 0);

        long recoilToken = CodmIl2cppResolver.getToken("RecoilScaleWeaponShake");
        assertTrue("RecoilScaleWeaponShake token should be > 0", recoilToken > 0);

        long fieldOffset = CodmIl2cppResolver.getFieldOffset("GAS_MainFireFunnelTask_RecoilUpBase");
        assertTrue("Field offset should be > 0", fieldOffset > 0);
    }

    @Test
    public void testStringOffsetAndCategoryResolution() {
        long shotSpreadBuf = CodmIl2cppResolver.getStringOffset("ShotSpread");
        assertEquals("ShotSpread buffer offset should match verified dump", 0x2a06a7L, shotSpreadBuf);

        String spreadCategory = CodmIl2cppResolver.getStringCategory("ShotSpread");
        assertEquals("Spread", spreadCategory);

        long damageAddBuf = CodmIl2cppResolver.getStringOffset("DamageAddition");
        assertEquals("DamageAddition buffer offset should match verified dump", 0x38686bL, damageAddBuf);

        String combatCategory = CodmIl2cppResolver.getStringCategory("DamageAddition");
        assertEquals("CombatDamage", combatCategory);

        long catBuf = CodmIl2cppResolver.getStringOffset("CatPlayerController");
        assertEquals("CatPlayerController buffer offset should match verified dump", 0x17865fL, catBuf);

        String playerCategory = CodmIl2cppResolver.getStringCategory("CatPlayerController");
        assertEquals("PlayerController", playerCategory);

        long recoilBuf = CodmIl2cppResolver.getStringOffset("RecoilScaleWeaponShake");
        assertEquals("RecoilScaleWeaponShake buffer offset should match verified dump", 0x24c147L, recoilBuf);

        String recoilCategory = CodmIl2cppResolver.getStringCategory("RecoilScaleWeaponShake");
        assertEquals("Recoil", recoilCategory);
    }

    @Test
    public void testNonExistentKeyReturnsZero() {
        assertEquals(0L, CodmIl2cppResolver.getOffset("NonExistent_Key"));
        assertEquals(0L, CodmIl2cppResolver.getToken("NonExistent_Token"));
        assertEquals(0L, CodmIl2cppResolver.getStringOffset("NonExistent_String"));
        assertEquals("", CodmIl2cppResolver.getStringCategory("NonExistent_String"));
    }
}
