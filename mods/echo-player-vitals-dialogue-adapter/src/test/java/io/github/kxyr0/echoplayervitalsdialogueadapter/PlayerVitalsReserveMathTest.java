package io.github.kxyr0.echoplayervitalsdialogueadapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlayerVitalsReserveMathTest {
    @Test
    void setFatigueValueIsVisibleReserveValue() {
        assertEquals(0, PlayerVitalsReserveMath.rawFatigueForReserve(100, 100));
        assertEquals(50, PlayerVitalsReserveMath.rawFatigueForReserve(50, 100));
        assertEquals(100, PlayerVitalsReserveMath.rawFatigueForReserve(0, 100));
    }

    @Test
    void reserveIsClampedToCurrentMaximum() {
        assertEquals(0, PlayerVitalsReserveMath.rawFatigueForReserve(150, 100));
        assertEquals(100, PlayerVitalsReserveMath.rawFatigueForReserve(-5, 100));
    }
}
