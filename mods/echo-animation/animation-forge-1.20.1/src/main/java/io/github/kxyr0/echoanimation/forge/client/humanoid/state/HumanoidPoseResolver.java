package io.github.kxyr0.echoanimation.forge.client.humanoid.state;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.core.playback.NormalizedClipSampler;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationHumanoidRenderContext;
import io.github.kxyr0.echoanimation.forge.client.sync.AnimationClientSyncState;
import io.github.kxyr0.echoanimation.forge.debug.SkinningDebugState;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogState;
import java.util.Optional;
import net.minecraft.client.Minecraft;

/** Resolves the authoritative BODY layers: SIT > foreground BODY > IDLE > bind. */
public final class HumanoidPoseResolver {
    private static final AnimationRigId DEBUG_RIG =
            AnimationRigId.parse("echo_animation:tests/full_humanoid");
    private static final AnimationRigId DEBUG_THERIG_RIG =
            AnimationRigId.parse("jdh:therig_2_6");
    private static final AnimationClipId DEBUG_CLIP =
            AnimationClipId.parse("echo_animation:full_humanoid_test");
    private static final AnimationClipId DEBUG_MATRIX_CLIP =
            AnimationClipId.parse("echo_animation:full_humanoid_matrix_test");

    private final AnimationCatalogState catalogState;
    private final HumanoidSystemPoseLibrary systemPoses;
    private final NormalizedClipSampler sampler = new NormalizedClipSampler();

    public HumanoidPoseResolver(AnimationCatalogState catalogState) {
        this.catalogState = java.util.Objects.requireNonNull(catalogState, "catalogState");
        this.systemPoses = new HumanoidSystemPoseLibrary(catalogState);
    }

    public Optional<AnimationPose> resolve(
            AnimationHumanoidRenderContext context,
            AnimationRigDefinition rig
    ) {
        Optional<AnimationPose> debug = debugPose(context, rig);
        if (debug.isPresent()) {
            return debug;
        }

        // Physical posture is a hard state boundary. Nothing may visually
        // overlay WALK/RUN/foreground BODY while the NPC is sitting.
        if (context.sitting()) {
            return Optional.of(systemPoses.sitting(rig));
        }

        double sampleTick = gameTick() + context.partialTick();
        Optional<AnimationPose> foreground = AnimationClientSyncState.resolvePose(
                context.subjectId(), rig, catalogState.current().animationClips(), sampleTick);
        Optional<AnimationPose> idle = systemPoses.idle(rig, sampleTick);
        if (idle.isEmpty()) {
            return foreground;
        }

        var authored = HumanoidPoseLayers.authoredBones(
                context, rig, catalogState.current().animationClips());
        if (foreground.isEmpty() || authored.isEmpty()) {
            return idle;
        }
        return Optional.of(HumanoidPoseLayers.overlay(idle.orElseThrow(), foreground.orElseThrow(), authored));
    }

    private Optional<AnimationPose> debugPose(
            AnimationHumanoidRenderContext context,
            AnimationRigDefinition rig
    ) {
        if (!SkinningDebugState.targets(context.subjectId())) {
            return Optional.empty();
        }
        AnimationPose bindPose = AnimationPose.defaults(rig.bones());
        if (rig.id().equals(DEBUG_THERIG_RIG)
                && SkinningDebugState.mode() == SkinningDebugState.Mode.THERIG_BIND) {
            return Optional.of(bindPose);
        }
        if (!rig.id().equals(DEBUG_RIG)) {
            return Optional.empty();
        }
        double progress = ((gameTick() + context.partialTick()) % 32.0D) / 32.0D;
        AnimationClipId debugClip = SkinningDebugState.mode() == SkinningDebugState.Mode.MATRIX
                ? DEBUG_MATRIX_CLIP
                : DEBUG_CLIP;
        return catalogState.current().animationClips().stream()
                .filter(clip -> clip.id().equals(debugClip))
                .findFirst()
                .map(clip -> sampler.sample(clip, bindPose, progress))
                .or(() -> Optional.of(bindPose));
    }

    private long gameTick() {
        return Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
    }
}
