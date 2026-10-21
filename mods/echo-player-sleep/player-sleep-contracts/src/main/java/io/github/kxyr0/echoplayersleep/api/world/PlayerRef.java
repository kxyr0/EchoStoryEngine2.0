package io.github.kxyr0.echoplayersleep.api.world;

import java.util.Objects;
import java.util.UUID;

public record PlayerRef(UUID uuid) {
    public PlayerRef {
        uuid = Objects.requireNonNull(uuid, "uuid");
    }
}
