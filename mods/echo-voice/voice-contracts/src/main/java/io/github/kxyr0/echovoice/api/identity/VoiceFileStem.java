package io.github.kxyr0.echovoice.api.identity;

import java.util.Locale;
import java.util.Objects;

/** Portable basename-only identity for one authored voice file. */
public record VoiceFileStem(String value) {
    private static final String PATTERN = "[a-z0-9_-]+";

    public VoiceFileStem {
        value = Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
        if (!value.matches(PATTERN) || value.contains("..")) {
            throw new IllegalArgumentException("Voice filename stem must match " + PATTERN + ".");
        }
    }

    public static VoiceFileStem parse(String raw) {
        return new VoiceFileStem(raw);
    }

    @Override
    public String toString() {
        return value;
    }
}
