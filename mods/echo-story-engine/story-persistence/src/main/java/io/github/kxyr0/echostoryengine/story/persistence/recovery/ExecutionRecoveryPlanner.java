package io.github.kxyr0.echostoryengine.story.persistence.recovery;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalCommand;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.CompiledStoryFingerprint;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinitionFingerprint;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryNode;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryNodeKind;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionCheckpoint;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.PendingActionCheckpoint;
import io.github.kxyr0.echostoryengine.story.persistence.diagnostic.PersistenceDiagnosticCatalog;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Validates durable checkpoints and creates idempotent recovery requests.
 */
public final class ExecutionRecoveryPlanner {
    private final PersistenceDiagnosticCatalog diagnostics = new PersistenceDiagnosticCatalog();

    /**
     * Recovers one loaded checkpoint against a compatible compiled story catalog.
     */
    public RecoveryResult recover(
            ExecutionCheckpoint checkpoint,
            CompiledStoryCatalog catalog,
            Instant occurredAt) {
        Objects.requireNonNull(checkpoint, "checkpoint");
        Objects.requireNonNull(catalog, "catalog");
        StoryExecutionState state = checkpoint.state();
        Optional<StoryDefinition> definition = catalog.find(state.storyId());
        if (definition.isEmpty()) {
            return rejected(state, "ESE-PERSISTENCE-007", occurredAt);
        }
        if (!compatible(checkpoint, definition.orElseThrow())) {
            return rejected(state, "ESE-PERSISTENCE-006", occurredAt);
        }
        Optional<StoryNode> node = definition.orElseThrow().find(state.currentNode());
        if (node.isEmpty()) {
            return rejected(state, "ESE-PERSISTENCE-008", occurredAt);
        }
        return recoverPhase(checkpoint, node.orElseThrow(), occurredAt);
    }

    private boolean compatible(ExecutionCheckpoint checkpoint, StoryDefinition definition) {
        CompiledStoryFingerprint fingerprint = StoryDefinitionFingerprint.from(definition);
        return checkpoint.definitionFingerprint().equals(fingerprint);
    }

    private RecoveryResult recoverPhase(
            ExecutionCheckpoint checkpoint,
            StoryNode node,
            Instant occurredAt) {
        StoryExecutionState state = checkpoint.state();
        if (state.phase() == StoryExecutionPhase.READY) {
            return RecoveryResult.of(RecoveryStatus.RESTORED, state, List.of(), List.of());
        }
        if (state.phase() == StoryExecutionPhase.WAITING) {
            return recoverWaiting(checkpoint, node, occurredAt);
        }
        if (state.phase() == StoryExecutionPhase.FINISHED || state.phase() == StoryExecutionPhase.FAILED) {
            return RecoveryResult.of(RecoveryStatus.TERMINAL, state, List.of(), List.of());
        }
        return rejected(state, "ESE-PERSISTENCE-014", occurredAt);
    }

    private RecoveryResult recoverWaiting(
            ExecutionCheckpoint checkpoint,
            StoryNode node,
            Instant occurredAt) {
        StoryExecutionState state = checkpoint.state();
        Optional<PendingActionCheckpoint> pending = checkpoint.pendingAction();
        if (pending.isEmpty() || !validPending(state, node, pending.orElseThrow())) {
            return rejected(state, "ESE-PERSISTENCE-009", occurredAt);
        }
        PendingActionCheckpoint action = pending.orElseThrow();
        ExternalCommand command = new ExternalCommand(
                action.actionType(),
                action.actionExecutionId(),
                action.arguments());
        StoryDiagnostic diagnostic = diagnostics.create(
                "ESE-PERSISTENCE-013",
                state,
                occurredAt,
                Map.of("actionExecutionId", action.actionExecutionId().asString()));
        return RecoveryResult.of(RecoveryStatus.RECOVERY_REQUESTED, state, List.of(command), List.of(diagnostic));
    }

    private boolean validPending(
            StoryExecutionState state,
            StoryNode node,
            PendingActionCheckpoint pending) {
        return !state.pendingActionExecutionId().isBlank()
                && pending.matches(state.pendingActionExecutionId())
                && node.kind() == StoryNodeKind.EXTERNAL_ACTION
                && node.actionType().equals(pending.actionType())
                && node.actionArguments().equals(pending.arguments());
    }

    private RecoveryResult rejected(StoryExecutionState state, String code, Instant occurredAt) {
        StoryDiagnostic diagnostic = diagnostics.create(code, state, occurredAt, Map.of());
        return RecoveryResult.of(RecoveryStatus.REJECTED, state, List.of(), List.of(diagnostic));
    }
}
