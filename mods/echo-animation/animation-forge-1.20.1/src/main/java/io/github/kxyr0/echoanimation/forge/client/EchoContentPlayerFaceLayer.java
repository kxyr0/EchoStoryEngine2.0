package io.github.kxyr0.echoanimation.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationFaceRenderContext;
import com.mojang.logging.LogUtils;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/**
 * Renders Echo Content Player face rigs on the normal Minecraft player model.
 */
final class EchoContentPlayerFaceLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CONTENT_PLAYER_NAMESPACE = "echo_content_player";
    private static final String PLAYER_SKIN_TEXTURE_PREFIX = "textures/echo_player/skins/";
    private static final String PLAYER_SKIN_TEXTURE_SUFFIX = ".png";
    private static final FaceRigId BASE_PLAYER_RIG = FaceRigId.parse("echo_content_player:player/base");
    private static final Set<String> REPORTED_LOCAL_SUBJECTS = ConcurrentHashMap.newKeySet();

    private final AnimationClientFaceRenderService renderService;

    EchoContentPlayerFaceLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer,
            AnimationClientFaceRenderService renderService
    ) {
        super(renderer);
        this.renderService = Objects.requireNonNull(renderService, "renderService");
    }

    @Override
    public void render(
            PoseStack poses,
            MultiBufferSource buffers,
            int packedLight,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (player.isInvisible()) {
            return;
        }
        List<FaceRigId> rigs = rigCandidates(player);
        if (rigs.isEmpty()) {
            return;
        }
        AnimationSubjectId subjectId = subjectId(rigs);
        if (isLocalPlayer(player) && REPORTED_LOCAL_SUBJECTS.add(subjectId + "|" + rigs)) {
            LOGGER.info("Echo player face layer rendering local player subject={} rigs={}", subjectId, rigs);
        }
        renderService.renderFace(new AnimationFaceRenderContext(
                subjectId,
                rigs,
                poses,
                buffers,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                partialTick,
                getParentModel().head));
    }

    private List<FaceRigId> rigCandidates(AbstractClientPlayer player) {
        return rigCandidates(skinTextureRigId(getTextureLocation(player)), isLocalPlayer(player));
    }

    static List<FaceRigId> rigCandidates(Optional<FaceRigId> skinRig, boolean localPlayer) {
        Set<FaceRigId> result = new LinkedHashSet<>();
        if (localPlayer) {
            result.add(BASE_PLAYER_RIG);
        }
        skinRig.ifPresent(result::add);
        if (shouldAddBasePlayerRig(skinRig.isPresent(), localPlayer)) {
            result.add(BASE_PLAYER_RIG);
        }
        return List.copyOf(result);
    }

    private boolean isLocalPlayer(AbstractClientPlayer player) {
        var local = Minecraft.getInstance().player;
        return local != null && local.getUUID().equals(player.getUUID());
    }

    static AnimationSubjectId subjectId(List<FaceRigId> rigs) {
        FaceRigId subjectRig = rigs.contains(BASE_PLAYER_RIG) ? BASE_PLAYER_RIG : rigs.get(0);
        return new AnimationSubjectId(subjectRig.toString());
    }

    static Optional<FaceRigId> skinTextureRigId(ResourceLocation skinTexture) {
        Objects.requireNonNull(skinTexture, "skinTexture");
        if (!CONTENT_PLAYER_NAMESPACE.equals(skinTexture.getNamespace())) {
            return Optional.empty();
        }
        String path = skinTexture.getPath();
        if (path.startsWith(PLAYER_SKIN_TEXTURE_PREFIX) && path.endsWith(PLAYER_SKIN_TEXTURE_SUFFIX)) {
            path = path.substring(PLAYER_SKIN_TEXTURE_PREFIX.length(),
                    path.length() - PLAYER_SKIN_TEXTURE_SUFFIX.length());
        }
        try {
            return Optional.of(FaceRigId.parse(skinTexture.getNamespace() + ":" + path));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    static boolean shouldAddBasePlayerRig(boolean hasSkinRig, boolean localPlayer) {
        return hasSkinRig || localPlayer;
    }
}
