package io.github.kxyr0.echonpcanimationadapter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kxyr0.echoanimation.forge.api.client.render.attachment.AnimationAttachmentTransformSpace;
import io.github.kxyr0.echoanimation.forge.api.client.render.attachment.AnimationBoneTransformProvider;
import io.github.kxyr0.echoanimation.forge.api.client.render.attachment.AnimationHumanoidAttachmentAnchor;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcVisualAttachmentAnchor;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcVisualAttachmentTransformProvider;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcVisualAttachmentTransformSpace;
import java.util.Objects;

/** Converts stable NPC attachment semantics to the active animation rig socket contract. */
final class NpcAnimationAttachmentBridge
        implements NpcVisualAttachmentTransformProvider {

    private final AnimationBoneTransformProvider bones;

    NpcAnimationAttachmentBridge(
            AnimationBoneTransformProvider bones) {

        this.bones = Objects.requireNonNull(
                bones,
                "bones");
    }

    @Override
    public void apply(
            NpcVisualAttachmentAnchor anchor,
            PoseStack poses) {

        bones.applyAnchor(
                animationAnchor(anchor),
                poses);
    }

    @Override
    public NpcVisualAttachmentTransformSpace space(
            NpcVisualAttachmentAnchor anchor) {

        AnimationAttachmentTransformSpace space =
                bones.anchorSpace(
                        animationAnchor(anchor));

        return switch (space) {
            case LEGACY_PART_RELATIVE ->
                    NpcVisualAttachmentTransformSpace.PART_RELATIVE;

            case MODEL_ABSOLUTE ->
                    NpcVisualAttachmentTransformSpace.MODEL_ABSOLUTE;
        };
    }

    static AnimationHumanoidAttachmentAnchor animationAnchor(
            NpcVisualAttachmentAnchor anchor) {

        return switch (Objects.requireNonNull(anchor, "anchor")) {
            case TORSO_BACK ->
                    AnimationHumanoidAttachmentAnchor.TORSO_BACK;

            case LEFT_HAND ->
                    AnimationHumanoidAttachmentAnchor.LEFT_HAND;

            case RIGHT_HAND ->
                    AnimationHumanoidAttachmentAnchor.RIGHT_HAND;
        };
    }
}
