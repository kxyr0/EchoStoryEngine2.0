package io.github.kxyr0.echostoryengine.story.core.application;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import java.time.Instant;
import java.util.Map;

/**
 * Diagnostics emitted by the application layer while enforcing persistence ordering.
 */
final class PersistenceRuntimeDiagnostics {
    StoryDiagnostic create(
            String code,
            StoryExecutionState state,
            Instant occurredAt,
            Map<String, String> context) {
        return new StoryDiagnostic(
                code,
                severity(code),
                "persistence",
                message(code),
                DiagnosticSubject.story(state.storyId(), state.executionId(), state.currentNode()),
                context,
                causeCategory(code),
                occurredAt,
                operatorHint(code));
    }

    StoryDiagnostic loadFailed(ExecutionId executionId, Instant occurredAt) {
        return new StoryDiagnostic(
                "ESE-PERSISTENCE-002",
                DiagnosticSeverity.ERROR,
                "persistence",
                message("ESE-PERSISTENCE-002"),
                new DiagnosticSubject("", executionId.asString(), "", "", executionId.asString(), ""),
                Map.of(),
                "load-failed",
                occurredAt,
                operatorHint("ESE-PERSISTENCE-002"));
    }

    private DiagnosticSeverity severity(String code) {
        return switch (code) {
            case "ESE-PERSISTENCE-015" -> DiagnosticSeverity.WARN;
            default -> DiagnosticSeverity.ERROR;
        };
    }

    private String message(String code) {
        return switch (code) {
            case "ESE-PERSISTENCE-001" -> "Execution checkpoint save failed";
            case "ESE-PERSISTENCE-002" -> "Execution checkpoint load failed";
            case "ESE-PERSISTENCE-006" -> "Persisted story definition fingerprint is incompatible";
            case "ESE-PERSISTENCE-010" -> "Execution checkpoint revision conflict";
            case "ESE-PERSISTENCE-015" -> "External command dispatch suppressed";
            default -> "Persistence diagnostic";
        };
    }

    private String causeCategory(String code) {
        return switch (code) {
            case "ESE-PERSISTENCE-001" -> "save-failed";
            case "ESE-PERSISTENCE-002" -> "load-failed";
            case "ESE-PERSISTENCE-006" -> "definition-fingerprint";
            case "ESE-PERSISTENCE-010" -> "revision-conflict";
            case "ESE-PERSISTENCE-015" -> "dispatch-suppressed";
            default -> "persistence";
        };
    }

    private String operatorHint(String code) {
        return switch (code) {
            case "ESE-PERSISTENCE-006" -> "Reload the matching compiled story definition before resuming.";
            case "ESE-PERSISTENCE-015" -> "Retry after persistence succeeds; do not dispatch the command.";
            default -> "Check the persistence adapter result and retry when storage is healthy.";
        };
    }
}
