package io.github.kxyr0.echostoryengine.story.core.domain;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import java.time.Instant;
import java.util.Map;

/**
 * Structured diagnostic catalog for deterministic runtime transitions.
 */
final class RuntimeDiagnostics {
    StoryDiagnostic create(
            String code,
            StoryExecutionState state,
            Instant occurredAt,
            Map<String, String> context) {
        return new StoryDiagnostic(
                code,
                severity(code),
                "runtime",
                message(code),
                DiagnosticSubject.story(state.storyId(), state.executionId(), state.currentNode()),
                context,
                causeCategory(code),
                occurredAt,
                operatorHint(code));
    }

    private DiagnosticSeverity severity(String code) {
        return switch (code) {
            case "ESE-ACTION-002", "ESE-RUNTIME-005" -> DiagnosticSeverity.INFO;
            case "ESE-ACTION-003", "ESE-ACTION-004" -> DiagnosticSeverity.WARN;
            default -> DiagnosticSeverity.ERROR;
        };
    }

    private String message(String code) {
        if (code.startsWith("ESE-ACTION-")) {
            return actionMessage(code);
        }
        return runtimeMessage(code);
    }

    private String actionMessage(String code) {
        return switch (code) {
            case "ESE-ACTION-001" -> "External action type is not supported";
            case "ESE-ACTION-002" -> "External action requested";
            case "ESE-ACTION-003" -> "Duplicate external action completion ignored";
            case "ESE-ACTION-004" -> "External action completion does not match the pending action";
            case "ESE-ACTION-005" -> "External action completed without success";
            case "ESE-ACTION-006" -> "External action completion is missing a required output";
            default -> "Runtime diagnostic";
        };
    }

    private String runtimeMessage(String code) {
        return switch (code) {
            case "ESE-RUNTIME-002" -> "Transition budget exceeded";
            case "ESE-RUNTIME-003" -> "Story condition evaluated to false";
            case "ESE-RUNTIME-004" -> "Story definition is missing the current node";
            case "ESE-RUNTIME-005" -> "Story execution finished";
            default -> "Runtime diagnostic";
        };
    }

    private String causeCategory(String code) {
        if (code.startsWith("ESE-ACTION-")) {
            return actionCauseCategory(code);
        }
        return runtimeCauseCategory(code);
    }

    private String actionCauseCategory(String code) {
        return switch (code) {
            case "ESE-ACTION-001" -> "missing-action-support";
            case "ESE-ACTION-002" -> "external-action-request";
            case "ESE-ACTION-003" -> "duplicate-action-completion";
            case "ESE-ACTION-004" -> "unknown-action-completion";
            case "ESE-ACTION-005" -> "external-action-result";
            case "ESE-ACTION-006" -> "external-action-output";
            default -> "runtime";
        };
    }

    private String runtimeCauseCategory(String code) {
        return switch (code) {
            case "ESE-RUNTIME-002" -> "transition-budget";
            case "ESE-RUNTIME-003" -> "condition-failed";
            case "ESE-RUNTIME-004" -> "missing-node";
            case "ESE-RUNTIME-005" -> "story-finished";
            default -> "runtime";
        };
    }

    private String operatorHint(String code) {
        return switch (code) {
            case "ESE-ACTION-001" -> "Register support for this action type before running the story.";
            case "ESE-ACTION-006" -> "Check the adapter completion output schema for this external action.";
            case "ESE-RUNTIME-002" -> "Check the story definition for a synchronous loop.";
            case "ESE-RUNTIME-004" -> "Check the programmatic story definition.";
            default -> "";
        };
    }
}
