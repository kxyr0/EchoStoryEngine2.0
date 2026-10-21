package io.github.kxyr0.echonpc.forge.api.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcReplacementAttachmentRenderer;
import java.util.Objects;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

/**
 * Safe public context for render extensions targeting an NPC head.
 */
public record NpcHeadRenderContext(
        ActorId actorId,
        PoseStack poses,
        MultiBufferSource buffers,
        int packedLight,
        int overlay,
        float partialTick,
        ModelPart head,
        boolean visible,
        String definitionId,
        String skinId,
        ResourceLocation skinTexture,
        String skinModelVariant,
        boolean replacementPass,
        boolean sitting,
        NpcReplacementAttachmentRenderer replacementAttachments
) {

    public NpcHeadRenderContext {
        actorId =
                Objects.requireNonNull(
                        actorId,
                        "actorId");

        poses =
                Objects.requireNonNull(
                        poses,
                        "poses");

        buffers =
                Objects.requireNonNull(
                        buffers,
                        "buffers");

        head =
                Objects.requireNonNull(
                        head,
                        "head");

        definitionId =
                Objects.requireNonNull(
                        definitionId,
                        "definitionId");

        skinId =
                Objects.requireNonNull(
                        skinId,
                        "skinId");

        skinTexture =
                Objects.requireNonNull(
                        skinTexture,
                        "skinTexture");

        skinModelVariant =
                Objects.requireNonNull(
                        skinModelVariant,
                        "skinModelVariant");

        replacementAttachments =
                Objects.requireNonNull(
                        replacementAttachments,
                        "replacementAttachments");
    }

    public NpcHeadRenderContext(
            ActorId actorId,
            PoseStack poses,
            MultiBufferSource buffers,
            int packedLight,
            int overlay,
            float partialTick,
            ModelPart head,
            boolean visible,
            String definitionId,
            String skinId,
            ResourceLocation skinTexture,
            String skinModelVariant,
            boolean replacementPass,
            boolean sitting) {

        this(
                actorId,
                poses,
                buffers,
                packedLight,
                overlay,
                partialTick,
                head,
                visible,
                definitionId,
                skinId,
                skinTexture,
                skinModelVariant,
                replacementPass,
                sitting,
                NpcReplacementAttachmentRenderer.NONE);
    }
}
