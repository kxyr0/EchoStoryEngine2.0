package io.github.kxyr0.echoplayeractor.api.identity;

public record PlayerControlScopeId(String value) implements Comparable<PlayerControlScopeId> {
    public static final PlayerControlScopeId HUMAN_CONTROL = new PlayerControlScopeId("player_actor:human_control");
    public static final PlayerControlScopeId CINEMATIC_CONTROL =
            new PlayerControlScopeId("player_actor:cinematic_control");

    public PlayerControlScopeId {
        value = PlayerActorIdSyntax.requireToken(value, "PlayerControlScopeId");
        if (!value.contains(":")) {
            throw new IllegalArgumentException("PlayerControlScopeId must be namespaced.");
        }
    }

    @Override
    public int compareTo(PlayerControlScopeId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
