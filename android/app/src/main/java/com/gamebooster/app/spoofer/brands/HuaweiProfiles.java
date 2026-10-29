package com.gamebooster.app.spoofer.brands;

import com.gamebooster.app.spoofer.SpoofProfile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HuaweiProfiles {

    public static List<SpoofProfile> getProfiles() {
        List<SpoofProfile> list = new ArrayList<>();

        // 1. Huawei Pura 70 Ultra
        list.add(new SpoofProfile(
                "huawei_pura_70_ultra",
                "Huawei Pura 70 Ultra (Kirin 9010 / 120Hz)",
                "Huawei",
                "HBP-AL00",
                "HUAWEI",
                "HUAWEI",
                "HBP",
                "HBP-AL00",
                "HBP",
                "kirin9010",
                "kirin9010",
                "kirin9010",
                "kirin9010",
                "HiSilicon Kirin 9010",
                "HiSilicon",
                8,
                2300000,
                "ARM64-v8a",
                "fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm",
                "HUAWEI/HBP-AL00/HBP:12/HUAWEIHBP-AL00/14.2.0.120:user/release-keys",
                "HUAWEIHBP-AL00 14.2.0.120(C00E120R3P5)",
                "14",
                34,
                "2024-05-01",
                "Maleoon 910",
                "HiSilicon",
                "OpenGL ES 3.2",
                "1.3.0",
                "1.0.0",
                16384,
                12288,
                120
        ));

        // 2. Honor Magic 6 Pro (Snapdragon 8 Gen 3 / 120Hz)
        list.add(new SpoofProfile(
                "honor_magic_6_pro",
                "Honor Magic 6 Pro (Snapdragon 8 Gen 3 / 120Hz)",
                "Honor",
                "BVL-AN16",
                "HONOR",
                "HONOR",
                "BVL",
                "BVL-AN16",
                "BVL",
                "qcom",
                "pineapple",
                "SM8650",
                "pineapple",
                "Snapdragon 8 Gen 3",
                "Qualcomm",
                8,
                3300000,
                "ARM64-v9-A",
                "fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm lrcpc dcpop sha3 asimddp sha512",
                "HONOR/BVL-AN16/BVL:14/HONORBVL-AN16/8.0.0.130:user/release-keys",
                "HONORBVL-AN16 8.0.0.130(C00E125R3P5)",
                "14",
                34,
                "2024-04-01",
                "Adreno (TM) 750",
                "Qualcomm",
                "OpenGL ES 3.2 V@0510.0",
                "1.3.268",
                "512.510.0",
                16384,
                12288,
                120
        ));

        return Collections.unmodifiableList(list);
    }
}
