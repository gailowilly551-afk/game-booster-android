package com.gamebooster.app.booster;

import org.junit.Test;
import static org.junit.Assert.*;

public class NetworkOptimizerTest {

    @Test
    public void testDnsModesAreValid() {
        for (NetworkOptimizer.DnsMode mode : NetworkOptimizer.DnsMode.values()) {
            assertNotNull("DnsMode primary IP must not be null for " + mode.name(), mode.primary);
            assertNotNull("DnsMode secondary IP must not be null for " + mode.name(), mode.secondary);
            assertNotNull("DnsMode private DNS host must not be null for " + mode.name(), mode.privateDnsHost);
            assertFalse("Private DNS host must not be empty for " + mode.name(), mode.privateDnsHost.trim().isEmpty());
        }
    }

    @Test
    public void testPhCarriersAreValid() {
        for (NetworkOptimizer.PhCarrier carrier : NetworkOptimizer.PhCarrier.values()) {
            assertNotNull("Carrier title must not be null for " + carrier.name(), carrier.title);
            assertNotNull("Default APN must not be null for " + carrier.name(), carrier.defaultApn);
            assertNotNull("Primary DNS must not be null for " + carrier.name(), carrier.dnsPrimary);
            assertNotNull("Private DNS host must not be null for " + carrier.name(), carrier.privateDnsHost);
            assertNotNull("Best bands must not be null for " + carrier.name(), carrier.bestBands);
            assertTrue("MTU must be standard gaming MTU (1400-1500) for " + carrier.name(),
                    carrier.mtu >= 1400 && carrier.mtu <= 1500);
        }
    }

    @Test
    public void testPingStatsQualityClassification() {
        NetworkOptimizer.PingStats excellent = new NetworkOptimizer.PingStats(25, 2, 0, true);
        assertEquals("[EXCELLENT / ULTRA LOW LATENCY]", excellent.quality);
        assertEquals(0xFF00FF66, excellent.qualityColor);

        NetworkOptimizer.PingStats good = new NetworkOptimizer.PingStats(50, 5, 0, true);
        assertEquals("[GOOD / FAST GAMING ROUTE]", good.quality);
        assertEquals(0xFF00F0FF, good.qualityColor);

        NetworkOptimizer.PingStats normal = new NetworkOptimizer.PingStats(90, 8, 0, true);
        assertEquals("[NORMAL / ACCEPTABLE]", normal.quality);
        assertEquals(0xFFFACC15, normal.qualityColor);

        NetworkOptimizer.PingStats highLatency = new NetworkOptimizer.PingStats(150, 20, 5, true);
        assertEquals("[HIGH LATENCY]", highLatency.quality);
        assertEquals(0xFFF97316, highLatency.qualityColor);

        NetworkOptimizer.PingStats timeout = new NetworkOptimizer.PingStats(-1, 0, 100, false);
        assertEquals("[OFFLINE / TIMEOUT]", timeout.quality);
        assertEquals(0xFFEF4444, timeout.qualityColor);

        NetworkOptimizer.PingStats zeroLatencyUnreachable = new NetworkOptimizer.PingStats(0, 0, 100, true);
        assertEquals("[OFFLINE / TIMEOUT]", zeroLatencyUnreachable.quality);
    }

    @Test
    public void testNetworkModesAreValid() {
        for (NetworkOptimizer.NetworkMode mode : NetworkOptimizer.NetworkMode.values()) {
            assertNotNull("NetworkMode title must not be null for " + mode.name(), mode.title);
            assertFalse("NetworkMode title must not be empty for " + mode.name(), mode.title.trim().isEmpty());
        }
    }
}
