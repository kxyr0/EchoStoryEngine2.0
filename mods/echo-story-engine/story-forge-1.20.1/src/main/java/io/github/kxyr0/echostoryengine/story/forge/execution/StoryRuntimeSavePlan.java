package io.github.kxyr0.echostoryengine.story.forge.execution;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeInput;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeResult;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionCheckpoint;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.CompiledStoryCatalogEntry;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Server-thread transition result waiting for durable save.
 */
public record StoryRuntimeSavePlan(
        StoryRuntimeInput input,
        CompiledStoryCatalogEntry catalogEntry,
        Optional<ExecutionCheckpoint> checkpoint,
        int expectedRevision,
        Optional<StoryRuntimeResult> transition,
        StoryExecutionState stateBeforeTransition,
        List<StoryDiagnostic> diagnostics) {
    public StoryRuntimeSavePlan {
        input = Objects.requireNonNull(input, "input");
        catalogEntry = Objects.requireNonNull(catalogEntry, "catalogEntry");
        checkpoint = Objects.requireNonNull(checkpoint, "checkpoint");
        transition = Objects.requireNonNull(transition, "transition");
        stateBeforeTransition = Objects.requireNonNull(stateBeforeTransition, "stateBeforeTransition");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public boolean requiresSave() {
        return checkpoint.isPresent() && transition.isPresent();
    }
}
