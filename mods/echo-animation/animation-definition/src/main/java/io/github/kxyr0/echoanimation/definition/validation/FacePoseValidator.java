package io.github.kxyr0.echoanimation.definition.validation;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.definition.resource.FacePoseDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates that pose deltas only target existing bones.
 */
public final class FacePoseValidator {
    public void validate(FaceRigDefinition rig, FacePoseDefinition pose) {
        Set<BoneId> bones = rig.hierarchy().bones().stream().map(bone -> bone.id()).collect(Collectors.toSet());
        for (BoneId boneId : pose.boneDeltas().keySet()) {
            if (!bones.contains(boneId)) {
                throw new IllegalArgumentException("Pose target bone не найден: " + boneId);
            }
        }
        if (!rig.id().equals(pose.rigId())) {
            throw new IllegalArgumentException("Pose rigId не совпадает с FaceRig: " + pose.rigId());
        }
    }
}

