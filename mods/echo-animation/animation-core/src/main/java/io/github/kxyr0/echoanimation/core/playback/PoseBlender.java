package io.github.kxyr0.echoanimation.core.playback;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.math.Matrix4Transform;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Blends two absolute poses with shortest-path Euler angles.
 */
public final class PoseBlender {
    public AnimationPose blend(AnimationPose left, AnimationPose right, double alpha) {
        requireAlpha(alpha);
        Map<BoneId, EulerTransform> result = new LinkedHashMap<>();
        Map<BoneId, Matrix4Transform> matrixResult = new LinkedHashMap<>();
        Set<BoneId> bones = new java.util.LinkedHashSet<>(left.transforms().keySet());
        bones.addAll(right.transforms().keySet());
        bones.addAll(left.matrixTransforms().keySet());
        bones.addAll(right.matrixTransforms().keySet());
        for (BoneId bone : bones) {
            boolean matrix = left.matrixTransform(bone).isPresent() || right.matrixTransform(bone).isPresent();
            if (matrix) {
                Matrix4Transform a = left.matrixTransform(bone).orElseGet(() -> Matrix4Transform.fromEuler(
                        left.transform(bone).orElse(EulerTransform.IDENTITY)));
                Matrix4Transform b = right.matrixTransform(bone).orElseGet(() -> Matrix4Transform.fromEuler(
                        right.transform(bone).orElse(left.transform(bone).orElse(EulerTransform.IDENTITY))));
                matrixResult.put(bone, blend(a, b, alpha));
            } else {
                EulerTransform a = left.transform(bone).orElse(EulerTransform.IDENTITY);
                EulerTransform b = right.transform(bone).orElse(a);
                result.put(bone, blend(a, b, alpha));
            }
        }
        return AnimationPose.of(result, matrixResult);
    }


    public Matrix4Transform blend(Matrix4Transform left, Matrix4Transform right, double alpha) {
        requireAlpha(alpha);
        return Matrix4Transform.lerp(left, right, alpha);
    }

    public EulerTransform blend(EulerTransform left, EulerTransform right, double alpha) {
        requireAlpha(alpha);
        return new EulerTransform(
                lerp(left.translation(), right.translation(), alpha),
                lerpAngles(left.rotationDegrees(), right.rotationDegrees(), alpha),
                lerp(left.scale(), right.scale(), alpha),
                left.pivot());
    }

    private Vec3 lerp(Vec3 left, Vec3 right, double alpha) {
        return new Vec3(
                left.x() + (right.x() - left.x()) * alpha,
                left.y() + (right.y() - left.y()) * alpha,
                left.z() + (right.z() - left.z()) * alpha);
    }

    private Vec3 lerpAngles(Vec3 left, Vec3 right, double alpha) {
        return new Vec3(angle(left.x(), right.x(), alpha), angle(left.y(), right.y(), alpha),
                angle(left.z(), right.z(), alpha));
    }

    private double angle(double left, double right, double alpha) {
        double delta = ((right - left + 540.0D) % 360.0D) - 180.0D;
        return left + delta * alpha;
    }

    private void requireAlpha(double alpha) {
        if (!Double.isFinite(alpha) || alpha < 0.0D || alpha > 1.0D) {
            throw new IllegalArgumentException("alpha must be finite in range 0..1.");
        }
    }
}
