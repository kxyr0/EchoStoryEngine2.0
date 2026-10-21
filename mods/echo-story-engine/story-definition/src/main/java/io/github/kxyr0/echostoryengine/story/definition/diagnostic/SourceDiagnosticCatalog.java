package io.github.kxyr0.echostoryengine.story.definition.diagnostic;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Stable diagnostic catalog for source parsing, validation, and compilation.
 */
public final class SourceDiagnosticCatalog {
    private static final Map<String, String> MESSAGES = Map.ofEntries(
            Map.entry("ESE-SOURCE-001", "Story source syntax is malformed"),
            Map.entry("ESE-SOURCE-002", "Story source format version is unsupported"),
            Map.entry("ESE-SOURCE-003", "Story source is missing a required field"),
            Map.entry("ESE-SOURCE-004", "Story source identifier has invalid format"),
            Map.entry("ESE-SOURCE-005", "Story source declares a duplicate node id"),
            Map.entry("ESE-SOURCE-006", "Story source start node does not exist"),
            Map.entry("ESE-SOURCE-007", "Story source transition target does not exist"),
            Map.entry("ESE-SOURCE-008", "Story source contains an unreachable node"),
            Map.entry("ESE-SOURCE-009", "Story source contains a synchronous cycle"),
            Map.entry("ESE-SOURCE-010", "Story source uses an unsupported node type"),
            Map.entry("ESE-SOURCE-011", "Story source could not be compiled"),
            Map.entry("ESE-SOURCE-012", "Story source field is not valid for this node"),
            Map.entry("ESE-SOURCE-013", "Story source has no reachable finish node"),
            Map.entry("ESE-SOURCE-014", "Story source uses an unsupported resource profile"),
            Map.entry("ESE-SOURCE-015", "Story source is missing a resource class"),
            Map.entry("ESE-SOURCE-016", "Story source declares multiple resource classes"),
            Map.entry("ESE-SOURCE-017", "Story source is missing the Build method"),
            Map.entry("ESE-SOURCE-018", "Story source has an invalid Build signature"),
            Map.entry("ESE-SOURCE-019", "Story source declares an unsupported member"),
            Map.entry("ESE-SOURCE-020", "Story source uses an unknown type"),
            Map.entry("ESE-SOURCE-021", "Story source uses an unknown intrinsic"),
            Map.entry("ESE-SOURCE-022", "Story source intrinsic has an invalid argument count"),
            Map.entry("ESE-SOURCE-023", "Story source intrinsic has an unknown named argument"),
            Map.entry("ESE-SOURCE-024", "Story source intrinsic has an invalid argument type"),
            Map.entry("ESE-SOURCE-025", "Story source is missing a required lifecycle method"));
    private static final Map<String, String> CAUSES = Map.ofEntries(
            Map.entry("ESE-SOURCE-001", "source-syntax"),
            Map.entry("ESE-SOURCE-002", "source-version"),
            Map.entry("ESE-SOURCE-003", "missing-required-field"),
            Map.entry("ESE-SOURCE-004", "invalid-identifier"),
            Map.entry("ESE-SOURCE-005", "duplicate-node"),
            Map.entry("ESE-SOURCE-006", "missing-start-node"),
            Map.entry("ESE-SOURCE-007", "missing-transition-target"),
            Map.entry("ESE-SOURCE-008", "unreachable-node"),
            Map.entry("ESE-SOURCE-009", "synchronous-cycle"),
            Map.entry("ESE-SOURCE-010", "unsupported-node-type"),
            Map.entry("ESE-SOURCE-011", "compilation-invariant"),
            Map.entry("ESE-SOURCE-012", "invalid-source-field"),
            Map.entry("ESE-SOURCE-013", "missing-reachable-finish"),
            Map.entry("ESE-SOURCE-014", "unsupported-resource-profile"),
            Map.entry("ESE-SOURCE-015", "missing-resource-class"),
            Map.entry("ESE-SOURCE-016", "duplicate-resource-class"),
            Map.entry("ESE-SOURCE-017", "missing-build-method"),
            Map.entry("ESE-SOURCE-018", "invalid-build-signature"),
            Map.entry("ESE-SOURCE-019", "unsupported-member"),
            Map.entry("ESE-SOURCE-020", "unknown-type"),
            Map.entry("ESE-SOURCE-021", "unknown-intrinsic"),
            Map.entry("ESE-SOURCE-022", "invalid-argument-count"),
            Map.entry("ESE-SOURCE-023", "unknown-named-argument"),
            Map.entry("ESE-SOURCE-024", "invalid-argument-type"),
            Map.entry("ESE-SOURCE-025", "missing-lifecycle-method"));
    private static final Map<String, String> HINTS = Map.of(
            "ESE-SOURCE-001", "Fix the source syntax before validation.",
            "ESE-SOURCE-002", "Use a supported story source format version.",
            "ESE-SOURCE-009", "Insert an external wait or remove the synchronous cycle.");

    public StoryDiagnostic create(String code, Instant occurredAt, SourceDiagnosticContext context) {
        Objects.requireNonNull(context, "context");
        return new StoryDiagnostic(
                code,
                severity(code),
                "source",
                message(code),
                context.subject(),
                context.boundedContext(),
                causeCategory(code),
                Objects.requireNonNull(occurredAt, "occurredAt"),
                operatorHint(code));
    }

    private DiagnosticSeverity severity(String code) {
        return "ESE-SOURCE-008".equals(code) ? DiagnosticSeverity.WARN : DiagnosticSeverity.ERROR;
    }

    private String message(String code) {
        return MESSAGES.getOrDefault(code, "Story source diagnostic");
    }

    private String causeCategory(String code) {
        return CAUSES.getOrDefault(code, "source");
    }

    private String operatorHint(String code) {
        return HINTS.getOrDefault(code, "");
    }
}
