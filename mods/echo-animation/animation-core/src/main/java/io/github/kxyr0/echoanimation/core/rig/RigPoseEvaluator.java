package io.github.kxyr0.echoanimation.core.rig;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.math.Matrix4Transform;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.rig.AnimationAttachmentSocketId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationBoneDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Evaluates local rig pose into model-space transforms.
 */
public final class RigPoseEvaluator {
    public EvaluatedRigPose evaluate(AnimationRigDefinition rig, AnimationPose pose) {
        Map<BoneId, AnimationBoneDefinition> byId = definitions(rig);
        Map<BoneId, EulerTransform> bones = new LinkedHashMap<>();
        for (AnimationBoneDefinition bone : rig.bones()) {
            resolveEuler(bone, pose, byId, bones);
        }
        return new EvaluatedRigPose(bones, eulerSockets(rig, bones));
    }

    public EvaluatedMatrixRigPose evaluateMatrices(AnimationRigDefinition rig, AnimationPose pose) {
        Map<BoneId, AnimationBoneDefinition> byId = definitions(rig);
        Map<BoneId, Matrix4Transform> bones = new LinkedHashMap<>();
        for (AnimationBoneDefinition bone : rig.bones()) {
            resolveMatrix(bone, pose, byId, bones);
        }
        return new EvaluatedMatrixRigPose(bones, matrixSockets(rig, bones));
    }

    private EulerTransform resolveEuler(
            AnimationBoneDefinition bone,
            AnimationPose pose,
            Map<BoneId, AnimationBoneDefinition> definitions,
            Map<BoneId, EulerTransform> world
    ) {
        if (world.containsKey(bone.id())) {
            return world.get(bone.id());
        }
        EulerTransform local = pose.transform(bone.id()).orElse(bone.defaultLocalTransform());
        EulerTransform value = bone.parentId()
                .map(parent -> composeEuler(resolveEuler(definitions.get(parent), pose, definitions, world), local))
                .orElse(local);
        world.put(bone.id(), value);
        return value;
    }

    private Matrix4Transform resolveMatrix(
            AnimationBoneDefinition bone,
            AnimationPose pose,
            Map<BoneId, AnimationBoneDefinition> definitions,
            Map<BoneId, Matrix4Transform> world
    ) {
        Matrix4Transform existing = world.get(bone.id());
        if (existing != null) {
            return existing;
        }
        Matrix4Transform local = pose.localMatrix(bone.id(), bone.defaultLocalTransform());
        Matrix4Transform value = bone.parentId()
                .map(parent -> resolveMatrix(definitions.get(parent), pose, definitions, world).multiply(local))
                .orElse(local);
        world.put(bone.id(), value);
        return value;
    }

    private Map<AnimationAttachmentSocketId, EulerTransform> eulerSockets(
            AnimationRigDefinition rig,
            Map<BoneId, EulerTransform> bones
    ) {
        Map<AnimationAttachmentSocketId, EulerTransform> result = new LinkedHashMap<>();
        rig.sockets().forEach(socket -> result.put(socket.id(),
                composeEuler(bones.get(socket.parentBoneId()), socket.localTransform())));
        return result;
    }

    private Map<AnimationAttachmentSocketId, Matrix4Transform> matrixSockets(
            AnimationRigDefinition rig,
            Map<BoneId, Matrix4Transform> bones
    ) {
        Map<AnimationAttachmentSocketId, Matrix4Transform> result = new LinkedHashMap<>();
        rig.sockets().forEach(socket -> result.put(socket.id(), bones.get(socket.parentBoneId())
                .multiply(Matrix4Transform.fromEuler(socket.localTransform()))));
        return result;
    }

    private Map<BoneId, AnimationBoneDefinition> definitions(AnimationRigDefinition rig) {
        Map<BoneId, AnimationBoneDefinition> result = new HashMap<>();
        for (AnimationBoneDefinition bone : rig.bones()) {
            result.put(bone.id(), bone);
        }
        return result;
    }

    private EulerTransform composeEuler(EulerTransform parent, EulerTransform child) {
        Vec3 translated = parent.applyPoint(child.translation());
        return new EulerTransform(
                translated,
                parent.rotationDegrees().plus(child.rotationDegrees()),
                parent.scale().componentTimes(child.scale()),
                child.pivot());
    }
}
