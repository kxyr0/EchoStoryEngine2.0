package io.github.kxyr0.echonpcnavigationadapter.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kxyr0.echonpcnavigationadapter.script.NpcNavigationPayloads.AwaitMode;
import io.github.kxyr0.echonavigation.api.movement.NavigationGait;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class NpcNavigationPayloadsTest {
    @Test
    void awaitModeDefaultsToArrivedForRawStoryActions() {
        assertEquals(AwaitMode.ARRIVED, NpcNavigationPayloads.awaitMode(Map.of()));
    }

    @Test
    void waitFalseMapsToStartedAwait() {
        assertEquals(AwaitMode.STARTED, NpcNavigationPayloads.awaitMode(Map.of("wait", "false")));
    }

    @Test
    void explicitStartedAwaitIsAccepted() {
        assertEquals(AwaitMode.STARTED, NpcNavigationPayloads.awaitMode(Map.of("await", "STARTED")));
    }

    @Test
    void unknownAwaitModeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> NpcNavigationPayloads.awaitMode(Map.of("await", "SOMEDAY")));
    }

    @Test
    void moveAcceptsVectorSpeedAndOptionalAnimationSync() {
        var command = NpcNavigationPayloads.move(Map.of(
                "arg0", "jdh:father",
                "arg1", "Vector3(12,64,-8)",
                "speed", "2.5",
                "syncAnimation", "false"));

        assertEquals(NavigationGait.WALK, command.gait());
        assertEquals(12, command.destination().blockX());
        assertEquals(-8, command.destination().blockZ());
        assertEquals(2.5D, command.speedBlocksPerSecond());
        assertEquals(false, command.synchronizeAnimationSpeed());
    }

    @Test
    void sprintDefaultsToRunAuthoredSpeed() {
        var command = NpcNavigationPayloads.sprint(Map.of(
                "npc", "jdh:father", "x", "12", "y", "64", "z", "-8"));

        assertEquals(NavigationGait.SPRINT, command.gait());
        assertEquals(NpcNavigationPayloads.SPRINT_SPEED_BLOCKS_PER_SECOND,
                command.speedBlocksPerSecond());
    }
}
