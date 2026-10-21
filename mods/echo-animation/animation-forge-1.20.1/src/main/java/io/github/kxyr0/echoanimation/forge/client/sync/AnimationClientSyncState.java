package io.github.kxyr0.echoanimation.forge.client.sync;

import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.api.sync.AnimationSubjectSyncSnapshot;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncApplyResult;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncRevision;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.core.sync.AnimationClientMirrorCatalog;
import io.github.kxyr0.echoanimation.core.sync.AnimationClientPoseResolver;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Client-thread store for server-authoritative animation sync snapshots.
 */
public final class AnimationClientSyncState {
    private static final AnimationClientMirrorCatalog MIRRORS = new AnimationClientMirrorCatalog();
    private static final AnimationClientPoseResolver POSE_RESOLVER = new AnimationClientPoseResolver();

    private AnimationClientSyncState() {
    }

    public static AnimationSyncApplyResult apply(AnimationSubjectSyncSnapshot snapshot) {
        return MIRRORS.apply(Objects.requireNonNull(snapshot, "snapshot"));
    }

    public static AnimationSyncApplyResult applyFull(AnimationSubjectSyncSnapshot snapshot) {
        return MIRRORS.applyFull(Objects.requireNonNull(snapshot, "snapshot"));
    }

    public static AnimationSyncApplyResult remove(AnimationSubjectId subjectId, AnimationSyncRevision revision) {
        return MIRRORS.remove(Objects.requireNonNull(subjectId, "subjectId"),
                Objects.requireNonNull(revision, "revision"));
    }

    public static Optional<AnimationSubjectSyncSnapshot> current(AnimationSubjectId subjectId) {
        return MIRRORS.mirror(Objects.requireNonNull(subjectId, "subjectId")).flatMap(mirror -> mirror.current());
    }

    public static Optional<AnimationPose> resolvePose(
            AnimationSubjectId subjectId,
            AnimationRigDefinition rig,
            List<AnimationClipDefinition> clips,
            long sampleTick
    ) {
        return resolvePose(subjectId, rig, clips, (double) sampleTick);
    }

    public static Optional<AnimationPose> resolvePose(
            AnimationSubjectId subjectId,
            AnimationRigDefinition rig,
            List<AnimationClipDefinition> clips,
            double sampleTick
    ) {
        return current(subjectId).map(snapshot -> POSE_RESOLVER.resolve(rig, snapshot, clips, sampleTick));
    }
}
