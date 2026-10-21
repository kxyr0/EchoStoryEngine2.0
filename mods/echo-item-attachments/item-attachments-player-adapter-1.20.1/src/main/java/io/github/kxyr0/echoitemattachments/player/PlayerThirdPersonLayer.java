package io.github.kxyr0.echoitemattachments.player;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.identity.SubjectKind;
import io.github.kxyr0.echoitemattachments.forge.api.client.ClientAttachmentBridge;
import io.github.kxyr0.echoitemattachments.forge.api.client.ItemGripCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.event.EntityRenderersEvent;

final class PlayerThirdPersonLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    PlayerThirdPersonLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            if (event.getPlayerSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new PlayerThirdPersonLayer(renderer));
            }
        }
    }

    @Override
    public void render(PoseStack poses, MultiBufferSource buffers, int light,
            AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
            float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        AttachmentSubjectRef subject = new AttachmentSubjectRef(SubjectKind.PLAYER,
                player.getUUID().toString());
        renderPoint(poses, buffers, light, subject, AttachmentPointId.LEFT_HAND);
        renderPoint(poses, buffers, light, subject, AttachmentPointId.RIGHT_HAND);
    }

    private void renderPoint(PoseStack poses, MultiBufferSource buffers, int light,
            AttachmentSubjectRef subject, AttachmentPointId point) {
        var attachment = ClientAttachmentBridge.lookup(subject, point);
        if (attachment.isEmpty()) return;
        boolean right = point.equals(AttachmentPointId.RIGHT_HAND);
        poses.pushPose();
        try {
            if (right) getParentModel().rightArm.translateAndRotate(poses);
            else getParentModel().leftArm.translateAndRotate(poses);
            ItemGripCatalog.INSTANCE.apply(poses, point, attachment.orElseThrow().stack());
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.getItemRenderer().renderStatic(attachment.orElseThrow().stack(),
                    right ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                            : ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                    light, OverlayTexture.NO_OVERLAY, poses, buffers, minecraft.level,
                    attachment.orElseThrow().attachmentId().hashCode());
        } finally {
            poses.popPose();
        }
    }
}
