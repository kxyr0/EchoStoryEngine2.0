package io.github.kxyr0.echoanimation.api.rig.definition;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fixed A2 segmented humanoid hierarchy.
 */
public enum SegmentedHumanoidBone {
    ROOT("root", null, 0.0D, 0.0D, 0.0D),
    BODY_LOWER("body_lower", "root", 0.0D, 0.0D, 0.0D),
    BODY("body", "body_lower", 0.0D, 12.0D, 0.0D),
    HEAD("head", "body", 0.0D, 12.0D, 0.0D),
    LEFT_SHOULDER("left_shoulder", "body", 4.0D, 10.0D, 0.0D),
    LEFT_UPPER_ARM("left_upper_arm", "left_shoulder", 0.0D, 0.0D, 0.0D),
    LEFT_FOREARM("left_forearm", "left_upper_arm", 0.0D, -6.0D, 0.0D),
    RIGHT_SHOULDER("right_shoulder", "body", -4.0D, 10.0D, 0.0D),
    RIGHT_UPPER_ARM("right_upper_arm", "right_shoulder", 0.0D, 0.0D, 0.0D),
    RIGHT_FOREARM("right_forearm", "right_upper_arm", 0.0D, -6.0D, 0.0D),
    LEFT_THIGH("left_thigh", "root", 2.0D, 0.0D, 0.0D),
    LEFT_SHIN("left_shin", "left_thigh", 0.0D, -6.0D, 0.0D),
    RIGHT_THIGH("right_thigh", "root", -2.0D, 0.0D, 0.0D),
    RIGHT_SHIN("right_shin", "right_thigh", 0.0D, -6.0D, 0.0D);

    private final BoneId id;
    private final BoneId parentId;
    private final EulerTransform defaultTransform;

    SegmentedHumanoidBone(String id, String parentId, double x, double y, double z) {
        this.id = BoneId.parse(id);
        this.parentId = parentId == null ? null : BoneId.parse(parentId);
        this.defaultTransform = EulerTransform.IDENTITY.withTranslation(new Vec3(x, y, z));
    }

    public BoneId id() {
        return id;
    }

    public Optional<BoneId> parentId() {
        return Optional.ofNullable(parentId);
    }

    public EulerTransform defaultTransform() {
        return defaultTransform;
    }

    public static List<AnimationBoneDefinition> defaultDefinitions() {
        return Arrays.stream(values())
                .map(bone -> new AnimationBoneDefinition(bone.id(), bone.parentId(),
                        defaultTransform(bone), Optional.empty()))
                .toList();
    }

    public static Set<BoneId> ids() {
        return Arrays.stream(values()).map(SegmentedHumanoidBone::id).collect(Collectors.toUnmodifiableSet());
    }

    private static EulerTransform defaultTransform(SegmentedHumanoidBone bone) {
        return bone.defaultTransform();
    }
}
