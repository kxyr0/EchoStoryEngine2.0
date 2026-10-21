package io.github.kxyr0.echoik.animationadapter;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationBoneDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoik.core.math.Quat;
import io.github.kxyr0.echoik.core.math.Vec3;
import io.github.kxyr0.echoik.core.model.Bone;
import io.github.kxyr0.echoik.core.model.Pose;
import io.github.kxyr0.echoik.core.model.SkeletonDefinition;
import io.github.kxyr0.echoik.core.model.Transform;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads Echo Animation public local transforms into solver-space IK data.
 */
public final class EchoPoseReader {
    public Pose read(AnimationRigDefinition rig, Map<BoneId, EulerTransform> localTransforms) {
        Map<BoneId, AnimationBoneDefinition> definitions = definitions(rig);
        Map<BoneId, EulerTransform> world = new LinkedHashMap<>();
        for (AnimationBoneDefinition bone : rig.bones()) {
            resolve(bone, definitions, localTransforms, world);
        }
        rig.sockets().forEach(socket -> world.put(BoneId.parse(socket.id().value()),
                compose(world.get(socket.parentBoneId()), socket.localTransform())));
        Map<String, Transform> result = new LinkedHashMap<>();
        world.forEach((id, transform) -> result.put(id.value(), transform(transform)));
        return new Pose(result);
    }

    public SkeletonDefinition skeleton(AnimationRigDefinition rig) {
        Map<String, Double> lengths = lengths(rig);
        List<Bone> bones = rig.bones().stream()
                .map(bone -> new Bone(
                        bone.id().value(),
                        bone.parentId().map(BoneId::value),
                        transform(bone.defaultLocalTransform()),
                        lengths.getOrDefault(bone.id().value(), 0.0D)))
                .toList();
        return new SkeletonDefinition(bones);
    }

    private EulerTransform resolve(
            AnimationBoneDefinition bone,
            Map<BoneId, AnimationBoneDefinition> definitions,
            Map<BoneId, EulerTransform> localTransforms,
            Map<BoneId, EulerTransform> world
    ) {
        if (world.containsKey(bone.id())) {
            return world.get(bone.id());
        }
        EulerTransform local = localTransforms.getOrDefault(bone.id(), bone.defaultLocalTransform());
        EulerTransform value = bone.parentId()
                .map(parent -> compose(resolve(definitions.get(parent), definitions, localTransforms, world), local))
                .orElse(local);
        world.put(bone.id(), value);
        return value;
    }

    private Map<BoneId, AnimationBoneDefinition> definitions(AnimationRigDefinition rig) {
        Map<BoneId, AnimationBoneDefinition> result = new LinkedHashMap<>();
        rig.bones().forEach(bone -> result.put(bone.id(), bone));
        return result;
    }

    private Map<String, Double> lengths(AnimationRigDefinition rig) {
        Map<String, Double> result = new LinkedHashMap<>();
        rig.bones().forEach(bone -> bone.parentId().ifPresent(parent -> result.merge(parent.value(),
                length(bone.defaultLocalTransform().translation()), Math::max)));
        rig.sockets().forEach(socket -> result.merge(socket.parentBoneId().value(),
                length(socket.localTransform().translation()), Math::max));
        return result;
    }

    private EulerTransform compose(EulerTransform parent, EulerTransform child) {
        return new EulerTransform(
                parent.applyPoint(child.translation()),
                parent.rotationDegrees().plus(child.rotationDegrees()),
                parent.scale().componentTimes(child.scale()),
                child.pivot());
    }

    private Transform transform(EulerTransform transform) {
        return new Transform(vec(transform.translation()), rotation(transform.rotationDegrees()), vec(transform.scale()));
    }

    private Vec3 vec(io.github.kxyr0.echoanimation.api.math.Vec3 value) {
        return new Vec3(value.x(), value.y(), value.z());
    }

    private Quat rotation(io.github.kxyr0.echoanimation.api.math.Vec3 degrees) {
        Quat x = axis(Vec3.unitX(), degrees.x());
        Quat y = axis(Vec3.unitY(), degrees.y());
        Quat z = axis(Vec3.unitZ(), degrees.z());
        return z.multiply(y).multiply(x).normalized();
    }

    private Quat axis(Vec3 axis, double degrees) {
        double radians = Math.toRadians(degrees) * 0.5D;
        double sin = Math.sin(radians);
        return new Quat(axis.x() * sin, axis.y() * sin, axis.z() * sin, Math.cos(radians));
    }

    private double length(io.github.kxyr0.echoanimation.api.math.Vec3 value) {
        return Math.sqrt(value.x() * value.x() + value.y() * value.y() + value.z() * value.z());
    }
}
