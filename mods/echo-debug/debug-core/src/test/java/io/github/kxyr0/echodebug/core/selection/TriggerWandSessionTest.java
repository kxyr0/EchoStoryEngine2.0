package io.github.kxyr0.echodebug.core.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodebug.core.diagnostic.DebugDiagnosticCode;
import io.github.kxyr0.echodebug.core.identity.DebugRequestId;
import io.github.kxyr0.echodebug.core.identity.TriggerWandSessionId;
import io.github.kxyr0.echodebug.core.shape.TriggerWandShapeMode;
import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import org.junit.jupiter.api.Test;

class TriggerWandSessionTest {
    @Test
    void seatFinalizesOnePointWithoutInventingAnArea() {
        TriggerWandSession session = session();
        session.changeShape(id("seat"), 0, true);
        assertFalse(session.finalizeSelection(id("empty"), 1).successful());
        session.addPoint(id("point"), 1, point(0, 64, 0), position(0, 64, 0));
        var result = session.finalizeSelection(id("finish"), 2);
        assertTrue(result.successful());
        assertTrue(result.value().orElseThrow().complete());
        assertTrue(result.value().orElseThrow().geometry().isEmpty());
        assertEquals(TriggerWandShapeMode.SEAT, result.value().orElseThrow().shape());
        assertFalse(session.addPoint(id("extra"), 2, point(1, 64, 0), position(1, 64, 0)).successful());
        session.undo(id("undo"), 2);
        assertFalse(session.snapshot().complete());
    }

    @Test
    void boxCompletesAfterTwoAcceptedPoints() {
        TriggerWandSession session = session();

        var first = session.addPoint(id("a"), 0, point(0, 64, 0), position(0, 64, 0));
        var second = session.addPoint(id("b"), 1, point(1, 65, 1), position(1, 65, 1));

        assertTrue(first.successful());
        assertTrue(second.value().orElseThrow().complete());
        assertEquals(2, second.value().orElseThrow().points().size());
    }

    @Test
    void boxIncludesBothBlocksWhenPointsAreSelectedInReverseOrder() {
        TriggerWandSession session = session();
        session.addPoint(id("a"), 0, point(5, 64, 5), position(5, 64, 5));

        var result = session.addPoint(id("b"), 1, point(2, 63, 3), position(2, 63, 3));
        var geometry = result.value().orElseThrow().geometry().orElseThrow();

        assertEquals(new Vector3(2, 63, 3), geometry.min());
        assertEquals(new Vector3(6, 65, 6), geometry.max());
    }

    @Test
    void sphereRejectsCenterAsRadiusPoint() {
        TriggerWandSession session = session();
        session.changeShape(id("shape"), 0, false);
        session.addPoint(id("a"), 1, point(0, 64, 0), position(0, 64, 0));

        var result = session.addPoint(id("b"), 2, point(0, 64, 0), position(0, 64, 0));

        assertFalse(result.successful());
        assertEquals(DebugDiagnosticCode.DEBUG_SELECTION_POINT_REJECTED, result.code());
    }

    @Test
    void cylinderCompletesAfterThreePoints() {
        TriggerWandSession session = session();
        session.changeShape(id("s1"), 0, false);
        session.changeShape(id("s2"), 1, false);

        session.addPoint(id("a"), 2, point(0, 64, 0), position(0, 64, 0));
        session.addPoint(id("b"), 3, point(3, 64, 0), position(3, 64, 0));
        var result = session.addPoint(id("c"), 4, point(0, 68, 0), position(0, 68, 0));

        assertTrue(result.value().orElseThrow().complete());
        assertEquals(TriggerWandShapeMode.CYLINDER, result.value().orElseThrow().shape());
    }

    @Test
    void polygonAcceptsFourPointsAndDefaultHeight() {
        TriggerWandSession session = polygonSession();

        session.addPoint(id("a"), 3, point(0, 64, 0), position(0, 64, 0));
        session.addPoint(id("b"), 4, point(4, 64, 0), position(4, 64, 0));
        session.addPoint(id("c"), 5, point(4, 64, 4), position(4, 64, 4));
        var result = session.addPoint(id("d"), 6, point(0, 64, 4), position(0, 64, 4));

        assertTrue(result.value().orElseThrow().complete());
        assertEquals(67.0D, result.value().orElseThrow().geometry().orElseThrow().max().Y());
    }

    @Test
    void staleRevisionDoesNotChangeSelection() {
        TriggerWandSession session = session();
        session.addPoint(id("a"), 0, point(0, 64, 0), position(0, 64, 0));

        var result = session.addPoint(id("b"), 0, point(1, 64, 0), position(1, 64, 0));

        assertFalse(result.successful());
        assertEquals(1, result.value().orElseThrow().points().size());
    }

    @Test
    void duplicateRequestReplaysSameResult() {
        TriggerWandSession session = session();
        DebugRequestId requestId = id("a");

        var first = session.addPoint(requestId, 0, point(0, 64, 0), position(0, 64, 0));
        var second = session.addPoint(requestId, 0, point(0, 64, 0), position(0, 64, 0));

        assertEquals(first.value().orElseThrow().revision(), second.value().orElseThrow().revision());
        assertEquals(1, second.value().orElseThrow().points().size());
    }

    @Test
    void sameRequestIdWithDifferentArgumentsConflicts() {
        TriggerWandSession session = session();
        DebugRequestId requestId = id("a");

        session.addPoint(requestId, 0, point(0, 64, 0), position(0, 64, 0));
        var second = session.addPoint(requestId, 0, point(1, 64, 0), position(1, 64, 0));

        assertEquals(DebugDiagnosticCode.DEBUG_REQUEST_IDEMPOTENCY_CONFLICT, second.code());
    }

    @Test
    void dimensionChangeIsRejected() {
        TriggerWandSession session = session();
        session.addPoint(id("a"), 0, point(0, 64, 0), position(0, 64, 0));

        var result = session.addPoint(id("b"), 1, new TriggerBlockPoint("minecraft:the_nether", 1, 64, 0),
                position(1, 64, 0));

        assertEquals(DebugDiagnosticCode.DEBUG_SELECTION_DIMENSION_CHANGED, result.code());
    }

    private static TriggerWandSession polygonSession() {
        TriggerWandSession session = session();
        session.changeShape(id("s1"), 0, false);
        session.changeShape(id("s2"), 1, false);
        session.changeShape(id("s3"), 2, false);
        return session;
    }

    private static TriggerWandSession session() {
        return new TriggerWandSession(new TriggerWandSessionId("session"), "player");
    }

    private static TriggerBlockPoint point(int x, int y, int z) {
        return new TriggerBlockPoint("minecraft:overworld", x, y, z);
    }

    private static DebugRequestId id(String value) {
        return new DebugRequestId(value);
    }

    private static Vector3 position(int x, int y, int z) {
        return new Vector3(x, y, z);
    }
}
