package io.github.kxyr0.echoik.core.solver;

import io.github.kxyr0.echoik.core.math.Quat;
import io.github.kxyr0.echoik.core.math.Vec3;
import io.github.kxyr0.echoik.core.model.Pose;
import io.github.kxyr0.echoik.core.model.Transform;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analytic two-bone IK solver for arbitrary chains.
 */
public final class TwoBoneSolver {
    private static final double EPSILON = 1.0E-6D;

    public IkResult solve(SolverContext context) {
        Pose pose = context.currentPose();
        IkTarget target = context.target();
        List<String> diagnostics = new ArrayList<>();
        try {
            Transform root = pose.requireTransform(target.rootBoneId());
            Transform mid = pose.requireTransform(target.midBoneId());
            Transform end = pose.requireTransform(target.endBoneId());
            return solveKnown(context, root, mid, end, diagnostics);
        } catch (IllegalArgumentException exception) {
            diagnostics.add("IK-ошибка: " + exception.getMessage());
            return IkResult.unsolved(pose, diagnostics);
        }
    }

    private IkResult solveKnown(
            SolverContext context,
            Transform root,
            Transform mid,
            Transform end,
            List<String> diagnostics
    ) {
        IkTarget target = context.target();
        if (!validInput(root, mid, end, target, diagnostics)) {
            return IkResult.unsolved(context.currentPose(), diagnostics);
        }
        if (target.weight() <= EPSILON) {
            diagnostics.add("IK вес равен 0: возвращена исходная pose.");
            return new IkResult(context.currentPose(), IkStatus.REACHED, List.of(), diagnostics);
        }
        double upper = length(context, target.rootBoneId(), root.translation(), mid.translation());
        double lower = length(context, target.midBoneId(), mid.translation(), end.translation());
        if (upper <= EPSILON || lower <= EPSILON) {
            diagnostics.add("IK цепь невалидна: длина сегмента слишком мала.");
            return IkResult.unsolved(context.currentPose(), diagnostics);
        }
        ReachEnd reach = constrainedEnd(root.translation(), end.translation(), target, upper, lower, diagnostics);
        Vec3 solvedMid = solvedMiddle(root.translation(), mid.translation(), end.translation(),
                reach.position(), upper, lower, target, diagnostics);
        return solvedPose(context, root, mid, end, reach, solvedMid, diagnostics);
    }

    private IkResult solvedPose(
            SolverContext context,
            Transform root,
            Transform mid,
            Transform end,
            ReachEnd reach,
            Vec3 solvedMid,
            List<String> diagnostics
    ) {
        IkTarget target = context.target();
        double positionBlend = target.weight() * target.positionWeight();
        double rotationBlend = target.weight() * target.rotationWeight();
        Transform nextRoot = root.withRotation(aim(root.translation(), solvedMid, root.rotation(), rotationBlend));
        Transform nextMid = mid.withTranslation(mid.translation().lerp(solvedMid, positionBlend))
                .withRotation(aim(solvedMid, reach.position(), mid.rotation(), rotationBlend));
        Transform nextEnd = end.withTranslation(end.translation().lerp(reach.position(), positionBlend));
        if (target.targetRotation().isPresent()) {
            Quat rotation = nextEnd.rotation().slerp(target.targetRotation().orElseThrow(), rotationBlend);
            nextEnd = nextEnd.withRotation(rotation);
        }
        Map<String, Transform> updates = new LinkedHashMap<>();
        updates.put(target.rootBoneId(), nextRoot);
        updates.put(target.midBoneId(), nextMid);
        updates.put(target.endBoneId(), nextEnd);
        return new IkResult(context.currentPose().withTransforms(updates), reach.status(),
                List.of(target.rootBoneId(), target.midBoneId()), diagnostics);
    }

