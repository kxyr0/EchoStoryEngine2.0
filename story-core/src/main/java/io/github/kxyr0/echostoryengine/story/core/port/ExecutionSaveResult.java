package io.github.kxyr0.echostoryengine.story.core.port;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import java.util.List;
import java.util.Objects;

/**
 * Explicit save result for the execution state repository port.
 */
public record ExecutionSaveResult(ExecutionSaveStatus status, List<StoryDiagnostic> diagnostics) {
    public ExecutionSaveResult {
        status = Objects.requireNonNull(status, "status");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    /**
     * Returns a successful save.
     */
    public static ExecutionSaveResult saved(List<StoryDiagnostic> diagnostics) {
        return new ExecutionSaveResult(ExecutionSaveStatus.SAVED, diagnostics);
    }

    /**
     * Returns an optimistic revision conflict.
     */
    public static ExecutionSaveResult revisionConflict(List<StoryDiagnostic> diagnostics) {
        return new ExecutionSaveResult(ExecutionSaveStatus.REVISION_CONFLICT, diagnostics);
    }

    /**
     * Returns a failed save.
     */
    public static ExecutionSaveResult failed(List<StoryDiagnostic> diagnostics) {
        return new ExecutionSaveResult(ExecutionSaveStatus.FAILED, diagnostics);
    }
}
