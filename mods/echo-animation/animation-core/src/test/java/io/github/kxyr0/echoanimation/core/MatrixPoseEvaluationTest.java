package io.github.kxyr0.echoanimation.core;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.math.Matrix4Transform;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationBoneDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigKind;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationSkinVariant;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.core.rig.RigPoseEvaluator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class MatrixPoseEvaluationTest {
    @Test
    void exactMatrixPosePreservesShear() {
        BoneId root = BoneId.parse("root");
        Matrix4Transform shear = Matrix4Transform.fromRowMajor(
                1.0D, 0.25D, 0.0D, 2.0D,
                0.0D, 1.0D, 0.0D, 3.0D,
                0.0D, 0.0D, 1.0D, 0.0D,
                0.0D, 0.0D, 0.0D, 1.0D);
        AnimationPose pose = AnimationPose.matrices(Map.of(root, shear));

        Matrix4Transform evaluated = new RigPoseEvaluator()
                .evaluateMatrices(rig(List.of(bone(root, Optional.empty()))), pose)
                .bone(root)
                .orElseThrow();
        Vec3 point = evaluated.transformPoint(new Vec3(0.0D, 4.0D, 0.0D));

        Assertions.assertEquals(3.0D, point.x(), 0.000001D);
        Assertions.assertEquals(7.0D, point.y(), 0.000001D);
    }

    @Test
    void arbitrary164BoneHierarchyComposesWithoutHumanoidAssumptions() {
        int boneCount = 164;
        List<AnimationBoneDefinition> bones = new ArrayList<>();
        Map<BoneId, Matrix4Transform> matrices = new LinkedHashMap<>();
        BoneId previous = null;
        for (int index = 0; index < boneCount; index++) {
            BoneId current = BoneId.parse("bone_" + index);
            bones.add(bone(current, Optional.ofNullable(previous)));
            matrices.put(current, translationX(1.0D));
            previous = current;
        }

        Matrix4Transform end = new RigPoseEvaluator()
                .evaluateMatrices(rig(bones), AnimationPose.matrices(matrices))
                .bone(previous)
                .orElseThrow();

        Assertions.assertEquals(164.0D, end.transformPoint(Vec3.ZERO).x(), 0.000001D);
    }

    @Test
    void exactMatrixOverridesLegacyEulerOnSameBone() {
        BoneId root = BoneId.parse("root");
        AnimationPose pose = AnimationPose.of(
                Map.of(root, EulerTransform.IDENTITY.withTranslation(new Vec3(99.0D, 0.0D, 0.0D))),
                Map.of(root, translationX(4.0D)));

        Matrix4Transform evaluated = new RigPoseEvaluator()
                .evaluateMatrices(rig(List.of(bone(root, Optional.empty()))), pose)
                .bone(root)
                .orElseThrow();

        Assertions.assertEquals(4.0D, evaluated.transformPoint(Vec3.ZERO).x(), 0.000001D);
    }

    private AnimationBoneDefinition bone(BoneId id, Optional<BoneId> parent) {
        return new AnimationBoneDefinition(id, parent, EulerTransform.IDENTITY, Optional.empty());
    }

    private AnimationRigDefinition rig(List<AnimationBoneDefinition> bones) {
        return new AnimationRigDefinition(AnimationRigId.parse("test:matrix_rig"), 1L,
                AnimationRigKind.CUSTOM, AnimationSkinVariant.CLASSIC,
                bones, List.of(), List.of(), "matrix-test");
    }

    private Matrix4Transform translationX(double x) {
        return Matrix4Transform.fromRowMajor(
                1.0D, 0.0D, 0.0D, x,
                0.0D, 1.0D, 0.0D, 0.0D,
                0.0D, 0.0D, 1.0D, 0.0D,
                0.0D, 0.0D, 0.0D, 1.0D);
    }
}
