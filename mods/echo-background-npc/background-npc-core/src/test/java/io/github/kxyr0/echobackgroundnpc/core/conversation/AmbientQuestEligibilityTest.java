package io.github.kxyr0.echobackgroundnpc.core.conversation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AmbientQuestEligibilityTest {
    private final AmbientQuestEligibility eligibility = new AmbientQuestEligibility();

    @Test
    void requiresCurrentObservationDimensionSightAndDistance() {
        assertTrue(eligibility.eligible(true, true, true, 25.0D, 36.0D, 8.0D));
        assertFalse(eligibility.eligible(false, true, true, 25.0D, 36.0D, 8.0D));
        assertFalse(eligibility.eligible(true, false, true, 25.0D, 36.0D, 8.0D));
        assertFalse(eligibility.eligible(true, true, false, 25.0D, 36.0D, 8.0D));
        assertFalse(eligibility.eligible(true, true, true, 81.0D, 100.0D, 8.0D));
    }
}
