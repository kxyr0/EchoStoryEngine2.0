package io.github.kxyr0.echobackgroundnpc.api.identity.agent;

import java.util.Objects;
import java.util.UUID;

/** Runtime materialization identity; it never replaces the authored NPC identity. */
public record BackgroundNpcInstanceId(UUID value) {
    public BackgroundNpcInstanceId { value = Objects.requireNonNull(value, "value"); }
    public static BackgroundNpcInstanceId stable(BackgroundNpcId id) {
        return new BackgroundNpcInstanceId(UUID.nameUUIDFromBytes(id.value().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
