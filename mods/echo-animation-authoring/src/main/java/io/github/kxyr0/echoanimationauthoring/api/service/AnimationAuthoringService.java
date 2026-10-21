package io.github.kxyr0.echoanimationauthoring.api.service;

import io.github.kxyr0.echoanimationauthoring.api.discovery.AuthoringDiscoveryResult;
import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringDraftId;
import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringDraftView;
import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringKeyframeMutation;
import java.util.Optional;

public interface AnimationAuthoringService {
    AuthoringDraftOpenResult openDraft(AuthoringDraftOpenRequest request);

    AuthoringDraftMutationResult applyKeyframe(AuthoringDraftId draftId, AuthoringKeyframeMutation mutation);

    AuthoringDraftSaveResult save(AuthoringDraftId draftId);

    AuthoringHotReloadResult hotReload(AuthoringDraftId draftId);

    AuthoringOperationResult undo(AuthoringDraftId draftId);

    AuthoringOperationResult redo(AuthoringDraftId draftId);

    AuthoringOperationResult autosave(AuthoringDraftId draftId);

    AuthoringOperationResult recoverDraft(AuthoringDraftOpenRequest request);

    AuthoringOperationResult applyPreview(AuthoringDraftId draftId);

    AuthoringOperationResult clearPreview(AuthoringDraftId draftId);

    AuthoringDiscoveryResult discover(AuthoringDraftOpenRequest request);

    Optional<AuthoringDraftView> draft(AuthoringDraftId draftId);
}
