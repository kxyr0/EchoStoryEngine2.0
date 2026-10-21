package io.github.kxyr0.echoanimation.core.mask;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.rig.definition.SegmentedHumanoidBone;
import java.util.Set;

/**
 * Authored segmented humanoid mask presets compiled to ordinary BoneId sets.
 */
public final class HumanoidBoneMasks {
    private HumanoidBoneMasks() {
    }

    public static Set<BoneId> fullBody() {
        return SegmentedHumanoidBone.ids();
    }

    public static Set<BoneId> upperBody() {
        return Set.of(
                SegmentedHumanoidBone.BODY_LOWER.id(),
                SegmentedHumanoidBone.BODY.id(),
                SegmentedHumanoidBone.HEAD.id(),
                SegmentedHumanoidBone.LEFT_SHOULDER.id(),
                SegmentedHumanoidBone.LEFT_UPPER_ARM.id(),
                SegmentedHumanoidBone.LEFT_FOREARM.id(),
                SegmentedHumanoidBone.RIGHT_SHOULDER.id(),
                SegmentedHumanoidBone.RIGHT_UPPER_ARM.id(),
                SegmentedHumanoidBone.RIGHT_FOREARM.id());
    }

    public static Set<BoneId> headOnly() {
        return Set.of(SegmentedHumanoidBone.HEAD.id());
    }

    public static Set<BoneId> leftArm() {
        return Set.of(SegmentedHumanoidBone.LEFT_SHOULDER.id(),
                SegmentedHumanoidBone.LEFT_UPPER_ARM.id(), SegmentedHumanoidBone.LEFT_FOREARM.id());
    }

    public static Set<BoneId> rightArm() {
        return Set.of(SegmentedHumanoidBone.RIGHT_SHOULDER.id(),
                SegmentedHumanoidBone.RIGHT_UPPER_ARM.id(), SegmentedHumanoidBone.RIGHT_FOREARM.id());
    }

    public static Set<BoneId> leftLeg() {
        return Set.of(SegmentedHumanoidBone.LEFT_THIGH.id(), SegmentedHumanoidBone.LEFT_SHIN.id());
    }

    public static Set<BoneId> rightLeg() {
        return Set.of(SegmentedHumanoidBone.RIGHT_THIGH.id(), SegmentedHumanoidBone.RIGHT_SHIN.id());
    }
}
