package io.github.kxyr0.echostorydialogueadapter.bridge;

import io.github.kxyr0.echodialogue.api.service.DialogueCancelResult;
import io.github.kxyr0.echodialogue.api.service.DialogueOperationStatus;
import io.github.kxyr0.echodialogue.api.service.DialogueQueryResult;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.service.DialogueStartRequest;
import io.github.kxyr0.echodialogue.api.service.DialogueStartResult;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionStatus;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionCancelRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionRecoveryRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

/**
 * Canonical `echo_dialogue:play` action handler.
 */
final class DialoguePlayActionHandler implements StoryActionHandler {
    private final MinecraftServer server;
    private final DialogueServiceLookup dialogueServices = new DialogueServiceLookup();
    private final io.github.kxyr0.echostorydialogueadapter.bridge.observation.DialogueObservationFlow observations =
            new io.github.kxyr0.echostorydialogueadapter.bridge.observation.DialogueObservationFlow();

    DialoguePlayActionHandler(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public NamespacedId actionType() {
        return DialogueBridgeIds.ACTION_TYPE;
    }

    @Override
    public ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink completionSink) {
        return invoke(new StoryActionContext(request.storyId(), request.executionId(), request.actionExecutionId()),
                request.actionType(), request.payload(), completionSink, true);
    }

    @Override
    public ActionInvocationResult recover(ActionRecoveryRequest request, ActionCompletionSink completionSink) {
        return invoke(new StoryActionContext(request.storyId(), request.executionId(), request.actionExecutionId()),
                request.actionType(), request.payload(), completionSink, false);
    }

    @Override
    public ActionInvocationResult cancel(ActionCancelRequest request) {
        if (!DialogueBridgeIds.ACTION_TYPE.equals(request.actionType())) {
            return ActionInvocationResult.unsupported("Неподдерживаемый action type: " + request.actionType());
        }
        try {
            DialogueCancelResult result = dialogueServices.lookup(server)
                    .cancel(DialogueBridgeIds.dialogueSessionId(request.actionExecutionId()));
            observations.close(request.actionExecutionId());
            return result.status() == DialogueOperationStatus.SERVICE_UNAVAILABLE
                    ? ActionInvocationResult.rejected("Echo Dialogue service недоступен для cancellation.")
                    : ActionInvocationResult.accepted();
        } catch (RuntimeException exception) {
            return ActionInvocationResult.rejected("Echo Dialogue cancellation lookup failed.");
        }
    }

    void close() {
        observations.clear();
    }

    private ActionInvocationResult invoke(
            StoryActionContext context,
            NamespacedId actionType,
            Map<String, String> payload,
            ActionCompletionSink completionSink,
            boolean startWhenMissing
    ) {
        if (!DialogueBridgeIds.ACTION_TYPE.equals(actionType)) {
            return ActionInvocationResult.unsupported("Неподдерживаемый action type: " + actionType);
        }
        try {
            DialogueActionPayload decoded = DialogueActionPayload.decode(payload, context.executionId(),
                    context.actionExecutionId(), server);
            DialogueService service = dialogueServices.lookup(server);
            return startWhenMissing
                    ? start(service, decoded, context, completionSink)
                    : handleRecovery(service, decoded, context, completionSink);
        } catch (IllegalArgumentException exception) {
            return ActionInvocationResult.rejected("Некорректный echo_dialogue:play payload.");
        } catch (RuntimeException exception) {
            return ActionInvocationResult.rejected("Echo Dialogue service lookup failed.");
        }
    }

    private ActionInvocationResult handleRecovery(
            DialogueService service,
            DialogueActionPayload decoded,
            StoryActionContext context,
            ActionCompletionSink completionSink
    ) {
        DialogueQueryResult query = service.query(decoded.sessionId());
        if (query.status() == DialogueOperationStatus.NOT_FOUND) {
            return start(service, decoded, context, completionSink);
        }
        return handle(query.status(), query.session(), service, context, completionSink);
    }

