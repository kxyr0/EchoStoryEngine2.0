package io.github.kxyr0.echostoryengine.story.core.port.persistence;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit load result for the execution state repository port.
 */
public record ExecutionLoadResult(
        ExecutionLoadStatus status,
        Optional<ExecutionCheckpoint> checkpoint,
        List<StoryDiagnostic> diagnostics) {
    public ExecutionLoadResult {
        status = Objects.requireNonNull(status, "status");
        checkpoint = Objects.requireNonNull(checkpoint, "checkpoint");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    /**
     * Returns a successful load.
     */
    public static ExecutionLoadResult found(
            ExecutionCheckpoint checkpoint,
            List<StoryDiagnostic> diagnostics) {
        return new ExecutionLoadResult(ExecutionLoadStatus.FOUND, Optional.of(checkpoint), diagnostics);
    }

    /**
     * Returns an expected absence.
     */
    public static ExecutionLoadResult notFound(List<StoryDiagnostic> diagnostics) {
        return new ExecutionLoadResult(ExecutionLoadStatus.NOT_FOUND, Optional.empty(), diagnostics);
    }

    /**
     * Returns a failed load.
     */
    public static ExecutionLoadResult failed(List<StoryDiagnostic> diagnostics) {
        return new ExecutionLoadResult(ExecutionLoadStatus.FAILED, Optional.empty(), diagnostics);
    }
}
