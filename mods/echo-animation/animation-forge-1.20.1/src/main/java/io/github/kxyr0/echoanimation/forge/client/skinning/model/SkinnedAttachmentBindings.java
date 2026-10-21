package io.github.kxyr0.echoanimation.forge.client.skinning.model;

import io.github.kxyr0.echoanimation.forge.api.client.render.attachment.AnimationAttachmentTransformSpace;
import io.github.kxyr0.echoanimation.forge.api.client.render.attachment.AnimationHumanoidAttachmentAnchor;
import java.util.Objects;

/** Rig-owned mapping from stable attachment semantics to concrete skeleton bones. */
public record SkinnedAttachmentBindings(
        AnimationAttachmentTransformSpace space,
        String torsoBackBone,
        String leftHandBone,
        String rightHandBone,
        boolean cancelBindBasis,
        boolean explicit
) {
    public SkinnedAttachmentBindings {
        space = Objects.requireNonNull(space, "space");
        torsoBackBone = requireBone(torsoBackBone, "torsoBackBone");
        leftHandBone = requireBone(leftHandBone, "leftHandBone");
        rightHandBone = requireBone(rightHandBone, "rightHandBone");
    }

    public static SkinnedAttachmentBindings legacy() {
        return new SkinnedAttachmentBindings(
                AnimationAttachmentTransformSpace.LEGACY_PART_RELATIVE,
                "body",
                "left_shoulder",
                "right_shoulder",
                false,
                false);
    }

    public String bone(AnimationHumanoidAttachmentAnchor anchor) {
        return switch (Objects.requireNonNull(anchor, "anchor")) {
            case TORSO_BACK -> torsoBackBone;
            case LEFT_HAND -> leftHandBone;
            case RIGHT_HAND -> rightHandBone;
        };
    }

    private static String requireBone(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return value;
    }
}
