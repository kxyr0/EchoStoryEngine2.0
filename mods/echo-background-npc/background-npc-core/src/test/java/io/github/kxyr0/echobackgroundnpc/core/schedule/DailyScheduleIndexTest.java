package io.github.kxyr0.echobackgroundnpc.core.schedule;

import io.github.kxyr0.echobackgroundnpc.api.definition.BackgroundNpcDefinition;
import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcDefinitionId;
import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.AmbientDialogueId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundAppearanceId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundLocationId;
import io.github.kxyr0.echobackgroundnpc.api.schedule.BackgroundScheduleEntry;
import io.github.kxyr0.echobackgroundnpc.api.source.BackgroundSourceRef;
import io.github.kxyr0.echobackgroundnpc.api.task.BackgroundTask;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class DailyScheduleIndexTest {
    @Test
    void lateLoadResolvesLatestStatefulPhaseWithoutReplayingConversation() {
        DailyScheduleIndex index = new DailyScheduleIndex(definition());
        var phase = index.resolveState(10 * 60 + 30, new BackgroundTask.Stay(location("home")));
        var travel = assertInstanceOf(BackgroundTask.Travel.class, phase.task());
        assertEquals(location("room_204"), travel.destination());
    }

    @Test
    void beforeFirstEntryUsesHomeFallbackAndDailyNextWraps() {
        DailyScheduleIndex index = new DailyScheduleIndex(definition());
        assertEquals(location("home"), assertInstanceOf(BackgroundTask.Stay.class,
                index.resolveState(100, new BackgroundTask.Stay(location("home"))).task()).location());
        assertEquals(8 * 60 + 40, index.next(23 * 60).orElseThrow().minuteOfDay());
    }

    private BackgroundNpcDefinition definition() {
        BackgroundNpcId id = new BackgroundNpcId("jdh:mira");
        var source = new BackgroundSourceRef("mira.es", 1, 1);
        return new BackgroundNpcDefinition(id, new BackgroundNpcDefinitionId(id.value()), location("home"),
                Optional.empty(), Optional.of(new BackgroundAppearanceId("jdh:student")), Optional.empty(),
                Optional.empty(), List.of(
                        new BackgroundScheduleEntry(520, new BackgroundTask.Travel(location("gate")), "mira/520", source),
                        new BackgroundScheduleEntry(590, new BackgroundTask.Converse(new BackgroundNpcId("jdh:anton"),
                                new AmbientDialogueId("jdh:before_class"), Optional.empty()), "mira/590", source),
                        new BackgroundScheduleEntry(600, new BackgroundTask.Travel(location("room_204")),
                                "mira/600", source)), source, 1L);
    }
    private BackgroundLocationId location(String value) { return new BackgroundLocationId("jdh:" + value); }
}
