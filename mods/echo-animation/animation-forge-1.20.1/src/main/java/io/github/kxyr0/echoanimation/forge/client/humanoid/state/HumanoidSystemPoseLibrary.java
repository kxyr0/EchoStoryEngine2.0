package io.github.kxyr0.echoanimation.forge.client.humanoid.state;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.core.playback.NormalizedClipSampler;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogState;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;

/** Resolves background/system poses that do not own the active BODY playback slot. */
final class HumanoidSystemPoseLibrary {
    private static final long SAFE_IDLE_DURATION_TICKS = 80L;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<AnimationClipId> SIT = ids(
            "jdh:pose_sit_main",
            "jdh:pose_sit");
    private static final List<AnimationClipId> IDLE = ids(
            "jdh:idle_main",
            "jdh:idle",
            "jdh:pose_idle_main",
            "jdh:pose_idle",
            "jdh:humanoid_idle_main",
            "jdh:humanoid_idle");

    private final AnimationCatalogState catalogState;
    private final NormalizedClipSampler sampler = new NormalizedClipSampler();
    private boolean missingSitPoseWarned;

    HumanoidSystemPoseLibrary(AnimationCatalogState catalogState) {
        this.catalogState = java.util.Objects.requireNonNull(catalogState, "catalogState");
    }

    AnimationPose sitting(AnimationRigDefinition rig) {
        AnimationPose defaults = AnimationPose.defaults(rig.bones());
        Optional<AnimationClipDefinition> clip = firstCompatible(SIT, rig);
        if (clip.isEmpty()) {
            warnMissingSitPoseOnce(rig);
            return defaults;
        }
        missingSitPoseWarned = false;
        return sampler.sample(clip.orElseThrow(), defaults, 1.0D);
    }

    Optional<AnimationPose> idle(AnimationRigDefinition rig, double sampleTick) {
        return firstCompatible(IDLE, rig).map(clip -> {
            AnimationPose defaults = AnimationPose.defaults(rig.bones());
            long duration = playbackDuration(clip);
            double progress = sampleTick / duration;
            progress -= Math.floor(progress);
            return sampler.sample(clip, defaults, progress);
        });
    }

    long playbackDuration(AnimationClipDefinition clip) {
        if (clip.durationTicks() > 0) {
            return clip.durationTicks();
        }
        return catalogState.current().animations().stream()
                .map(animation -> animation.main())
                .filter(main -> main.clipId().equals(clip.id()))
                .mapToLong(main -> main.durationTicks())
                .filter(duration -> duration > 0L)
                .findFirst()
                .orElse(SAFE_IDLE_DURATION_TICKS);
    }

    private Optional<AnimationClipDefinition> firstCompatible(
            List<AnimationClipId> candidates,
            AnimationRigDefinition rig
    ) {
        for (AnimationClipId id : candidates) {
            Optional<AnimationClipDefinition> found = catalogState.current().animationClips().stream()
                    .filter(clip -> clip.id().equals(id))
                    .filter(clip -> clip.rigId().map(rig.id()::equals).orElse(true))
                    .findFirst();
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private void warnMissingSitPoseOnce(AnimationRigDefinition rig) {
        if (missingSitPoseWarned) {
            return;
        }
        missingSitPoseWarned = true;
        LOGGER.warn(
                "NPC is sitting but jdh:pose_sit_main was not found for rig {}. "
                        + "BODY remains locked to bind pose; export Blender Action 'pose_sit' as Pose.",
                rig.id());
    }

    private static List<AnimationClipId> ids(String... values) {
        return java.util.Arrays.stream(values).map(AnimationClipId::parse).toList();
    }
}
