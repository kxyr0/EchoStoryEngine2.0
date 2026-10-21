package io.github.kxyr0.echonpcbackpack.script.story;

import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackCommandId;
import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackCommandResult;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackOutcome;
import io.github.kxyr0.echonpcbackpack.script.NpcBackpackScriptFacade;
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

final class NpcBackpackStoryAction implements StoryActionHandler {
    private final NpcBackpackScriptFacade facade;
    private final NamespacedId actionType;
    private final boolean equip;
    private final Map<NpcBackpackCommandId, Pending> pending = new LinkedHashMap<>();

    NpcBackpackStoryAction(MinecraftServer server, NamespacedId actionType, boolean equip) {
        facade = new NpcBackpackScriptFacade(server);
        this.actionType = actionType;
        this.equip = equip;
    }

    @Override
    public NamespacedId actionType() {
        return actionType;
    }

    @Override
    public ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink sink) {
        try {
            String owner = request.executionId().asString() + ":" + request.actionExecutionId().asString();
            NpcBackpackCommandResult result = equip
                    ? facade.equip(owner, request.payload()) : facade.unequip(owner, request.payload());
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
        pending.entrySet().removeIf(entry -> entry.getValue().request().actionExecutionId()
                .equals(request.actionExecutionId()));
        return ActionInvocationResult.accepted();
    }

    void tick() {
        for (Map.Entry<NpcBackpackCommandId, Pending> entry : pending.entrySet().stream().toList()) {
            facade.command(entry.getKey().value()).filter(NpcBackpackCommandResult::terminal)
                    .ifPresent(result -> complete(entry.getKey(), entry.getValue(), result));
        }
    }

    void close() {
        pending.clear();
    }

    private void complete(NpcBackpackCommandId commandId, Pending action, NpcBackpackCommandResult result) {
        ExternalActionResultCategory category = success(result.outcome())
                ? ExternalActionResultCategory.SUCCESS : ExternalActionResultCategory.FAILURE;
        ActionStartRequest request = action.request();
        action.sink().complete(new ActionCompletion(request.storyId(), request.executionId(),
                request.actionExecutionId(), request.actionType(), category, Map.of(
                "commandId", commandId.value(), "outcome", result.outcome().name(),
                "diagnosticCode", result.diagnosticCode())));
        pending.remove(commandId);
    }

    private boolean success(NpcBackpackOutcome outcome) {
        return outcome == NpcBackpackOutcome.SUCCEEDED || outcome == NpcBackpackOutcome.ALREADY_SATISFIED;
    }

    private record Pending(ActionStartRequest request, ActionCompletionSink sink) {
    }
}
