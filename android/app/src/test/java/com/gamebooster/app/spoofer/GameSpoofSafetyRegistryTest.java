package com.gamebooster.app.spoofer;

import org.junit.Test;
import static org.junit.Assert.*;

public class GameSpoofSafetyRegistryTest {

    @Test
    public void riskTierFor_classifiesHighRiskKernelAntiCheatGames() {
        assertEquals(GameSpoofSafetyRegistry.RiskTier.HIGH_RISK,
                GameSpoofSafetyRegistry.riskTierFor("com.tencent.ig"));
        assertEquals(GameSpoofSafetyRegistry.RiskTier.HIGH_RISK,
                GameSpoofSafetyRegistry.riskTierFor("com.garena.game.codm"));
        assertEquals(GameSpoofSafetyRegistry.RiskTier.HIGH_RISK,
                GameSpoofSafetyRegistry.riskTierFor("com.activision.callofduty.shooter"));
        assertEquals(GameSpoofSafetyRegistry.RiskTier.HIGH_RISK,
                GameSpoofSafetyRegistry.riskTierFor("com.levelinfinite.sgameGlobal"));
    }

    @Test
    public void riskTierFor_classifiesMediumAndLowRiskGames() {
        assertEquals(GameSpoofSafetyRegistry.RiskTier.MEDIUM_RISK,
                GameSpoofSafetyRegistry.riskTierFor("com.mobile.legends"));
        assertEquals(GameSpoofSafetyRegistry.RiskTier.MEDIUM_RISK,
                GameSpoofSafetyRegistry.riskTierFor("com.dts.freefireth"));
        assertEquals(GameSpoofSafetyRegistry.RiskTier.LOW_RISK,
                GameSpoofSafetyRegistry.riskTierFor("com.example.offlinegame"));
        assertEquals(GameSpoofSafetyRegistry.RiskTier.LOW_RISK,
                GameSpoofSafetyRegistry.riskTierFor(null));
    }

    @Test
    public void isTrustedConfigReader_allowsLauncherAndKnownGamesOnly() {
        assertTrue(GameSpoofSafetyRegistry.isTrustedConfigReader("com.gamebooster.app"));
        assertTrue(GameSpoofSafetyRegistry.isTrustedConfigReader("com.tencent.ig"));
        assertTrue(GameSpoofSafetyRegistry.isTrustedConfigReader("com.mobile.legends"));
        assertFalse(GameSpoofSafetyRegistry.isTrustedConfigReader("com.random.scanner"));
        assertFalse(GameSpoofSafetyRegistry.isTrustedConfigReader(null));
    }
}

