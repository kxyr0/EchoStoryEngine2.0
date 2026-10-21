package io.github.kxyr0.echomagic.core.gesture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echomagic.api.gesture.MagicTechnique;
import java.util.List;
import org.junit.jupiter.api.Test;

class MagicTechniqueRecognizerTest {
    private final MagicTechniqueRecognizer recognizer = new MagicTechniqueRecognizer();

    @Test void acceptsStraightLinesInDifferentDirections() {
        assertTrue(recognizer.isLine(points(0, 0, 0.7, 0.02)));
        assertTrue(recognizer.isLine(points(0, 0, 0.02, 0.7)));
        assertTrue(recognizer.isLine(points(0, 0, -0.6, -0.5)));
    }

    @Test void rejectsTapWaveLoopAndEraseStroke() {
        assertFalse(recognizer.isLine(points(0, 0, 0.02, 0.01)));
        assertFalse(recognizer.isLine(List.of(
                point(0, 0, 0), point(0.25, 0.18, 100), point(0.5, -0.18, 200), point(0.8, 0, 300))));
        assertFalse(recognizer.isLine(List.of(
                point(0, 0, 0), point(0.4, 0.4, 100), point(0.8, 0, 200), point(0.4, -0.4, 300),
                point(0, 0, 400))));
        assertFalse(recognizer.isLine(List.of(
                point(0, 0, 0), point(0.7, 0, 150), point(0.02, 0, 300))));
    }

    @Test void recognizesBroadArcAndLargeCircleWithoutHold() {
        assertEquals(MagicTechnique.ARC, recognizer.recognize(List.of(
                point(-0.5, 0, 0), point(-0.28, 0.23, 100), point(0, 0.32, 200),
                point(0.28, 0.23, 300), point(0.5, 0, 400))).orElseThrow());
        assertEquals(MagicTechnique.CIRCLE, recognizer.recognize(List.of(
                point(0.45, 0, 0), point(0.32, 0.32, 100), point(0, 0.45, 200),
                point(-0.32, 0.32, 300), point(-0.45, 0, 400), point(-0.32, -0.32, 500),
                point(0, -0.45, 600), point(0.32, -0.32, 700), point(0.45, 0, 800))).orElseThrow());
    }

    @Test void recognizesShallowArcButLeavesQuickMediumLoopForAirSchool() {
        assertEquals(MagicTechnique.ARC, recognizer.recognize(List.of(
                point(-0.5, 0, 0), point(-0.25, 0.08, 100), point(0, 0.1, 200),
                point(0.25, 0.08, 300), point(0.5, 0, 400))).orElseThrow());
        assertTrue(recognizer.recognize(List.of(
                point(0.23, 0, 0), point(0, 0.23, 120), point(-0.23, 0, 240),
                point(0, -0.23, 360), point(0.23, 0, 480))).isEmpty());
    }

    @Test void heldMediumLoopBecomesCircleTechnique() {
        assertEquals(MagicTechnique.CIRCLE, recognizer.recognize(List.of(
                point(0.23, 0, 0), point(0, 0.23, 120), point(-0.23, 0, 240),
                point(0, -0.23, 360), point(0.23, 0, 480), point(0.23, 0, 700),
                point(0.23, 0, 900))).orElseThrow());
    }

    @Test void recognizesDescendingHookWithCurvedTip() {
        assertEquals(MagicTechnique.HOOK, recognizer.recognize(List.of(
                point(0, 0.5, 0), point(0.01, 0.25, 100), point(0, 0, 200),
                point(0.03, -0.3, 300), point(0.18, -0.42, 400), point(0.38, -0.32, 500),
                point(0.48, -0.15, 600))).orElseThrow());
    }

    @Test void recognizesClosedTriangleAsWeaponRune() {
        assertEquals(MagicTechnique.WEAPON, recognizer.recognize(List.of(
                point(0.0, 0.5, 0), point(0.25, 0.05, 100), point(0.5, -0.4, 200),
                point(0.0, -0.4, 300), point(-0.5, -0.4, 400), point(-0.25, 0.05, 500),
                point(0.0, 0.5, 600))).orElseThrow());
    }

    @Test void recognizesAStableHeldPointButNotAQuickTap() {
        assertEquals(MagicTechnique.POINT, recognizer.recognize(List.of(
                point(0.2, 0.2, 0), point(0.22, 0.19, 250),
                point(0.19, 0.21, 520))).orElseThrow());
        assertTrue(recognizer.recognize(List.of(
                point(0.2, 0.2, 0), point(0.2, 0.2, 300))).isEmpty());
    }

    private static List<MagicGesturePoint> points(double x1, double y1, double x2, double y2) {
        return List.of(point(x1, y1, 0), point((x1 + x2) / 2.0D, (y1 + y2) / 2.0D, 100),
                point(x2, y2, 200));
    }

    private static MagicGesturePoint point(double x, double y, long time) {
        return new MagicGesturePoint(x, y, time);
    }
}
