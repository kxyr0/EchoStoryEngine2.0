package jdh.echo.animator.epicfight.client.render;

import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

final class EchoFaceIdentity {
    private static final String ECHO_CONTENT_PLAYER_NAMESPACE = "echo_content_player";
    private static final String SKIN_PREFIX = "textures/echo_player/skins/";
    private static final String SKIN_SUFFIX = ".png";
    private static final FaceRigId BASE_PLAYER_RIG = FaceRigId.parse("echo_content_player:player/base");
    private static final List<FaceRigId> NO_RIGS = List.of();
    private static final List<FaceRigId> BASE_RIGS = List.of(BASE_PLAYER_RIG);
    private static final Map<ResourceLocation, Optional<FaceRigId>> SKIN_RIGS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, List<FaceRigId>> LOCAL_CANDIDATES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, List<FaceRigId>> REMOTE_CANDIDATES = new ConcurrentHashMap<>();
    private static final Map<FaceRigId, AnimationSubjectId> SUBJECTS = new ConcurrentHashMap<>();

    private EchoFaceIdentity() {
    }

    static List<FaceRigId> rigCandidates(AbstractClientPlayer player) {
        ResourceLocation skinTexture = player.getSkinTextureLocation();
        if (isLocalPlayer(player)) {
            return LOCAL_CANDIDATES.computeIfAbsent(skinTexture, EchoFaceIdentity::localCandidates);
        }
        return REMOTE_CANDIDATES.computeIfAbsent(skinTexture, EchoFaceIdentity::remoteCandidates);
    }

    static AnimationSubjectId subjectId(List<FaceRigId> rigCandidates) {
        FaceRigId selectedRig = rigCandidates.contains(BASE_PLAYER_RIG)
                ? BASE_PLAYER_RIG
                : rigCandidates.get(0);
        return SUBJECTS.computeIfAbsent(selectedRig, rig -> new AnimationSubjectId(rig.value()));
    }

    private static List<FaceRigId> localCandidates(ResourceLocation skinTexture) {
        Optional<FaceRigId> skinRig = skinRigId(skinTexture);
        if (skinRig.isEmpty() || BASE_PLAYER_RIG.equals(skinRig.get())) {
            return BASE_RIGS;
        }
        return List.of(BASE_PLAYER_RIG, skinRig.get());
    }

    private static List<FaceRigId> remoteCandidates(ResourceLocation skinTexture) {
        Optional<FaceRigId> skinRig = skinRigId(skinTexture);
        if (skinRig.isEmpty()) {
            return NO_RIGS;
        }
        if (BASE_PLAYER_RIG.equals(skinRig.get())) {
            return BASE_RIGS;
        }
        return List.of(skinRig.get(), BASE_PLAYER_RIG);
    }

    private static Optional<FaceRigId> skinRigId(ResourceLocation skinTexture) {
        return SKIN_RIGS.computeIfAbsent(skinTexture, EchoFaceIdentity::parseSkinRigId);
    }

    private static Optional<FaceRigId> parseSkinRigId(ResourceLocation skinTexture) {
        if (!ECHO_CONTENT_PLAYER_NAMESPACE.equals(skinTexture.getNamespace())) {
            return Optional.empty();
        }

        String path = skinTexture.getPath();
        if (!path.startsWith(SKIN_PREFIX) || !path.endsWith(SKIN_SUFFIX)) {
            return Optional.empty();
        }

        String rigPath = path.substring(SKIN_PREFIX.length(), path.length() - SKIN_SUFFIX.length());
        if (rigPath.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(FaceRigId.parse(ECHO_CONTENT_PLAYER_NAMESPACE + ":" + rigPath));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private static boolean isLocalPlayer(AbstractClientPlayer player) {
        return Minecraft.getInstance().player == player;
    }
}
