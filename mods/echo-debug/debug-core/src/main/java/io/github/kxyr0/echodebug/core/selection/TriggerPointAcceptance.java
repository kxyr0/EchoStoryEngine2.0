package io.github.kxyr0.echodebug.core.selection;

import io.github.kxyr0.echodebug.core.diagnostic.DebugDiagnosticCode;
import io.github.kxyr0.echodebug.core.diagnostic.DebugOperationResult;
import io.github.kxyr0.echodebug.core.shape.TriggerWandShapeMode;
import java.util.List;
import java.util.Optional;

final class TriggerPointAcceptance {
    DebugOperationResult<TriggerSelectionSnapshot> accept(
            TriggerWandSession session,
            TriggerBlockPoint point,
            double distance,
            double maxDistance
    ) {
        if (distance > maxDistance) {
            return session.failure(DebugDiagnosticCode.DEBUG_SELECTION_DISTANCE_INVALID,
                    "Selected block is outside interaction distance.");
        }
        if (wrongDimension(session.dimension(), point)) {
            return session.failure(DebugDiagnosticCode.DEBUG_SELECTION_DIMENSION_CHANGED,
                    "Selection point is in a different dimension.");
        }
        if (!session.shape().acceptsMorePoints(session.points().size())) {
            return session.failure(DebugDiagnosticCode.DEBUG_SELECTION_STAGE_INVALID,
                    "Current shape already has all required points.");
        }
        return validateShape(session, point)
                .orElseGet(() -> session.acceptPoint(point));
    }

    private Optional<DebugOperationResult<TriggerSelectionSnapshot>> validateShape(TriggerWandSession session,
                                                                                   TriggerBlockPoint point) {
        TriggerWandShapeMode shape = session.shape();
        List<TriggerBlockPoint> points = session.points();
        if (shape == TriggerWandShapeMode.POLYGON && points.size() >= 64) {
            return Optional.of(session.failure(DebugDiagnosticCode.DEBUG_POLYGON_SELECTION_INVALID,
                    "Polygon selection is limited to 64 points."));
        }
        if ((shape == TriggerWandShapeMode.SPHERE || shape == TriggerWandShapeMode.CYLINDER)
                && points.size() == 1 && sameBlock(points.get(0), point)) {
            return Optional.of(session.failure(DebugDiagnosticCode.DEBUG_SELECTION_POINT_REJECTED,
                    "Radius point must be different from center."));
        }
        return Optional.empty();
    }

    private static boolean wrongDimension(Optional<String> dimension, TriggerBlockPoint point) {
        return dimension.isPresent() && !dimension.orElseThrow().equals(point.dimension());
    }

    private static boolean sameBlock(TriggerBlockPoint first, TriggerBlockPoint second) {
        return first.x() == second.x() && first.y() == second.y() && first.z() == second.z();
    }
}
