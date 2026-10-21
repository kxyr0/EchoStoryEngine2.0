package io.github.kxyr0.echoanimationeditor.core.state;

import io.github.kxyr0.echoanimationeditor.api.session.AnimatorDraftId;
import io.github.kxyr0.echoanimationeditor.api.session.AnimatorDraftRevision;

public record AnimatorDraftState(AnimatorDraftId draftId, AnimatorDraftRevision revision, boolean dirty) {
    public static AnimatorDraftState create() {
        return new AnimatorDraftState(AnimatorDraftId.random(), new AnimatorDraftRevision(0L), false);
    }

    public AnimatorDraftState touch() {
        return new AnimatorDraftState(draftId, revision.next(), true);
    }

    public AnimatorDraftState saved() {
        return new AnimatorDraftState(draftId, revision, false);
    }
}
