package io.github.kxyr0.echostoryengine.story.forge.diagnostic.catalog;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeDiagnosticFactory;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeDiagnosticParts;
import java.time.Instant;
import java.util.Map;

/**
 * Diagnostics emitted by the server-scoped Forge story host.
 */
public final class ForgeHostDiagnostics {
    public StoryDiagnostic hostStarted(Instant occurredAt) {
        return ForgeDiagnosticFactory.create(new ForgeDiagnosticParts(
                "ESE-FORGE-HOST-001",
                DiagnosticSeverity.INFO,
                "forge-host",
                "Forge story host started for the active server.",
                emptySubject(),
                Map.of("storageRoot", "world/data/echo_story_engine/executions"),
                "host-started",
                occurredAt,
                "Story runtime is ready to accept server-side requests."));
    }

    public StoryDiagnostic hostStopped(Instant occurredAt) {
        return ForgeDiagnosticFactory.create(new ForgeDiagnosticParts(
                "ESE-FORGE-HOST-002",
                DiagnosticSeverity.INFO,
                "forge-host",
                "Forge story host stopped.",
                emptySubject(),
                Map.of(),
                "host-stopped",
                occurredAt,
                "No operator action required."));
    }

    public StoryDiagnostic missingStory(StoryId storyId, ExecutionId executionId, Instant occurredAt) {
        return ForgeDiagnosticFactory.create(new ForgeDiagnosticParts(
                "ESE-FORGE-HOST-003",
                DiagnosticSeverity.ERROR,
                "forge-host",
                "Story was not found in the active catalog.",
                new DiagnosticSubject(storyId.asString(), executionId.asString(), "", "", executionId.asString(), ""),
                Map.of(),
                "story-not-found",
                occurredAt,
                "Check data/<namespace>/echo_story/stories and run reload."));
    }

    private DiagnosticSubject emptySubject() {
        return new DiagnosticSubject("", "", "", "", "", "");
    }
}
