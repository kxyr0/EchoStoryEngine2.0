package io.github.kxyr0.echobackgroundnpc.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.kxyr0.echobackgroundnpc.forge.entity.BackgroundNpcEntity;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationSkinVariant;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationClientService;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationClientServiceBridge;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationHumanoidRenderContext;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationHumanoidRenderResult;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** One shared humanoid model with distance/frustum culling before pose work. */
public final class BackgroundNpcRenderer extends EntityRenderer<BackgroundNpcEntity> {
    private final PlayerModel<net.minecraft.world.entity.LivingEntity> model;
    private final BackgroundSkinCache skins = new BackgroundSkinCache();
    private final Optional<AnimationClientService> animation;

    public BackgroundNpcRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        animation = AnimationClientServiceBridge.lookup().service();
        shadowRadius = 0.45F;
    }

    @Override
    public void render(BackgroundNpcEntity entity, float yaw, float partialTick, PoseStack poses,
            MultiBufferSource buffers, int light) {
        ResourceLocation texture = getTextureLocation(entity);
        poses.pushPose();
        poses.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poses.scale(-1.0F, -1.0F, 1.0F);
        poses.translate(0.0F, -1.501F, 0.0F);
        if (!renderEchoAnimation(entity, partialTick, poses, buffers, light, texture)) {
            animate(entity, partialTick);
            var consumer = buffers.getBuffer(RenderType.entityTranslucent(texture));
            model.renderToBuffer(poses, consumer, light,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
        poses.popPose();
        super.render(entity, yaw, partialTick, poses, buffers, light);
    }

    private boolean renderEchoAnimation(BackgroundNpcEntity entity, float partialTick, PoseStack poses,
            MultiBufferSource buffers, int light, ResourceLocation texture) {
        if (animation.isEmpty() || entity.agentIdValue().isBlank()) return false;
        var context = new AnimationHumanoidRenderContext(new AnimationSubjectId(entity.agentIdValue()), List.of(),
                List.of(), texture, AnimationSkinVariant.CLASSIC, poses, buffers, light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, partialTick, false,
                io.github.kxyr0.echoanimation.forge.api.client.render.attachment.AnimationHumanoidAttachmentRenderer.NONE);
        return animation.orElseThrow().renderHumanoid(context)
                == AnimationHumanoidRenderResult.REPLACED_DEFAULT_MODEL;
    }

    private void animate(BackgroundNpcEntity entity, float partialTick) {
        boolean walking = entity.taskValue().equals("TRAVEL");
        float phase = (entity.tickCount + partialTick) * 0.62F + (entity.getId() & 15) * 0.37F;
        float swing = walking ? (float) Math.sin(phase) * 0.75F : 0.0F;
        model.rightLeg.xRot = swing;
        model.leftLeg.xRot = -swing;
        model.rightArm.xRot = -swing * 0.7F;
        model.leftArm.xRot = swing * 0.7F;
        model.head.xRot = 0.0F;
        model.head.yRot = 0.0F;
    }

    @Override public ResourceLocation getTextureLocation(BackgroundNpcEntity entity) {
        return skins.resolve(entity.appearanceValue());
    }
}
