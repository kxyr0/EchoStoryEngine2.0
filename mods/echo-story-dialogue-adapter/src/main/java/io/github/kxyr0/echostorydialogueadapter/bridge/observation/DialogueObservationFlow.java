package io.github.kxyr0.echostorydialogueadapter.bridge.observation;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.observation.DialogueObservationHandle;
import io.github.kxyr0.echodialogue.api.service.DialogueOperationStatus;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Owns bounded terminal observations, including synchronous terminal callbacks. */
public final class DialogueObservationFlow {
    private final Map<ActionExecutionId, DialogueObservationHandle> handles = new HashMap<>();
    public boolean capacity(ActionExecutionId id) { return handles.containsKey(id) || handles.size() < 1024; }
    public void close(ActionExecutionId id) {
        Optional.ofNullable(handles.remove(id)).ifPresent(DialogueObservationHandle::close);
    }
    public void clear() {
        handles.values().forEach(DialogueObservationHandle::close);
        handles.clear();
    }
    public ActionInvocationResult observe(DialogueService service, DialogueSessionView session,
            ActionExecutionId id, Consumer<DialogueSessionView> completed) {
        if (handles.containsKey(id)) { return ActionInvocationResult.accepted(); }
        if (!capacity(id)) { return ActionInvocationResult.rejected("Adapter terminal observation limit reached."); }
        register(service, session.sessionId(), id, completed);
        return ActionInvocationResult.accepted();
    }
    public ActionInvocationResult replace(DialogueService service, DialogueSessionView conflicting,
            ActionExecutionId id, Supplier<ActionInvocationResult> restart, Consumer<String> failed) {
        if (handles.containsKey(id)) { return ActionInvocationResult.accepted(); }
        if (!capacity(id)) { return ActionInvocationResult.rejected("Adapter terminal observation limit reached."); }
        register(service, conflicting.sessionId(), id, ignored -> restart.get());
        var status = service.cancel(conflicting.sessionId()).status();
        if (status == DialogueOperationStatus.SERVICE_UNAVAILABLE) {
            close(id);
            return ActionInvocationResult.rejected("Echo Dialogue service недоступен для replacement.");
        }
        if (status == DialogueOperationStatus.NOT_FOUND || status == DialogueOperationStatus.ALREADY_TERMINAL) {
            close(id);
            return restart.get();
        }
        if (status != DialogueOperationStatus.ACCEPTED) {
            close(id);
            failed.accept("dialogue-replacement-cancel-" + status.name());
        }
        return ActionInvocationResult.accepted();
    }
    private void register(DialogueService service, DialogueSessionId session, ActionExecutionId id,
            Consumer<DialogueSessionView> completed) {
        boolean[] notified = {false};
        var handle = service.observeTerminal(session, event -> {
            notified[0] = true;
            close(id);
            completed.accept(event.session());
        });
        if (notified[0]) { handle.close(); } else { handles.put(id, handle); }
    }
}
