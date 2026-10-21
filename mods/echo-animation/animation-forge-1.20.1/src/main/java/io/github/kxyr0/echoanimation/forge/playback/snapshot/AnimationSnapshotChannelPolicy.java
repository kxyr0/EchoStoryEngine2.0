package io.github.kxyr0.echoanimation.forge.playback.snapshot;

import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.core.mask.HumanoidBoneMasks;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.PlaybackRecord;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Resolves authored mask and fallback blend policy for a synchronized channel. */
public final class AnimationSnapshotChannelPolicy {
    public double weight(PlaybackRecord playback, AnimationDefinition definition, long duration) {
        if (playback.fallbackExit) {
            return Math.max(0.0D, 1.0D - playback.phaseElapsedTicks / (double) duration);
        }
        int ticks = definition.blendPolicy().enterTicks();
        if (!playback.usedAuthoredEntry && playback.phase == AnimationPhase.MAIN && ticks > 0) {
            return Math.min(1.0D, playback.phaseElapsedTicks / (double) ticks);
        }
        return 1.0D;
    }

    public Set<BoneId> boneMask(AnimationDefinition definition,
            Map<AnimationClipId, AnimationClipDefinition> clips, Optional<AnimationClipId> clipId) {
        if (definition.blendPolicy().maskId().isPresent()) {
            return preset(definition.blendPolicy().maskId().orElseThrow());
        }
        return clipId.flatMap(id -> Optional.ofNullable(clips.get(id)))
                .map(clip -> clip.tracks().stream().map(track -> track.boneId()).collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    private Set<BoneId> preset(String id) {
        return switch (id) {
            case "upper_body" -> HumanoidBoneMasks.upperBody();
            case "head", "head_only" -> HumanoidBoneMasks.headOnly();
            case "left_arm" -> HumanoidBoneMasks.leftArm();
            case "right_arm" -> HumanoidBoneMasks.rightArm();
            case "left_leg" -> HumanoidBoneMasks.leftLeg();
            case "right_leg" -> HumanoidBoneMasks.rightLeg();
            case "full_body" -> HumanoidBoneMasks.fullBody();
            default -> throw new IllegalArgumentException("Unknown animation mask preset: " + id);
        };
    }
}
