package com.gamebooster.app.spoofer.brands;

import com.gamebooster.app.spoofer.SpoofProfile;
import java.util.ArrayList;
import java.util.List;

/**
 * AsusRogProfiles — ASUS Republic of Gamers flagship gaming device profiles.
 * Consolidated to single active flagship: ROG Phone 9 Pro (Snapdragon 8 Elite / 185Hz).
 */
public class AsusRogProfiles {

    /**
     * Direct accessor for the single active profile — ROG Phone 9 Pro.
     * Used by SpoofProfileRegistry and DeviceSpooferEngine for locked single-profile mode.
     */
    public static SpoofProfile getRog9ProProfile() {
        return new SpoofProfile(
                "asus_rog9_pro",
                "ASUS ROG Phone 9 Pro (Snapdragon 8 Elite / 185Hz)",
                "ASUS ROG",
                "ASUS_AI2501_Pro",
                "asus",
                "asus",
                "ASUS_AI2501",
                "WW_AI2501",
                "AI2501",
                "qcom",
                "sun",
                "SM8750-AB",
                "sun",
                "Snapdragon 8 Elite",
                "Qualcomm",
                8,
                4320000,
                "ARM64-v9.2-A",
                "fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm lrcpc dcpop sha3 asimddp sha512 sve asimdfhm",
                "asus/WW_AI2501/ASUS_AI2501:15/UKQ1.231003.002/35.0810.0810.27:user/release-keys",
                "35.0810.0810.27",
                "15",
                35,
                "2025-01-01",
                "Adreno (TM) 830",
                "Qualcomm",
                "OpenGL ES 3.2 V@0615.0",
                "1.3.280",
                "512.615.0",
                16384,
                13107,
                185
        );
    }

    public static List<SpoofProfile> getProfiles() {
        List<SpoofProfile> list = new ArrayList<>();

        // ASUS ROG Phone 9 Pro (Snapdragon 8 Elite / Adreno 830 / 16GB LPDDR5X / 185Hz AMOLED)
        // Only this flagship is retained — ROG 8 Pro and ROG 7 Ultimate removed.
        list.add(new SpoofProfile(
                "asus_rog9_pro",
                "ASUS ROG Phone 9 Pro (Snapdragon 8 Elite / 185Hz)",
                "ASUS ROG",
                "ASUS_AI2501_Pro",
                "asus",
                "asus",
                "ASUS_AI2501",
                "WW_AI2501",
                "AI2501",
                "qcom",
                "sun",
                "SM8750-AB",
                "sun",
                "Snapdragon 8 Elite",
                "Qualcomm",
                8,
                4320000,
                "ARM64-v9.2-A",
                "fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm lrcpc dcpop sha3 asimddp sha512 sve asimdfhm",
                "asus/WW_AI2501/ASUS_AI2501:15/UKQ1.231003.002/35.0810.0810.27:user/release-keys",
                "35.0810.0810.27",
                "15",
                35,
                "2025-01-01",
                "Adreno (TM) 830",
                "Qualcomm",
                "OpenGL ES 3.2 V@0615.0",
                "1.3.280",
                "512.615.0",
                16384,
                13107,
                185
        ));

        return list;
    }
}
