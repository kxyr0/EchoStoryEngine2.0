package io.github.kxyr0.echowardrobe.core.coverage;

import io.github.kxyr0.echowardrobe.api.outfit.OutfitKey;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import java.util.List;
import java.util.Objects;

/**
 * Authoring report for exact outfit coverage.
 */
public record CoverageReport(
        WardrobeProfileId profileId,
        int totalExpected,
        int mapped,
        List<OutfitKey> missing,
        int duplicateKeys,
        int unusedVariants
) {
    public CoverageReport {
        profileId = Objects.requireNonNull(profileId, "profileId");
        missing = List.copyOf(Objects.requireNonNull(missing, "missing"));
    }
}
