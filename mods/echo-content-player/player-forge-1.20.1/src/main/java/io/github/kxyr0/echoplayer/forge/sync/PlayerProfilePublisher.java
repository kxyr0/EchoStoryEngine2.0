package io.github.kxyr0.echoplayer.forge.sync;

import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Publishes saved server-authoritative profile snapshots to online clients.
 */
public final class PlayerProfilePublisher {
    private final PlayerProfileSyncChannel syncChannel;

    public PlayerProfilePublisher(PlayerProfileSyncChannel syncChannel) {
        this.syncChannel = Objects.requireNonNull(syncChannel, "syncChannel");
    }

    public void publish(PlayerProfileSnapshot snapshot) {
        syncChannel.sendToAll(snapshot);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(snapshot.playerId().accountUuid());
        if (player != null) {
            player.refreshDisplayName();
            player.refreshTabListName();
        }
    }
}
