package io.github.kxyr0.echomagic.api.player;

import java.util.Objects;
import java.util.UUID;

public record MagicPlayerRef(UUID uuid) implements Comparable<MagicPlayerRef> {
    public MagicPlayerRef { uuid = Objects.requireNonNull(uuid, "uuid"); }
    @Override public int compareTo(MagicPlayerRef other) { return uuid.compareTo(other.uuid); }
    @Override public String toString() { return uuid.toString(); }
}

