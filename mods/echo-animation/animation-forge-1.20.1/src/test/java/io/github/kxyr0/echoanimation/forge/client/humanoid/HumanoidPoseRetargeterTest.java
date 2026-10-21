package io.github.kxyr0.echoanimation.forge.client.humanoid;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationBoneDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigKind;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationSkinVariant;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class HumanoidPoseRetargeterTest {
    private static final BoneId BODY = BoneId.parse("body");

    @Test
    void preservesAnimatedDeltaInsteadOfSourceBindTranslation() {
        AnimationRigDefinition source = rig("test:source", bone(BODY, 12));
        AnimationRigDefinition target = rig("test:target", bone(BODY, 0));
        EulerTransform animated = new EulerTransform(new Vec3(0, 13, 0), new Vec3(10, 0, 0),
                new Vec3(2, 1, 1), Vec3.ZERO);

        EulerTransform result = new HumanoidPoseRetargeter()
                .retarget(source, target, new AnimationPose(Map.of(BODY, animated)))
                .transform(BODY).orElseThrow();

        Assertions.assertEquals(new Vec3(0, 1, 0), result.translation());
        Assertions.assertEquals(new Vec3(10, 0, 0), result.rotationDegrees());
        Assertions.assertEquals(new Vec3(2, 1, 1), result.scale());
    }

    private AnimationRigDefinition rig(String id, AnimationBoneDefinition... bones) {
        return new AnimationRigDefinition(AnimationRigId.parse(id), 1, AnimationRigKind.SKINNED_MESH,
                AnimationSkinVariant.CLASSIC, List.of(bones), List.of(), List.of(), id);
    }

    private AnimationBoneDefinition bone(BoneId id, double y) {
        return new AnimationBoneDefinition(id, Optional.empty(),
                EulerTransform.IDENTITY.withTranslation(new Vec3(0, y, 0)), Optional.empty());
    }
}
