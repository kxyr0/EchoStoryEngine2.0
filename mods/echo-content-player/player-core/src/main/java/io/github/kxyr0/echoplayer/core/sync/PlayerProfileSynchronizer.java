package io.github.kxyr0.echoplayer.core.sync;

import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import java.util.Objects;

/**
 * Builds immutable synchronization decisions from profile revisions.
 */
public final class PlayerProfileSynchronizer {
    public boolean shouldApply(PlayerProfileSnapshot local, PlayerProfileSnapshot incoming) {
        Objects.requireNonNull(local, "local");
        Objects.requireNonNull(incoming, "incoming");
        return incoming.profileRevision().compareTo(local.profileRevision()) > 0
                || incoming.repositoryRevision() > local.repositoryRevision();
    }
}
