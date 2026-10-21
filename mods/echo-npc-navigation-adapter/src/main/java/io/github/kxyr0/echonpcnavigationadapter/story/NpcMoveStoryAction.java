package io.github.kxyr0.echonpcnavigationadapter.story;

import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.movement.NavigationSnapshot;
import io.github.kxyr0.echonavigation.api.movement.NavigationStatus;
import io.github.kxyr0.echonavigation.forge.api.service.EchoNavigationServiceBridge;
import io.github.kxyr0.echonavigation.forge.api.service.NavigationService;
import io.github.kxyr0.echonpcnavigationadapter.script.NpcNavigationPayloads.AwaitMode;
import io.github.kxyr0.echonpcnavigationadapter.script.NpcNavigationPayloads;
import io.github.kxyr0.echonpcnavigationadapter.script.NpcNavigationScriptFacade;
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

final class NpcMoveStoryAction implements StoryActionHandler {
    private final MinecraftServer server;
    private final NpcNavigationScriptFacade facade = new NpcNavigationScriptFacade();
    private final Map<NavigationMoveId, Pending> pending = new LinkedHashMap<>();

    NpcMoveStoryAction(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public NamespacedId actionType() {
        return NpcNavigationStoryExtensionFactory.MOVE;
    }

    @Override
    public ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink sink) {
        return invoke(new Pending(request, sink));
    }

    @Override
    public ActionInvocationResult recover(ActionRecoveryRequest request, ActionCompletionSink sink) {
        return invoke(new Pending(request, sink));
    }

    private ActionInvocationResult invoke(Pending pendingAction) {
        ActionStartRequest request = pendingAction.request();
        try {
            AwaitMode awaitMode = NpcNavigationPayloads.awaitMode(request.payload());
            var move = facade.move(server, request.executionId().asString(),
                    NpcNavigationPayloads.move(request.payload()));
            if (move.snapshot().isEmpty()) {
                return ActionInvocationResult.rejected("Echo NPC Navigation Adapter: " + move.message());
            }
            NavigationSnapshot snapshot = move.snapshot().orElseThrow();
            if (!acceptedMove(move.status())) {
                if (move.status() == NavigationService.Status.FAILED && snapshot.status().terminal()) {
                    complete(snapshot.moveId(), pendingAction, snapshot.status());
                    return ActionInvocationResult.accepted();
                }
                return ActionInvocationResult.rejected("Echo NPC Navigation Adapter: " + move.message());
            }
            if (awaitMode == AwaitMode.STARTED) {
                completeStarted(snapshot, pendingAction);
                return ActionInvocationResult.accepted();
            }
            if (snapshot.status().terminal()) {
                complete(snapshot.moveId(), pendingAction, snapshot.status());
            } else {
                pending.put(snapshot.moveId(), pendingAction);
            }
            return ActionInvocationResult.accepted();
        } catch (IllegalArgumentException exception) {
            return ActionInvocationResult.rejected("Echo NPC Navigation Adapter: " + exception.getMessage());
        }
    }

    private boolean acceptedMove(NavigationService.Status status) {
        return status == NavigationService.Status.ACCEPTED
                || status == NavigationService.Status.WAITING
                || status == NavigationService.Status.DUPLICATE;
    }

    private void completeStarted(NavigationSnapshot snapshot, Pending pendingAction) {
        boolean terminalFailure = snapshot.status().terminal() && snapshot.status() != NavigationStatus.ARRIVED;
        ExternalActionResultCategory category = terminalFailure
                ? ExternalActionResultCategory.FAILURE
                : ExternalActionResultCategory.SUCCESS;
        pendingAction.sink.complete(new ActionCompletion(pendingAction.request.storyId(),
                pendingAction.request.executionId(), pendingAction.request.actionExecutionId(),
                pendingAction.request.actionType(), category,
                Map.of("moveId", snapshot.moveId().value(), "status", snapshot.status().name())));
    }

    @Override
    public ActionInvocationResult cancel(ActionCancelRequest request) {
        pending.entrySet().stream()
                .filter(entry -> entry.getValue().request.actionExecutionId().equals(request.actionExecutionId()))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(this::cancelMove);
        return ActionInvocationResult.accepted();
    }

    void tick() {
        var lookup = EchoNavigationServiceBridge.lookup(server);
        if (lookup.queryService().isEmpty()) {
            return;
        }
        for (Map.Entry<NavigationMoveId, Pending> entry : pending.entrySet().stream().toList()) {
            lookup.queryService().orElseThrow().move(entry.getKey()).filter(snapshot -> snapshot.status().terminal())
                    .ifPresent(snapshot -> complete(entry.getKey(), entry.getValue(), snapshot.status()));
        }
    }

    void close() {
        pending.clear();
    }

    private void cancelMove(NavigationMoveId moveId) {
        EchoNavigationServiceBridge.lookup(server).service().ifPresent(service -> service.cancel(moveId));
        pending.remove(moveId);
    }

    private void complete(NavigationMoveId moveId, Pending pendingAction, NavigationStatus status) {
        ExternalActionResultCategory category = status == NavigationStatus.ARRIVED
                ? ExternalActionResultCategory.SUCCESS : ExternalActionResultCategory.FAILURE;
        pendingAction.sink.complete(new ActionCompletion(pendingAction.request.storyId(),
                pendingAction.request.executionId(), pendingAction.request.actionExecutionId(),
                pendingAction.request.actionType(), category, Map.of("moveId", moveId.value(), "status", status.name())));
        pending.remove(moveId);
    }

    private record Pending(ActionStartRequest request, ActionCompletionSink sink) {
        Pending(ActionRecoveryRequest request, ActionCompletionSink sink) {
            this(new ActionStartRequest(request.storyId(), request.executionId(), request.actionExecutionId(),
                    request.actionType(), request.payload()), sink);
        }
    }
}
