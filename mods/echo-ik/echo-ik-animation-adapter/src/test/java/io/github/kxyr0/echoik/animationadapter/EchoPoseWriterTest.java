package io.github.kxyr0.echoik.animationadapter;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.rig.AnimationAttachmentSocketId;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationAttachmentSocketDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationBoneDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigKind;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationSkinVariant;
import io.github.kxyr0.echoik.core.math.Vec3;
import io.github.kxyr0.echoik.core.model.Pose;
import io.github.kxyr0.echoik.core.model.Transform;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class EchoPoseWriterTest {
    private static final BoneId UPPER = BoneId.parse("right_upper_arm");
    private static final BoneId FOREARM = BoneId.parse("right_forearm");
    private static final AnimationAttachmentSocketId HAND = AnimationAttachmentSocketId.parse("main_hand");
    private static final EchoIkChain CHAIN = new EchoIkChain(UPPER.value(), FOREARM.value(), HAND.value());

    @Test
    void modelUpperArmAxisPointsTowardSolvedPositiveZDirection() {
        Map<BoneId, EulerTransform> output = new EchoPoseWriter()
                .write(rig(), Map.of(), solvedPositiveZPose(), CHAIN)
                .orElseThrow();

        Vec3 visibleUpperAxis = modelAxis(output.get(UPPER).rotationDegrees());

        assertNear(new Vec3(0.0D, 0.0D, 1.0D), visibleUpperAxis);
    }

    private Pose solvedPositiveZPose() {
        return new Pose(Map.of(
                UPPER.value(), Transform.translated(new Vec3(0.0D, 0.0D, 0.0D)),
                FOREARM.value(), Transform.translated(new Vec3(0.0D, 0.0D, 6.0D)),
                HAND.value(), Transform.translated(new Vec3(0.0D, 0.0D, 12.0D))));
    }

    private AnimationRigDefinition rig() {
        return new AnimationRigDefinition(AnimationRigId.parse("jdh:ik_test"), 1L,
                AnimationRigKind.SEGMENTED_HUMANOID, AnimationSkinVariant.CLASSIC,
                List.of(
                        new AnimationBoneDefinition(UPPER, Optional.empty(), transform(0.0D, 0.0D, 0.0D),
                                Optional.empty()),
                        new AnimationBoneDefinition(FOREARM, Optional.of(UPPER), transform(0.0D, -6.0D, 0.0D),
                                Optional.empty())),
                List.of(), List.of(new AnimationAttachmentSocketDefinition(HAND, FOREARM,
                transform(0.0D, -6.0D, 0.0D))), "ik-test");
    }

    private EulerTransform transform(double x, double y, double z) {
        return EulerTransform.IDENTITY.withTranslation(new io.github.kxyr0.echoanimation.api.math.Vec3(x, y, z));
    }

    private Vec3 modelAxis(io.github.kxyr0.echoanimation.api.math.Vec3 rotation) {
        io.github.kxyr0.echoanimation.api.math.Vec3 rotated = new EulerTransform(
                io.github.kxyr0.echoanimation.api.math.Vec3.ZERO,
                rotation,
                io.github.kxyr0.echoanimation.api.math.Vec3.ONE,
                io.github.kxyr0.echoanimation.api.math.Vec3.ZERO)
                .applyPoint(new io.github.kxyr0.echoanimation.api.math.Vec3(0.0D, 1.0D, 0.0D));
        return new Vec3(rotated.x(), rotated.y(), rotated.z()).normalize();
    }

    private void assertNear(Vec3 expected, Vec3 actual) {
        Assertions.assertEquals(expected.x(), actual.x(), 0.0001D);
        Assertions.assertEquals(expected.y(), actual.y(), 0.0001D);
        Assertions.assertEquals(expected.z(), actual.z(), 0.0001D);
    }
}
