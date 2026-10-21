package io.github.kxyr0.echostoryengine.story.api;

import io.github.kxyr0.echostoryengine.story.api.command.ExternalCommand;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import java.util.List;
import java.util.Objects;

/**
 * Immutable result of one runtime transition.
 *
 * <p>Commands are abstract external commands, not game actions. Diagnostics are structured facts that a platform
 * adapter can log or expose through diagnostics UI without creating a second logging pipeline.
 */
public record StoryRuntimeResult(
        ExecutionId executionId,
        StoryExecutionState state,
        List<ExternalCommand> commands,
        List<StoryDiagnostic> diagnostics) {
    public StoryRuntimeResult {
        executionId = Objects.requireNonNull(executionId, "executionId");
        state = Objects.requireNonNull(state, "state");
        commands = List.copyOf(Objects.requireNonNull(commands, "commands"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }
}
