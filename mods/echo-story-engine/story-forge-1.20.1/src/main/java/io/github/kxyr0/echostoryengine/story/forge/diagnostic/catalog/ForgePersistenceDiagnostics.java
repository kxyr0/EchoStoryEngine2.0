package io.github.kxyr0.echostoryengine.story.forge.diagnostic.catalog;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeDiagnosticFactory;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeDiagnosticParts;
import java.time.Instant;
import java.util.Map;

/**
 * Diagnostics emitted by persistence queue and checkpoint coordination.
 */
public final class ForgePersistenceDiagnostics {
    public StoryDiagnostic persistenceQueueRejected(ExecutionId executionId, Instant occurredAt) {
        return ForgeDiagnosticFactory.create(new ForgeDiagnosticParts(
                "ESE-FORGE-PERSISTENCE-001",
                DiagnosticSeverity.ERROR,
                "forge-persistence",
                "Persistence queue is full or closed.",
                new DiagnosticSubject("", executionId.asString(), "", "", executionId.asString(), ""),
                Map.of(),
                "queue-rejected",
                occurredAt,
                "Retry the request after the queue drains or after the server restarts."));
    }

    public StoryDiagnostic persistenceWorkerFailed(String failureCategory, Instant occurredAt) {
        return ForgeDiagnosticFactory.create(new ForgeDiagnosticParts(
                "ESE-FORGE-PERSISTENCE-002",
                DiagnosticSeverity.ERROR,
                "forge-persistence",
                "Background persistence worker failed an operation.",
                emptySubject(),
                Map.of("failureCategory", failureCategory),
                "worker-failed",
                occurredAt,
                "Check storage diagnostics and retry the request."));
    }

    public StoryDiagnostic incompatibleDefinition(StoryExecutionState state, Instant occurredAt) {
        return ForgeDiagnosticFactory.create(new ForgeDiagnosticParts(
                "ESE-FORGE-PERSISTENCE-003",
                DiagnosticSeverity.ERROR,
                "forge-persistence",
                "Checkpoint references a fingerprint absent from the selected StoryResource.",
                DiagnosticSubject.story(state.storyId(), state.executionId(), state.currentNode()),
                Map.of(),
                "definition-fingerprint",
                occurredAt,
                "Restore a compatible StoryResource or start a new execution."));
    }

    public StoryDiagnostic externalCommandSuppressed(StoryExecutionState state, Instant occurredAt) {
        return ForgeDiagnosticFactory.create(new ForgeDiagnosticParts(
                "ESE-FORGE-PERSISTENCE-004",
                DiagnosticSeverity.WARN,
                "forge-persistence",
                "External command was not dispatched because the save failed.",
                DiagnosticSubject.story(state.storyId(), state.executionId(), state.currentNode()),
                Map.of(),
                "dispatch-suppressed",
                occurredAt,
                "Do not execute the action until the checkpoint saves successfully."));
    }

    private DiagnosticSubject emptySubject() {
        return new DiagnosticSubject("", "", "", "", "", "");
    }
}
