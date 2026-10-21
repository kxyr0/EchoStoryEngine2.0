package io.github.kxyr0.echotime.forge.daylight;

import java.util.List;
import java.util.Objects;

public record DaylightSyncResult(long expectedDayTime, List<String> externallyMutatedDimensions,
                                 boolean gameruleConflict) {
    public DaylightSyncResult {
        externallyMutatedDimensions = List.copyOf(Objects.requireNonNull(
                externallyMutatedDimensions, "externallyMutatedDimensions"));
    }

    public boolean conflict() {
        return gameruleConflict || !externallyMutatedDimensions.isEmpty();
    }
}
