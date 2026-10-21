package io.github.kxyr0.echoanimation.definition.resolve;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationBlendPolicy;
import io.github.kxyr0.echoanimation.api.animation.definition.MainMode;
import io.github.kxyr0.echoanimation.api.animation.definition.RecoveryPolicy;
import io.github.kxyr0.echoanimation.api.animation.definition.ReplayPolicy;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipPoint;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationRootMotion;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipSegment;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.model.AnimationLoopSeam;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class AnimationDefinitionSegmentResolverTest {
    @Test
    void resolvesDurationsAndSourcePointProgressFromClipMetadata() {
        AnimationDefinition resolved = new AnimationDefinitionSegmentResolver().resolve(definition(), List.of(clip()));

        Assertions.assertEquals(2L, resolved.entryVariants().get(0).durationTicks());
        Assertions.assertEquals(12L, resolved.main().durationTicks());
        Assertions.assertEquals(3L, resolved.exitVariants().get(0).durationTicks());
        Assertions.assertEquals(0.0D, resolved.main().exitPoints().get(0).progress(), 0.0001D);
    }

    @Test
    void unknownSegmentProducesSpecificDiagnosticText() {
        AnimationDefinition original = definition();
        AnimationDefinition bad = new AnimationDefinition(original.id(), original.controlledChannels(),
                original.allowedSourceStates(), original.entryVariants(),
                new AnimationDefinition.MainPhase(original.main().clipId(), Optional.of("missing"), Optional.empty(),
                        MainMode.LOOP_UNTIL_STOP, 0L, original.main().exitPoints()), original.exitVariants(),
                original.markers(), original.targetState(), original.recoveryPolicy(), 3,
                original.replayPolicy(), original.blendPolicy(), "bad");

        IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new AnimationDefinitionSegmentResolver().resolve(bad, List.of(clip())));
        Assertions.assertTrue(error.getMessage().contains("does not contain segment 'missing'"));
    }

    private AnimationDefinition definition() {
        AnimationClipId clip = AnimationClipId.parse("jdh:run");
        AnimationStateId idle = new AnimationStateId("body/idle");
        return new AnimationDefinition(AnimationId.parse("jdh:run"), Set.of(new AnimationChannelId("body")),
                Set.of(idle), List.of(new AnimationDefinition.EntryVariant(idle, clip,
                        Optional.of("idle_to_run"), 0L)),
                new AnimationDefinition.MainPhase(clip, Optional.of("run_loop"), Optional.empty(),
                        MainMode.LOOP_UNTIL_STOP, 0L, List.of(new AnimationDefinition.ExitPoint("neutral", 0.0D,
                        Optional.of("neutral")))),
                List.of(new AnimationDefinition.ExitVariant("neutral", clip, Optional.of("run_to_idle"), 0L)),
                List.of(), idle, RecoveryPolicy.BOUNDED, 3, ReplayPolicy.REJECT_IF_RUNNING,
                AnimationBlendPolicy.NONE, "fingerprint");
    }

    private AnimationClipDefinition clip() {
        return new AnimationClipDefinition(AnimationClipId.parse("jdh:run"), Optional.empty(), List.of(), 17,
                List.of(new AnimationClipSegment("idle_to_run", 0, 2),
                        new AnimationClipSegment("run_loop", 2, 14),
                        new AnimationClipSegment("run_to_idle", 14, 17)),
                List.of(new AnimationClipPoint("neutral", 2, Optional.of("run_loop"))), List.of(),
                AnimationRootMotion.disabled(), 2,
                AnimationLoopSeam.SMOOTH, 4.3D, "fingerprint");
    }
}
