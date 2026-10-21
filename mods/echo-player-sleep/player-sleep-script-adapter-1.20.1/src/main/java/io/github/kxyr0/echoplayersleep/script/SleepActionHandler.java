package io.github.kxyr0.echoplayersleep.script;

import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepResult;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepCommandId;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepStatus;
import io.github.kxyr0.echoplayersleep.api.world.Ticks;
import io.github.kxyr0.echoplayersleep.forge.api.PlayerSleepServiceBridge;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionCancelRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionRecoveryRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import java.util.Map;
import java.util.Objects;

public final class SleepActionHandler implements StoryActionHandler {
    private final NamespacedId actionType;
    private final PlayerSleepServiceBridge services;
    private final SleepBlackspaceActionFlow blackspaceActions;

    public SleepActionHandler(NamespacedId actionType, PlayerSleepServiceBridge services) {
        this.actionType = Objects.requireNonNull(actionType, "actionType");
        this.services = Objects.requireNonNull(services, "services");
        blackspaceActions = new SleepBlackspaceActionFlow(actionType, services);
    }

    @Override
    public NamespacedId actionType() {
        return actionType;
    }

    @Override
    public ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink completionSink) {
        try {
            SleepPayload payload = SleepPayload.inherited(request.executionId().asString(), request.payload());
            ActionInvocationResult delayed = delayed(request, completionSink, payload);
            if (delayed != null) {
                return delayed;
            }
            PlayerSleepResult result = dispatch(payload, request.actionExecutionId().toString());
            complete(request, completionSink, category(result), Map.of("outcome", result.outcome().name()));
            return ActionInvocationResult.accepted();
        } catch (RuntimeException exception) {
            return ActionInvocationResult.rejected(exception.getMessage());
        }
    }

    @Override
    public ActionInvocationResult recover(ActionRecoveryRequest request, ActionCompletionSink completionSink) {
        return start(new ActionStartRequest(request.storyId(), request.executionId(),
                request.actionExecutionId(), request.actionType(), request.payload()), completionSink);
    }

    @Override
    public ActionInvocationResult cancel(ActionCancelRequest request) {
        return ActionInvocationResult.accepted();
    }

    private ActionInvocationResult delayed(
            ActionStartRequest request,
            ActionCompletionSink completionSink,
            SleepPayload payload) {
        String execution = request.executionId().asString();
        String fallback = request.actionExecutionId().toString();
        PlayerSleepCommandId command = payload.command(fallback);
        if (actionType.equals(SleepActionCatalog.CLOSE_EYES)) {
            return closeEyes(request, completionSink, payload, execution, fallback);
        }
        if (actionType.equals(SleepActionCatalog.OPEN_EYES)) {
            return openEyes(request, completionSink, payload, execution, command);
        }
        if (actionType.equals(SleepActionCatalog.WAIT_UNTIL_CLOSED)) {
            return SleepActionDelayScheduler.waitUntilClosed(request, completionSink, services, payload, command);
        }
        if (actionType.equals(SleepActionCatalog.WAIT_UNTIL_OPEN)) {
            return SleepActionDelayScheduler.waitUntilOpen(request, completionSink, services, payload, command);
        }
        if (actionType.equals(SleepActionCatalog.WAIT)) {
            return SleepActionDelayScheduler.wait(request, completionSink, payload.waitTicks(20L));
        }
        if (actionType.equals(SleepActionCatalog.ENTER_DREAM) && payload.blackspaceInstanceId().isPresent()) {
            return blackspaceActions.enterDream(request, completionSink, payload, command);
        }
        if (actionType.equals(SleepActionCatalog.RETURN_TO_BED) && SleepBlackspaceBridge.isActive(payload.player())) {
            return blackspaceActions.returnToBed(request, completionSink, payload, command);
        }
        return null;
    }

    private ActionInvocationResult closeEyes(
            ActionStartRequest request,
            ActionCompletionSink completionSink,
            SleepPayload payload,
            String execution,
            String fallback) {
        Ticks duration = payload.duration(18L);
        PlayerSleepResult result = services.sessions().closeEyes(payload.player(),
                payload.command(fallback), duration);
        if (result.diagnostics().isEmpty()) {
            SleepActionDelayScheduler.rememberClose(execution, payload.player(), duration);
        }
        complete(request, completionSink, category(result), Map.of("outcome", result.outcome().name()));
        return ActionInvocationResult.accepted();
    }

    private ActionInvocationResult openEyes(
            ActionStartRequest request,
            ActionCompletionSink completionSink,
            SleepPayload payload,
            String execution,
            PlayerSleepCommandId command) {
        Ticks duration = payload.duration(16L);
        PlayerSleepResult result = services.sessions().openEyes(payload.player(), command,
                duration, payload.sleepyBlink(), payload.showHud());
        if (result.diagnostics().isEmpty()) {
            SleepActionDelayScheduler.rememberOpen(execution, payload.player(), duration);
        }
        complete(request, completionSink, category(result), Map.of("outcome", result.outcome().name()));
        return ActionInvocationResult.accepted();
    }

    private PlayerSleepResult dispatch(SleepPayload payload, String fallbackCommand) {
        if (actionType.equals(SleepActionCatalog.ARM)) {
            return services.arms().arm(payload.player(), payload.scenarioId(),
                    payload.owner(fallbackCommand), payload.command(fallbackCommand));
        }
        if (actionType.equals(SleepActionCatalog.DISARM)) {
            services.arms().disarm(payload.player());
            return PlayerSleepResult.success(io.github.kxyr0.echoplayersleep.api.session.PlayerSleepOutcome.COMPLETED);
        }
        return dispatchSession(payload, fallbackCommand);
    }

    private PlayerSleepResult dispatchSession(SleepPayload payload, String fallbackCommand) {
        var command = payload.command(fallbackCommand);
        if (actionType.equals(SleepActionCatalog.ENTER_DREAM)) {
            return services.sessions().enterDream(payload.player(), command, payload.destination());
        }
        if (actionType.equals(SleepActionCatalog.RETURN_TO_BED)) {
            return services.sessions().returnToBed(payload.player(), command);
        }
        if (actionType.equals(SleepActionCatalog.COMPLETE_NIGHT)) {
            return services.sessions().completeNight(payload.player(), command);
        }
        if (actionType.equals(SleepActionCatalog.CANCEL)) {
            return services.sessions().cancel(payload.player(), command);
        }
        return terminalOrQuery(payload, command);
    }

    private PlayerSleepResult terminalOrQuery(SleepPayload payload, io.github.kxyr0.echoplayersleep.api.id.PlayerSleepCommandId command) {
        if (actionType.equals(SleepActionCatalog.COMPLETE)) {
            return services.sessions().complete(payload.player(), command);
        }
        boolean state = services.query().active(payload.player())
                .filter(snapshot -> actionType.equals(SleepActionCatalog.IS_ACTIVE)
                        || actionType.equals(SleepActionCatalog.IS_EYES_CLOSED)
                        && snapshot.status() == PlayerSleepStatus.EYES_CLOSED)
                .isPresent() || actionType.equals(SleepActionCatalog.IS_ARMED)
                && services.arms().isArmed(payload.player());
        return PlayerSleepResult.success(state
                ? io.github.kxyr0.echoplayersleep.api.session.PlayerSleepOutcome.COMPLETED
                : io.github.kxyr0.echoplayersleep.api.session.PlayerSleepOutcome.SKIPPED);
    }

    private ExternalActionResultCategory category(PlayerSleepResult result) {
        return result.diagnostics().isEmpty()
                ? ExternalActionResultCategory.SUCCESS
                : ExternalActionResultCategory.FAILURE;
    }

    private void complete(ActionStartRequest request, ActionCompletionSink sink,
                          ExternalActionResultCategory category, Map<String, String> outputs) {
        sink.complete(new ActionCompletion(request.storyId(), request.executionId(),
                request.actionExecutionId(), actionType, category, outputs));
    }
}
