package io.github.kxyr0.echoteleportmagic.api;

import java.util.Objects;
import java.util.UUID;

public record TeleportExecutionId(UUID value) {
    public TeleportExecutionId { Objects.requireNonNull(value); }
    public static TeleportExecutionId random() { return new TeleportExecutionId(UUID.randomUUID()); }
    public static TeleportExecutionId owned(String owner, String step) {
        return new TeleportExecutionId(UUID.nameUUIDFromBytes((owner + "\0" + step)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
