package io.github.kxyr0.echonpc.api.form.body;

import java.util.Locale;
import java.util.Objects;

/**
 * Semantic camera/body anchor identity. Custom anchors use the same bounded token syntax.
 */
public record NpcBodyAnchorId(String value) implements Comparable<NpcBodyAnchorId> {
    public static final NpcBodyAnchorId FEET = new NpcBodyAnchorId("feet");
    public static final NpcBodyAnchorId CHEST = new NpcBodyAnchorId("chest");
    public static final NpcBodyAnchorId HEAD = new NpcBodyAnchorId("head");
    public static final NpcBodyAnchorId EYES = new NpcBodyAnchorId("eyes");

    public NpcBodyAnchorId {
        value = Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z0-9_.-]{1,64}")) {
            throw new IllegalArgumentException("NpcBodyAnchorId contains invalid characters.");
        }
    }

    @Override
    public int compareTo(NpcBodyAnchorId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
