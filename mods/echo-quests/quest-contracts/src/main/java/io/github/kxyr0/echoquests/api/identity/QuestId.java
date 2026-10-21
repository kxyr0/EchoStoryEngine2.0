package io.github.kxyr0.echoquests.api.identity;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record QuestId(String value) implements Comparable<QuestId> {
    private static final Pattern VALUE = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public QuestId {
        value = Objects.requireNonNull(value, "value").toLowerCase(Locale.ROOT);
        if (!VALUE.matcher(value).matches()) throw new IllegalArgumentException("Invalid quest ID: " + value);
    }

    public static QuestId parse(String value) { return new QuestId(value); }
    @Override public int compareTo(QuestId other) { return value.compareTo(other.value); }
    @Override public String toString() { return value; }
}
