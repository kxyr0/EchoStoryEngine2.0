package io.github.kxyr0.echowardrobe.api.profile;

import io.github.kxyr0.echowardrobe.api.identity.WardrobeResourceId;
import io.github.kxyr0.echowardrobe.api.revision.WardrobeCatalogRevision;
import io.github.kxyr0.echowardrobe.api.revision.WearableSourceFingerprint;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeSlot;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable wardrobe profile root.
 */
public record WardrobeProfileDefinition(
        WardrobeProfileId id,
        String baseSkin,
        Set<WardrobeSlot> slots,
        CoveragePolicy coveragePolicy,
        WardrobeCatalogRevision revision,
        WearableSourceFingerprint sourceFingerprint
) {
    public WardrobeProfileDefinition {
        id = Objects.requireNonNull(id, "id");
        baseSkin = WardrobeResourceId.require(baseSkin, "Base skin");
        slots = slots.isEmpty() ? Set.copyOf(EnumSet.allOf(WardrobeSlot.class)) : Set.copyOf(slots);
        coveragePolicy = Objects.requireNonNull(coveragePolicy, "coveragePolicy");
        revision = Objects.requireNonNull(revision, "revision");
        sourceFingerprint = Objects.requireNonNull(sourceFingerprint, "sourceFingerprint");
    }
}
