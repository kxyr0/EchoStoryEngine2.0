package io.github.kxyr0.echoanimation.core.transform;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneDefinition;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneHierarchy;
import io.github.kxyr0.echoanimation.core.hierarchy.BonePose;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deterministic local-to-world evaluator.
 */
public final class TransformEvaluator {
    public WorldTransform evaluate(BoneHierarchy hierarchy, BonePose pose) {
        Map<BoneId, Transform3> world = new LinkedHashMap<>();
        Map<BoneId, BoneDefinition> definitions = new HashMap<>();
        for (BoneDefinition bone : hierarchy.bones()) {
            definitions.put(bone.id(), bone);
        }
        for (BoneDefinition bone : hierarchy.bones()) {
            resolve(bone, pose, definitions, world);
        }
        return new WorldTransform(world);
    }

    private Transform3 resolve(
            BoneDefinition bone,
            BonePose pose,
            Map<BoneId, BoneDefinition> definitions,
            Map<BoneId, Transform3> world
    ) {
        if (world.containsKey(bone.id())) {
            return world.get(bone.id());
        }
        Transform3 local = bone.localTransform().plusDelta(pose.transform(bone.id()).orElse(Transform3.IDENTITY));
        Transform3 value = bone.parentId()
                .map(parent -> compose(resolve(definitions.get(parent), pose, definitions, world), local))
                .orElse(local);
        world.put(bone.id(), value);
        return value;
    }

    private Transform3 compose(Transform3 parent, Transform3 child) {
        Vec3 translated = parent.applyPoint(child.translation());
        return new Transform3(
                translated,
                parent.rotationZDegrees() + child.rotationZDegrees(),
                parent.scale().componentTimes(child.scale()),
                child.pivot());
    }
}
