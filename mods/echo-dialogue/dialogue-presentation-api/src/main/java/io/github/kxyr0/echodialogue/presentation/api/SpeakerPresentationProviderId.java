package io.github.kxyr0.echodialogue.presentation.api;

import io.github.kxyr0.echodialogue.api.identity.NamespacedId;
import java.util.Objects;

/**
 * Stable namespaced identity of one speaker presentation provider.
 */
public record SpeakerPresentationProviderId(NamespacedId value) implements Comparable<SpeakerPresentationProviderId> {
    private static final int MAX_LENGTH = 128;

    public SpeakerPresentationProviderId {
        value = Objects.requireNonNull(value, "value");
        if (value.value().length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Provider ID слишком длинный.");
        }
    }

    public static SpeakerPresentationProviderId parse(String raw) {
        return new SpeakerPresentationProviderId(new NamespacedId(raw));
    }

    @Override
    public int compareTo(SpeakerPresentationProviderId other) {
        return toString().compareTo(other.toString());
    }

    @Override
    public String toString() {
        return value.value();
    }
}
