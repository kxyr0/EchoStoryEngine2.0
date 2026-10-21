package io.github.kxyr0.echonpcnavigationadapter.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DeterministicNavigationIdsTest {
    @Test
    void longDialogueSessionIdsCollapseToValidNavigationIds() {
        String ownerExecution = "seating-dialogue/seat-event_7454bc33/jdh_trigger_sat_on_john_seat#story"
                + ":seating-dialogue/seat-event_7454bc33/jdh_trigger_sat_on_john_seat#story"
                + ":asked_okay:2:dialogue_external_wait_payload";

        String execution = DeterministicNavigationIds.execution(ownerExecution).value();
        String owner = DeterministicNavigationIds.owner(ownerExecution).value();

        assertTrue(ownerExecution.length() > 160);
        assertTrue(execution.length() <= 160);
        assertTrue(owner.length() <= 160);
        assertTrue(execution.startsWith("echo_navigation:exec_"));
        assertTrue(owner.startsWith("echo_navigation:owner_"));
        assertEquals(execution, DeterministicNavigationIds.execution(ownerExecution).value());
        assertEquals(owner, DeterministicNavigationIds.owner(ownerExecution).value());
    }
}
