package io.github.kxyr0.echoanimationauthoring.draft.state;

import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringDraftRevision;
import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringKeyframeMutation;
import java.util.List;
import java.util.Objects;

public record AnimationClipDraftSnapshot(
        AuthoringDraftRevision revision,
        List<AuthoringKeyframeMutation> keyframes,
        boolean dirty,
        long sourceRevision
) {
    public AnimationClipDraftSnapshot {
        revision = Objects.requireNonNull(revision, "revision");
        keyframes = List.copyOf(Objects.requireNonNull(keyframes, "keyframes"));
    }
}
