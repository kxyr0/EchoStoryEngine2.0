package io.github.kxyr0.echomagic.core.gesture.composition;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureModifier;
import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;
import io.github.kxyr0.echomagic.core.gesture.MagicTechniqueRecognizer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class MagicGestureCompositionRecognizer {
    private static final long POINT_HOLD_MILLIS = 280L;
    private static final double POINT_RADIUS = 0.14D;
    private static final double POINT_ENDPOINT_RADIUS = 0.36D;
    private static final double BRANCH_JUNCTION_RADIUS = 0.28D;
    private static final double MINIMUM_BRANCH_LENGTH = 0.18D;
    private final MagicTechniqueRecognizer techniques = new MagicTechniqueRecognizer();

    public Optional<MagicGestureComposition> recognize(List<List<MagicGesturePoint>> strokes) {
        return recognize(strokes, Optional.empty());
    }

    public Optional<MagicGestureComposition> recognize(List<List<MagicGesturePoint>> strokes,
                                                        Optional<io.github.kxyr0.echomagic.api.gesture.MagicTechnique>
                                                                personalTechnique) {
        if (strokes == null || strokes.isEmpty()) return Optional.empty();
        Optional<io.github.kxyr0.echomagic.api.gesture.MagicTechnique> builtIn =
                techniques.recognize(strokes.get(0));
        var technique = builtIn.filter(value -> value
                == io.github.kxyr0.echomagic.api.gesture.MagicTechnique.POINT);
        if (technique.isEmpty()) technique = personalTechnique.isPresent() ? personalTechnique : builtIn;
        if (technique.isEmpty()) return Optional.empty();
        MagicGestureModifier modifier = modifier(strokes);
        if (modifier == null) return Optional.empty();
        return Optional.of(new MagicGestureComposition(technique.orElseThrow(), modifier));
    }

    private MagicGestureModifier modifier(List<List<MagicGesturePoint>> strokes) {
        if (strokes.size() == 1) return MagicGestureModifier.NONE;
        if (strokes.size() == 2 && isEndPoint(strokes.get(0), strokes.get(1))) {
            return MagicGestureModifier.DELAY;
        }
        if (isFork(strokes)) return MagicGestureModifier.SPLIT;
        return null;
    }

    private static boolean isEndPoint(List<MagicGesturePoint> primary, List<MagicGesturePoint> point) {
        return isPoint(point)
                && primary.get(primary.size() - 1).distanceTo(pointCenter(point)) <= POINT_ENDPOINT_RADIUS;
    }

    private boolean isFork(List<List<MagicGesturePoint>> strokes) {
        List<MagicGesturePoint> primary = strokes.get(0);
        MagicGesturePoint junction = primary.get(primary.size() - 1);
        if (strokes.size() == 2) return isContinuousFork(strokes.get(1), junction);
        return strokes.size() == 3 && isTwoBranchFork(strokes.get(1), strokes.get(2), junction);
    }

    private boolean isContinuousFork(List<MagicGesturePoint> stroke, MagicGesturePoint expectedJunction) {
        if (stroke.size() < 3 || isPoint(stroke)) return false;
        int junctionIndex = closestIndex(stroke, expectedJunction);
        if (junctionIndex == 0 || junctionIndex == stroke.size() - 1
                || stroke.get(junctionIndex).distanceTo(expectedJunction) > BRANCH_JUNCTION_RADIUS) return false;
        MagicGesturePoint junction = stroke.get(junctionIndex);
        return branchesDiverge(junction, stroke.get(0), stroke.get(stroke.size() - 1));
    }

    private boolean isTwoBranchFork(List<MagicGesturePoint> first, List<MagicGesturePoint> second,
                                    MagicGesturePoint junction) {
        if (!techniques.isLine(first) || !techniques.isLine(second)) return false;
        MagicGesturePoint firstFar = farEndpoint(first, junction);
        MagicGesturePoint secondFar = farEndpoint(second, junction);
        if (nearEndpoint(first, junction).distanceTo(junction) > BRANCH_JUNCTION_RADIUS
                || nearEndpoint(second, junction).distanceTo(junction) > BRANCH_JUNCTION_RADIUS) return false;
        return branchesDiverge(junction, firstFar, secondFar);
    }

    private static boolean isPoint(List<MagicGesturePoint> stroke) {
        if (stroke == null || stroke.size() < 2) return false;
        long duration = stroke.get(stroke.size() - 1).elapsedMillis() - stroke.get(0).elapsedMillis();
        if (duration < POINT_HOLD_MILLIS) return false;
        if (stroke.size() == 2) return stroke.get(0).distanceTo(stroke.get(1)) <= POINT_RADIUS;
        MagicGesturePoint center = pointCenter(stroke);
        int stable = 0;
        for (MagicGesturePoint point : stroke) if (point.distanceTo(center) <= POINT_RADIUS) stable++;
        int allowedOutliers = Math.max(1, stroke.size() / 4);
        return stable >= stroke.size() - allowedOutliers;
    }

    private static MagicGesturePoint pointCenter(List<MagicGesturePoint> stroke) {
        double[] x = new double[stroke.size()];
        double[] y = new double[stroke.size()];
        for (int index = 0; index < stroke.size(); index++) {
            x[index] = stroke.get(index).x();
            y[index] = stroke.get(index).y();
        }
        Arrays.sort(x);
        Arrays.sort(y);
        int middle = stroke.size() / 2;
        double centerX = stroke.size() % 2 == 0 ? (x[middle - 1] + x[middle]) * 0.5D : x[middle];
        double centerY = stroke.size() % 2 == 0 ? (y[middle - 1] + y[middle]) * 0.5D : y[middle];
        return new MagicGesturePoint(centerX, centerY, stroke.get(0).elapsedMillis());
    }

    private static int closestIndex(List<MagicGesturePoint> points, MagicGesturePoint target) {
        int result = 0;
        double closest = Double.POSITIVE_INFINITY;
        for (int index = 0; index < points.size(); index++) {
            double distance = points.get(index).distanceTo(target);
            if (distance < closest) { closest = distance; result = index; }
        }
        return result;
    }

    private static MagicGesturePoint nearEndpoint(List<MagicGesturePoint> stroke, MagicGesturePoint junction) {
        MagicGesturePoint first = stroke.get(0);
        MagicGesturePoint last = stroke.get(stroke.size() - 1);
        return first.distanceTo(junction) <= last.distanceTo(junction) ? first : last;
    }

    private static MagicGesturePoint farEndpoint(List<MagicGesturePoint> stroke, MagicGesturePoint junction) {
        MagicGesturePoint first = stroke.get(0);
        MagicGesturePoint last = stroke.get(stroke.size() - 1);
        return first.distanceTo(junction) > last.distanceTo(junction) ? first : last;
    }

    private static boolean branchesDiverge(MagicGesturePoint junction, MagicGesturePoint first,
                                            MagicGesturePoint second) {
        double firstX = first.x() - junction.x();
        double firstY = first.y() - junction.y();
        double secondX = second.x() - junction.x();
        double secondY = second.y() - junction.y();
        double firstLength = Math.hypot(firstX, firstY);
        double secondLength = Math.hypot(secondX, secondY);
        if (firstLength < MINIMUM_BRANCH_LENGTH || secondLength < MINIMUM_BRANCH_LENGTH) return false;
        double cosine = (firstX * secondX + firstY * secondY) / (firstLength * secondLength);
        return cosine < 0.88D && cosine > -0.985D;
    }
}
