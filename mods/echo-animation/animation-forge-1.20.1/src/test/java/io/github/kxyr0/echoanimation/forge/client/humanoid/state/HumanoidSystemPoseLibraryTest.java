package io.github.kxyr0.echoanimation.forge.client.humanoid.state;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationBlendPolicy;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.animation.definition.MainMode;
import io.github.kxyr0.echoanimation.api.animation.definition.RecoveryPolicy;
import io.github.kxyr0.echoanimation.api.animation.definition.ReplayPolicy;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.model.AnimationLoopSeam;
import io.github.kxyr0.echoanimation.definition.resource.AnimationResourceSet;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogCandidate;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogState;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class HumanoidSystemPoseLibraryTest {
    private static final AnimationClipId IDLE_CLIP = AnimationClipId.parse("jdh:humanoid_idle_main");

    @Test
    void legacyNormalizedIdleUsesOwningAnimationDuration() {
        AnimationCatalogState catalog = catalog(82L);
        HumanoidSystemPoseLibrary poses = new HumanoidSystemPoseLibrary(catalog);

        Assertions.assertEquals(82L, poses.playbackDuration(legacyIdleClip()));
    }

    @Test
    void legacyIdleWithoutOwningAnimationUsesSafeDuration() {
        AnimationCatalogState catalog = catalog(0L);
        HumanoidSystemPoseLibrary poses = new HumanoidSystemPoseLibrary(catalog);

        Assertions.assertEquals(80L, poses.playbackDuration(legacyIdleClip()));
    }

    private AnimationCatalogState catalog(long animationDuration) {
        List<AnimationDefinition> animations = animationDuration > 0L
                ? List.of(idleAnimation(animationDuration)) : List.of();
        AnimationResourceSet resources = new AnimationResourceSet(List.of(), List.of(), List.of(), List.of(),
                List.of(legacyIdleClip()), animations, List.of());
        AnimationCatalogState catalog = new AnimationCatalogState();
        catalog.install(new AnimationCatalogCandidate(resources, List.of()));
        return catalog;
    }

    private AnimationClipDefinition legacyIdleClip() {
        return AnimationClipDefinition.legacy(IDLE_CLIP, Optional.empty(), List.of(), new
                AnimationClipDefinition.LegacySettings(3, AnimationLoopSeam.SMOOTH, 1.0D, "idle-clip"));
    }

    private AnimationDefinition idleAnimation(long duration) {
        AnimationStateId idle = new AnimationStateId("body/idle");
        return new AnimationDefinition(AnimationId.parse("jdh:humanoid_idle"),
                Set.of(new AnimationChannelId("body")), Set.of(idle), List.of(),
                new AnimationDefinition.MainPhase(IDLE_CLIP, MainMode.LOOP_UNTIL_STOP, duration, List.of()),
                List.of(), List.of(), idle, RecoveryPolicy.BOUNDED, 0,
                ReplayPolicy.REJECT_IF_RUNNING, AnimationBlendPolicy.NONE, "idle-animation");
    }
}
