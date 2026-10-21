package io.github.kxyr0.echoplayeractor.api.identity;

import java.util.UUID;

public record PlayerActorExecutionId(String value) implements Comparable<PlayerActorExecutionId> {
    public PlayerActorExecutionId {
        value = PlayerActorIdSyntax.requireToken(value, "PlayerActorExecutionId");
    }

    public static PlayerActorExecutionId create() {
        return new PlayerActorExecutionId("player-execution-" + UUID.randomUUID());
    }

    @Override
    public int compareTo(PlayerActorExecutionId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
