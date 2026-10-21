package io.github.kxyr0.echonpc.api.form.body;

import java.util.Objects;

public record NpcBodyAnchorSnapshot(NpcBodyAnchorId id, double x, double y, double z) {
    public NpcBodyAnchorSnapshot {
        id = Objects.requireNonNull(id, "id");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Anchor snapshot coordinates must be finite.");
        }
    }
}
