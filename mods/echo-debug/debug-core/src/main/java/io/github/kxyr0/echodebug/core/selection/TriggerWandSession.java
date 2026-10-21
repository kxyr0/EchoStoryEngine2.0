package io.github.kxyr0.echodebug.core.selection;

import io.github.kxyr0.echodebug.core.diagnostic.DebugDiagnosticCode;
import io.github.kxyr0.echodebug.core.diagnostic.DebugOperationResult;
import io.github.kxyr0.echodebug.core.identity.DebugRequestId;
import io.github.kxyr0.echodebug.core.identity.TriggerWandSessionId;
import io.github.kxyr0.echodebug.core.request.DebugOperationKind;
import io.github.kxyr0.echodebug.core.request.TriggerWandRequestLedger;
import io.github.kxyr0.echodebug.core.shape.TriggerWandShapeCycle;
import io.github.kxyr0.echodebug.core.shape.TriggerWandShapeMode;
import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TriggerWandSession {
    private final TriggerWandRequestLedger<TriggerSelectionSnapshot> ledger = new TriggerWandRequestLedger<>();
    private final TriggerPointAcceptance acceptance = new TriggerPointAcceptance();
    private final TriggerWandSessionId sessionId;
    private final String playerId;
    private TriggerWandShapeMode shape = TriggerWandShapeMode.BOX;
    private final List<TriggerBlockPoint> points = new ArrayList<>();
    private Optional<String> dimension = Optional.empty();
    private long revision;

    public TriggerWandSession(TriggerWandSessionId sessionId, String playerId) {
        this.sessionId = sessionId;
        this.playerId = java.util.Objects.requireNonNull(playerId, "playerId");
    }

    public DebugOperationResult<TriggerSelectionSnapshot> changeShape(
            DebugRequestId requestId,
            long expectedRevision,
            boolean reverse
    ) {
        return ledger.apply(requestId, DebugOperationKind.CHANGE_SHAPE, expectedRevision + ":" + reverse,
                () -> stale(expectedRevision).orElseGet(() -> {
                    shape = TriggerWandShapeCycle.next(shape, reverse);
                    points.clear();
                    dimension = Optional.empty();
                    revision++;
                    return DebugOperationResult.success(snapshot());
                }));
    }

    public DebugOperationResult<TriggerSelectionSnapshot> addPoint(
            DebugRequestId requestId,
            long expectedRevision,
            TriggerBlockPoint point,
            Vector3 playerPosition
    ) {
        String args = expectedRevision + ":" + point + ":" + playerPosition.stableText();
        return ledger.apply(requestId, DebugOperationKind.ADD_POINT, args,
                () -> stale(expectedRevision).orElseGet(() -> acceptance.accept(this, point,
                        point.distanceTo(playerPosition), 6.0D)));
    }

    public DebugOperationResult<TriggerSelectionSnapshot> undo(DebugRequestId requestId, long expectedRevision) {
        return ledger.apply(requestId, DebugOperationKind.UNDO_POINT, Long.toString(expectedRevision),
                () -> stale(expectedRevision).orElseGet(this::undoLatest));
    }

    public DebugOperationResult<TriggerSelectionSnapshot> clear(DebugRequestId requestId, long expectedRevision) {
        return ledger.apply(requestId, DebugOperationKind.CLEAR_SELECTION, Long.toString(expectedRevision),
                () -> stale(expectedRevision).orElseGet(this::clearAccepted));
    }

    public DebugOperationResult<TriggerSelectionSnapshot> finalizeSelection(
            DebugRequestId requestId,
            long expectedRevision
    ) {
        return ledger.apply(requestId, DebugOperationKind.FINALIZE_SELECTION, Long.toString(expectedRevision),
                () -> stale(expectedRevision).orElseGet(this::finalizeAccepted));
    }

    DebugOperationResult<TriggerSelectionSnapshot> acceptPoint(TriggerBlockPoint point) {
        points.add(point);
        dimension = Optional.of(point.dimension());
        revision++;
        return DebugOperationResult.success(snapshot());
    }

    public DebugOperationResult<TriggerSelectionSnapshot> failure(DebugDiagnosticCode code, String message) {
        return DebugOperationResult.failure(code, message, snapshot());
    }

    private Optional<DebugOperationResult<TriggerSelectionSnapshot>> stale(long expectedRevision) {
        if (expectedRevision == revision) {
            return Optional.empty();
        }
        return Optional.of(failure(DebugDiagnosticCode.DEBUG_SELECTION_REVISION_STALE,
                "Client selection revision is stale."));
    }

    private DebugOperationResult<TriggerSelectionSnapshot> undoLatest() {
        if (!points.isEmpty()) {
            points.remove(points.size() - 1);
            revision++;
            dimension = points.isEmpty() ? Optional.empty() : dimension;
        }
        return DebugOperationResult.success(snapshot());
    }

    private DebugOperationResult<TriggerSelectionSnapshot> clearAccepted() {
        points.clear();
        dimension = Optional.empty();
        revision++;
        return DebugOperationResult.success(snapshot());
    }

    private DebugOperationResult<TriggerSelectionSnapshot> finalizeAccepted() {
        if (snapshot().complete()) {
            return DebugOperationResult.success(snapshot());
        }
        return failure(DebugDiagnosticCode.DEBUG_SELECTION_INCOMPLETE,
                "Not enough points for " + shape + ": " + points.size() + " / " + shape.minimumPoints());
    }

    TriggerWandShapeMode shape() {
        return shape;
    }

    List<TriggerBlockPoint> points() {
        return points;
    }

    Optional<String> dimension() {
        return dimension;
    }

    public TriggerSelectionSnapshot snapshot() {
        return new TriggerSelectionSnapshot(sessionId, playerId, shape, dimension, points, revision,
                TriggerSelectionGeometry.from(shape, points));
    }
}
