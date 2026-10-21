package io.github.kxyr0.echoanimation.forge.client.humanoid;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationBoneDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Moves a named humanoid pose between rigs while preserving the target bind skeleton. */
final class HumanoidPoseRetargeter {
    AnimationPose retarget(AnimationRigDefinition source, AnimationRigDefinition target, AnimationPose pose) {
        Map<BoneId, AnimationBoneDefinition> sourceBones = source.bones().stream()
                .collect(Collectors.toMap(AnimationBoneDefinition::id, Function.identity()));
        Map<BoneId, EulerTransform> result = new LinkedHashMap<>();
        for (AnimationBoneDefinition targetBone : target.bones()) {
            AnimationBoneDefinition sourceBone = sourceBones.get(targetBone.id());
            if (sourceBone == null) {
                result.put(targetBone.id(), targetBone.defaultLocalTransform());
                continue;
            }
            EulerTransform sourceBind = sourceBone.defaultLocalTransform();
            EulerTransform actual = pose.transform(targetBone.id()).orElse(sourceBind);
            result.put(targetBone.id(), applyDelta(targetBone.defaultLocalTransform(), sourceBind, actual));
        }
        return new AnimationPose(result);
    }

    private EulerTransform applyDelta(EulerTransform target, EulerTransform source, EulerTransform actual) {
        return new EulerTransform(
                target.translation().plus(actual.translation().minus(source.translation())),
                target.rotationDegrees().plus(actual.rotationDegrees().minus(source.rotationDegrees())),
                multiply(target.scale(), divide(actual.scale(), source.scale())),
                target.pivot().plus(actual.pivot().minus(source.pivot())));
    }

    private Vec3 divide(Vec3 left, Vec3 right) {
        return new Vec3(left.x() / right.x(), left.y() / right.y(), left.z() / right.z());
    }

    private Vec3 multiply(Vec3 left, Vec3 right) {
        return left.componentTimes(right);
    }
}
