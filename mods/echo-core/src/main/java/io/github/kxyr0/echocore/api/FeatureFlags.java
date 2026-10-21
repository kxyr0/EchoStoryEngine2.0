package io.github.kxyr0.echocore.api;

import io.github.kxyr0.echocore.features.FeatureClientState;
import io.github.kxyr0.echocore.features.FeatureNetwork;
import io.github.kxyr0.echocore.features.FeatureSavedData;
import java.util.UUID;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class FeatureFlags {
    public static final String THOUGHT_SPACE = "thought_space_unlocked";
    public static final String MAGIC = "magic_unlocked";
    private FeatureFlags() { }
    public static boolean clientAllowed(UUID player, String flag) { return FeatureClientState.allowed(player, flag); }
    public static boolean serverAllowed(UUID player, String flag) {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server != null && FeatureSavedData.get(server).state().allowed(player, flag);
    }
    /** Invoke on the server thread from a story action or command. */
    public static void set(UUID player, String flag, boolean value) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isSameThread()) throw new IllegalStateException("Feature flags require the server thread");
        var data = FeatureSavedData.get(server); data.state().set(player, flag, value); data.setDirty();
        var online = server.getPlayerList().getPlayer(player);
        if (online != null) FeatureNetwork.sync(online);
    }
}
