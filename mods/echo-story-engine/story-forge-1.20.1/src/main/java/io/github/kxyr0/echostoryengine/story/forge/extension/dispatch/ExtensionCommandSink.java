package io.github.kxyr0.echostoryengine.story.forge.extension.dispatch;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalCommand;
import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionAcceptance;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionRecoveryRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationStatus;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.execution.ExternalCommandSink;
import io.github.kxyr0.echostoryengine.story.forge.extension.catalog.ExtensionActionBinding;
import io.github.kxyr0.echostoryengine.story.forge.extension.catalog.ExtensionCatalog;
import java.util.Map;
import java.util.Objects;

/**
 * Dispatches saved external commands to active extension handlers.
 */
public final class ExtensionCommandSink implements ExternalCommandSink {
    private final ExtensionCatalog catalog;
    private final ExtensionCompletionQueue completionQueue;
    private final DiagnosticSink diagnosticSink;
    private final StoryClock clock;
    private final ForgeRuntimeDiagnostics diagnostics;

    public ExtensionCommandSink(
            ExtensionCatalog catalog,
            ExtensionCompletionQueue completionQueue,
            DiagnosticSink diagnosticSink,
            StoryClock clock,
            ForgeRuntimeDiagnostics diagnostics) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.completionQueue = Objects.requireNonNull(completionQueue, "completionQueue");
        this.diagnosticSink = Objects.requireNonNull(diagnosticSink, "diagnosticSink");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    @Override
    public void dispatch(StoryExecutionState state, ExternalCommand command) {
        completionQueue.track(state, command);
        catalog.find(command.actionType()).ifPresentOrElse(
                binding -> start(state, command, binding),
                () -> failMissingStart(state, command));
    }

    @Override
    public void recover(StoryExecutionState state, ExternalCommand command) {
        catalog.find(command.actionType()).ifPresentOrElse(
                binding -> {
                    completionQueue.track(state, command);
                    recover(state, command, binding);
                },
                () -> diagnosticSink.accept(diagnostics.extensionRecoveryBlocked(
                        state.executionId(), command.actionType().asString(), clock.now())));
    }

    private void start(
            StoryExecutionState state,
            ExternalCommand command,
            ExtensionActionBinding binding) {
        try {
            ActionInvocationResult result = binding.handler().start(startRequest(state, command),
                    completionQueue.sink());
            if (result.status() != ActionInvocationStatus.ACCEPTED) {
                diagnosticSink.accept(diagnostics.extensionStartRejected(state.executionId(),
                        command.actionType().asString(), result.status().name(), clock.now()));
                enqueueFailure(state, command);
            }
        } catch (RuntimeException exception) {
            diagnosticSink.accept(diagnostics.extensionStartFailed(state.executionId(), command.actionType().asString(),
                    exception.getClass().getSimpleName(), clock.now()));
            enqueueFailure(state, command);
        }
    }

    private void recover(
            StoryExecutionState state,
            ExternalCommand command,
            ExtensionActionBinding binding) {
        if (!binding.capabilities().supportsRecovery()) {
            diagnosticSink.accept(diagnostics.extensionRecoveryRejected(state.executionId(),
                    command.actionType().asString(), "UNSUPPORTED", clock.now()));
            return;
        }
        try {
            ActionInvocationResult result = binding.handler().recover(recoveryRequest(state, command),
                    completionQueue.sink());
            if (result.status() != ActionInvocationStatus.ACCEPTED) {
                diagnosticSink.accept(diagnostics.extensionRecoveryRejected(state.executionId(),
                        command.actionType().asString(), result.status().name(), clock.now()));
            }
        } catch (RuntimeException exception) {
            diagnosticSink.accept(diagnostics.extensionRecoveryRejected(state.executionId(),
                    command.actionType().asString(), exception.getClass().getSimpleName(), clock.now()));
        }
    }

    private void failMissingStart(StoryExecutionState state, ExternalCommand command) {
        diagnosticSink.accept(diagnostics.commandHandlerMissing(state.executionId(), command.actionType().asString(),
                clock.now()));
        enqueueFailure(state, command);
    }

    private void enqueueFailure(StoryExecutionState state, ExternalCommand command) {
        ActionCompletionAcceptance accepted = completionQueue.sink().complete(new ActionCompletion(
                state.storyId(),
                state.executionId(),
                command.actionExecutionId(),
                command.actionType(),
                ExternalActionResultCategory.FAILURE,
                Map.of()));
        if (accepted != ActionCompletionAcceptance.ACCEPTED) {
            diagnosticSink.accept(diagnostics.extensionCompletionRejected(state.executionId(),
                    command.actionType().asString(), accepted.name(), clock.now()));
        }
    }

    private ActionStartRequest startRequest(StoryExecutionState state, ExternalCommand command) {
        return new ActionStartRequest(state.storyId(), state.executionId(), command.actionExecutionId(),
                command.actionType(), command.payload());
    }

    private ActionRecoveryRequest recoveryRequest(StoryExecutionState state, ExternalCommand command) {
        return new ActionRecoveryRequest(state.storyId(), state.executionId(), command.actionExecutionId(),
                command.actionType(), command.payload());
    }
}
