package io.github.kxyr0.echoplayeractor.api.pose;

import java.util.Objects;
import java.util.Optional;

public record PlayerActorSeatSnapshot(
        boolean seated,
        Optional<String> providerId,
        Optional<String> seatId,
        Optional<String> vehicleType) {
    public PlayerActorSeatSnapshot {
        providerId = Objects.requireNonNull(providerId, "providerId");
        seatId = Objects.requireNonNull(seatId, "seatId");
        vehicleType = Objects.requireNonNull(vehicleType, "vehicleType");
    }

    public static PlayerActorSeatSnapshot standing() {
        return new PlayerActorSeatSnapshot(false, Optional.empty(), Optional.empty(), Optional.empty());
    }
}
