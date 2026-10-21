package io.github.kxyr0.echomagic.core.gesture.template;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureTemplateTarget;
import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;
import java.util.List;

record MagicGestureShape(Kind kind, double directness, double area, double turn) {
    enum Kind { POINT, LOOP, RETURN, STRAIGHT, CURVE }

    static MagicGestureShape from(List<MagicGesturePoint> points) {
        double path = path(points);
        if (path < 1.0E-7D) return new MagicGestureShape(Kind.POINT, 0.0D, 0.0D, 0.0D);
        double direct = points.get(0).distanceTo(points.get(points.size() - 1)) / path;
        double area = Math.abs(area(points));
        double turn = turn(points);
        Kind kind;
        if (direct <= 0.30D && area >= 0.08D) kind = Kind.LOOP;
        else if (direct <= 0.30D) kind = Kind.RETURN;
        else if (direct >= 0.88D && turn <= 0.75D) kind = Kind.STRAIGHT;
        else kind = Kind.CURVE;
        return new MagicGestureShape(kind, direct, area, turn);
    }

    static boolean compatible(MagicGestureShape first, MagicGestureShape second) {
        return first.kind == second.kind;
    }

    static boolean validTarget(MagicGestureTemplateTarget target, List<MagicGesturePoint> raw,
                               List<MagicGesturePoint> normalized) {
        long duration = raw.get(raw.size() - 1).elapsedMillis() - raw.get(0).elapsedMillis();
        double rawPath = path(raw);
        if (target == MagicGestureTemplateTarget.BLOOD) return duration >= 450L && rawPath <= 0.22D;
        if (rawPath < 0.12D) return false;
        MagicGestureShape shape = from(normalized);
        MagicGesturePoint first = normalized.get(0);
        MagicGesturePoint last = normalized.get(normalized.size() - 1);
        double dx = last.x() - first.x();
        double dy = last.y() - first.y();
        return switch (target) {
            case FIRE -> (shape.kind == Kind.STRAIGHT || shape.kind == Kind.CURVE)
                    && dy > 0.42D && Math.abs(dy) > Math.abs(dx) * 1.05D;
            case WATER -> shape.kind == Kind.CURVE && shape.turn >= 0.55D;
            case EARTH -> shape.kind == Kind.CURVE || shape.kind == Kind.RETURN;
            case AIR, CIRCLE, WEAPON -> shape.kind == Kind.LOOP;
            case VOID -> shape.kind == Kind.RETURN;
            case LINE -> shape.kind == Kind.STRAIGHT;
            case ARC -> shape.kind == Kind.CURVE && shape.directness >= 0.42D;
            case HOOK -> shape.kind == Kind.CURVE && shape.turn >= 0.45D;
            case BLOOD -> false;
        };
    }

    private static double path(List<MagicGesturePoint> points) {
        double result = 0.0D;
        for (int index = 1; index < points.size(); index++) {
            result += points.get(index - 1).distanceTo(points.get(index));
        }
        return result;
    }

    private static double area(List<MagicGesturePoint> points) {
        double result = 0.0D;
        for (int index = 0; index < points.size(); index++) {
            MagicGesturePoint current = points.get(index);
            MagicGesturePoint next = points.get((index + 1) % points.size());
            result += current.x() * next.y() - next.x() * current.y();
        }
        return result * 0.5D;
    }

    private static double turn(List<MagicGesturePoint> points) {
        double result = 0.0D;
        for (int index = 2; index < points.size(); index++) {
            MagicGesturePoint a = points.get(index - 2);
            MagicGesturePoint b = points.get(index - 1);
            MagicGesturePoint c = points.get(index);
            double ax = b.x() - a.x(); double ay = b.y() - a.y();
            double bx = c.x() - b.x(); double by = c.y() - b.y();
            if (Math.hypot(ax, ay) > 1.0E-8D && Math.hypot(bx, by) > 1.0E-8D) {
                result += Math.abs(Math.atan2(ax * by - ay * bx, ax * bx + ay * by));
            }
        }
        return result;
    }
}
