package io.github.kxyr0.echomagic.core.gesture.composition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureModifier;
import io.github.kxyr0.echomagic.api.gesture.MagicTechnique;
import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;
import java.util.List;
import org.junit.jupiter.api.Test;

class MagicGestureCompositionRecognizerTest {
    private final MagicGestureCompositionRecognizer recognizer = new MagicGestureCompositionRecognizer();

    @Test void recognizesHeldPointAsDelay() {
        var result = recognizer.recognize(List.of(line(), List.of(
                point(0.7, 0, 300), point(0.71, 0.01, 500), point(0.7, 0, 650)))).orElseThrow();
        assertEquals(MagicTechnique.LINE, result.technique());
        assertEquals(MagicGestureModifier.DELAY, result.modifier());
    }

    @Test void recognizesStandaloneHeldPointAsTechnique() {
        var result = recognizer.recognize(List.of(List.of(
                point(0.3, 0.2, 0), point(0.32, 0.19, 250),
                point(0.29, 0.21, 520)))).orElseThrow();
        assertEquals(MagicTechnique.POINT, result.technique());
        assertEquals(MagicGestureModifier.NONE, result.modifier());
    }

    @Test void rejectsAQuickTapAsModifier() {
        assertTrue(recognizer.recognize(List.of(line(), List.of(
                point(0.7, 0, 300), point(0.7, 0, 420)))).isEmpty());
    }

    @Test void rejectsHeldPointAwayFromTheEnd() {
        assertTrue(recognizer.recognize(List.of(line(), List.of(
                point(-0.2, 0.4, 300), point(-0.2, 0.4, 650)))).isEmpty());
    }

    @Test void recognizesHeldPointDespiteMouseDriftAndOneTwitch() {
        var result = recognizer.recognize(List.of(line(), List.of(
                point(0.70, 0.00, 300), point(0.75, 0.03, 390),
                point(1.02, -0.24, 480), point(0.68, 0.05, 570),
                point(0.73, -0.02, 660)))).orElseThrow();
        assertEquals(MagicGestureModifier.DELAY, result.modifier());
    }

    @Test void recognizesHeldPointWhenMouseTwitchesOnRelease() {
        var result = recognizer.recognize(List.of(line(), List.of(
                point(0.70, 0.00, 300), point(0.72, 0.01, 500),
                point(1.01, 0.25, 650)))).orElseThrow();
        assertEquals(MagicGestureModifier.DELAY, result.modifier());
    }

    @Test void rejectsARealMovingStrokeAsPoint() {
        assertTrue(recognizer.recognize(List.of(line(), List.of(
                point(0.70, 0.00, 300), point(0.90, 0.08, 480),
                point(1.10, 0.16, 660)))).isEmpty());
    }

    @Test void recognizesContinuousForkAcrossPrimaryEndpoint() {
        var result = recognizer.recognize(List.of(line(), List.of(
                point(0.45, 0.28, 300), point(0.7, 0, 400), point(0.95, 0.28, 500)))).orElseThrow();
        assertEquals(MagicGestureModifier.SPLIT, result.modifier());
    }

    @Test void recognizesTwoSeparateBranches() {
        var result = recognizer.recognize(List.of(line(),
                List.of(point(0.7, 0, 300), point(0.95, 0.28, 450)),
                List.of(point(0.7, 0, 500), point(0.95, -0.28, 650)))).orElseThrow();
        assertEquals(MagicGestureModifier.SPLIT, result.modifier());
    }

    @Test void recognizesHookWithHeldEndPoint() {
        List<MagicGesturePoint> hook = List.of(point(0, 0.5, 0), point(0, 0.2, 100),
                point(0.02, -0.2, 200), point(0.16, -0.42, 300), point(0.4, -0.3, 400),
                point(0.48, -0.12, 500));
        var result = recognizer.recognize(List.of(hook, List.of(
                point(0.48, -0.12, 550), point(0.48, -0.12, 900)))).orElseThrow();
        assertEquals(MagicTechnique.HOOK, result.technique());
        assertEquals(MagicGestureModifier.DELAY, result.modifier());
    }

    private static List<MagicGesturePoint> line() {
        return List.of(point(0, 0, 0), point(0.35, 0, 120), point(0.7, 0, 240));
    }

    private static MagicGesturePoint point(double x, double y, long time) {
        return new MagicGesturePoint(x, y, time);
    }
}
