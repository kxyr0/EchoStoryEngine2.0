package io.github.kxyr0.echonpc.api.form.body;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Immutable global scale plus per-part transforms.
 */
public record NpcBodyGeometry(double globalScale, List<NpcBodyPartTransform> parts) {
    public NpcBodyGeometry {
        if (!Double.isFinite(globalScale) || globalScale < 0.05D || globalScale > 4.0D) {
            throw new IllegalArgumentException("globalScale is invalid.");
        }
        parts = List.copyOf(Objects.requireNonNull(parts, "parts"));
        EnumSet<NpcBodyPartId> seen = EnumSet.noneOf(NpcBodyPartId.class);
        for (NpcBodyPartTransform part : parts) {
            if (!seen.add(part.partId())) {
                throw new IllegalArgumentException("Duplicate body part transform: " + part.partId());
            }
        }
    }

    public NpcBodyPartTransform transform(NpcBodyPartId id) {
        return parts.stream().filter(part -> part.partId() == id).findFirst()
                .orElseGet(() -> NpcBodyPartTransform.uniform(id, 1.0D));
    }
}
