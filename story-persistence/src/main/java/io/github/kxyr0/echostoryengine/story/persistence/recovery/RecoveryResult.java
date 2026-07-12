package io.github.kxyr0.echostoryengine.story.persistence.recovery;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalCommand;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import java.util.List;
import java.util.Objects;

/**
 * Result of validating and recovering a durable checkpoint.
 */
public record RecoveryResult(
        RecoveryStatus status,
        StoryExecutionState state,
        List<ExternalCommand> commands,
        List<StoryDiagnostic> diagnostics) {
    public RecoveryResult {
        status = Objects.requireNonNull(status, "status");
        state = Objects.requireNonNull(state, "state");
        commands = List.copyOf(Objects.requireNonNull(commands, "commands"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    static RecoveryResult of(
            RecoveryStatus status,
            StoryExecutionState state,
            List<ExternalCommand> commands,
            List<StoryDiagnostic> diagnostics) {
        return new RecoveryResult(status, state, commands, diagnostics);
    }
}
