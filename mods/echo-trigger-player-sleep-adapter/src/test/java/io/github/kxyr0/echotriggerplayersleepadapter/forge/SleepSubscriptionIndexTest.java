package io.github.kxyr0.echotriggerplayersleepadapter.forge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepEventId;
import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepEventKind;
import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepEventSnapshot;
import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepSessionRef;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepScenarioId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepSessionId;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepRevision;
import io.github.kxyr0.echoplayersleep.api.world.BlockId;
import io.github.kxyr0.echoplayersleep.api.world.BlockPosition;
import io.github.kxyr0.echoplayersleep.api.world.DimensionId;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.subscription.TriggerEventSelector;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import io.github.kxyr0.echotrigger.api.subscription.TriggerSubscriptionId;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SleepSubscriptionIndexTest {
    @Test
    void exactSleepCountOverridesWildcard() {
        TriggerStorySubscription wildcard = subscription("wildcard", OptionalLong.empty(), OptionalLong.empty());
        TriggerStorySubscription fifth = subscription("fifth", OptionalLong.of(5L), OptionalLong.empty());

        List<TriggerStorySubscription> matches = SleepSubscriptionIndex.from(List.of(wildcard, fifth))
                .matches(event(5L));

        assertEquals(List.of(fifth), matches);
    }

    @Test
    void higherMinimumSleepCountOverridesLowerMinimum() {
        TriggerStorySubscription fifthAndLater =
                subscription("fifth", OptionalLong.empty(), OptionalLong.of(5L));
        TriggerStorySubscription tenthAndLater =
                subscription("tenth", OptionalLong.empty(), OptionalLong.of(10L));

        List<TriggerStorySubscription> matches = SleepSubscriptionIndex.from(List.of(fifthAndLater, tenthAndLater))
                .matches(event(12L));

        assertEquals(List.of(tenthAndLater), matches);
    }

    @Test
    void armCandidatesUseNextSleepCountSpecificity() {
        TriggerStorySubscription wildcard = subscription("wildcard", OptionalLong.empty(), OptionalLong.empty());
        TriggerStorySubscription first = subscription("first", OptionalLong.of(1L), OptionalLong.empty());

        List<TriggerStorySubscription> candidates = SleepSubscriptionIndex.from(List.of(wildcard, first))
                .armCandidates(1L);

        assertEquals(List.of(first), candidates);
    }

    private static TriggerStorySubscription subscription(
            String id,
            OptionalLong sleepCount,
            OptionalLong sleepCountAtLeast) {
        return new TriggerStorySubscription(
                new TriggerSubscriptionId("jdh:" + id),
                new TriggerId("jdh:" + id),
                TriggerEventSelector.of("echo_player_sleep:events", "CONFIRMED", "jdh:chapter_1/night",
                        sleepCount, sleepCountAtLeast),
                "jdh:chapter_1/" + id,
                false);
    }

    private static PlayerSleepEventSnapshot event(long sleepCount) {
        return new PlayerSleepEventSnapshot(
                new PlayerSleepEventId("event:" + sleepCount),
                new PlayerSleepSessionRef(new PlayerSleepSessionId("session:" + sleepCount),
                        PlayerSleepRevision.initial()),
                PlayerSleepEventKind.CONFIRMED,
                new PlayerSleepScenarioId("jdh:chapter_1/night"),
                new PlayerRef(UUID.fromString("00000000-0000-0000-0000-000000000005")),
                new DimensionId("minecraft:overworld"),
                new BlockPosition(0, 64, 0),
                new BlockId("minecraft:red_bed"),
                "minecraft:bed",
                sleepCount,
                100L,
                "fingerprint");
    }
}
