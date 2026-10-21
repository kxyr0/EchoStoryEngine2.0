package io.github.kxyr0.echomagic.core.gesture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureSymbol;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MagicGestureRecognizerTest {
    private final MagicGestureRecognizer recognizer = new MagicGestureRecognizer();

    @Test void recognizesUpwardFireStroke() {
        assertSymbol(MagicGestureSymbol.FIRE,
                points(new double[][] {{0, 0}, {0.01, 0.24}, {0.02, 0.48}, {-0.01, 0.72}}));
    }

    @Test void fastUpwardTechniqueDoesNotAccidentallySwitchToFire() {
        List<MagicGesturePoint> fastLine = List.of(
                new MagicGesturePoint(0.0D, 0.0D, 0L),
                new MagicGesturePoint(0.01D, 0.36D, 90L),
                new MagicGesturePoint(0.0D, 0.72D, 180L));
        assertTrue(recognizer.recognize(fastLine).symbol().isEmpty());
        assertTrue(new MagicTechniqueRecognizer().isLine(fastLine));
    }

    @Test void recognizesWaterWave() {
        List<MagicGesturePoint> points = new ArrayList<>();
        for (int index = 0; index <= 16; index++) {
            double x = index / 16.0D;
            points.add(new MagicGesturePoint(x, Math.sin(x * Math.PI * 2.0D) * 0.16D, index * 30L));
        }
        assertSymbol(MagicGestureSymbol.WATER, points);
    }

    @Test void recognizesEarthStrokeWithFoot() {
        assertSymbol(MagicGestureSymbol.EARTH, points(new double[][] {
                {0, 0}, {0, -0.35}, {0, -0.75}, {-0.28, -0.76}, {0.3, -0.75}}));
    }

    @Test void recognizesAirLoop() {
        List<MagicGesturePoint> points = new ArrayList<>();
        for (int index = 0; index <= 24; index++) {
            double angle = index * Math.PI * 2.0D / 24.0D;
            points.add(new MagicGesturePoint(Math.cos(angle) * 0.35D, Math.sin(angle) * 0.35D, index * 25L));
        }
        assertSymbol(MagicGestureSymbol.AIR, points);
    }

    @Test void recognizesHeldBloodPoint() {
        assertSymbol(MagicGestureSymbol.BLOOD, List.of(
                new MagicGesturePoint(0, 0, 0), new MagicGesturePoint(0.01, -0.01, 700)));
    }

    @Test void recognizesVoidEraseStroke() {
        assertSymbol(MagicGestureSymbol.VOID, points(new double[][] {
                {0, 0}, {0.3, 0.01}, {0.7, 0}, {0.35, -0.01}, {0.02, 0}}));
    }

    @Test void recognizesNoisyVoidWithUnevenOutboundAndReturnSampling() {
        assertSymbol(MagicGestureSymbol.VOID, points(new double[][] {
                {0, 0}, {0.1, 0.01}, {0.2, 0.02}, {0.3, 0.03}, {0.4, 0.035},
                {0.5, 0.04}, {0.6, 0.045}, {0.75, 0.04}, {0.46, 0.01},
                {0.19, -0.015}, {0.025, 0.005}}));
    }

    @Test void recognizesVoidDrawnAsTwoConsecutiveStrokes() {
        List<MagicGesturePoint> merged = MagicGestureRecognizer.mergeStrokes(List.of(
                points(new double[][] {{0, 0}, {0.22, 0.025}, {0.52, 0.04}, {0.76, 0.02}}),
                List.of(new MagicGesturePoint(0.76, 0.02, 480L),
                        new MagicGesturePoint(0.43, -0.01, 600L),
                        new MagicGesturePoint(0.16, -0.02, 720L),
                        new MagicGesturePoint(0.03, 0.01, 840L))));
        assertSymbol(MagicGestureSymbol.VOID, merged);
    }

    @Test void rejectsTapAndRandomCorner() {
        assertTrue(recognizer.recognize(List.of(
                new MagicGesturePoint(0, 0, 0), new MagicGesturePoint(0.01, 0.01, 80))).symbol().isEmpty());
        assertTrue(recognizer.recognize(points(new double[][] {
                {0, 0}, {0.4, 0.4}, {-0.3, 0.6}, {0.5, -0.2}})).symbol().isEmpty());
    }

    private void assertSymbol(MagicGestureSymbol expected, List<MagicGesturePoint> points) {
        assertEquals(expected, recognizer.recognize(points).symbol().orElseThrow());
    }

    private static List<MagicGesturePoint> points(double[][] coordinates) {
        List<MagicGesturePoint> result = new ArrayList<>();
        for (int index = 0; index < coordinates.length; index++) {
            result.add(new MagicGesturePoint(coordinates[index][0], coordinates[index][1], index * 120L));
        }
        return result;
    }
}
