package io.github.kxyr0.echonpcanimationadapter.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kxyr0.echoanimation.forge.api.client.render.attachment.AnimationAttachmentTransformSpace;
import io.github.kxyr0.echoanimation.forge.api.client.render.attachment.AnimationBoneTransformProvider;
import io.github.kxyr0.echoanimation.forge.api.client.render.attachment.AnimationHumanoidAttachmentAnchor;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcVisualAttachmentAnchor;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcVisualAttachmentTransformSpace;
import org.junit.jupiter.api.Test;

final class NpcAnimationAttachmentBridgeTest {
    @Test
    void mapsNpcAnchorsToAnimationSemantics() {
        assertEquals(
                AnimationHumanoidAttachmentAnchor.TORSO_BACK,
                NpcAnimationAttachmentBridge.animationAnchor(
                        NpcVisualAttachmentAnchor.TORSO_BACK));
        assertEquals(
                AnimationHumanoidAttachmentAnchor.LEFT_HAND,
                NpcAnimationAttachmentBridge.animationAnchor(
                        NpcVisualAttachmentAnchor.LEFT_HAND));
        assertEquals(
                AnimationHumanoidAttachmentAnchor.RIGHT_HAND,
                NpcAnimationAttachmentBridge.animationAnchor(
                        NpcVisualAttachmentAnchor.RIGHT_HAND));
    }

    @Test
    void carriesAbsoluteSocketSpaceAcrossAdapterBoundary() {
        AnimationBoneTransformProvider bones =
                new AnimationBoneTransformProvider() {
                    @Override
                    public void apply(
                            String boneName,
                            PoseStack poses) {
                    }

                    @Override
                    public AnimationAttachmentTransformSpace anchorSpace(
                            AnimationHumanoidAttachmentAnchor anchor) {

                        return AnimationAttachmentTransformSpace.MODEL_ABSOLUTE;
                    }
                };

        NpcAnimationAttachmentBridge bridge =
                new NpcAnimationAttachmentBridge(bones);

        assertEquals(
                NpcVisualAttachmentTransformSpace.MODEL_ABSOLUTE,
                bridge.space(NpcVisualAttachmentAnchor.LEFT_HAND));
    }
}
