package io.github.kxyr0.echoplayersleep.script;

import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepDiagnostic;
import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepResult;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepCommandId;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepFailure;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepOutcome;
import io.github.kxyr0.echoplayersleep.forge.api.PlayerSleepServiceBridge;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import java.util.Map;
import java.util.Objects;

final class SleepBlackspaceActionFlow {
    private final NamespacedId actionType;
    private final PlayerSleepServiceBridge services;

    SleepBlackspaceActionFlow(NamespacedId actionType, PlayerSleepServiceBridge services) {
        this.actionType = Objects.requireNonNull(actionType, "actionType");
        this.services = Objects.requireNonNull(services, "services");
    }

    ActionInvocationResult enterDream(
            ActionStartRequest request,
            ActionCompletionSink sink,
            SleepPayload payload,
            PlayerSleepCommandId command) {
        String instanceId = payload.blackspaceInstanceId().orElseThrow();
        try {
            var target = SleepBlackspaceBridge.find(payload.player(), instanceId);
            if (target.isEmpty()) {
                PlayerSleepResult failure = failure(PlayerSleepOutcome.DREAM_DESTINATION_INVALID,
                        PlayerSleepFailure.PLAYER_SLEEP_DREAM_ENTRY_FAILED, instanceId,
                        "Blackspace instance is not ready or echo-blackspace is not loaded.");
                complete(request, sink, failure);
                return ActionInvocationResult.accepted();
            }
            PlayerSleepResult begin = services.sessions().beginBlackspaceDream(
                    payload.player(), command, instanceId);
            if (!begin.diagnostics().isEmpty()) {
                complete(request, sink, begin);
                return ActionInvocationResult.accepted();
            }
            target.orElseThrow().enter(() -> completeDream(request, sink, payload, command));
            return ActionInvocationResult.accepted();
        } catch (RuntimeException exception) {
            services.sessions().cancel(payload.player(), command);
            complete(request, sink, ExternalActionResultCategory.FAILURE,
                    Map.of("outcome", PlayerSleepOutcome.INTERNAL_FAILURE.name()));
            return ActionInvocationResult.accepted();
        }
    }

    ActionInvocationResult returnToBed(
            ActionStartRequest request,
            ActionCompletionSink sink,
            SleepPayload payload,
            PlayerSleepCommandId command) {
        try {
            PlayerSleepResult begin = services.sessions().beginBlackspaceReturn(payload.player(), command);
            if (!begin.diagnostics().isEmpty()) {
                complete(request, sink, begin);
                return ActionInvocationResult.accepted();
            }
            if (!SleepBlackspaceBridge.exit(payload.player(),
                    () -> completeReturn(request, sink, payload, command))) {
                complete(request, sink, failure(PlayerSleepOutcome.INTERNAL_FAILURE,
                        PlayerSleepFailure.PLAYER_SLEEP_RETURN_FAILED, payload.player().uuid().toString(),
                        "Blackspace session is not active."));
            }
            return ActionInvocationResult.accepted();
        } catch (RuntimeException exception) {
            services.sessions().cancel(payload.player(), command);
            complete(request, sink, ExternalActionResultCategory.FAILURE,
                    Map.of("outcome", PlayerSleepOutcome.INTERNAL_FAILURE.name()));
            return ActionInvocationResult.accepted();
        }
    }

    private void completeDream(
            ActionStartRequest request,
            ActionCompletionSink sink,
            SleepPayload payload,
            PlayerSleepCommandId command) {
        complete(request, sink, services.sessions().completeBlackspaceDream(payload.player(), command));
    }

    private void completeReturn(
            ActionStartRequest request,
            ActionCompletionSink sink,
            SleepPayload payload,
            PlayerSleepCommandId command) {
        complete(request, sink, services.sessions().completeBlackspaceReturn(payload.player(), command));
    }

    private PlayerSleepResult failure(
            PlayerSleepOutcome outcome,
            PlayerSleepFailure code,
            String subject,
            String message) {
        return PlayerSleepResult.failure(outcome, PlayerSleepDiagnostic.error(code, subject, message));
    }

    private void complete(ActionStartRequest request, ActionCompletionSink sink, PlayerSleepResult result) {
        complete(request, sink, category(result), Map.of("outcome", result.outcome().name()));
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