    private boolean validInput(Transform root, Transform mid, Transform end, IkTarget target,
            List<String> diagnostics) {
        if (!root.isFinite() || !mid.isFinite() || !end.isFinite()) {
            diagnostics.add("IK вход невалиден: pose содержит NaN или infinity.");
            return false;
        }
        if (!target.targetPosition().isFinite()) {
            diagnostics.add("IK вход невалиден: target position содержит NaN или infinity.");
            return false;
        }
        if (target.poleTarget().isPresent() && !target.poleTarget().orElseThrow().isFinite()) {
            diagnostics.add("IK вход невалиден: pole target содержит NaN или infinity.");
            return false;
        }
        return true;
    }

    private double length(SolverContext context, String boneId, Vec3 from, Vec3 to) {
        return context.skeleton().boneLength(boneId, from.distanceTo(to));
    }

    private ReachEnd constrainedEnd(Vec3 root, Vec3 currentEnd, IkTarget target, double upper, double lower,
            List<String> diagnostics) {
        Vec3 offset = target.targetPosition().subtract(root);
        double distance = offset.length();
        double maxReach = upper + lower;
        double minReach = Math.abs(upper - lower);
        if (distance <= EPSILON) {
            diagnostics.add("IK target слишком близко к корню цепи: использована стабильная fallback-ось.");
            return new ReachEnd(root.add(fallbackDirection(currentEnd.subtract(root)).multiply(minReach)),
                    IkStatus.CLAMPED_MIN);
        }
        if (distance > maxReach) {
            diagnostics.add("IK target вне досягаемости: цель ограничена максимальной длиной цепи.");
            return new ReachEnd(root.add(offset.normalize().multiply(maxReach)), IkStatus.CLAMPED_MAX);
        }
        if (distance < minReach) {
            diagnostics.add("IK target ближе минимальной длины цепи: цель ограничена без растяжения костей.");
            return new ReachEnd(root.add(offset.normalize().multiply(minReach)), IkStatus.CLAMPED_MIN);
        }
        return new ReachEnd(target.targetPosition(), IkStatus.REACHED);
    }

    private Vec3 solvedMiddle(Vec3 root, Vec3 currentMid, Vec3 currentEnd, Vec3 end, double upper, double lower,
            IkTarget target, List<String> diagnostics) {
        Vec3 direction = fallbackDirection(end.subtract(root), currentEnd.subtract(root));
        double distance = Math.max(EPSILON, root.distanceTo(end));
        double x = (distance * distance + upper * upper - lower * lower) / (2.0D * distance);
        double height = Math.sqrt(Math.max(0.0D, upper * upper - x * x));
        Vec3 plane = polePlane(root, currentMid, direction, target, diagnostics);
        return root.add(direction.multiply(x)).add(plane.multiply(height));
    }

    private Vec3 polePlane(Vec3 root, Vec3 currentMid, Vec3 direction, IkTarget target, List<String> diagnostics) {
        Vec3 pole = target.poleTarget().orElse(currentMid).subtract(root);
        Vec3 plane = pole.subtract(direction.multiply(pole.dot(direction)));
        if (!plane.nearZero()) {
            return plane.normalize();
        }
        diagnostics.add("IK pole target вырожден: использована резервная плоскость сгиба.");
        return orthogonal(direction);
    }

    private Quat aim(Vec3 from, Vec3 to, Quat current, double blend) {
        Vec3 direction = to.subtract(from);
        if (direction.nearZero() || blend <= 0.0D) {
            return current;
        }
        return current.slerp(Quat.fromUnitVectors(Vec3.unitZ(), direction.normalize()), blend);
    }

    private Vec3 fallbackDirection(Vec3 preferred) {
        return fallbackDirection(preferred, new Vec3(0.0D, -1.0D, 0.0D));
    }

    private Vec3 fallbackDirection(Vec3 preferred, Vec3 fallback) {
        return preferred.nearZero() ? fallback.normalize() : preferred.normalize();
    }

    private Vec3 orthogonal(Vec3 direction) {
        Vec3 axis = Math.abs(direction.x()) < 0.8D ? Vec3.unitX() : Vec3.unitY();
        return direction.cross(axis).normalize();
    }

    private record ReachEnd(Vec3 position, IkStatus status) {
    }
}
