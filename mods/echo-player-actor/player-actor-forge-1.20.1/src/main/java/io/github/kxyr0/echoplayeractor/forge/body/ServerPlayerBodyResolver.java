package io.github.kxyr0.echoplayeractor.forge.body;

import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ServerPlayerBodyResolver {
    private final MinecraftServer server;

    public ServerPlayerBodyResolver(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public Optional<ServerPlayer> resolve(PlayerActorId actorId) {
        return Optional.ofNullable(server.getPlayerList().getPlayer(actorId.value()));
    }

    public PlayerActorId actorId(ServerPlayer player) {
        return PlayerActorId.of(player.getUUID());
    }
}
