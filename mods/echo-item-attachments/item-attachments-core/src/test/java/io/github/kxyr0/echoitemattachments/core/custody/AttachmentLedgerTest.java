package io.github.kxyr0.echoitemattachments.core.custody;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.identity.CustodyId;
import io.github.kxyr0.echoitemattachments.api.identity.ReleaseId;
import io.github.kxyr0.echoitemattachments.api.identity.SubjectKind;
import io.github.kxyr0.echoitemattachments.api.model.ItemCustodyState;
import io.github.kxyr0.echoitemattachments.core.model.ReleasePlan;
import org.junit.jupiter.api.Test;

class AttachmentLedgerTest {
    private static final AttachmentSubjectRef SUBJECT =
            new AttachmentSubjectRef(SubjectKind.NPC, "demo:npc");

    @Test
    void preservesOneCustodyWitnessAcrossFullLifecycle() {
        AttachmentLedger ledger = new AttachmentLedger();
        AttachmentId id = new AttachmentId("test:item");
        ledger.reserveWorld(id, new CustodyId("test:custody"), SUBJECT,
                AttachmentPointId.RIGHT_HAND, "minecraft:diamond", 3, 3, new byte[] {1}, "world-uuid");
        assertEquals(ItemCustodyState.ATTACHED_TO_SUBJECT, ledger.attach(id).state());
        assertEquals(AttachmentPointId.LEFT_HAND, ledger.move(id, AttachmentPointId.LEFT_HAND).point());
        ledger.beginRelease(id, new ReleaseId("test:release"),
                new ReleasePlan("minecraft:overworld", 0, 1, 0, 0, 0, 0));
        assertEquals(ItemCustodyState.WORLD_RELEASED, ledger.confirmReleased(id).state());
        assertEquals(1, ledger.records().size());
    }

    @Test
    void rejectsASecondCustodyForTheSameHand() {
        AttachmentLedger ledger = new AttachmentLedger();
        ledger.reserveWorld(new AttachmentId("test:first"), new CustodyId("test:c1"), SUBJECT,
                AttachmentPointId.RIGHT_HAND, "minecraft:stone", 1, 1, new byte[] {1}, "one");
        assertThrows(CustodyConflictException.class, () -> ledger.reserveWorld(
                new AttachmentId("test:second"), new CustodyId("test:c2"), SUBJECT,
                AttachmentPointId.RIGHT_HAND, "minecraft:dirt", 1, 1, new byte[] {2}, "two"));
    }

    @Test
    void persistsAnExplicitAttachIntentBeforeCompletingTheHandoff() {
        AttachmentLedger ledger = new AttachmentLedger();
        AttachmentId id = new AttachmentId("test:recovering");
        ledger.reserveWorld(id, new CustodyId("test:c3"), SUBJECT,
                AttachmentPointId.LEFT_HAND, "minecraft:book", 2, 5, new byte[] {3}, "three");
        assertEquals(ItemCustodyState.RECOVERING, ledger.beginAttach(id).state());
        assertEquals(ItemCustodyState.ATTACHED_TO_SUBJECT, ledger.completeAttach(id).state());
    }
}
