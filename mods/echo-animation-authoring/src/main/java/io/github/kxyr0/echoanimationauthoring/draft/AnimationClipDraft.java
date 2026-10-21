package io.github.kxyr0.echoanimationauthoring.draft;

import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringDraftId;
import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringDraftRevision;
import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringDraftView;
import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringKeyframeMutation;
import io.github.kxyr0.echoanimationauthoring.draft.state.AnimationClipDraftSnapshot;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

record AnimationClipDraft(
        AuthoringDraftId id,
        AuthoringDraftRevision revision,
        Path sourcePath,
        String clipId,
        String rigId,
        List<AuthoringKeyframeMutation> keyframes,
        boolean dirty,
        long sourceRevision
) {
    AnimationClipDraft apply(AuthoringKeyframeMutation mutation) {
        List<AuthoringKeyframeMutation> next = new ArrayList<>(keyframes);
        next.removeIf(frame -> sameKey(frame, mutation));
        next.add(mutation);
        next.sort(Comparator.comparing(AuthoringKeyframeMutation::boneId)
                .thenComparingInt(AuthoringKeyframeMutation::frame));
        return new AnimationClipDraft(id, revision.next(), sourcePath, clipId, rigId, List.copyOf(next),
                true, sourceRevision);
    }

    AnimationClipDraft saved(long nextSourceRevision) {
        return new AnimationClipDraft(id, revision, sourcePath, clipId, rigId, keyframes, false, nextSourceRevision);
    }

    AnimationClipDraft replaceKeyframes(List<AuthoringKeyframeMutation> nextKeyframes) {
        return new AnimationClipDraft(id, revision.next(), sourcePath, clipId, rigId, List.copyOf(nextKeyframes),
                true, sourceRevision);
    }

    AnimationClipDraft restore(AnimationClipDraftSnapshot snapshot) {
        return new AnimationClipDraft(id, snapshot.revision(), sourcePath, clipId, rigId, snapshot.keyframes(),
                snapshot.dirty(), snapshot.sourceRevision());
    }

    AnimationClipDraftSnapshot snapshot() {
        return new AnimationClipDraftSnapshot(revision, keyframes, dirty, sourceRevision);
    }

    AuthoringDraftView view() {
        return new AuthoringDraftView(id, revision, sourcePath, clipId, rigId, keyframes.size(), dirty);
    }

    private static boolean sameKey(AuthoringKeyframeMutation first, AuthoringKeyframeMutation second) {
        return first.boneId().equals(second.boneId()) && first.frame() == second.frame();
    }
}
