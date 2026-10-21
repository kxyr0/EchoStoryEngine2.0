package io.github.kxyr0.echomagic.forge.service;

import io.github.kxyr0.echocore.api.FeatureFlags;
import java.util.UUID;
import net.minecraftforge.fml.ModList;

/** Optional story feature gate; standalone Echo Magic retains its existing behavior. */
public final class MagicFeatureAccess {
    private MagicFeatureAccess() { }
    public static boolean client(UUID player) {
        return !ModList.get().isLoaded("echo_core") || FeatureFlags.clientAllowed(player, FeatureFlags.MAGIC);
    }
    public static boolean server(UUID player) {
        return !ModList.get().isLoaded("echo_core") || FeatureFlags.serverAllowed(player, FeatureFlags.MAGIC);
    }
}
