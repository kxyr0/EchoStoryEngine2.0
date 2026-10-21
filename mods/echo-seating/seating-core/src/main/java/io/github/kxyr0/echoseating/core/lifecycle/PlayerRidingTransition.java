package io.github.kxyr0.echoseating.core.lifecycle;

import io.github.kxyr0.echoseating.api.anchor.SeatAnchor;
import io.github.kxyr0.echoseating.api.world.PlayerRef;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record PlayerRidingTransition(
        PlayerRef player,
        Optional<UUID> previousVehicle,
        Optional<SeatAnchor> newAnchor,
        long ridingGeneration,
        long serverTick) {
    public PlayerRidingTransition {
        player = Objects.requireNonNull(player, "player");
        previousVehicle = Objects.requireNonNull(previousVehicle, "previousVehicle");
        newAnchor = Objects.requireNonNull(newAnchor, "newAnchor");
        if (ridingGeneration < 0L || serverTick < 0L) {
            throw new IllegalArgumentException("transition ticks must not be negative.");
        }
    }
}
