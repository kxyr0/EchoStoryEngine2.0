package io.github.kxyr0.echoplayeractor.api.identity;

import java.util.UUID;

public record PlayerActorCommandId(String value) implements Comparable<PlayerActorCommandId> {
    public PlayerActorCommandId {
        value = PlayerActorIdSyntax.requireToken(value, "PlayerActorCommandId");
    }

    public static PlayerActorCommandId create() {
        return new PlayerActorCommandId("player-command-" + UUID.randomUUID());
    }

    @Override
    public int compareTo(PlayerActorCommandId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
