package com.gamebooster.app.spoofer.games;

import android.util.Log;
import com.gamebooster.app.games.GameFamily;
import com.gamebooster.app.spoofer.SpoofProfile;

/**
 * GameHardwareMaskCoordinator — Decouples and routes game-specific hardware masking
 * to dedicated, non-overlapping masker engines.
 */
public final class GameHardwareMaskCoordinator {

    private static final String TAG = "GameHwMaskCoordinator";

    private GameHardwareMaskCoordinator() {}

    /**
     * Injects hardware profile using the strictly isolated masker corresponding to the game family.
     * Returns true if a dedicated masker was invoked and executed.
     */
    public static boolean injectGameHardware(String packageName, SpoofProfile profile) {
        if (packageName == null || profile == null) return false;
        GameFamily family = GameFamily.fromPackage(packageName);

        Log.i(TAG, "Routing hardware mask for " + packageName + " -> GameFamily: " + family);

        switch (family) {
            case MLBB:
                return MlbbHardwareMasker.inject(packageName, profile);
            case CODM:
                return CodmHardwareMasker.inject(packageName, profile);
            case PUBGM:
                return PubgmHardwareMasker.inject(packageName, profile);
            default:
                Log.d(TAG, "No dedicated per-game masker required for family: " + family);
                return false;
        }
    }
}
