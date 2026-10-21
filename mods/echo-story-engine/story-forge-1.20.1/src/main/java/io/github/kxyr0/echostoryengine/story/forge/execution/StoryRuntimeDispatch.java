package io.github.kxyr0.echostoryengine.story.forge.execution;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalCommand;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.CompiledStoryCatalogEntry;
import java.util.List;
import java.util.Objects;

/**
 * Commands and diagnostics that may be exposed after save completion.
 */
public record StoryRuntimeDispatch(
        CompiledStoryCatalogEntry catalogEntry,
        StoryExecutionState state,
        List<ExternalCommand> commands,
        List<StoryDiagnostic> diagnostics,
        boolean saved) {
    public StoryRuntimeDispatch {
        catalogEntry = Objects.requireNonNull(catalogEntry, "catalogEntry");
        state = Objects.requireNonNull(state, "state");
        commands = List.copyOf(Objects.requireNonNull(commands, "commands"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }
}
