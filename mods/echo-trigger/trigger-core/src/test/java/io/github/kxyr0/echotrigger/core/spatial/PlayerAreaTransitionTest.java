package io.github.kxyr0.echotrigger.core.spatial;

import io.github.kxyr0.echotrigger.api.event.TriggerEventKind;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.api.model.TriggerArea;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerAreaTransitionTest {
    private final PlayerAreaTransition transitions = new PlayerAreaTransition();
    private final TriggerArea area = TriggerArea.block("minecraft:overworld", 5, 5, 5);

    @Test
    void outsideToInsideCreatesEnterAndInsideEvents() {
        PlayerSnapshot snapshot = snapshot(5.25, 5.0, 5.25);
        PlayerAreaMembership previous = new PlayerAreaMembership(snapshot.owner(), area, false);

        var events = transitions.events(snapshot, area, Optional.of(previous), 20);

        assertEquals(2, events.size());
        assertEquals(TriggerEventKind.PLAYER_ENTER, events.get(0).kind());
        assertEquals(TriggerEventKind.PLAYER_INSIDE, events.get(1).kind());
    }

    @Test
    void reconnectBaselineDoesNotCreateSyntheticEnter() {
        PlayerSnapshot snapshot = snapshot(5.25, 5.0, 5.25);

        var events = transitions.events(snapshot, area, Optional.empty(), 20);

        assertEquals(1, events.size());
        assertEquals(TriggerEventKind.PLAYER_INSIDE, events.get(0).kind());
    }

    @Test
    void insideToOutsideCreatesExitEvent() {
        PlayerSnapshot snapshot = snapshot(6.0, 5.0, 5.0);
        PlayerAreaMembership previous = new PlayerAreaMembership(snapshot.owner(), area, true);

        var events = transitions.events(snapshot, area, Optional.of(previous), 20);

        assertEquals(TriggerEventKind.PLAYER_EXIT, events.get(0).kind());
    }

    @Test
    void spatialIndexLimitsCandidatesByDimension() {
        TriggerArea nether = TriggerArea.block("minecraft:the_nether", 5, 5, 5);
        TriggerSpatialIndex index = new TriggerSpatialIndex(java.util.List.of(area, nether));

        assertEquals(1, index.candidates(snapshot(5, 5, 5)).size());
        assertTrue(index.candidates(snapshot(5, 5, 5)).contains(area));
    }

    private static PlayerSnapshot snapshot(double x, double y, double z) {
        return new PlayerSnapshot(new TriggerOwnerId("player:a"), "minecraft:overworld", x, y, z);
    }
}
