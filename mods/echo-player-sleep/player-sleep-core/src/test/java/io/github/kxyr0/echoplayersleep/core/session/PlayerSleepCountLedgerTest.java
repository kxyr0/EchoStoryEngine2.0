package io.github.kxyr0.echoplayersleep.core.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerSleepCountLedgerTest {
    @Test
    void incrementsCountsPerPlayer() {
        PlayerSleepCountLedger ledger = new PlayerSleepCountLedger();
        PlayerRef first = new PlayerRef(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        PlayerRef second = new PlayerRef(UUID.fromString("00000000-0000-0000-0000-000000000002"));

        assertEquals(0L, ledger.count(first));
        assertEquals(1L, ledger.recordConfirmedSleep(first));
        assertEquals(2L, ledger.recordConfirmedSleep(first));
        assertEquals(1L, ledger.recordConfirmedSleep(second));
        assertEquals(2L, ledger.count(first));
        assertEquals(1L, ledger.count(second));
    }

    @Test
    void resetReturnsPreviousCountAndClearsPlayer() {
        PlayerSleepCountLedger ledger = new PlayerSleepCountLedger();
        PlayerRef player = new PlayerRef(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        ledger.recordConfirmedSleep(player);
        ledger.recordConfirmedSleep(player);

        assertEquals(2L, ledger.reset(player));
        assertEquals(0L, ledger.count(player));
        assertEquals(0L, ledger.reset(player));
    }
}
