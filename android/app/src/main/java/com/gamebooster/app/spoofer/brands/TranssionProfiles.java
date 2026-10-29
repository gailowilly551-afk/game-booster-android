package com.gamebooster.app.spoofer.brands;

import com.gamebooster.app.spoofer.SpoofProfile;
import java.util.ArrayList;
import java.util.List;

/**
 * TranssionProfiles — Infinix & Tecno flagship gaming device profiles.
 * Features 100% authentic, whitelisted hardware parameters and official XOS / HiOS release builds.
 */
public class TranssionProfiles {

    public static List<SpoofProfile> getProfiles() {
        List<SpoofProfile> list = new ArrayList<>();

        // 1. Infinix GT 20 Pro (Dimensity 8200 Ultimate / Pixelworks X5 Turbo / 144Hz AMOLED / 12GB RAM)
        list.add(new SpoofProfile(
                "infinix_gt_20_pro",
                "Infinix GT 20 Pro 5G (Dimensity 8200-U / 144Hz)",
                "Infinix",
                "Infinix X6871",
                "Infinix",
                "Infinix",
                "Infinix-X6871",
                "X6871-GL",
                "Infinix-X6871",
                "mtk",
                "k6895v1_64",
                "MT6895Z_TN/CZA",
                "k6895v1_64",
                "MediaTek Dimensity 8200 Ultimate",
                "MediaTek",
                8,
                3100000,
                "ARM64-v8a",
                "fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm lrcpc dcpop asimddp",
                "Infinix/X6871-GL/Infinix-X6871:14/UP1A.231005.007/240417V101:user/release-keys",
                "X6871-XOSV14.0.0-U-240417V101",
                "14",
                34,
                "2024-05-01",
                "Mali-G610 MC6",
                "ARM",
                "OpenGL ES 3.2 v1.r38p0-01eac0",
                "1.3.268",
                "38.0.0",
                12288,
                9216,
                144
        ));

        // 2. Tecno Camon 30 Premier 5G (Dimensity 8200 Ultimate / PolarAce / 144Hz 1.5K LTPO / 12GB RAM)
        list.add(new SpoofProfile(
                "tecno_camon30_premier",
                "Tecno Camon 30 Premier 5G (Dimensity 8200-U / 144Hz)",
                "Tecno",
                "TECNO CL9",
                "TECNO",
                "TECNO",
                "TECNO-CL9",
                "CL9-GL",
                "TECNO-CL9",
                "mtk",
                "k6895v1_64",
                "MT6895Z_TN/CZA",
                "k6895v1_64",
                "MediaTek Dimensity 8200 Ultimate",
                "MediaTek",
                8,
                3100000,
                "ARM64-v8a",
                "fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm lrcpc dcpop asimddp",
                "TECNO/CL9-GL/TECNO-CL9:14/UP1A.231005.007/240508V134:user/release-keys",
                "CL9-HiOS14.0.0-U-240508V134",
                "14",
                34,
                "2024-05-01",
                "Mali-G610 MC6",
                "ARM",
                "OpenGL ES 3.2 v1.r38p0-01eac0",
                "1.3.268",
                "38.0.0",
                12288,
                9216,
                144
        ));

        // 3. Tecno Pova 6 Pro 5G (Dimensity 6080 / 120Hz Dynamic AMOLED / 12GB RAM)
        list.add(new SpoofProfile(
                "tecno_pova_6_pro",
                "Tecno Pova 6 Pro 5G (Dimensity 6080 / 120Hz)",
                "Tecno",
                "TECNO LI9",
                "TECNO",
                "TECNO",
                "TECNO-LI9",
                "LI9-GL",
                "TECNO-LI9",
                "mtk",
                "k6833v1_64",
                "MT6833P",
                "k6833v1_64",
                "MediaTek Dimensity 6080",
                "MediaTek",
                8,
                2400000,
                "ARM64-v8a",
                "fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm",
                "TECNO/LI9-GL/TECNO-LI9:14/UP1A.231005.007/240402V128:user/release-keys",
                "LI9-HiOS14.0.0-U-240402V128",
                "14",
                34,
                "2024-04-01",
                "Mali-G57 MC2",
                "ARM",
                "OpenGL ES 3.2 v1.r32p1-01eac0",
                "1.3.250",
                "32.0.0",
                12288,
                8192,
                120
        ));

        // 4. Infinix GT 10 Pro (Dimensity 8050 / 120Hz AMOLED / Cyber Mecha Design)
        list.add(new SpoofProfile(
                "infinix_gt_10_pro",
                "Infinix GT 10 Pro (Dimensity 8050 / 120Hz)",
                "Infinix",
                "Infinix X6739",
                "Infinix",
                "Infinix",
                "Infinix-X6739",
                "X6739-GL",
                "Infinix-X6739",
                "mtk",
                "k6893v1_64",
                "MT6893Z",
                "k6893v1_64",
                "MediaTek Dimensity 8050",
                "MediaTek",
                8,
                3000000,
                "ARM64-v8a",
                "fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm",
                "Infinix/X6739-GL/Infinix-X6739:13/TP1A.220624.014/231108V115:user/release-keys",
                "X6739-XOSV13.1.0-T-231108V115",
                "13",
                33,
                "2023-11-01",
                "Mali-G77 MC9",
                "ARM",
                "OpenGL ES 3.2 v1.r26p0-01eac0",
                "1.3.238",
                "26.0.0",
                8192,
                6144,
                120
        ));

        return list;
    }
}
