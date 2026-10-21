package io.github.kxyr0.echoquests.api.identity;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record QuestStateId(String value) implements Comparable<QuestStateId> {
    private static final Pattern VALUE = Pattern.compile("[a-z0-9_.-]+");

    public QuestStateId {
        value = Objects.requireNonNull(value, "value").toLowerCase(Locale.ROOT);
        if (!VALUE.matcher(value).matches()) throw new IllegalArgumentException("Invalid quest state ID: " + value);
    }

    public static QuestStateId parse(String value) { return new QuestStateId(value); }
    @Override public int compareTo(QuestStateId other) { return value.compareTo(other.value); }
    @Override public String toString() { return value; }
}