    private ActionInvocationResult start(
            DialogueService service,
            DialogueActionPayload decoded,
            StoryActionContext context,
            ActionCompletionSink completionSink
    ) {
        if (!observations.capacity(context.actionExecutionId())) {
            return ActionInvocationResult.rejected("Adapter terminal observation limit reached.");
        }
        DialogueStartResult result = service.start(request(decoded, context));
        if (replaceableStoryConflict(result.status(), result.session(), decoded)) {
            return replaceConflictingStorySession(service, result.session().orElseThrow(), decoded, context,
                    completionSink);
        }
        return handleStart(result, service, context, completionSink);
    }

    private ActionInvocationResult handleStart(
            DialogueStartResult result,
            DialogueService service,
            StoryActionContext context,
            ActionCompletionSink completionSink
    ) {
        return handle(result.status(), result.session(), service, context, completionSink);
    }

    private ActionInvocationResult handle(
            DialogueOperationStatus status,
            Optional<DialogueSessionView> session,
            DialogueService service,
            StoryActionContext context,
            ActionCompletionSink completionSink
    ) {
        if (status == DialogueOperationStatus.SERVICE_UNAVAILABLE) {
            return ActionInvocationResult.rejected("Echo Dialogue service ещё не готов.");
        }
        if (DialogueBridgeIds.sameActionSession(status, session,
                DialogueBridgeIds.dialogueSessionId(context.actionExecutionId()))
                && session.filter(DialogueBridgeIds::activeOrWaiting).isPresent()) {
            return observe(service, session.orElseThrow(), context, completionSink);
        }
        session.ifPresentOrElse(value -> complete(value, context, completionSink),
                () -> DialogueCompletionMapping.completeFailure(context.storyId(), context.executionId(),
                        context.actionExecutionId(),
                        "dialogue-start-" + status.name(), completionSink));
        return ActionInvocationResult.accepted();
    }

    private ActionInvocationResult replaceConflictingStorySession(DialogueService service,
            DialogueSessionView conflicting, DialogueActionPayload decoded, StoryActionContext context,
            ActionCompletionSink completionSink) {
        DialogueService actual = service == null ? dialogueServices.lookup(server) : service;
        return observations.replace(actual, conflicting, context.actionExecutionId(),
                () -> handleStart(actual.start(request(decoded, context)), actual, context, completionSink),
                code -> DialogueCompletionMapping.completeFailure(context.storyId(), context.executionId(),
                        context.actionExecutionId(), code, completionSink));
    }
    private ActionInvocationResult observe(DialogueService service, DialogueSessionView session,
            StoryActionContext context, ActionCompletionSink completionSink) {
        DialogueService actual = service == null ? dialogueServices.lookup(server) : service;
        return observations.observe(actual, session, context.actionExecutionId(),
                value -> complete(value, context, completionSink));
    }

    private DialogueStartRequest request(DialogueActionPayload decoded, StoryActionContext context) {
        return new DialogueStartRequest(decoded.sessionId(), decoded.dialogueId(), decoded.audienceId(),
                Optional.of(new DialogueStartRequest.Origin(context.executionId().asString(),
                        context.executionId().asString())));
    }

    private void complete(
            DialogueSessionView session,
            StoryActionContext context,
            ActionCompletionSink completionSink
    ) {
        DialogueCompletionMapping.complete(session, context.storyId(), context.executionId(),
                context.actionExecutionId(), completionSink);
    }

    static boolean replaceableStoryConflict(DialogueOperationStatus status,
            Optional<DialogueSessionView> session, DialogueActionPayload decoded) {
        return DialogueBridgeIds.replaceableStoryConflict(status, session, decoded);
    }

    private record StoryActionContext(StoryId storyId, ExecutionId executionId, ActionExecutionId actionExecutionId) {
    }
}
