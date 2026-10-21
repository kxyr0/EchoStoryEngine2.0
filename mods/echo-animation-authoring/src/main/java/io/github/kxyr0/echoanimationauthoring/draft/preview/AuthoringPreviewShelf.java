package io.github.kxyr0.echoanimationauthoring.draft.preview;

import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringDraftId;
import io.github.kxyr0.echoanimationauthoring.draft.state.AnimationClipDraftSnapshot;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class AuthoringPreviewShelf {
    private final Map<AuthoringDraftId, AnimationClipDraftSnapshot> previews = new LinkedHashMap<>();

    public void apply(AuthoringDraftId id, AnimationClipDraftSnapshot snapshot) {
        previews.put(id, snapshot);
    }

    public boolean clear(AuthoringDraftId id) {
        return previews.remove(id) != null;
    }

    public Optional<AnimationClipDraftSnapshot> preview(AuthoringDraftId id) {
        return Optional.ofNullable(previews.get(id));
    }
}
