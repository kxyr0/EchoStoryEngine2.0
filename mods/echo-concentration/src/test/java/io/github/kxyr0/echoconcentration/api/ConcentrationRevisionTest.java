package io.github.kxyr0.echoconcentration.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConcentrationRevisionTest {
    @Test
    void revisionsAreTypedAndMonotonic() {
        assertEquals(2L, new ConcentrationRevision(1L).next().value());
        assertThrows(IllegalArgumentException.class, () -> new ConcentrationRevision(-1L));
    }
}
