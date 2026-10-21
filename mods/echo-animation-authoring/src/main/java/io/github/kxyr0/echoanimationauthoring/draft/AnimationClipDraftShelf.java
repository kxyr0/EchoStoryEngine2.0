package io.github.kxyr0.echoanimationauthoring.draft;

import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringDraftId;
import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringDraftRevision;
import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringKeyframeMutation;
import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringTransform;
import io.github.kxyr0.echoanimationauthoring.draft.state.AnimationClipDraftHistory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

final class AnimationClipDraftShelf {
    private final java.util.Map<AuthoringDraftId, AnimationClipDraft> drafts = new LinkedHashMap<>();
    private final java.util.Map<AuthoringDraftId, AnimationClipDraftHistory> history = new LinkedHashMap<>();

    AnimationClipDraft open(Path sourcePath, String clipId, String rigId) {
        AnimationClipDraft draft = new AnimationClipDraft(AuthoringDraftId.random(),
                new AuthoringDraftRevision(0L), sourcePath, clipId, rigId, List.of(identityFrame()),
                false, sourceRevision(sourcePath));
        drafts.put(draft.id(), draft);
        history.put(draft.id(), new AnimationClipDraftHistory());
        return draft;
    }

    Optional<AnimationClipDraft> get(AuthoringDraftId id) {
        return Optional.ofNullable(drafts.get(id));
    }

    AnimationClipDraft replace(AnimationClipDraft draft) {
        drafts.put(draft.id(), draft);
        return draft;
    }

    Optional<AnimationClipDraft> edit(AuthoringDraftId id, Function<AnimationClipDraft, AnimationClipDraft> edit) {
        return get(id).map(draft -> {
            history.computeIfAbsent(id, ignored -> new AnimationClipDraftHistory()).pushEdit(draft.snapshot());
            AnimationClipDraft changed = edit.apply(draft);
            drafts.put(id, changed);
            return changed;
        });
    }

    Optional<AnimationClipDraft> undo(AuthoringDraftId id) {
        return get(id).flatMap(draft -> history.computeIfAbsent(id, ignored -> new AnimationClipDraftHistory())
                .undo(draft.snapshot()).map(snapshot -> replace(draft.restore(snapshot))));
    }

    Optional<AnimationClipDraft> redo(AuthoringDraftId id) {
        return get(id).flatMap(draft -> history.computeIfAbsent(id, ignored -> new AnimationClipDraftHistory())
                .redo(draft.snapshot()).map(snapshot -> replace(draft.restore(snapshot))));
    }

    private static AuthoringKeyframeMutation identityFrame() {
        return new AuthoringKeyframeMutation("body", 0, AuthoringTransform.identity(), "LINEAR");
    }

    private static long sourceRevision(Path sourcePath) {
        try {
            return Files.exists(sourcePath) ? Files.getLastModifiedTime(sourcePath).toMillis() : -1L;
        } catch (java.io.IOException exception) {
            return -1L;
        }
    }
}
