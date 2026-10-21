package io.github.kxyr0.echostoryengine.story.forge.execution;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalCommand;
import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import java.util.Objects;

/**
 * Production M4 sink: action is diagnosed, not integrated with gameplay systems.
 */
public final class DiagnosticExternalCommandSink implements ExternalCommandSink {
    private final DiagnosticSink diagnosticSink;
    private final StoryClock clock;
    private final ForgeRuntimeDiagnostics diagnostics;

    public DiagnosticExternalCommandSink(
            DiagnosticSink diagnosticSink,
            StoryClock clock,
            ForgeRuntimeDiagnostics diagnostics) {
        this.diagnosticSink = Objects.requireNonNull(diagnosticSink, "diagnosticSink");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    @Override
    public void dispatch(StoryExecutionState state, ExternalCommand command) {
        diagnosticSink.accept(diagnostics.commandHandlerMissing(
                state.executionId(),
                command.actionType().asString(),
                clock.now()));
    }
}
