package io.github.kxyr0.echowardrobe.core.profile;

import io.github.kxyr0.echowardrobe.api.profile.CoveragePolicy;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileDefinition;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import io.github.kxyr0.echowardrobe.api.revision.WardrobeCatalogRevision;
import io.github.kxyr0.echowardrobe.api.revision.WearableSourceFingerprint;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeSlot;
import java.util.LinkedHashSet;
import java.util.Set;

final class WardrobeProfileDraft {
    String id;
    String baseSkin;
    Set<WardrobeSlot> slots = new LinkedHashSet<>();
    CoveragePolicy coveragePolicy = CoveragePolicy.REPORT;

    WardrobeProfileDefinition toDefinition(WearableSourceFingerprint fingerprint) {
        return new WardrobeProfileDefinition(WardrobeProfileId.parse(id), baseSkin, slots, coveragePolicy,
                new WardrobeCatalogRevision(0L), fingerprint);
    }
}
