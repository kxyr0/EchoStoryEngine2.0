package io.github.kxyr0.echothoughtspace.forge.client;

import io.github.kxyr0.echocore.api.FeatureFlags;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;

public final class ThoughtSpaceFeatureAccess {
    private ThoughtSpaceFeatureAccess() { }
    public static boolean allowed() {
        if (!ModList.get().isLoaded("echo_core")) return true;
        var player = Minecraft.getInstance().player;
        return player != null && FeatureFlags.clientAllowed(player.getUUID(), FeatureFlags.THOUGHT_SPACE);
    }
}
