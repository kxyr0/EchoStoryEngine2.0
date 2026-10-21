package io.github.kxyr0.echomagic.core.gesture.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureTemplateTarget;
import io.github.kxyr0.echomagic.core.gesture.MagicGestureMode;
import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MagicGestureTemplateLibraryTest {
    private final MagicGestureTemplateLibrary library = new MagicGestureTemplateLibrary();

    @Test void learnsPlayersAsymmetricHookFromThreeExamples() {
        var template = library.train(MagicGestureTemplateTarget.HOOK, List.of(
                hook(0.00D, 1.00D), hook(0.03D, 0.92D), hook(-0.02D, 1.08D)));
        assertEquals(64, template.points().size());
        assertEquals(3, template.variants().size());
        var recognized = library.recognize(MagicGestureMode.TECHNIQUE, hook(0.05D, 0.97D), List.of(template));
        assertEquals(MagicGestureTemplateTarget.HOOK, recognized.orElseThrow());
    }

    @Test void toleratesSmallRotationButRejectsAmbiguousPersonalTemplates() {
        var samples = List.of(hook(0.00D, 1.00D), hook(0.03D, 0.92D), hook(-0.02D, 1.08D));
        var hook = library.train(MagicGestureTemplateTarget.HOOK, samples);
        assertEquals(MagicGestureTemplateTarget.HOOK, library.recognize(MagicGestureMode.TECHNIQUE,
                rotate(hook(0.01D, 1.02D), 8.0D), List.of(hook)).orElseThrow());
        var arc = library.train(MagicGestureTemplateTarget.ARC, samples);
        assertTrue(library.recognizeDetailed(MagicGestureMode.TECHNIQUE,
                hook(0.01D, 1.02D), List.of(hook, arc)).ambiguous());
    }

    @Test void trainingAcceptsAConsistentPersonalGlyphInsteadOfDemandingTheCanonicalShape() {
        assertEquals(MagicGestureTrainingFeedback.ACCEPTED, library.assess(
                MagicGestureTemplateTarget.CIRCLE, horizontalLine(), List.of()));
    }

    @Test void rejectsDifferentShapeAndWrongMode() {
        var template = library.train(MagicGestureTemplateTarget.HOOK, List.of(
                hook(0.00D, 1.00D), hook(0.03D, 0.92D), hook(-0.02D, 1.08D)));
        assertTrue(library.recognize(MagicGestureMode.TECHNIQUE, horizontalLine(), List.of(template)).isEmpty());
        assertTrue(library.recognize(MagicGestureMode.SCHOOL, hook(0.0D, 1.0D), List.of(template)).isEmpty());
    }

    @Test void personalizedPointStillRequiresAHold() {
        var held = List.of(point(0, 0, 0), point(0.01, -0.01, 650));
        var template = library.train(MagicGestureTemplateTarget.BLOOD, List.of(held, held, held));
        assertTrue(library.recognize(MagicGestureMode.SCHOOL,
                List.of(point(0, 0, 0), point(0.01, 0, 120)), List.of(template)).isEmpty());
        assertEquals(MagicGestureTemplateTarget.BLOOD,
                library.recognize(MagicGestureMode.SCHOOL, held, List.of(template)).orElseThrow());
    }

    @Test void learnsPlayersWeaponTriangleInsteadOfTreatingItAsAHook() {
        var template = library.train(MagicGestureTemplateTarget.WEAPON, List.of(
                triangle(0.00D, 1.00D), triangle(0.03D, 0.94D), triangle(-0.02D, 1.07D)));
        var recognized = library.recognize(MagicGestureMode.TECHNIQUE,
                triangle(0.04D, 0.98D), List.of(template));
        assertEquals(MagicGestureTemplateTarget.WEAPON, recognized.orElseThrow());
    }

    @Test void learnedVoidAcceptsEndpointDriftAndUnevenTurnTiming() {
        var template = library.train(MagicGestureTemplateTarget.VOID, List.of(
                erase(0.00D, 0.02D), erase(0.03D, 0.06D), erase(-0.025D, 0.04D)));
        var recognized = library.recognize(MagicGestureMode.SCHOOL,
                erase(0.055D, 0.10D), List.of(template));
        assertEquals(MagicGestureTemplateTarget.VOID, recognized.orElseThrow());
    }

    private static List<MagicGesturePoint> hook(double offset, double scale) {
        double[][] raw = {{0.0, 0.6}, {0.03, 0.25}, {0.02, -0.15}, {0.10, -0.43},
                {0.30, -0.50}, {0.48, -0.37}, {0.54, -0.17}};
        List<MagicGesturePoint> result = new ArrayList<>();
        for (int index = 0; index < raw.length; index++) {
            result.add(point(offset + raw[index][0] * scale, raw[index][1] * scale, index * 90L));
        }
        return result;
    }

    private static List<MagicGesturePoint> horizontalLine() {
        return List.of(point(-0.5, 0, 0), point(0, 0.02, 180), point(0.5, 0, 360));
    }

    private static List<MagicGesturePoint> triangle(double offset, double scale) {
        double[][] raw = {{0.0, 0.52}, {0.22, 0.08}, {0.5, -0.42}, {0.0, -0.38},
                {-0.48, -0.4}, {-0.2, 0.1}, {0.0, 0.52}};
        List<MagicGesturePoint> result = new ArrayList<>();
        for (int index = 0; index < raw.length; index++) {
            result.add(point(offset + raw[index][0] * scale, raw[index][1] * scale, index * 90L));
        }
        return result;
    }

    private static List<MagicGesturePoint> erase(double bend, double endpoint) {
        double[][] raw = {{0.0, 0.0}, {0.11, bend}, {0.23, bend * 1.4D}, {0.38, bend},
                {0.55, bend * 0.7D}, {0.74, 0.0}, {0.49, -bend}, {0.21, -bend * 0.6D}, {endpoint, 0.01}};
        List<MagicGesturePoint> result = new ArrayList<>();
        for (int index = 0; index < raw.length; index++) {
            result.add(point(raw[index][0], raw[index][1], index * index * 18L));
        }
        return result;
    }

    private static List<MagicGesturePoint> rotate(List<MagicGesturePoint> points, double degrees) {
        double angle = Math.toRadians(degrees); double cosine = Math.cos(angle); double sine = Math.sin(angle);
        return points.stream().map(point -> point(point.x() * cosine - point.y() * sine,
                point.x() * sine + point.y() * cosine, point.elapsedMillis())).toList();
    }

    private static MagicGesturePoint point(double x, double y, long time) {
        return new MagicGesturePoint(x, y, time);
    }
}
