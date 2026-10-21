package io.github.kxyr0.echobackgroundnpc.forge.api.location;

import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundLocationId;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record BackgroundLocationResolution(
        BackgroundLocationId id,
        Status status,
        Optional<ResourceKey<Level>> dimension,
        Optional<BlockPos> position
) {
    public BackgroundLocationResolution {
        id = Objects.requireNonNull(id, "id");
        status = Objects.requireNonNull(status, "status");
        dimension = Objects.requireNonNull(dimension, "dimension");
        position = Objects.requireNonNull(position, "position");
    }
    public static BackgroundLocationResolution found(BackgroundLocationId id, ResourceKey<Level> dimension,
            BlockPos position) {
        return new BackgroundLocationResolution(id, Status.FOUND, Optional.of(dimension), Optional.of(position));
    }
    public static BackgroundLocationResolution missing(BackgroundLocationId id) {
        return new BackgroundLocationResolution(id, Status.NOT_FOUND, Optional.empty(), Optional.empty());
    }
    public enum Status { FOUND, NOT_FOUND, AMBIGUOUS, UNAVAILABLE }
}
