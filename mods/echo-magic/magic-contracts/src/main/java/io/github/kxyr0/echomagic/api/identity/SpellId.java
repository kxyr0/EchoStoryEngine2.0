package io.github.kxyr0.echomagic.api.identity;

import java.util.Objects;
import java.util.regex.Pattern;

public record SpellId(String value) implements Comparable<SpellId> {
    private static final Pattern FORMAT = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public SpellId {
        value = Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) throw new IllegalArgumentException("Invalid SpellId: " + value);
    }

    public static SpellId of(String value) { return new SpellId(value); }
    @Override public int compareTo(SpellId other) { return value.compareTo(other.value); }
    @Override public String toString() { return value; }
}

