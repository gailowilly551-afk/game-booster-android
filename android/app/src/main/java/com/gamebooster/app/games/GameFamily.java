package com.gamebooster.app.games;

/**
 * GameFamily — canonical package-family mapping for the supported competitive titles.
 *
 * A single family can be installed under multiple package IDs (regional builds,
 * Garena vs Activision, KR/JP/VNG variants). Per-game preferences are still stored
 * against the real installed package, but shared defaults and patch routing must
 * resolve through the family so aliases never fall through to a global default.
 */
public enum GameFamily {

    MLBB("Mobile Legends: Bang Bang", "com.mobile.legends"),
    CODM("Call of Duty: Mobile", "com.garena.game.codm"),
    PUBGM("PUBG Mobile", "com.tencent.ig"),
    HOK("Honor of Kings", "com.levelinfinite.sgameGlobal"),
    FREE_FIRE("Free Fire", "com.dts.freefireth"),
    OTHER("Other", "");

    public final String displayName;
    public final String primaryPackage;

    GameFamily(String displayName, String primaryPackage) {
        this.displayName = displayName;
        this.primaryPackage = primaryPackage;
    }

    public boolean isSupported() {
        return this != OTHER;
    }

    /** Resolves the family from any known regional/vendor package alias. */
    public static GameFamily fromPackage(String packageName) {
        if (packageName == null) return OTHER;
        String pkg = packageName.trim().toLowerCase();

        if (pkg.contains("mobile.legends") || pkg.contains("mobilelegends")
                || pkg.contains("vng.mlbbvn") || pkg.contains("mlbb")) {
            return MLBB;
        }
        if (pkg.contains("callofduty") || pkg.contains("garena.game.codm")
                || pkg.contains("tencent.tmgp.cod") || pkg.contains("vng.codmvn")
                || pkg.contains("codm") || pkg.contains("warzone")) {
            return CODM;
        }
        if (pkg.contains("pubg") || pkg.contains("tencent.ig") || pkg.contains("igfit")
                || pkg.contains("igce") || pkg.contains("pubgm")
                || pkg.contains("imobile") || pkg.contains("rekoo.pubgm")
                || pkg.contains("bgmi") || pkg.contains("iglite")) {
            return PUBGM;
        }
        if (pkg.contains("sgame") || pkg.contains("levelinfinite")
                || pkg.contains("arenaofvalor") || pkg.contains("kgtw")
                || pkg.contains("kgvn") || pkg.contains("kgid")
                || pkg.contains("honorofkings") || pkg.contains("hok")) {
            return HOK;
        }
        if (pkg.contains("freefire") || pkg.contains("dts.freefire")) {
            return FREE_FIRE;
        }
        return OTHER;
    }

    /** Legacy default target for a family when the user has never chosen one. */
    public int defaultTargetHz() {
        switch (this) {
            case MLBB:
            case CODM:
            case PUBGM:
            case HOK:
                return 185;
            case FREE_FIRE:
                return 120;
            default:
                return 60;
        }
    }

    public static GameFamily[] supportedFamilies() {
        return new GameFamily[]{MLBB, CODM, PUBGM, HOK, FREE_FIRE};
    }
}
