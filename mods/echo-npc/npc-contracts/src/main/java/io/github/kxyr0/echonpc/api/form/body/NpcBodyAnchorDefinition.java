package io.github.kxyr0.echonpc.api.form.body;

import java.util.Objects;

public record NpcBodyAnchorDefinition(NpcBodyAnchorId id, double x, double y, double z) {
    public NpcBodyAnchorDefinition {
        id = Objects.requireNonNull(id, "id");
        bounded(x, "x");
        bounded(y, "y");
        bounded(z, "z");
    }

    public NpcBodyAnchorSnapshot snapshot() {
        return new NpcBodyAnchorSnapshot(id, x, y, z);
    }

    private static void bounded(double value, String name) {
        if (!Double.isFinite(value) || Math.abs(value) > 16.0D) {
            throw new IllegalArgumentException("Anchor " + name + " is invalid.");
        }
    }
}
