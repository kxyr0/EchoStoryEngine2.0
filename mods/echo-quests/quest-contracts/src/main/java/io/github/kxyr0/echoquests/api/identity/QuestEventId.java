package io.github.kxyr0.echoquests.api.identity;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record QuestEventId(String value) implements Comparable<QuestEventId> {
    private static final Pattern VALUE = Pattern.compile("[a-z0-9_.:/-]+");

    public QuestEventId {
        value = Objects.requireNonNull(value, "value").toLowerCase(Locale.ROOT);
        if (!VALUE.matcher(value).matches()) throw new IllegalArgumentException("Invalid quest event ID: " + value);
    }

    public static QuestEventId parse(String value) { return new QuestEventId(value); }
    @Override public int compareTo(QuestEventId other) { return value.compareTo(other.value); }
    @Override public String toString() { return value; }
}
