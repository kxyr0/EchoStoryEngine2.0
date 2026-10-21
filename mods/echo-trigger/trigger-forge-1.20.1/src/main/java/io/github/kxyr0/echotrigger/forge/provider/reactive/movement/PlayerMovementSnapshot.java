package io.github.kxyr0.echotrigger.forge.provider.reactive.movement;

import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import io.github.kxyr0.echotrigger.forge.provider.payload.ForgeIds;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

record PlayerMovementSnapshot(String dimension, Vector3 position) {
    PlayerMovementSnapshot {
        dimension = Objects.requireNonNull(dimension, "dimension");
        position = Objects.requireNonNull(position, "position");
    }

    static PlayerMovementSnapshot of(ServerPlayer player) {
        return new PlayerMovementSnapshot(ForgeIds.dimension(player.level()), ForgeIds.position(player));
    }
}
