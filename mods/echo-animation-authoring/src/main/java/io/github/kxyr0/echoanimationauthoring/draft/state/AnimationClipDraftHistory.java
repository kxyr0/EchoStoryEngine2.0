package io.github.kxyr0.echoanimationauthoring.draft.state;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public final class AnimationClipDraftHistory {
    private final Deque<AnimationClipDraftSnapshot> undo = new ArrayDeque<>();
    private final Deque<AnimationClipDraftSnapshot> redo = new ArrayDeque<>();

    public void pushEdit(AnimationClipDraftSnapshot snapshot) {
        undo.push(snapshot);
        redo.clear();
    }

    public Optional<AnimationClipDraftSnapshot> undo(AnimationClipDraftSnapshot current) {
        if (undo.isEmpty()) {
            return Optional.empty();
        }
        redo.push(current);
        return Optional.of(undo.pop());
    }

    public Optional<AnimationClipDraftSnapshot> redo(AnimationClipDraftSnapshot current) {
        if (redo.isEmpty()) {
            return Optional.empty();
        }
        undo.push(current);
        return Optional.of(redo.pop());
    }
}
