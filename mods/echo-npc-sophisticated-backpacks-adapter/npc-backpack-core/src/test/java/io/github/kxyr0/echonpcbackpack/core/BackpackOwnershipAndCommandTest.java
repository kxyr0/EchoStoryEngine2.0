package io.github.kxyr0.echonpcbackpack.core;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackCommandId;
import io.github.kxyr0.echonpcbackpack.api.payload.NpcBackpackPayloadRef;
import io.github.kxyr0.echonpcbackpack.core.command.BackpackCommandLedger;
import io.github.kxyr0.echonpcbackpack.core.ownership.BackpackOwnershipBook;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class BackpackOwnershipAndCommandTest {
    @Test
    void oneContentsUuidCannotBelongToTwoActors() {
        BackpackOwnershipBook ownership = new BackpackOwnershipBook();
        NpcBackpackPayloadRef payload = new NpcBackpackPayloadRef(UUID.randomUUID(),
                "sophisticatedbackpacks:backpack", "fingerprint");
        Assertions.assertTrue(ownership.attach(new ActorId("jdh:first"), payload));
        Assertions.assertFalse(ownership.attach(new ActorId("jdh:second"), payload));
    }

    @Test
    void commandIdRejectsDifferentArgumentsButAcceptsExactRetry() {
        BackpackCommandLedger ledger = new BackpackCommandLedger();
        NpcBackpackCommandId command = new NpcBackpackCommandId("test:equip");
        Assertions.assertEquals(BackpackCommandLedger.Claim.NEW, ledger.claim(command, "source-a"));
        Assertions.assertEquals(BackpackCommandLedger.Claim.DUPLICATE, ledger.claim(command, "source-a"));
        Assertions.assertEquals(BackpackCommandLedger.Claim.CONFLICT, ledger.claim(command, "source-b"));
    }
}
