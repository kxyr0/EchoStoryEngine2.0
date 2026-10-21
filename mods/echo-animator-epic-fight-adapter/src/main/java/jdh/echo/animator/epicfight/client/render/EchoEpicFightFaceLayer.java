package jdh.echo.animator.epicfight.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationClientService;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationClientServiceBridge;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationFaceRenderContext;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationFaceRenderResult;
import jdh.echo.animator.epicfight.EchoAnimatorEpicFightMod;
import jdh.echo.animator.epicfight.client.pose.EpicFightHeadPose;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;

public final class EchoEpicFightFaceLayer extends PatchedLayer<
        AbstractClientPlayer,
        AbstractClientPlayerPatch<AbstractClientPlayer>,
        PlayerModel<AbstractClientPlayer>,
        RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> {
    private static final ModelPart NEUTRAL_HEAD = new ModelPart(List.of(), Map.of());
    private static final float MC_MODEL_SPACE_X = -1.0F;
    private static final float MC_MODEL_SPACE_Y = -1.0F;
    private static final float MC_MODEL_SPACE_Z = 1.0F;
    private static final float PLAYER_DEFAULT_LAYER_HEIGHT = 0.75F;

    private final EpicFightHeadPose headPose = new EpicFightHeadPose();
    private AnimationClientService echoService;
    private boolean warnedServiceUnavailable;
    private boolean warnedRenderFailure;
    private boolean warnedFallbackFailure;

    @Override
    protected void renderLayer(
            AbstractClientPlayerPatch<AbstractClientPlayer> patch,
            AbstractClientPlayer player,
            RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> vanillaLayer,
            PoseStack poses,
            MultiBufferSource buffers,
            int packedLight,
            OpenMatrix4f[] poseMatrices,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            float partialTick) {
        if (player.isInvisible()) {
            renderOriginalLayer(patch, player, vanillaLayer, poses, buffers, packedLight, poseMatrices,
                    ageInTicks, netHeadYaw, headPitch, partialTick);
            return;
        }

        List<FaceRigId> rigCandidates = EchoFaceIdentity.rigCandidates(player);
        if (rigCandidates.isEmpty()) {
            renderOriginalLayer(patch, player, vanillaLayer, poses, buffers, packedLight, poseMatrices,
                    ageInTicks, netHeadYaw, headPitch, partialTick);
            return;
        }

        OpenMatrix4f headMatrix = headPose.resolveHead(patch, poseMatrices);
        AnimationClientService service = resolveEchoService();
        if (headMatrix == null || service == null) {
            renderOriginalLayer(patch, player, vanillaLayer, poses, buffers, packedLight, poseMatrices,
                    ageInTicks, netHeadYaw, headPitch, partialTick);
            return;
        }

        AnimationFaceRenderResult result =
                renderFaceOnHead(service, rigCandidates, player, poses, buffers, packedLight, partialTick, headMatrix);
        if (result != AnimationFaceRenderResult.RENDERED) {
            renderOriginalLayer(patch, player, vanillaLayer, poses, buffers, packedLight, poseMatrices,
                    ageInTicks, netHeadYaw, headPitch, partialTick);
        }
    }

    private AnimationClientService resolveEchoService() {
        if (echoService != null) {
            return echoService;
        }

        AnimationClientServiceBridge.Result result = AnimationClientServiceBridge.lookup();
        if (result.status() == AnimationClientServiceBridge.Status.RESOLVED && result.service().isPresent()) {
            echoService = result.service().get();
            return echoService;
        }

        if (!warnedServiceUnavailable) {
            EchoAnimatorEpicFightMod.LOGGER.warn(
                    "Echo Animation client render service is unavailable: {}", result.message());
            warnedServiceUnavailable = true;
        }
        return null;
    }

    private AnimationFaceRenderResult renderFaceOnHead(
            AnimationClientService service,
            List<FaceRigId> rigCandidates,
            AbstractClientPlayer player,
            PoseStack poses,
            MultiBufferSource buffers,
            int packedLight,
            float partialTick,
            OpenMatrix4f headMatrix) {
        poses.pushPose();
        try {
            MathUtils.mulStack(poses, headMatrix);
            poses.scale(MC_MODEL_SPACE_X, MC_MODEL_SPACE_Y, MC_MODEL_SPACE_Z);
            return service.renderFace(new AnimationFaceRenderContext(
                    EchoFaceIdentity.subjectId(rigCandidates),
                    rigCandidates,
                    poses,
                    buffers,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    partialTick,
                    NEUTRAL_HEAD));
        } catch (RuntimeException exception) {
            warnRenderFailure(player, exception);
            return AnimationFaceRenderResult.FAILED;
        } finally {
            poses.popPose();
        }
    }

    private void renderOriginalLayer(
            AbstractClientPlayerPatch<AbstractClientPlayer> patch,
            AbstractClientPlayer player,
            RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> vanillaLayer,
            PoseStack poses,
            MultiBufferSource buffers,
            int packedLight,
            OpenMatrix4f[] poseMatrices,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            float partialTick) {
        if (vanillaLayer == null) {
            return;
        }

        try {
            poses.pushPose();
            OpenMatrix4f rootMatrix = headPose.resolveRoot(patch, poseMatrices);
            if (rootMatrix != null) {
                MathUtils.mulStack(poses, rootMatrix);
                poses.translate(0.0F, PLAYER_DEFAULT_LAYER_HEIGHT, 0.0F);
                poses.scale(MC_MODEL_SPACE_X, MC_MODEL_SPACE_Y, MC_MODEL_SPACE_Z);
            }
            vanillaLayer.render(poses, buffers, packedLight, player, player.walkAnimation.position(),
                    player.walkAnimation.speed(), partialTick, ageInTicks, netHeadYaw, headPitch);
        } catch (RuntimeException exception) {
            if (!warnedFallbackFailure) {
                EchoAnimatorEpicFightMod.LOGGER.warn(
                        "Epic Fight fallback Echo face layer failed for {}; skipping fallback face render",
                        player.getGameProfile().getName(),
                        exception);
                warnedFallbackFailure = true;
            }
        } finally {
            poses.popPose();
        }
    }

    private void warnRenderFailure(AbstractClientPlayer player, RuntimeException exception) {
        if (!warnedRenderFailure) {
            EchoAnimatorEpicFightMod.LOGGER.warn(
                    "Echo face render failed on Epic Fight Head joint for {}; using original Echo face layer",
                    player.getGameProfile().getName(),
                    exception);
            warnedRenderFailure = true;
        }
    }
}
