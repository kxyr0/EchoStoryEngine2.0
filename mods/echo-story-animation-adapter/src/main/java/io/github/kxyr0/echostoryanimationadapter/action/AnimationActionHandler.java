package io.github.kxyr0.echostoryanimationadapter.action;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlayResult;
import io.github.kxyr0.echoanimation.api.animation.AnimationStopResult;
import io.github.kxyr0.echoanimation.forge.api.AnimationService;
import io.github.kxyr0.echoanimation.forge.api.AnimationServiceBridge;
import io.github.kxyr0.echoanimation.forge.api.AnimationServiceLookupResult;
import io.github.kxyr0.echostoryanimationadapter.action.AnimationActionPayload.AwaitMode;
import io.github.kxyr0.echostoryanimationadapter.action.AnimationCompletionMapping.Scope;
import io.github.kxyr0.echostoryanimationadapter.action.AnimationCompletionMapping.StartedPlayback;
import io.github.kxyr0.echostoryanimationadapter.bridge.AnimationBridgeIds;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionCancelRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionRecoveryRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

/**
 * Story action handler for one Echo Animation action type.
 */
public final class AnimationActionHandler implements StoryActionHandler {
    private final MinecraftServer server;
    private final NamespacedId actionType;
    private final AnimationPendingActions pending = new AnimationPendingActions();

    public AnimationActionHandler(MinecraftServer server, NamespacedId actionType) {
        this.server = Objects.requireNonNull(server, "server");
        this.actionType = Objects.requireNonNull(actionType, "actionType");
    }

    @Override
    public NamespacedId actionType() {
        return actionType;
    }

    @Override
    public ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink completionSink) {
        return invoke(scope(request.storyId(), request.executionId(), request.actionExecutionId(),
                request.actionType()), request.payload(), completionSink);
    }

    @Override
    public ActionInvocationResult recover(ActionRecoveryRequest request, ActionCompletionSink completionSink) {
        return invoke(scope(request.storyId(), request.executionId(), request.actionExecutionId(),
                request.actionType()), request.payload(), completionSink);
    }

    @Override
    public ActionInvocationResult cancel(ActionCancelRequest request) {
        if (!actionType.equals(request.actionType())) {
            return ActionInvocationResult.unsupported("Unsupported action type: " + request.actionType());
        }
        pending.cancel(request.actionExecutionId());
        return ActionInvocationResult.accepted();
    }

    public void tick() {
        Optional<AnimationService> resolved = service();
        resolved.ifPresent(pending::tick);
    }

    public void close() {
        pending.close();
    }

    private ActionInvocationResult invoke(
            Scope scope,
            Map<String, String> rawPayload,
            ActionCompletionSink completionSink
    ) {
        if (!actionType.equals(scope.actionType())) {
            return ActionInvocationResult.unsupported("Unsupported action type: " + scope.actionType());
        }
        Optional<AnimationService> resolved = service();
        if (resolved.isEmpty()) {
            return ActionInvocationResult.rejected("Echo Animation service is unavailable.");
        }
        try {
            Optional<ActionInvocationResult> rejectedAwait = AnimationAwaitPolicy.rejectImpossibleAwait(
                    actionType, resolved.orElseThrow(), rawPayload);
            if (rejectedAwait.isPresent()) {
                return rejectedAwait.orElseThrow();
            }
            AnimationActionPayload payload = AnimationActionPayload.decode(rawPayload,
                    AnimationBridgeIds.PLAY_ANIMATION.equals(actionType),
                    AnimationAwaitPolicy.defaultAwait(actionType, resolved.orElseThrow(), rawPayload));
            return dispatch(resolved.orElseThrow(), scope, payload, completionSink);
        } catch (IllegalArgumentException exception) {
            return ActionInvocationResult.rejected("Invalid Echo Animation action payload.");
        }
    }

    private ActionInvocationResult dispatch(
            AnimationService service,
            Scope scope,
            AnimationActionPayload payload,
            ActionCompletionSink completionSink
    ) {
        if (AnimationBridgeIds.PLAY_ANIMATION.equals(actionType)) {
            return play(service, scope, payload, completionSink);
        }
        if (AnimationBridgeIds.STOP_ANIMATION.equals(actionType)) {
            return stop(service, scope, payload, completionSink);
        }
        return waitForTerminal(service, scope, payload, completionSink);
    }

    private ActionInvocationResult play(
            AnimationService service,
            Scope scope,
            AnimationActionPayload payload,
            ActionCompletionSink completionSink
    ) {
        AnimationCommandId commandId = DeterministicAnimationCommandId.from(scope.actionExecutionId(), actionType);
        AnimationPlayResult play = service.playAnimation(payload.animationId(), payload.subjectId(), commandId);
        if (play.status() == AnimationPlayResult.Status.STARTED
                || play.status() == AnimationPlayResult.Status.ALREADY_APPLIED) {
            if (payload.awaitMode() == AwaitMode.STARTED) {
                var query = service.queryAnimation(payload.subjectId(), payload.animationId());
                AnimationCompletionMapping.started(scope, payload, new StartedPlayback(play, query, commandId),
                        completionSink);
                return ActionInvocationResult.accepted();
            }
            return pending.observeOrComplete(scope, payload, completionSink,
                    service.queryAnimation(payload.subjectId(), payload.animationId()));
        }
        AnimationCompletionMapping.failure(scope, payload, play.status().name(), play.message(), completionSink);
        return ActionInvocationResult.accepted();
    }

    private ActionInvocationResult stop(
            AnimationService service,
            Scope scope,
            AnimationActionPayload payload,
            ActionCompletionSink completionSink
    ) {
        AnimationCommandId commandId = DeterministicAnimationCommandId.from(scope.actionExecutionId(), actionType);
        AnimationStopResult stop = service.stopAnimation(payload.subjectId(), Optional.of(payload.animationId()),
                commandId);
        if (stop.status() == AnimationStopResult.Status.STOP_REQUESTED
                || stop.status() == AnimationStopResult.Status.ALREADY_APPLIED) {
            return pending.observeOrComplete(scope, payload, completionSink,
                    service.queryAnimation(payload.subjectId(), payload.animationId()));
        }
        AnimationCompletionMapping.failure(scope, payload, stop.status().name(), stop.message(), completionSink);
        return ActionInvocationResult.accepted();
    }

    private ActionInvocationResult waitForTerminal(
            AnimationService service,
            Scope scope,
            AnimationActionPayload payload,
            ActionCompletionSink completionSink
    ) {
        return pending.observeOrComplete(scope, payload, completionSink,
                service.queryAnimation(payload.subjectId(), payload.animationId()));
    }

    private Optional<AnimationService> service() {
        AnimationServiceLookupResult lookup = AnimationServiceBridge.lookup(server);
        return lookup.status() == AnimationServiceLookupResult.Status.RESOLVED ? lookup.service() : Optional.empty();
    }

    private Scope scope(
            io.github.kxyr0.echostoryengine.story.api.identity.StoryId storyId,
            io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId executionId,
            ActionExecutionId actionExecutionId,
            NamespacedId requestedActionType
    ) {
        return new Scope(storyId, executionId, actionExecutionId, requestedActionType);
    }

}
