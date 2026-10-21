package io.github.kxyr0.echoanimation.core.playback;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.math.Matrix4Transform;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationBoneDefinition;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable local pose. Legacy Euler tracks and exact matrix tracks may coexist; matrix tracks win for rendering.
 */
public final class AnimationPose {
    public static final AnimationPose EMPTY = new AnimationPose(Map.of(), Map.of());

    private final Map<BoneId, EulerTransform> transforms;
    private final Map<BoneId, Matrix4Transform> matrixTransforms;

    public AnimationPose(Map<BoneId, EulerTransform> transforms) {
        this(transforms, Map.of());
    }

    private AnimationPose(
            Map<BoneId, EulerTransform> transforms,
            Map<BoneId, Matrix4Transform> matrixTransforms
    ) {
        this.transforms = Map.copyOf(Objects.requireNonNull(transforms, "transforms"));
        this.matrixTransforms = Map.copyOf(Objects.requireNonNull(matrixTransforms, "matrixTransforms"));
    }

    public static AnimationPose matrices(Map<BoneId, Matrix4Transform> transforms) {
        return new AnimationPose(Map.of(), transforms);
    }

    public static AnimationPose of(
            Map<BoneId, EulerTransform> transforms,
            Map<BoneId, Matrix4Transform> matrixTransforms
    ) {
        return new AnimationPose(transforms, matrixTransforms);
    }

    public Map<BoneId, EulerTransform> transforms() {
        return transforms;
    }

    public Map<BoneId, Matrix4Transform> matrixTransforms() {
        return matrixTransforms;
    }

    public Optional<EulerTransform> transform(BoneId boneId) {
        return Optional.ofNullable(transforms.get(boneId));
    }

    public Optional<Matrix4Transform> matrixTransform(BoneId boneId) {
        return Optional.ofNullable(matrixTransforms.get(boneId));
    }

    public Matrix4Transform localMatrix(BoneId boneId, EulerTransform fallback) {
        Matrix4Transform exact = matrixTransforms.get(boneId);
        if (exact != null) {
            return exact;
        }
        return Matrix4Transform.fromEuler(transforms.getOrDefault(boneId, fallback));
    }

    public static AnimationPose defaults(Collection<AnimationBoneDefinition> bones) {
        Map<BoneId, EulerTransform> result = new LinkedHashMap<>();
        for (AnimationBoneDefinition bone : bones) {
            result.put(bone.id(), bone.defaultLocalTransform());
        }
        return new AnimationPose(result);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AnimationPose pose)) {
            return false;
        }
        return transforms.equals(pose.transforms) && matrixTransforms.equals(pose.matrixTransforms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transforms, matrixTransforms);
    }

    @Override
    public String toString() {
        return "AnimationPose[transforms=" + transforms + ", matrixTransforms=" + matrixTransforms + ']';
    }
}
