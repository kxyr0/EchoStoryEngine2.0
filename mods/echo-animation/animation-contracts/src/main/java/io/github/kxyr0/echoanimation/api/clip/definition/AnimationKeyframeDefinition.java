package io.github.kxyr0.echoanimation.api.clip.definition;

import io.github.kxyr0.echoanimation.api.math.Matrix4Transform;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.InterpolationKind;
import java.util.Objects;
import java.util.Optional;

/**
 * Normalized visual keyframe with optional transform components.
 */
public record AnimationKeyframeDefinition(
        double progress,
        Optional<Vec3> translation,
        Optional<Vec3> rotationDegrees,
        Optional<Vec3> scale,
        Optional<Matrix4Transform> localMatrix,
        InterpolationKind interpolation
) {
    public AnimationKeyframeDefinition {
        translation = Objects.requireNonNull(translation, "translation");
        rotationDegrees = Objects.requireNonNull(rotationDegrees, "rotationDegrees");
        scale = Objects.requireNonNull(scale, "scale");
        localMatrix = Objects.requireNonNull(localMatrix, "localMatrix");
        interpolation = Objects.requireNonNull(interpolation, "interpolation");
        if (!Double.isFinite(progress) || progress < 0.0D || progress > 1.0D) {
            throw new IllegalArgumentException("Keyframe progress must be finite in range 0..1.");
        }
        scale.ifPresent(AnimationKeyframeDefinition::requireScale);
        if (localMatrix.isPresent() && (translation.isPresent() || rotationDegrees.isPresent() || scale.isPresent())) {
            throw new IllegalArgumentException("Matrix keyframes cannot also declare Position/Rotation/Scale.");
        }
    }

    public AnimationKeyframeDefinition(
            double progress,
            Optional<Vec3> translation,
            Optional<Vec3> rotationDegrees,
            Optional<Vec3> scale,
            InterpolationKind interpolation
    ) {
        this(progress, translation, rotationDegrees, scale, Optional.empty(), interpolation);
    }

    private static void requireScale(Vec3 value) {
        if (value.x() == 0.0D || value.y() == 0.0D || value.z() == 0.0D) {
            throw new IllegalArgumentException("Scale keyframe components must not be zero.");
        }
    }
}
