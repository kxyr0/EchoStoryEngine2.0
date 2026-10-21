package io.github.kxyr0.echoitem.core.source;

import io.github.kxyr0.echoitem.api.source.ItemSourceFingerprint;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * UTF-8 decoded `.weapon.es` candidate from packaged or external content.
 */
public record ItemSourceCandidate(String sourceId, String sourceText, ItemSourceOrigin origin) {
    public ItemSourceCandidate {
        sourceId = require(sourceId, "sourceId");
        sourceText = Objects.requireNonNull(sourceText, "sourceText");
        origin = Objects.requireNonNull(origin, "origin");
    }

    public ItemSourceFingerprint fingerprint() {
        return ItemSourceFingerprint.ofBytes(sourceText.getBytes(StandardCharsets.UTF_8));
    }

    private static String require(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return value;
    }
}
