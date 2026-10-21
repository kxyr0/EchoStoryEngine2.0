package io.github.kxyr0.echonpcbackpack.core;

import io.github.kxyr0.echonpcbackpack.core.recovery.BackpackRecoveryDecision;
import io.github.kxyr0.echonpcbackpack.core.recovery.BackpackRecoveryMatrix;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class BackpackRecoveryMatrixTest {
    @Test
    void payloadWithoutSourceContinuesInsteadOfDuplicating() {
        Assertions.assertEquals(BackpackRecoveryDecision.CONTINUE_WITH_PERSISTED_PAYLOAD,
                new BackpackRecoveryMatrix().equip(false, true, false));
    }

    @Test
    void occupiedDestinationKeepsPayloadOnNpc() {
        Assertions.assertEquals(BackpackRecoveryDecision.KEEP_ON_NPC_DESTINATION_OCCUPIED,
                new BackpackRecoveryMatrix().unequip(false, true));
    }
}
