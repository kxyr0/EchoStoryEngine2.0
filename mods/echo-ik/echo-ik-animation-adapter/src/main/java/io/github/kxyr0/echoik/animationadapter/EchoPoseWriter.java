package io.github.kxyr0.echoik.animationadapter;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationBoneDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoik.core.math.Quat;
import io.github.kxyr0.echoik.core.math.Vec3;
import io.github.kxyr0.echoik.core.model.Pose;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Writes IK solver output back as local Echo Animation render overlay transforms.
 */
public final class EchoPoseWriter {
    private static final Vec3 DEFAULT_AXIS = new Vec3(0.0D, 1.0D, 0.0D);

    public Optional<Map<BoneId, EulerTransform>> write(
            AnimationRigDefinition rig,
            Map<BoneId, EulerTransform> localTransforms,
            Pose solvedPose,
            EchoIkChain chain
    ) {
        Map<BoneId, AnimationBoneDefinition> definitions = definitions(rig);
        BoneId rootId = BoneId.parse(chain.rootBoneId());
        BoneId midId = BoneId.parse(chain.midBoneId());
        if (!definitions.containsKey(rootId) || !definitions.containsKey(midId)) {
            return Optional.empty();
        }
        Vec3 root = solvedPose.requireTransform(chain.rootBoneId()).translation();
        Vec3 mid = solvedPose.requireTransform(chain.midBoneId()).translation();
        Vec3 end = solvedPose.requireTransform(chain.endEffectorId()).translation();
        Vec3 upper = mid.subtract(root);
        Vec3 lower = end.subtract(mid);
        if (upper.nearZero() || lower.nearZero()) {
            return Optional.empty();
        }
        Map<BoneId, EulerTransform> output = new LinkedHashMap<>(localTransforms);
        io.github.kxyr0.echoanimation.api.math.Vec3 rootWorld = eulerForDirection(upper);
        io.github.kxyr0.echoanimation.api.math.Vec3 lowerWorld = eulerForDirection(lower);
        EulerTransform rootLocal = local(rootId, definitions, localTransforms);
        EulerTransform midLocal = local(midId, definitions, localTransforms);
        output.put(rootId, withRotation(rootLocal,
                rootWorld.minus(parentWorldRotation(rootId, definitions, localTransforms))));
        output.put(midId, withRotation(midLocal, lowerWorld.minus(rootWorld)));
        return Optional.of(Map.copyOf(output));
    }

    private Map<BoneId, AnimationBoneDefinition> definitions(AnimationRigDefinition rig) {
        Map<BoneId, AnimationBoneDefinition> result = new LinkedHashMap<>();
        rig.bones().forEach(bone -> result.put(bone.id(), bone));
        return result;
    }

    private EulerTransform local(
            BoneId id,
            Map<BoneId, AnimationBoneDefinition> definitions,
            Map<BoneId, EulerTransform> localTransforms
    ) {
        return localTransforms.getOrDefault(id, definitions.get(id).defaultLocalTransform());
    }

    private io.github.kxyr0.echoanimation.api.math.Vec3 parentWorldRotation(
            BoneId id,
            Map<BoneId, AnimationBoneDefinition> definitions,
            Map<BoneId, EulerTransform> localTransforms
    ) {
        return definitions.get(id).parentId()
                .map(parent -> worldRotation(parent, definitions, localTransforms))
                .orElse(io.github.kxyr0.echoanimation.api.math.Vec3.ZERO);
    }

    private io.github.kxyr0.echoanimation.api.math.Vec3 worldRotation(
            BoneId id,
            Map<BoneId, AnimationBoneDefinition> definitions,
            Map<BoneId, EulerTransform> localTransforms
    ) {
        return parentWorldRotation(id, definitions, localTransforms)
                .plus(local(id, definitions, localTransforms).rotationDegrees());
    }

    private EulerTransform withRotation(
            EulerTransform source,
            io.github.kxyr0.echoanimation.api.math.Vec3 rotation
    ) {
        return new EulerTransform(source.translation(), rotation, source.scale(), source.pivot());
    }

    private io.github.kxyr0.echoanimation.api.math.Vec3 eulerForDirection(Vec3 direction) {
        Quat rotation = Quat.fromUnitVectors(DEFAULT_AXIS, direction.normalize());
        return euler(rotation.normalized());
    }

    private io.github.kxyr0.echoanimation.api.math.Vec3 euler(Quat value) {
        double sinX = 2.0D * (value.w() * value.x() + value.y() * value.z());
        double cosX = 1.0D - 2.0D * (value.x() * value.x() + value.y() * value.y());
        double y = Math.asin(clamp(2.0D * (value.w() * value.y() - value.z() * value.x())));
        double sinZ = 2.0D * (value.w() * value.z() + value.x() * value.y());
        double cosZ = 1.0D - 2.0D * (value.y() * value.y() + value.z() * value.z());
        return new io.github.kxyr0.echoanimation.api.math.Vec3(
                Math.toDegrees(Math.atan2(sinX, cosX)),
                Math.toDegrees(y),
                Math.toDegrees(Math.atan2(sinZ, cosZ)));
    }

    private double clamp(double value) {
        return Math.max(-1.0D, Math.min(1.0D, value));
    }
}
