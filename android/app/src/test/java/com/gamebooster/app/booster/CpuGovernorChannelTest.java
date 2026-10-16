package com.gamebooster.app.booster;

import org.junit.Test;
import static org.junit.Assert.*;

public class CpuGovernorChannelTest {

    @Test
    public void testDetectCpuCoreCount() {
        int coreCount = CpuGovernorChannel.detectCpuCoreCount();
        assertTrue("Detected core count must be at least 1", coreCount >= 1);
    }

    @Test
    public void testDetectCpuClusterTopology() {
        int[][] topology = CpuGovernorChannel.detectCpuClusterTopology();
        assertNotNull("Topology must not be null", topology);
        assertTrue("Topology must have at least one cluster", topology.length >= 1);

        for (int[] cluster : topology) {
            assertEquals("Cluster info must have 3 elements: [start, end, type]", 3, cluster.length);
            assertTrue("Start core must be non-negative", cluster[0] >= 0);
            assertTrue("End core must be >= start core", cluster[1] >= cluster[0]);
            assertTrue("Cluster type must be 0 (efficiency), 1 (big), or 2 (prime)",
                    cluster[2] >= 0 && cluster[2] <= 2);
        }
    }
}
