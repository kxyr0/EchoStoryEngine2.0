package io.github.kxyr0.echostoryengine.story.forge.environment.runtime;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalCommand;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import java.time.Instant;
import java.util.Map;

final class ForgeEnvironmentRuntimeDiagnostics {
    StoryDiagnostic setFailed(
            StoryExecutionState state,
            ExternalCommand command,
            String message,
            Instant occurredAt) {
        return new StoryDiagnostic(
                "ESE-FORGE-ENV-101",
                DiagnosticSeverity.ERROR,
                "forge-environment",
                message,
                DiagnosticSubject.story(state.storyId(), state.executionId(), state.currentNode()),
                Map.of("actionType", command.actionType().asString()),
                "environment-set-failed",
                occurredAt,
                "Fix the EnvironmentResource or the Environment.Set arguments.");
    }
}
