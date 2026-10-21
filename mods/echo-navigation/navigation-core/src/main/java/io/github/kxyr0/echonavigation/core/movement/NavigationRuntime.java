package io.github.kxyr0.echonavigation.core.movement;

import io.github.kxyr0.echonavigation.api.backend.NavigationSettingsSnapshot;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.movement.NavigationFailure;
import io.github.kxyr0.echonavigation.api.movement.NavigationOutcome;
import io.github.kxyr0.echonavigation.api.movement.NavigationRequest;
import io.github.kxyr0.echonavigation.api.movement.NavigationSnapshot;
import io.github.kxyr0.echonavigation.api.movement.NavigationStatus;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echonavigation.core.command.CommandLedgerResult;
import io.github.kxyr0.echonavigation.core.command.NavigationCommandLedger;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class NavigationRuntime {
    private final NavigationCommandLedger ledger = new NavigationCommandLedger();
    private final NavigationRequestValidator validator = new NavigationRequestValidator();
    private final ActiveMoveIndex active = new ActiveMoveIndex();
    private final Map<NavigationMoveId, NavigationMovementRecord> records = new LinkedHashMap<>();

    public MovementStartResult start(NavigationRequest request, Vector3 currentPosition,
            NavigationSettingsSnapshot settings, long tick) {
        Optional<NavigationFailure> invalid = validator.validate(request, settings);
        if (invalid.isPresent()) {
            return new MovementStartResult(MovementStartResult.Status.INVALID, Optional.empty(), invalid);
        }
        CommandLedgerResult bound = ledger.bind(request.commandId(), request.moveId());
        if (bound.status() == CommandLedgerResult.Status.CONFLICT) {
            return conflict();
        }
        if (bound.status() == CommandLedgerResult.Status.DUPLICATE) {
            return duplicate(bound.moveId().orElseThrow());
        }
        Optional<NavigationMoveId> existing = active.active(request.subjectId());
        if (existing.isPresent()) {
            return new MovementStartResult(MovementStartResult.Status.SUBJECT_BUSY,
                    query(existing.orElseThrow()), Optional.of(NavigationFailure.NPC_MOVE_ALREADY_ACTIVE));
        }
        NavigationMovementRecord record = new NavigationMovementRecord(request, currentPosition, tick);
        records.put(request.moveId(), record);
        active.put(request.subjectId(), request.moveId());
        return new MovementStartResult(MovementStartResult.Status.ACCEPTED, Optional.of(record.snapshot()),
                Optional.empty());
    }

    public MovementUpdateResult status(NavigationMoveId moveId, NavigationStatus status) {
        NavigationMovementRecord record = records.get(moveId);
        record.status(status);
        return new MovementUpdateResult(record.snapshot(), false);
    }

    public MovementUpdateResult progress(NavigationMoveId moveId,
            io.github.kxyr0.echonavigation.api.movement.NavigationProgressSnapshot progress) {
        NavigationMovementRecord record = records.get(moveId);
        record.progress(progress);
        return new MovementUpdateResult(record.snapshot(), false);
    }

    public MovementUpdateResult replan(NavigationMoveId moveId) {
        NavigationMovementRecord record = records.get(moveId);
        record.replan();
        return new MovementUpdateResult(record.snapshot(), false);
    }

    public MovementUpdateResult arrived(NavigationMoveId moveId) {
        NavigationMovementRecord record = records.get(moveId);
        record.arrive();
        active.remove(record.request().subjectId(), moveId);
        return new MovementUpdateResult(record.snapshot(), true);
    }

    public MovementUpdateResult cancel(NavigationMoveId moveId, NavigationOutcome outcome) {
        NavigationMovementRecord record = records.get(moveId);
        record.cancel(outcome);
        active.remove(record.request().subjectId(), moveId);
        return new MovementUpdateResult(record.snapshot(), true);
    }

    public MovementUpdateResult fail(
            NavigationMoveId moveId,
            NavigationStatus status,
            NavigationOutcome outcome,
            NavigationFailure failure) {
        NavigationMovementRecord record = records.get(moveId);
        record.fail(status, outcome, failure);
        active.remove(record.request().subjectId(), moveId);
        return new MovementUpdateResult(record.snapshot(), true);
    }

    public Optional<NavigationSnapshot> query(NavigationMoveId moveId) {
        return Optional.ofNullable(records.get(moveId)).map(NavigationMovementRecord::snapshot);
    }

    public Optional<NavigationSnapshot> active(NavigationSubjectId subjectId) {
        return active.active(subjectId).flatMap(this::query);
    }

    public List<NavigationSnapshot> list(int limit) {
        return records.values().stream()
                .map(NavigationMovementRecord::snapshot)
                .sorted(Comparator.comparing(NavigationSnapshot::moveId))
                .limit(Math.max(0, limit))
                .toList();
    }

    public boolean isMoving(NavigationSubjectId subjectId) {
        return active(subjectId).filter(snapshot -> !snapshot.status().terminal()).isPresent();
    }

    private MovementStartResult duplicate(NavigationMoveId moveId) {
        return new MovementStartResult(MovementStartResult.Status.DUPLICATE_COMMAND, query(moveId), Optional.empty());
    }

    private MovementStartResult conflict() {
        return new MovementStartResult(MovementStartResult.Status.COMMAND_CONFLICT, Optional.empty(),
                Optional.of(NavigationFailure.NAVIGATION_COMMAND_IDEMPOTENCY_CONFLICT));
    }
}
