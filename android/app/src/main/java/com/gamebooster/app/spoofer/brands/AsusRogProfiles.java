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
     * Direct accessor for the single active profile — ROG Phone 10 Pro+.
     * Used by SpoofProfileRegistry and DeviceSpooferEngine for locked single-profile mode.
     */
    public static SpoofProfile getRog9ProProfile() {
        return new SpoofProfile(
                "asus_rog9_pro",
                "ASUS ROG Phone 10 Pro+ (Snapdragon 8 Elite Gen 2 / 165Hz)",
                "ASUS ROG",
                "ASUS_AI2601_A",
                "asus",
                "asus",
                "ASUS_AI2601",
                "WW_AI2601",
                "AI2601",
                "qcom",
                "sun",
                "SM8750-AC",
                "sun",
                "Snapdragon 8 Elite Gen 2",
                "Qualcomm",
                8,
                4320000,
                "ARM64-v9.2-A",
                "fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm lrcpc dcpop sha3 asimddp sha512 sve asimdfhm",
                "asus/WW_AI2601/ASUS_AI2601:15/UKQ1.231003.002/36.0810.0810.01:user/release-keys",
                "36.0810.0810.01",
                "15",
                35,
                "2026-01-01",
                "Adreno (TM) 840",
                "Qualcomm",
                "OpenGL ES 3.2 V@0615.0",
                "1.3.280",
                "512.615.0",
                24576,
                20480,
                165
        );
    }

    public static List<SpoofProfile> getProfiles() {
        List<SpoofProfile> list = new ArrayList<>();

        // ASUS ROG Phone 10 Pro+ (Snapdragon 8 Elite Gen 2 / Adreno 840 / 24GB LPDDR5X / 165Hz AMOLED)
        // Single locked flagship identity for all spoofing operations
        list.add(getRog9ProProfile());

        return list;
    }
}
