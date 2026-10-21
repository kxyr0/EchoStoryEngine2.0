package io.github.kxyr0.echostoryengine.story.persistence.diagnostic;

import static java.util.Map.entry;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Structured persistence diagnostic catalog.
 */
public final class PersistenceDiagnosticCatalog {
    private static final Map<String, String> MESSAGES = Map.ofEntries(
            entry("ESE-PERSISTENCE-001", "Execution checkpoint save failed"),
            entry("ESE-PERSISTENCE-002", "Execution checkpoint load failed"),
            entry("ESE-PERSISTENCE-003", "Execution checkpoint was not found"),
            entry("ESE-PERSISTENCE-004", "Execution checkpoint is malformed"),
            entry("ESE-PERSISTENCE-005", "Execution checkpoint schema version is unsupported"),
            entry("ESE-PERSISTENCE-006", "Story definition fingerprint is incompatible"),
            entry("ESE-PERSISTENCE-007", "Story definition is missing"),
            entry("ESE-PERSISTENCE-008", "Current story node is missing"),
            entry("ESE-PERSISTENCE-009", "Pending action checkpoint is invalid"),
            entry("ESE-PERSISTENCE-010", "Execution checkpoint revision conflict"),
            entry("ESE-PERSISTENCE-011", "Execution checkpoint backup was used"),
            entry("ESE-PERSISTENCE-012", "Primary and backup checkpoints are corrupt"),
            entry("ESE-PERSISTENCE-013", "Recovery request created"),
            entry("ESE-PERSISTENCE-014", "RUNNING checkpoint rejected during recovery"),
            entry("ESE-PERSISTENCE-015", "External command dispatch suppressed"));
    private static final Map<String, String> CAUSES = Map.ofEntries(
            entry("ESE-PERSISTENCE-001", "save-failed"),
            entry("ESE-PERSISTENCE-002", "load-failed"),
            entry("ESE-PERSISTENCE-003", "not-found"),
            entry("ESE-PERSISTENCE-004", "malformed"),
            entry("ESE-PERSISTENCE-005", "unsupported-schema"),
            entry("ESE-PERSISTENCE-006", "definition-fingerprint"),
            entry("ESE-PERSISTENCE-007", "missing-definition"),
            entry("ESE-PERSISTENCE-008", "missing-current-node"),
            entry("ESE-PERSISTENCE-009", "invalid-pending-action"),
            entry("ESE-PERSISTENCE-010", "revision-conflict"),
            entry("ESE-PERSISTENCE-011", "backup-fallback"),
            entry("ESE-PERSISTENCE-012", "both-corrupt"),
            entry("ESE-PERSISTENCE-013", "recovery-request"),
            entry("ESE-PERSISTENCE-014", "running-recovery"),
            entry("ESE-PERSISTENCE-015", "dispatch-suppressed"));

    /**
     * Creates a diagnostic for a known execution id without assuming a loaded state.
     */
    public StoryDiagnostic create(
            String code,
            ExecutionId executionId,
            Instant occurredAt,
            Map<String, String> context) {
        return diagnostic(
                code,
                subject(Objects.requireNonNull(executionId, "executionId")),
                occurredAt,
                context);
    }

    /**
     * Creates a diagnostic for a loaded execution state.
     */
    public StoryDiagnostic create(
            String code,
            StoryExecutionState state,
            Instant occurredAt,
            Map<String, String> context) {
        Objects.requireNonNull(state, "state");
        return diagnostic(
                code,
                DiagnosticSubject.story(state.storyId(), state.executionId(), state.currentNode()),
                occurredAt,
                context);
    }

    private StoryDiagnostic diagnostic(
            String code,
            DiagnosticSubject subject,
            Instant occurredAt,
            Map<String, String> context) {
        return new StoryDiagnostic(
                code,
                severity(code),
                "persistence",
                message(code),
                subject,
                Map.copyOf(Objects.requireNonNull(context, "context")),
                causeCategory(code),
                Objects.requireNonNull(occurredAt, "occurredAt"),
                operatorHint(code));
    }

    private DiagnosticSubject subject(ExecutionId executionId) {
        return new DiagnosticSubject("", executionId.asString(), "", "", executionId.asString(), "");
    }

    private DiagnosticSeverity severity(String code) {
        return switch (code) {
            case "ESE-PERSISTENCE-003", "ESE-PERSISTENCE-013" -> DiagnosticSeverity.INFO;
            case "ESE-PERSISTENCE-011", "ESE-PERSISTENCE-015" -> DiagnosticSeverity.WARN;
            default -> DiagnosticSeverity.ERROR;
        };
    }

    private String message(String code) {
        return MESSAGES.getOrDefault(code, "Persistence diagnostic");
    }

    private String causeCategory(String code) {
        return CAUSES.getOrDefault(code, "persistence");
    }

    private String operatorHint(String code) {
        return switch (code) {
            case "ESE-PERSISTENCE-003" -> "Start a new execution or check the execution id.";
            case "ESE-PERSISTENCE-011" -> "Inspect storage and replace the corrupt primary checkpoint.";
            case "ESE-PERSISTENCE-013" -> "Dispatch the recovered command idempotently.";
            case "ESE-PERSISTENCE-015" -> "Do not dispatch the command until persistence succeeds.";
            default -> "Check the persisted checkpoint and retry with a compatible story definition.";
        };
    }
}
