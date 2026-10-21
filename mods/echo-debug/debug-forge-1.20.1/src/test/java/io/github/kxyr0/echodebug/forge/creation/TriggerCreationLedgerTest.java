package io.github.kxyr0.echodebug.forge.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodebug.core.diagnostic.DebugDiagnosticCode;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TriggerCreationLedgerTest {
    @Test
    void replaysSameCreationRequest() {
        TriggerCreationLedger ledger = new TriggerCreationLedger();

        TriggerCreationResult first = ledger.apply("r1", "same", this::success);
        TriggerCreationResult second = ledger.apply("r1", "same", () -> fail("should not run"));

        assertTrue(first.successful());
        assertEquals(first, second);
    }

    @Test
    void rejectsSameRequestWithDifferentArguments() {
        TriggerCreationLedger ledger = new TriggerCreationLedger();

        ledger.apply("r1", "same", this::success);
        TriggerCreationResult second = ledger.apply("r1", "different", this::success);

        assertFalse(second.successful());
        assertEquals(DebugDiagnosticCode.DEBUG_REQUEST_IDEMPOTENCY_CONFLICT, second.code());
    }

    private TriggerCreationResult success() {
        return new TriggerCreationResult(true, DebugDiagnosticCode.OK, "OK", Optional.of("jdh:test"),
                Optional.empty(), Optional.empty(), 1, 1, 1, 2);
    }

    private TriggerCreationResult fail(String message) {
        return TriggerCreationResult.failure(DebugDiagnosticCode.DEBUG_TRIGGER_VALIDATION_FAILED, message);
    }
}
