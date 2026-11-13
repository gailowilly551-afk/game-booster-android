package com.gamebooster.app.spoofer;

import com.gamebooster.app.device.DeviceDetector;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpoofSanityCheckerTest {

    @Test
    public void inferGpuFamily_detectsRendererAndVendor() {
        assertEquals(SpoofSanityChecker.GpuFamily.ADRENO,
                SpoofSanityChecker.inferGpuFamily("Qualcomm", "Adreno (TM) 840"));
        assertEquals(SpoofSanityChecker.GpuFamily.MALI,
                SpoofSanityChecker.inferGpuFamily("ARM", "Mali-G720 Immortalis"));
        assertEquals(SpoofSanityChecker.GpuFamily.APPLE,
                SpoofSanityChecker.inferGpuFamily("Apple Inc.", "Apple A18 Pro GPU"));
        assertEquals(SpoofSanityChecker.GpuFamily.UNKNOWN,
                SpoofSanityChecker.inferGpuFamily("", ""));
    }

    @Test
    public void inferDeviceGpuFamily_mapsChipsetVendors() {
        assertEquals(SpoofSanityChecker.GpuFamily.ADRENO,
                SpoofSanityChecker.inferDeviceGpuFamily(DeviceDetector.ChipsetVendor.QUALCOMM));
        assertEquals(SpoofSanityChecker.GpuFamily.MALI,
                SpoofSanityChecker.inferDeviceGpuFamily(DeviceDetector.ChipsetVendor.MEDIATEK));
        assertEquals(SpoofSanityChecker.GpuFamily.MALI,
                SpoofSanityChecker.inferDeviceGpuFamily(DeviceDetector.ChipsetVendor.EXYNOS));
        assertEquals(SpoofSanityChecker.GpuFamily.UNKNOWN,
                SpoofSanityChecker.inferDeviceGpuFamily(null));
    }
}

