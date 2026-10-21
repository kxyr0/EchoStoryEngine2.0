package io.github.kxyr0.echoplayer.core.mutation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoplayer.api.mutation.PlayerMutationId;
import org.junit.jupiter.api.Test;

final class PlayerMutationLedgerTest {
    @Test
    void sameIdAndRequestReplaysSameResultWithoutSecondMutation() {
        PlayerMutationLedger ledger = new PlayerMutationLedger();
        PlayerMutationId id = new PlayerMutationId("spark-spend-1");

        PlayerMutationReceipt first = ledger.apply(id, "spend:30", () -> "SUCCESS");
        PlayerMutationReceipt replay = ledger.apply(id, "spend:30", () -> "SHOULD_NOT_RUN");

        assertFalse(first.replayed());
        assertTrue(replay.replayed());
        assertFalse(replay.conflict());
        assertEquals("SUCCESS", replay.result());
        assertEquals(1, ledger.size());
    }

    @Test
    void sameIdDifferentArgumentsConflicts() {
        PlayerMutationLedger ledger = new PlayerMutationLedger();
        PlayerMutationId id = new PlayerMutationId("spark-spend-2");

        ledger.apply(id, "spend:30", () -> "SUCCESS");
        PlayerMutationReceipt conflict = ledger.apply(id, "spend:40", () -> "SHOULD_NOT_RUN");

        assertTrue(conflict.conflict());
        assertEquals(1, ledger.size());
    }
}
