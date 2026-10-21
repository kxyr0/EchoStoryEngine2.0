package io.github.kxyr0.echoseating.api.world;

import java.util.Objects;
import java.util.UUID;

public record PlayerRef(UUID uuid) {
    public PlayerRef {
        uuid = Objects.requireNonNull(uuid, "uuid");
    }
}
