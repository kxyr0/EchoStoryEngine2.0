package io.github.kxyr0.echoitemattachments.npc;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.identity.SubjectKind;
import io.github.kxyr0.echoitemattachments.forge.api.client.ClientAttachmentBridge;
import io.github.kxyr0.echoitemattachments.forge.api.client.ItemGripCatalog;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcBodyAttachmentRenderContext;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcBodyAttachmentRenderExtension;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcVisualAttachmentAnchor;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderResult;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;

final class NpcHandItemRenderer implements NpcBodyAttachmentRenderExtension {
    @Override
    public NpcClientRenderResult render(NpcBodyAttachmentRenderContext context) {
        if (!context.visible()) return NpcClientRenderResult.NOT_HANDLED;
        AttachmentSubjectRef subject = new AttachmentSubjectRef(SubjectKind.NPC,
                context.actorId().value());
        boolean left = renderPoint(context, subject, AttachmentPointId.LEFT_HAND);
        boolean right = renderPoint(context, subject, AttachmentPointId.RIGHT_HAND);
        return left || right ? NpcClientRenderResult.RENDERED_OVERLAY
                : NpcClientRenderResult.NOT_HANDLED;
    }

    private boolean renderPoint(NpcBodyAttachmentRenderContext context,
            AttachmentSubjectRef subject, AttachmentPointId point) {
        var attachment = ClientAttachmentBridge.lookup(subject, point);
        if (attachment.isEmpty()) return false;
        boolean right = point.equals(AttachmentPointId.RIGHT_HAND);
        context.poses().pushPose();
        try {
            context.applyAnchor(right ? NpcVisualAttachmentAnchor.RIGHT_HAND
                    : NpcVisualAttachmentAnchor.LEFT_HAND);
            ItemGripCatalog.INSTANCE.apply(context.poses(), point, attachment.orElseThrow().stack());
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.getItemRenderer().renderStatic(attachment.orElseThrow().stack(),
                    right ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                            : ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                    context.packedLight(), context.packedOverlay(), context.poses(),
                    context.buffers(), minecraft.level, attachment.orElseThrow().attachmentId().hashCode());
        } finally {
            context.poses().popPose();
        }
        return true;
    }
}
