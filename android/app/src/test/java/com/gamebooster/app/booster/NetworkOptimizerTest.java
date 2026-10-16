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
        assertEquals("one.one.one.one", NetworkOptimizer.DnsMode.CLOUDFLARE_1_1_1_1.privateDnsHost);
        assertEquals("dns.google", NetworkOptimizer.DnsMode.GOOGLE_8_8_8_8.privateDnsHost);
        assertEquals("dns.adguard-dns.com", NetworkOptimizer.DnsMode.ADGUARD_GAMING.privateDnsHost);
        assertEquals("dns.quad9.net", NetworkOptimizer.DnsMode.QUAD9_GAMING.privateDnsHost);
        assertEquals("off", NetworkOptimizer.DnsMode.SYSTEM_DEFAULT.privateDnsHost);
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
        assertEquals(1460, NetworkOptimizer.PhCarrier.TNT_SMART.mtu);
        assertEquals(1440, NetworkOptimizer.PhCarrier.TM_GLOBE.mtu);
        assertEquals(1460, NetworkOptimizer.PhCarrier.DITO.mtu);
    }

    @Test
    public void testPingStatsQualityClassificationAndBoundaries() {
        // 1. Excellent: 1ms to 34ms
        NetworkOptimizer.PingStats p1 = new NetworkOptimizer.PingStats(1, 1, 0, true);
        assertEquals("[EXCELLENT / ULTRA LOW LATENCY]", p1.quality);
        assertEquals(0xFF00FF66, p1.qualityColor);

        NetworkOptimizer.PingStats p34 = new NetworkOptimizer.PingStats(34, 2, 0, true);
        assertEquals("[EXCELLENT / ULTRA LOW LATENCY]", p34.quality);
        assertEquals(0xFF00FF66, p34.qualityColor);

        // 2. Good: 35ms to 69ms
        NetworkOptimizer.PingStats p35 = new NetworkOptimizer.PingStats(35, 3, 0, true);
        assertEquals("[GOOD / FAST GAMING ROUTE]", p35.quality);
        assertEquals(0xFF00F0FF, p35.qualityColor);

        NetworkOptimizer.PingStats p69 = new NetworkOptimizer.PingStats(69, 5, 0, true);
        assertEquals("[GOOD / FAST GAMING ROUTE]", p69.quality);
        assertEquals(0xFF00F0FF, p69.qualityColor);

        // 3. Normal / Acceptable: 70ms to 109ms
        NetworkOptimizer.PingStats p70 = new NetworkOptimizer.PingStats(70, 5, 0, true);
        assertEquals("[NORMAL / ACCEPTABLE]", p70.quality);
        assertEquals(0xFFFACC15, p70.qualityColor);

        NetworkOptimizer.PingStats p109 = new NetworkOptimizer.PingStats(109, 8, 0, true);
        assertEquals("[NORMAL / ACCEPTABLE]", p109.quality);
        assertEquals(0xFFFACC15, p109.qualityColor);

        // 4. High Latency: 110ms and above
        NetworkOptimizer.PingStats p110 = new NetworkOptimizer.PingStats(110, 15, 2, true);
        assertEquals("[HIGH LATENCY]", p110.quality);
        assertEquals(0xFFF97316, p110.qualityColor);

        NetworkOptimizer.PingStats p200 = new NetworkOptimizer.PingStats(200, 25, 10, true);
        assertEquals("[HIGH LATENCY]", p200.quality);
        assertEquals(0xFFF97316, p200.qualityColor);

        // 5. Offline / Timeout / Zero Latency
        NetworkOptimizer.PingStats pTimeout = new NetworkOptimizer.PingStats(-1, 0, 100, false);
        assertEquals("[OFFLINE / TIMEOUT]", pTimeout.quality);
        assertEquals(0xFFEF4444, pTimeout.qualityColor);

        NetworkOptimizer.PingStats pZero = new NetworkOptimizer.PingStats(0, 0, 100, true);
        assertEquals("[OFFLINE / TIMEOUT]", pZero.quality);
        assertEquals(0xFFEF4444, pZero.qualityColor);

        NetworkOptimizer.PingStats pUnreachable = new NetworkOptimizer.PingStats(40, 2, 50, false);
        assertEquals("[OFFLINE / TIMEOUT]", pUnreachable.quality);
        assertEquals(0xFFEF4444, pUnreachable.qualityColor);
    }

    @Test
    public void testNetworkModesAreValid() {
        for (NetworkOptimizer.NetworkMode mode : NetworkOptimizer.NetworkMode.values()) {
            assertNotNull("NetworkMode title must not be null for " + mode.name(), mode.title);
            assertFalse("NetworkMode title must not be empty for " + mode.name(), mode.title.trim().isEmpty());
        }
        assertEquals(4, NetworkOptimizer.NetworkMode.values().length);
    }

    @Test
    public void testNetworkTelemetryModel() {
        NetworkOptimizer.NetworkTelemetry telemetry =
                new NetworkOptimizer.NetworkTelemetry("wlan0", "1.1.1.1", true, false, true);
        assertEquals("wlan0", telemetry.activeInterface);
        assertEquals("1.1.1.1", telemetry.activeDns);
        assertTrue(telemetry.isWifiLockHeld);
        assertFalse(telemetry.isCellularActive);
        assertTrue(telemetry.isWifiActive);
    }

    @Test
    public void testCarrierDetectionNullContextFallback() {
        String detected = NetworkOptimizer.detectActiveSimCarriers(null);
        assertEquals("Unknown / No SIM", detected);

        NetworkOptimizer.PhCarrier carrierEnum = NetworkOptimizer.detectActiveSimCarrierEnum(null);
        assertEquals(NetworkOptimizer.PhCarrier.TNT_SMART, carrierEnum);
    }

    @Test
    public void testCarrierOptimizationNullSafety() {
        assertFalse(NetworkOptimizer.applyPhCarrierOptimization(null, null));
    }

    @Test
    public void testOptimizeAllForGamingExecutionSafety() {
        // Must handle null context gracefully without unhandled exceptions
        boolean result = NetworkOptimizer.optimizeAllForGaming(null, null);
        assertNotNull(result);
    }
}
