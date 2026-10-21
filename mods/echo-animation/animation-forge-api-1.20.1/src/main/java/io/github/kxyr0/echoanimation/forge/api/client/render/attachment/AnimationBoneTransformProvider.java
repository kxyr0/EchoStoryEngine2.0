package io.github.kxyr0.echoanimation.forge.api.client.render.attachment;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kxyr0.echoanimation.forge.api.client.render.attachment.AnimationAttachmentTransformSpace;
import io.github.kxyr0.echoanimation.forge.api.client.render.attachment.AnimationHumanoidAttachmentAnchor;
import java.util.Objects;

@FunctionalInterface
public interface AnimationBoneTransformProvider {

    void apply(
            String boneName,
            PoseStack poses);

    /**
     * Applies the rig-specific bone bound to a stable humanoid attachment semantic.
     * Old providers keep the historical body/shoulder mapping automatically.
     */
    default void applyAnchor(
            AnimationHumanoidAttachmentAnchor anchor,
            PoseStack poses) {

        Objects.requireNonNull(anchor, "anchor");

        apply(
                switch (anchor) {
                    case TORSO_BACK -> "body";
                    case LEFT_HAND -> "left_shoulder";
                    case RIGHT_HAND -> "right_shoulder";
                },
                poses);
    }

    /**
     * Returns the composition contract for the selected anchor.
     */
    default AnimationAttachmentTransformSpace anchorSpace(
            AnimationHumanoidAttachmentAnchor anchor) {

        Objects.requireNonNull(anchor, "anchor");
        return AnimationAttachmentTransformSpace.LEGACY_PART_RELATIVE;
    }
}
