package com.gamebooster.app.booster;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SnapshotSystemTest {

    @Test
    public void testIsThermalKey() {
        assertTrue(SnapshotSystem.isThermalKey("/sys/class/thermal/thermal_zone0/mode"));
        assertTrue(SnapshotSystem.isThermalKey("/sys/class/thermal/thermal_zone0/trip_point_0_temp"));
        assertTrue(SnapshotSystem.isThermalKey("/sys/class/thermal/cooling_device0/cur_state"));
        assertTrue(SnapshotSystem.isThermalKey("/sys/class/kgsl/kgsl-3d0/thermal_pwrlevel"));
        assertTrue(SnapshotSystem.isThermalKey("/sys/class/kgsl/kgsl-3d0/throttling"));
        assertTrue(SnapshotSystem.isThermalKey("debug.thermal.throttle.disable"));
        assertFalse(SnapshotSystem.isThermalKey("/sys/devices/system/cpu/cpufreq/policy0/scaling_governor"));
        assertFalse(SnapshotSystem.isThermalKey("system.min_refresh_rate"));
        assertFalse(SnapshotSystem.isThermalKey(null));
    }

    @Test
    public void testRefreshPairSafe() {
        assertTrue(SnapshotSystem.refreshPairSafe("60", "120"));
        assertTrue(SnapshotSystem.refreshPairSafe("120", "120"));
        assertTrue(SnapshotSystem.refreshPairSafe("60.0", "185.0"));
        assertFalse(SnapshotSystem.refreshPairSafe("144", "60"));
        assertTrue(SnapshotSystem.refreshPairSafe(null, "120"));
        assertTrue(SnapshotSystem.refreshPairSafe("60", null));
        assertTrue(SnapshotSystem.refreshPairSafe("invalid", "120"));
    }

    @Test
    public void testSerializationAndDeserializationRoundTrip() {
        List<SnapshotSystem.Entry> entries = new ArrayList<>();
        entries.add(new SnapshotSystem.Entry(SnapshotSystem.EntryType.SYSFS, "/sys/devices/system/cpu/cpufreq/policy0/scaling_governor", "schedutil"));
        entries.add(new SnapshotSystem.Entry(SnapshotSystem.EntryType.SETTING, "system.peak_refresh_rate", "120"));
        entries.add(new SnapshotSystem.Entry(SnapshotSystem.EntryType.PROP, "debug.thermal.throttle.disable", "0"));

        long now = System.currentTimeMillis();
        SnapshotSystem.Snapshot original = new SnapshotSystem.Snapshot("pre-boost-test", now, entries);

        String json = SnapshotSystem.serialize(original);
        assertNotNull(json);
        assertFalse(json.isEmpty());

        SnapshotSystem.Snapshot restored = SnapshotSystem.deserialize(json);
        assertNotNull(restored);
        assertEquals("pre-boost-test", restored.label);
        assertEquals(now, restored.capturedAtMs);
        assertEquals(3, restored.entries.size());

        assertEquals(SnapshotSystem.EntryType.SYSFS, restored.entries.get(0).type);
        assertEquals("/sys/devices/system/cpu/cpufreq/policy0/scaling_governor", restored.entries.get(0).key);
        assertEquals("schedutil", restored.entries.get(0).value);

        assertEquals(SnapshotSystem.EntryType.SETTING, restored.entries.get(1).type);
        assertEquals("system.peak_refresh_rate", restored.entries.get(1).key);
        assertEquals("120", restored.entries.get(1).value);

        assertEquals(SnapshotSystem.EntryType.PROP, restored.entries.get(2).type);
        assertEquals("debug.thermal.throttle.disable", restored.entries.get(2).key);
        assertEquals("0", restored.entries.get(2).value);
    }

    @Test(expected = IllegalStateException.class)
    public void testDeserializeNullThrows() {
        SnapshotSystem.deserialize(null);
    }

    @Test(expected = IllegalStateException.class)
    public void testDeserializeInvalidJsonThrows() {
        SnapshotSystem.deserialize("{invalid-json");
    }
}
