package io.github.kxyr0.echostoryengine.story.core.application;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeInput;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeResult;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionCheckpoint;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionLoadResult;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionSaveResult;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionSaveStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class RuntimePersistenceResults {
    private final DiagnosticSink diagnosticSink;
    private final PersistenceRuntimeDiagnostics persistenceDiagnostics = new PersistenceRuntimeDiagnostics();

    RuntimePersistenceResults(DiagnosticSink diagnosticSink) {
        this.diagnosticSink = diagnosticSink;
    }

    StoryRuntimeResult success(
            StoryRuntimeInput input,
            StoryRuntimeResult transition,
            List<StoryDiagnostic> loadDiagnostics,
            List<StoryDiagnostic> saveDiagnostics) {
        List<StoryDiagnostic> diagnostics = combined(loadDiagnostics, transition.diagnostics(), saveDiagnostics);
        diagnostics.forEach(diagnosticSink::accept);
        return new StoryRuntimeResult(input.executionId(), transition.state(), transition.commands(), diagnostics);
    }

    StoryRuntimeResult saveSuppressed(SaveFailure failure) {
        List<StoryDiagnostic> diagnostics = new ArrayList<>(failure.loadDiagnostics());
        diagnostics.addAll(failure.saveResult().diagnostics());
        addSaveFailureDiagnostics(failure, diagnostics);
        diagnostics.forEach(diagnosticSink::accept);
        return new StoryRuntimeResult(failure.input().executionId(), failure.current(), List.of(), diagnostics);
    }

    StoryRuntimeResult loadFailed(StoryRuntimeInput input, ExecutionLoadResult loaded, Instant occurredAt) {
        StoryExecutionState failed = StoryExecutionState.start(input.storyId(), input.executionId())
                .failedAt(NodeId.root(), "ESE-PERSISTENCE-002");
        List<StoryDiagnostic> diagnostics = new ArrayList<>(loaded.diagnostics());
        if (diagnostics.isEmpty()) {
            diagnostics.add(persistenceDiagnostics.loadFailed(input.executionId(), occurredAt));
        }
        diagnostics.forEach(diagnosticSink::accept);
        return new StoryRuntimeResult(input.executionId(), failed, List.of(), diagnostics);
    }

    StoryRuntimeResult incompatible(
            StoryRuntimeInput input,
            ExecutionCheckpoint checkpoint,
            List<StoryDiagnostic> loadDiagnostics,
            Instant occurredAt) {
        StoryDiagnostic diagnostic = persistenceDiagnostics.create(
                "ESE-PERSISTENCE-006",
                checkpoint.state(),
                occurredAt,
                Map.of());
        List<StoryDiagnostic> diagnostics = combined(loadDiagnostics, List.of(diagnostic), List.of());
        diagnostics.forEach(diagnosticSink::accept);
        return new StoryRuntimeResult(input.executionId(), checkpoint.state(), List.of(), diagnostics);
    }

    private void addSaveFailureDiagnostics(SaveFailure failure, List<StoryDiagnostic> diagnostics) {
        if (failure.saveResult().status() == ExecutionSaveStatus.REVISION_CONFLICT) {
            diagnostics.add(persistenceDiagnostics.create(
                    "ESE-PERSISTENCE-010",
                    failure.current(),
                    failure.occurredAt(),
                    Map.of()));
        }
        if (failure.saveResult().status() == ExecutionSaveStatus.FAILED
                && failure.saveResult().diagnostics().isEmpty()) {
            diagnostics.add(persistenceDiagnostics.create(
                    "ESE-PERSISTENCE-001",
                    failure.current(),
                    failure.occurredAt(),
                    Map.of()));
        }
        if (!failure.transition().commands().isEmpty()) {
            diagnostics.add(persistenceDiagnostics.create(
                    "ESE-PERSISTENCE-015",
                    failure.current(),
                    failure.occurredAt(),
                    Map.of()));
        }
    }

    private List<StoryDiagnostic> combined(
            List<StoryDiagnostic> first,
            List<StoryDiagnostic> second,
            List<StoryDiagnostic> third) {
        List<StoryDiagnostic> diagnostics = new ArrayList<>(first);
        diagnostics.addAll(second);
        diagnostics.addAll(third);
        return List.copyOf(diagnostics);
    }

    record SaveFailure(
            StoryRuntimeInput input,
            StoryExecutionState current,
            StoryRuntimeResult transition,
            ExecutionSaveResult saveResult,
            List<StoryDiagnostic> loadDiagnostics,
            Instant occurredAt) {
    }
}
