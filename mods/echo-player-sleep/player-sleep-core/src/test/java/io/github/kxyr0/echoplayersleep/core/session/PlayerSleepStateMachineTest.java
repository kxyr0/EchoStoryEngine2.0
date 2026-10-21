package io.github.kxyr0.echoplayersleep.core.session;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepStatus;
import org.junit.jupiter.api.Test;

final class PlayerSleepStateMachineTest {
    @Test
    void allowsOnlyAuthoredEntryFlow() {
        PlayerSleepStateMachine machine = new PlayerSleepStateMachine();

        assertTrue(machine.canMove(PlayerSleepStatus.CREATED, PlayerSleepStatus.WAITING_FOR_VANILLA_SLEEP));
        assertTrue(machine.canMove(PlayerSleepStatus.VANILLA_SLEEP_CONFIRMED,
                PlayerSleepStatus.WAITING_FOR_STORY_CLAIM));
        assertTrue(machine.canMove(PlayerSleepStatus.WAITING_FOR_STORY_CLAIM, PlayerSleepStatus.STORY_CLAIMED));
        assertTrue(machine.canMove(PlayerSleepStatus.EYES_CLOSED, PlayerSleepStatus.ENTERING_DREAM));
        assertTrue(machine.canMove(PlayerSleepStatus.EYES_CLOSED, PlayerSleepStatus.STORY_CLAIMED));
        assertTrue(machine.canMove(PlayerSleepStatus.EYES_CLOSED, PlayerSleepStatus.DREAMING));
        assertTrue(machine.canMove(PlayerSleepStatus.EYES_CLOSED, PlayerSleepStatus.RETURNING));
        assertTrue(machine.canMove(PlayerSleepStatus.CLOSING_FOR_RETURN, PlayerSleepStatus.EYES_CLOSED));
        assertFalse(machine.canMove(PlayerSleepStatus.VANILLA_SLEEP_CONFIRMED, PlayerSleepStatus.CLOSING_EYES));
        assertFalse(machine.canMove(PlayerSleepStatus.CREATED, PlayerSleepStatus.DREAMING));
    }

    @Test
    void rejectsDreamingToCompletedWithoutReturn() {
        PlayerSleepStateMachine machine = new PlayerSleepStateMachine();

        assertThrows(IllegalStateException.class,
                () -> machine.move(PlayerSleepStatus.DREAMING, PlayerSleepStatus.COMPLETED));
    }
}
