package io.github.kxyr0.echoplayeractor.api.identity;

import java.util.UUID;

public record PlayerActorOwnerId(String value) implements Comparable<PlayerActorOwnerId> {
    public PlayerActorOwnerId {
        value = PlayerActorIdSyntax.requireToken(value, "PlayerActorOwnerId");
    }

    public static PlayerActorOwnerId cutscene(String playbackId) {
        return new PlayerActorOwnerId("cutscene:" + PlayerActorIdSyntax.requireToken(playbackId, "playbackId"));
    }

    public static PlayerActorOwnerId dialogue(String sessionId) {
        return new PlayerActorOwnerId("dialogue:" + PlayerActorIdSyntax.requireToken(sessionId, "sessionId"));
    }

    public static PlayerActorOwnerId player(UUID uuid) {
        return new PlayerActorOwnerId("player:" + uuid);
    }

    public static PlayerActorOwnerId system(String name) {
        return new PlayerActorOwnerId("system:" + PlayerActorIdSyntax.requireToken(name, "name"));
    }

    @Override
    public int compareTo(PlayerActorOwnerId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
