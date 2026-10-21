package io.github.kxyr0.echovisibility.core.concealment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echovisibility.api.identity.VisibilityConcealmentLeaseId;
import io.github.kxyr0.echovisibility.api.identity.VisibilityDefinitionRevision;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchGeneration;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import org.junit.jupiter.api.Test;

class VisibilityConcealmentLedgerTest {
    @Test
    void claimedNpcRemainsHiddenUntilCommit() {
        VisibilityConcealmentLedger ledger = new VisibilityConcealmentLedger();
        VisibilityConcealmentLease lease = lease("jdh:lease/rapid", 10L);
        assertTrue(ledger.add(lease));
        assertTrue(ledger.hidesNpc("jdh:father"));
        assertTrue(lease.claim());
        assertTrue(ledger.hidesNpc("jdh:father"));
        assertTrue(lease.commit());
        assertFalse(ledger.hidesNpc("jdh:father"));
    }

    @Test
    void unclaimedLeaseTimesOut() {
        VisibilityConcealmentLedger ledger = new VisibilityConcealmentLedger();
        ledger.add(lease("jdh:lease/timeout", 10L));
        assertTrue(ledger.expire(11L).size() == 1);
        assertFalse(ledger.hidesNpc("jdh:father"));
    }

    private static VisibilityConcealmentLease lease(String id, long deadline) {
        return new VisibilityConcealmentLease(new VisibilityConcealmentLeaseId(id),
                new VisibilityWatchId("jdh:father_watch"), new VisibilityWatchGeneration(1L),
                new VisibilityDefinitionRevision(1L), "jdh:father", 1L, deadline);
    }
}
