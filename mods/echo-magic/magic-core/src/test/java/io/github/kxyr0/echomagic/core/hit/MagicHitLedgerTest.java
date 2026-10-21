package io.github.kxyr0.echomagic.core.hit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echomagic.api.hit.MagicHitReceipt;
import io.github.kxyr0.echomagic.api.identity.MagicDamageTypeId;
import io.github.kxyr0.echomagic.api.identity.SpellCastId;
import io.github.kxyr0.echomagic.api.identity.SpellProjectileId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MagicHitLedgerTest {
    @Test void projectileCanRecordDamageOnlyOnce() {
        MagicHitLedger ledger = new MagicHitLedger(8);
        MagicHitReceipt receipt = new MagicHitReceipt(new SpellProjectileId("projectile"), new SpellCastId("cast"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                new MagicDamageTypeId("test:magic"), 2, 20, true);
        assertTrue(ledger.record(receipt));
        assertFalse(ledger.record(receipt));
    }
}

