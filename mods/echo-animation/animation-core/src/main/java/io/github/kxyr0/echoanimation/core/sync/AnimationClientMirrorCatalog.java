package io.github.kxyr0.echoanimation.core.sync;

import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.api.sync.AnimationSubjectSyncSnapshot;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncApplyResult;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncRevision;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Client-thread collection of per-subject mirrors.
 */
public final class AnimationClientMirrorCatalog {
    private final Map<AnimationSubjectId, AnimationClientSubjectMirror> mirrors = new HashMap<>();

    public AnimationSyncApplyResult apply(AnimationSubjectSyncSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return mirrors.computeIfAbsent(snapshot.subjectId(), ignored -> new AnimationClientSubjectMirror())
                .apply(snapshot);
    }

    public AnimationSyncApplyResult applyFull(AnimationSubjectSyncSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return mirrors.computeIfAbsent(snapshot.subjectId(), ignored -> new AnimationClientSubjectMirror())
                .applyFull(snapshot);
    }

    public AnimationSyncApplyResult remove(AnimationSubjectId subjectId, AnimationSyncRevision revision) {
        Objects.requireNonNull(subjectId, "subjectId");
        return mirrors.computeIfAbsent(subjectId, ignored -> new AnimationClientSubjectMirror()).remove(revision);
    }

    public Optional<AnimationClientSubjectMirror> mirror(AnimationSubjectId subjectId) {
        return Optional.ofNullable(mirrors.get(subjectId));
    }
}
