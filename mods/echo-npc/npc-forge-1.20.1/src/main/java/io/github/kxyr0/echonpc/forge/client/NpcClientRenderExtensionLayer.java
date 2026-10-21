package io.github.kxyr0.echonpc.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcBodyAttachmentRenderContext;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcHeadRenderContext;
import io.github.kxyr0.echonpc.forge.client.attachment.NpcBodyAttachmentExtensionSet;
import io.github.kxyr0.echonpc.forge.entity.EchoNpcEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.slf4j.Logger;

/**
 * Internal safety boundary for public NPC client render extensions.
 */
final class NpcClientRenderExtensionLayer extends RenderLayer<EchoNpcEntity, PlayerModel<EchoNpcEntity>> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final EchoNpcEntityRenderer renderer;
    private final NpcClientRenderExtensionSet extensions;
    private final NpcBodyAttachmentExtensionSet attachments;
    private boolean invalidActorReported;

    NpcClientRenderExtensionLayer(
            EchoNpcEntityRenderer parent,
            NpcClientRenderExtensionSet extensions,
            NpcBodyAttachmentExtensionSet attachments
    ) {
        super(parent);
        this.renderer = parent;
        this.extensions = extensions;
        this.attachments = attachments;
    }

    @Override
    public void render(PoseStack poses, MultiBufferSource buffers, int light, EchoNpcEntity entity,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
            float netHeadYaw, float headPitch) {
        ActorId actorId = actorId(entity);
        if (actorId == null) {
            return;
        }
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
        PlayerModel<EchoNpcEntity> model = renderer.modelFor(entity);
        NpcHeadRenderContext context = new NpcHeadRenderContext(actorId, poses, buffers, light, overlay, partialTick,
                model.head, !entity.isInvisible(), entity.definitionIdValue(), entity.skinIdValue(),
                EchoNpcClientBootstrap.skins().location(entity.skinIdValue()),
                renderer.skinModelVariant(entity).apiName(), false, entity.isPassenger());
        extensions.render(context);
        attachments.render(new NpcBodyAttachmentRenderContext(
                actorId, poses, buffers, light, overlay, partialTick,
                model.body, model.leftArm, model.rightArm, !entity.isInvisible(),
                entity.definitionIdValue(), entity.formIdValue(), model.young,
                entity.isPassenger(), entity.sleepingValue(), entity.bodyScaleValue()));
    }

    private ActorId actorId(EchoNpcEntity entity) {
        try {
            return new ActorId(entity.actorIdValue());
        } catch (IllegalArgumentException exception) {
            if (!invalidActorReported) {
                invalidActorReported = true;
                LOGGER.warn("Echo NPC render extension skipped entity with invalid ActorId.");
            }
            return null;
        }
    }
}
