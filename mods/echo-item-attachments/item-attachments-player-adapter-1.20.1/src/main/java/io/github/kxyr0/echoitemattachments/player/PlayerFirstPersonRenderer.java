package io.github.kxyr0.echoitemattachments.player;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.identity.SubjectKind;
import io.github.kxyr0.echoitemattachments.forge.api.client.ClientAttachmentBridge;
import io.github.kxyr0.echoitemattachments.forge.api.client.ItemGripCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.event.RenderHandEvent;

final class PlayerFirstPersonRenderer {
    private PlayerFirstPersonRenderer() {
    }

    static void render(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        AttachmentSubjectRef subject = new AttachmentSubjectRef(SubjectKind.PLAYER,
                minecraft.player.getUUID().toString());
        var point = PlayerAttachmentPoints.physical(minecraft.player, event.getHand());
        var attachment = ClientAttachmentBridge.lookup(subject, point);
        if (attachment.isEmpty()) return;
        boolean right = point.equals(io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId.RIGHT_HAND);
        event.getPoseStack().pushPose();
        try {
            float side = right ? 1.0F : -1.0F;
            event.getPoseStack().translate(0.52D * side,
                    -0.48D - event.getEquipProgress() * 0.18D, -0.72D);
            ItemGripCatalog.INSTANCE.apply(event.getPoseStack(), point,
                    attachment.orElseThrow().stack());
            minecraft.getItemRenderer().renderStatic(attachment.orElseThrow().stack(),
                    right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                            : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                    event.getPackedLight(), net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    event.getPoseStack(), event.getMultiBufferSource(), minecraft.level,
                    attachment.orElseThrow().attachmentId().hashCode());
        } finally {
            event.getPoseStack().popPose();
        }
        event.setCanceled(true);
    }
}
