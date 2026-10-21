package io.github.kxyr0.echoeffects.core.runtime;

import io.github.kxyr0.echoeffects.api.channel.EffectChannelId;
import io.github.kxyr0.echoeffects.api.channel.EffectTimelineEvent;
import io.github.kxyr0.echoeffects.api.channel.EffectTrack;
import io.github.kxyr0.echoeffects.api.curve.EffectFloatCurve;
import io.github.kxyr0.echoeffects.api.definition.EffectDefinition;
import io.github.kxyr0.echoeffects.api.identity.EffectCommandId;
import io.github.kxyr0.echoeffects.api.identity.EffectDefinitionRevision;
import io.github.kxyr0.echoeffects.api.identity.EffectId;
import io.github.kxyr0.echoeffects.api.status.EffectLifetimePolicy;
import io.github.kxyr0.echoeffects.api.status.EffectRemovalPolicy;
import io.github.kxyr0.echoeffects.api.status.EffectStackingPolicy;
import io.github.kxyr0.echoeffects.api.target.EffectTargetRef;
import io.github.kxyr0.echoeffects.api.time.EffectDuration;
import io.github.kxyr0.echoeffects.api.time.EffectTime;
import io.github.kxyr0.echoeffects.core.catalog.InMemoryEffectCatalog;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EffectRuntimeTest {
    private final EffectId effectId = EffectId.of("jdh:first_morning/daze");
    private final EffectTargetRef target = EffectTargetRef.player(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    @Test
    void applyCreatesActiveInstanceAndCompletesByServerTick() {
        EffectRuntime runtime = runtime(EffectStackingPolicy.RESTART);

        EffectRuntimeResult result = runtime.apply(request("a", 1.0D), 100L);

        Assertions.assertTrue(result.successful());
        Assertions.assertEquals(1, runtime.activeViews(target, 100L).size());
        runtime.tick(111L);
        Assertions.assertEquals(0, runtime.activeViews(target, 111L).size());
    }

    @Test
    void stackingIntensityUsesSingleInstance() {
        EffectRuntime runtime = runtime(EffectStackingPolicy.STACK_INTENSITY);

        runtime.apply(request("a", 1.0D), 0L);
        runtime.apply(request("b", 0.5D), 1L);

        Assertions.assertEquals(1, runtime.activeViews(target, 1L).size());
        Assertions.assertEquals(1.5D, runtime.contributionSnapshot(target, 1L).movementSpeedMultiplier(), 0.0001D);
    }

    @Test
    void timelineEventsDrainOnceWhenDue() {
        EffectRuntime runtime = runtime(EffectStackingPolicy.RESTART);

        runtime.apply(request("a", 1.0D), 100L);

        Assertions.assertEquals(1, runtime.drainTimelineEvents(101L).size());
        Assertions.assertEquals(0, runtime.drainTimelineEvents(102L).size());
    }

    private EffectRuntime runtime(EffectStackingPolicy stacking) {
        InMemoryEffectCatalog catalog = new InMemoryEffectCatalog();
        catalog.publish(List.of(definition(stacking)));
        return new EffectRuntime(catalog);
    }

    private EffectDefinition definition(EffectStackingPolicy stacking) {
        return new EffectDefinition(effectId, EffectDefinitionRevision.initial(), EffectDuration.ticks(10L),
                stacking, EffectLifetimePolicy.INDEPENDENT, EffectRemovalPolicy.FADE_OUT, List.of(),
                List.of(new EffectTrack(EffectChannelId.MOVEMENT_SPEED, EffectFloatCurve.constant(1.0D), 1.0D)),
                List.of(new EffectTimelineEvent(new EffectTime(0L, 0.0F), EffectChannelId.VANILLA_MOB_EFFECT_APPLY,
                        Map.of("effect", "minecraft:nausea", "durationTicks", "80"))));
    }

    private EffectApplyRequest request(String command, double intensity) {
        return new EffectApplyRequest(target, effectId, new EffectCommandId(command), intensity);
    }
}
