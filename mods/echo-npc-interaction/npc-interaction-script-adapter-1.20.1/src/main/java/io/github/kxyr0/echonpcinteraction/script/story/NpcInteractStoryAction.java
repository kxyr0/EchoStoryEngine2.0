package io.github.kxyr0.echonpcinteraction.script.story;

import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionCommandId;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionStatus;
import io.github.kxyr0.echonpcinteraction.script.NpcInteractionScriptFacade;
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

final class NpcInteractStoryAction implements StoryActionHandler {
    private final MinecraftServer server;
    private final NpcInteractionScriptFacade facade = new NpcInteractionScriptFacade();
    private final Map<NpcInteractionCommandId, Pending> pending = new LinkedHashMap<>();

    NpcInteractStoryAction(MinecraftServer server) {
        this.server = server;
    }

    MinecraftServer server() {
        return server;
    }

    @Override
    public NamespacedId actionType() {
        return NpcInteractionStoryExtensionFactory.INTERACT;
    }

    @Override
    public ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink sink) {
        String owner = request.executionId().asString() + ":" + request.actionExecutionId().asString();
        facade.interact(server, owner, request.payload())
                .ifPresent(result -> pending.put(result.commandId(), new Pending(request, sink)));
        return ActionInvocationResult.accepted();
    }

    @Override
    public ActionInvocationResult recover(ActionRecoveryRequest request, ActionCompletionSink sink) {
        return start(new ActionStartRequest(request.storyId(), request.executionId(),
                request.actionExecutionId(), request.actionType(), request.payload()), sink);
    }

    @Override
    public ActionInvocationResult cancel(ActionCancelRequest request) {
        pending.entrySet().removeIf(entry -> entry.getValue().request().actionExecutionId()
                .equals(request.actionExecutionId()));
        return ActionInvocationResult.accepted();
    }

    void tick() {
        for (Map.Entry<NpcInteractionCommandId, Pending> entry : pending.entrySet().stream().toList()) {
            facade.result(server, entry.getKey()).filter(facade::isTerminal)
                    .ifPresent(result -> complete(entry.getKey(), entry.getValue(), result.status()));
        }
    }

    void close() {
        pending.clear();
    }

    private void complete(NpcInteractionCommandId commandId, Pending pendingAction, NpcInteractionStatus status) {
        ExternalActionResultCategory category = status == NpcInteractionStatus.COMPLETED
                ? ExternalActionResultCategory.SUCCESS : ExternalActionResultCategory.FAILURE;
        var request = pendingAction.request();
        pendingAction.sink().complete(new ActionCompletion(request.storyId(), request.executionId(),
                request.actionExecutionId(), request.actionType(), category,
                Map.of("commandId", commandId.value(), "status", status.name())));
        pending.remove(commandId);
    }

    private record Pending(ActionStartRequest request, ActionCompletionSink sink) {
    }
}
