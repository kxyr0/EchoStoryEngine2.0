package io.github.kxyr0.echoanimation.core.sync;

import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.graph.definition.ChannelCompositionPolicy;
import io.github.kxyr0.echoanimation.api.graph.definition.PlaybackMode;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.api.sync.AnimationChannelSyncState;
import io.github.kxyr0.echoanimation.api.sync.AnimationSubjectSyncSnapshot;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncApplyResult;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncRevision;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class AnimationClientMirrorTest {
    private static final AnimationSubjectId SUBJECT = new AnimationSubjectId("jdh:first");

    @Test
    void fullSnapshotLateJoinAndReconnectAreApplied() {
        AnimationClientSubjectMirror mirror = new AnimationClientSubjectMirror();
        Assertions.assertEquals(AnimationSyncApplyResult.APPLIED, mirror.applyFull(snapshot(4L)));
        Assertions.assertEquals(4L, mirror.current().orElseThrow().syncRevision().value());
        Assertions.assertEquals(AnimationSyncApplyResult.APPLIED, mirror.applyFull(snapshot(9L)));
        Assertions.assertEquals(9L, mirror.current().orElseThrow().syncRevision().value());
    }

    @Test
    void duplicateStaleAndRevisionGapAreRejected() {
        AnimationClientSubjectMirror mirror = new AnimationClientSubjectMirror();
        mirror.applyFull(snapshot(4L));
        Assertions.assertEquals(AnimationSyncApplyResult.DUPLICATE, mirror.apply(snapshot(4L)));
        Assertions.assertEquals(AnimationSyncApplyResult.STALE, mirror.apply(snapshot(3L)));
        Assertions.assertEquals(AnimationSyncApplyResult.REVISION_GAP, mirror.apply(snapshot(6L)));
        Assertions.assertEquals(4L, mirror.current().orElseThrow().syncRevision().value());
    }

    @Test
    void subjectRemovalClearsMirror() {
        AnimationClientSubjectMirror mirror = new AnimationClientSubjectMirror();
        mirror.applyFull(snapshot(1L));
        Assertions.assertEquals(AnimationSyncApplyResult.SUBJECT_REMOVED,
                mirror.remove(new AnimationSyncRevision(2L)));
        Assertions.assertTrue(mirror.current().isEmpty());
    }

    private AnimationSubjectSyncSnapshot snapshot(long revision) {
        return new AnimationSubjectSyncSnapshot(SUBJECT, 1L, new AnimationSyncRevision(revision),
                List.of(new AnimationChannelSyncState(new AnimationChannelId("body"),
                        new AnimationStateId("body/idle"), Optional.empty(), Optional.empty(), Optional.empty(),
                        0L, 0L, 20L,
                        0.0D, 0L, false, false, PlaybackMode.LOOP, 1.0D,
                        0, ChannelCompositionPolicy.OVERRIDE, java.util.Set.of(), 1.0D,
                        Optional.empty(), 0L, 0L, 1.0D)));
    }
}
