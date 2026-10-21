package io.github.kxyr0.echoplayeractor.api.identity;

import java.util.UUID;

public record PlayerControlLeaseId(String value) implements Comparable<PlayerControlLeaseId> {
    public PlayerControlLeaseId {
        value = PlayerActorIdSyntax.requireToken(value, "PlayerControlLeaseId");
    }

    public static PlayerControlLeaseId create() {
        return new PlayerControlLeaseId("player-lease-" + UUID.randomUUID());
    }

    @Override
    public int compareTo(PlayerControlLeaseId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
