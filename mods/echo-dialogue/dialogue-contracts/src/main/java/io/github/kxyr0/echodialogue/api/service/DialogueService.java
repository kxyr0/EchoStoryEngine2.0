package io.github.kxyr0.echodialogue.api.service;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.observation.DialogueObservationHandle;
import io.github.kxyr0.echodialogue.api.observation.DialogueTerminalObserver;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalResumeRequest;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWaitRequest;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWaitResult;

/**
 * Server-scoped public Echo Dialogue service for external integrations.
 */
public interface DialogueService {
    default io.github.kxyr0.echodialogue.api.failure.DialogueFailureResult fail(
            io.github.kxyr0.echodialogue.api.failure.DialogueFailureRequest request) {
        return new io.github.kxyr0.echodialogue.api.failure.DialogueFailureResult(
                DialogueOperationStatus.SERVICE_UNAVAILABLE, java.util.Optional.empty(), java.util.List.of());
    }
    /** True only after a durable start commit, including the initial repository revision zero. */
    default boolean isDurablyAccepted(DialogueSessionId sessionId) { return false; }

    DialogueStartResult start(DialogueStartRequest request);

    DialogueQueryResult query(DialogueSessionId sessionId);

    DialogueCancelResult cancel(DialogueSessionId sessionId);

    DialogueExternalWaitResult waitExternal(DialogueExternalWaitRequest request);

    DialogueExternalWaitResult resumeExternal(DialogueExternalResumeRequest request);

    DialogueObservationHandle observeTerminal(DialogueSessionId sessionId, DialogueTerminalObserver observer);
}
