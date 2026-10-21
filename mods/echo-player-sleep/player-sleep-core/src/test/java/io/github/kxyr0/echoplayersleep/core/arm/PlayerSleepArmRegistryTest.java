package io.github.kxyr0.echoplayersleep.core.arm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepCommandId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepOwnerId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepScenarioId;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepFailure;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PlayerSleepArmRegistryTest {
    private static final PlayerRef PLAYER = new PlayerRef(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final PlayerSleepOwnerId OWNER = new PlayerSleepOwnerId("story:chapter_1");
    private static final PlayerSleepScenarioId DREAM = new PlayerSleepScenarioId("jdh:chapter_1/first_night_dream");
    private static final PlayerSleepCommandId COMMAND = new PlayerSleepCommandId("story-command-1");

    @Test
    void sameDurableCommandReplaysSameArm() {
        PlayerSleepArmRegistry registry = new PlayerSleepArmRegistry();
        PlayerSleepArmRequest request = new PlayerSleepArmRequest(PLAYER, DREAM, OWNER, COMMAND, 10L);

        PlayerSleepArmDecision first = registry.arm(request);
        PlayerSleepArmDecision replay = registry.arm(request);

        assertTrue(first.accepted());
        assertTrue(replay.accepted());
        assertEquals(first.arm().orElseThrow().armId(), replay.arm().orElseThrow().armId());
    }

    @Test
    void onePlayerCannotHaveTwoActiveArms() {
        PlayerSleepArmRegistry registry = new PlayerSleepArmRegistry();

        registry.arm(new PlayerSleepArmRequest(PLAYER, DREAM, OWNER, COMMAND, 10L));
        PlayerSleepArmDecision conflict = registry.arm(new PlayerSleepArmRequest(PLAYER,
                new PlayerSleepScenarioId("jdh:chapter_1/second_dream"), OWNER,
                new PlayerSleepCommandId("story-command-2"), 11L));

        assertFalse(conflict.accepted());
        assertEquals(PlayerSleepFailure.PLAYER_SLEEP_ALREADY_ARMED, conflict.diagnostics().get(0).code());
    }

    @Test
    void consumedArmIsNotActiveButRemainsReplayable() {
        PlayerSleepArmRegistry registry = new PlayerSleepArmRegistry();
        PlayerSleepArmRequest request = new PlayerSleepArmRequest(PLAYER, DREAM, OWNER, COMMAND, 10L);

        registry.arm(request);
        registry.consume(PLAYER);
        PlayerSleepArmDecision replay = registry.arm(request);

        assertTrue(registry.activeArm(PLAYER).isEmpty());
        assertTrue(replay.accepted());
        assertTrue(replay.arm().orElseThrow().consumed());
    }
}
