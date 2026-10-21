package io.github.kxyr0.echocontentdoctor.model;

import java.util.Objects;

public record ContentResourceId(String value) implements Comparable<ContentResourceId> {
    public ContentResourceId {
        value = Objects.requireNonNull(value, "value").trim();
        if (!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Invalid content resource id: " + value);
        }
    }

    public String namespace() {
        return value.substring(0, value.indexOf(':'));
    }

    @Override
    public int compareTo(ContentResourceId other) {
        return value.compareTo(other.value);
    }
}
