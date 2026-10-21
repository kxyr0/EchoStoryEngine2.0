package io.github.kxyr0.echoitemattachments.script.story;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.model.CommandStatus;
import io.github.kxyr0.echoitemattachments.api.result.AttachmentCommandResult;
import io.github.kxyr0.echoitemattachments.script.ItemAttachmentScriptFacade;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionCancelRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionRecoveryRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

final class ItemAttachmentStoryAction implements StoryActionHandler {
    private final ItemAttachmentScriptFacade facade;
    private final NamespacedId actionType;
    private final String operation;
    private final Map<AttachmentCommandId, Pending> pending = new LinkedHashMap<>();

    ItemAttachmentStoryAction(MinecraftServer server, NamespacedId actionType, String operation) {
        facade = new ItemAttachmentScriptFacade(server);
        this.actionType = actionType;
        this.operation = operation;
    }

    @Override public NamespacedId actionType() { return actionType; }

    @Override
    public ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink sink) {
        try {
            String owner = "system:story/" + request.actionExecutionId().asString();
            AttachmentCommandResult result = facade.invoke(operation, owner, request.payload());
            pending.put(result.commandId(), new Pending(request, sink));
            return ActionInvocationResult.accepted();
        } catch (RuntimeException exception) {
            return ActionInvocationResult.rejected(exception.getMessage());
        }
    }

    @Override
    public ActionInvocationResult recover(ActionRecoveryRequest request, ActionCompletionSink sink) {
        return start(new ActionStartRequest(request.storyId(), request.executionId(),
                request.actionExecutionId(), request.actionType(), request.payload()), sink);
    }

    @Override
    public ActionInvocationResult cancel(ActionCancelRequest request) {
        pending.entrySet().stream().filter(entry -> entry.getValue().request().actionExecutionId()
                .equals(request.actionExecutionId())).map(Map.Entry::getKey).toList().forEach(facade::cancel);
        pending.entrySet().removeIf(entry -> entry.getValue().request().actionExecutionId()
                .equals(request.actionExecutionId()));
        return ActionInvocationResult.accepted();
    }

    void tick() {
        pending.entrySet().stream().toList().forEach(entry -> facade.command(entry.getKey())
                .filter(AttachmentCommandResult::terminal)
                .ifPresent(result -> complete(entry.getKey(), entry.getValue(), result)));
    }

    void close() { pending.clear(); }

    private void complete(AttachmentCommandId id, Pending action, AttachmentCommandResult result) {
        ExternalActionResultCategory category = result.status() == CommandStatus.SUCCEEDED
                ? ExternalActionResultCategory.SUCCESS : ExternalActionResultCategory.FAILURE;
        ActionStartRequest request = action.request();
        action.sink().complete(new ActionCompletion(request.storyId(), request.executionId(),
                request.actionExecutionId(), request.actionType(), category, Map.of(
                "commandId", id.value(), "status", result.status().name(),
                "diagnosticCode", result.diagnosticCode(), "message", result.message())));
        pending.remove(id);
    }

    private record Pending(ActionStartRequest request, ActionCompletionSink sink) {
    }
}
