package io.github.kxyr0.echoik.core.solver;

import io.github.kxyr0.echoik.core.math.Quat;
import io.github.kxyr0.echoik.core.math.Vec3;
import io.github.kxyr0.echoik.core.model.Bone;
import io.github.kxyr0.echoik.core.model.Pose;
import io.github.kxyr0.echoik.core.model.SkeletonDefinition;
import io.github.kxyr0.echoik.core.model.Transform;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class TwoBoneSolverTest {
    private static final double EPSILON = 1.0E-5D;
    private static final Vec3 TARGET = new Vec3(6.0D, -6.0D, 3.0D);
    private static final Vec3 POLE = new Vec3(0.0D, 0.0D, -5.0D);

    @Test
    void verticalReachKeepsLengthsPoleSideAndInputPose() {
        Pose input = pose();
        IkResult result = solve(target(TARGET, Optional.of(POLE), 1.0D));

        Assertions.assertEquals(IkStatus.REACHED, result.status(), result.diagnostics().toString());
        assertNear(TARGET, point(result, "hand"));
        Assertions.assertEquals(6.0D, point(result, "upper").distanceTo(point(result, "forearm")), EPSILON);
        Assertions.assertEquals(6.0D, point(result, "forearm").distanceTo(point(result, "hand")), EPSILON);
        Assertions.assertTrue(point(result, "forearm").z() < 0.0D, "Локоть должен уйти в сторону pole target.");
        Assertions.assertEquals(point(input, "hand"), new Vec3(0.0D, -12.0D, 0.0D));
        Assertions.assertEquals(List.of("upper", "forearm"), result.changedBoneIds());
    }

    @Test
    void unreachableTargetClampsMaxReachWithoutStretching() {
        IkResult result = solve(target(new Vec3(30.0D, 0.0D, 0.0D), Optional.of(POLE), 1.0D));

        Assertions.assertEquals(IkStatus.CLAMPED_MAX, result.status());
        Assertions.assertEquals(12.0D, point(result, "upper").distanceTo(point(result, "hand")), EPSILON);
        Assertions.assertEquals(6.0D, point(result, "upper").distanceTo(point(result, "forearm")), EPSILON);
        Assertions.assertEquals(6.0D, point(result, "forearm").distanceTo(point(result, "hand")), EPSILON);
    }

    @Test
    void tooCloseTargetIsStable() {
        IkResult result = solve(target(Vec3.zero(), Optional.of(POLE), 1.0D));

        Assertions.assertEquals(IkStatus.CLAMPED_MIN, result.status());
        Assertions.assertTrue(point(result, "forearm").isFinite());
        Assertions.assertEquals(6.0D, point(result, "upper").distanceTo(point(result, "forearm")), EPSILON);
        Assertions.assertEquals(6.0D, point(result, "forearm").distanceTo(point(result, "hand")), EPSILON);
    }

    @Test
    void weightZeroKeepsInputAndHalfWeightBlendsEffector() {
        Pose input = pose();
        IkResult zero = solve(target(TARGET, Optional.of(POLE), 0.0D));
        IkResult half = solve(target(TARGET, Optional.of(POLE), 0.5D));

        Assertions.assertEquals(input.transforms(), zero.pose().transforms());
        double initialDistance = point(input, "hand").distanceTo(TARGET);
        double halfDistance = point(half, "hand").distanceTo(TARGET);
        Assertions.assertTrue(halfDistance > EPSILON);
        Assertions.assertTrue(halfDistance < initialDistance);
    }

    @Test
    void repeatedSolveIsNumericallyStable() {
        IkResult first = solve(target(TARGET, Optional.of(POLE), 1.0D));
        IkResult second = solve(target(TARGET, Optional.of(POLE), 1.0D));

        assertNear(point(first, "forearm"), point(second, "forearm"));
        assertNear(point(first, "hand"), point(second, "hand"));
    }

    @Test
    void invalidNumbersReturnInvalidWithoutCorruptingPose() {
        Pose input = pose();
        IkResult nan = new TwoBoneSolver().solve(new SolverContext(skeleton(), input,
                target(new Vec3(Double.NaN, 0.0D, 0.0D), Optional.of(POLE), 1.0D)));
        IkResult infinity = new TwoBoneSolver().solve(new SolverContext(skeleton(), input,
                target(new Vec3(Double.POSITIVE_INFINITY, 0.0D, 0.0D), Optional.of(POLE), 1.0D)));

        Assertions.assertEquals(IkStatus.INVALID, nan.status());
        Assertions.assertEquals(IkStatus.INVALID, infinity.status());
        Assertions.assertEquals(input.transforms(), nan.pose().transforms());
        Assertions.assertEquals(input.transforms(), infinity.pose().transforms());
    }

    @Test
    void degeneratePoleTargetReportsDiagnosticAndKeepsPoseFinite() {
        IkResult result = solve(target(TARGET, Optional.of(TARGET), 1.0D));

        Assertions.assertTrue(result.solved(), result.diagnostics().toString());
        Assertions.assertTrue(result.diagnostics().stream().anyMatch(message -> message.contains("pole target")));
        Assertions.assertTrue(point(result, "forearm").isFinite());
        Assertions.assertTrue(point(result, "hand").isFinite());
    }

    private IkResult solve(IkTarget target) {
        return new TwoBoneSolver().solve(new SolverContext(skeleton(), pose(), target));
    }

    private IkTarget target(Vec3 position, Optional<Vec3> pole, double weight) {
        return new IkTarget("upper", "forearm", "hand", position, Optional.of(Quat.identity()),
                pole, weight, 0.0D, 1.0D);
    }

    private SkeletonDefinition skeleton() {
        return new SkeletonDefinition(List.of(
                new Bone("upper", Optional.empty(), Transform.identity(), 6.0D),
                new Bone("forearm", Optional.of("upper"), Transform.identity(), 6.0D),
                new Bone("hand", Optional.of("forearm"), Transform.identity(), 0.0D)));
    }

    private Pose pose() {
        return new Pose(Map.of(
                "upper", Transform.translated(new Vec3(0.0D, 0.0D, 0.0D)),
                "forearm", Transform.translated(new Vec3(0.0D, -6.0D, 0.0D)),
                "hand", Transform.translated(new Vec3(0.0D, -12.0D, 0.0D)),
                "head", Transform.translated(new Vec3(0.0D, 12.0D, 0.0D))));
    }

    private Vec3 point(IkResult result, String boneId) {
        return point(result.pose(), boneId);
    }

    private Vec3 point(Pose pose, String boneId) {
        return pose.requireTransform(boneId).translation();
    }

    private void assertNear(Vec3 expected, Vec3 actual) {
        Assertions.assertEquals(expected.x(), actual.x(), EPSILON);
        Assertions.assertEquals(expected.y(), actual.y(), EPSILON);
        Assertions.assertEquals(expected.z(), actual.z(), EPSILON);
    }
}
