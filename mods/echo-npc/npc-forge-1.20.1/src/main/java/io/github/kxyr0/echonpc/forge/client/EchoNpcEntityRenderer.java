package io.github.kxyr0.echonpc.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcBodyAttachmentRenderContext;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcVisualAttachmentTransformProvider;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderResult;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcHeadRenderContext;
import io.github.kxyr0.echonpc.forge.client.attachment.NpcBodyAttachmentExtensionSet;
import io.github.kxyr0.echonpc.forge.client.attachment.NpcReplacementAttachmentAnchorProvider;
import io.github.kxyr0.echonpc.forge.client.form.EchoNpcFormPlayerModel;
import io.github.kxyr0.echonpc.forge.entity.EchoNpcEntity;
import java.util.List;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

/**
 * Client-only humanoid renderer for stationary N1 NPC entities.
 */
public final class EchoNpcEntityRenderer
        extends MobRenderer<EchoNpcEntity, PlayerModel<EchoNpcEntity>> {

    private static final Logger LOGGER =
            LogUtils.getLogger();

    private final SkinResourceCache skins;

    private final EchoNpcFormPlayerModel<EchoNpcEntity>
            classicModel;

    private final EchoNpcFormPlayerModel<EchoNpcEntity>
            slimModel;

    private NpcClientRenderExtensionSet extensions =
            new NpcClientRenderExtensionSet(
                    List.of());

    private NpcBodyAttachmentExtensionSet attachments =
            new NpcBodyAttachmentExtensionSet(
                    List.of());

    private boolean invalidActorReported;

    public EchoNpcEntityRenderer(
            EntityRendererProvider.Context context) {

        super(
                context,
                new PlayerModel<>(
                        context.bakeLayer(
                                ModelLayers.PLAYER),
                        false),
                0.5F);

        skins =
                EchoNpcClientBootstrap.skins();

        classicModel =
                new EchoNpcFormPlayerModel<>(
                        context.bakeLayer(
                                ModelLayers.PLAYER),
                        false);

        slimModel =
                new EchoNpcFormPlayerModel<>(
                        context.bakeLayer(
                                ModelLayers.PLAYER_SLIM),
                        true);

        model =
                classicModel;
    }

    @Override
    public ResourceLocation getTextureLocation(
            EchoNpcEntity entity) {

        return skins.location(
                entity.skinIdValue());
    }

    void installRenderExtensions(
            NpcClientRenderExtensionSet value) {

        extensions = value;
    }

    void installBodyAttachmentExtensions(
            NpcBodyAttachmentExtensionSet value) {

        attachments = value;
    }

    @Override
    public void render(
            EchoNpcEntity entity,
            float yaw,
            float partialTick,
            PoseStack poses,
            MultiBufferSource buffers,
            int light) {

        model =
                modelFor(entity);

        modelFor(entity)
                .applyFormParts(
                        entity.partTransformsValue());

        shadowRadius =
                0.5F
                        * entity.bodyScaleValue();

        NpcClientRenderResult extensionResult =
                renderExtensions(
                        entity,
                        poses,
                        buffers,
                        light,
                        partialTick);

        if (extensionResult
                == NpcClientRenderResult.CANCELLED) {

            return;
        }

        if (extensionResult
                == NpcClientRenderResult.REPLACED_DEFAULT_MODEL) {

            renderNameTag(
                    entity,
                    entity.getDisplayName(),
                    poses,
                    buffers,
                    light);

            return;
        }

        super.render(
                entity,
                yaw,
                partialTick,
                poses,
                buffers,
                light);
    }

    @Override
    protected boolean shouldShowName(
            EchoNpcEntity entity) {

        return true;
    }

    EchoNpcFormPlayerModel<EchoNpcEntity> modelFor(
            EchoNpcEntity entity) {

        return skinModelVariant(entity)
                == SkinModelVariant.SLIM
                ? slimModel
                : classicModel;
    }

    @Override
    protected void scale(
            EchoNpcEntity entity,
            PoseStack poses,
            float partialTick) {

        float scale =
                entity.bodyScaleValue();

        poses.scale(
                scale,
                scale,
                scale);
    }

    SkinModelVariant skinModelVariant(
            EchoNpcEntity entity) {

        return EchoNpcClientBootstrap
                .skinModelVariants()
                .variant(
                        skins.location(
                                entity.skinIdValue()));
    }

    private NpcClientRenderResult renderExtensions(
            EchoNpcEntity entity,
            PoseStack poses,
            MultiBufferSource buffers,
            int light,
            float partialTick) {

        ActorId actorId =
                actorId(entity);

        if (actorId == null
                || extensions.empty()) {

            return NpcClientRenderResult.NOT_HANDLED;
        }

        int overlay =
                LivingEntityRenderer
                        .getOverlayCoords(
                                entity,
                                0.0F);

        PlayerModel<EchoNpcEntity> activeModel =
                modelFor(entity);

        NpcHeadRenderContext context =
                new NpcHeadRenderContext(
                        actorId,
                        poses,
                        buffers,
                        light,
                        overlay,
                        partialTick,
                        activeModel.head,
                        !entity.isInvisible(),
                        entity.definitionIdValue(),
                        entity.skinIdValue(),
                        skins.location(
                                entity.skinIdValue()),
                        skinModelVariant(entity)
                                .apiName(),
                        true,
                        entity.isPassenger(),
                        anchorTransforms ->
                                renderReplacementAttachments(
                                        entity,
                                        actorId,
                                        activeModel,
                                        poses,
                                        buffers,
                                        light,
                                        overlay,
                                        partialTick,
                                        anchorTransforms));

        poses.pushPose();

        try {
            float bodyYaw =
                    Mth.rotLerp(
                            partialTick,
                            entity.yBodyRotO,
                            entity.yBodyRot);

            poses.mulPose(
                    Axis.YP.rotationDegrees(
                            180.0F
                                    - bodyYaw));

            poses.scale(
                    -1.0F,
                    -1.0F,
                    1.0F);

            poses.translate(
                    0.0F,
                    -1.501F,
                    0.0F);

            return extensions.render(
                    context);
        } finally {
            poses.popPose();
        }
    }

    private void renderReplacementAttachments(
            EchoNpcEntity entity,
            ActorId actorId,
            PlayerModel<EchoNpcEntity> activeModel,
            PoseStack poses,
            MultiBufferSource buffers,
            int light,
            int overlay,
            float partialTick,
            NpcVisualAttachmentTransformProvider anchorTransforms) {

        if (attachments.empty()) {
            return;
        }

        /*
         * Animation providers may expose either historical part-relative
         * transforms or complete model-space sockets. The provider wrapper
         * appends vanilla pivots only for the historical contract.
         */
        NpcVisualAttachmentTransformProvider animatedAnchors =
                new NpcReplacementAttachmentAnchorProvider(
                        activeModel,
                        anchorTransforms);

        NpcBodyAttachmentRenderContext context =
                new NpcBodyAttachmentRenderContext(
                        actorId,
                        poses,
                        buffers,
                        light,
                        overlay,
                        partialTick,
                        activeModel.body,
                        activeModel.leftArm,
                        activeModel.rightArm,
                        !entity.isInvisible(),
                        entity.definitionIdValue(),
                        entity.formIdValue(),
                        activeModel.young,
                        entity.isPassenger(),
                        entity.sleepingValue(),
                        entity.bodyScaleValue(),
                        animatedAnchors);

        attachments.render(
                context);
    }

    private ActorId actorId(
            EchoNpcEntity entity) {

        try {
            return new ActorId(
                    entity.actorIdValue());
        } catch (IllegalArgumentException exception) {

            if (!invalidActorReported) {
                invalidActorReported = true;

                LOGGER.warn(
                        "Echo NPC render extension skipped "
                                + "entity with invalid ActorId.");
            }

            return null;
        }
    }
}
