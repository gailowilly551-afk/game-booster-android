package com.gamebooster.app.booster;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

public class BotLobbyRankMatchEngineTest {

    @Test
    public void testGameTitleResolution() {
        assertEquals("Mobile Legends: Bang Bang", BotLobbyRankMatchEngine.getGameTitle("com.mobile.legends"));
        assertEquals("Mobile Legends: Bang Bang", BotLobbyRankMatchEngine.getGameTitle("com.mobilelegends.v2"));
        assertEquals("Call of Duty: Mobile", BotLobbyRankMatchEngine.getGameTitle("com.garena.game.codm"));
        assertEquals("Call of Duty: Mobile", BotLobbyRankMatchEngine.getGameTitle("com.activision.callofduty.shooter"));
        assertEquals("PUBG Mobile", BotLobbyRankMatchEngine.getGameTitle("com.tencent.ig"));
        assertEquals("PUBG Mobile", BotLobbyRankMatchEngine.getGameTitle("com.pubg.imobile"));
        assertEquals("Garena Free Fire", BotLobbyRankMatchEngine.getGameTitle("com.dts.freefireth"));
        assertEquals("Garena Free Fire", BotLobbyRankMatchEngine.getGameTitle("com.dts.freefiremax"));
        assertEquals("League of Legends: Wild Rift", BotLobbyRankMatchEngine.getGameTitle("com.riotgames.league.wildrift"));

        assertNotNull(BotLobbyRankMatchEngine.getGameTitle(null));
        assertNotNull(BotLobbyRankMatchEngine.getGameTitle("com.unknown.game"));
    }

    @Test
    public void testBestBotClusterForGame() {
        String mlbbCluster = BotLobbyRankMatchEngine.getBestBotClusterForGame("com.mobile.legends");
        assertNotNull(mlbbCluster);
        assertTrue(mlbbCluster.contains("Bot Lobby Cluster") || mlbbCluster.contains("KH"));

        String codmCluster = BotLobbyRankMatchEngine.getBestBotClusterForGame("com.garena.game.codm");
        assertNotNull(codmCluster);
        assertTrue(codmCluster.contains("Matchmaker Gateway") || codmCluster.contains("IN"));

        String pubgCluster = BotLobbyRankMatchEngine.getBestBotClusterForGame("com.tencent.ig");
        assertNotNull(pubgCluster);
        assertTrue(pubgCluster.contains("Bot Match Cluster") || pubgCluster.contains("KZ"));

        String ffCluster = BotLobbyRankMatchEngine.getBestBotClusterForGame("com.dts.freefireth");
        assertNotNull(ffCluster);
        assertTrue(ffCluster.contains("Low-Density Gateway") || ffCluster.contains("BD"));

        assertNotNull(BotLobbyRankMatchEngine.getBestBotClusterForGame(null));
    }

    @Test
    public void testBotLobbyKeysContainEssentialMatchmakingOverrides() {
        String[] mlbbKeys = BotLobbyRankMatchEngine.getBotLobbyKeysForGame("com.mobile.legends");
        assertNotNull(mlbbKeys);
        assertTrue(mlbbKeys.length >= 10);
        boolean hasBotEnable = false;
        boolean hasAiOverride = false;
        boolean hasRankBalancer = false;
        for (String k : mlbbKeys) {
            if (k.equals("BotLobbyEnable=1")) hasBotEnable = true;
            if (k.equals("AiMatchOverride=1")) hasAiOverride = true;
            if (k.equals("RankMatchmakingBalancer=1")) hasRankBalancer = true;
        }
        assertTrue("MLBB must have BotLobbyEnable=1", hasBotEnable);
        assertTrue("MLBB must have AiMatchOverride=1", hasAiOverride);
        assertTrue("MLBB must have RankMatchmakingBalancer=1", hasRankBalancer);

        String[] codmKeys = BotLobbyRankMatchEngine.getBotLobbyKeysForGame("com.garena.game.codm");
        assertNotNull(codmKeys);
        boolean hasCodmBot = false;
        boolean hasCodmSbmm = false;
        for (String k : codmKeys) {
            if (k.equals("BotLobby=1")) hasCodmBot = true;
            if (k.equals("SkillBasedMatchmaking=0")) hasCodmSbmm = true;
        }
        assertTrue("CODM must have BotLobby=1", hasCodmBot);
        assertTrue("CODM must disable SBMM", hasCodmSbmm);

        String[] pubgKeys = BotLobbyRankMatchEngine.getBotLobbyKeysForGame("com.tencent.ig");
        assertNotNull(pubgKeys);
        boolean hasPubgBotCVar = false;
        for (String k : pubgKeys) {
            if (k.contains("bEnableBotLobby=1")) hasPubgBotCVar = true;
        }
        assertTrue("PUBGM must have bEnableBotLobby CVar", hasPubgBotCVar);
    }

    @Test
    public void testEngageBotLobbyRankMatchSafeExecution() {
        BotLobbyRankMatchEngine.RankMatchOptimizationResult result =
                BotLobbyRankMatchEngine.engageBotLobbyRankMatch(null, "com.mobile.legends");
        assertNotNull(result);
        assertTrue(result.success);
        assertEquals("com.mobile.legends", result.targetPackage);
        assertEquals("Mobile Legends: Bang Bang", result.gameTitle);
        assertNotNull(result.assignedServerCluster);
        assertTrue(result.serverPingMs >= 0);
        assertNotNull(result.summary);
        assertTrue(result.summary.contains("Bot Lobby"));
    }

    @Test
    public void testApplyBotLobbyEdgeCases() {
        assertEquals(0, BotLobbyRankMatchEngine.applyBotLobbyAndRankMatchmaker(null));
        assertEquals(0, BotLobbyRankMatchEngine.applyBotLobbyAndRankMatchmaker(""));
        assertEquals(0, BotLobbyRankMatchEngine.applyBotLobbyAndRankMatchmaker("   "));
    }

    @Test
    public void testGameServerPingRadarBotServers() {
        assertNotNull(GameServerPingRadar.BOT_LOBBY_SERVERS);
        assertFalse(GameServerPingRadar.BOT_LOBBY_SERVERS.isEmpty());

        List<GameServerPingRadar.ServerTarget> mlbbTargets =
                GameServerPingRadar.getBotServersForPackage("com.mobile.legends");
        assertNotNull(mlbbTargets);
        assertFalse(mlbbTargets.isEmpty());
        assertTrue(mlbbTargets.get(0).gameTitle.contains("Mobile Legends"));

        GameServerPingRadar.PingResult pingResult =
                GameServerPingRadar.measureBestBotServer("com.mobile.legends");
        assertNotNull(pingResult);
        assertNotNull(pingResult.region);
        assertTrue(pingResult.latencyMs >= 0 || !pingResult.reachable);
    }
}
