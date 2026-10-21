package io.github.kxyr0.echomagic.core.gesture.template;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureTemplateTarget;
import io.github.kxyr0.echomagic.core.gesture.MagicGestureMode;
import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;
import io.github.kxyr0.echomagic.core.gesture.MagicGestureRecognizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MagicGestureTemplateLibrary {
    public static final int SAMPLE_COUNT = 3;
    public static final int POINT_COUNT = 64;
    private static final double MINIMUM_MOVING_PATH = 0.12D;
    private static final double AMBIGUITY_DISTANCE = 0.008D;
    private static final double MAXIMUM_SAMPLE_DISTANCE = 0.42D;
    private static final double MINIMUM_MATCH_THRESHOLD = 0.22D;

    public record Template(MagicGestureTemplateTarget target, List<MagicGesturePoint> points,
                           List<List<MagicGesturePoint>> variants, double threshold) {
        public Template {
            target = Objects.requireNonNull(target, "target");
            points = List.copyOf(points);
            variants = variants.stream().map(List::copyOf).toList();
            if (points.size() != POINT_COUNT || variants.isEmpty() || variants.size() > SAMPLE_COUNT
                    || variants.stream().anyMatch(value -> value.size() != POINT_COUNT)
                    || !Double.isFinite(threshold) || threshold < 0.08D || threshold > 0.40D) {
                throw new IllegalArgumentException("Invalid personal gesture template.");
            }
        }

        public Template(MagicGestureTemplateTarget target, List<MagicGesturePoint> points, double threshold) {
            this(target, points, List.of(points), threshold);
        }
    }

    public record Recognition(Optional<MagicGestureTemplateTarget> target, double confidence,
                              boolean ambiguous) { }

    public Template train(MagicGestureTemplateTarget target, List<List<MagicGesturePoint>> samples) {
        if (samples == null || samples.size() != SAMPLE_COUNT) {
            throw new IllegalArgumentException("A personal gesture needs exactly three samples.");
        }
        List<List<MagicGesturePoint>> normalized = samples.stream().map(this::normalize).toList();
        List<MagicGesturePoint> average = average(normalized);
        double variation = normalized.stream().mapToDouble(sample ->
                MagicGestureTemplateScoring.score(sample, average)).max().orElse(0.0D);
        double threshold = Math.max(MINIMUM_MATCH_THRESHOLD, Math.min(0.38D, 0.10D + variation * 2.4D));
        return new Template(target, average, normalized, threshold);
    }

    public Optional<MagicGestureTemplateTarget> recognize(MagicGestureMode mode,
            List<MagicGesturePoint> points, Collection<Template> templates) {
        Recognition result = recognizeDetailed(mode, points, templates);
        return result.ambiguous() ? Optional.empty() : result.target();
    }

    public Recognition recognizeDetailed(MagicGestureMode mode, List<MagicGesturePoint> points,
                                          Collection<Template> templates) {
        if (!eligible(points)) return new Recognition(Optional.empty(), 0.0D, false);
        long duration = points.get(points.size() - 1).elapsedMillis() - points.get(0).elapsedMillis();
        double path = pathLength(points);
        List<MagicGesturePoint> normalized = normalize(points);
        boolean school = mode == MagicGestureMode.SCHOOL;
        List<Match> matches = templates.stream().filter(template -> template.target().school() == school)
                .filter(template -> allowed(template.target(), path, duration))
                .map(template -> match(normalized, template)).filter(match -> match.ratio() <= 1.0D)
                .sorted(Comparator.comparingDouble(Match::distance)).toList();
        if (matches.isEmpty()) return new Recognition(Optional.empty(), 0.0D, false);
        Match best = matches.get(0);
        boolean ambiguous = matches.size() > 1
                && matches.get(1).distance() - best.distance() < AMBIGUITY_DISTANCE;
        return new Recognition(ambiguous ? Optional.empty() : Optional.of(best.target()),
                Math.max(0.0D, 1.0D - best.ratio()), ambiguous);
    }

    public MagicGestureTrainingFeedback assess(MagicGestureTemplateTarget target,
            List<MagicGesturePoint> points, List<List<MagicGesturePoint>> accepted) {
        if (!eligible(points)) return MagicGestureTrainingFeedback.INVALID;
        long duration = points.get(points.size() - 1).elapsedMillis() - points.get(0).elapsedMillis();
        if (!allowed(target, pathLength(points), duration)) return MagicGestureTrainingFeedback.WRONG_STRUCTURE;
        List<MagicGesturePoint> normalized = normalize(points);
        if (target == MagicGestureTemplateTarget.BLOOD) return MagicGestureTrainingFeedback.ACCEPTED;
        for (List<MagicGesturePoint> previous : accepted) {
            List<MagicGesturePoint> normalizedPrevious = normalize(previous);
            if (!MagicGestureShape.compatible(MagicGestureShape.from(normalized),
                    MagicGestureShape.from(normalizedPrevious))
                    || MagicGestureTemplateScoring.score(normalized, normalizedPrevious) > MAXIMUM_SAMPLE_DISTANCE) {
                return MagicGestureTrainingFeedback.INCONSISTENT;
            }
        }
        return MagicGestureTrainingFeedback.ACCEPTED;
    }

    public double consistency(List<MagicGesturePoint> points, List<List<MagicGesturePoint>> accepted) {
        if (!eligible(points) || accepted.isEmpty()) return 1.0D;
        List<MagicGesturePoint> normalized = normalize(points);
        double worst = accepted.stream().map(this::normalize)
                .mapToDouble(previous -> MagicGestureTemplateScoring.score(normalized, previous))
                .max().orElse(MAXIMUM_SAMPLE_DISTANCE);
        return Math.max(0.0D, Math.min(1.0D, 1.0D - worst / MAXIMUM_SAMPLE_DISTANCE));
    }

    public List<MagicGesturePoint> normalize(List<MagicGesturePoint> points) {
        if (!eligible(points)) throw new IllegalArgumentException("Gesture sample is invalid.");
        List<MagicGesturePoint> sampled = resample(points);
        double centerX = sampled.stream().mapToDouble(MagicGesturePoint::x).average().orElseThrow();
        double centerY = sampled.stream().mapToDouble(MagicGesturePoint::y).average().orElseThrow();
        double minX = sampled.stream().mapToDouble(MagicGesturePoint::x).min().orElseThrow();
        double maxX = sampled.stream().mapToDouble(MagicGesturePoint::x).max().orElseThrow();
        double minY = sampled.stream().mapToDouble(MagicGesturePoint::y).min().orElseThrow();
        double maxY = sampled.stream().mapToDouble(MagicGesturePoint::y).max().orElseThrow();
        double scale = Math.max(maxX - minX, maxY - minY);
        if (scale < 1.0E-6D) scale = 1.0D;
        List<MagicGesturePoint> result = new ArrayList<>(POINT_COUNT);
        for (int index = 0; index < sampled.size(); index++) {
            MagicGesturePoint point = sampled.get(index);
            result.add(new MagicGesturePoint((point.x() - centerX) / scale,
                    (point.y() - centerY) / scale, index));
        }
        return List.copyOf(result);
    }

    private Match match(List<MagicGesturePoint> points, Template template) {
        if (template.target() == MagicGestureTemplateTarget.BLOOD) {
            return new Match(template.target(), 0.0D, 0.0D);
        }
        double value = MagicGestureTemplateScoring.score(points, template.points());
        for (List<MagicGesturePoint> variant : template.variants()) {
            value = Math.min(value, MagicGestureTemplateScoring.score(points, variant));
        }
        double threshold = Math.max(MINIMUM_MATCH_THRESHOLD, template.threshold());
        return new Match(template.target(), value, value / threshold);
    }

    private static boolean allowed(MagicGestureTemplateTarget target, double path, long duration) {
        return target == MagicGestureTemplateTarget.BLOOD ? duration >= 450L && path <= 0.22D
                : path >= MINIMUM_MOVING_PATH;
    }

    private static List<MagicGesturePoint> average(List<List<MagicGesturePoint>> samples) {
        List<MagicGesturePoint> result = new ArrayList<>(POINT_COUNT);
        for (int index = 0; index < POINT_COUNT; index++) {
            int pointIndex = index;
            double x = samples.stream().mapToDouble(sample -> sample.get(pointIndex).x()).average().orElseThrow();
            double y = samples.stream().mapToDouble(sample -> sample.get(pointIndex).y()).average().orElseThrow();
            result.add(new MagicGesturePoint(x, y, index));
        }
        return List.copyOf(result);
    }

    private static List<MagicGesturePoint> resample(List<MagicGesturePoint> points) {
        double length = pathLength(points);
        if (length < 1.0E-8D) return java.util.Collections.nCopies(POINT_COUNT,
                new MagicGesturePoint(points.get(0).x(), points.get(0).y(), 0L));
        List<MagicGesturePoint> result = new ArrayList<>(POINT_COUNT);
        result.add(new MagicGesturePoint(points.get(0).x(), points.get(0).y(), 0L));
        double interval = length / (POINT_COUNT - 1); double target = interval; double walked = 0.0D;
        for (int segment = 1; segment < points.size() && result.size() < POINT_COUNT - 1; segment++) {
            MagicGesturePoint from = points.get(segment - 1); MagicGesturePoint to = points.get(segment);
            double segmentLength = from.distanceTo(to);
            while (segmentLength > 1.0E-8D && walked + segmentLength >= target
                    && result.size() < POINT_COUNT - 1) {
                double ratio = (target - walked) / segmentLength;
                result.add(new MagicGesturePoint(from.x() + (to.x() - from.x()) * ratio,
                        from.y() + (to.y() - from.y()) * ratio, result.size())); target += interval;
            }
            walked += segmentLength;
        }
        MagicGesturePoint last = points.get(points.size() - 1);
        while (result.size() < POINT_COUNT) result.add(new MagicGesturePoint(last.x(), last.y(), result.size()));
        return result;
    }

    private static boolean eligible(List<MagicGesturePoint> points) {
        if (points == null || points.size() < 2 || points.size() > MagicGestureRecognizer.MAXIMUM_POINTS) return false;
        long previous = -1L;
        for (MagicGesturePoint point : points) {
            if (point == null || point.elapsedMillis() < previous
                    || point.elapsedMillis() > MagicGestureRecognizer.MAXIMUM_DURATION_MILLIS) return false;
            previous = point.elapsedMillis();
        }
        return true;
    }

    private static double pathLength(List<MagicGesturePoint> points) {
        double result = 0.0D;
        for (int index = 1; index < points.size(); index++) result += points.get(index - 1).distanceTo(points.get(index));
        return result;
    }

    private record Match(MagicGestureTemplateTarget target, double distance, double ratio) { }
}
