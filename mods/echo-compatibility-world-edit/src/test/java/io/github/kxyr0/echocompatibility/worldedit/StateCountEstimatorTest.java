package io.github.kxyr0.echocompatibility.worldedit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class StateCountEstimatorTest {
    @Test
    void multipliesPropertyCardinalities() {
        StateCountEstimator.Estimate estimate = StateCountEstimator.estimate(List.of(2, 3, 4), 100L);

        assertEquals(24L, estimate.count());
        assertFalse(estimate.exceedsLimit());
    }

    @Test
    void capsBeforeOverflowOrPathologicalGrowth() {
        StateCountEstimator.Estimate estimate = StateCountEstimator.estimate(
                List.of(256, 256, 256), 4_096L);

        assertTrue(estimate.exceedsLimit());
        assertTrue(estimate.count() > 4_096L);
    }

    @Test
    void acceptsTheConfiguredBoundary() {
        StateCountEstimator.Estimate estimate = StateCountEstimator.estimate(List.of(64, 64), 4_096L);

        assertEquals(4_096L, estimate.count());
        assertFalse(estimate.exceedsLimit());
    }

    @Test
    void rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> StateCountEstimator.estimate(List.of(2, -1), 4_096L));
        assertThrows(IllegalArgumentException.class,
                () -> StateCountEstimator.estimate(List.of(2), 0L));
    }
}
