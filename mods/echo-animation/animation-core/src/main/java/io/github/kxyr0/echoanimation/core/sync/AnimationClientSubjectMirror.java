package io.github.kxyr0.echoanimation.core.sync;

import io.github.kxyr0.echoanimation.api.sync.AnimationSubjectSyncSnapshot;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncApplyResult;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncRevision;
import java.util.Objects;
import java.util.Optional;

/**
 * Client-owned mirror of one server-authoritative animation subject.
 */
public final class AnimationClientSubjectMirror {
    private AnimationSubjectSyncSnapshot current;
    private boolean removed;

    public AnimationSyncApplyResult apply(AnimationSubjectSyncSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (removed) {
            current = null;
            removed = false;
        }
        if (current == null) {
            current = snapshot;
            return AnimationSyncApplyResult.APPLIED;
        }
        long next = snapshot.syncRevision().value();
        long previous = current.syncRevision().value();
        if (next == previous) {
            return AnimationSyncApplyResult.DUPLICATE;
        }
        if (next < previous) {
            return AnimationSyncApplyResult.STALE;
        }
        if (next > previous + 1L) {
            return AnimationSyncApplyResult.REVISION_GAP;
        }
        current = snapshot;
        return AnimationSyncApplyResult.APPLIED;
    }

    public AnimationSyncApplyResult applyFull(AnimationSubjectSyncSnapshot snapshot) {
        current = Objects.requireNonNull(snapshot, "snapshot");
        removed = false;
        return AnimationSyncApplyResult.APPLIED;
    }

    public AnimationSyncApplyResult remove(AnimationSyncRevision revision) {
        Objects.requireNonNull(revision, "revision");
        if (current != null && revision.value() < current.syncRevision().value()) {
            return AnimationSyncApplyResult.STALE;
        }
        current = null;
        removed = true;
        return AnimationSyncApplyResult.SUBJECT_REMOVED;
    }

    public Optional<AnimationSubjectSyncSnapshot> current() {
        return Optional.ofNullable(current);
    }

    public boolean removed() {
        return removed;
    }
}
