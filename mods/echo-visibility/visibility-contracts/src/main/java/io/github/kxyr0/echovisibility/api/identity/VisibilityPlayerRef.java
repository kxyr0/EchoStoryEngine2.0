package io.github.kxyr0.echovisibility.api.identity;

import java.util.Objects;
import java.util.UUID;

public record VisibilityPlayerRef(UUID uuid) implements Comparable<VisibilityPlayerRef> {
    public VisibilityPlayerRef {
        uuid = Objects.requireNonNull(uuid, "uuid");
    }

    @Override
    public int compareTo(VisibilityPlayerRef other) {
        return uuid.compareTo(other.uuid);
    }
}
