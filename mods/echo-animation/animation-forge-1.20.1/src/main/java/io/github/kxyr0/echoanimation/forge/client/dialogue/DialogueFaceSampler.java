package io.github.kxyr0.echoanimation.forge.client.dialogue;

import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.id.FacialAnimationId;
import io.github.kxyr0.echoanimation.core.hierarchy.BonePose;
import io.github.kxyr0.echoanimation.core.pose.ClipSampler;
import io.github.kxyr0.echoanimation.definition.resource.AnimationResourceSet;
import io.github.kxyr0.echoanimation.definition.resource.FacialAnimationDefinition;
import io.github.kxyr0.echoanimation.forge.client.dialogue.DialoguePortraitAnimationState.ActiveFaceAnimation;
import java.util.Map;
import java.util.Optional;

final class DialogueFaceSampler {
    private final ClipSampler sampler = new ClipSampler();
    Optional<BonePose> sample( Map<String, ActiveFaceAnimation> target, String key,
            FacialAnimationDefinition.Surface surface, ActiveFaceAnimation active, double sampleTick) {
        double elapsed = secondsSince(active.startTick(), sampleTick);
        if (active.exiting()) {
            return sampleExit(target, key, surface.exit(), elapsed);
        }
        FacialAnimationDefinition.Phase enter = surface.enter();
        if (enter.present() && elapsed < enter.durationSeconds()) {
            return Optional.of(sample(enter, elapsed));
        }
        FacialAnimationDefinition.Phase stay = surface.stay();
        return stay.present() ? Optional.of(sample(stay, Math.max(0.0D, elapsed - enter.durationSeconds())))
                : Optional.empty();
    }

    private Optional<BonePose> sampleExit( Map<String, ActiveFaceAnimation> target, String key,
            FacialAnimationDefinition.Phase exit, double elapsed) {
        if (!exit.present() || elapsed >= exit.durationSeconds()) {
            target.remove(key);
            return Optional.empty();
        }
        return Optional.of(sample(exit, elapsed));
    }

    private BonePose sample(FacialAnimationDefinition.Phase phase, double elapsed) {
        return new BonePose( sampler.sample( phase.tracks(), phase.durationSeconds(), phase.endBehavior(), elapsed)
                        .transforms());
    }

    Optional<FacialAnimationDefinition> animation(
            AnimationResourceSet resources, FaceRigId rigId, String animationId) {
        FacialAnimationId id;
        try {
            id = FacialAnimationId.parse(animationId);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        return resources.facialAnimations().stream() .filter(animation -> animation.id().equals(id))
                .filter(animation -> animation.rigId().map(rigId::equals).orElse(true)) .findFirst();
    }

    private double secondsSince(long startTick, double sampleTick) {
        return Math.max(0.0D, sampleTick - startTick) / 20.0D;
    }

}
