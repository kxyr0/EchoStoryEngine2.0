package io.github.kxyr0.echoitem.core.manifest;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.ItemKindId;
import io.github.kxyr0.echoitem.api.source.ItemSourceFingerprint;
import java.util.Objects;

/**
 * One durable machine-owned registry manifest entry.
 */
public record RegisteredItemRecord(
        EchoItemId id,
        ItemKindId kind,
        String firstSeenVersion,
        ItemSourceFingerprint lastSeenSourceFingerprint,
        boolean pendingRegistration,
        boolean deprecated
) {
    public RegisteredItemRecord {
        id = Objects.requireNonNull(id, "id");
        kind = Objects.requireNonNull(kind, "kind");
        firstSeenVersion = require(firstSeenVersion, "firstSeenVersion");
        lastSeenSourceFingerprint = Objects.requireNonNull(lastSeenSourceFingerprint, "lastSeenSourceFingerprint");
    }

    public RegisteredItemRecord seen(ItemSourceFingerprint fingerprint) {
        return new RegisteredItemRecord(id, kind, firstSeenVersion, fingerprint, false, deprecated);
    }

    public RegisteredItemRecord pending() {
        return new RegisteredItemRecord(id, kind, firstSeenVersion, lastSeenSourceFingerprint, true, deprecated);
    }

    private static String require(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return value;
    }
}
