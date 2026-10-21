package io.github.kxyr0.echotriggervisibilityadapter.forge.story;

import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionCancelRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionRecoveryRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import io.github.kxyr0.echovisibility.forge.api.VisibilityServiceResult;
import io.github.kxyr0.echovisibility.forge.api.runtime.VisibilityServiceBridge;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

final class VisibilityStoryActionHandler implements StoryActionHandler {
    private final MinecraftServer server;
    private final VisibilityStoryActionKind kind;
    private final Map<ActionExecutionId, Pending> pending = new LinkedHashMap<>();

    VisibilityStoryActionHandler(MinecraftServer server, VisibilityStoryActionKind kind) {
        this.server = server;
        this.kind = kind;
    }

    @Override
    public NamespacedId actionType() {
        return kind.actionType();
    }

    @Override
    public ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink sink) {
        return invoke(new Pending(request, sink, false));
    }

    @Override
    public ActionInvocationResult recover(ActionRecoveryRequest request, ActionCompletionSink sink) {
        return invoke(new Pending(request, sink));
    }

    private ActionInvocationResult invoke(Pending invocation) {
        Optional<VisibilityStoryRequestContext> context = VisibilityStoryRequestContext.resolve(
                invocation.request());
        Optional<VisibilityServiceBridge> visibility = VisibilityStorySession.lookup(server);
        if (context.isEmpty()) {
            return ActionInvocationResult.rejected(
                    "Echo Visibility requires a persisted player and watch context.");
        }
        Optional<VisibilityStoryOnlineContext> online = context.orElseThrow().online(server);
        if (online.isEmpty() || visibility.isEmpty()) {
            if (invocation.recovering()) {
                pending.putIfAbsent(invocation.request().actionExecutionId(), invocation);
                return ActionInvocationResult.accepted();
            }
            return ActionInvocationResult.rejected(
                    "Echo Visibility requires an online player and an available service.");
        }
        var access = online.orElseThrow();
        var bridge = visibility.orElseThrow();
        if (kind.waiting()) {
            return waitOrComplete(invocation, access, bridge);
        }
        return switch (kind) {
            case ARM -> arm(invocation, access, bridge, invocation.recovering());
            case DISARM -> disarm(invocation, access, bridge, invocation.recovering());
            case ASSERT_OUT -> service(invocation, bridge.service().assertStillOutOfView(
                    access.player(), access.watchId()));
            case IS_OUT, IS_VISIBLE -> query(invocation, access, bridge);
            default -> ActionInvocationResult.unsupported("Unsupported visibility action.");
        };
    }

    private ActionInvocationResult arm(
            Pending invocation,
            VisibilityStoryOnlineContext access,
            VisibilityServiceBridge bridge,
            boolean recovering) {
        String owner = "story:" + invocation.request().actionExecutionId().asString();
        if (recovering && bridge.query().find(access.player(), access.watchId())
                .filter(watch -> watch.ownerLineage().equals(owner)).isPresent()) {
            complete(invocation, ExternalActionResultCategory.SUCCESS, Map.of("status", "RECOVERED"));
            return ActionInvocationResult.accepted();
        }
        return service(invocation, bridge.service().arm(access.player(), access.watchId(), owner));
    }

    private ActionInvocationResult disarm(
            Pending invocation,
            VisibilityStoryOnlineContext access,
            VisibilityServiceBridge bridge,
            boolean recovering) {
        VisibilityServiceResult result = bridge.service().disarm(access.player(), access.watchId());
        if (recovering && result.status() == VisibilityServiceResult.Status.NOT_ARMED) {
            complete(invocation, ExternalActionResultCategory.SUCCESS, Map.of("status", "RECOVERED"));
            return ActionInvocationResult.accepted();
        }
        return service(invocation, result);
    }

    private ActionInvocationResult waitOrComplete(
            Pending invocation,
            VisibilityStoryOnlineContext access,
            VisibilityServiceBridge bridge) {
        if (desired(access, bridge)) {
            complete(invocation, ExternalActionResultCategory.SUCCESS, Map.of("status", "SATISFIED"));
        } else {
            pending.putIfAbsent(invocation.request().actionExecutionId(), invocation);
        }
        return ActionInvocationResult.accepted();
    }

    private ActionInvocationResult query(
            Pending invocation,
            VisibilityStoryOnlineContext access,
            VisibilityServiceBridge bridge) {
        boolean result = desired(access, bridge);
        String outputKey = invocation.request().payload().getOrDefault("outputKey", "result");
        Map<String, String> outputs = new LinkedHashMap<>();
        outputs.put("status", "QUERIED");
        outputs.put(outputKey, Boolean.toString(result));
        complete(invocation, ExternalActionResultCategory.SUCCESS, outputs);
        return ActionInvocationResult.accepted();
    }

    private boolean desired(VisibilityStoryOnlineContext access, VisibilityServiceBridge bridge) {
        boolean visible = bridge.query().isVisible(access.player(), access.watchId());
        boolean out = bridge.query().isOutOfView(access.player(), access.watchId());
        return kind.desiredState(visible, out);
    }

    private ActionInvocationResult service(Pending invocation, VisibilityServiceResult result) {
        ExternalActionResultCategory category = result.successful()
                ? ExternalActionResultCategory.SUCCESS : ExternalActionResultCategory.FAILURE;
        complete(invocation, category, Map.of("status", result.status().name()));
        return ActionInvocationResult.accepted();
    }

    @Override
    public ActionInvocationResult cancel(ActionCancelRequest request) {
        pending.remove(request.actionExecutionId());
        return ActionInvocationResult.accepted();
    }

    void tick(VisibilityServiceBridge bridge) {
        for (Pending invocation : pending.values().stream().toList()) {
            VisibilityStoryRequestContext.resolve(invocation.request())
                    .flatMap(context -> context.online(server))
                    .ifPresent(access -> resume(invocation, access, bridge));
        }
    }

    private void resume(
            Pending invocation,
            VisibilityStoryOnlineContext access,
            VisibilityServiceBridge bridge) {
        if (kind.waiting() && !desired(access, bridge)) {
            return;
        }
        pending.remove(invocation.request().actionExecutionId());
        if (kind.waiting()) {
            complete(invocation, ExternalActionResultCategory.SUCCESS, Map.of("status", "SATISFIED"));
        } else {
            invoke(invocation);
        }
    }

    void close() {
        pending.clear();
    }

    private void complete(
            Pending invocation,
            ExternalActionResultCategory category,
            Map<String, String> outputs) {
        ActionStartRequest request = invocation.request();
        invocation.sink().complete(new ActionCompletion(request.storyId(), request.executionId(),
                request.actionExecutionId(), request.actionType(), category, outputs));
    }

    private record Pending(ActionStartRequest request, ActionCompletionSink sink, boolean recovering) {
        Pending(ActionRecoveryRequest request, ActionCompletionSink sink) {
            this(new ActionStartRequest(request.storyId(), request.executionId(), request.actionExecutionId(),
                    request.actionType(), request.payload()), sink, true);
        }
    }
}
