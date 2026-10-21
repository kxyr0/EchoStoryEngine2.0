package io.github.kxyr0.echoplayeractor.api.identity;

import java.util.Objects;
import java.util.UUID;

public record PlayerActorId(UUID value) implements Comparable<PlayerActorId> {
    public PlayerActorId {
        value = Objects.requireNonNull(value, "value");
    }

    public static PlayerActorId of(UUID uuid) {
        return new PlayerActorId(uuid);
    }

    public static PlayerActorId parse(String value) {
        return new PlayerActorId(UUID.fromString(PlayerActorIdSyntax.requireToken(value, "PlayerActorId")));
    }

    @Override
    public int compareTo(PlayerActorId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
