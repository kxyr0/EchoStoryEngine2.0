package io.github.kxyr0.echoanimation.client.humanoid;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.rig.definition.SegmentedHumanoidBone;

/**
 * Visible segmented humanoid cuboids.
 */
public enum HumanoidSegmentId {
    HEAD(SegmentedHumanoidBone.HEAD),
    BODY_LOWER(SegmentedHumanoidBone.BODY_LOWER),
    BODY(SegmentedHumanoidBone.BODY),
    LEFT_UPPER_ARM(SegmentedHumanoidBone.LEFT_UPPER_ARM),
    LEFT_FOREARM(SegmentedHumanoidBone.LEFT_FOREARM),
    RIGHT_UPPER_ARM(SegmentedHumanoidBone.RIGHT_UPPER_ARM),
    RIGHT_FOREARM(SegmentedHumanoidBone.RIGHT_FOREARM),
    LEFT_THIGH(SegmentedHumanoidBone.LEFT_THIGH),
    LEFT_SHIN(SegmentedHumanoidBone.LEFT_SHIN),
    RIGHT_THIGH(SegmentedHumanoidBone.RIGHT_THIGH),
    RIGHT_SHIN(SegmentedHumanoidBone.RIGHT_SHIN);

    private final BoneId boneId;

    HumanoidSegmentId(SegmentedHumanoidBone bone) {
        this.boneId = bone.id();
    }

    public BoneId boneId() {
        return boneId;
    }
}
